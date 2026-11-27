import SwiftUI
import UniformTypeIdentifiers
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

// Renderers of Interactive.kt: views whose content depends on values only SwiftUI has (load phases, scroll proxies,
// timeline dates, key presses, drawing contexts, table rows, style states). View kinds and modifier ids 100_020….

// MARK: - Environment carrying SwiftUI-only values to Kotlin placeholder views

private struct LoadedImageKey: EnvironmentKey {
    static let defaultValue: Image? = nil
}

private struct StyleLabelKey: EnvironmentKey {
    static let defaultValue: AnyView? = nil
}

private struct StyleIconKey: EnvironmentKey {
    static let defaultValue: AnyView? = nil
}

extension EnvironmentValues {
    var composeLoadedImage: Image? {
        get { self[LoadedImageKey.self] }
        set { self[LoadedImageKey.self] = newValue }
    }
    var composeStyleLabel: AnyView? {
        get { self[StyleLabelKey.self] }
        set { self[StyleLabelKey.self] = newValue }
    }
    var composeStyleIcon: AnyView? {
        get { self[StyleIconKey.self] }
        set { self[StyleIconKey.self] = newValue }
    }
}

// MARK: - AsyncImage

private struct AsyncPhaseView: View {
    let m: NodeModel
    let phase: AsyncImagePhase
    let report: (Int) -> Void

    private var index: Int {
        switch phase {
        case .success: return 1
        case .failure: return 2
        default: return 0
        }
    }

    var body: some View {
        ChildrenView(m)
            .environment(\.composeLoadedImage, phase.image)
            .onChange(of: index, initial: true) { _, i in report(i) }
    }
}

private struct LoadedImageView: View {
    let m: NodeModel
    @Environment(\.composeLoadedImage) private var image

    var body: some View {
        if let image {
            ModChain.apply(sm_Image(image, m), m, m.consumedPrefix)
        }
    }
}

private struct StylePartView: View {
    let m: NodeModel
    let icon: Bool
    @Environment(\.composeStyleLabel) private var label
    @Environment(\.composeStyleIcon) private var iconView

    var body: some View {
        if let part = icon ? iconView : label {
            ModChain.apply(part, m, 0)
        }
    }
}

// MARK: - ScrollViewReader

@MainActor final class ScrollProxyBox: NSObject, @preconcurrency ScrollProxyHandle {
    let proxy: ScrollViewProxy
    init(_ proxy: ScrollViewProxy) { self.proxy = proxy }

    func scrollTo(id: Any, anchor: Any?) {
        // same wrapping as `Modifier.id(_:)` (Args.hashable)
        proxy.scrollTo(AnyHashable(KHashable(id as? NSObject ?? NSNull())), anchor: d_UnitPoint(anchor as? SExpr))
    }
}

// MARK: - TimelineView

private struct TimelineDateView: View {
    let m: NodeModel
    let date: Date
    let report: (Double) -> Void

    var body: some View {
        ChildrenView(m).onChange(of: date, initial: true) { _, d in report(d.timeIntervalSince1970) }
    }
}

@MainActor private func timelineView<S: TimelineSchedule>(_ schedule: S, _ m: NodeModel, _ report: @escaping (Double) -> Void) -> AnyView {
    AnyView(TimelineView(schedule) { context in TimelineDateView(m: m, date: context.date, report: report) })
}

// MARK: - Canvas

@MainActor private enum CanvasDrawing {
    static func path(_ raw: Any?) -> Path {
        let c = (raw as? [Any] ?? []).compactMap { ($0 as? NSNumber)?.doubleValue }
        var p = Path()
        var i = 0
        while i < c.count {
            switch Int(c[i]) {
            case 0 where i + 2 < c.count: p.move(to: CGPoint(x: c[i + 1], y: c[i + 2])); i += 3
            case 1 where i + 2 < c.count: p.addLine(to: CGPoint(x: c[i + 1], y: c[i + 2])); i += 3
            case 2 where i + 4 < c.count:
                p.addQuadCurve(to: CGPoint(x: c[i + 3], y: c[i + 4]), control: CGPoint(x: c[i + 1], y: c[i + 2])); i += 5
            case 3 where i + 6 < c.count:
                p.addCurve(to: CGPoint(x: c[i + 5], y: c[i + 6]), control1: CGPoint(x: c[i + 1], y: c[i + 2]),
                           control2: CGPoint(x: c[i + 3], y: c[i + 4])); i += 7
            case 4: p.closeSubpath(); i += 1
            case 5 where i + 4 < c.count: p.addEllipse(in: CGRect(x: c[i + 1], y: c[i + 2], width: c[i + 3], height: c[i + 4])); i += 5
            case 6 where i + 4 < c.count: p.addRect(CGRect(x: c[i + 1], y: c[i + 2], width: c[i + 3], height: c[i + 4])); i += 5
            // Canvas coordinates are points, so the radii are too (PathShape scales them to its frame instead).
            case 7 where i + 6 < c.count:
                p.addArc(center: CGPoint(x: c[i + 1], y: c[i + 2]), radius: c[i + 3], startAngle: .degrees(c[i + 4]),
                         endAngle: .degrees(c[i + 5]), clockwise: c[i + 6] != 0)
                i += 7
            case 8 where i + 5 < c.count:
                p.addRelativeArc(center: CGPoint(x: c[i + 1], y: c[i + 2]), radius: c[i + 3], startAngle: .degrees(c[i + 4]), delta: .degrees(c[i + 5]))
                i += 6
            case 9 where i + 5 < c.count:
                p.addArc(tangent1End: CGPoint(x: c[i + 1], y: c[i + 2]), tangent2End: CGPoint(x: c[i + 3], y: c[i + 4]), radius: c[i + 5])
                i += 6
            case 10 where i + 9 < c.count:
                let radii = RectangleCornerRadii(topLeading: c[i + 5], bottomLeading: c[i + 6], bottomTrailing: c[i + 7], topTrailing: c[i + 8])
                p.addRoundedRect(in: CGRect(x: c[i + 1], y: c[i + 2], width: c[i + 3], height: c[i + 4]), cornerRadii: radii,
                                 style: c[i + 9] == 0 ? .circular : .continuous)
                i += 10
            default: i = c.count
            }
        }
        return p
    }

