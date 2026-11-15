import SwiftUI
import UniformTypeIdentifiers

// Renderers of Advanced.kt: SwiftUI APIs built on Swift protocols that an app implements (preference keys, anchors,
// subview containers, custom animations and transitions, keyframes, text renderers, gestures, transferables,
// drop delegates). View kinds 100_040…, modifier ids 100_060…, hand-written value 7 (custom animation).

// MARK: - Calling Kotlin values nested in lists

/// Invokes a Kotlin `SValueCallback` received inside a list argument.
@MainActor func kotlinCall(_ callback: Any?, _ value: Any?) {
    (callback as? SValueCallback)?.invoke(value: value)
    ComposeSwiftUIHostKt.flushCompose()
}

/// Invokes a Kotlin `SAction` received inside a list argument.
@MainActor func kotlinRun(_ action: Any?) {
    (action as? SAction)?.invoke()
    ComposeSwiftUIHostKt.flushCompose()
}

private func doubles(_ raw: Any?) -> [Double] { (raw as? [Any] ?? []).compactMap { ($0 as? NSNumber)?.doubleValue } }

// MARK: - Preferences and anchors

struct KotlinPreferenceKey: PreferenceKey {
    static var defaultValue: [String: [KHashable]] { [:] }
    static func reduce(value: inout [String: [KHashable]], nextValue: () -> [String: [KHashable]]) {
        for (key, values) in nextValue() { value[key, default: []].append(contentsOf: values) }
    }
}

struct AnchoredBounds {
    let anchor: Anchor<CGRect>
    let point: UnitPoint?

    func resolve(_ proxy: GeometryProxy) -> CGRect {
        let r = proxy[anchor]
        guard let point else { return r }
        return CGRect(x: r.minX + r.width * point.x, y: r.minY + r.height * point.y, width: 0, height: 0)
    }
}

struct KotlinAnchorKey: PreferenceKey {
    static var defaultValue: [String: [AnchoredBounds]] { [:] }
    static func reduce(value: inout [String: [AnchoredBounds]], nextValue: () -> [String: [AnchoredBounds]]) {
        for (key, values) in nextValue() { value[key, default: []].append(contentsOf: values) }
    }
}

private let anchorPoints: [UnitPoint?] = [nil, .topLeading, .top, .topTrailing, .leading, .center, .trailing, .bottomLeading, .bottom, .bottomTrailing]

private struct AnchorReaderView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let key = a.string(0)
        let report = a.callback(2, box: { (rects: [CGRect]) -> Any? in rects.map { e_CGRect($0) ?? NSNull() } as NSArray })
        let overlay = a.slotModel(3)
        let layer = { (anchors: [String: [AnchoredBounds]]) in
            GeometryReader { proxy in
                let rects = (anchors[key] ?? []).map { $0.resolve(proxy) }
                SlotView(model: overlay).onChange(of: rects, initial: true) { _, r in report(r) }
            }
        }
        if a.bool(1) {
            ChildrenView(m).backgroundPreferenceValue(KotlinAnchorKey.self, layer)
        } else {
            ChildrenView(m).overlayPreferenceValue(KotlinAnchorKey.self, layer)
        }
    }
}

// MARK: - Subview containers

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
struct KotlinContainerValuesKey: ContainerValueKey {
    static var defaultValue: [String: KHashable] { [:] }
}

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
extension ContainerValues {
    var kotlinValues: [String: KHashable] {
        get { self[KotlinContainerValuesKey.self] }
        set { self[KotlinContainerValuesKey.self] = newValue }
    }
}

private struct SubviewsKey: EnvironmentKey {
    static let defaultValue: [AnyView] = []
}

extension EnvironmentValues {
    var composeSubviews: [AnyView] {
        get { self[SubviewsKey.self] }
        set { self[SubviewsKey.self] = newValue }
    }
}

private struct SubviewsReaderView: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let content = a.slotModel(1)
        if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            let report = a.callback(0, box: { (infos: [[String: KHashable]]) -> Any? in
                infos.map { d in d.flatMap { [$0.key as Any, $0.value.object] } as NSArray } as NSArray
            })
            Group(subviews: SlotView(model: content)) { subviews in
                let infos = subviews.map { $0.containerValues.kotlinValues }
                ChildrenView(m)
                    .environment(\.composeSubviews, subviews.map { AnyView($0) })
                    .onChange(of: infos, initial: true) { _, i in report(i) }
            }
        } else {
            SlotView(model: content)
        }
    }
}

