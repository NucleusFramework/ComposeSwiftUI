@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.native.HiddenFromObjC

// Swift Charts. Rendered by swift/ComposeSwiftUIIntegrations/Charts.swift: add that file to the app and call
// `ComposeIntegrations.registerCharts()` once at launch.

internal object ChartIds {
    const val CHART = 200_100
    const val MARK = 200_101
    const val PLOT = 200_102
    const val FUNCTION_PLOT = 200_103
    const val CHART_3D = 200_104
    const val MARK_3D = 200_105
    const val SURFACE = 200_106

    const val AXIS = 200_100
    const val AXIS_LABEL = 200_101
    const val AXIS_STYLE = 200_102
    const val PLOT_STYLE = 200_103
    const val LEGEND = 200_104
    const val SCALE = 200_105
    const val STYLE_SCALE = 200_106
    const val SELECTION = 200_107
    const val SCROLLABLE_AXES = 200_108
    const val VISIBLE_DOMAIN = 200_109
    const val SCROLL_POSITION = 200_110
    const val SCROLL_TARGET = 200_111
    const val POSE = 200_112
    const val CAMERA_PROJECTION = 200_113
}

/** A Swift Charts date: seconds since 1970. */
@HiddenFromObjC
public data class ChartDate(val epochSeconds: Double)

/** `Calendar.Component` for dates plotted by unit (bars spanning a day, a month…). */
@HiddenFromObjC
public enum class DateUnit { era, year, month, day, hour, minute, second, weekday, weekOfYear, weekOfMonth, quarter, dayOfYear }

/**
 * A Swift Charts `PlottableValue`: `.value(label, value)` where the value is a `String` (categories), a number or a
 * [ChartDate] (optionally binned by [unit]).
 */
@HiddenFromObjC
public class PlottableValue internal constructor(internal val label: String, internal val value: Any, internal val unit: DateUnit? = null) {
    internal fun encode(): List<Any?> = listOf(label, kindOf(value), rawOf(value), unit?.ordinal)

    public companion object {
        /** `PlottableValue.value(_:_:)` with a category. */
        public fun value(label: String, value: String): PlottableValue = PlottableValue(label, value)

        /** `PlottableValue.value(_:_:)` with a number. */
        public fun value(label: String, value: Number): PlottableValue = PlottableValue(label, value.toDouble())

        /** `PlottableValue.value(_:_:unit:calendar:)` with a date. */
        public fun value(label: String, value: ChartDate, unit: DateUnit? = null): PlottableValue = PlottableValue(label, value, unit)

        internal fun kindOf(v: Any): Int = when (v) {
            is String -> 0
            is ChartDate -> 2
            else -> 1
        }

        internal fun rawOf(v: Any): Any = when (v) {
            is ChartDate -> v.epochSeconds
            is Number -> v.toDouble()
            else -> v.toString()
        }

        /** Decodes a `[kind, value]` pair sent by Swift. */
        internal fun decodeRaw(v: Any?): Any? {
            val l = v as? List<*> ?: return null
            return when ((l[0] as Number).toInt()) {
                0 -> l[1] as String
                2 -> ChartDate((l[1] as Number).toDouble())
                else -> (l[1] as Number).toDouble()
            }
        }
    }
}

/** Swift Charts `MarkDimension`. */
@HiddenFromObjC
public data class MarkDimension internal constructor(internal val kind: Int, internal val value: Double = 0.0) {
    public companion object {
        public val automatic: MarkDimension = MarkDimension(0)
        public fun fixed(value: Double): MarkDimension = MarkDimension(1, value)
        public fun inset(value: Double): MarkDimension = MarkDimension(2, value)
        public fun ratio(value: Double): MarkDimension = MarkDimension(3, value)
    }

    internal fun encode(): List<Any> = listOf(kind, value)
}

/** Swift Charts `MarkStackingMethod`. */
@HiddenFromObjC
public enum class MarkStackingMethod { standard, normalized, center, unstacked }

/** Swift Charts `InterpolationMethod`. */
@HiddenFromObjC
public data class InterpolationMethod internal constructor(internal val kind: Int, internal val parameter: Double? = null) {
    public companion object {
        public val linear: InterpolationMethod = InterpolationMethod(0)
        public val monotone: InterpolationMethod = InterpolationMethod(1)
        public val catmullRom: InterpolationMethod = InterpolationMethod(2)
        public fun catmullRom(alpha: Double): InterpolationMethod = InterpolationMethod(2, alpha)
        public val cardinal: InterpolationMethod = InterpolationMethod(3)
        public fun cardinal(tension: Double): InterpolationMethod = InterpolationMethod(3, tension)
        public val stepStart: InterpolationMethod = InterpolationMethod(4)
        public val stepCenter: InterpolationMethod = InterpolationMethod(5)
        public val stepEnd: InterpolationMethod = InterpolationMethod(6)
    }
}

/** Swift Charts `BasicChartSymbolShape` (`ChartSymbolShape.circle`…), optionally stroked. */
@HiddenFromObjC
public data class ChartSymbolShape internal constructor(internal val kind: Int, internal val strokeWidth: Double? = null) {
    /** `strokeBorder(lineWidth:)`. */
    public fun strokeBorder(lineWidth: Double = 1.0): ChartSymbolShape = copy(strokeWidth = lineWidth)

    public companion object {
        public val circle: ChartSymbolShape = ChartSymbolShape(0)
        public val square: ChartSymbolShape = ChartSymbolShape(1)
        public val triangle: ChartSymbolShape = ChartSymbolShape(2)
        public val diamond: ChartSymbolShape = ChartSymbolShape(3)
        public val pentagon: ChartSymbolShape = ChartSymbolShape(4)
        public val plus: ChartSymbolShape = ChartSymbolShape(5)
        public val cross: ChartSymbolShape = ChartSymbolShape(6)
        public val asterisk: ChartSymbolShape = ChartSymbolShape(7)
    }
}

/** Swift Charts `AnnotationPosition`. */
@HiddenFromObjC
public enum class AnnotationPosition { automatic, overlay, top, bottom, leading, trailing, topLeading, topTrailing, bottomLeading, bottomTrailing }

/** Swift Charts `AnnotationOverflowResolution` for one axis. */
@HiddenFromObjC
public data class AnnotationOverflow(val strategy: Strategy = Strategy.automatic, val boundary: Boundary = Boundary.automatic) {
    public enum class Strategy { automatic, disabled, fit, padScale }
    public enum class Boundary { automatic, chart, plot }
}

