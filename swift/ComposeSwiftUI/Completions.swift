import SwiftUI
import UniformTypeIdentifiers
import Accessibility

// Renderers of Completions.kt: editable lists, the remaining custom styles, text selection, accessibility rotors,
// custom alignments and layout values, mixed-state controls, pasteboard and Services commands (macOS), pencil and
// modifier-key events, AsyncImage requests. View kinds 100_050…, modifier ids 100_080…, hand-written values 8 and 9.

// MARK: - Editable ForEach

/// `ForEach(items).onDelete().onMove()` over Kotlin rows; expanded by `ChildrenView` with static types.
public struct EditableForEach: View {
    static let kind: Int32 = 100_050
    let model: NodeModel

    public var body: some View {
        let _ = model.version
        let a = model.args
        let delete = a.isNil(0) ? nil : a.callback(0, box: { (s: IndexSet) -> Any? in s.map { KotlinInt(value: Int32($0)) } as NSArray })
        let move = a.isNil(1) ? nil : a.callback(1, box: { (v: NSArray) -> Any? in v })
        // one SwiftUI row per Kotlin item: a nested ForEach would be flattened by List and get its own offsets
        let rows = ForEach(model.children, id: \.nodeId) { EditableRow(group: $0) }
            .onDelete(perform: delete.map { d in { d($0) } })
        #if os(watchOS)
        rows
        #else
        rows.onMove(perform: move.map { f in
            { (s: IndexSet, to: Int) in f([s.map { KotlinInt(value: Int32($0)) } as NSArray, KotlinInt(value: Int32(to))] as NSArray) }
        })
        #endif
    }
}

private struct EditableRow: View {
    let group: NodeModel

    var body: some View {
        let _ = group.version
        let children = group.children
        if children.count == 1 {
            NodeView(model: children[0])
        } else {
            VStack(alignment: .leading) { ForEach(children, id: \.nodeId) { NodeView(model: $0) } }
        }
    }
}

// MARK: - Custom styles (continued)

private struct StyleContentKey: EnvironmentKey {
    static let defaultValue: AnyView? = nil
}

private struct StyleFractionKey: EnvironmentKey {
    static let defaultValue: Double? = nil
}

extension EnvironmentValues {
    var composeStyleContent: AnyView? {
        get { self[StyleContentKey.self] }
        set { self[StyleContentKey.self] = newValue }
    }
    var composeStyleFraction: Double? {
        get { self[StyleFractionKey.self] }
        set { self[StyleFractionKey.self] = newValue }
    }
}

private struct StyleContentView: View {
    let m: NodeModel
    @Environment(\.composeStyleContent) private var content

    var body: some View {
        if let content { ModChain.apply(content, m, 0) }
    }
}

private struct StyleProgressFillView: View {
    let m: NodeModel
    @Environment(\.composeStyleFraction) private var fraction

    var body: some View {
        ModChain.apply(GeometryReader { proxy in
            Rectangle().frame(width: proxy.size.width * min(max(fraction ?? 0, 0), 1))
        }, m, 0)
    }
}

private func styled(_ body: NodeModel?, label: AnyView? = nil, content: AnyView? = nil) -> some View {
    SlotView(model: body)
        .environment(\.composeStyleLabel, label)
        .environment(\.composeStyleContent, content)
}

#if os(iOS) || os(macOS)
private struct KotlinGroupBoxStyle: GroupBoxStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, label: AnyView(configuration.label), content: AnyView(configuration.content))
    }
}
#endif

private struct KotlinLabeledContentStyle: LabeledContentStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, label: AnyView(configuration.label), content: AnyView(configuration.content))
    }
}

#if !os(watchOS)
private struct KotlinControlGroupStyle: ControlGroupStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, label: AnyView(configuration.label), content: AnyView(configuration.content))
    }
}

private struct KotlinMenuStyle: MenuStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, content: AnyView(Menu(configuration)))
    }
}
#endif

private struct KotlinFormStyle: FormStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, content: AnyView(configuration.content))
    }
}

private struct KotlinProgressViewStyle: ProgressViewStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, label: configuration.label.map { AnyView($0) })
            .environment(\.composeStyleFraction, configuration.fractionCompleted)
    }
}

// MARK: - Text selection