    static func shading(_ raw: Any?) -> GraphicsContext.Shading {
        guard let style = dp_ShapeStyle(raw as? SExpr) else { return .color(.primary) }
        return .style(style)
    }

    static func number(_ raw: Any?) -> CGFloat { CGFloat((raw as? NSNumber)?.doubleValue ?? 0) }

    static func text(_ s: String, font: Any?, color: Any?) -> Text {
        var t = Text(verbatim: s)
        if let f = d_Font(font as? SExpr) { t = t.font(f) }
        if let c = d_Color(color as? SExpr) { t = t.foregroundColor(c) }
        return t
    }

    static func draw(_ commands: [Any], in context: inout GraphicsContext) {
        var stack: [GraphicsContext] = []
        for raw in commands {
            guard let c = raw as? [Any], let op = (c.first as? NSNumber)?.intValue else { continue }
            func arg(_ i: Int) -> Any? { i < c.count ? c[i] : nil }
            switch op {
            case 1: context.fill(path(arg(1)), with: shading(arg(2)))
            case 2: context.stroke(path(arg(1)), with: shading(arg(2)), lineWidth: number(arg(3)))
            case 3:
                context.draw(text(arg(1) as? String ?? "", font: arg(4), color: arg(5)),
                             at: CGPoint(x: number(arg(2)), y: number(arg(3))), anchor: d_UnitPoint(arg(6) as? SExpr) ?? .center)
            case 11:
                var image = Text(Image(systemName: arg(1) as? String ?? ""))
                if let f = d_Font(arg(4) as? SExpr) { image = image.font(f) }
                if let col = d_Color(arg(5) as? SExpr) { image = image.foregroundColor(col) }
                context.draw(image, at: CGPoint(x: number(arg(2)), y: number(arg(3))))
            case 4: context.translateBy(x: number(arg(1)), y: number(arg(2)))
            case 5: context.scaleBy(x: number(arg(1)), y: number(arg(2)))
            case 6: context.rotate(by: .degrees(Double(number(arg(1)))))
            case 7: context.opacity *= Double(number(arg(1)))
            case 8: stack.append(context)
            case 9: if let saved = stack.popLast() { context = saved }
            case 10: context.clip(to: path(arg(1)))
            default: break
            }
        }
    }
}

// MARK: - Table

private struct TableRowRef: Identifiable {
    let model: NodeModel
    var id: Int64 { model.nodeId }
    /// Cells, without the group of child rows (Kotlin `Table(children:)`).
    var cells: [NodeModel] { model.children.filter { $0.kind != 100_061 } }
    var children: [TableRowRef]? {
        model.children.first { $0.kind == 100_061 }.map { $0.children.map(TableRowRef.init) }
    }
}

#if os(iOS) || os(macOS)
private struct KotlinTable: View {
    let m: NodeModel
    @State private var customization = TableColumnCustomization<TableRowRef>()

    var body: some View {
        let _ = m.version
        let a = m.args
        let rows = m.children.map(TableRowRef.init)
        if #available(iOS 17.4, macOS 14.4, *) {
            let selectable = !a.isNil(3)
            let onSelect = a.callback(3, box: { (keys: [AnyHashable]) -> Any? in
                keys.map { ($0.base as? KHashable)?.object ?? NSNull() } as NSArray
            })
            let selectedKeys = Set(a.list(2).map { AnyHashable(KHashable($0 as? NSObject ?? NSNull())) })
            let all = { () -> [TableRowRef] in
                var out: [TableRowRef] = []
                func walk(_ r: [TableRowRef]) { for x in r { out.append(x); walk(x.children ?? []) } }
                walk(rows)
                return out
            }
            let selection = Binding<Set<Int64>>(
                get: { Set(all().filter { selectedKeys.contains($0.model.args.hashable(0)) }.map(\.id)) },
                set: { ids in onSelect(all().filter { ids.contains($0.id) }.map { $0.model.args.hashable(0) }) }
            )
            let bound = selectable ? selection : .constant([])
            if a.bool(4) {
                Table(rows, children: \.children, selection: bound, columnCustomization: $customization) { columns(a) }
            } else {
                Table(rows, selection: bound, columnCustomization: $customization) { columns(a) }
            }
        } else {
            List(rows) { row in
                HStack { ForEach(row.cells, id: \.nodeId) { NodeView(model: $0) } }
            }
        }
    }

    @available(iOS 17.4, macOS 14.4, *)
    @TableColumnBuilder<TableRowRef, Never>
    private func columns(_ a: Args) -> some TableColumnContent<TableRowRef, Never> {
        let titles = a.list(0).map { $0 as? String ?? "" }
        let widths = a.list(1).map { w -> [CGFloat?] in
            ((w as? [Any]) ?? []).map { ($0 as? NSNumber).map { CGFloat($0.doubleValue) } }
        }
        let alignments = a.list(5).map { d_TableColumnAlignment($0 as? SExpr) }
        let customizable = a.bool(6)
        TableColumnForEach(Array(titles.indices), id: \.self) { c in
            let w = c < widths.count ? widths[c] : []
            TableColumn(titles[c]) { (row: TableRowRef) in
                let cells = row.cells
                if c < cells.count { NodeView(model: cells[c]) }
            }
            .width(min: w.count > 0 ? w[0] : nil, ideal: w.count > 1 ? w[1] : nil, max: w.count > 2 ? w[2] : nil)
            .alignment((c < alignments.count ? alignments[c] : nil) ?? .automatic)
            .customizationID(titles[c])
            .disabledCustomizationBehavior(customizable ? [] : .all)
        }
    }
}
#endif

