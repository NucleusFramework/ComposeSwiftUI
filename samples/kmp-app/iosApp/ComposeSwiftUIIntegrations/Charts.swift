// Swift Charts for Compose SwiftUI: renders the Kotlin `Chart`, marks, plots, 3D charts and `chart*` modifiers
// (runtime Charts.kt). Add this file to the app target and call `ComposeIntegrations.registerCharts()` at launch.
import SwiftUI
import Charts
#if os(iOS) || os(macOS) || os(visionOS)
import Spatial
#endif

public extension ComposeIntegrations {
    @MainActor static func registerCharts() {
        ComposeRegistry.views[200_100] = { m in ModChain.apply(KotlinChart(m: m), m, 0) }
        #if os(iOS) || os(macOS) || os(visionOS)
        if #available(iOS 26.0, macOS 26.0, *) {
            ComposeRegistry.views[200_104] = { m in ModChain.apply(KotlinChart3D(m: m), m, 0) }
            ComposeRegistry.modifiers[200_112] = { v, a, _ in AnyView(v.modifier(Chart3DPoseModifier(a: a))) }
            ComposeRegistry.modifiers[200_113] = { v, a, _ in
                let p: [Chart3DCameraProjection] = [.automatic, .orthographic, .perspective]
                return AnyView(v.chart3DCameraProjection(p[min(max(a.int(0), 0), 2)]))
            }
        }
        #endif
        ComposeRegistry.modifiers[200_100] = { v, a, _ in ChartModifiers.axis(v, a) }
        ComposeRegistry.modifiers[200_101] = { v, a, _ in ChartModifiers.axisLabel(v, a) }
        ComposeRegistry.modifiers[200_102] = { v, a, _ in ChartModifiers.axisStyle(v, a) }
        ComposeRegistry.modifiers[200_103] = { v, a, _ in
            let slot = a.slotModel(0)
            return AnyView(v.chartPlotStyle { plot in SlotView(model: slot).environment(\.composeStyleContent, AnyView(plot)) })
        }
        ComposeRegistry.modifiers[200_104] = { v, a, _ in ChartModifiers.legend(v, a) }
        ComposeRegistry.modifiers[200_105] = { v, a, _ in ChartModifiers.scale(v, a) }
        ComposeRegistry.modifiers[200_106] = { v, a, _ in ChartModifiers.styleScale(v, a) }
        ComposeRegistry.modifiers[200_107] = { v, a, _ in ChartModifiers.selection(v, a) }
        ComposeRegistry.modifiers[200_108] = { v, a, _ in
            guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let axes = d_Axis_Set(a.expr(0)) else { return v }
            return AnyView(v.chartScrollableAxes(axes))
        }
        ComposeRegistry.modifiers[200_109] = { v, a, _ in ChartModifiers.visibleDomain(v, a) }
        ComposeRegistry.modifiers[200_110] = { v, a, _ in ChartModifiers.scrollPosition(v, a) }
        ComposeRegistry.modifiers[200_111] = { v, a, _ in ChartModifiers.scrollTarget(v, a) }
    }
}

// MARK: - Plottable values

private let dateUnits: [Calendar.Component] = {
    let base: [Calendar.Component] = [.era, .year, .month, .day, .hour, .minute, .second, .weekday, .weekOfYear, .weekOfMonth, .quarter]
    if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) { return base + [.dayOfYear] }
    return base + [.day]
}()

/// A Kotlin `PlottableValue`: `[label, kind (0 String, 1 Double, 2 date), raw, unit?]`.
struct KPlot {
    let label: String
    let kind: Int
    let raw: Any?
    let unit: Calendar.Component?

    init(label: String, kind: Int, raw: Any?, unit: Calendar.Component? = nil) {
        self.label = label; self.kind = kind; self.raw = raw; self.unit = unit
    }

    init?(_ v: Any?) {
        guard let l = v as? [Any], l.count >= 3 else { return nil }
        label = l[0] as? String ?? ""
        kind = (l[1] as? NSNumber)?.intValue ?? 1
        raw = l[2]
        unit = l.count > 3 ? (l[3] as? NSNumber).map { dateUnits[min(max($0.intValue, 0), dateUnits.count - 1)] } : nil
    }

    var string: String { raw as? String ?? (raw as? NSNumber)?.stringValue ?? "" }
    var double: Double { (raw as? NSNumber)?.doubleValue ?? Double(string) ?? 0 }
    var date: Date { Date(timeIntervalSince1970: double) }
    var value: Any { kind == 0 ? string as Any : kind == 2 ? date as Any : double as Any }
}

/// Encodes a value read from a chart for Kotlin: `[kind, raw]`.
func kRaw(_ v: Any?) -> NSArray? {
    switch v {
    case let s as String: return [KotlinInt(value: 0), s] as NSArray
    case let d as Date: return [KotlinInt(value: 2), KotlinDouble(value: d.timeIntervalSince1970)] as NSArray
    case let d as Double: return [KotlinInt(value: 1), KotlinDouble(value: d)] as NSArray
    case let i as Int: return [KotlinInt(value: 1), KotlinDouble(value: Double(i))] as NSArray
    case let f as Float: return [KotlinInt(value: 1), KotlinDouble(value: Double(f))] as NSArray
    default: return nil
    }
}

@MainActor protocol PlotVisitor {
    func visit<X: Plottable>(_ x: PlottableValue<X>) -> AnyChartContent
}

@MainActor protocol PlotVisitor2 {
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent
}

/// Opens the Swift type of a Kotlin value (String, Double, Date) for a generic mark initializer.
@MainActor func openPlot<V: PlotVisitor>(_ p: KPlot, _ v: V) -> AnyChartContent {
    switch p.kind {
    case 0: return v.visit(.value(p.label, p.string))
    case 2:
        if let unit = p.unit { return v.visit(.value(p.label, p.date, unit: unit)) }
        return v.visit(.value(p.label, p.date))
    default: return v.visit(.value(p.label, p.double))
    }
}

private struct Open2A<V: PlotVisitor2>: PlotVisitor {
    let b: KPlot
    let v: V
    func visit<X: Plottable>(_ x: PlottableValue<X>) -> AnyChartContent { openPlot(b, Open2B(x: x, v: v)) }
}

private struct Open2B<X: Plottable, V: PlotVisitor2>: PlotVisitor {
    let x: PlottableValue<X>
    let v: V
    func visit<Y: Plottable>(_ y: PlottableValue<Y>) -> AnyChartContent { v.visit(x, y) }
}

@MainActor func openPlots<V: PlotVisitor2>(_ a: KPlot, _ b: KPlot, _ v: V) -> AnyChartContent {
    openPlot(a, Open2A(b: b, v: v))
}

/// The end of a range, with the Swift type of its start (`fallback` when the Kotlin types differ).
func typed<X: Plottable>(_ p: KPlot?, _ fallback: PlottableValue<X>) -> PlottableValue<X> {
    guard let p else { return fallback }
    if p.kind == 2, let unit = p.unit, let r = PlottableValue<Date>.value(p.label, p.date, unit: unit) as? PlottableValue<X> { return r }
    guard let v = p.value as? X else { return fallback }
    return .value(p.label, v)
}

// MARK: - Marks

private func dimension(_ v: Any?) -> MarkDimension {
    let l = v as? [Any] ?? []
    let value = l.count > 1 ? CGFloat((l[1] as? NSNumber)?.doubleValue ?? 0) : 0
    switch (l.first as? NSNumber)?.intValue ?? 0 {
    case 1: return .fixed(value)
    case 2: return .inset(value)
    case 3: return .ratio(value)
    default: return .automatic
    }
}

private func stacking(_ v: Any?) -> MarkStackingMethod {
    switch (v as? NSNumber)?.intValue ?? 0 {
    case 1: return .normalized
    case 2: return .center
    case 3: return .unstacked
    default: return .standard
    }
}

/// `series` keeps the category label of its value.
private func seriesValue(_ p: KPlot) -> PlottableValue<String> { .value(p.label, p.kind == 2 ? "\(p.date)" : p.string) }

private struct BarXY: PlotVisitor2 {
    let w: MarkDimension, h: MarkDimension, s: MarkStackingMethod
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: BarMark(x: x, y: y, width: w, height: h, stacking: s))
    }
}

private struct BarXRangeY: PlotVisitor2 {
    let xEnd: KPlot?, h: MarkDimension, s: MarkStackingMethod
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: BarMark(xStart: xs, xEnd: typed(xEnd, xs), y: y, height: h))
    }
}

private struct BarXYRange: PlotVisitor2 {
    let yEnd: KPlot?, w: MarkDimension, s: MarkStackingMethod
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: BarMark(x: x, yStart: ys, yEnd: typed(yEnd, ys), width: w))
    }
}

