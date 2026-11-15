import SwiftUI

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
}

#if os(iOS) || os(macOS)
private struct KotlinTable: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let titles = a.list(0).map { $0 as? String ?? "" }
        let widths = a.list(1).map { w -> [CGFloat?] in
            ((w as? [Any]) ?? []).map { ($0 as? NSNumber).map { CGFloat($0.doubleValue) } }
        }
        let rows = m.children.map(TableRowRef.init)
        let selectable = !a.isNil(3)
        let onSelect = a.callback(3, box: { (keys: [AnyHashable]) -> Any? in
            keys.map { ($0.base as? KHashable)?.object ?? NSNull() } as NSArray
        })
        let selectedKeys = Set(a.list(2).map { AnyHashable(KHashable($0 as? NSObject ?? NSNull())) })
        let selection = Binding<Set<Int64>>(
            get: { Set(rows.filter { selectedKeys.contains($0.model.args.hashable(0)) }.map(\.id)) },
            set: { ids in onSelect(rows.filter { ids.contains($0.id) }.map { $0.model.args.hashable(0) }) }
        )
        if #available(iOS 17.4, macOS 14.4, *) {
            Table(rows, selection: selectable ? selection : .constant([])) {
                TableColumnForEach(Array(titles.indices), id: \.self) { c in
                    let w = c < widths.count ? widths[c] : []
                    TableColumn(titles[c]) { (row: TableRowRef) in
                        let cells = row.model.children
                        if c < cells.count { NodeView(model: cells[c]) }
                    }
                    .width(min: w.count > 0 ? w[0] : nil, ideal: w.count > 1 ? w[1] : nil, max: w.count > 2 ? w[2] : nil)
                }
            }
        } else {
            List(rows) { row in
                HStack { ForEach(row.model.children, id: \.nodeId) { NodeView(model: $0) } }
            }
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
                HStack { ForEach(row.model.children, id: \.nodeId) { NodeView(model: $0) } }
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
    }
}