// MARK: - Custom styles

private struct KotlinButtonStyle: ButtonStyle {
    let normal: NodeModel?
    let pressed: NodeModel?

    func makeBody(configuration: Configuration) -> some View {
        SlotView(model: configuration.isPressed ? pressed : normal)
            .environment(\.composeStyleLabel, AnyView(configuration.label))
    }
}

private struct KotlinToggleStyle: ToggleStyle {
    let off: NodeModel?
    let on: NodeModel?

    func makeBody(configuration: Configuration) -> some View {
        SlotView(model: configuration.isOn ? on : off)
            .environment(\.composeStyleLabel, AnyView(configuration.label))
            .contentShape(Rectangle())
            .onTapGesture { configuration.isOn.toggle() }
    }
}

private struct KotlinLabelStyle: LabelStyle {
    let body: NodeModel?

    func makeBody(configuration: Configuration) -> some View {
        SlotView(model: body)
            .environment(\.composeStyleLabel, AnyView(configuration.title))
            .environment(\.composeStyleIcon, AnyView(configuration.icon))
    }
}

// MARK: - Effects

private func applyEffects<E: VisualEffect>(_ effect: E, _ v: [Double]) -> some VisualEffect {
    func at(_ i: Int, _ d: Double) -> Double { i < v.count ? v[i] : d }
    return effect
        .opacity(at(0, 1))
        .scaleEffect(at(1, 1))
        .offset(x: at(2, 0), y: at(3, 0))
        .rotationEffect(.degrees(at(4, 0)))
        .blur(radius: at(5, 0))
}

private func effectValues(_ list: [NSNumber]) -> [Double] { list.map(\.doubleValue) }

// MARK: - Registration