private struct BarRanges: PlotVisitor2 {
    let xEnd: KPlot?, yEnd: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RectangleMark(xStart: xs, xEnd: typed(xEnd, xs), yStart: ys, yEnd: typed(yEnd, ys)))
    }
}

/// A range on one axis only (`BarMark(xStart:xEnd:)`, `RuleMark(xStart:xEnd:)`…).
private struct Range1D: PlotVisitor {
    let end: KPlot?, vertical: Bool, rule: Bool
    func visit<X: Plottable>(_ start: PlottableValue<X>) -> AnyChartContent {
        let end = typed(self.end, start)
        switch (vertical, rule) {
        case (false, false): return AnyChartContent(erasing: BarMark(xStart: start, xEnd: end))
        case (true, false): return AnyChartContent(erasing: BarMark(yStart: start, yEnd: end))
        case (false, true): return AnyChartContent(erasing: RuleMark(xStart: start, xEnd: end))
        case (true, true): return AnyChartContent(erasing: RuleMark(yStart: start, yEnd: end))
        }
    }
}

private struct Bar1D: PlotVisitor {
    let vertical: Bool, w: MarkDimension, h: MarkDimension, s: MarkStackingMethod
    func visit<X: Plottable>(_ x: PlottableValue<X>) -> AnyChartContent {
        vertical ? AnyChartContent(erasing: BarMark(y: x, height: h, stacking: s)) : AnyChartContent(erasing: BarMark(x: x, width: w, stacking: s))
    }
}

private struct LineXY: PlotVisitor2 {
    let series: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        if let series { return AnyChartContent(erasing: LineMark(x: x, y: y, series: seriesValue(series))) }
        return AnyChartContent(erasing: LineMark(x: x, y: y))
    }
}

private struct PointXY: PlotVisitor2 {
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: PointMark(x: x, y: y))
    }
}

private struct Point1D: PlotVisitor {
    let vertical: Bool
    func visit<X: Plottable>(_ x: PlottableValue<X>) -> AnyChartContent {
        vertical ? AnyChartContent(erasing: PointMark(y: x)) : AnyChartContent(erasing: PointMark(x: x))
    }
}

private struct AreaXY: PlotVisitor2 {
    let series: KPlot?, s: MarkStackingMethod
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        if let series { return AnyChartContent(erasing: AreaMark(x: x, y: y, series: seriesValue(series), stacking: s)) }
        return AnyChartContent(erasing: AreaMark(x: x, y: y, stacking: s))
    }
}

private struct AreaXYRange: PlotVisitor2 {
    let yEnd: KPlot?, series: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        if let series { return AnyChartContent(erasing: AreaMark(x: x, yStart: ys, yEnd: typed(yEnd, ys), series: seriesValue(series))) }
        return AnyChartContent(erasing: AreaMark(x: x, yStart: ys, yEnd: typed(yEnd, ys)))
    }
}

private struct AreaXRangeY: PlotVisitor2 {
    let xEnd: KPlot?, series: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        if let series { return AnyChartContent(erasing: AreaMark(xStart: xs, xEnd: typed(xEnd, xs), y: y, series: seriesValue(series))) }
        return AnyChartContent(erasing: AreaMark(xStart: xs, xEnd: typed(xEnd, xs), y: y))
    }
}

private struct Rule1D: PlotVisitor {
    let vertical: Bool
    func visit<X: Plottable>(_ x: PlottableValue<X>) -> AnyChartContent {
        vertical ? AnyChartContent(erasing: RuleMark(y: x)) : AnyChartContent(erasing: RuleMark(x: x))
    }
}

private struct RuleXRangeY: PlotVisitor2 {
    let xEnd: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RuleMark(xStart: xs, xEnd: typed(xEnd, xs), y: y))
    }
}

private struct RuleXYRange: PlotVisitor2 {
    let yEnd: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RuleMark(x: x, yStart: ys, yEnd: typed(yEnd, ys)))
    }
}

private struct RectXY: PlotVisitor2 {
    let w: MarkDimension, h: MarkDimension
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RectangleMark(x: x, y: y, width: w, height: h))
    }
}

private struct RectXRangeY: PlotVisitor2 {
    let xEnd: KPlot?, h: MarkDimension
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ y: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RectangleMark(xStart: xs, xEnd: typed(xEnd, xs), y: y, height: h))
    }
}

private struct RectXYRange: PlotVisitor2 {
    let yEnd: KPlot?, w: MarkDimension
    func visit<X: Plottable, Y: Plottable>(_ x: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RectangleMark(x: x, yStart: ys, yEnd: typed(yEnd, ys), width: w))
    }
}

private struct RectRanges: PlotVisitor2 {
    let xEnd: KPlot?, yEnd: KPlot?
    func visit<X: Plottable, Y: Plottable>(_ xs: PlottableValue<X>, _ ys: PlottableValue<Y>) -> AnyChartContent {
        AnyChartContent(erasing: RectangleMark(xStart: xs, xEnd: typed(xEnd, xs), yStart: ys, yEnd: typed(yEnd, ys)))
    }
}

private struct Sector: PlotVisitor {
    let inner: MarkDimension, outer: MarkDimension, inset: CGFloat?
    func visit<X: Plottable>(_ angle: PlottableValue<X>) -> AnyChartContent {
        if #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) {
            return AnyChartContent(erasing: SectorMark(angle: angle, innerRadius: inner, outerRadius: outer, angularInset: inset))
        }
        return AnyChartContent(erasing: BarMark(x: angle))
    }
}

/// Builds a mark: `kind` 0 bar, 1 line, 2 point, 3 area, 4 rule, 5 rectangle, 6 sector; `v` the Kotlin values in
/// the order of its Kotlin parameters, `extra` its dimensions and stacking.
@MainActor func buildMark(_ kind: Int, _ v: [KPlot?], _ extra: [Any]) -> AnyChartContent {
    func at(_ i: Int) -> KPlot? { i < v.count ? v[i] : nil }
    func ex(_ i: Int) -> Any? { i < extra.count ? extra[i] : nil }
    let x = at(0), y = at(1), xs = at(2), xe = at(3), ys = at(4), ye = at(5)
    switch kind {
    case 0:
        let w = dimension(ex(0)), h = dimension(ex(1)), s = stacking(ex(2))
        if let xs, let ys { return openPlots(xs, ys, BarRanges(xEnd: xe, yEnd: ye)) }
        if let xs, let y { return openPlots(xs, y, BarXRangeY(xEnd: xe, h: h, s: s)) }
        if let x, let ys { return openPlots(x, ys, BarXYRange(yEnd: ye, w: w, s: s)) }
        if let x, let y { return openPlots(x, y, BarXY(w: w, h: h, s: s)) }
        if let x { return openPlot(x, Bar1D(vertical: false, w: w, h: h, s: s)) }
        if let y { return openPlot(y, Bar1D(vertical: true, w: w, h: h, s: s)) }
        if let xs { return openPlot(xs, Range1D(end: xe, vertical: false, rule: false)) }
        if let ys { return openPlot(ys, Range1D(end: ye, vertical: true, rule: false)) }
    case 1:
        if let x, let y { return openPlots(x, y, LineXY(series: at(2))) }
    case 2:
        if let x, let y { return openPlots(x, y, PointXY()) }
        if let x { return openPlot(x, Point1D(vertical: false)) }
        if let y { return openPlot(y, Point1D(vertical: true)) }
    case 3:
        let series = at(6)
        if let x, let ys { return openPlots(x, ys, AreaXYRange(yEnd: ye, series: series)) }
        if let xs, let y { return openPlots(xs, y, AreaXRangeY(xEnd: xe, series: series)) }
        if let x, let y { return openPlots(x, y, AreaXY(series: series, s: stacking(ex(0)))) }
    case 4:
        if let xs, let y { return openPlots(xs, y, RuleXRangeY(xEnd: xe)) }
        if let x, let ys { return openPlots(x, ys, RuleXYRange(yEnd: ye)) }
        if let x { return openPlot(x, Rule1D(vertical: false)) }
        if let y { return openPlot(y, Rule1D(vertical: true)) }
        if let xs { return openPlot(xs, Range1D(end: xe, vertical: false, rule: true)) }
        if let ys { return openPlot(ys, Range1D(end: ye, vertical: true, rule: true)) }
    case 5:
        let w = dimension(ex(0)), h = dimension(ex(1))
        if let xs, let ys { return openPlots(xs, ys, RectRanges(xEnd: xe, yEnd: ye)) }
        if let xs, let y { return openPlots(xs, y, RectXRangeY(xEnd: xe, h: h)) }
        if let x, let ys { return openPlots(x, ys, RectXYRange(yEnd: ye, w: w)) }
        if let x, let y { return openPlots(x, y, RectXY(w: w, h: h)) }
    case 6:
        if let angle = x {
            return openPlot(angle, Sector(inner: dimension(ex(0)), outer: dimension(ex(1)),
                                          inset: (ex(2) as? NSNumber).map { CGFloat($0.doubleValue) }))
        }
    default: break
    }
    return AnyChartContent(erasing: NoChartContent())
}