private struct SubviewView: View {
    let m: NodeModel
    let index: Int
    @Environment(\.composeSubviews) private var subviews

    var body: some View {
        if index >= 0, index < subviews.count {
            ModChain.apply(subviews[index], m, 0)
        }
    }
}

// MARK: - Custom animations, transitions, keyframes

struct KotlinCurveAnimation: CustomAnimation, @unchecked Sendable {
    let curve: AnimationCurve
    let duration: Double

    func animate<V: VectorArithmetic>(value: V, time: TimeInterval, context: inout AnimationContext<V>) -> V? {
        guard duration > 0, time < duration else { return nil }
        return value.scaled(by: curve.progress(t: time / duration))
    }

    static func == (l: Self, r: Self) -> Bool { l.curve === r.curve && l.duration == r.duration }
    func hash(into hasher: inout Hasher) {
        hasher.combine(ObjectIdentifier(curve))
        hasher.combine(duration)
    }
}

enum AdvancedValues {
    @MainActor static func decode(_ e: SExpr) -> Any? {
        switch e.id {
        case 7:
            let a = Args(expr: e)
            guard let curve = a.raw(0) as? AnimationCurve else { return nil }
            return Animation(KotlinCurveAnimation(curve: curve, duration: a.double(1)))
        default:
            return nil
        }
    }
}

private struct KotlinTransition: Transition {
    let effects: [[Double]]

    func body(content: Content, phase: TransitionPhase) -> some View {
        let i = phase == .willAppear ? 0 : phase == .identity ? 1 : 2
        return applyViewEffects(content, i < effects.count ? effects[i] : [])
    }
}

struct EffectValues {
    var opacity, scale, x, y, rotation, blur: Double

    init(_ v: [Double]) {
        func at(_ i: Int, _ d: Double) -> Double { i < v.count ? v[i] : d }
        opacity = at(0, 1); scale = at(1, 1); x = at(2, 0); y = at(3, 0); rotation = at(4, 0); blur = at(5, 0)
    }
}

/// Keyframes as phases: each phase is reached with its keyframe's curve and duration (SwiftUI's `KeyframeTrack`
/// builders can't be built from a runtime list).
private func keyframeAnimation(_ frame: [Double]) -> Animation? {
    let duration = frame.count > 6 ? frame[6] : 0.3
    switch frame.count > 7 ? Int(frame[7]) : 0 {
    case 1: return .spring(duration: duration)
    case 2: return .easeInOut(duration: duration)
    case 3: return nil
    default: return .linear(duration: duration)
    }
}

// MARK: - Text renderer

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
private struct KotlinTextRenderer: TextRenderer {
    let granularity: Int
    let provider: EffectsProvider

    private func effects(_ i: Int, _ count: Int, _ rect: CGRect) -> EffectValues {
        EffectValues(provider.effects(phase: Double(i), isIdentity: true, width: Double(count), height: 0,
                                      minX: Double(rect.minX), minY: Double(rect.minY)).map(\.doubleValue))
    }

    private func prepare(_ ctx: GraphicsContext, _ e: EffectValues, _ rect: CGRect) -> GraphicsContext {
        var c = ctx
        c.opacity *= e.opacity
        c.translateBy(x: e.x + rect.midX, y: e.y + rect.midY)
        c.rotate(by: .degrees(e.rotation))
        c.scaleBy(x: e.scale, y: e.scale)
        c.translateBy(x: -rect.midX, y: -rect.midY)
        if e.blur > 0 { c.addFilter(.blur(radius: e.blur)) }
        return c
    }

    func draw(layout: Text.Layout, in ctx: inout GraphicsContext) {
        switch granularity {
        case 0:
            let lines = Array(layout)
            for (i, line) in lines.enumerated() {
                let r = line.typographicBounds.rect
                prepare(ctx, effects(i, lines.count, r), r).draw(line)
            }
        case 1:
            let runs = layout.flatMap { $0 }
            for (i, run) in runs.enumerated() {
                let r = run.typographicBounds.rect
                prepare(ctx, effects(i, runs.count, r), r).draw(run)
            }
        default:
            let slices = layout.flatMap { $0 }.flatMap { $0 }
            for (i, slice) in slices.enumerated() {
                let r = slice.typographicBounds.rect
                prepare(ctx, effects(i, slices.count, r), r).draw(slice)
            }
        }
    }
}