@MainActor enum InteractiveRenderers {
    static func register() {
        ComposeRegistry.views[100_020] = { m in
            let a = m.args
            let report = a.callback(3, box: KBox.int)
            let transaction = a.isNil(2) ? Transaction() : Transaction(animation: d_Animation(a.expr(2)))
            return ModChain.apply(AsyncImage(url: a.stringOpt(0).flatMap { URL(string: $0) }, scale: a.cgFloat(1), transaction: transaction) {
                AsyncPhaseView(m: m, phase: $0, report: report)
            }, m, 0)
        }
        ComposeRegistry.views[100_021] = { m in AnyView(LoadedImageView(m: m)) }
        ComposeRegistry.views[100_022] = { m in
            let report = m.args.callback(0, box: { (box: ScrollProxyBox) -> Any? in box })
            return ModChain.apply(ScrollViewReader { proxy in
                ChildrenView(m).onAppear { report(ScrollProxyBox(proxy)) }
            }, m, 0)
        }
        ComposeRegistry.views[100_023] = { m in
            let a = m.args
            guard let schedule = dp_TimelineSchedule(a.expr(0)) else { return AnyView(ChildrenView(m)) }
            return ModChain.apply(timelineView(schedule, m, a.callback(1, box: KBox.double)), m, 0)
        }
        ComposeRegistry.views[100_024] = { m in
            let a = m.args
            let renderer = a.raw(0) as? CanvasRenderer
            return ModChain.apply(Canvas(opaque: a.bool(1)) { context, size in
                let commands = renderer?.draw(width: Double(size.width), height: Double(size.height)) ?? []
                CanvasDrawing.draw(commands.map { $0 as Any }, in: &context)
            }, m, 0)
        }
        #if os(iOS) || os(macOS)
        ComposeRegistry.views[100_025] = { m in ModChain.apply(KotlinTable(m: m), m, 0) }
        #else
        ComposeRegistry.views[100_025] = { m in
            ModChain.apply(List(m.children.map(TableRowRef.init)) { row in
                HStack { ForEach(row.cells, id: \.nodeId) { NodeView(model: $0) } }
            }, m, 0)
        }
        #endif
        ComposeRegistry.views[100_026] = { m in AnyView(ChildrenView(m)) }
        ComposeRegistry.views[100_027] = { m in ModChain.apply(ChildrenView(m), m, 0) }
        ComposeRegistry.views[100_028] = { m in AnyView(StylePartView(m: m, icon: false)) }
        ComposeRegistry.views[100_029] = { m in AnyView(StylePartView(m: m, icon: true)) }

        #if !os(watchOS)
        ComposeRegistry.modifiers[100_020] = { v, a, _ in
            guard let handler = a.raw(0) as? KeyPressHandler else { return v }
            let mask = a.int(1)
            var phases: KeyPress.Phases = []
            if mask & 1 != 0 { phases.insert(.down) }
            if mask & 2 != 0 { phases.insert(.repeat) }
            if mask & 4 != 0 { phases.insert(.up) }
            let keys = Set(a.list(2).compactMap { ($0 as? String)?.first.map { KeyEquivalent($0) } })
            let action = { (press: KeyPress) -> KeyPress.Result in
                let phase: Int32 = press.phase == .up ? 2 : press.phase == .repeat ? 1 : 0
                let handled = handler.onKeyPress(characters: press.characters, key: String(press.key.character),
                                                 modifiers: Int32(press.modifiers.rawValue), phase: phase)
                return handled ? .handled : .ignored
            }
            return keys.isEmpty ? AnyView(v.onKeyPress(phases: phases, action: action))
                : AnyView(v.onKeyPress(keys: keys, phases: phases, action: action))
        }
        #endif
        ComposeRegistry.modifiers[100_021] = { v, a, _ in
            guard let provider = a.raw(2) as? EffectsProvider else { return v }
            let configuration = d_ScrollTransitionConfiguration(a.expr(0)) ?? .interactive
            let transition = { (effect: EmptyVisualEffect, phase: ScrollTransitionPhase) in
                applyEffects(effect, effectValues(provider.effects(
                    phase: phase.value, isIdentity: phase.isIdentity, width: 0, height: 0, minX: 0, minY: 0)))
            }
            if let axis = d_Axis(a.expr(1)) {
                return AnyView(v.scrollTransition(configuration, axis: axis, transition: transition))
            }
            return AnyView(v.scrollTransition(configuration, transition: transition))
        }
        ComposeRegistry.modifiers[100_022] = { v, a, _ in
            guard let provider = a.raw(0) as? EffectsProvider else { return v }
            return AnyView(v.visualEffect { effect, geometry in
                let frame = geometry.frame(in: .global)
                return applyEffects(effect, effectValues(provider.effects(
                    phase: 0, isIdentity: true, width: Double(frame.width), height: Double(frame.height),
                    minX: Double(frame.minX), minY: Double(frame.minY))))
            })
        }
        ComposeRegistry.modifiers[100_023] = { v, a, _ in
            guard let guide = d_HorizontalAlignment(a.expr(0)), let provider = a.raw(1) as? AlignmentGuideProvider else { return v }
            return AnyView(v.alignmentGuide(guide) { d in CGFloat(provider.compute(width: Double(d.width), height: Double(d.height))) })
        }
        ComposeRegistry.modifiers[100_024] = { v, a, _ in
            guard let guide = d_VerticalAlignment(a.expr(0)), let provider = a.raw(1) as? AlignmentGuideProvider else { return v }
            return AnyView(v.alignmentGuide(guide) { d in CGFloat(provider.compute(width: Double(d.width), height: Double(d.height))) })
        }
        ComposeRegistry.modifiers[100_025] = { v, a, m in
            AnyView(v.buttonStyle(KotlinButtonStyle(normal: a.slotModel(0), pressed: a.slotModel(1))))
        }
        ComposeRegistry.modifiers[100_026] = { v, a, m in
            AnyView(v.toggleStyle(KotlinToggleStyle(off: a.slotModel(0), on: a.slotModel(1))))
        }
        ComposeRegistry.modifiers[100_027] = { v, a, m in
            AnyView(v.labelStyle(KotlinLabelStyle(body: a.slotModel(0))))
        }
        registerSelectionAndInput()
        registerEffectsAndFiles()
        registerLayoutAndAccessibility()
    }
}

// MARK: - Selection, search tokens, focus, number fields

private struct TokenRef: Identifiable {
    let id: Int
}

private func kotlinKeys(_ keys: [AnyHashable]) -> NSArray {
    keys.map { ($0.base as? KHashable)?.object ?? NSNull() } as NSArray
}

private func swiftKeys(_ list: [Any]) -> Set<AnyHashable> {
    Set(list.map { AnyHashable(KHashable($0 as? NSObject ?? NSNull())) })
}

private struct SearchFocusBridge: ViewModifier {
    let isFocused: Bool
    let onChange: (Bool) -> Void
    @FocusState private var focused: Bool