#if os(iOS) || os(macOS)
/// Text typed but not yet echoed back by Kotlin: SwiftUI reports the new selection before the new text
/// reaches the args, and the selection's indices belong to the typed string.
private final class PendingText {
    var value: String?
}

private func textBinding(_ m: NodeModel, _ i: Int, _ pending: PendingText) -> Binding<String> {
    let base = m.args.stringBinding(i)
    return Binding(
        get: {
            let text = m.args.string(i)
            if pending.value == text { pending.value = nil }
            return text
        },
        set: { pending.value = $0; base.wrappedValue = $0 }
    )
}

@available(iOS 18.0, macOS 15.0, *)
private func selectionBinding(_ m: NodeModel, textIndex: Int, rangeIndex: Int, callbackIndex: Int, pending: PendingText) -> Binding<TextSelection?> {
    let report = m.args.callback(callbackIndex, box: { (r: [Int]?) -> Any? in
        r.map { $0.map { KotlinInt(value: Int32($0)) } as NSArray } ?? NSNull()
    })
    return Binding(
        get: {
            let a = m.args
            let text = a.string(textIndex)
            let r = a.list(rangeIndex).compactMap { ($0 as? NSNumber)?.intValue }
            guard r.count == 2, r[0] >= 0, r[1] <= text.utf16.count, r[0] <= r[1] else { return nil }
            let lo = String.Index(utf16Offset: r[0], in: text), hi = String.Index(utf16Offset: r[1], in: text)
            return TextSelection(range: lo..<hi)
        },
        set: { selection in
            guard let selection, case .selection(let range) = selection.indices else { report(nil); return }
            func send(retry: Bool) {
                let text = pending.value ?? m.args.string(textIndex)
                if range.upperBound <= text.endIndex {
                    report([range.lowerBound.utf16Offset(in: text), range.upperBound.utf16Offset(in: text)])
                } else if retry {
                    // the selection can arrive before the text it indexes: wait for the text binding
                    DispatchQueue.main.async { send(retry: false) }
                }
            }
            send(retry: true)
        }
    )
}

private struct SelectionTextFieldView: View {
    let m: NodeModel
    @State private var pending = PendingText()

    var body: some View {
        let _ = m.version
        let a = m.args
        let axis = d_Axis(a.expr(5)) ?? .horizontal
        if #available(iOS 18.0, macOS 15.0, *) {
            TextField(a.string(0), text: textBinding(m, 1, pending), selection: selectionBinding(m, textIndex: 1, rangeIndex: 3, callbackIndex: 4, pending: pending), axis: axis)
        } else {
            TextField(a.string(0), text: a.stringBinding(1), axis: axis)
        }
    }
}

private struct SelectionTextEditorView: View {
    let m: NodeModel
    @State private var pending = PendingText()

    var body: some View {
        let _ = m.version
        let a = m.args
        if #available(iOS 18.0, macOS 15.0, *) {
            TextEditor(text: textBinding(m, 0, pending), selection: selectionBinding(m, textIndex: 0, rangeIndex: 2, callbackIndex: 3, pending: pending))
        } else {
            TextEditor(text: a.stringBinding(0))
        }
    }
}
#endif

// MARK: - Accessibility

private struct KotlinChart: AXChartDescriptorRepresentable {
    let title, xTitle, yTitle: String
    let categories: [String]
    let values: [Double]
    let continuous: Bool

    func makeChartDescriptor() -> AXChartDescriptor {
        let x = AXCategoricalDataAxisDescriptor(title: xTitle, categoryOrder: categories)
        let lo = values.min() ?? 0, hi = max(values.max() ?? 1, lo + 1)
        let y = AXNumericDataAxisDescriptor(title: yTitle, range: lo...hi, gridlinePositions: []) { "\($0)" }
        let points = zip(categories, values).map { AXDataPoint(x: $0, y: $1) }
        let series = AXDataSeriesDescriptor(name: title, isContinuous: continuous, dataPoints: points)
        return AXChartDescriptor(title: title, summary: nil, xAxis: x, yAxis: y, additionalAxes: [], series: [series])
    }
}

private struct RotorEntryModifier: ViewModifier {
    let id: KHashable
    let namespace: Int64
    @Environment(\.composeNamespaces) private var namespaces

