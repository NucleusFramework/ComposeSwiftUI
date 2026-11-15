import SwiftUI


/// Observable model backing one Kotlin `SNode`. Kotlin notifies it when the node changes, which
/// invalidates only the `NodeView` rendering this node.
@Observable
public final class NodeModel: NSObject, SNodeObserver, Identifiable {
    @ObservationIgnored public let node: SNode

    /// Bumped on every Kotlin change notification; views read it to subscribe (Observation framework:
    /// lighter than one Combine subscription per view).
    public private(set) var version = 0

    private init(node: SNode) {
        self.node = node
    }

    /// Returns the unique model attached to `node`, creating it on first use.
    public static func of(_ node: SNode) -> NodeModel {
        if let model = node.observer as? NodeModel { return model }
        let model = NodeModel(node: node)
        node.observer = model
        return model
    }

    // Bridged copies of the Kotlin collections, rebuilt lazily after each change notification.
    @ObservationIgnored private var cachedChildren: [NodeModel]?
    @ObservationIgnored private var cachedModifiers: [SModifier]?

    public func nodeDidChange() {
        cachedChildren = nil
        cachedModifiers = nil
        version &+= 1
    }

    /// Number of leading modifiers already applied by the view factory (concrete-type methods).
    @ObservationIgnored public var consumedPrefix = 0

    public var nodeId: Int64 { id }
    /// Cached node id (Identifiable): avoids a Kotlin call per ForEach element.
    @ObservationIgnored public lazy var id: Int64 = node.id
    public var kind: Int32 { node.kind }
    public var args: Args { Args(node: node, src: node, modifierIndex: nil) }

    public var children: [NodeModel] {
        if let cachedChildren { return cachedChildren }
        let children = node.children.map(NodeModel.of)
        cachedChildren = children
        return children
    }

    public var modifiers: [SModifier] {
        if let cachedModifiers { return cachedModifiers }
        let count = Int(node.modifierCount())
        var modifiers = [SModifier]()
        modifiers.reserveCapacity(count)
        for i in 0..<count { modifiers.append(node.modifierAt(index: Int32(i))) }
        cachedModifiers = modifiers
        return modifiers
    }

    public func slot(_ index: Int) -> NodeModel? {
        node.slot(index: Int32(index)).map(NodeModel.of)
    }
}

/// Renders one node: builds the SwiftUI view for its kind, then applies its modifiers in order.
@MainActor
public struct NodeView: View {
    let model: NodeModel

    public init(model: NodeModel) {
        self.model = model
    }

    public var body: some View {
        // Reading `version` subscribes this view to the node; the factory builds the concrete view
        // and applies the modifier chain (ModChain).
        let _ = model.version
        return ComposeRegistry.makeView(model)
    }
}

/// The `@ViewBuilder content` of a node: its regular children, identified by stable node ids.
public struct ChildrenView: View {
    let model: NodeModel

    public init(_ model: NodeModel) {
        self.model = model
    }

    public var body: some View {
        // reading `version` re-runs the ForEach when children are inserted / removed / moved
        let _ = model.version
        let children = model.children
        // The common case of a List holding only a LazyForEach: no outer ForEach, which makes SwiftUI handle
        // the rows several times faster than as a nested ForEach.
        if children.count == 1, children[0].kind == LazyForEachContent.kind {
            LazyForEachContent(model: children[0])
        } else {
            ForEach(children, id: \.nodeId) { child in
                // A LazyForEach is expanded here, with static types: behind the node's AnyView, a List would build
                // every row up front instead of only the visible ones.
                if child.kind == LazyForEachContent.kind {
                    LazyForEachContent(model: child)
                } else {
                    NodeView(model: child)
                }
            }
        }
    }
}

/// Content of a secondary builder (label, overlay, sheet content...).
public struct SlotView: View {
    let model: NodeModel?

    public var body: some View {
        if let model {
            ChildrenView(model)
        }
    }

    public init(model: NodeModel?) {
        self.model = model
    }
}

/// Owns a Kotlin composition host for the lifetime of the SwiftUI view.
@MainActor
final class HostHolder: ObservableObject {
    let host: ComposeSwiftUIHost

    init(host: ComposeSwiftUIHost, input: AnyHashable? = nil) {
        self.host = host
        // the first composition already sees the initial input
        if let input { host.updateInput(value: input.base as AnyObject) }
        host.start()
    }

    deinit {
        let host = self.host
        DispatchQueue.main.async { host.dispose() }
    }
}

/// Displays Kotlin Compose SwiftUI content: `ComposeView { MyScreensKt.homeScreen() }`.
/// With `input:`, every new value is pushed into the composition (read it with Kotlin `hostInput()`).
public struct ComposeView: View {
    @StateObject private var holder: HostHolder
    private let input: AnyHashable?

    public init(_ makeHost: @escaping () -> ComposeSwiftUIHost) {
        ComposeGenerated.ensureSetUp()
        _holder = StateObject(wrappedValue: HostHolder(host: makeHost()))
        input = nil
    }

    public init<Input: Hashable>(input: Input, _ makeHost: @escaping () -> ComposeSwiftUIHost) {
        ComposeGenerated.ensureSetUp()
        _holder = StateObject(wrappedValue: HostHolder(host: makeHost(), input: AnyHashable(input)))
        self.input = AnyHashable(input)
    }

    public var body: some View {
        let host = holder.host
        return ChildrenView(NodeModel.of(host.root))
            .onChange(of: input) { _, value in host.updateInput(value: value.map { $0.base as AnyObject }) }
    }
}

/// Registry of view / modifier renderers. The generated code registers itself here; hand-written
/// renderers can override or extend any id.
@MainActor
public enum ComposeRegistry {
    public typealias ViewFactory = (NodeModel) -> AnyView
    public typealias ModifierFactory = (AnyView, Args, NodeModel) -> AnyView

    public static var views: [Int32: ViewFactory] = [:]
    public static var modifiers: [Int: ModifierFactory] = [:]
    public static var selfModifierIds: Set<Int32> = []
    /// Swift views rendered by Kotlin `NativeView(key, …)`; the Args hold the extra Kotlin arguments (from index 1).
    public static var nativeViews: [String: (Args) -> AnyView] = [:]

    static func makeView(_ model: NodeModel) -> AnyView {
        model.consumedPrefix = 0
        if model.kind < 0 {  // SNodeKinds.ROOT / SLOT
            return ModChain.apply(ChildrenView(model), model, 0)
        }
        if let factory = views[model.kind] { return factory(model) }
        return AnyView(Text("Unsupported view kind \(model.kind)").foregroundStyle(.red))
    }
}

/// Applies a node's modifiers from index `i` with generic recursion: each step keeps the concrete
/// SwiftUI type (`ModifiedContent<…>`), and only the final result is type-erased.
@MainActor
public enum ModChain {
    // Never specialized: the recursion over every modifier type would otherwise explode code size
    // and compiler memory under whole-module optimization.
    @_semantics("optimize.sil.specialize.generic.never")
    public static func apply<V: View>(_ v: V, _ m: NodeModel, _ i: Int) -> AnyView {
        let mods = m.modifiers
        guard i < mods.count else { return AnyView(v) }
        let mod = mods[i]
        let id = Int(mod.id)
        let a = Args(node: m.node, src: mod, modifierIndex: i)
        if let applied = GeneratedChain.apply(v, id, a, m, i) { return applied }
        if let custom = ComposeRegistry.modifiers[id] { return apply(custom(AnyView(v), a, m), m, i + 1) }
        return apply(v, m, i + 1)
    }
}