/// Empty chart content.
private struct NoChartContent: ChartContent {
    var body: some ChartContent {}
}

// MARK: - Mark styles

private struct StyleBy: PlotVisitor {
    let content: AnyChartContent
    let op: Int
    var axis: Axis? = nil
    var span: MarkDimension = .automatic

    func visit<X: Plottable>(_ v: PlottableValue<X>) -> AnyChartContent {
        switch op {
        case 0: return AnyChartContent(erasing: content.foregroundStyle(by: v))
        case 1: return AnyChartContent(erasing: content.symbol(by: v))
        case 2: return AnyChartContent(erasing: content.symbolSize(by: v))
        case 3: return AnyChartContent(erasing: content.lineStyle(by: v))
        default:
            if let axis { return AnyChartContent(erasing: content.position(by: v, axis: axis, span: span)) }
            return AnyChartContent(erasing: content.position(by: v, span: span))
        }
    }
}

private func symbolShape(_ v: Any?) -> AnyChartSymbolShape? {
    guard let l = v as? [Any] else { return nil }
    let shapes: [BasicChartSymbolShape] = [.circle, .square, .triangle, .diamond, .pentagon, .plus, .cross, .asterisk]
    let shape = shapes[min(max((l.first as? NSNumber)?.intValue ?? 0, 0), shapes.count - 1)]
    if l.count > 1, let w = l[1] as? NSNumber { return AnyChartSymbolShape(shape.strokeBorder(lineWidth: CGFloat(w.doubleValue))) }
    return AnyChartSymbolShape(shape)
}

private func interpolation(_ v: Any?) -> InterpolationMethod? {
    guard let l = v as? [Any] else { return nil }
    let p = l.count > 1 ? (l[1] as? NSNumber)?.doubleValue : nil
    switch (l.first as? NSNumber)?.intValue ?? 0 {
    case 1: return .monotone
    case 2: return p.map { .catmullRom(alpha: $0) } ?? .catmullRom
    case 3: return p.map { .cardinal(tension: $0) } ?? .cardinal
    case 4: return .stepStart
    case 5: return .stepCenter
    case 6: return .stepEnd
    default: return .linear
    }
}

private let annotationPositions: [AnnotationPosition] = [.automatic, .overlay, .top, .bottom, .leading, .trailing, .topLeading,
                                                         .topTrailing, .bottomLeading, .bottomTrailing]

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private func overflow(_ v: Any?) -> AnnotationOverflowResolution.Strategy {
    guard let l = v as? [Any] else { return .automatic }
    let boundaries: [AnnotationOverflowResolution.Boundary] = [.automatic, .chart, .plot]
    let boundary = boundaries[min(max(l.count > 1 ? (l[1] as? NSNumber)?.intValue ?? 0 : 0, 0), 2)]
    switch (l.first as? NSNumber)?.intValue ?? 0 {
    case 1: return .disabled
    case 2: return .fit(to: boundary)
    case 3: return .padScale
    default: return .automatic
    }
}

/// Applies a Kotlin `ChartMarkStyle`, annotation, symbol view and mask to a mark.
@MainActor func styleMark(_ mark: AnyChartContent, _ style: [Any], _ a: Args, annotationIndex: Int?, slots: Int?) -> AnyChartContent {
    func at(_ i: Int) -> Any? { i < style.count ? style[i] : nil }
    func num(_ i: Int) -> CGFloat? { (at(i) as? NSNumber).map { CGFloat($0.doubleValue) } }
    var c = mark
    if let s = dp_ShapeStyle(at(0) as? SExpr) { c = AnyChartContent(erasing: c.foregroundStyle(AnyShapeStyle(s))) }
    if let p = KPlot(at(1)) { c = openPlot(p, StyleBy(content: c, op: 0)) }
    if let shape = symbolShape(at(2)) { c = AnyChartContent(erasing: c.symbol(shape)) }
    if let p = KPlot(at(3)) { c = openPlot(p, StyleBy(content: c, op: 1)) }
    if let size = num(4) { c = AnyChartContent(erasing: c.symbolSize(size * size)) }
    if let p = KPlot(at(5)) { c = openPlot(p, StyleBy(content: c, op: 2)) }
    if let stroke = d_StrokeStyle(at(6) as? SExpr) { c = AnyChartContent(erasing: c.lineStyle(stroke)) }
    if let p = KPlot(at(7)) { c = openPlot(p, StyleBy(content: c, op: 3)) }
    if let m = interpolation(at(8)) { c = AnyChartContent(erasing: c.interpolationMethod(m)) }
    if let r = num(9) { c = AnyChartContent(erasing: c.cornerRadius(r, style: d_RoundedCornerStyle(at(10) as? SExpr) ?? .continuous)) }
    if let o = at(11) as? NSNumber { c = AnyChartContent(erasing: c.opacity(o.doubleValue)) }
    if let off = at(12) as? [Any] {
        func o(_ i: Int) -> CGFloat? { i < off.count ? (off[i] as? NSNumber).map { CGFloat($0.doubleValue) } : nil }
        if o(2) != nil || o(3) != nil || o(4) != nil || o(5) != nil {
            c = AnyChartContent(erasing: c.offset(xStart: o(2) ?? 0, xEnd: o(3) ?? 0, yStart: o(4) ?? 0, yEnd: o(5) ?? 0))
        } else if o(0) != nil || o(1) != nil {
            c = AnyChartContent(erasing: c.offset(x: o(0) ?? 0, y: o(1) ?? 0))
        }
    }
    if let pos = at(13) as? [Any], let p = KPlot(pos.first) {
        let axis = pos.count > 1 ? d_Axis(pos[1] as? SExpr) : nil
        c = openPlot(p, StyleBy(content: c, op: 4, axis: axis, span: pos.count > 2 ? dimension(pos[2]) : .automatic))
    }
    if #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) {
        if let z = at(14) as? NSNumber { c = AnyChartContent(erasing: c.zIndex(z.doubleValue)) }
        if let b = num(15) { c = AnyChartContent(erasing: c.blur(radius: b)) }
        if let sh = at(16) as? [Any], sh.count >= 4 {
            let color = d_Color(sh[0] as? SExpr) ?? Color(.sRGBLinear, white: 0, opacity: 0.33)
            c = AnyChartContent(erasing: c.shadow(color: color, radius: CGFloat((sh[1] as? NSNumber)?.doubleValue ?? 0),
                                                  x: CGFloat((sh[2] as? NSNumber)?.doubleValue ?? 0), y: CGFloat((sh[3] as? NSNumber)?.doubleValue ?? 0)))
        }
        if let shape = dp_Shape(at(17) as? SExpr) { c = AnyChartContent(erasing: c.clipShape(AnyShape(shape))) }
        if (at(18) as? NSNumber)?.boolValue == true { c = AnyChartContent(erasing: c.compositingLayer()) }
    }
    if let aligns = at(19) as? NSNumber { c = AnyChartContent(erasing: c.alignsMarkStylesWithPlotArea(aligns.boolValue)) }
    if let ax = at(20) as? [Any] {
        if ax.count > 0, let l = ax[0] as? String { c = AnyChartContent(erasing: c.accessibilityLabel(Text(l))) }
        if ax.count > 1, let v = ax[1] as? String { c = AnyChartContent(erasing: c.accessibilityValue(Text(v))) }
        if ax.count > 2, let i = ax[2] as? String { c = AnyChartContent(erasing: c.accessibilityIdentifier(i)) }
        if ax.count > 3, let h = ax[3] as? NSNumber { c = AnyChartContent(erasing: c.accessibilityHidden(h.boolValue)) }
    }
    guard let slots else { return c }
    // annotation, symbol view, mask
    if let annotationIndex, !a.isNil(annotationIndex), let content = a.slotModel(slots) {
        let l = a.list(annotationIndex)
        let position = annotationPositions[min(max((l.first as? NSNumber)?.intValue ?? 0, 0), annotationPositions.count - 1)]
        let alignment = l.count > 1 ? d_Alignment(l[1] as? SExpr) ?? .center : .center
        let spacing = l.count > 2 ? (l[2] as? NSNumber).map { CGFloat($0.doubleValue) } : nil
        if #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), l.count > 4, l[3] is [Any] || l[4] is [Any] {
            let resolution = AnnotationOverflowResolution(x: overflow(l[3]), y: overflow(l[4]))
            c = AnyChartContent(erasing: c.annotation(position: position, alignment: alignment, spacing: spacing, overflowResolution: resolution) {
                SlotView(model: content)
            })
        } else {
            c = AnyChartContent(erasing: c.annotation(position: position, alignment: alignment, spacing: spacing) { SlotView(model: content) })
        }
    }
    if let symbol = a.slotModel(slots + 1) { c = AnyChartContent(erasing: c.symbol { SlotView(model: symbol) }) }
    if #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let mask = a.slotModel(slots + 2) {
        c = AnyChartContent(erasing: c.mask { ChartContentList(model: mask) })
    }
    return c
}