// MARK: - Gestures

#if !os(tvOS)
@MainActor private func dragValues(_ g: DragGesture.Value) -> NSArray {
    [g.translation.width, g.translation.height, g.location.x, g.location.y, g.startLocation.x, g.startLocation.y,
     g.predictedEndTranslation.width, g.predictedEndTranslation.height, g.velocity.width, g.velocity.height]
        .map { KotlinDouble(value: Double($0)) } as NSArray
}

@MainActor private func buildGesture(_ raw: Any?) -> AnyGesture<Void>? {
    guard let s = raw as? [Any], let kind = (s.first as? NSNumber)?.intValue else { return nil }
    func arg(_ i: Int) -> Any? { i < s.count ? s[i] : nil }
    func num(_ i: Int) -> Double { (arg(i) as? NSNumber)?.doubleValue ?? 0 }
    switch kind {
    case 0:
        return AnyGesture(SpatialTapGesture(count: Int(num(1))).onEnded { v in kotlinCall(arg(2), e_CGPoint(v.location)) }.map { _ in () })
    case 1:
        return AnyGesture(LongPressGesture(minimumDuration: num(1), maximumDistance: num(2))
            .onChanged { _ in kotlinCall(arg(3), KotlinBoolean(value: true)) }
            .onEnded { _ in kotlinCall(arg(3), KotlinBoolean(value: false)); kotlinRun(arg(4)) }
            .map { _ in () })
    case 2:
        return AnyGesture(DragGesture(minimumDistance: num(1))
            .onChanged { kotlinCall(arg(2), dragValues($0)) }
            .onEnded { kotlinCall(arg(3), dragValues($0)) }
            .map { _ in () })
    #if os(iOS) || os(macOS)
    case 3:
        return AnyGesture(MagnifyGesture()
            .onChanged { kotlinCall(arg(1), KotlinDouble(value: Double($0.magnification))) }
            .onEnded { kotlinCall(arg(2), KotlinDouble(value: Double($0.magnification))) }
            .map { _ in () })
    case 4:
        return AnyGesture(RotateGesture()
            .onChanged { kotlinCall(arg(1), KotlinDouble(value: $0.rotation.degrees)) }
            .onEnded { kotlinCall(arg(2), KotlinDouble(value: $0.rotation.degrees)) }
            .map { _ in () })
    #endif
    case 5, 6, 7:
        guard let first = buildGesture(arg(1)), let second = buildGesture(arg(2)) else { return nil }
        switch kind {
        case 5: return AnyGesture(SimultaneousGesture(first, second).map { _ in () })
        case 6: return AnyGesture(SequenceGesture(first, second).map { _ in () })
        default: return AnyGesture(ExclusiveGesture(first, second).map { _ in () })
        }
    default:
        return nil
    }
}
#endif

// MARK: - Transfer, drop delegates, files

#if os(iOS) || os(macOS)
private final class DroppedItems: @unchecked Sendable {
    var texts: [String] = []
    var urls: [String] = []
}

private struct KotlinDropDelegate: DropDelegate {
    let handler: DropDelegateHandler

    func validateDrop(info: DropInfo) -> Bool { handler.validateDrop(x: info.location.x, y: info.location.y) }
    func dropEntered(info: DropInfo) { handler.dropEntered(x: info.location.x, y: info.location.y) }
    func dropExited(info: DropInfo) { handler.dropExited() }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        guard let op = d_DropOperation(handler.dropUpdated(x: info.location.x, y: info.location.y) as? SExpr) else { return nil }
        return DropProposal(operation: op)
    }

    func performDrop(info: DropInfo) -> Bool {
        let location = info.location
        let items = DroppedItems()
        let group = DispatchGroup()
        for provider in info.itemProviders(for: [.url, .fileURL, .text]) {
            group.enter()
            if provider.canLoadObject(ofClass: URL.self) {
                _ = provider.loadObject(ofClass: URL.self) { url, _ in
                    DispatchQueue.main.async { if let url { items.urls.append(url.absoluteString) }; group.leave() }
                }
            } else {
                _ = provider.loadObject(ofClass: String.self) { text, _ in
                    DispatchQueue.main.async { if let text { items.texts.append(text) }; group.leave() }
                }
            }
        }
        let handler = self.handler
        group.notify(queue: .main) {
            _ = handler.performDrop(x: location.x, y: location.y, texts: items.texts, urls: items.urls)
            ComposeSwiftUIHostKt.flushCompose()
        }
        return true
    }
}