/** An annotation of a mark (`annotation(position:alignment:spacing:overflowResolution:content:)`). */
@HiddenFromObjC
public data class ChartAnnotation(
    val position: AnnotationPosition = AnnotationPosition.automatic,
    val alignment: Alignment? = null,
    val spacing: Double? = null,
    val overflowX: AnnotationOverflow? = null,
    val overflowY: AnnotationOverflow? = null,
)

/** Swift Charts `position(by:axis:span:)`. */
@HiddenFromObjC
public data class ChartPositionBy(val value: PlottableValue, val axis: Axis? = null, val span: MarkDimension = MarkDimension.automatic)

/**
 * The `ChartContent` modifiers of a mark: styles by value (`foregroundStyle(by:)`, `symbol(by:)`…) put the marks in
 * the chart legend.
 */
@HiddenFromObjC
public data class ChartMarkStyle(
    val foregroundStyle: ShapeStyle? = null,
    val foregroundStyleBy: PlottableValue? = null,
    val symbol: ChartSymbolShape? = null,
    val symbolBy: PlottableValue? = null,
    val symbolSize: Double? = null,
    val symbolSizeBy: PlottableValue? = null,
    val lineStyle: StrokeStyle? = null,
    val lineStyleBy: PlottableValue? = null,
    val interpolationMethod: InterpolationMethod? = null,
    val cornerRadius: Double? = null,
    val cornerStyle: RoundedCornerStyle? = null,
    val opacity: Double? = null,
    val offsetX: Double? = null,
    val offsetY: Double? = null,
    /** `offset(xStart:xEnd:…)` / `offset(…yStart:yEnd:)`: shifts the ends of a ranged mark. */
    val offsetXStart: Double? = null,
    val offsetXEnd: Double? = null,
    val offsetYStart: Double? = null,
    val offsetYEnd: Double? = null,
    val positionBy: ChartPositionBy? = null,
    val zIndex: Double? = null,
    val blur: Double? = null,
    val shadowColor: Color? = null,
    val shadowRadius: Double? = null,
    val shadowX: Double = 0.0,
    val shadowY: Double = 0.0,
    val clipShape: Shape? = null,
    val compositingLayer: Boolean = false,
    val alignsMarkStylesWithPlotArea: Boolean? = null,
    val accessibilityLabel: String? = null,
    val accessibilityValue: String? = null,
    val accessibilityIdentifier: String? = null,
    val accessibilityHidden: Boolean? = null,
) {
    internal fun encode(): List<Any?> = listOf(
        foregroundStyle, foregroundStyleBy?.encode(), symbol?.let { listOf(it.kind, it.strokeWidth) }, symbolBy?.encode(), symbolSize,
        symbolSizeBy?.encode(), lineStyle, lineStyleBy?.encode(), interpolationMethod?.let { listOf(it.kind, it.parameter) }, cornerRadius,
        cornerStyle, opacity, listOf(offsetX, offsetY, offsetXStart, offsetXEnd, offsetYStart, offsetYEnd),
        positionBy?.let { listOf(it.value.encode(), it.axis, it.span.encode()) }, zIndex, blur,
        shadowRadius?.let { listOf(shadowColor, it, shadowX, shadowY) }, clipShape, compositingLayer, alignsMarkStylesWithPlotArea,
        listOf(accessibilityLabel, accessibilityValue, accessibilityIdentifier, accessibilityHidden),
    )
}

@Composable
private fun mark(
    kind: Int,
    values: List<PlottableValue?>,
    extra: List<Any?>,
    style: ChartMarkStyle,
    annotation: ChartAnnotation?,
    annotationContent: (@Composable () -> Unit)?,
    symbolContent: (@Composable () -> Unit)?,
    mask: (@Composable () -> Unit)?,
) {
    val slots = listOfNotNull(annotationContent, symbolContent, mask)
    var next = 0
    fun ref(c: Any?) = c?.let { SSlotRef(next++) }
    SwiftView(
        ChartIds.MARK,
        listOf(
            kind, values.map { it?.encode() }, extra, style.encode(),
            annotation?.let { listOf(it.position.ordinal, it.alignment, it.spacing,
                it.overflowX?.let { o -> listOf(o.strategy.ordinal, o.boundary.ordinal) },
                it.overflowY?.let { o -> listOf(o.strategy.ordinal, o.boundary.ordinal) }) },
            ref(annotationContent), ref(symbolContent), ref(mask),
        ),
        Modifier,
        slots = slots,
    )
}

/**
 * Swift Charts `BarMark`: give [x] and [y], or a range ([xStart]/[xEnd], [yStart]/[yEnd]) on one or both axes.
 * [annotation] places [annotationContent] next to the bar; [mask] holds chart marks.
 */
@HiddenFromObjC
@Composable
public fun BarMark(
    x: PlottableValue? = null,
    y: PlottableValue? = null,
    xStart: PlottableValue? = null,
    xEnd: PlottableValue? = null,
    yStart: PlottableValue? = null,
    yEnd: PlottableValue? = null,
    width: MarkDimension = MarkDimension.automatic,
    height: MarkDimension = MarkDimension.automatic,
    stacking: MarkStackingMethod = MarkStackingMethod.standard,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
    mask: (@Composable () -> Unit)? = null,
) = mark(0, listOf(x, y, xStart, xEnd, yStart, yEnd), listOf(width.encode(), height.encode(), stacking.ordinal), style, annotation,
    annotationContent, null, mask)

/** Swift Charts `LineMark(x:y:)` / `LineMark(x:y:series:)`. [symbol] draws a Kotlin view at each point. */
@HiddenFromObjC
@Composable
public fun LineMark(
    x: PlottableValue,
    y: PlottableValue,
    series: PlottableValue? = null,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
    symbol: (@Composable () -> Unit)? = null,
) = mark(1, listOf(x, y, series), emptyList(), style, annotation, annotationContent, symbol, null)

/** Swift Charts `PointMark(x:y:)` (one axis may be null). */
@HiddenFromObjC
@Composable
public fun PointMark(
    x: PlottableValue? = null,
    y: PlottableValue? = null,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
    symbol: (@Composable () -> Unit)? = null,
) = mark(2, listOf(x, y), emptyList(), style, annotation, annotationContent, symbol, null)