    func body(content: Content) -> some View {
        if let ns = namespaces[namespace] {
            content.accessibilityRotorEntry(id: id, in: ns)
        } else {
            content
        }
    }
}

// MARK: - Custom alignments

private enum KH0: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH1: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH2: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH3: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH4: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH5: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH6: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KH7: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.leading] } }
private enum KV0: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV1: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV2: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV3: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV4: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV5: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV6: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }
private enum KV7: AlignmentID { static func defaultValue(in d: ViewDimensions) -> CGFloat { d[.top] } }

// ponytail: eight guides per axis; add types here if an app needs more distinct custom guides
private let horizontalIDs: [AlignmentID.Type] = [KH0.self, KH1.self, KH2.self, KH3.self, KH4.self, KH5.self, KH6.self, KH7.self]
private let verticalIDs: [AlignmentID.Type] = [KV0.self, KV1.self, KV2.self, KV3.self, KV4.self, KV5.self, KV6.self, KV7.self]

/// Custom presentation detents: their Kotlin height functions, by index (static requirement, hence the table).
@MainActor enum KotlinDetents {
    nonisolated(unsafe) static var providers: [AlignmentGuideProvider?] = [nil, nil, nil, nil]
    static func height(_ i: Int, _ max: CGFloat) -> CGFloat? {
        guard let p = providers[i] else { return nil }
        let h = p.compute(width: Double(max), height: 0)
        return h.isNaN ? nil : CGFloat(h)
    }
}

private struct KD0: CustomPresentationDetent { static func height(in c: Context) -> CGFloat? { MainActor.assumeIsolated { KotlinDetents.height(0, c.maxDetentValue) } } }
private struct KD1: CustomPresentationDetent { static func height(in c: Context) -> CGFloat? { MainActor.assumeIsolated { KotlinDetents.height(1, c.maxDetentValue) } } }
private struct KD2: CustomPresentationDetent { static func height(in c: Context) -> CGFloat? { MainActor.assumeIsolated { KotlinDetents.height(2, c.maxDetentValue) } } }
private struct KD3: CustomPresentationDetent { static func height(in c: Context) -> CGFloat? { MainActor.assumeIsolated { KotlinDetents.height(3, c.maxDetentValue) } } }

enum CompletionValues {
    @MainActor static func decode(_ e: SExpr) -> Any? {
        let a = Args(expr: e)
        switch e.id {
        case 13: return shapeOperation(a)
        case 14:
            guard let g = d_Gradient(a.expr(0)), let space = d_Gradient_ColorSpace(a.expr(1)) else { return nil }
            if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) { return g.colorSpace(space) }
            return AnyGradient(g)
        case 8: return HorizontalAlignment(horizontalIDs[min(max(a.int(0), 0), 7)])
        case 9: return VerticalAlignment(verticalIDs[min(max(a.int(0), 0), 7)])
        case 10: return ExplicitTimelineSchedule(a.list(0).compactMap { ($0 as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue) } })
        case 11: return ImagePaint(image: Image(a.string(0)), scale: a.cgFloat(1))
        case 12:
            let i = min(max(a.int(0), 0), 3)
            KotlinDetents.providers[i] = a.raw(1) as? AlignmentGuideProvider
            switch i {
            case 0: return PresentationDetent.custom(KD0.self)
            case 1: return PresentationDetent.custom(KD1.self)
            case 2: return PresentationDetent.custom(KD2.self)
            default: return PresentationDetent.custom(KD3.self)
            }
        default: return nil
        }
    }
}

// MARK: - Layout values

struct KotlinLayoutValuesKey: LayoutValueKey {
    static let defaultValue: [String: KHashable] = [:]
}

/// Every `layoutValue` of the node (chained ones are merged).
@MainActor private func layoutValues(_ m: NodeModel) -> [String: KHashable] {
    var values: [String: KHashable] = [:]
    for (index, mod) in m.modifiers.enumerated() where mod.id == 100_091 {
        let a = Args(node: m.node, src: mod, modifierIndex: index)
        values[a.string(0)] = KHashable(a.raw(1) as? NSObject ?? NSNull())
    }
    return values
}

// MARK: - Pasteboard