// MARK: - Chart content

@MainActor struct ChartContentList: ChartContent {
    let model: NodeModel

    var body: some ChartContent {
        let _ = model.version
        ForEach(model.children, id: \.nodeId) { child in
            ChartNode(model: child)
        }
    }
}

@MainActor struct ChartNode: ChartContent {
    let model: NodeModel

    var body: some ChartContent {
        let _ = model.version
        if model.kind == 200_101 || model.kind == 200_102 || model.kind == 200_103 {
            ChartNode.build(model)
        } else {
            // a Kotlin group (loop, if, key) of marks
            ChartContentList(model: model)
        }
    }

    static func build(_ m: NodeModel) -> AnyChartContent {
        let a = m.args
        switch m.kind {
        case 200_101:
            let mark = buildMark(a.int(0), a.list(1).map { KPlot($0) }, a.list(2))
            return styleMark(mark, a.list(3), a, annotationIndex: 4, slots: 5)
        case 200_102:
            return plot(a)
        default:
            return functionPlot(a)
        }
    }

    /// Vectorized plots, drawn as one mark per item.
    private static func plot(_ a: Args) -> AnyChartContent {
        let kind = a.int(0)
        let columns: [[Any]?] = a.list(1).map { $0 as? [Any] }
        let extra = a.list(2)
        func column(_ i: Int) -> (label: String, kind: Int, values: [Any], unit: Calendar.Component?)? {
            guard i < columns.count, let c = columns[i], c.count >= 3 else { return nil }
            let unit = c.count > 3 ? (c[3] as? NSNumber).map { dateUnits[min(max($0.intValue, 0), dateUnits.count - 1)] } : nil
            return (c[0] as? String ?? "", (c[1] as? NSNumber)?.intValue ?? 1, c[2] as? [Any] ?? [], unit)
        }
        // Kotlin columns: x, y, xStart, xEnd, yStart, yEnd, series, angle; mark value order per kind
        let order: [Int]
        let markExtra: [Any]
        switch kind {
        case 1: order = [0, 1, 6]; markExtra = []
        case 2: order = [0, 1]; markExtra = []
        case 3: order = [0, 1, 2, 3, 4, 5, 6]; markExtra = [extra.count > 2 ? extra[2] : 0]
        case 6: order = [7]; markExtra = Array(extra.dropFirst(3))
        default: order = [0, 1, 2, 3, 4, 5]; markExtra = Array(extra.prefix(3))
        }
        let cols = order.map(column)
        let count = cols.compactMap { $0?.values.count }.max() ?? 0
        let style = a.list(3)
        let marks = (0..<count).map { row -> AnyChartContent in
            let values = cols.map { c -> KPlot? in
                guard let c, row < c.values.count else { return nil }
                return KPlot(label: c.label, kind: c.kind, raw: c.values[row], unit: c.unit)
            }
            return styleMark(buildMark(kind, values, markExtra), style, a, annotationIndex: nil, slots: nil)
        }
        return AnyChartContent(erasing: ForEach(marks.indices, id: \.self) { marks[$0] })
    }

    private static func functionPlot(_ a: Args) -> AnyChartContent {
        let spec = a.list(0)
        let style = a.list(1)
        guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *), spec.count >= 4,
              let fn = spec[3] as? ChartFunction else { return AnyChartContent(erasing: NoChartContent()) }
        let labels = (spec[1] as? [Any] ?? []).map { $0 as? String ?? "" }
        let d = (spec[2] as? [Any])?.compactMap { ($0 as? NSNumber)?.doubleValue }
        let domain: ClosedRange<Double>? = (d?.count == 2 && d![0] <= d![1]) ? d![0]...d![1] : nil
        let box = ChartFunctionBox(fn)
        func label(_ i: Int) -> String { i < labels.count ? labels[i] : "" }
        let content: AnyChartContent
        switch (spec[0] as? NSNumber)?.intValue ?? 0 {
        case 1:
            content = AnyChartContent(erasing: LinePlot(x: label(0), y: label(1), t: label(2), domain: domain ?? 0...1) { t in
                let r = box.call([t])
                return (x: r.count > 0 ? r[0] : .nan, y: r.count > 1 ? r[1] : .nan)
            })
        case 2:
            content = AnyChartContent(erasing: AreaPlot(x: label(0), yStart: label(1), yEnd: label(1), domain: domain) { x in
                let r = box.call([x])
                return (yStart: r.count > 0 ? r[0] : .nan, yEnd: r.count > 1 ? r[1] : .nan)
            })
        default:
            content = AnyChartContent(erasing: LinePlot(x: label(0), y: label(1), domain: domain) { x in box.call([x]).first ?? .nan })
        }
        return styleMark(content, style, a, annotationIndex: nil, slots: nil)
    }
}

/// A Kotlin function that Swift Charts may call from any thread.
final class ChartFunctionBox: @unchecked Sendable {
    let fn: ChartFunction
    init(_ fn: ChartFunction) { self.fn = fn }

    func call(_ input: [Double]) -> [Double] {
        fn.evaluate(input: input.map { KotlinDouble(value: $0) }).map { $0.doubleValue }
    }
}

// MARK: - Chart

private struct KotlinChart: View {
    let m: NodeModel
    @State private var box = ChartProxyBox()

    var body: some View {
        let _ = m.version
        let a = m.args
        let box = box
        overlays(Chart { ChartContentList(model: m) }, a, box)
            .onAppear { a.callback(0, box: { (b: ChartProxyBox) -> Any? in b })(box) }
    }

    @ViewBuilder
    private func overlays(_ chart: some View, _ a: Args, _ box: ChartProxyBox) -> some View {
        let overlay = a.slotModel(1)
        let background = a.slotModel(3)
        let gestured = gesture(chart, a, box)
        gestured
            .chartOverlay(alignment: d_Alignment(a.expr(2)) ?? .center) { proxy in
                GeometryReader { geo in
                    let _ = box.update(proxy, geo)
                    if let overlay { SlotView(model: overlay) }
                }
            }
            .chartBackground(alignment: d_Alignment(a.expr(4)) ?? .center) { proxy in
                if let background {
                    GeometryReader { geo in
                        let _ = box.update(proxy, geo)
                        SlotView(model: background)
                    }
                }
            }
    }

    @ViewBuilder
    private func gesture(_ chart: some View, _ a: Args, _ box: ChartProxyBox) -> some View {
        #if os(tvOS)
        chart
        #else
        if #available(iOS 17.0, macOS 14.0, watchOS 10.0, *), !a.isNil(5), let g = buildGesture(a.list(5)) {
            chart.chartGesture { proxy in
                box.proxy = proxy
                return g
            }
        } else {
            chart
        }
        #endif
    }
}

/// The Kotlin `ChartProxy`: the proxy and plot frame of the last layout.
@MainActor final class ChartProxyBox: NSObject, @preconcurrency ChartProxyHandle {
    var proxy: ChartProxy?
    var frame: CGRect?