/** Swift Charts `AreaMark`: [x] with [y] or [yStart]/[yEnd], or [xStart]/[xEnd] with [y]. */
@HiddenFromObjC
@Composable
public fun AreaMark(
    x: PlottableValue? = null,
    y: PlottableValue? = null,
    xStart: PlottableValue? = null,
    xEnd: PlottableValue? = null,
    yStart: PlottableValue? = null,
    yEnd: PlottableValue? = null,
    series: PlottableValue? = null,
    stacking: MarkStackingMethod = MarkStackingMethod.standard,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
) = mark(3, listOf(x, y, xStart, xEnd, yStart, yEnd, series), listOf(stacking.ordinal), style, annotation, annotationContent, null, null)

/** Swift Charts `RuleMark`: a line at [x] or [y], or between [xStart]/[xEnd] at [y], or [yStart]/[yEnd] at [x]. */
@HiddenFromObjC
@Composable
public fun RuleMark(
    x: PlottableValue? = null,
    y: PlottableValue? = null,
    xStart: PlottableValue? = null,
    xEnd: PlottableValue? = null,
    yStart: PlottableValue? = null,
    yEnd: PlottableValue? = null,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
) = mark(4, listOf(x, y, xStart, xEnd, yStart, yEnd), emptyList(), style, annotation, annotationContent, null, null)

/** Swift Charts `RectangleMark`: centered at [x]/[y] with [width]/[height], or spanning ranges. */
@HiddenFromObjC
@Composable
public fun RectangleMark(
    x: PlottableValue? = null,
    y: PlottableValue? = null,
    xStart: PlottableValue? = null,
    xEnd: PlottableValue? = null,
    yStart: PlottableValue? = null,
    yEnd: PlottableValue? = null,
    width: MarkDimension = MarkDimension.automatic,
    height: MarkDimension = MarkDimension.automatic,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
) = mark(5, listOf(x, y, xStart, xEnd, yStart, yEnd), listOf(width.encode(), height.encode()), style, annotation, annotationContent, null, null)

/** Swift Charts `SectorMark` (pie and donut charts, iOS 17 / macOS 14). */
@HiddenFromObjC
@Composable
public fun SectorMark(
    angle: PlottableValue,
    innerRadius: MarkDimension = MarkDimension.automatic,
    outerRadius: MarkDimension = MarkDimension.automatic,
    angularInset: Double? = null,
    style: ChartMarkStyle = ChartMarkStyle(),
    annotation: ChartAnnotation? = null,
    annotationContent: (@Composable () -> Unit)? = null,
) = mark(6, listOf(angle), listOf(innerRadius.encode(), outerRadius.encode(), angularInset), style, annotation, annotationContent, null, null)

/** The vectorized plots (iOS 18 / macOS 15): one node for many points. */
@HiddenFromObjC
public enum class PlotKind { bar, line, point, area, rule, rectangle, sector }

/**
 * Swift Charts vectorized plots (`BarPlot`, `LinePlot`, `PointPlot`, `AreaPlot`, `RulePlot`, `RectanglePlot`,
 * `SectorPlot`; iOS 18 / macOS 15) over [data]: each projection maps an item to its value (labels come from the first
 * item). Use the same projections as the mark of the same [kind] (`x`/`y`, ranges, `series`).
 */
@HiddenFromObjC
@Composable
public fun <T> ChartPlot(
    kind: PlotKind,
    data: List<T>,
    x: ((T) -> PlottableValue)? = null,
    y: ((T) -> PlottableValue)? = null,
    xStart: ((T) -> PlottableValue)? = null,
    xEnd: ((T) -> PlottableValue)? = null,
    yStart: ((T) -> PlottableValue)? = null,
    yEnd: ((T) -> PlottableValue)? = null,
    series: ((T) -> PlottableValue)? = null,
    angle: ((T) -> PlottableValue)? = null,
    width: MarkDimension = MarkDimension.automatic,
    height: MarkDimension = MarkDimension.automatic,
    stacking: MarkStackingMethod = MarkStackingMethod.standard,
    innerRadius: MarkDimension = MarkDimension.automatic,
    outerRadius: MarkDimension = MarkDimension.automatic,
    angularInset: Double? = null,
    style: ChartMarkStyle = ChartMarkStyle(),
) {
    val projections = listOf(x, y, xStart, xEnd, yStart, yEnd, series, angle)
    val columns = projections.map { p ->
        p?.let { f ->
            val values = data.map(f)
            listOf(values.firstOrNull()?.label ?: "", values.firstOrNull()?.let { PlottableValue.kindOf(it.value) } ?: 1,
                values.map { PlottableValue.rawOf(it.value) }, values.firstOrNull()?.unit?.ordinal)
        }
    }
    SwiftView(ChartIds.PLOT, listOf(kind.ordinal, columns, listOf(width.encode(), height.encode(), stacking.ordinal, innerRadius.encode(),
        outerRadius.encode(), angularInset), style.encode()), Modifier)
}

/** Function plots: Swift Charts calls the Kotlin function for each sample. */
@HiddenFromObjC
public sealed class ChartFunctionPlot {
    internal abstract fun encode(): List<Any?>

    /** `LinePlot(x:y:domain:function:)`: y = [function] (x). */
    public class Line(private val x: String, private val y: String, private val domain: ClosedFloatingPointRange<Double>? = null, private val function: (Double) -> Double) : ChartFunctionPlot() {
        override fun encode() = listOf(0, listOf(x, y), domain?.let { listOf(it.start, it.endInclusive) }, fn { listOf(function(it[0])) })
    }

    /** `LinePlot(x:y:t:domain:function:)`: a parametric curve. */
    public class Parametric(private val x: String, private val y: String, private val t: String, private val domain: ClosedFloatingPointRange<Double>, private val function: (Double) -> Pair<Double, Double>) : ChartFunctionPlot() {
        override fun encode() = listOf(1, listOf(x, y, t), listOf(domain.start, domain.endInclusive), fn { function(it[0]).let { p -> listOf(p.first, p.second) } })
    }

    /** `AreaPlot(x:y:domain:function:)` (from 0) and `AreaPlot(x:yStart:yEnd:domain:function:)` (between two curves). */
    public class Area(private val x: String, private val y: String, private val domain: ClosedFloatingPointRange<Double>? = null, private val function: (Double) -> Pair<Double, Double>) : ChartFunctionPlot() {
        override fun encode() = listOf(2, listOf(x, y), domain?.let { listOf(it.start, it.endInclusive) }, fn { function(it[0]).let { p -> listOf(p.first, p.second) } })
    }