#if os(macOS)
private func loadTexts(_ providers: [NSItemProvider], _ done: @escaping @MainActor ([String]) -> Void) {
    final class Box: @unchecked Sendable { var texts: [String] = [] }
    let box = Box(), group = DispatchGroup()
    for p in providers where p.canLoadObject(ofClass: String.self) {
        group.enter()
        _ = p.loadObject(ofClass: String.self) { text, _ in
            DispatchQueue.main.async { if let text { box.texts.append(text) }; group.leave() }
        }
    }
    group.notify(queue: .main) { MainActor.assumeIsolated { done(box.texts) } }
}
#endif

// MARK: - iOS 26 / 27 additions

@available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *)
private func lineHeight(_ kind: Int, _ value: Double) -> AttributedString.LineHeight {
    switch kind {
    case 0: return .variable
    case 2: return .tight
    case 3: return .loose
    case 4: return .multiple(factor: value)
    case 5: return .leading(increase: value)
    case 6: return .exact(points: value)
    default: return .normal
    }
}

@available(iOS 27.1, macOS 27.1, tvOS 27.1, watchOS 27.1, *)
private struct KotlinArrangementStyle: ArrangementViewStyle {
    let body: NodeModel?
    func makeBody(configuration: Configuration) -> some View {
        styled(body, label: AnyView(configuration.primary), content: AnyView(configuration.secondary))
    }
}

/// Container items reordered or dragged by id (`draggable(containerItemID:)` tags them with the same ids).
private struct KotlinItem: Identifiable, Transferable {
    let id: AnyHashable
    var text = ""
    static var transferRepresentation: some TransferRepresentation { ProxyRepresentation(exporting: \.text) }
}

private func kotlinKey(_ id: AnyHashable) -> Any { (id.base as? KHashable)?.object ?? NSNull() }

private func dragPhase(_ p: Any) -> Int {
    switch "\(p)" {
    case let s where s.hasPrefix("initial"): return 0
    case let s where s.hasPrefix("active"): return 1
    case let s where s.hasPrefix("ending"): return 2
    case let s where s.hasPrefix("ended"): return 3
    case let s where s.hasPrefix("dataTransferCompleted"): return 4
    case let s where s.hasPrefix("entering"): return 5
    case let s where s.hasPrefix("exiting"): return 6
    default: return 1
    }
}

// MARK: - Registration