    func update(_ proxy: ChartProxy, _ geo: GeometryProxy) {
        self.proxy = proxy
        if #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) {
            frame = proxy.plotFrame.map { geo[$0] }
        } else {
            frame = geo[proxy.plotAreaFrame]
        }
    }

    func valueAt(axis: Int32, position: Double, type: Int32) -> Any? {
        guard let proxy else { return nil }
        let p = CGFloat(position)
        if axis == 0 {
            if let s = proxy.value(atX: p, as: String.self) { return kRaw(s) }
            return type == 2 ? kRaw(proxy.value(atX: p, as: Date.self)) : kRaw(proxy.value(atX: p, as: Double.self))
        }
        if let s = proxy.value(atY: p, as: String.self) { return kRaw(s) }
        return type == 2 ? kRaw(proxy.value(atY: p, as: Date.self)) : kRaw(proxy.value(atY: p, as: Double.self))
    }

    private func plot(_ v: Any) -> KPlot? {
        guard let l = v as? [Any], l.count >= 2 else { return nil }
        return KPlot(label: "", kind: (l[0] as? NSNumber)?.intValue ?? 1, raw: l[1])
    }

    func positionFor(axis: Int32, value: Any) -> KotlinDouble? {
        guard let proxy, let p = plot(value) else { return nil }
        let r: CGFloat?
        switch (p.kind, axis) {
        case (0, 0): r = proxy.position(forX: p.string)
        case (2, 0): r = proxy.position(forX: p.date)
        case (_, 0): r = proxy.position(forX: p.double)
        case (0, _): r = proxy.position(forY: p.string)
        case (2, _): r = proxy.position(forY: p.date)
        default: r = proxy.position(forY: p.double)
        }
        return r.map { KotlinDouble(value: Double($0)) }
    }

    func positionForPoint(x: Any, y: Any) -> [KotlinDouble]? {
        guard let px = positionFor(axis: 0, value: x), let py = positionFor(axis: 1, value: y) else { return nil }
        return [px, py]
    }

    func positionRange(axis: Int32, value: Any) -> [KotlinDouble]? {
        guard let proxy, let p = plot(value) else { return nil }
        let r: ClosedRange<CGFloat>?
        switch (p.kind, axis) {
        case (0, 0): r = proxy.positionRange(forX: p.string)
        case (2, 0): r = proxy.positionRange(forX: p.date)
        case (_, 0): r = proxy.positionRange(forX: p.double)
        case (0, _): r = proxy.positionRange(forY: p.string)
        case (2, _): r = proxy.positionRange(forY: p.date)
        default: r = proxy.positionRange(forY: p.double)
        }
        return r.map { [KotlinDouble(value: Double($0.lowerBound)), KotlinDouble(value: Double($0.upperBound))] }
    }

    func angleAt(x: Double, y: Double) -> KotlinDouble? {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let proxy else { return nil }
        return KotlinDouble(value: proxy.angle(at: CGPoint(x: x, y: y)).degrees)
    }

    func valueAtAngle(degrees: Double, type: Int32) -> Any? {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let proxy else { return nil }
        let a = Angle.degrees(degrees)
        if let s = proxy.value(atAngle: a, as: String.self) { return kRaw(s) }
        return type == 2 ? kRaw(proxy.value(atAngle: a, as: Date.self)) : kRaw(proxy.value(atAngle: a, as: Double.self))
    }

    func plotFrame() -> [KotlinDouble]? {
        frame.map { [$0.minX, $0.minY, $0.width, $0.height].map { KotlinDouble(value: Double($0)) } }
    }

    func domain(axis: Int32, type: Int32) -> [Any]? {
        guard let proxy else { return nil }
        if axis == 0 {
            let s = proxy.xDomain(dataType: String.self)
            if !s.isEmpty { return s.compactMap(kRaw) }
            return type == 2 ? proxy.xDomain(dataType: Date.self).compactMap(kRaw) : proxy.xDomain(dataType: Double.self).compactMap(kRaw)
        }
        let s = proxy.yDomain(dataType: String.self)
        if !s.isEmpty { return s.compactMap(kRaw) }
        return type == 2 ? proxy.yDomain(dataType: Date.self).compactMap(kRaw) : proxy.yDomain(dataType: Double.self).compactMap(kRaw)
    }

    func select(axis: Int32, values: [KotlinDouble]) {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let proxy else { return }
        let v = values.map { CGFloat($0.doubleValue) }
        switch (axis, v.count) {
        case (0, 1): proxy.selectXValue(at: v[0])
        case (0, 2): proxy.selectXRange(from: v[0], to: v[1])
        case (1, 1): proxy.selectYValue(at: v[0])
        case (1, 2): proxy.selectYRange(from: v[0], to: v[1])
        case (2, 2): proxy.selectAngleValue(at: proxy.angle(at: CGPoint(x: v[0], y: v[1])))
        default: break
        }
    }
}

// MARK: - Modifiers