    internal fun fn(body: (List<Double>) -> List<Double>): ChartFunction = object : ChartFunction {
        override fun evaluate(input: List<Double>): List<Double> = body(input)
    }
}

/** A function plot in a [Chart] (iOS 18 / macOS 15). */
@HiddenFromObjC
@Composable
public fun FunctionPlot(plot: ChartFunctionPlot, style: ChartMarkStyle = ChartMarkStyle()) {
    SwiftView(ChartIds.FUNCTION_PLOT, listOf(plot.encode(), style.encode()), Modifier)
}

/** Reads a [Chart]: positions ↔ values in the plot area (Swift Charts `ChartProxy`). */
@HiddenFromObjC
public class ChartProxy internal constructor(private val handle: () -> ChartProxyHandle?) {
    /** `value(atX:as:)`: the x value (String, Double or [ChartDate]) at a horizontal position in the plot area. */
    public fun valueAtX(position: Double, type: PlottableType = PlottableType.number): Any? =
        PlottableValue.decodeRaw(handle()?.valueAt(0, position, type.ordinal))

    /** `value(atY:as:)`. */
    public fun valueAtY(position: Double, type: PlottableType = PlottableType.number): Any? =
        PlottableValue.decodeRaw(handle()?.valueAt(1, position, type.ordinal))

    /** `value(at:as:)`: the (x, y) values at a point of the plot area. */
    public fun valueAt(point: CGPoint, xType: PlottableType = PlottableType.number, yType: PlottableType = PlottableType.number): Pair<Any?, Any?> =
        valueAtX(point.x, xType) to valueAtY(point.y, yType)

    /** `position(forX:)`. */
    public fun positionForX(value: Any): Double? = handle()?.positionFor(0, encodeRaw(value))

    /** `position(forY:)`. */
    public fun positionForY(value: Any): Double? = handle()?.positionFor(1, encodeRaw(value))

    /** `position(for:)`: the point of an (x, y) pair. */
    public fun positionFor(x: Any, y: Any): CGPoint? =
        handle()?.positionForPoint(encodeRaw(x), encodeRaw(y))?.let { CGPoint(it[0], it[1]) }

    /** `positionRange(forX:)`: the span of a category or bin. */
    public fun positionRangeForX(value: Any): ClosedFloatingPointRange<Double>? = handle()?.positionRange(0, encodeRaw(value))?.let { it[0]..it[1] }

    /** `positionRange(forY:)`. */
    public fun positionRangeForY(value: Any): ClosedFloatingPointRange<Double>? = handle()?.positionRange(1, encodeRaw(value))?.let { it[0]..it[1] }

    /** `angle(at:)` in degrees, for sector charts. */
    public fun angleAt(point: CGPoint): Double? = handle()?.angleAt(point.x, point.y)

    /** `value(atAngle:as:)`. */
    public fun valueAtAngle(degrees: Double, type: PlottableType = PlottableType.number): Any? =
        PlottableValue.decodeRaw(handle()?.valueAtAngle(degrees, type.ordinal))

    /** The plot area frame in the chart's coordinate space (`plotFrame`). */
    public fun plotFrame(): CGRect? = handle()?.plotFrame()?.let { CGRect(it[0], it[1], it[2], it[3]) }

    /** `xDomain(dataType:)` / `yDomain(dataType:)`: the domain values (two for a numeric or date range). */
    public fun xDomain(type: PlottableType = PlottableType.number): List<Any?> = handle()?.domain(0, type.ordinal)?.map { PlottableValue.decodeRaw(it) } ?: emptyList()
    public fun yDomain(type: PlottableType = PlottableType.number): List<Any?> = handle()?.domain(1, type.ordinal)?.map { PlottableValue.decodeRaw(it) } ?: emptyList()

    /** `selectXValue(at:)` / `selectYValue(at:)`: updates the [chartXSelection] / [chartYSelection] of the chart. */
    public fun selectXValue(position: Double) { handle()?.select(0, listOf(position)) }
    public fun selectYValue(position: Double) { handle()?.select(1, listOf(position)) }

    /** `selectXRange(from:to:)` / `selectYRange(from:to:)`. */
    public fun selectXRange(from: Double, to: Double) { handle()?.select(0, listOf(from, to)) }
    public fun selectYRange(from: Double, to: Double) { handle()?.select(1, listOf(from, to)) }

    /** `selectAngleValue(at:)`. */
    public fun selectAngleValue(point: CGPoint) { handle()?.select(2, listOf(point.x, point.y)) }

    private fun encodeRaw(v: Any): List<Any> = listOf(PlottableValue.kindOf(v), PlottableValue.rawOf(v))
}

/**
 * Swift Charts `Chart { }`: [content] holds marks and plots. [overlay] and [background] are drawn over / under the
 * chart with a [ChartProxy] (`chartOverlay`, `chartBackground`); [gesture] receives it too (`chartGesture`).
 */
@HiddenFromObjC
@Composable
public fun Chart(
    modifier: Modifier = Modifier,
    overlay: (@Composable (ChartProxy) -> Unit)? = null,
    overlayAlignment: Alignment = Alignment.center,
    background: (@Composable (ChartProxy) -> Unit)? = null,
    backgroundAlignment: Alignment = Alignment.center,
    gesture: ((ChartProxy) -> GestureSpec)? = null,
    content: @Composable () -> Unit,
) {
    val holder = remember { arrayOfNulls<ChartProxyHandle>(1) }
    val proxy = remember { ChartProxy { holder[0] } }
    val slots = listOfNotNull(overlay?.let { o -> @Composable { o(proxy) } }, background?.let { b -> @Composable { b(proxy) } })
    var next = 0
    SwiftView(
        ChartIds.CHART,
        listOf(
            SValueCallback { holder[0] = it as? ChartProxyHandle },
            overlay?.let { SSlotRef(next++) }, overlayAlignment,
            background?.let { SSlotRef(next++) }, backgroundAlignment,
            gesture?.invoke(proxy)?.encode(),
        ),
        modifier,
        slots = slots,
        content = content,
    )
}

// ---- 3D charts (iOS 26 / macOS 26) ------------------------------------------------------------

/** Swift Charts `Chart3DSymbolShape`. */
@HiddenFromObjC
public enum class Chart3DSymbolShape { sphere, cube, cylinder, cone }