@MainActor enum CompletionRenderers {
    static func register() {
        registerNewest()
        ComposeRegistry.views[100_062] = { m in
            let text = Text(LocalizedStringKey(m.args.string(0)))
            if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *), let v = dp_TextVariantPreference(m.args.expr(1)) {
                return ModChain.apply(text.textVariant(v), m, 0)
            }
            return ModChain.apply(text, m, 0)
        }
        ComposeRegistry.views[100_060] = { m in
            let text = m.args.stringOpt(0)
            return ModChain.apply(text.map { ContentUnavailableView.search(text: $0) } ?? ContentUnavailableView.search, m, 0)
        }
        #if os(iOS)
        ComposeRegistry.modifiers[100_111] = { v, a, _ in
            let report = a.callback(1, box: KBox.bool)
            let editing = a.bool(0)
            return AnyView(v.environment(\.editMode, Binding<EditMode>(get: { editing ? .active : .inactive },
                                                                        set: { report($0.isEditing) })))
        }
        #endif
        ComposeRegistry.modifiers[100_112] = { v, a, _ in
            guard let handler = a.raw(0) as? DropHandler else { return v }
            return AnyView(v.environment(\.openURL, OpenURLAction { url in
                MainActor.assumeIsolated { handler.onDrop(items: [url.absoluteString], x: 0, y: 0) } ? .handled : .systemAction
            }))
        }
        #if os(macOS)
        ComposeRegistry.modifiers[100_113] = { v, a, _ in
            guard #available(macOS 15.0, *) else { return v }
            return AnyView(v.pointerStyle(.image(Image(a.string(0)), hotSpot: d_UnitPoint(a.expr(1)) ?? .center)))
        }
        #endif
        ComposeRegistry.views[100_050] = { m in AnyView(EditableForEach(model: m)) }
        ComposeRegistry.views[100_051] = { m in AnyView(StyleContentView(m: m)) }
        ComposeRegistry.views[100_052] = { m in AnyView(StyleProgressFillView(m: m)) }
        #if os(iOS) || os(macOS)
        ComposeRegistry.views[100_053] = { m in ModChain.apply(SelectionTextFieldView(m: m), m, 0) }
        ComposeRegistry.views[100_054] = { m in ModChain.apply(SelectionTextEditorView(m: m), m, 0) }
        ComposeRegistry.views[100_064] = { m in ModChain.apply(formattedNumber(m.args), m, 0) }
        #endif
        ComposeRegistry.views[100_055] = { m in
            let report = m.args.callback(2, box: { (v: NSArray) -> Any? in v })
            let sources = m.args.list(1).indices.map { i in
                Binding<Bool>(
                    get: { let l = m.args.list(1); return i < l.count ? (l[i] as? NSNumber)?.boolValue ?? false : false },
                    set: { report([KotlinInt(value: Int32(i)), KotlinBoolean(value: $0)] as NSArray) }
                )
            }
            return ModChain.apply(Toggle(sources: sources, isOn: \.self) { Text(m.args.string(0)) }, m, 0)
        }
        ComposeRegistry.views[100_056] = { m in
            let report = m.args.callback(2, box: { (v: NSArray) -> Any? in v })
            let sources = m.args.list(1).indices.map { i in
                Binding<AnyHashable>(
                    get: { let l = m.args.list(1); return AnyHashable(KHashable(i < l.count ? l[i] as? NSObject ?? NSNull() : NSNull())) },
                    set: { report([KotlinInt(value: Int32(i)), ($0.base as? KHashable)?.object ?? NSNull()] as NSArray) }
                )
            }
            return ModChain.apply(Picker(sources: sources, selection: \.self, content: { ChildrenView(m) }, label: { Text(m.args.string(0)) }), m, 0)
        }
        ComposeRegistry.views[100_057] = { m in
            let a = m.args
            guard let url = URL(string: a.string(0)) else { return AnyView(EmptyView()) }
            if #available(iOS 27.0, macOS 27.0, tvOS 27.0, watchOS 27.0, *) {
                var request = URLRequest(url: url)
                for (name, value) in zip(a.list(1), a.list(2)) {
                    if let name = name as? String, let value = value as? String { request.setValue(value, forHTTPHeaderField: name) }
                }
                return ModChain.apply(AsyncImage(request: request, scale: a.cgFloat(3)), m, 0)
            }
            return ModChain.apply(AsyncImage(url: url, scale: a.cgFloat(3)), m, 0)
        }

        #if os(iOS) || os(macOS)
        ComposeRegistry.modifiers[100_080] = { v, a, _ in AnyView(v.groupBoxStyle(KotlinGroupBoxStyle(body: a.slotModel(0)))) }
        #endif
        ComposeRegistry.modifiers[100_081] = { v, a, _ in AnyView(v.labeledContentStyle(KotlinLabeledContentStyle(body: a.slotModel(0)))) }
        #if !os(watchOS)
        ComposeRegistry.modifiers[100_082] = { v, a, _ in AnyView(v.controlGroupStyle(KotlinControlGroupStyle(body: a.slotModel(0)))) }
        ComposeRegistry.modifiers[100_084] = { v, a, _ in AnyView(v.menuStyle(KotlinMenuStyle(body: a.slotModel(0)))) }
        #endif
        ComposeRegistry.modifiers[100_083] = { v, a, _ in AnyView(v.formStyle(KotlinFormStyle(body: a.slotModel(0)))) }
        ComposeRegistry.modifiers[100_085] = { v, a, _ in AnyView(v.progressViewStyle(KotlinProgressViewStyle(body: a.slotModel(0)))) }
        ComposeRegistry.modifiers[100_086] = { v, a, _ in
            let ids = a.list(1).map { AnyHashable(KHashable($0 as? NSObject ?? NSNull())) }
            let labels = a.list(2).map { $0 as? String ?? "" }
            return AnyView(v.accessibilityRotor(Text(a.string(0))) {
                ForEach(Array(zip(ids.indices, ids)), id: \.0) { i, id in
                    AccessibilityRotorEntry(Text(i < labels.count ? labels[i] : ""), id: id)
                }
            })
        }
        ComposeRegistry.modifiers[100_087] = { v, a, _ in
            AnyView(v.modifier(RotorEntryModifier(id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)))))
        }
        ComposeRegistry.modifiers[100_088] = { v, a, _ in
            let report = a.callback(0, box: { (d: [Double]) -> Any? in d.map { KotlinDouble(value: $0) } as NSArray })
            return AnyView(v.accessibilityZoomAction { action in
                report([action.direction == .zoomIn ? 1 : -1, Double(action.point.x), Double(action.point.y)])
            })
        }
        ComposeRegistry.modifiers[100_089] = { v, a, _ in
            AnyView(v.accessibilityChartDescriptor(KotlinChart(
                title: a.string(0), xTitle: a.string(1), yTitle: a.string(2),
                categories: a.list(3).map { $0 as? String ?? "" },
                values: a.list(4).compactMap { ($0 as? NSNumber)?.doubleValue }, continuous: a.bool(5))))
        }
        ComposeRegistry.modifiers[100_090] = { v, a, _ in AnyView(v.dialogIcon(Image(systemName: a.string(0)))) }
        ComposeRegistry.modifiers[100_091] = { v, _, m in AnyView(v.layoutValue(key: KotlinLayoutValuesKey.self, value: layoutValues(m))) }
        #if os(macOS)
        ComposeRegistry.modifiers[100_092] = { v, a, _ in AnyView(v.copyable(a.list(0).map { $0 as? String ?? "" })) }
        ComposeRegistry.modifiers[100_093] = { v, a, _ in
            guard let provider = a.raw(0) as? TextItemsProvider else { return v }
            return AnyView(v.cuttable(for: String.self) { provider.items() })
        }
        ComposeRegistry.modifiers[100_094] = { v, a, _ in
            let report = a.callback(0, box: { (items: [String]) -> Any? in items as NSArray })
            return AnyView(v.pasteDestination(for: String.self) { report($0) })
        }
        ComposeRegistry.modifiers[100_095] = { v, a, _ in
            guard let provider = a.raw(0) as? TextItemsProvider else { return v }
            return AnyView(v.onCopyCommand { provider.items().map { NSItemProvider(object: $0 as NSString) } })
        }
        ComposeRegistry.modifiers[100_096] = { v, a, _ in
            guard let provider = a.raw(0) as? TextItemsProvider else { return v }
            return AnyView(v.onCutCommand { provider.items().map { NSItemProvider(object: $0 as NSString) } })
        }
        ComposeRegistry.modifiers[100_097] = { v, a, _ in
            let types = a.list(0).compactMap { ($0 as? String).flatMap { UTType($0) } }
            let report = a.callback(1, box: { (items: [String]) -> Any? in items as NSArray })
            return AnyView(v.onPasteCommand(of: types) { providers in loadTexts(providers) { report($0) } })
        }
        ComposeRegistry.modifiers[100_098] = { v, a, _ in
            guard let provider = a.raw(0) as? TextItemsProvider else { return v }
            return AnyView(v.exportableToServices(provider.items()))
        }
        ComposeRegistry.modifiers[100_099] = { v, a, _ in
            guard let handler = a.raw(0) as? DropHandler else { return v }
            return AnyView(v.importableFromServices(for: String.self) { handler.onDrop(items: $0, x: 0, y: 0) })
        }
        ComposeRegistry.modifiers[100_100] = { v, a, _ in
            guard #available(macOS 15.0, *) else { return v }
            let report = a.callback(1, box: { (keys: [Int]) -> Any? in keys.map { KotlinInt(value: Int32($0)) } as NSArray })
            return AnyView(v.onModifierKeysChanged(mask: .all, initial: a.bool(0)) { old, new in report([old.rawValue, new.rawValue]) })
        }
        #endif
        #if os(iOS)
        ComposeRegistry.modifiers[100_101] = { v, a, _ in
            guard #available(iOS 17.5, *) else { return v }
            let report = a.callback(0, box: { (p: CGPoint?) -> Any? in p.flatMap { e_CGPoint($0) } ?? NSNull() })
            return AnyView(v.onPencilDoubleTap { value in report(value.hoverPose?.location) })
        }
        #endif
    }
}