@MainActor enum ChartModifiers {
    static func axis(_ v: AnyView, _ a: Args) -> AnyView {
        let axis = a.int(0)
        if let vis = d_Visibility(a.expr(1)) {
            switch axis {
            case 0: return AnyView(v.chartXAxis(vis))
            case 1: return AnyView(v.chartYAxis(vis))
            default:
                #if os(iOS) || os(macOS) || os(visionOS)
                if #available(iOS 26.0, macOS 26.0, *) { return AnyView(v.chartZAxis(vis)) }
                #endif
                return v
            }
        }
        let specs = a.list(2).compactMap { $0 as? [Any] }
        let layer = a.bool(3)
        let style = a.slotModel(4)
        switch axis {
        case 0: return AnyView(v.chartXAxis { composited(specs, layer, style) })
        case 1: return AnyView(v.chartYAxis { composited(specs, layer, style) })
        default:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 26.0, macOS 26.0, *) { return AnyView(v.chartZAxis { composited(specs, layer, style) }) }
            #endif
            return v
        }
    }

    static func axisLabel(_ v: AnyView, _ a: Args) -> AnyView {
        let position = annotationPositions[min(max(a.int(1), 0), annotationPositions.count - 1)]
        let alignment = d_Alignment(a.expr(2))
        let spacing = a.cgFloatOpt(3)
        let slot = a.slotModel(4)
        switch a.int(0) {
        case 0: return AnyView(v.chartXAxisLabel(position: position, alignment: alignment, spacing: spacing) { SlotView(model: slot) })
        case 1: return AnyView(v.chartYAxisLabel(position: position, alignment: alignment, spacing: spacing) { SlotView(model: slot) })
        default:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 26.0, macOS 26.0, *) {
                return AnyView(v.chartZAxisLabel(a.stringOpt(5) ?? "", position: position, alignment: alignment, spacing: spacing))
            }
            #endif
            return v
        }
    }

    static func axisStyle(_ v: AnyView, _ a: Args) -> AnyView {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return v }
        let slot = a.slotModel(1)
        if a.int(0) == 0 {
            return AnyView(v.chartXAxisStyle { axis in SlotView(model: slot).environment(\.composeStyleContent, AnyView(axis)) })
        }
        return AnyView(v.chartYAxisStyle { axis in SlotView(model: slot).environment(\.composeStyleContent, AnyView(axis)) })
    }

    static func legend(_ v: AnyView, _ a: Args) -> AnyView {
        if let vis = d_Visibility(a.expr(0)) { return AnyView(v.chartLegend(vis)) }
        let position = annotationPositions[min(max(a.int(1), 0), annotationPositions.count - 1)]
        let alignment = d_Alignment(a.expr(2))
        let spacing = a.cgFloatOpt(3)
        if let slot = a.slotModel(4) {
            return AnyView(v.chartLegend(position: position, alignment: alignment, spacing: spacing) { SlotView(model: slot) })
        }
        return AnyView(v.chartLegend(position: position, alignment: alignment, spacing: spacing))
    }

    private static func scaleType(_ v: Any?) -> ScaleType? {
        guard let l = v as? [Any] else { return nil }
        let p = l.count > 1 ? (l[1] as? NSNumber)?.doubleValue : nil
        switch (l.first as? NSNumber)?.intValue ?? 0 {
        case 1: return .log
        case 2: return .squareRoot
        case 3: return .power(exponent: p ?? 2)
        case 4: return p.map { .symmetricLog(slopeAtZero: $0) } ?? .symmetricLog
        case 5: return .date
        case 6: return .category
        default: return .linear
        }
    }

    static func scale(_ v: AnyView, _ a: Args) -> AnyView {
        let axis = a.int(0)
        let domain = a.isNil(1) ? nil : a.list(1)
        let range: PositionScaleRange? = a.isNil(2) ? nil : {
            let l = a.list(2).map { CGFloat(($0 as? NSNumber)?.doubleValue ?? 0) }
            return .plotDimension(startPadding: l.first ?? 0, endPadding: l.count > 1 ? l[1] : 0)
        }()
        let type = scaleType(a.isNil(3) ? nil : a.list(3))
        func n(_ l: [Any], _ i: Int) -> Double { i < l.count ? (l[i] as? NSNumber)?.doubleValue ?? 0 : 0 }
        guard let domain else {
            if let range { return AnyView(axis == 0 ? AnyView(v.chartXScale(range: range, type: type)) : AnyView(v.chartYScale(range: range, type: type))) }
            return axis == 0 ? AnyView(v.chartXScale(type: type)) : AnyView(v.chartYScale(type: type))
        }
        switch (domain.first as? NSNumber)?.intValue ?? 0 {
        case 1:
            let d = n(domain, 1)...max(n(domain, 1), n(domain, 2))
            return apply(v, axis, d, range, type)
        case 2:
            let d = Date(timeIntervalSince1970: n(domain, 1))...Date(timeIntervalSince1970: max(n(domain, 1), n(domain, 2)))
            return apply(v, axis, d, range, type)
        case 3:
            return apply(v, axis, (domain.count > 1 ? domain[1] as? [Any] : nil)?.compactMap { $0 as? String } ?? [], range, type)
        default:
            let includesZero = domain.count > 1 ? (domain[1] as? NSNumber)?.boolValue : nil
            let reversed = domain.count > 2 ? (domain[2] as? NSNumber)?.boolValue : nil
            return apply(v, axis, .automatic(includesZero: includesZero, reversed: reversed), range, type)
        }
    }

    private static func apply<D: ScaleDomain>(_ v: AnyView, _ axis: Int, _ d: D, _ range: PositionScaleRange?, _ type: ScaleType?) -> AnyView {
        switch (axis, range) {
        case (0, let r?): return AnyView(v.chartXScale(domain: d, range: r, type: type))
        case (0, nil): return AnyView(v.chartXScale(domain: d, type: type))
        case (1, let r?): return AnyView(v.chartYScale(domain: d, range: r, type: type))
        case (1, nil): return AnyView(v.chartYScale(domain: d, type: type))
        default:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 26.0, macOS 26.0, *) {
                if let r = range { return AnyView(v.chartZScale(domain: d, range: r, type: type)) }
                return AnyView(v.chartZScale(domain: d, type: type))
            }
            #endif
            return v
        }
    }

    static func styleScale(_ v: AnyView, _ a: Args) -> AnyView {
        let domain = a.list(1).compactMap { $0 as? String }
        let range = a.list(2)
        let type = scaleType(a.isNil(3) ? nil : a.list(3))
        func index(_ s: String) -> Int { domain.firstIndex(of: s) ?? 0 }
        switch a.int(0) {
        case 0:
            if domain.isEmpty { return AnyView(v.chartForegroundStyleScale(type: type)) }
            let styles = range.map { dp_ShapeStyle($0 as? SExpr).map { AnyShapeStyle($0) } ?? AnyShapeStyle(.foreground) }
            return AnyView(v.chartForegroundStyleScale(domain: domain) { (s: String) in styles.isEmpty ? AnyShapeStyle(.foreground) : styles[min(index(s), styles.count - 1)] })
        case 1:
            let shapes = range.map { symbolShape($0) ?? AnyChartSymbolShape(BasicChartSymbolShape.circle) }
            return AnyView(v.chartSymbolScale(domain: domain) { (s: String) in shapes.isEmpty ? AnyChartSymbolShape(BasicChartSymbolShape.circle) : shapes[min(index(s), shapes.count - 1)] })
        case 2:
            if domain.isEmpty { return AnyView(v.chartSymbolSizeScale(type: type)) }
            let sizes = range.map { CGFloat(($0 as? NSNumber)?.doubleValue ?? 1) }
            return AnyView(v.chartSymbolSizeScale(domain: domain) { (s: String) in sizes.isEmpty ? 1 : sizes[min(index(s), sizes.count - 1)] * sizes[min(index(s), sizes.count - 1)] })
        default:
            let strokes = range.map { d_StrokeStyle($0 as? SExpr) ?? StrokeStyle() }
            return AnyView(v.chartLineStyleScale(domain: domain) { (s: String) in strokes.isEmpty ? StrokeStyle() : strokes[min(index(s), strokes.count - 1)] })
        }
    }

    static func selection(_ v: AnyView, _ a: Args) -> AnyView {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return v }
        let axis = a.int(0), range = a.int(1) == 1, type = a.int(2)
        let report = a.callback(4, box: { (x: NSArray?) -> Any? in x ?? NSNull() })
        if range {
            let l = a.isNil(3) ? [] : a.list(3).compactMap { ($0 as? NSNumber)?.doubleValue }
            if type == 2 {
                let b = Binding<ClosedRange<Date>?>(
                    get: { l.count == 2 ? Date(timeIntervalSince1970: l[0])...Date(timeIntervalSince1970: max(l[0], l[1])) : nil },
                    set: { r in report(r.map { [KotlinInt(value: 2), KotlinDouble(value: $0.lowerBound.timeIntervalSince1970), KotlinDouble(value: $0.upperBound.timeIntervalSince1970)] as NSArray }) })
                return axis == 0 ? AnyView(v.chartXSelection(range: b)) : AnyView(v.chartYSelection(range: b))
            }
            let b = Binding<ClosedRange<Double>?>(
                get: { l.count == 2 ? l[0]...max(l[0], l[1]) : nil },
                set: { r in report(r.map { [KotlinInt(value: 1), KotlinDouble(value: $0.lowerBound), KotlinDouble(value: $0.upperBound)] as NSArray }) })
            return axis == 0 ? AnyView(v.chartXSelection(range: b)) : AnyView(v.chartYSelection(range: b))
        }
        let raw: Any? = a.isNil(3) ? nil : a.raw(3)
        switch type {
        case 0:
            let b = Binding<String?>(get: { raw as? String }, set: { report(kRaw($0)) })
            return selection(v, axis, b)
        case 2:
            let b = Binding<Date?>(get: { (raw as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue) } }, set: { report(kRaw($0)) })
            return selection(v, axis, b)
        default:
            let b = Binding<Double?>(get: { (raw as? NSNumber)?.doubleValue }, set: { report(kRaw($0)) })
            return selection(v, axis, b)
        }
    }

    @available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
    private static func selection<P: Plottable>(_ v: AnyView, _ axis: Int, _ b: Binding<P?>) -> AnyView {
        switch axis {
        case 0: return AnyView(v.chartXSelection(value: b))
        case 1: return AnyView(v.chartYSelection(value: b))
        case 2:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 26.0, macOS 26.0, *) { return AnyView(v.chartZSelection(value: b)) }
            #endif
            return v
        default: return AnyView(v.chartAngleSelection(value: b))
        }
    }

    static func visibleDomain(_ v: AnyView, _ a: Args) -> AnyView {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return v }
        let length = a.double(1)
        if a.int(2) == 0 {
            let n = Int(length)
            return a.int(0) == 0 ? AnyView(v.chartXVisibleDomain(length: n)) : AnyView(v.chartYVisibleDomain(length: n))
        }
        return a.int(0) == 0 ? AnyView(v.chartXVisibleDomain(length: length)) : AnyView(v.chartYVisibleDomain(length: length))
    }

    static func scrollPosition(_ v: AnyView, _ a: Args) -> AnyView {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *), let p = KPlot(["", a.list(1).first as Any, a.list(1).count > 1 ? a.list(1)[1] : 0] as [Any]) else { return v }
        let x = a.int(0) == 0
        guard !a.isNil(2) else {
            switch p.kind {
            case 0: return x ? AnyView(v.chartScrollPosition(initialX: p.string)) : AnyView(v.chartScrollPosition(initialY: p.string))
            case 2: return x ? AnyView(v.chartScrollPosition(initialX: p.date)) : AnyView(v.chartScrollPosition(initialY: p.date))
            default: return x ? AnyView(v.chartScrollPosition(initialX: p.double)) : AnyView(v.chartScrollPosition(initialY: p.double))
            }
        }
        let report = a.callback(2, box: { (x: NSArray?) -> Any? in x ?? NSNull() })
        switch p.kind {
        case 0:
            let b = Binding<String>(get: { p.string }, set: { report(kRaw($0)) })
            return x ? AnyView(v.chartScrollPosition(x: b)) : AnyView(v.chartScrollPosition(y: b))
        case 2:
            let b = Binding<Date>(get: { p.date }, set: { report(kRaw($0)) })
            return x ? AnyView(v.chartScrollPosition(x: b)) : AnyView(v.chartScrollPosition(y: b))
        default:
            let b = Binding<Double>(get: { p.double }, set: { report(kRaw($0)) })
            return x ? AnyView(v.chartScrollPosition(x: b)) : AnyView(v.chartScrollPosition(y: b))
        }
    }

    /// One axis of `valueAligned`: a numeric unit or date components, with its major alignment.
    private enum Alignment1 {
        case unit(Double, MajorValueAlignment<Double>?)
        case matching(DateComponents, MajorValueAlignment<Date>?)
    }

    private static func alignment(_ v: Any?) -> Alignment1? {
        guard let l = v as? [Any] else { return nil }
        func n(_ l: [Any], _ i: Int) -> Double? { i < l.count ? (l[i] as? NSNumber)?.doubleValue : nil }
        func components(_ unit: Double, _ count: Double?) -> DateComponents {
            var c = DateComponents()
            c.setValue(Int(count ?? 1), for: dateUnits[min(max(Int(unit), 0), dateUnits.count - 1)])
            return c
        }
        let major = l.count > 3 ? l[3] as? [Any] : nil
        if let unit = n(l, 1) {
            var m: MajorValueAlignment<Date>?
            if let major {
                switch Int(n(major, 0) ?? 0) {
                case 2: m = .matching(components(n(major, 1) ?? 3, n(major, 2)))
                default: m = .page
                }
            }
            return .matching(components(unit, n(l, 2)), m)
        }
        var m: MajorValueAlignment<Double>?
        if let major {
            switch Int(n(major, 0) ?? 0) {
            case 1: m = .unit(n(major, 1) ?? 1)
            default: m = .page
            }
        }
        return .unit(n(l, 0) ?? 1, m)
    }

    static func scrollTarget(_ v: AnyView, _ a: Args) -> AnyView {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return v }
        let limits: [ValueAlignedLimitBehavior] = [.automatic, .always, .never]
        let limit = limits[min(max(a.int(2), 0), 2)]
        let x = alignment(a.isNil(0) ? nil : a.list(0)), y = alignment(a.isNil(1) ? nil : a.list(1))
        let behavior: ValueAlignedChartScrollTargetBehavior
        switch (x, y) {
        case let (.unit(u, m)?, nil), let (nil, .unit(u, m)?):
            behavior = .valueAligned(unit: u, majorAlignment: m, limitBehavior: limit)
        case let (.matching(c, m)?, nil), let (nil, .matching(c, m)?):
            behavior = .valueAligned(matching: c, majorAlignment: m, limitBehavior: limit)
        case let (.unit(xu, xm)?, .unit(yu, ym)?):
            behavior = .valueAligned(xUnit: xu, yUnit: yu, xMajorAlignment: xm, yMajorAlignment: ym, limitBehavior: limit)
        case let (.matching(xc, xm)?, .matching(yc, ym)?):
            behavior = .valueAligned(xMatching: xc, yMatching: yc, xMajorAlignment: xm, yMajorAlignment: ym, limitBehavior: limit)
        case let (.matching(xc, xm)?, .unit(yu, ym)?):
            behavior = .valueAligned(xMatching: xc, yUnit: yu, xMajorAlignment: xm, yMajorAlignment: ym, limitBehavior: limit)
        case let (.unit(xu, xm)?, .matching(yc, ym)?):
            behavior = .valueAligned(xUnit: xu, yMatching: yc, xMajorAlignment: xm, yMajorAlignment: ym, limitBehavior: limit)
        case (nil, nil):
            return v
        }
        return AnyView(v.chartScrollTargetBehavior(behavior))
    }
}