/** Swift Charts `Chart3DSurfaceStyle`. */
@HiddenFromObjC
public data class Chart3DSurfaceStyle internal constructor(internal val kind: Int, internal val gradient: Gradient? = null, internal val yRange: ClosedFloatingPointRange<Double>? = null) {
    public companion object {
        public val normalBased: Chart3DSurfaceStyle = Chart3DSurfaceStyle(0)
        public val heightBased: Chart3DSurfaceStyle = Chart3DSurfaceStyle(1)
        public fun heightBased(gradient: Gradient? = null, yRange: ClosedFloatingPointRange<Double>? = null): Chart3DSurfaceStyle = Chart3DSurfaceStyle(1, gradient, yRange)
    }
}

/** The `Chart3DContent` modifiers. */
@HiddenFromObjC
public data class Chart3DMarkStyle(
    val foregroundStyle: ShapeStyle? = null,
    val foregroundStyleBy: PlottableValue? = null,
    val surfaceStyle: Chart3DSurfaceStyle? = null,
    val symbol: Chart3DSymbolShape? = null,
    /** `symbolSize(_:)` in points. */
    val symbolSize: Double? = null,
    /** `symbolRotation(_:)` as a quaternion (x, y, z, w). */
    val symbolRotation: List<Double>? = null,
    val metalness: Double? = null,
    val roughness: Double? = null,
) {
    internal fun encode(): List<Any?> = listOf(foregroundStyle, foregroundStyleBy?.encode(),
        surfaceStyle?.let { listOf(it.kind, it.gradient, it.yRange?.let { r -> listOf(r.start, r.endInclusive) }) },
        symbol?.ordinal, symbolSize?.let { listOf(it) }, symbolRotation, metalness, roughness)
}

/** Swift Charts `Chart3DPose`. */
@HiddenFromObjC
public data class Chart3DPose(val azimuthDegrees: Double, val inclinationDegrees: Double) {
    public companion object {
        public val default: Chart3DPose = Chart3DPose(-30.0, 30.0)
        public val front: Chart3DPose = Chart3DPose(0.0, 0.0)
        public val back: Chart3DPose = Chart3DPose(180.0, 0.0)
        public val left: Chart3DPose = Chart3DPose(-90.0, 0.0)
        public val right: Chart3DPose = Chart3DPose(90.0, 0.0)
        public val top: Chart3DPose = Chart3DPose(0.0, 90.0)
        public val bottom: Chart3DPose = Chart3DPose(0.0, -90.0)
    }
}

/** Swift Charts `Chart3DCameraProjection`. */
@HiddenFromObjC
public enum class Chart3DCameraProjection { automatic, orthographic, perspective }

/** Swift Charts `Chart3D { }` (iOS 26 / macOS 26): holds [PointMark3D], [RuleMark3D], [RectangleMark3D], [SurfacePlot]. */
@HiddenFromObjC
@Composable
public fun Chart3D(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SwiftView(ChartIds.CHART_3D, emptyList(), modifier, content = content)
}

/** Swift Charts `PointMark(x:y:z:)` in a [Chart3D]. */
@HiddenFromObjC
@Composable
public fun PointMark3D(x: PlottableValue, y: PlottableValue, z: PlottableValue, style: Chart3DMarkStyle = Chart3DMarkStyle()) {
    SwiftView(ChartIds.MARK_3D, listOf(0, listOf(x.encode(), y.encode(), z.encode()), style.encode()), Modifier)
}

/** Swift Charts `RuleMark(x:y:z:)` in a [Chart3D]. */
@HiddenFromObjC
@Composable
public fun RuleMark3D(x: PlottableValue, y: PlottableValue, z: PlottableValue, style: Chart3DMarkStyle = Chart3DMarkStyle()) {
    SwiftView(ChartIds.MARK_3D, listOf(1, listOf(x.encode(), y.encode(), z.encode()), style.encode()), Modifier)
}

/** Swift Charts `RectangleMark(x:y:z:)` in a [Chart3D]. */
@HiddenFromObjC
@Composable
public fun RectangleMark3D(x: PlottableValue, y: PlottableValue, z: PlottableValue, style: Chart3DMarkStyle = Chart3DMarkStyle()) {
    SwiftView(ChartIds.MARK_3D, listOf(2, listOf(x.encode(), y.encode(), z.encode()), style.encode()), Modifier)
}

/** Swift Charts `SurfacePlot(x:y:z:function:)`: y = [function] (x, z). */
@HiddenFromObjC
@Composable
public fun SurfacePlot(x: String, y: String, z: String, style: Chart3DMarkStyle = Chart3DMarkStyle(), function: (Double, Double) -> Double) {
    val fn = object : ChartFunction {
        override fun evaluate(input: List<Double>): List<Double> = listOf(function(input[0], input[1]))
    }
    SwiftView(ChartIds.SURFACE, listOf(listOf(x, y, z), fn, style.encode()), Modifier)
}

// ---- axes -----------------------------------------------------------------------------------------

/** Swift Charts `AxisMarkPreset`. */
@HiddenFromObjC
public enum class AxisMarkPreset { automatic, aligned, extended, inset }

/** Swift Charts `AxisMarkPosition`. */
@HiddenFromObjC
public enum class AxisMarkPosition { automatic, leading, trailing, top, bottom }

/** Swift Charts `AxisMarkValues`. */
@HiddenFromObjC
public sealed class AxisMarkValues {
    internal abstract fun encode(): List<Any?>

    /** `.automatic(minimumStride:desiredCount:roundLowerBound:roundUpperBound:)`. */
    public data class Automatic(val desiredCount: Int? = null, val minimumStride: Double? = null, val roundLowerBound: Boolean? = null, val roundUpperBound: Boolean? = null) : AxisMarkValues() {
        override fun encode() = listOf(0, desiredCount, minimumStride, roundLowerBound, roundUpperBound)
    }

    /** `.stride(by:roundLowerBound:roundUpperBound:)` for numbers. */
    public data class Stride(val step: Double, val roundLowerBound: Boolean? = null, val roundUpperBound: Boolean? = null) : AxisMarkValues() {
        override fun encode() = listOf(1, step, roundLowerBound, roundUpperBound)
    }