extension CompletionRenderers {
    static func registerNewest() {
        ComposeRegistry.modifiers[100_102] = { v, a, _ in
            guard #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) else { return v }
            return AnyView(v.lineHeight(a.isNil(0) ? nil : lineHeight(a.int(0), a.double(1))))
        }
        ComposeRegistry.modifiers[100_103] = { v, a, _ in
            guard #available(iOS 27.1, macOS 27.1, tvOS 27.1, watchOS 27.1, *) else { return v }
            return AnyView(v.arrangementViewStyle(KotlinArrangementStyle(body: a.slotModel(0))))
        }
        ComposeRegistry.modifiers[100_104] = { v, a, _ in
            guard #available(iOS 27.1, macOS 27.1, tvOS 27.1, watchOS 27.1, *) else { return v }
            let report = a.callback(1, box: { (d: [Double?]) -> Any? in d.map { $0.map { KotlinDouble(value: $0) } ?? NSNull() } as NSArray })
            return AnyView(v.onHingeChange(isEnabled: a.bool(0)) { old, new in report([old.hinge?.angle.degrees, new.hinge?.angle.degrees]) })
        }
        #if os(iOS) || os(macOS) || os(watchOS)
        ComposeRegistry.modifiers[100_105] = { v, a, _ in
            guard #available(iOS 27.0, macOS 27.0, watchOS 27.0, *) else { return v }
            let report = a.callback(1, box: { (v: NSArray) -> Any? in v })
            return AnyView(v.reorderContainer(for: KotlinItem.self, isEnabled: a.bool(0)) { difference in
                let before: Any
                switch difference.destination.position {
                case .before(let id): before = kotlinKey(id)
                case .end: before = NSNull()
                }
                report([difference.sources.map(kotlinKey) as NSArray, before] as NSArray)
            })
        }
        #endif
        #if os(iOS) || os(macOS)
        ComposeRegistry.modifiers[100_106] = { v, a, _ in
            guard #available(iOS 27.0, macOS 26.0, *), let provider = a.raw(0) as? TextForIdProvider else { return v }
            return AnyView(v.dragContainer(for: KotlinItem.self) { (id: AnyHashable) in
                [KotlinItem(id: id, text: provider.text(id: kotlinKey(id)))]
            })
        }
        ComposeRegistry.modifiers[100_107] = { v, a, _ in
            guard #available(iOS 27.0, macOS 26.0, *) else { return v }
            let ids = a.list(0).map { AnyHashable(KHashable($0 as? NSObject ?? NSNull())) }
            return AnyView(v.dragContainerSelection(ids))
        }
        ComposeRegistry.modifiers[100_108] = { v, a, _ in
            guard #available(iOS 27.0, macOS 26.0, *) else { return v }
            let report = a.callback(0, box: { (d: [Double]) -> Any? in d.map { KotlinDouble(value: $0) } as NSArray })
            return AnyView(v.onDragSessionUpdated { s in
                report([Double(dragPhase(s.phase)), Double(s.location.x), Double(s.location.y), Double(s.draggedItemIndex)])
            })
        }
        ComposeRegistry.modifiers[100_109] = { v, a, _ in
            guard #available(iOS 27.0, macOS 26.0, *) else { return v }
            let report = a.callback(0, box: { (d: [Double]) -> Any? in d.map { KotlinDouble(value: $0) } as NSArray })
            return AnyView(v.onDropSessionUpdated { s in
                report([Double(dragPhase(s.phase)), Double(s.location.x), Double(s.location.y), Double(s.itemsCount)])
            })
        }
        ComposeRegistry.modifiers[100_110] = { v, a, _ in
            guard #available(iOS 27.0, macOS 26.0, *), let provider = a.raw(0) as? DropOperationProvider else { return v }
            return AnyView(v.dropConfiguration { s in
                DropConfiguration(operation: d_DropOperation(provider.operation(x: Double(s.location.x), y: Double(s.location.y),
                                                                                itemsCount: Int32(s.itemsCount)) as? SExpr) ?? .copy)
            })
        }
        #endif
        #if !os(tvOS)
        ComposeRegistry.views[100_058] = { m in
            let a = m.args
            let report = a.callback(2, box: KBox.double)
            let value = Binding<Double>(get: { m.args.double(1) }, set: { report($0) })
            let bounds = a.doubleRange(3)
            if #available(iOS 26.0, macOS 26.0, watchOS 26.0, *) {
                let ticks = a.list(4).compactMap { ($0 as? NSNumber)?.doubleValue }
                return ModChain.apply(Slider(value: value, in: bounds, neutralValue: a.isNil(5) ? nil : a.double(5),
                                             label: { Text(a.string(0)) },
                                             ticks: { SliderTickContentForEach(ticks, id: \.self) { SliderTick($0) } }), m, 0)
            }
            return ModChain.apply(Slider(value: value, in: bounds) { Text(a.string(0)) }, m, 0)
        }
        #endif
    }
}