private struct KotlinDataDocument: FileDocument {
    static var readableContentTypes: [UTType] {
        [.data, .item, .content, .image, .png, .jpeg, .pdf, .json, .plainText, .commaSeparatedText, .zip, .audio, .movie]
    }
    let data: Data

    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}

private func bytes(_ base64: String) -> Data { Data(base64Encoded: base64) ?? Data() }
#endif

// MARK: - Share links

#if !os(tvOS)
/// ShareLink with optional label and `SharePreview(title, image:, icon:)` (asset-catalog names).
@MainActor private func shareLink<T: Transferable>(_ item: T, _ subject: Text?, _ message: Text?, _ a: Args, _ m: NodeModel) -> AnyView {
    let hasLabel = a.bool(3)
    if let title = a.stringOpt(4) {
        let preview: SharePreview<Image, Image>
        switch (a.stringOpt(5), a.stringOpt(6)) {
        case let (image?, icon?): preview = SharePreview(title, image: Image(image), icon: Image(icon))
        case let (image?, nil): preview = SharePreview(title, image: Image(image), icon: Image(systemName: "square.and.arrow.up"))
        case let (nil, icon?): preview = SharePreview(title, image: Image(icon), icon: Image(icon))
        default: preview = SharePreview(title, image: Image(systemName: "doc"), icon: Image(systemName: "doc"))
        }
        if hasLabel { return AnyView(ShareLink(item: item, subject: subject, message: message, preview: preview) { ChildrenView(m) }) }
        return AnyView(ShareLink(item: item, subject: subject, message: message, preview: preview))
    }
    // without a preview, SwiftUI only shares URLs and strings
    switch item {
    case let url as URL:
        if hasLabel { return AnyView(ShareLink(item: url, subject: subject, message: message) { ChildrenView(m) }) }
        return AnyView(ShareLink(item: url, subject: subject, message: message))
    case let text as String:
        if hasLabel { return AnyView(ShareLink(item: text, subject: subject, message: message) { ChildrenView(m) }) }
        return AnyView(ShareLink(item: text, subject: subject, message: message))
    default:
        return AnyView(EmptyView())
    }
}
#endif

// MARK: - Registration