    /** `.stride(by:count:roundLowerBound:roundUpperBound:calendar:)` for dates. */
    public data class DateStride(val unit: DateUnit, val count: Int = 1, val roundLowerBound: Boolean? = null, val roundUpperBound: Boolean? = null) : AxisMarkValues() {
        override fun encode() = listOf(2, unit.ordinal, count, roundLowerBound, roundUpperBound)
    }

    /** Explicit values (Strings, numbers or [ChartDate]s). */
    public data class Values(val values: List<Any>) : AxisMarkValues() {
        override fun encode() = listOf(3, values.firstOrNull()?.let { PlottableValue.kindOf(it) } ?: 1, values.map { PlottableValue.rawOf(it) })
    }

    public companion object {
        public val automatic: AxisMarkValues = Automatic()
    }
}

/** Swift Charts `AxisGridLine`. */
@HiddenFromObjC
public data class AxisGridLine(
    val centered: Boolean? = null,
    val stroke: StrokeStyle? = null,
    val foregroundStyle: ShapeStyle? = null,
    /** `AxisMark.offset(x:y:)`. */
    val offset: CGSize? = null,
)

/** Swift Charts `AxisTick`. */
@HiddenFromObjC
public data class AxisTick(
    val centered: Boolean? = null,
    /** `AxisTick.Length`: null = automatic, a number = fixed length, or [labelLength] / [longestLabel]. */
    val length: Double? = null,
    val labelLength: Boolean = false,
    val longestLabel: Boolean = false,
    val extendPastBy: Double = 0.0,
    val stroke: StrokeStyle? = null,
    val foregroundStyle: ShapeStyle? = null,
    /** `AxisMark.offset(x:y:)`. */
    val offset: CGSize? = null,
)

/** Swift Charts `AxisValueLabelOrientation`. */
@HiddenFromObjC
public enum class AxisValueLabelOrientation { automatic, horizontal, vertical, verticalReversed }

/** Swift Charts `AxisValueLabelCollisionResolution`. */
@HiddenFromObjC
public data class AxisValueLabelCollisionResolution internal constructor(internal val kind: Int, internal val priority: Double = 0.0, internal val minimumSpacing: Double = 0.0) {
    public companion object {
        public val automatic: AxisValueLabelCollisionResolution = AxisValueLabelCollisionResolution(0)
        public val disabled: AxisValueLabelCollisionResolution = AxisValueLabelCollisionResolution(1)
        public val truncate: AxisValueLabelCollisionResolution = AxisValueLabelCollisionResolution(2)
        public val greedy: AxisValueLabelCollisionResolution = AxisValueLabelCollisionResolution(3)
        public fun greedy(priority: Double = 0.0, minimumSpacing: Double = 0.0): AxisValueLabelCollisionResolution =
            AxisValueLabelCollisionResolution(3, priority, minimumSpacing)
    }
}

/**
 * Swift Charts `AxisValueLabel`: [text] maps each axis value (String, Double or [ChartDate]) and its index to the
 * label (null = the default label).
 */
@HiddenFromObjC
public data class AxisValueLabel(
    val text: ((value: Any?, index: Int) -> String?)? = null,
    val centered: Boolean? = null,
    val anchor: UnitPoint? = null,
    val multiLabelAlignment: Alignment? = null,
    val collisionResolution: AxisValueLabelCollisionResolution = AxisValueLabelCollisionResolution.automatic,
    val offsetsMarks: Boolean? = null,
    val orientation: AxisValueLabelOrientation = AxisValueLabelOrientation.automatic,
    val horizontalSpacing: Double? = null,
    val verticalSpacing: Double? = null,
    val font: Font? = null,
    val foregroundStyle: ShapeStyle? = null,
    /** The axis values' type: Strings are recognized, numbers are the default, set [PlottableType.date] for dates. */
    val valueType: PlottableType = PlottableType.number,
    /** `AxisMark.offset(x:y:)`. */
    val offset: CGSize? = null,
)

/** Swift Charts `AxisMarks(preset:position:values:content:)` with its grid line, tick and value label. */
@HiddenFromObjC
public data class AxisMarks(
    val preset: AxisMarkPreset = AxisMarkPreset.automatic,
    val position: AxisMarkPosition = AxisMarkPosition.automatic,
    val values: AxisMarkValues = AxisMarkValues.automatic,
    val gridLine: AxisGridLine? = AxisGridLine(),
    val tick: AxisTick? = AxisTick(),
    val valueLabel: AxisValueLabel? = AxisValueLabel(),
) {
    internal fun encode(): List<Any?> = listOf(
        preset.ordinal, position.ordinal, values.encode(),
        gridLine?.let { listOf(it.centered, it.stroke, it.foregroundStyle, it.offset?.let { o -> listOf(o.width, o.height) }) },
        tick?.let { listOf(it.centered, it.length, it.labelLength, it.longestLabel, it.extendPastBy, it.stroke, it.foregroundStyle, it.offset?.let { o -> listOf(o.width, o.height) }) },
        valueLabel?.let { l ->
            listOf(
                l.text?.let { f -> object : AxisLabelProvider {
                    override fun label(value: Any?, index: Int): String? = f(PlottableValue.decodeRaw(value), index)
                } },
                l.centered, l.anchor, l.multiLabelAlignment, listOf(l.collisionResolution.kind, l.collisionResolution.priority, l.collisionResolution.minimumSpacing),
                l.offsetsMarks, l.orientation.ordinal, l.horizontalSpacing, l.verticalSpacing, l.font, l.foregroundStyle, l.valueType.ordinal, l.offset?.let { o -> listOf(o.width, o.height) },
            )
        },
    )
}

/** Axis of a chart modifier. */
@HiddenFromObjC
public enum class ChartAxis { x, y, z }

/** `chartXAxis(_:)` / `chartYAxis(_:)` / `chartZAxis(_:)` visibility. */
@HiddenFromObjC
public fun Modifier.chartAxis(axis: ChartAxis, visibility: Visibility): Modifier = element(ChartIds.AXIS, axis.ordinal, visibility, null, false, null)

/**
 * `chartXAxis { AxisMarks… }` (and y, z): the axis marks of [axis]. [compositingLayer] renders them as one layer
 * (`AxisContent.compositingLayer()`); [compositingStyle] places [StyleContent] (the marks) with modifiers
 * (`compositingLayer(style:)`).
 */
@HiddenFromObjC
public fun Modifier.chartAxis(
    axis: ChartAxis,
    marks: List<AxisMarks>,
    compositingLayer: Boolean = false,
    compositingStyle: (@Composable () -> Unit)? = null,
): Modifier = element(ChartIds.AXIS, axis.ordinal, null, marks.map { it.encode() }, compositingLayer, compositingStyle?.let { slotContent(it) })

