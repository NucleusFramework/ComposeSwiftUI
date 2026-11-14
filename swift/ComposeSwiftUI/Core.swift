import SwiftUI
import Combine

/// Observable model backing one Kotlin `SNode`. Kotlin notifies it when the node changes, which
/// invalidates only the `NodeView` rendering this node.
public final class NodeModel: NSObject, ObservableObject, SNodeObserver {
    public let node: SNode

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
    private var cachedChildren: [NodeModel]?
    private var cachedModifiers: [SModifier]?

    public func nodeDidChange() {
        cachedChildren = nil
        cachedModifiers = nil
        objectWillChange.send()
    }

    public var nodeId: Int64 { node.id }
    public var kind: Int32 { node.kind }
    public var args: Args { Args(node: node, values: node.args, modifierIndex: nil) }

    public var children: [NodeModel] {
        if let cachedChildren { return cachedChildren }
        let children = node.children.map(NodeModel.of)
        cachedChildren = children
        return children
    }

    public var modifiers: [SModifier] {
        if let cachedModifiers { return cachedModifiers }
        let modifiers = node.modifiers
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
    @ObservedObject var model: NodeModel

    public init(model: NodeModel) {
        self.model = model
    }

    public var body: some View {
        var view = ComposeRegistry.makeView(model)
        for (index, modifier) in model.modifiers.enumerated() {
            // Modifiers of the concrete view type (Image.resizable...) are applied by the view factory.
            if ComposeRegistry.selfModifierIds.contains(modifier.id) { continue }
            let args = Args(node: model.node, values: modifier.args, modifierIndex: index)
            view = ComposeRegistry.applyModifier(view, Int(modifier.id), args, model)
        }
        return view
    }
}

/// The `@ViewBuilder content` of a node: its regular children, identified by stable node ids.
public struct ChildrenView: View {
    // Observed so that inserting / removing / moving children re-runs the ForEach.
    @ObservedObject var model: NodeModel

    public init(_ model: NodeModel) {
        self.model = model
    }

    public var body: some View {
        ForEach(model.children, id: \.nodeId) { NodeView(model: $0) }
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

    init(host: ComposeSwiftUIHost) {
        self.host = host
        host.start()
    }

    deinit {
        let host = self.host
        DispatchQueue.main.async { host.dispose() }
    }
}

/// Displays Kotlin Compose SwiftUI content: `ComposeView { MyScreensKt.homeScreen() }`.
public struct ComposeView: View {
    @StateObject private var holder: HostHolder

    public init(_ makeHost: @escaping () -> ComposeSwiftUIHost) {
        _holder = StateObject(wrappedValue: HostHolder(host: makeHost()))
    }

    public var body: some View {
        ChildrenView(NodeModel.of(holder.host.root))
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

    static func makeView(_ model: NodeModel) -> AnyView {
        if model.kind == SNodeKinds.shared.ROOT || model.kind == SNodeKinds.shared.SLOT {
            return AnyView(ChildrenView(model))
        }
        if let factory = views[model.kind] { return factory(model) }
        return AnyView(Text("Unsupported view kind \(model.kind)").foregroundStyle(.red))
    }

    static func applyModifier(_ view: AnyView, _ id: Int, _ args: Args, _ model: NodeModel) -> AnyView {
        guard let factory = modifiers[id] else { return view }
        return factory(view, args, model)
    }
}
