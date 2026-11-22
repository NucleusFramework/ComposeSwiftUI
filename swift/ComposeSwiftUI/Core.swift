import SwiftUI
import os


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
    /// Cached: the kind never changes, and every render reads it.
    @ObservationIgnored public lazy var kind: Int32 = node.kind
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
        } else if children.count == 1, children[0].kind == EditableForEach.kind {
            // static type, so that List sees the ForEach and offers swipe-to-delete and reordering
            EditableForEach(model: children[0])
        } else {
            ForEach(children, id: \.nodeId) { child in
                // A LazyForEach is expanded here, with static types: behind the node's AnyView, a List would build
                // every row up front instead of only the visible ones.
                if child.kind == LazyForEachContent.kind {
                    LazyForEachContent(model: child)
                } else if child.kind == EditableForEach.kind {
                    EditableForEach(model: child)
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
    }

    /// Starts the composition with the SwiftUI environment around the view (no flash of light mode), restoring the
    /// state saved by an earlier run. Called from `body`, before the first render; no-op afterwards.
    func start(environment: [Any], restoredState: String?) {
        if let restoredState, !host.isStarted { host.restoreState(state: restoredState) }
        host.start(environment: environment)
    }

    deinit {
        let host = self.host
        DispatchQueue.main.async { host.dispose() }
    }
}

/// Displays Kotlin Compose SwiftUI content: `ComposeView { MyScreensKt.homeScreen() }`.
/// With `input:`, every new value is pushed into the composition (read it with Kotlin `hostInput()`).
/// With `restorationID:`, the `rememberSaveable` values and `SavedStateHandle`s are kept in `@SceneStorage` and survive
/// the process (the id must be unique among the scene's `ComposeView`s).
public struct ComposeView: View {
    @StateObject private var holder: HostHolder
    private let input: AnyHashable?
    private let restorationID: String?
    @SceneStorage private var savedState: String?
    @Environment(\.scenePhase) private var scenePhase

    public init(restorationID: String? = nil, _ makeHost: @escaping () -> ComposeSwiftUIHost) {
        ComposeGenerated.ensureSetUp()
        _holder = StateObject(wrappedValue: HostHolder(host: makeHost()))
        input = nil
        self.restorationID = restorationID
        _savedState = SceneStorage("ComposeSwiftUI.\(restorationID ?? "")")
    }

    public init<Input: Hashable>(input: Input, restorationID: String? = nil, _ makeHost: @escaping () -> ComposeSwiftUIHost) {
        ComposeGenerated.ensureSetUp()
        _holder = StateObject(wrappedValue: HostHolder(host: makeHost(), input: AnyHashable(input)))
        self.input = AnyHashable(input)
        self.restorationID = restorationID
        _savedState = SceneStorage("ComposeSwiftUI.\(restorationID ?? "")")
    }

    public var body: some View {
        let holder = self.holder, host = holder.host
        let restored = restorationID == nil ? nil : savedState
        return EnvironmentReporter(
            content: HostContent(host: host),
            report: { host.updateEnvironment(values: $0) },
            first: { holder.start(environment: $0, restoredState: restored) }
        )
        .onAppear { host.setVisible(visible: true) }
        .onDisappear {
            host.setVisible(visible: false)
            save()
        }
        .onChange(of: scenePhase, initial: true) { _, phase in
            host.setScenePhase(phase: phase == .background ? "background" : phase == .inactive ? "inactive" : "active")
            if phase != .active { save() }
        }
        .onChange(of: input) { _, value in host.updateInput(value: value.map { $0.base as AnyObject }) }
    }

    private func save() {
        guard restorationID != nil, holder.host.isStarted else { return }
        savedState = holder.host.saveState()
    }
}

/// The root nodes of a host, or its failure when the composition threw.
struct HostContent: View {
    let host: ComposeSwiftUIHost

    var body: some View {
        let model = NodeModel.of(host.root)
        let _ = model.version
        if let failure = host.failure {
            #if DEBUG
            ScrollView {
                Text(verbatim: "Composition failed: \(failure)").font(.footnote.monospaced()).foregroundStyle(.red).padding()
            }
            #else
            let _ = failure
            EmptyView()
            #endif
        } else {
            ChildrenView(model)
        }
    }
}

/// Writes the Kotlin runtime's log lines to the unified log (`os_log`), subsystem `dev.nucleusframework.composeswiftui`.
final class OSLogRuntimeLogger: NSObject, ComposeSwiftUILogger {
    private let logger = Logger(subsystem: "dev.nucleusframework.composeswiftui", category: "runtime")

    func log(level: ComposeSwiftUILogLevel, message: String) {
        switch level.ordinal {
        case 0: logger.debug("\(message, privacy: .public)")
        case 1: logger.info("\(message, privacy: .public)")
        case 2: logger.error("\(message, privacy: .public)")
        default: logger.fault("\(message, privacy: .public)")
        }
    }
}

/// Logs once per message: renderers may be asked for the same unsupported view on every render.
@MainActor
enum RuntimeLog {
    private static var logged = Set<String>()

    static func errorOnce(_ message: String) {
        guard logged.insert(message).inserted else { return }
        ComposeSwiftUI.shared.logger.log(level: .error, message: "Compose SwiftUI: \(message)")
    }
}

/// Opt-in renderers for the SwiftUI APIs of other Apple frameworks (MapKit, Charts, StoreKit…): add the
/// framework's file from `ComposeSwiftUIIntegrations` to the app and call its `register…()` once at launch.
public enum ComposeIntegrations {}

/// Calls a synchronous Kotlin function (`KotlinFunction`) passed as an argument; nil when absent.
func kotlinInvoke(_ f: Any?, _ args: [Any]) -> Any? { (f as? KotlinFunction)?.invoke(args: args) }

/// Awaits a Kotlin `suspend` function (`SAsyncFunction`) passed as an argument; nil when absent, failed or cancelled.
@MainActor func kotlinAwait(_ f: Any?, _ args: [Any]) async -> Any? {
    guard let function = f as? SAsyncFunction else { return nil }
    let cancel = UncheckedSendable<SwiftAction?>(nil)
    let result = UncheckedSendable<Any?>(nil)
    await withTaskCancellationHandler {
        await withCheckedContinuation { (done: CheckedContinuation<Void, Never>) in
            cancel.value = function.start(args: args, done: KotlinResultReceiver { value in
                result.value = value
                done.resume()
            })
            ComposeSwiftUIHostKt.flushCompose()
        }
    } onCancel: {
        Task { @MainActor in cancel.value?.run() }
    }
    return result.value
}

/// Receives the result of a Kotlin `SAsyncFunction` (`[result]`).
final class KotlinResultReceiver: NSObject, KotlinFunction {
    let receive: (Any?) -> Void
    init(_ receive: @escaping (Any?) -> Void) { self.receive = receive }
    func invoke(args: [Any]) -> Any? {
        receive(args.first.flatMap { $0 is NSNull ? nil : $0 })
        return nil
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
    /// Hand-written Kotlin values (ids 900…999) of the framework integrations.
    public static var values: [Int32: (SExpr) -> Any?] = [:]

    static func makeView(_ model: NodeModel) -> AnyView {
        let view = makeNodeView(model)
        guard ComposeInspection.enabled else { return view }
        return AnyView(view.background(InspectionFrameReporter(id: model.node.id)))
    }

    private static func makeNodeView(_ model: NodeModel) -> AnyView {
        model.consumedPrefix = 0
        if model.kind < 0 {  // SNodeKinds.ROOT / SLOT
            return ModChain.apply(ChildrenView(model), model, 0)
        }
        if let factory = views[model.kind] { return factory(model) }
        return unsupported("Unsupported view kind \(model.kind)")
    }

    /// A red message in debug builds; nothing (and a log line) in release builds.
    static func unsupported(_ message: String) -> AnyView {
        #if DEBUG
        return AnyView(Text(verbatim: message).foregroundStyle(.red))
        #else
        RuntimeLog.errorOnce(message)
        return AnyView(EmptyView())
        #endif
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

/// For the IDE previews: when enabled, every node reports its frame (window coordinates) under its Kotlin node id, so
/// a point of a preview maps to the node, then to its code. Off in apps.
@MainActor
public enum ComposeInspection {
    public static var enabled = false
    public static var frames: [Int64: CGRect] = [:]
}

private struct InspectionFrameReporter: View {
    let id: Int64

    var body: some View {
        // Lighter than a GeometryReader: no layout container, only a callback when the frame changes.
        Color.clear.onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { frame in
            ComposeInspection.frames[id] = frame
        }
    }
}