/** `chartXAxisLabel(position:alignment:spacing:content:)` (and y, z). */
@HiddenFromObjC
public fun Modifier.chartAxisLabel(
    axis: ChartAxis,
    position: AnnotationPosition = AnnotationPosition.automatic,
    alignment: Alignment? = null,
    spacing: Double? = null,
    content: @Composable () -> Unit,
): Modifier = element(ChartIds.AXIS_LABEL, axis.ordinal, position.ordinal, alignment, spacing, slotContent(content), null)

/** `chartXAxisLabel(_:)` with a text (the only form of the z axis). */
@HiddenFromObjC
public fun Modifier.chartAxisLabel(
    axis: ChartAxis,
    text: String,
    position: AnnotationPosition = AnnotationPosition.automatic,
    alignment: Alignment? = null,
    spacing: Double? = null,
): Modifier = element(ChartIds.AXIS_LABEL, axis.ordinal, position.ordinal, alignment, spacing, slotContent { Text(text) }, text)

/** `chartXAxisStyle { axis in }` / `chartYAxisStyle`: [content] places [StyleContent] (the axis). */
@HiddenFromObjC
public fun Modifier.chartAxisStyle(axis: ChartAxis, content: @Composable () -> Unit): Modifier =
    element(ChartIds.AXIS_STYLE, axis.ordinal, slotContent(content))

/** `chartPlotStyle { plotArea in }`: [content] places [StyleContent] (the plot area) with its modifiers. */
@HiddenFromObjC
public fun Modifier.chartPlotStyle(content: @Composable () -> Unit): Modifier = element(ChartIds.PLOT_STYLE, slotContent(content))

/** `chartLegend(_:)`. */
@HiddenFromObjC
public fun Modifier.chartLegend(visibility: Visibility): Modifier = element(ChartIds.LEGEND, visibility, null, null, null, null)

/** `chartLegend(position:alignment:spacing:content:)`; [content] replaces the legend when given. */
@HiddenFromObjC
public fun Modifier.chartLegend(
    position: AnnotationPosition = AnnotationPosition.automatic,
    alignment: Alignment? = null,
    spacing: Double? = null,
    content: (@Composable () -> Unit)? = null,
): Modifier = element(ChartIds.LEGEND, null, position.ordinal, alignment, spacing, content?.let { slotContent(it) })

/** Swift Charts `ScaleType`. */
@HiddenFromObjC
public data class ScaleType internal constructor(internal val kind: Int, internal val parameter: Double? = null) {
    public companion object {
        public val linear: ScaleType = ScaleType(0)
        public val log: ScaleType = ScaleType(1)
        public val squareRoot: ScaleType = ScaleType(2)
        public val power: ScaleType = ScaleType(3)
        public fun power(exponent: Double): ScaleType = ScaleType(3, exponent)
        public val symmetricLog: ScaleType = ScaleType(4)
        public fun symmetricLog(slopeAtZero: Double): ScaleType = ScaleType(4, slopeAtZero)
        public val date: ScaleType = ScaleType(5)
        public val category: ScaleType = ScaleType(6)
    }
}

/** A scale domain: automatic, a numeric or date range, or categories. */
@HiddenFromObjC
public sealed class ScaleDomain {
    internal abstract fun encode(): List<Any?>

    /** `ScaleDomain.automatic(includesZero:reversed:)`. */
    public data class Automatic(val includesZero: Boolean? = null, val reversed: Boolean? = null) : ScaleDomain() {
        override fun encode() = listOf(0, includesZero, reversed)
    }

    /** A numeric range. */
    public data class Range(val range: ClosedFloatingPointRange<Double>) : ScaleDomain() {
        override fun encode() = listOf(1, range.start, range.endInclusive)
    }

    /** A date range. */
    public data class Dates(val start: ChartDate, val end: ChartDate) : ScaleDomain() {
        override fun encode() = listOf(2, start.epochSeconds, end.epochSeconds)
    }

    /** Categories, in order. */
    public data class Categories(val values: List<String>) : ScaleDomain() {
        override fun encode() = listOf(3, values)
    }
}

/** Swift Charts `PositionScaleRange`: the plot dimension with optional padding. */
@HiddenFromObjC
public data class PositionScaleRange(val startPadding: Double = 0.0, val endPadding: Double = 0.0)

/** `chartXScale(domain:range:type:)` (and y, z). */
@HiddenFromObjC
public fun Modifier.chartScale(axis: ChartAxis, domain: ScaleDomain? = null, range: PositionScaleRange? = null, type: ScaleType? = null): Modifier =
    element(ChartIds.SCALE, axis.ordinal, domain?.encode(), range?.let { listOf(it.startPadding, it.endPadding) }, type?.let { listOf(it.kind, it.parameter) })

/** `chartForegroundStyleScale(domain:range:type:)` / `chartForegroundStyleScale(_ mapping)`: categories → styles. */
@HiddenFromObjC
public fun Modifier.chartForegroundStyleScale(mapping: List<Pair<String, ShapeStyle>> = emptyList(), type: ScaleType? = null): Modifier =
    element(ChartIds.STYLE_SCALE, 0, mapping.map { it.first }, mapping.map { it.second }, type?.let { listOf(it.kind, it.parameter) })

/** `chartSymbolScale(_ mapping)`: categories → symbols. */
@HiddenFromObjC
public fun Modifier.chartSymbolScale(mapping: List<Pair<String, ChartSymbolShape>>): Modifier =
    element(ChartIds.STYLE_SCALE, 1, mapping.map { it.first }, mapping.map { listOf(it.second.kind, it.second.strokeWidth) }, null)

/** `chartSymbolSizeScale(_ mapping)` / `chartSymbolSizeScale(domain:range:type:)`. */
@HiddenFromObjC
public fun Modifier.chartSymbolSizeScale(mapping: List<Pair<String, Double>> = emptyList(), type: ScaleType? = null): Modifier =
    element(ChartIds.STYLE_SCALE, 2, mapping.map { it.first }, mapping.map { it.second }, type?.let { listOf(it.kind, it.parameter) })