    func body(content: Content) -> some View {
        #if os(iOS) || os(macOS)
        if #available(iOS 18.0, macOS 15.0, *) {
            content
                .searchFocused($focused)
                .onAppear { focused = isFocused }
                .onChange(of: isFocused) { _, value in focused = value }
                .onChange(of: focused) { _, value in if value != isFocused { onChange(value) } }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

private struct AccessibilityFocusBridge: ViewModifier {
    let isFocused: Bool
    let onChange: (Bool) -> Void
    @AccessibilityFocusState private var focused: Bool

    func body(content: Content) -> some View {
        content
            .accessibilityFocused($focused)
            .onAppear { focused = isFocused }
            .onChange(of: isFocused) { _, value in focused = value }
            .onChange(of: focused) { _, value in if value != isFocused { onChange(value) } }
    }
}

/// Glass ids must be Sendable; Kotlin keys are only read on the main actor.
private struct GlassID: Hashable, @unchecked Sendable {
    let value: KHashable
}

private struct NamespacedModifier: ViewModifier {
    enum Kind { case glassID, glassUnion, focusScope, linkedGroup, labeledPair, defaultFocus }
    let kind: Kind
    let id: KHashable
    let namespace: Int64
    var role: AccessibilityLabeledPairRole = .label
    var flag = true
    @Environment(\.composeNamespaces) private var namespaces

    func body(content: Content) -> some View {
        if let ns = namespaces[namespace] {
            switch kind {
            case .glassID, .glassUnion:
                if #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) {
                    if kind == .glassID {
                        content.glassEffectID(GlassID(value: id), in: ns)
                    } else {
                        content.glassEffectUnion(id: GlassID(value: id), namespace: ns)
                    }
                } else {
                    content
                }
            case .focusScope:
                #if os(iOS)
                content
                #else
                content.focusScope(ns)
                #endif
            case .linkedGroup:
                content.accessibilityLinkedGroup(id: id, in: ns)
            case .labeledPair:
                content.accessibilityLabeledPair(role: role, id: id, in: ns)
            case .defaultFocus:
                #if os(iOS)
                content
                #else
                content.prefersDefaultFocus(flag, in: ns)
                #endif
            }
        } else {
            content
        }
    }
}

extension InteractiveRenderers {
    static func registerSelectionAndInput() {
        // List(selection:) with one selected row. SwiftUI's non-optional selection initializer (the generated one)
        // only exists on macOS: elsewhere the optional one is used, so tapping a tagged row selects it on iOS too.
        let singleSelection = { (m: NodeModel, value: Int, callback: Int) -> AnyView in
            let a = m.args
            let report = a.callback(callback, box: { (k: AnyHashable?) -> Any? in k.map { KBox.hashable($0) } ?? NSNull() })
            let selection = Binding<AnyHashable?>(get: { m.args.hashableOpt(value) }, set: { report($0) })
            #if os(watchOS) || os(tvOS)
            _ = selection
            return ModChain.apply(List { ChildrenView(m) }, m, 0)
            #else
            return ModChain.apply(List(selection: selection) { ChildrenView(m) }, m, 0)
            #endif
        }
        #if !os(macOS)
        ComposeRegistry.views[134] = { m in singleSelection(m, 0, 1) }
        #endif
        ComposeRegistry.views[100_063] = { m in singleSelection(m, 0, 1) }
        ComposeRegistry.views[100_030] = { m in
            let report = m.args.callback(1, box: { (keys: [AnyHashable]) -> Any? in kotlinKeys(keys) })
            let selection = Binding<Set<AnyHashable>>(get: { swiftKeys(m.args.list(0)) }, set: { report(Array($0)) })
            #if os(watchOS)
            _ = selection
            return ModChain.apply(List { ChildrenView(m) }, m, 0)
            #else
            return ModChain.apply(List(selection: selection) { ChildrenView(m) }, m, 0)
            #endif
        }
        ComposeRegistry.views[100_031] = { m in
            let a = m.args
            let report = a.callback(2, box: KBox.double)
            let value = Binding<Double>(get: { m.args.double(1) }, set: { report($0) })
            let title = a.string(0)
            let prompt = a.stringOpt(5).map { Text($0) }
            switch a.int(3) {
            case 1: return ModChain.apply(TextField(title, value: value, format: .percent, prompt: prompt), m, 0)
            case 2: return ModChain.apply(TextField(title, value: value, format: .currency(code: a.stringOpt(4) ?? "USD"), prompt: prompt), m, 0)
            default: return ModChain.apply(TextField(title, value: value, format: .number, prompt: prompt), m, 0)
            }
        }
        #if os(iOS)
        ComposeRegistry.views[100_034] = { m in
            let calendar = Calendar.current
            let report = m.args.callback(2, box: { (days: [[Int]]) -> Any? in
                days.map { $0.map { KotlinInt(value: Int32($0)) } as NSArray } as NSArray
            })
            let selection = Binding<Set<DateComponents>>(
                get: {
                    Set(m.args.list(1).compactMap { raw -> DateComponents? in
                        let c = (raw as? [Any] ?? []).compactMap { ($0 as? NSNumber)?.intValue }
                        guard c.count == 3, let date = calendar.date(from: DateComponents(year: c[0], month: c[1], day: c[2])) else { return nil }
                        return calendar.dateComponents([.calendar, .era, .year, .month, .day], from: date)
                    })
                },
                set: { set in
                    report(set.compactMap { d in d.year.flatMap { y in d.month.flatMap { mo in d.day.map { [y, mo, $0] } } } })
                }
            )
            return ModChain.apply(MultiDatePicker(m.args.string(0), selection: selection), m, 0)
        }
        #else
        ComposeRegistry.views[100_034] = { _ in AnyView(EmptyView()) }
        #endif
        #if os(iOS) || os(macOS)
        ComposeRegistry.views[100_032] = { m in
            let report = m.args.callback(0, box: { (items: [String]) -> Any? in items as NSArray })
            return ModChain.apply(PasteButton(payloadType: String.self) { items in
                Task { @MainActor in report(items) }
            }, m, 0)
        }
        ComposeRegistry.modifiers[100_028] = { v, a, _ in
            let count = a.int(2)
            let report = a.callback(3, box: { (ids: [Int]) -> Any? in ids.map { KotlinInt(value: Int32($0)) } as NSArray })
            let tokens = Binding<[TokenRef]>(get: { (0..<count).map(TokenRef.init) }, set: { report($0.map(\.id)) })
            let slot = a.slotModel(5)
            return AnyView(v.searchable(text: a.stringBinding(0), tokens: tokens, prompt: a.stringOpt(4).map { Text($0) }) { token in
                if let slot, token.id < slot.children.count { NodeView(model: slot.children[token.id]) }
            })
        }
        #else
        ComposeRegistry.views[100_032] = { _ in AnyView(EmptyView()) }
        #endif
        #if os(iOS)
        ComposeRegistry.views[100_015] = { m in
            let image = Image(uiImage: m.args.raw(0) as? UIImage ?? UIImage())
            return ModChain.apply(sm_Image(image, m), m, m.consumedPrefix)
        }
        #elseif os(macOS)
        ComposeRegistry.views[100_016] = { m in
            let image = Image(nsImage: m.args.raw(0) as? NSImage ?? NSImage())
            return ModChain.apply(sm_Image(image, m), m, m.consumedPrefix)
        }
        #endif
        ComposeRegistry.modifiers[100_029] = { v, a, _ in
            AnyView(v.modifier(SearchFocusBridge(isFocused: a.bool(0), onChange: a.callback(1, box: KBox.bool))))
        }
        ComposeRegistry.modifiers[100_030] = { v, a, _ in
            AnyView(v.modifier(AccessibilityFocusBridge(isFocused: a.bool(0), onChange: a.callback(1, box: KBox.bool))))
        }
        ComposeRegistry.modifiers[100_031] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .glassID, id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)))))
        }
        ComposeRegistry.modifiers[100_032] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .glassUnion, id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)))))
        }
        ComposeRegistry.modifiers[100_033] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .focusScope, id: KHashable(NSNull()), namespace: Int64(a.int(0)))))
        }
        #if os(iOS) || os(tvOS)
        ComposeRegistry.modifiers[100_043] = { v, a, _ in
            AnyView(v.keyboardType(UIKeyboardType(rawValue: a.int(0)) ?? .default))
        }
        ComposeRegistry.modifiers[100_044] = { v, a, _ in
            let types: [UITextContentType] = [.username, .password, .newPassword, .oneTimeCode, .emailAddress, .telephoneNumber,
                                              .name, .givenName, .familyName, .fullStreetAddress, .postalCode, .addressCity, .URL,
                                              .creditCardNumber]
            let i = a.int(0)
            return i < types.count ? AnyView(v.textContentType(types[i])) : v
        }
        #elseif os(macOS)
        ComposeRegistry.modifiers[100_044] = { v, a, _ in
            switch a.int(0) {
            case 0: return AnyView(v.textContentType(.username))
            case 1, 2: return AnyView(v.textContentType(.password))
            case 3: return AnyView(v.textContentType(.oneTimeCode))
            default: return v
            }
        }
        #endif
    }
}