// MARK: - Shape operations

/// Kotlin `Shape.offset`, `rotation`, `union`…: the result keeps its concrete type, so an inset shape stays
/// insettable.
@MainActor private func shapeOperation(_ a: Args) -> Any? {
    let op = a.int(0)
    if op == 12 {
        guard let base = dp_InsettableShape(a.expr(1)) else { return nil }
        return base.inset(by: a.cgFloat(2))
    }
    guard let base = dp_Shape(a.expr(1)) else { return nil }
    return shapeOperation(op, base, a)
}

@MainActor private func shapeOperation<S: Shape>(_ op: Int, _ s: S, _ a: Args) -> Any? {
    switch op {
    case 0: return s.offset(x: a.cgFloat(2), y: a.cgFloat(3))
    case 1: return s.rotation(d_Angle(a.expr(2)) ?? .zero, anchor: d_UnitPoint(a.expr(3)) ?? .center)
    case 2: return s.scale(x: a.cgFloat(2), y: a.cgFloat(3), anchor: d_UnitPoint(a.expr(4)) ?? .center)
    case 3: return s.trim(from: a.cgFloat(2), to: a.cgFloat(3))
    case 4: return s.transform(d_CGAffineTransform(a.expr(2)) ?? .identity)
    case 5:
        if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            return s.size(width: a.cgFloat(2), height: a.cgFloat(3), anchor: d_UnitPoint(a.expr(4)) ?? .topLeading)
        }
        return s.size(width: a.cgFloat(2), height: a.cgFloat(3))
    case 6...11:
        guard let other = dp_Shape(a.expr(2)) else { return nil }
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return s }
        let eo = a.bool(3)
        switch op {
        case 6: return s.union(other, eoFill: eo)
        case 7: return s.intersection(other, eoFill: eo)
        case 8: return s.subtracting(other, eoFill: eo)
        case 9: return s.symmetricDifference(other, eoFill: eo)
        case 10: return s.lineIntersection(other, eoFill: eo)
        default: return s.lineSubtraction(other, eoFill: eo)
        }
    case 13: return s.stroke(lineWidth: a.cgFloat(2))
    case 14: return s.stroke(style: d_StrokeStyle(a.expr(2)) ?? StrokeStyle())
    default: return nil
    }
}