/** `chartLineStyleScale(_ mapping)`: categories → stroke styles. */
@HiddenFromObjC
public fun Modifier.chartLineStyleScale(mapping: List<Pair<String, StrokeStyle>>): Modifier =
    element(ChartIds.STYLE_SCALE, 3, mapping.map { it.first }, mapping.map { it.second }, null)

/** The type of the values on a chart axis. */
@HiddenFromObjC
public enum class PlottableType { string, number, date }

/**
 * `chartXSelection(value:)` / `chartYSelection(value:)` / `chartZSelection(value:)` / `chartAngleSelection(value:)`:
 * the selected value ([type]: String, Double or [ChartDate]), driven by a drag or tap on the chart.
 */
@HiddenFromObjC
public fun Modifier.chartSelection(axis: ChartSelectionAxis, type: PlottableType, value: Any?, onValueChange: (Any?) -> Unit): Modifier =
    element(ChartIds.SELECTION, axis.ordinal, 0, type.ordinal, value?.let { PlottableValue.rawOf(it) },
        SValueCallback { onValueChange(PlottableValue.decodeRaw(it)) })

/** `chartXSelection(range:)` / `chartYSelection(range:)`: a two-finger (or drag) range selection. */
@HiddenFromObjC
public fun Modifier.chartSelection(axis: ChartSelectionAxis, type: PlottableType, range: Pair<Any, Any>?, onRangeChange: (Pair<Any, Any>?) -> Unit): Modifier =
    element(ChartIds.SELECTION, axis.ordinal, 1, type.ordinal, range?.let { listOf(PlottableValue.rawOf(it.first), PlottableValue.rawOf(it.second)) },
        SValueCallback { v ->
            val l = v as? List<*>
            onRangeChange(l?.let { PlottableValue.decodeRaw(listOf(it[0], it[1]))!! to PlottableValue.decodeRaw(listOf(it[0], it[2]))!! })
        })

/** The axis of a [chartSelection]. */
@HiddenFromObjC
public enum class ChartSelectionAxis { x, y, z, angle }

/** `chartScrollableAxes(_:)`. */
@HiddenFromObjC
public fun Modifier.chartScrollableAxes(axes: Axis.Set): Modifier = element(ChartIds.SCROLLABLE_AXES, axes)

/** `chartXVisibleDomain(length:)` / `chartYVisibleDomain(length:)` (seconds for a date axis). */
@HiddenFromObjC
public fun Modifier.chartVisibleDomain(axis: ChartAxis, length: Double): Modifier = element(ChartIds.VISIBLE_DOMAIN, axis.ordinal, length, 1)

/** `chartXVisibleDomain(length:)` with a number of categories. */
@HiddenFromObjC
public fun Modifier.chartVisibleDomain(axis: ChartAxis, categories: Int): Modifier = element(ChartIds.VISIBLE_DOMAIN, axis.ordinal, categories.toDouble(), 0)

/**
 * `chartScrollPosition(x:)` / `chartScrollPosition(y:)` (with [onValueChange]) or `chartScrollPosition(initialX:)` /
 * `(initialY:)` (without): the leading visible value of a scrollable chart.
 */
@HiddenFromObjC
public fun Modifier.chartScrollPosition(axis: ChartAxis, value: Any, onValueChange: ((Any) -> Unit)? = null): Modifier =
    element(ChartIds.SCROLL_POSITION, axis.ordinal, listOf(PlottableValue.kindOf(value), PlottableValue.rawOf(value)),
        onValueChange?.let { cb -> SValueCallback { v -> PlottableValue.decodeRaw(v)?.let(cb) } })

/** Swift Charts `MajorValueAlignment`: how far a flick scrolls. */
@HiddenFromObjC
public sealed class MajorValueAlignment {
    internal abstract fun encode(): List<Any?>

    /** `.page`. */
    public data object Page : MajorValueAlignment() { override fun encode() = listOf(0) }

    /** `.unit(_:)`: a multiple of a number (numeric axis). */
    public data class Unit(val step: Double) : MajorValueAlignment() { override fun encode() = listOf(1, step) }

    /** `.matching(_:)`: a calendar boundary (date axis). */
    public data class Matching(val unit: DateUnit, val count: Int = 1) : MajorValueAlignment() { override fun encode() = listOf(2, unit.ordinal, count) }
}

/**
 * The value alignment of one axis for `chartScrollTargetBehavior(.valueAligned(…))`: multiples of [unit] on a
 * numeric axis, or calendar boundaries ([matching], every [count]) on a date axis.
 */
@HiddenFromObjC
public data class ValueAlignment(
    val unit: Double? = null,
    val matching: DateUnit? = null,
    val count: Int = 1,
    val majorAlignment: MajorValueAlignment? = null,
) {
    internal fun encode(): List<Any?> = listOf(unit, matching?.ordinal, count, majorAlignment?.encode())
}

/** Swift Charts `ValueAlignedLimitBehavior`. */
@HiddenFromObjC
public enum class ValueAlignedLimitBehavior { automatic, always, never }

/**
 * `chartScrollTargetBehavior(.valueAligned(…))`: scrolling snaps to [x] and/or [y] alignments
 * (`valueAligned(unit:)`, `(matching:)`, `(xUnit:yUnit:)`, `(xMatching:yMatching:)`…).
 */
@HiddenFromObjC
public fun Modifier.chartScrollTargetBehavior(
    x: ValueAlignment? = null,
    y: ValueAlignment? = null,
    limitBehavior: ValueAlignedLimitBehavior = ValueAlignedLimitBehavior.automatic,
): Modifier = element(ChartIds.SCROLL_TARGET, x?.encode(), y?.encode(), limitBehavior.ordinal)

/** `chart3DPose(_:)` with an optional binding (iOS 26 / macOS 26): the user rotates the chart when [onPoseChange] is set. */
@HiddenFromObjC
public fun Modifier.chart3DPose(pose: Chart3DPose, onPoseChange: ((Chart3DPose) -> Unit)? = null): Modifier =
    element(ChartIds.POSE, listOf(pose.azimuthDegrees, pose.inclinationDegrees), onPoseChange?.let { cb ->
        SValueCallback { v -> (v as List<*>).let { cb(Chart3DPose((it[0] as Number).toDouble(), (it[1] as Number).toDouble())) } }
    })

/** `chart3DCameraProjection(_:)`. */
@HiddenFromObjC
public fun Modifier.chart3DCameraProjection(projection: Chart3DCameraProjection): Modifier = element(ChartIds.CAMERA_PROJECTION, projection.ordinal)