// MARK: - Axis marks

private let axisPresets: [AxisMarkPreset] = [.automatic, .aligned, .extended, .inset]
private let axisPositions: [AxisMarkPosition] = [.automatic, .leading, .trailing, .top, .bottom]

@AxisContentBuilder @MainActor private func composited(_ specs: [[Any]], _ layer: Bool, _ style: NodeModel?) -> some AxisContent {
    if let style {
        axisList(specs).compositingLayer { marks in SlotView(model: style).environment(\.composeStyleContent, AnyView(marks)) }
    } else if layer {
        axisList(specs).compositingLayer()
    } else {
        axisList(specs)
    }
}

@AxisContentBuilder @MainActor private func axisList(_ specs: [[Any]]) -> some AxisContent {
    if specs.count > 0 { axisMarks(specs[0]) }
    if specs.count > 1 { axisMarks(specs[1]) }
    if specs.count > 2 { axisMarks(specs[2]) }
    if specs.count > 3 { axisMarks(specs[3]) }
}

@MainActor private func axisMarks(_ s: [Any]) -> AnyAxisContent {
    func n(_ i: Int) -> Int { i < s.count ? (s[i] as? NSNumber)?.intValue ?? 0 : 0 }
    let preset = axisPresets[min(max(n(0), 0), 3)]
    let position = axisPositions[min(max(n(1), 0), 4)]
    let values = s.count > 2 ? s[2] as? [Any] ?? [] : []
    let grid = s.count > 3 ? s[3] as? [Any] : nil
    let tick = s.count > 4 ? s[4] as? [Any] : nil
    let label = s.count > 5 ? s[5] as? [Any] : nil
    func vn(_ i: Int) -> NSNumber? { i < values.count ? values[i] as? NSNumber : nil }
    let spec = AxisMarkSpec(grid: grid, tick: tick, label: label)
    let content = { (value: AxisValue) in spec.content(value) }
    switch vn(0)?.intValue ?? 0 {
    case 1:
        return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position,
                                                 values: .stride(by: vn(1)?.doubleValue ?? 1, roundLowerBound: vn(2)?.boolValue, roundUpperBound: vn(3)?.boolValue), content: content))
    case 2:
        let unit = dateUnits[min(max(vn(1)?.intValue ?? 3, 0), dateUnits.count - 1)]
        return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position,
                                                 values: .stride(by: unit, count: vn(2)?.intValue ?? 1, roundLowerBound: vn(3)?.boolValue, roundUpperBound: vn(4)?.boolValue), content: content))
    case 3:
        let list = values.count > 2 ? values[2] as? [Any] ?? [] : []
        switch vn(1)?.intValue ?? 1 {
        case 0: return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position, values: list.compactMap { $0 as? String }, content: content))
        case 2: return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position, values: list.compactMap { ($0 as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue) } }, content: content))
        default: return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position, values: list.compactMap { ($0 as? NSNumber)?.doubleValue }, content: content))
        }
    default:
        let count = vn(1)?.intValue
        if let stride = vn(2)?.doubleValue {
            return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position,
                                                     values: .automatic(minimumStride: stride, desiredCount: count, roundLowerBound: vn(3)?.boolValue, roundUpperBound: vn(4)?.boolValue), content: content))
        }
        return AnyAxisContent(erasing: AxisMarks(preset: preset, position: position,
                                                 values: .automatic(desiredCount: count, roundLowerBound: vn(3)?.boolValue, roundUpperBound: vn(4)?.boolValue), content: content))
    }
}

@MainActor private struct AxisMarkSpec {
    let grid: [Any]?
    let tick: [Any]?
    let label: [Any]?

    private func at(_ l: [Any]?, _ i: Int) -> Any? { l.flatMap { i < $0.count ? $0[i] : nil } }
    private func bool(_ l: [Any]?, _ i: Int) -> Bool? { (at(l, i) as? NSNumber)?.boolValue }
    private func num(_ l: [Any]?, _ i: Int) -> CGFloat? { (at(l, i) as? NSNumber).map { CGFloat($0.doubleValue) } }

    @AxisMarkBuilder func content(_ value: AxisValue) -> some AxisMark {
        if grid != nil { gridMark }
        if tick != nil { tickMark }
        if label != nil { labelMark(value) }
    }

    private func offset(_ l: [Any]?, _ i: Int) -> CGSize {
        let o = (at(l, i) as? [Any])?.compactMap { ($0 as? NSNumber)?.doubleValue } ?? []
        return o.count == 2 ? CGSize(width: o[0], height: o[1]) : .zero
    }

    private func style(_ l: [Any]?, _ i: Int) -> AnyShapeStyle? { dp_ShapeStyle(at(l, i) as? SExpr).map { AnyShapeStyle($0) } }

    @AxisMarkBuilder private var gridMark: some AxisMark {
        let line = AxisGridLine(centered: bool(grid, 0), stroke: d_StrokeStyle(at(grid, 1) as? SExpr)).offset(offset(grid, 3))
        if let s = style(grid, 2) { line.foregroundStyle(s) } else { line }
    }

    @AxisMarkBuilder private var tickMark: some AxisMark {
        let t = AxisTick(centered: bool(tick, 0), length: tickLength, stroke: d_StrokeStyle(at(tick, 5) as? SExpr)).offset(offset(tick, 7))
        if let s = style(tick, 6) { t.foregroundStyle(s) } else { t }
    }

    @AxisMarkBuilder private func labelMark(_ value: AxisValue) -> some AxisMark {
        let font = d_Font(at(label, 9) as? SExpr)
        let s = style(label, 10)
        if let text = labelText(value) {
            let l = customLabel { Text(text) }.font(font).offset(offset(label, 12))
            if let s { l.foregroundStyle(s) } else { l }
        } else {
            let l = AxisValueLabel(centered: bool(label, 1), anchor: d_UnitPoint(at(label, 2) as? SExpr),
                                   multiLabelAlignment: d_Alignment(at(label, 3) as? SExpr), collisionResolution: collision,
                                   offsetsMarks: bool(label, 5), orientation: orientation, horizontalSpacing: num(label, 7),
                                   verticalSpacing: num(label, 8)).font(font).offset(offset(label, 12))
            if let s { l.foregroundStyle(s) } else { l }
        }
    }

    private var tickLength: AxisTick.Length {
        if bool(tick, 3) == true { return .longestLabel(extendPastBy: num(tick, 4) ?? 0) }
        if bool(tick, 2) == true { return .label(extendPastBy: num(tick, 4) ?? 0) }
        return .automatic
    }