/// Kotlin `Text(value, NumberFormat)`: [kind, minFraction, maxFraction, grouping, currency code].
@MainActor private func formattedNumber(_ a: Args) -> Text {
    let f = a.list(1)
    func int(_ i: Int) -> Int? { i < f.count ? (f[i] as? NSNumber)?.intValue : nil }
    let precision: NumberFormatStyleConfiguration.Precision? = switch (int(1), int(2)) {
    case let (lo?, hi?): .fractionLength(lo...max(lo, hi))
    case let (lo?, nil): .fractionLength(lo...)
    case let (nil, hi?): .fractionLength(...hi)
    default: nil
    }
    let value = a.double(0)
    switch int(0) ?? 0 {
    case 1:
        let style = FloatingPointFormatStyle<Double>.Percent()
        return Text(value, format: precision.map { style.precision($0) } ?? style)
    case 2:
        let style = FloatingPointFormatStyle<Double>.Currency(code: f.count > 4 ? f[4] as? String ?? "USD" : "USD")
        return Text(value, format: precision.map { style.precision($0) } ?? style)
    default:
        var style = FloatingPointFormatStyle<Double>.number
        if let precision { style = style.precision(precision) }
        if (f.count > 3 ? (f[3] as? NSNumber)?.boolValue : true) == false { style = style.grouping(.never) }
        return Text(value, format: style)
    }
}
