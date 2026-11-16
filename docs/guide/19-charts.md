<!-- group: Apple frameworks -->
# Charts

> Draw Swift Charts from Kotlin: marks and plottable values, series and stacking, axes, scales, legends, selection, scrolling, function plots, 3D charts and the chart proxy.

Swift Charts is available from Kotlin with its own vocabulary: `Chart`, `BarMark`, `LineMark`, `PlottableValue`,
`AxisMarks`. You write the marks in a Kotlin loop over your data; Swift Charts lays them out, animates them and draws
the axes and the legend.

Charts is an opt-in integration (see [Apple frameworks](apple-frameworks.md)): add
`ComposeSwiftUIIntegrations/Charts.swift` to your target and register it at launch.

```swift
@main
struct MyApp: App {
    init() { ComposeIntegrations.registerCharts() }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

Charts work on iOS 17, macOS 14, tvOS 17 and watchOS 10. Function plots need iOS 18 / macOS 15, and 3D charts iOS 26 /
macOS 26.

## A first chart

```kotlin
data class MonthlySales(val month: String, val sales: Double)

@Composable
fun SalesChart(data: List<MonthlySales>) {
    Chart(modifier = Modifier.frame(height = 220.0)) {
        for (item in data) {
            BarMark(
                x = PlottableValue.value("Month", item.month),
                y = PlottableValue.value("Sales", item.sales),
            )
        }
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `Chart(data) { BarMark(x: .value("Month", $0.month), y: .value("Sales", $0.sales)) }` | `Chart { for (d in data) BarMark(x = PlottableValue.value("Month", d.month), y = PlottableValue.value("Sales", d.sales)) }` |
| `.foregroundStyle(by: .value("Product", p))` | `style = ChartMarkStyle(foregroundStyleBy = PlottableValue.value("Product", p))` |
| `.annotation(position: .top) { Text("…") }` | `annotation = ChartAnnotation(AnnotationPosition.top), annotationContent = { Text("…") }` |
| `.chartXAxis { AxisMarks(…) }` | `Modifier.chartAxis(ChartAxis.x, listOf(AxisMarks(…)))` |
| `.chartYScale(domain: 0...100)` | `Modifier.chartScale(ChartAxis.y, domain = ScaleDomain.Range(0.0..100.0))` |
| `.chartXSelection(value: $month)` | `Modifier.chartSelection(ChartSelectionAxis.x, PlottableType.string, month) { month = it as String? }` |

Mark modifiers in Swift Charts (`foregroundStyle`, `symbol`, `annotation`…) apply to chart content, not to views, so
in Kotlin they are parameters of the mark: `style` (a `ChartMarkStyle`) and `annotation`. Chart modifiers
(`chartXAxis`, `chartLegend`…) are regular `Modifier`s on the `Chart`.

## Plottable values

A `PlottableValue` is a label and a value. The value's type decides the kind of axis:

| Kotlin value | Axis |
|---|---|
| `String` | categorical: one slot per distinct value, in order of appearance |
| any `Number` (sent as a `Double`) | numeric |
| `ChartDate(epochSeconds)` | dates; add `unit = DateUnit.day` (or `month`, `hour`…) to bin by calendar unit |

```kotlin
data class Steps(val day: ChartDate, val count: Int)

@Composable
fun StepsChart(days: List<Steps>) {
    Chart(modifier = Modifier.frame(height = 200.0)) {
        for (d in days) {
            BarMark(
                x = PlottableValue.value("Day", d.day, unit = DateUnit.day),
                y = PlottableValue.value("Steps", d.count),
            )
        }
    }
}
```

With a unit, each bar spans the whole day and the axis shows calendar labels. Without it, a date is a point in time.
`ChartDate` is seconds since 1970; convert from `kotlinx.datetime.Instant` with `ChartDate(instant.epochSeconds.toDouble())`.

## Marks

All the Swift Charts marks are there, with the same parameters:

| Mark | Use |
|---|---|
| `BarMark(x, y)` | bars; ranges with `xStart`/`xEnd` or `yStart`/`yEnd` (Gantt charts, floating bars) |
| `LineMark(x, y, series)` | lines; one line per `series` value |
| `PointMark(x, y)` | scatter plots; one axis may be null |
| `AreaMark(x, y)` | areas; `yStart`/`yEnd` for bands |
| `RuleMark(x = …)` / `RuleMark(y = …)` | a vertical or horizontal line (thresholds, averages, the selection) |
| `RectangleMark(x, y)` | heat maps; ranges for arbitrary rectangles |
| `SectorMark(angle)` | pie and donut charts |

Marks combine freely in one chart:

```kotlin
data class Reading(val hour: Int, val value: Double)

@Composable
fun ReadingsChart(readings: List<Reading>, target: Double) {
    Chart(modifier = Modifier.frame(height = 200.0)) {
        for (r in readings) {
            AreaMark(
                x = PlottableValue.value("Hour", r.hour),
                y = PlottableValue.value("Value", r.value),
                style = ChartMarkStyle(foregroundStyle = Color.blue.opacity(0.15)),
            )
            LineMark(
                x = PlottableValue.value("Hour", r.hour),
                y = PlottableValue.value("Value", r.value),
                style = ChartMarkStyle(interpolationMethod = InterpolationMethod.catmullRom, symbol = ChartSymbolShape.circle),
            )
        }
        RuleMark(
            y = PlottableValue.value("Target", target),
            style = ChartMarkStyle(foregroundStyle = Color.red, lineStyle = StrokeStyle(lineWidth = 1.0, dash = listOf(4.0, 4.0))),
            annotation = ChartAnnotation(AnnotationPosition.top, alignment = Alignment.leading),
            annotationContent = { Text("Target", modifier = Modifier.font(Font.caption)) },
        )
    }
}
```

`ChartMarkStyle` holds every mark modifier: `foregroundStyle`, `symbol`, `symbolSize`, `lineStyle`,
`interpolationMethod`, `cornerRadius`, `opacity`, `offsetX`/`offsetY`, `positionBy`, `zIndex`, `blur`, shadow,
`clipShape` and the accessibility label and value. `LineMark` and `PointMark` also take a `symbol` composable to draw a
Kotlin view at each point.

### Donuts

```kotlin
data class Share(val product: String, val revenue: Double)

@Composable
fun RevenueDonut(shares: List<Share>) {
    Chart(modifier = Modifier.frame(width = 220.0, height = 220.0)) {
        for (s in shares) {
            SectorMark(
                angle = PlottableValue.value("Revenue", s.revenue),
                innerRadius = MarkDimension.ratio(0.6),
                angularInset = 1.5,
                style = ChartMarkStyle(foregroundStyleBy = PlottableValue.value("Product", s.product), cornerRadius = 4.0),
            )
        }
    }
}
```

## Series, colors and stacking

Styles "by value" (`foregroundStyleBy`, `symbolBy`, `symbolSizeBy`, `lineStyleBy`) split the marks into series and add
them to the legend. Bars of the same x with different series stack by default:

```kotlin
data class Sale(val month: String, val product: String, val units: Int)

@Composable
fun StackedSales(sales: List<Sale>, grouped: Boolean) {
    Chart(
        modifier = Modifier
            .frame(height = 240.0)
            .chartForegroundStyleScale(listOf("Coffee" to Color.brown, "Tea" to Color.green, "Juice" to Color.orange)),
    ) {
        for (s in sales) {
            BarMark(
                x = PlottableValue.value("Month", s.month),
                y = PlottableValue.value("Units", s.units),
                style = ChartMarkStyle(
                    foregroundStyleBy = PlottableValue.value("Product", s.product),
                    positionBy = if (grouped) ChartPositionBy(PlottableValue.value("Product", s.product)) else null,
                ),
            )
        }
    }
}
```

- `stacking = MarkStackingMethod.normalized` on `BarMark` / `AreaMark` stacks to 100%, `center` makes a streamgraph,
  `unstacked` overlaps the marks.
- `positionBy` places the series side by side (grouped bars) instead of stacking them.
- For lines, `foregroundStyleBy` already draws one line per value; use `series` to split lines without changing their
  color.
- `chartForegroundStyleScale`, `chartSymbolScale`, `chartSymbolSizeScale` and `chartLineStyleScale` map the series
  values to explicit styles. Without them, Swift Charts picks the colors.

## Axes

`Modifier.chartAxis(axis, marks)` replaces the axis marks of `ChartAxis.x`, `y` or `z`. Each `AxisMarks` has a preset,
a position, the values where marks go, and an optional grid line, tick and value label (pass `null` to remove one):

```kotlin
data class PricePoint(val x: Double, val y: Double)

@Composable
fun PriceChart(points: List<PricePoint>) {
    Chart(
        modifier = Modifier
            .frame(height = 200.0)
            .chartAxis(
                ChartAxis.y,
                listOf(
                    AxisMarks(
                        position = AxisMarkPosition.leading,
                        values = AxisMarkValues.Stride(25.0),
                        valueLabel = AxisValueLabel(text = { value, _ -> "${(value as Double).toInt()} €" }),
                    ),
                ),
            )
            .chartAxis(ChartAxis.x, listOf(AxisMarks(values = AxisMarkValues.Automatic(desiredCount = 4), gridLine = null)))
            .chartAxisLabel(ChartAxis.y, "Price"),
    ) {
        for (p in points) LineMark(x = PlottableValue.value("Day", p.x), y = PlottableValue.value("Price", p.y))
    }
}
```

- Values: `AxisMarkValues.automatic`, `Automatic(desiredCount = …)`, `Stride(step)` for numbers,
  `DateStride(DateUnit.month)` for dates, or explicit `Values(listOf(…))`.
- Labels from Kotlin: `AxisValueLabel(text = { value, index -> … })` receives the value (`String`, `Double` or
  `ChartDate`, as set by `valueType`) and returns the label, or `null` to keep the default one. The default labels are
  formatted by Swift Charts in the user's locale (dates included), so only override them when you need to.
- `AxisGridLine` and `AxisTick` take a stroke and a foreground style; `AxisValueLabel` takes a font, a foreground
  style, an orientation and a collision resolution.
- `Modifier.chartAxis(ChartAxis.x, Visibility.hidden)` hides an axis.
- `Modifier.chartAxisLabel(axis, text)` titles an axis; the overload with a content lambda takes any view.
- `Modifier.chartAxisStyle(axis) { … }` and `Modifier.chartPlotStyle { … }` style the axis or the plot area: place
  `StyleContent` (the area itself) and wrap it with modifiers.

```kotlin
Chart(
    modifier = Modifier.chartPlotStyle {
        StyleContent(modifier = Modifier.background(Color.gray.opacity(0.1)).border(Color.gray, width = 1.0))
    },
) {
    PointMark(x = PlottableValue.value("x", 1.0), y = PlottableValue.value("y", 2.0))
}
```

## Scales

```kotlin
Modifier
    .chartScale(ChartAxis.y, domain = ScaleDomain.Range(0.0..100.0))
    .chartScale(ChartAxis.x, domain = ScaleDomain.Categories(listOf("Mon", "Tue", "Wed", "Thu", "Fri")))
    .chartScale(ChartAxis.y, type = ScaleType.log)
```

`ScaleDomain` is `Automatic(includesZero, reversed)`, a numeric `Range`, a date range (`Dates(start, end)`) or ordered
`Categories`. `PositionScaleRange(startPadding, endPadding)` adds padding inside the plot. `ScaleType` covers `linear`,
`log`, `squareRoot`, `power(exponent)`, `symmetricLog`, `date` and `category`.

## Legend

The legend appears as soon as a mark uses a style "by value". Move it or replace it:

```kotlin
Modifier.chartLegend(position = AnnotationPosition.bottom, alignment = Alignment.center, spacing = 12.0)
```

`Modifier.chartLegend(Visibility.hidden)` hides it; pass `content` to draw your own legend.

## Selection

`Modifier.chartSelection` connects the chart's built-in selection gesture (a drag on iOS, hover and click on macOS) to
Kotlin state. Give the type of the axis values so the selection comes back as a `String`, a `Double` or a
`ChartDate`:

```kotlin
@Composable
fun SelectableSales(data: List<MonthlySales>) {
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    Chart(
        modifier = Modifier
            .frame(height = 220.0)
            .chartSelection(ChartSelectionAxis.x, PlottableType.string, selectedMonth) { selectedMonth = it as String? },
    ) {
        for (item in data) {
            BarMark(
                x = PlottableValue.value("Month", item.month),
                y = PlottableValue.value("Sales", item.sales),
                style = ChartMarkStyle(opacity = if (selectedMonth == null || selectedMonth == item.month) 1.0 else 0.4),
            )
        }
        val selected = data.firstOrNull { it.month == selectedMonth }
        if (selected != null) {
            RuleMark(
                x = PlottableValue.value("Month", selected.month),
                style = ChartMarkStyle(foregroundStyle = Color.gray.opacity(0.3), zIndex = -1.0),
                annotation = ChartAnnotation(
                    AnnotationPosition.top,
                    overflowY = AnnotationOverflow(AnnotationOverflow.Strategy.fit),
                ),
                annotationContent = { Text("${selected.sales}", modifier = Modifier.font(Font.headline)) },
            )
        }
    }
}
```

- On iOS the selection goes back to `null` when the finger lifts; keep your own copy if it should stick.
- A numeric axis reports the exact value under the finger, not the nearest data point: find the closest item yourself.
- The range overload (`chartSelection(axis, type, range) { … }`) selects an interval with a two-finger drag.
- `ChartSelectionAxis.angle` selects in a `SectorMark` chart. Its value is the cumulative angle value: walk your data,
  summing, to find the sector.
- `ChartSelectionAxis.z` is for 3D charts (iOS 26 / macOS 26).

## Scrolling

```kotlin
data class DailyCount(val day: ChartDate, val count: Int)

@Composable
fun ScrollingHistory(history: List<DailyCount>, start: ChartDate) {
    var scrollPosition by remember { mutableStateOf<Any>(start) }
    Chart(
        modifier = Modifier
            .frame(height = 220.0)
            .chartScrollableAxes(Axis.Set.horizontal)
            .chartVisibleDomain(ChartAxis.x, length = 7 * 86_400.0)
            .chartScrollPosition(ChartAxis.x, scrollPosition) { scrollPosition = it }
            .chartScrollTargetBehavior(x = ValueAlignment(matching = DateUnit.day, majorAlignment = MajorValueAlignment.Page)),
    ) {
        for (d in history) {
            BarMark(x = PlottableValue.value("Day", d.day, unit = DateUnit.day), y = PlottableValue.value("Count", d.count))
        }
    }
}
```

- `chartVisibleDomain(axis, length)` is in axis units (seconds on a date axis); the `categories` overload counts
  categories.
- `chartScrollPosition` with a callback is two-way; without it, the value is the initial position only.
- `chartScrollTargetBehavior` snaps to multiples of a number (`unit`) or to calendar boundaries (`matching`), and
  `majorAlignment` decides how far a flick goes.

## Vectorized and function plots

`ChartPlot` sends a whole data set as one node, with projections from your items to plottable values:

```kotlin
data class Sample(val t: Double, val value: Double)

@Composable
fun SignalChart(samples: List<Sample>) {
    Chart(modifier = Modifier.frame(height = 160.0)) {
        ChartPlot(
            PlotKind.line,
            samples,
            x = { PlottableValue.value("Time", it.t) },
            y = { PlottableValue.value("Signal", it.value) },
        )
    }
}
```

This keeps the Kotlin side cheap for thousands of points: one composable instead of one per item. The Swift side
currently draws one mark per item, not Swift Charts' vectorized `LinePlot`, so a `ChartPlot` renders like the
equivalent loop of marks.

`FunctionPlot` draws a mathematical function: Swift Charts samples it and calls your Kotlin function for each sample
(iOS 18 / macOS 15):

```kotlin
Chart(modifier = Modifier.frame(height = 200.0).chartScale(ChartAxis.y, domain = ScaleDomain.Range(-1.5..1.5))) {
    FunctionPlot(ChartFunctionPlot.Line("x", "sin(x)", domain = 0.0..6.28) { x -> kotlin.math.sin(x) })
    FunctionPlot(
        ChartFunctionPlot.Area("x", "band", domain = 0.0..6.28) { x -> kotlin.math.sin(x) - 0.2 to kotlin.math.sin(x) + 0.2 },
        style = ChartMarkStyle(foregroundStyle = Color.blue.opacity(0.2)),
    )
}
```

`ChartFunctionPlot.Area` returns the lower and upper y; `Parametric` returns (x, y) for a parameter `t`. The function
runs on the main thread during layout, so keep it pure and fast.

## The chart proxy

`Chart` takes `overlay`, `background` and `gesture` lambdas that receive a `ChartProxy` (Swift Charts' `chartOverlay`,
`chartBackground` and `chartGesture`). The proxy converts between positions in the plot area and data values:

```kotlin
@Composable
fun ScrubbableChart(data: List<MonthlySales>) {
    var month by remember { mutableStateOf<String?>(null) }
    Chart(
        modifier = Modifier
            .frame(height = 220.0)
            .chartSelection(ChartSelectionAxis.x, PlottableType.string, month) { month = it as String? },
        gesture = { proxy ->
            GestureSpec.Drag(minimumDistance = 0.0, onChanged = { value -> proxy.selectXValue(value.locationX) })
        },
    ) {
        for (item in data) {
            BarMark(x = PlottableValue.value("Month", item.month), y = PlottableValue.value("Sales", item.sales))
        }
    }
}
```

The proxy offers `valueAtX` / `valueAtY` / `valueAt(point)`, `positionForX` / `positionForY` / `positionFor(x, y)`,
`positionRangeForX` (the span of a category), `angleAt` and `valueAtAngle` for sectors, `plotFrame()`, the domains, and
`selectXValue` / `selectXRange` to drive `chartSelection`. Its functions return `null` (or do nothing) until the chart
has been laid out. Chart gestures are not available on tvOS.

## 3D charts

`Chart3D` (iOS 26 / macOS 26) takes `PointMark3D`, `RuleMark3D`, `RectangleMark3D` and `SurfacePlot`:

```kotlin
data class Measure(val x: Double, val y: Double, val z: Double, val group: String)

@Composable
fun Scatter3D(points: List<Measure>) {
    var pose by remember { mutableStateOf(Chart3DPose.default) }
    Chart3D(modifier = Modifier.frame(height = 320.0).chart3DPose(pose) { pose = it }) {
        for (p in points) {
            PointMark3D(
                x = PlottableValue.value("X", p.x),
                y = PlottableValue.value("Y", p.y),
                z = PlottableValue.value("Z", p.z),
                style = Chart3DMarkStyle(foregroundStyleBy = PlottableValue.value("Group", p.group), symbol = Chart3DSymbolShape.sphere),
            )
        }
    }
}
```

With a callback, `chart3DPose` lets the user rotate the chart. `SurfacePlot("x", "y", "z") { x, z -> … }` draws a
surface from a Kotlin function, and `Modifier.chart3DCameraProjection` switches between orthographic and perspective.
3D marks take numeric values. On older systems and on tvOS and watchOS, `Chart3D` is not registered and renders as
unsupported: check the OS version before showing it.

## Not covered

Swift Charts' `Chart(data, id:)` initializers and `ForEach` are replaced by Kotlin loops. Values are limited to strings,
numbers and `ChartDate`; custom `Plottable` types are not supported. The full list of mapped APIs is on
[/charts.html](/charts.html).