@MainActor enum AdvancedRenderers {
    static func register() {
        ComposeRegistry.views[100_040] = { m in ModChain.apply(AnchorReaderView(m: m), m, 0) }
        ComposeRegistry.views[100_041] = { m in ModChain.apply(SubviewsReaderView(m: m), m, 0) }
        ComposeRegistry.views[100_042] = { m in AnyView(SubviewView(m: m, index: m.args.int(0))) }
        #if !os(tvOS)
        ComposeRegistry.views[100_043] = { m in
            let a = m.args
            let subject = a.stringOpt(1).map { Text($0) }, message = a.stringOpt(2).map { Text($0) }
            if a.bool(7) {
                return ModChain.apply(shareLink(a.string(0), subject, message, a, m), m, 0)
            }
            guard let url = URL(string: a.string(0)) else { return AnyView(EmptyView()) }
            return ModChain.apply(shareLink(url, subject, message, a, m), m, 0)
        }
        #endif

        ComposeRegistry.modifiers[100_060] = { v, a, _ in
            let key = a.string(0), value = KHashable(a.raw(1) as? NSObject ?? NSNull())
            return AnyView(v.transformPreference(KotlinPreferenceKey.self) { $0[key] = [value] })
        }
        ComposeRegistry.modifiers[100_061] = { v, a, _ in
            let key = a.string(0)
            let report = a.callback(1, box: { (values: [KHashable]) -> Any? in values.map(\.object) as NSArray })
            return AnyView(v.onPreferenceChange(KotlinPreferenceKey.self) { all in
                let values = all[key] ?? []
                MainActor.assumeIsolated { report(values) }
            })
        }
        ComposeRegistry.modifiers[100_062] = { v, a, _ in
            let key = a.string(0), i = a.int(1)
            let point = i > 0 && i < anchorPoints.count ? anchorPoints[i] : nil
            return AnyView(v.anchorPreference(key: KotlinAnchorKey.self, value: .bounds) { [key: [AnchoredBounds(anchor: $0, point: point)]] })
        }
        ComposeRegistry.modifiers[100_063] = { v, a, m in
            // every containerValue of the node, so chained ones don't overwrite each other
            var values: [String: KHashable] = [:]
            for (index, mod) in m.modifiers.enumerated() where mod.id == 100_063 {
                let args = Args(node: m.node, src: mod, modifierIndex: index)
                values[args.string(0)] = KHashable(args.raw(1) as? NSObject ?? NSNull())
            }
            guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) else { return v }
            return AnyView(v.containerValue(\.kotlinValues, values))
        }
        ComposeRegistry.modifiers[100_064] = { v, a, _ in
            AnyView(v.transition(KotlinTransition(effects: a.list(0).map(doubles))))
        }
        ComposeRegistry.modifiers[100_065] = { v, a, _ in
            let phases = [doubles(a.list(0) as Any)] + a.list(1).map(doubles)
            guard phases.count > 1 else { return v }
            let indices = Array(phases.indices)
            let animation = { (i: Int) in i == 0 ? Animation?.none : keyframeAnimation(phases[i]) }
            if a.isNil(2) && a.bool(3) {
                return AnyView(v.phaseAnimator(indices) { c, i in applyViewEffects(c, phases[i]) } animation: { animation($0) })
            }
            return AnyView(v.phaseAnimator(indices, trigger: a.hashable(2)) { c, i in applyViewEffects(c, phases[i]) } animation: { animation($0) })
        }
        ComposeRegistry.modifiers[100_066] = { v, a, _ in
            guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *), let provider = a.raw(1) as? EffectsProvider else { return v }
            return AnyView(v.textRenderer(KotlinTextRenderer(granularity: a.int(0), provider: provider)))
        }
        #if !os(tvOS)
        ComposeRegistry.modifiers[100_067] = { v, a, _ in
            guard let gesture = buildGesture(a.list(0)) else { return v }
            switch a.int(1) {
            case 1: return AnyView(v.highPriorityGesture(gesture))
            case 2: return AnyView(v.simultaneousGesture(gesture))
            default: return AnyView(v.gesture(gesture))
            }
        }
        #endif
        #if os(iOS) || os(macOS)
        ComposeRegistry.modifiers[100_068] = { v, a, _ in
            guard let url = URL(string: a.string(0)) else { return v }
            return AnyView(v.draggable(url))
        }
        ComposeRegistry.modifiers[100_069] = { v, a, _ in
            guard let handler = a.raw(0) as? DropHandler else { return v }
            let targeted = a.callback(1, box: KBox.bool)
            return AnyView(v.dropDestination(for: URL.self) { urls, location in
                handler.onDrop(items: urls.map(\.absoluteString), x: Double(location.x), y: Double(location.y))
            } isTargeted: { targeted($0) })
        }
        ComposeRegistry.modifiers[100_070] = { v, a, _ in
            guard let handler = a.raw(1) as? DropDelegateHandler else { return v }
            let types = a.list(0).compactMap { ($0 as? String).flatMap { UTType($0) } }
            return AnyView(v.onDrop(of: types, delegate: KotlinDropDelegate(handler: handler)))
        }
        ComposeRegistry.modifiers[100_071] = { v, a, _ in
            let done = a.callback(5, box: KBox.string), fail = a.callback(6, box: KBox.string)
            return AnyView(v.fileExporter(isPresented: a.boolBinding(0), document: KotlinDataDocument(data: bytes(a.string(2))),
                                          contentType: UTType(a.string(3)) ?? .data, defaultFilename: a.stringOpt(4)) { result in
                switch result {
                case .success(let url): done(url.absoluteString)
                case .failure(let error): fail(error.localizedDescription)
                }
            })
        }
        ComposeRegistry.modifiers[100_072] = { v, a, _ in
            let done = a.callback(3, box: KBox.string), fail = a.callback(4, box: KBox.string)
            return AnyView(v.fileMover(isPresented: a.boolBinding(0), file: URL(string: a.string(2))) { result in
                switch result {
                case .success(let url): done(url.absoluteString)
                case .failure(let error): fail(error.localizedDescription)
                }
            })
        }
        #endif
    }
}