    private var collision: AxisValueLabelCollisionResolution {
        let l = at(label, 4) as? [Any] ?? []
        func n(_ i: Int) -> Double { i < l.count ? (l[i] as? NSNumber)?.doubleValue ?? 0 : 0 }
        switch Int(n(0)) {
        case 1: return .disabled
        case 2: return .truncate
        case 3: return .greedy(priority: n(1), minimumSpacing: n(2))
        default: return .automatic
        }
    }

    private var orientation: AxisValueLabelOrientation {
        [.automatic, .horizontal, .vertical, .verticalReversed][min(max((at(label, 6) as? NSNumber)?.intValue ?? 0, 0), 3)]
    }

    private func labelText(_ value: AxisValue) -> String? {
        guard let provider = at(label, 0) as? AxisLabelProvider else { return nil }
        let type = (at(label, 11) as? NSNumber)?.intValue ?? 1
        let raw: Any? = value.as(String.self) ?? (type == 2 ? value.as(Date.self) as Any? : value.as(Double.self))
        return provider.label(value: kRaw(raw), index: Int32(value.index))
    }

    private func customLabel<C: View>(@ViewBuilder _ c: () -> C) -> AxisValueLabel<C> {
        AxisValueLabel(centered: bool(label, 1), anchor: d_UnitPoint(at(label, 2) as? SExpr),
                       multiLabelAlignment: d_Alignment(at(label, 3) as? SExpr), collisionResolution: collision,
                       offsetsMarks: bool(label, 5), orientation: orientation, horizontalSpacing: num(label, 7),
                       verticalSpacing: num(label, 8), content: c)
    }
}

// MARK: - 3D charts

#if os(iOS) || os(macOS) || os(visionOS)
@available(iOS 26.0, macOS 26.0, *)
private struct KotlinChart3D: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        Chart3D { Chart3DList(model: m) }
    }
}

@available(iOS 26.0, macOS 26.0, *)
@MainActor private struct Chart3DList: Chart3DContent {
    let model: NodeModel

    var body: some Chart3DContent {
        let _ = model.version
        ForEach(model.children, id: \.nodeId) { child in
            Chart3DNode(model: child)
        }
    }
}

@available(iOS 26.0, macOS 26.0, *)
@MainActor private struct Chart3DNode: Chart3DContent {
    let model: NodeModel

    var body: some Chart3DContent {
        let a = model.args
        if model.kind == 200_105 {
            let v = a.list(1).compactMap { KPlot($0) }
            let x = v.count > 0 ? v[0] : KPlot(label: "", kind: 1, raw: 0)
            let y = v.count > 1 ? v[1] : x
            let z = v.count > 2 ? v[2] : x
            switch a.int(0) {
            case 1:
                styled(RuleMark(x: .value(x.label, x.double), y: .value(y.label, y.double), z: .value(z.label, z.double)), a.list(2))
            case 2:
                styled(RectangleMark(x: .value(x.label, x.double), y: .value(y.label, y.double), z: .value(z.label, z.double)), a.list(2))
            default:
                styled(PointMark(x: .value(x.label, x.double), y: .value(y.label, y.double), z: .value(z.label, z.double)), a.list(2))
            }
        } else if model.kind == 200_106, let fn = a.raw(1) as? ChartFunction {
            let labels = a.list(0).map { $0 as? String ?? "" }
            let box = ChartFunctionBox(fn)
            surfaced(SurfacePlot(x: labels.first ?? "", y: labels.count > 1 ? labels[1] : "", z: labels.count > 2 ? labels[2] : "") { x, z in
                box.call([x, z]).first ?? .nan
            }, a.list(2))
        } else {
            Chart3DList(model: model)
        }
    }

    private func style(_ s: [Any], _ i: Int) -> Any? { i < s.count ? s[i] : nil }

    @Chart3DContentBuilder
    private func styled<C: Chart3DContent>(_ c: C, _ s: [Any]) -> some Chart3DContent {
        let fill = dp_ShapeStyle(style(s, 0) as? SExpr).map { AnyShapeStyle($0) }
        let by = KPlot(style(s, 1))
        let shape = (style(s, 3) as? NSNumber)?.intValue
        let size = (style(s, 4) as? [Any])?.compactMap { ($0 as? NSNumber)?.doubleValue }
        let metal = (style(s, 6) as? NSNumber)?.doubleValue
        let rough = (style(s, 7) as? NSNumber)?.doubleValue
        let q = (style(s, 5) as? [Any])?.compactMap { ($0 as? NSNumber)?.doubleValue }
        rotated(material(symbolized(filled(c, fill, by), shape, size), metal, rough), q)
    }

    @Chart3DContentBuilder
    private func filled<C: Chart3DContent>(_ c: C, _ fill: AnyShapeStyle?, _ by: KPlot?) -> some Chart3DContent {
        if let by {
            c.foregroundStyle(by: .value(by.label, by.kind == 2 ? "\(by.date)" : by.string))
        } else if let fill {
            c.foregroundStyle(fill)
        } else {
            c
        }
    }

    @Chart3DContentBuilder
    private func symbolized<C: Chart3DContent>(_ c: C, _ shape: Int?, _ size: [Double]?) -> some Chart3DContent {
        sized(shaped(c, shape), size)
    }

    @Chart3DContentBuilder
    private func shaped<C: Chart3DContent>(_ c: C, _ shape: Int?) -> some Chart3DContent {
        switch shape {
        case 0: c.symbol(.sphere)
        case 1: c.symbol(.cube)
        case 2: c.symbol(.cylinder)
        case 3: c.symbol(.cone)
        default: c
        }
    }

    @Chart3DContentBuilder
    private func sized<C: Chart3DContent>(_ c: C, _ size: [Double]?) -> some Chart3DContent {
        if let size, !size.isEmpty { c.symbolSize(CGFloat(size[0])) } else { c }
    }

    @Chart3DContentBuilder
    private func rotated<C: Chart3DContent>(_ c: C, _ q: [Double]?) -> some Chart3DContent {
        if let q, q.count == 4 {
            c.symbolRotation(Rotation3D(simd_quatd(ix: q[0], iy: q[1], iz: q[2], r: q[3])))
        } else {
            c
        }
    }

    @Chart3DContentBuilder
    private func material<C: Chart3DContent>(_ c: C, _ metal: Double?, _ rough: Double?) -> some Chart3DContent {
        if let metal, let rough {
            c.metalness(metal).roughness(rough)
        } else if let metal {
            c.metalness(metal)
        } else if let rough {
            c.roughness(rough)
        } else {
            c
        }
    }

    @Chart3DContentBuilder
    private func surfaced(_ c: SurfacePlot, _ s: [Any]) -> some Chart3DContent {
        let surface = style(s, 2) as? [Any]
        let gradient = surface.flatMap { $0.count > 1 ? d_Gradient($0[1] as? SExpr) : nil }
        let range = (surface.flatMap { $0.count > 2 ? $0[2] as? [Any] : nil })?.compactMap { ($0 as? NSNumber)?.doubleValue }
        if let surface, (surface.first as? NSNumber)?.intValue == 1 {
            if let gradient, let range, range.count == 2 {
                c.foregroundStyle(.heightBased(gradient, yRange: range[0]...max(range[0], range[1])))
            } else if let range, range.count == 2 {
                c.foregroundStyle(.heightBased(yRange: range[0]...max(range[0], range[1])))
            } else {
                c.foregroundStyle(.heightBased)
            }
        } else if surface != nil {
            c.foregroundStyle(.normalBased)
        } else {
            styled(c, s)
        }
    }
}

@available(iOS 26.0, macOS 26.0, *)
private struct Chart3DPoseModifier: ViewModifier {
    let a: Args
    @State private var pose: Chart3DPose = .default
    @State private var applied: [Double] = []

    func body(content: Content) -> some View {
        let l = a.list(0).compactMap { ($0 as? NSNumber)?.doubleValue }
        if a.isNil(1) {
            content.chart3DPose(l.count == 2 ? Chart3DPose(azimuth: .degrees(l[0]), inclination: .degrees(l[1])) : .default)
        } else {
            content.chart3DPose($pose)
                .onChange(of: l, initial: true) { _, v in
                    guard v.count == 2, v != applied else { return }
                    applied = v
                    pose = Chart3DPose(azimuth: .degrees(v[0]), inclination: .degrees(v[1]))
                }
                .onChange(of: pose) { _, p in
                    let v = [p.azimuth.degrees, p.inclination.degrees]
                    guard v != applied else { return }
                    applied = v
                    a.callback(1, box: { (l: [Double]) -> Any? in l.map { KotlinDouble(value: $0) } as NSArray })(v)
                }
        }
    }
}
#endif