// MARK: - Geometry, files, drag and drop, animations, symbols

#if os(iOS) || os(macOS)
private struct KotlinTextDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText, .utf8PlainText, .text, .json, .commaSeparatedText, .xml, .html, .data] }
    let text: String

    init(text: String) { self.text = text }

    init(configuration: ReadConfiguration) throws {
        text = String(decoding: configuration.file.regularFileContents ?? Data(), as: UTF8.self)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
#endif

func applyViewEffects(_ content: some View, _ v: [Double]) -> some View {
    func at(_ i: Int, _ d: Double) -> Double { i < v.count ? v[i] : d }
    return content
        .opacity(at(0, 1))
        .scaleEffect(at(1, 1))
        .offset(x: at(2, 0), y: at(3, 0))
        .rotationEffect(.degrees(at(4, 0)))
        .blur(radius: at(5, 0))
}

@MainActor private func decodeSymbolEffectOptions(speed: Double?, repeatCount: Int?, repeating: Bool?) -> SymbolEffectOptions {
    var options = SymbolEffectOptions.default
    if let repeating {
        options = repeating ? options.repeating : options.nonRepeating
    }
    if let repeatCount {
        options = options.repeat(repeatCount)
    }
    if let speed {
        options = options.speed(speed)
    }
    return options
}

@MainActor private func makeBounceEffect(_ variants: [String]) -> BounceSymbolEffect {
    var e = BounceSymbolEffect.bounce
    for m in variants {
        switch m {
        case "up": e = e.up
        case "down": e = e.down
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@MainActor private func makePulseEffect(_ variants: [String]) -> PulseSymbolEffect {
    var e = PulseSymbolEffect.pulse
    for m in variants {
        switch m {
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@MainActor private func makeVariableColorEffect(_ variants: [String]) -> VariableColorSymbolEffect {
    var e = VariableColorSymbolEffect.variableColor
    for m in variants {
        switch m {
        case "iterative": e = e.iterative
        case "cumulative": e = e.cumulative
        case "reversing": e = e.reversing
        case "nonReversing": e = e.nonReversing
        case "hideInactiveLayers": e = e.hideInactiveLayers
        case "dimInactiveLayers": e = e.dimInactiveLayers
        default: break
        }
    }
    return e
}

@MainActor private func makeScaleEffect(_ variants: [String]) -> ScaleSymbolEffect {
    var e = ScaleSymbolEffect.scale
    for m in variants {
        switch m {
        case "up": e = e.up
        case "down": e = e.down
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@MainActor private func makeAppearEffect(_ variants: [String]) -> AppearSymbolEffect {
    var e = AppearSymbolEffect.appear
    for m in variants {
        switch m {
        case "up": e = e.up
        case "down": e = e.down
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@MainActor private func makeDisappearEffect(_ variants: [String]) -> DisappearSymbolEffect {
    var e = DisappearSymbolEffect.disappear
    for m in variants {
        switch m {
        case "up": e = e.up
        case "down": e = e.down
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
@MainActor private func makeWiggleEffect(_ variants: [String]) -> WiggleSymbolEffect {
    var e = WiggleSymbolEffect.wiggle
    for m in variants {
        switch m {
        case "left": e = e.left
        case "right": e = e.right
        case "up": e = e.up
        case "down": e = e.down
        case "forward": e = e.forward
        case "backward": e = e.backward
        case "clockwise": e = e.clockwise
        case "counterClockwise": e = e.counterClockwise
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
@MainActor private func makeRotateEffect(_ variants: [String]) -> RotateSymbolEffect {
    var e = RotateSymbolEffect.rotate
    for m in variants {
        switch m {
        case "clockwise": e = e.clockwise
        case "counterClockwise": e = e.counterClockwise
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
@MainActor private func makeBreatheEffect(_ variants: [String]) -> BreatheSymbolEffect {
    var e = BreatheSymbolEffect.breathe
    for m in variants {
        switch m {
        case "pulse": e = e.pulse
        case "plain": e = e.plain
        case "byLayer": e = e.byLayer
        case "wholeSymbol": e = e.wholeSymbol
        default: break
        }
    }
    return e
}

@MainActor private func indefiniteSymbolEffect(
    _ v: AnyView,
    _ kind: Int,
    _ variants: [String],
    _ options: SymbolEffectOptions,
    _ active: Bool
) -> AnyView {
    switch kind {
    case 1: return AnyView(v.symbolEffect(makePulseEffect(variants), options: options, isActive: active))
    case 2: return AnyView(v.symbolEffect(makeVariableColorEffect(variants), options: options, isActive: active))
    case 3: return AnyView(v.symbolEffect(makeScaleEffect(variants), options: options, isActive: active))
    case 4: return AnyView(v.symbolEffect(makeAppearEffect(variants), options: options, isActive: active))
    case 5: return AnyView(v.symbolEffect(makeDisappearEffect(variants), options: options, isActive: active))
    default: break
    }
    if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
        switch kind {
        case 0: return AnyView(v.symbolEffect(makeBounceEffect(variants), options: options, isActive: active))
        case 6: return AnyView(v.symbolEffect(makeWiggleEffect(variants), options: options, isActive: active))
        case 7: return AnyView(v.symbolEffect(makeRotateEffect(variants), options: options, isActive: active))
        case 8: return AnyView(v.symbolEffect(makeBreatheEffect(variants), options: options, isActive: active))
        default: break
        }
    }
    return v
}

@MainActor private func discreteSymbolEffect(
    _ v: AnyView,
    _ kind: Int,
    _ variants: [String],
    _ options: SymbolEffectOptions,
    _ value: AnyHashable
) -> AnyView {
    switch kind {
    case 0: return AnyView(v.symbolEffect(makeBounceEffect(variants), options: options, value: value))
    case 1: return AnyView(v.symbolEffect(makePulseEffect(variants), options: options, value: value))
    case 2: return AnyView(v.symbolEffect(makeVariableColorEffect(variants), options: options, value: value))
    default: break
    }
    if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
        switch kind {
        case 6: return AnyView(v.symbolEffect(makeWiggleEffect(variants), options: options, value: value))
        case 7: return AnyView(v.symbolEffect(makeRotateEffect(variants), options: options, value: value))
        case 8: return AnyView(v.symbolEffect(makeBreatheEffect(variants), options: options, value: value))
        default: break
        }
    }
    return v
}

extension InteractiveRenderers {
    static func registerEffectsAndFiles() {
        ComposeRegistry.views[100_033] = { m in
            let a = m.args
            if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
                let p = a.list(2).compactMap { ($0 as? NSNumber)?.floatValue }
                let points = stride(from: 0, to: p.count - 1, by: 2).map { SIMD2<Float>(p[$0], p[$0 + 1]) }
                let colors = a.exprList(3).compactMap { d_Color($0) }
                guard points.count == a.int(0) * a.int(1), colors.count == points.count else { return AnyView(EmptyView()) }
                return ModChain.apply(MeshGradient(width: a.int(0), height: a.int(1), points: points, colors: colors, smoothsColors: a.bool(4)), m, 0)
            }
            return AnyView(EmptyView())
        }
        ComposeRegistry.modifiers[100_034] = { v, a, _ in
            let report = a.callback(0, box: { (r: CGRect) -> Any? in e_CGRect(r) })
            return AnyView(v.onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { report($0) })
        }
        ComposeRegistry.modifiers[100_035] = { v, a, _ in
            guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) else { return v }
            let report = a.callback(0, box: { (d: [CGFloat]) -> Any? in d.map { KotlinDouble(value: Double($0)) } as NSArray })
            return AnyView(v.onScrollGeometryChange(for: [CGFloat].self) { g in
                [g.contentOffset.x, g.contentOffset.y, g.contentSize.width, g.contentSize.height, g.containerSize.width, g.containerSize.height]
            } action: { _, new in report(new) })
        }
        ComposeRegistry.modifiers[100_036] = { v, a, _ in
            guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) else { return v }
            let report = a.callback(1, box: { (ids: [AnyHashable]) -> Any? in kotlinKeys(ids) })
            return AnyView(v.onScrollTargetVisibilityChange(idType: AnyHashable.self, threshold: a.double(0)) { report($0) })
        }
        #if os(iOS) || os(macOS)
        ComposeRegistry.modifiers[100_037] = { v, a, _ in
            let types = a.list(2).compactMap { ($0 as? String).flatMap { UTType($0) } }
            let done = a.callback(4, box: { (urls: [String]) -> Any? in urls as NSArray })
            let fail = a.callback(5, box: KBox.string)
            return AnyView(v.fileImporter(isPresented: a.boolBinding(0), allowedContentTypes: types,
                                          allowsMultipleSelection: a.bool(3)) { result in
                switch result {
                case .success(let urls): done(urls.map(\.absoluteString))
                case .failure(let error): fail(error.localizedDescription)
                }
            })
        }
        ComposeRegistry.modifiers[100_038] = { v, a, _ in
            let done = a.callback(5, box: KBox.string)
            let fail = a.callback(6, box: KBox.string)
            return AnyView(v.fileExporter(isPresented: a.boolBinding(0), document: KotlinTextDocument(text: a.string(2)),
                                          contentType: UTType(a.string(3)) ?? .plainText, defaultFilename: a.stringOpt(4)) { result in
                switch result {
                case .success(let url): done(url.absoluteString)
                case .failure(let error): fail(error.localizedDescription)
                }
            })
        }
        ComposeRegistry.modifiers[100_039] = { v, a, _ in
            guard let handler = a.raw(0) as? DropHandler else { return v }
            let targeted = a.callback(1, box: KBox.bool)
            return AnyView(v.dropDestination(for: String.self) { items, location in
                handler.onDrop(items: items, x: Double(location.x), y: Double(location.y))
            } isTargeted: { targeted($0) })
        }
        ComposeRegistry.modifiers[100_040] = { v, a, _ in AnyView(v.draggable(a.string(0))) }
        #endif
        ComposeRegistry.modifiers[100_041] = { v, a, _ in
            let phases = a.list(0).map { ($0 as? [Any] ?? []).compactMap { ($0 as? NSNumber)?.doubleValue } }
            guard !phases.isEmpty else { return v }
            let animation = a.isNil(2) ? Animation.default : (d_Animation(a.expr(2)) ?? .default)
            if a.isNil(1) {
                return AnyView(v.phaseAnimator(Array(phases.indices)) { content, i in
                    applyViewEffects(content, phases[i])
                } animation: { _ in animation })
            }
            return AnyView(v.phaseAnimator(Array(phases.indices), trigger: a.hashable(1)) { content, i in
                applyViewEffects(content, phases[i])
            } animation: { _ in animation })
        }
        ComposeRegistry.modifiers[100_042] = { v, a, _ in
            a.bool(3)
                ? discreteSymbolEffect(v, a.int(0), [], .default, a.hashable(2))
                : indefiniteSymbolEffect(v, a.int(0), [], .default, a.bool(1))
        }
        ComposeRegistry.modifiers[100_118] = { v, a, _ in
            let variants = a.list(1).compactMap { $0 as? String }
            let speed: Double? = a.isNil(2) ? nil : a.double(2)
            let repeatCount: Int? = a.isNil(3) ? nil : a.int(3)
            let repeating: Bool? = a.isNil(4) ? nil : a.bool(4)
            let options = decodeSymbolEffectOptions(speed: speed, repeatCount: repeatCount, repeating: repeating)
            return a.bool(7)
                ? discreteSymbolEffect(v, a.int(0), variants, options, a.hashable(6))
                : indefiniteSymbolEffect(v, a.int(0), variants, options, a.bool(5))
        }
    }
}

// MARK: - 3D rotation, type size ranges, container-relative frames, detents, accessibility groups

@MainActor private func typeSizeRange(_ v: AnyView, _ min: DynamicTypeSize?, _ max: DynamicTypeSize?) -> AnyView {
    switch (min, max) {
    case let (lo?, hi?) where lo <= hi: return AnyView(v.dynamicTypeSize(lo...hi))
    case let (lo?, nil): return AnyView(v.dynamicTypeSize(lo...))
    case let (nil, hi?): return AnyView(v.dynamicTypeSize(...hi))
    default: return v
    }
}

extension InteractiveRenderers {
    static func registerLayoutAndAccessibility() {
        ComposeRegistry.modifiers[100_045] = { v, a, _ in
            AnyView(v.rotation3DEffect(d_Angle(a.expr(0)) ?? .zero, axis: (x: a.cgFloat(1), y: a.cgFloat(2), z: a.cgFloat(3)),
                                       anchor: d_UnitPoint(a.expr(4)) ?? .center, anchorZ: a.cgFloat(5), perspective: a.cgFloat(6)))
        }
        ComposeRegistry.modifiers[100_046] = { v, a, _ in
            typeSizeRange(v, d_DynamicTypeSize(a.expr(0)), d_DynamicTypeSize(a.expr(1)))
        }
        ComposeRegistry.modifiers[100_047] = { v, a, _ in
            guard let axes = d_Axis_Set(a.expr(0)), let provider = a.raw(2) as? LengthProvider else { return v }
            return AnyView(v.containerRelativeFrame(axes, alignment: d_Alignment(a.expr(1)) ?? .center) { length, axis in
                CGFloat(provider.length(length: Double(length), axis: axis == .horizontal ? 0 : 1))
            })
        }
        ComposeRegistry.modifiers[100_048] = { v, a, _ in
            let detents = a.exprList(0).compactMap { d_PresentationDetent($0) }
            guard !detents.isEmpty else { return v }
            let index = a.int(1)
            let report = a.callback(2, box: KBox.int)
            let selection = Binding<PresentationDetent>(
                get: { detents[index >= 0 && index < detents.count ? index : 0] },
                set: { new in if let i = detents.firstIndex(of: new) { report(i) } }
            )
            return AnyView(v.presentationDetents(Set(detents), selection: selection))
        }
        ComposeRegistry.modifiers[100_049] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .linkedGroup, id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)))))
        }
        ComposeRegistry.modifiers[100_050] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .labeledPair, id: KHashable(a.raw(1) as? NSObject ?? NSNull()),
                                                  namespace: Int64(a.int(2)), role: d_AccessibilityLabeledPairRole(a.expr(0)) ?? .label)))
        }
        ComposeRegistry.modifiers[100_051] = { v, a, _ in
            AnyView(v.modifier(NamespacedModifier(kind: .defaultFocus, id: KHashable(NSNull()), namespace: Int64(a.int(1)), flag: a.bool(0))))
        }
    }
}
