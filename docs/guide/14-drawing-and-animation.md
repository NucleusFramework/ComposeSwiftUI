<!-- group: Building screens -->
# Drawing and animation

> Shapes, custom paths, Canvas drawing in Kotlin, and SwiftUI animations, transitions and effects driven by Kotlin state.

Everything on this page is rendered by SwiftUI itself: a `Circle()` is a SwiftUI `Circle`, `withAnimation` opens a real
SwiftUI transaction, and a `Canvas` draws into a SwiftUI `GraphicsContext`. Kotlin decides *what* to draw and *when*
state changes; SwiftUI interpolates. A few SwiftUI features that are protocols or closures in Swift (`Shape`,
`Transition`, `CustomAnimation`, `keyframeAnimator`, `visualEffect`) have a Kotlin form described below.

## Shapes

The built-in shapes are Kotlin values (`Circle()`, `Capsule()`, `Rectangle()`, `Ellipse()`,
`RoundedRectangle(cornerRadius = 12.0)`, `UnevenRoundedRectangle(...)`, `ContainerRelativeShape()`). To show one as
a view, use `ShapeView`, which covers SwiftUI's `fill`, `stroke` and `trim` in one call:

```kotlin
HStack(spacing = 16.0) {
    ShapeView(Circle(), fill = Color.orange, modifier = Modifier.frame(width = 44.0, height = 44.0))
    ShapeView(
        RoundedRectangle(cornerRadius = 12.0),
        stroke = Color.blue,
        lineWidth = 3.0,
        modifier = Modifier.frame(width = 80.0, height = 44.0),
    )
    // A progress ring: Circle().trim(from: 0, to: 0.7).stroke(...)
    ShapeView(
        Circle(),
        stroke = Color.green,
        strokeStyle = StrokeStyle(lineWidth = 6.0, dash = emptyList()),
        trimFrom = 0.0,
        trimTo = 0.7,
        modifier = Modifier.frame(width = 44.0, height = 44.0).rotationEffect(Angle.degrees(-90.0)),
    )
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `Circle().fill(.red)` | `ShapeView(Circle(), fill = Color.red)` |
| `Capsule().stroke(.blue, lineWidth: 2)` | `ShapeView(Capsule(), stroke = Color.blue, lineWidth = 2.0)` |
| `Circle().trim(from: 0, to: 0.5)` | `ShapeView(Circle(), stroke = …, trimFrom = 0.0, trimTo = 0.5)` |
| `.clipShape(Circle())` | `Modifier.clipShape(Circle())` |
| `.background(.blue, in: Capsule())` | `Modifier.background(Color.blue, Capsule())` (see [Styling](styling.md)) |
| `.contentShape(Rectangle())` | `Modifier.contentShape(Rectangle())` |

Any `ShapeStyle` works as a fill or stroke: colors, `ShapeStyle.secondary`, gradients. `StrokeStyle` takes a line width,
a miter limit and a dash pattern (`dash = listOf(4.0, 2.0)`); line caps and joins are not exposed.

### Shape operations

The SwiftUI `Shape` operations are extension functions that return a new `Shape`, so they compose and can be used
anywhere a shape is accepted (`ShapeView`, `clipShape`, `contentShape`, `mask`…):

```kotlin
val ring = Circle().subtracting(Circle().scale(0.6))
val badge = RoundedRectangle(cornerRadius = 8.0).union(Circle().offset(x = 30.0, y = -10.0))

VStack(spacing = 12.0) {
    ShapeView(ring, fill = Color.purple, modifier = Modifier.frame(width = 60.0, height = 60.0))
    ShapeView(badge, fill = Color.red, modifier = Modifier.frame(width = 80.0, height = 40.0))
    ShapeView(Rectangle().rotation(Angle.degrees(45.0)), fill = Color.teal, modifier = Modifier.frame(width = 40.0, height = 40.0))
}
```

Available: `offset`, `rotation`, `scale`, `trim`, `transform`, `size`, `union`, `intersection`, `subtracting`,
`symmetricDifference`, `lineIntersection`, `lineSubtraction`, `strokedShape`, and `inset(by)` on insettable shapes
(`Circle`, `Capsule`, `RoundedRectangle`…). The boolean operations need iOS 17 / macOS 14, which is the library
minimum anyway.

## Custom shapes with PathShape

A Swift custom `Shape` implements `path(in:)`. In Kotlin, build a `PathShape` with a `PathBuilder` in **unit
coordinates**: `0.0…1.0` on each axis, scaled to whatever frame the shape gets. The same shape therefore works at any
size, exactly like a SwiftUI shape that reads `rect`.

```kotlin
val Triangle = PathShape {
    moveTo(0.5, 0.0)
    lineTo(1.0, 1.0)
    lineTo(0.0, 1.0)
    close()
}

val Wave = PathShape {
    moveTo(0.0, 0.5)
    curveTo(0.25, 0.0, 0.25, 1.0, 0.5, 0.5)
    curveTo(0.75, 0.0, 0.75, 1.0, 1.0, 0.5)
}

@Composable
fun ShapesDemo() {
    VStack(spacing = 16.0) {
        ShapeView(Triangle, fill = Color.yellow, modifier = Modifier.frame(width = 60.0, height = 50.0))
        ShapeView(Wave, stroke = Color.blue, lineWidth = 3.0, modifier = Modifier.frame(height = 40.0))
        Image(systemName = SFSymbol.star, modifier = Modifier.padding().background(Color.gray).clipShape(Triangle))
    }
}
```

`PathBuilder` mirrors SwiftUI's `Path`: `moveTo`, `lineTo`, `quadTo`, `curveTo`, `close`, `rect`, `ellipse`,
`roundedRect`, `arc`, `relativeArc`, `tangentArc`, `lines(...)`, `rects(...)` and `path { }` to append another path.
Angles are in degrees. In a `PathShape`, arc radii and corner radii are fractions of the **shorter side** of the
frame (so `arc(0.5, 0.5, 0.5, 0.0, 360.0, false)` is an inscribed circle).

`PathShape` conforms to SwiftUI's `InsettableShape` and `Animatable`, so `inset(by = 4.0)`, `background(style, shape = ...)`,
`containerShape(...)` and animations between two paths with the same command structure work natively.

### Animatable custom shapes with AnimatablePathShape

When a shape's geometry depends on animated parameters or exact point math, use `AnimatablePathShape`. Its path is
expressed with symbolic `ScalarExpr` operations over `rect` (`RectExpr`: `minX`, `minY`, `midX`, `midY`, `maxX`, `maxY`,
`width`, `height`, `minSide`) and `params` (`List<ScalarExpr>`). SwiftUI interpolates `parameters` via
`Animatable.animatableData` and evaluates the compiled expression tree natively on every display refresh without
crossing back into Kotlin:

```kotlin
@Composable
fun MorphingDiamond() {
    var inset by remember { mutableStateOf(0.0) }
    val shape = AnimatablePathShape(inset) { rect, params ->
        val p = params[0]
        moveTo(rect.midX, rect.minY + p)
        lineTo(rect.maxX - p, rect.midY)
        lineTo(rect.midX, rect.maxY - p)
        lineTo(rect.minX + p, rect.midY)
        close()
    }
    VStack(spacing = 12.0) {
        ShapeView(
            shape.inset(by = 2.0),
            fill = Color.orange,
            modifier = Modifier
                .frame(width = 80.0, height = 80.0)
                .animation(Animation.spring(duration = 0.4), value = inset),
        )
        Button("Morph") { inset = if (inset == 0.0) 16.0 else 0.0 }
    }
}
```

`ScalarExpr` supports `+`, `-`, `*`, `/`, `%`, unary `-`, `abs()`, `sqrt()`, `sin()`, `cos()`, `floor()`, `ceil()`,
`round()`, `pow(...)`, `clamp(...)`, `ScalarExpr.min(...)`, `ScalarExpr.max(...)`, and conditionals
(`lt`, `le`, `gt`, `ge`, `eq`, `ScalarExpr.select(...)`).

## Canvas

`Canvas` is SwiftUI's immediate-mode drawing view. Its `onDraw` lambda runs in Kotlin every time SwiftUI renders the
canvas, with the size in points, and records drawing commands into a `DrawScope`. Coordinates in a `Canvas` are
**points**, not unit coordinates.

```kotlin
@Composable
fun Gauge(progress: Double, label: String) {
    // Read state in composition, not inside onDraw: the lambda runs during SwiftUI's draw pass, outside Compose.
    val fraction = progress.coerceIn(0.0, 1.0)
    Canvas(modifier = Modifier.frame(width = 200.0, height = 120.0)) {
        fill(Color.gray.opacity(0.2)) { rect(0.0, 0.0, width, height) }
        fill(Color.green) { rect(0.0, height - 12.0, width * fraction, 12.0) }
        stroke(Color.primary, lineWidth = 2.0) {
            moveTo(0.0, height / 2)
            curveTo(width / 3, 0.0, 2 * width / 3, height, width, height / 2)
        }
        drawText(label, width / 2, 20.0, font = Font.headline)
        drawSymbol(SFSymbol.boltFill, width - 20.0, 20.0, color = Color.yellow)
    }
}
```

The `DrawScope` holds `width` and `height` and offers:

| SwiftUI `GraphicsContext` | `DrawScope` |
|---|---|
| `fill(path, with:)` | `fill(style) { path }` |
| `stroke(path, with:, lineWidth:)` | `stroke(style, lineWidth) { path }` |
| `draw(Text, at:, anchor:)` | `drawText(text, x, y, font, color, anchor)` |
| `draw(Image(systemName:), at:)` | `drawSymbol(symbol, x, y, font, color)` |
| `translateBy`, `scaleBy`, `rotate(by:)`, `opacity`, `clip(to:)` | `translate`, `scale`, `rotate(degrees)`, `opacity`, `clip { }` |
| a copy of the context (`var c = context`) | `withState { … }` |

State changes (`translate`, `scale`, `rotate`, `opacity`, `clip`) apply to every following command; wrap them in
`withState { }` to scope them:

```kotlin
Canvas(modifier = Modifier.frame(width = 120.0, height = 120.0)) {
    for (i in 0 until 12) {
        withState {
            translate(width / 2, height / 2)
            rotate(i * 30.0)
            fill(Color.blue.opacity(0.3 + i / 20.0)) { rect(-2.0, -height / 2, 4.0, 16.0) }
        }
    }
}
```

Arcs draw rings and pie slices directly:

```kotlin
@Composable
fun Ring(progress: Double) {
    Canvas(modifier = Modifier.frame(width = 100.0, height = 100.0)) {
        val r = minOf(width, height) / 2 - 6.0
        stroke(Color.gray.opacity(0.2), lineWidth = 12.0) { arc(width / 2, height / 2, r, 0.0, 360.0, false) }
        stroke(Color.green, lineWidth = 12.0) { relativeArc(width / 2, height / 2, r, -90.0, 360.0 * progress) }
        fill(Color.blue.opacity(0.1)) { roundedRect(20.0, 40.0, width - 40.0, 20.0, cornerRadius = 6.0) }
    }
}
```

Gotchas:

- Inside a `Canvas`, every `PathBuilder` command works, arcs and rounded rectangles included, but in points: radii
  and corner radii are absolute, not fractions of the shorter side as in a `PathShape`.
- `onDraw` reads Kotlin values at draw time but is not observed by Compose. Pass state in as parameters (or read it
  into a local `val` before `Canvas`) so a state change recomposes the canvas and SwiftUI redraws it.
- Images other than SF Symbols, filters, blend modes and resolved symbols are not available in a canvas. For layered
  effects, stack views in a `ZStack` and use view modifiers (`blendMode`, `blur`, `shadow`).

## Implicit animations

`Modifier.animation(animation, value)` animates every animatable change of the modified view when `value` changes,
as in SwiftUI. Animation values mirror SwiftUI's: `Animation.default`, `Animation.spring`, `Animation.bouncy`,
`Animation.snappy`, `Animation.smooth`, `Animation.easeInOut(0.3)`, `Animation.linear(1.0)`,
`Animation.spring(duration = 0.4, bounce = 0.3)`, plus `.delay(0.1)`, `.speed(2.0)`, `.repeatCount(3)` and
`.repeatForever(autoreverses = true)`.

```kotlin
var expanded by remember { mutableStateOf(false) }
VStack {
    ShapeView(
        RoundedRectangle(cornerRadius = if (expanded) 32.0 else 8.0),
        fill = if (expanded) Color.orange else Color.blue,
        modifier = Modifier
            .frame(width = if (expanded) 200.0 else 80.0, height = 80.0)
            .animation(Animation.spring(duration = 0.4, bounce = 0.3), value = expanded),
    )
    Button(if (expanded) "Collapse" else "Expand") { expanded = !expanded }
}
```

`value` is compared with `equals`, so any Kotlin value works (an enum, a data class, an `Int`).

## withAnimation

`withAnimation` runs a block of Kotlin state changes inside one animated SwiftUI transaction: the recomposition it
triggers is applied before the transaction ends, so every view that changed animates with the given curve.

```kotlin
var selected by remember { mutableStateOf(0) }
HStack {
    for (i in 0 until 3) {
        Button("Tab $i") {
            withAnimation(Animation.snappy) { selected = i }
        }
    }
}
```

The overload with `completion` maps to `withAnimation(_:completionCriteria:_:completion:)`:

```kotlin
var visible by remember { mutableStateOf(true) }
var log by remember { mutableStateOf("") }
Button("Hide") {
    withAnimation(Animation.easeOut(0.25), completion = { log = "hidden" }) { visible = false }
}
```

Changes made in a `ViewModel` work the same way: call `withAnimation { viewModel.select(id) }` from the action. A
`StateFlow` emission that arrives later, outside the block (after a network call, say), is not animated; use
`Modifier.animation(…, value)` on the views that should animate in that case.

## Transactions

`withTransaction` gives finer control over a SwiftUI transaction than `withAnimation`—for example disabling implicit
animations during a programmatic reset, or marking drag updates as continuous (`isContinuous`):

```kotlin
var expanded by remember { mutableStateOf(false) }
HStack(spacing = 12.0) {
    Button("Snap immediately") {
        withTransaction(disablesAnimations = true) { expanded = !expanded }
    }
    Button("Custom transaction") {
        val tx = transaction(animation = Animation.snappy, isContinuous = true)
        withTransaction(tx) { expanded = !expanded }
    }
}
```

You can also chain modifiers on a `Transaction` value (`Transaction.animation(...)`, `Transaction.disablesAnimations(...)`,
`Transaction.isContinuous(...)`), or override transactions for a subtree with `Modifier.transaction(...)` (unconditional
or scoped to a `value`):

```kotlin
var count by remember { mutableStateOf(0) }
Text(
    "$count",
    modifier = Modifier.transaction(value = count, animation = Animation.easeOut(0.2), disablesAnimations = false),
)
```

Pass `setAnimation = true, animation = null` to clear an inherited animation on a subtree.

## Transitions

A view that appears or disappears because of a Kotlin `if` is inserted or removed in SwiftUI, so
`Modifier.transition` behaves as in Swift. Trigger the change inside `withAnimation` (or an enclosing `animation`):

```kotlin
var showBanner by remember { mutableStateOf(false) }
VStack {
    Button("Toggle") { withAnimation { showBanner = !showBanner } }
    if (showBanner) {
        Text(
            "Saved",
            modifier = Modifier
                .padding()
                .background(Color.green.opacity(0.2))
                .transition(AnyTransition.move(Edge.top).combined(AnyTransition.opacity)),
        )
    }
}
```

`AnyTransition` offers `opacity`, `scale`, `scale(scale, anchor)`, `slide`, `move(edge)`, `push(edge)`, `offset(…)`,
`identity`, `asymmetric(insertion, removal)`, `.combined(other)` and `.animation(animation)`.

A custom `Transition` is a Kotlin function from the transition stage to `VisualEffects` (opacity, scale, offset,
rotation, blur):

```kotlin
var showCard by remember { mutableStateOf(false) }
if (showCard) {
    Text("Card", modifier = Modifier.padding().transition { stage ->
        when (stage) {
            TransitionStage.WillAppear -> VisualEffects(opacity = 0.0, scale = 0.8, offsetY = 20.0)
            TransitionStage.Identity -> VisualEffects()
            TransitionStage.DidDisappear -> VisualEffects(opacity = 0.0, blurRadius = 8.0)
        }
    })
}
```

## Content transitions

`contentTransition` animates a change of content inside the same view, such as a number in a `Text`. Combine it with
an animation:

```kotlin
var count by remember { mutableStateOf(0) }
VStack {
    Text(
        "$count",
        modifier = Modifier
            .font(Font.largeTitle)
            .contentTransition(ContentTransition.numericText(value = count.toDouble()))
            .animation(Animation.default, value = count),
    )
    Button("Add") { count++ }
}
```

Other values: `ContentTransition.interpolate`, `opacity`, `identity`, `symbolEffect` (for an `Image(systemName:)`
whose symbol changes) and `numericText(countsDown = true)`.

## Matched geometry

`@Namespace` is a Kotlin `NamespaceScope`, which hands you a `NamespaceId`. Views with the same id in that namespace
morph into each other when one replaces the other inside an animated transaction:

```kotlin
@Composable
fun ExpandingAvatar() {
    var expanded by remember { mutableStateOf(false) }
    NamespaceScope { ns ->
        if (expanded) {
            ShapeView(
                RoundedRectangle(cornerRadius = 24.0),
                fill = Color.indigo,
                modifier = Modifier
                    .matchedGeometryEffect(id = "avatar", namespace = ns)
                    .frame(width = 240.0, height = 240.0)
                    .onTapGesture { withAnimation(Animation.spring) { expanded = false } },
            )
        } else {
            ShapeView(
                Circle(),
                fill = Color.indigo,
                modifier = Modifier
                    .matchedGeometryEffect(id = "avatar", namespace = ns)
                    .frame(width = 48.0, height = 48.0)
                    .onTapGesture { withAnimation(Animation.spring) { expanded = true } },
            )
        }
    }
}
```

`matchedGeometryEffect` also takes `properties` (`MatchedGeometryProperties.frame`, `.position`, `.size`), `anchor`
and `isSource`. The same namespace drives the navigation zoom transition: mark the source with
`Modifier.matchedTransitionSource(id, ns)` and the destination with `Modifier.zoomTransition(id, ns)` (iOS 18+,
macOS 15+ for the source; see [Navigation](navigation.md)).

## Phase and keyframe animations

`phaseAnimator` steps a view through a list of `VisualEffects`. With no `trigger` it loops; with a `trigger` it runs
once each time the trigger changes:

```kotlin
var likes by remember { mutableStateOf(0) }
HStack {
    // Loops forever: a pulsing dot
    ShapeView(
        Circle(),
        fill = Color.red,
        modifier = Modifier.frame(width = 12.0, height = 12.0)
            .phaseAnimator(listOf(VisualEffects(), VisualEffects(scale = 1.4, opacity = 0.5))),
    )
    // Runs once per like
    Button(action = { likes++ }) {
        Image(
            systemName = SFSymbol.heartFill,
            modifier = Modifier.phaseAnimator(
                phases = listOf(VisualEffects(), VisualEffects(scale = 1.3, rotationDegrees = -10.0), VisualEffects()),
                trigger = likes,
                animation = Animation.bouncy,
            ),
        )
    }
}
```

`keyframeAnimator` maps directly to SwiftUI's `keyframeAnimator(initialValue:trigger:content:keyframes:)`. Use the
multi-track DSL to animate properties independently in parallel—each `KeyframeProperty` (`Opacity`, `Scale`, `OffsetX`,
`OffsetY`, `RotationDegrees`, `BlurRadius`) gets its own SwiftUI `KeyframeTrack` with `linear`, `spring`, `cubic` and
`move` keyframes:

```kotlin
var trigger by remember { mutableStateOf(0) }
Image(
    systemName = SFSymbol.starFill,
    modifier = Modifier.keyframeAnimator(trigger = trigger) {
        track(KeyframeProperty.Scale) {
            spring(1.4, duration = 0.25, bounce = 0.4)
            spring(1.0, duration = 0.25, bounce = 0.2)
        }
        track(KeyframeProperty.RotationDegrees) {
            linear(15.0, duration = 0.15)
            cubic(-15.0, duration = 0.2)
            linear(0.0, duration = 0.15)
        }
        track(KeyframeProperty.OffsetY) {
            cubic(-12.0, duration = 0.2)
            spring(0.0, duration = 0.3)
        }
    },
)
```

A single-track list overload is also available for simple sequences:

```kotlin
var shakes by remember { mutableStateOf(0) }
TextField("PIN", text = "", onTextChange = {}, modifier = Modifier.keyframeAnimator(
    keyframes = listOf(
        Keyframe(VisualEffects(offsetX = -10.0), 0.08),
        Keyframe(VisualEffects(offsetX = 10.0), 0.08),
        Keyframe(VisualEffects(offsetX = -6.0), 0.08),
        Keyframe(VisualEffects(), 0.1, KeyframeCurve.Spring),
    ),
    trigger = shakes,
))
```

## Custom animation curves

`customAnimation(duration) { t -> progress }` is a SwiftUI `CustomAnimation` whose curve is a Kotlin function, called by
SwiftUI on every frame. Keep it cheap:

```kotlin
val overshoot = customAnimation(duration = 0.6) { t -> 1 - (1 - t) * (1 - t) * (1 - 2.5 * t) }
var big by remember { mutableStateOf(false) }
Text("Hi", modifier = Modifier.scaleEffect(if (big) 2.0 else 1.0).animation(overshoot, value = big))
```

## SF Symbol effects

`symbolEffect` animates an `Image(systemName:)` with composable `SymbolEffect` variants and `SymbolEffectOptions`:

```kotlin
var downloads by remember { mutableStateOf(0) }
var syncing by remember { mutableStateOf(true) }
HStack(spacing = 20.0) {
    // Discrete: bounce up by layer, twice, at 1.5× speed
    Image(
        systemName = SFSymbol.arrowDownCircle,
        modifier = Modifier.symbolEffect(
            effect = SymbolEffect.bounce.up.byLayer,
            options = SymbolEffectOptions.repeat(2).speed(1.5),
            value = downloads,
        ),
    )
    // Indefinite: iterative reversing variable color while syncing
    Image(
        systemName = SFSymbol.wifi,
        modifier = Modifier.symbolEffect(
            effect = SymbolEffect.variableColor.iterative.reversing,
            options = SymbolEffectOptions.repeating,
            isActive = syncing,
        ),
    )
}
```

Available base effects: `SymbolEffect.bounce`, `pulse`, `variableColor`, `scale`, `appear`, `disappear`, plus `wiggle`,
`rotate` and `breathe` (iOS 18 / macOS 15 / tvOS 18 / watchOS 11; ignored on older systems). Chainable variants include
`.up`, `.down`, `.byLayer`, `.wholeSymbol`, `.reversing`, `.nonReversing`, `.cumulative`, `.iterative`,
`.hideInactiveLayers`, `.dimInactiveLayers`, `.clockwise`, `.counterClockwise`, `.left`, `.right`, `.forward`,
`.backward`, `.plain`, `.pulseVariant`. `SymbolEffectOptions` supports `.speed(factor)`, `.repeat(count)`, `.repeating`
and `.nonRepeating`. The shorter `SymbolEffectKind` overloads continue to work for default options.

## Scroll transitions and visual effects

For smooth 120 fps scrolling without per-frame Kotlin callbacks, use `scrollTransitionExpr` and `visualEffectExpr`.
They build symbolic `ScalarExpr` expressions evaluated natively in Swift:

```kotlin
ScrollView {
    LazyVStack(spacing = 12.0) {
        for (i in 0 until 50) {
            Text(
                "Row $i",
                modifier = Modifier
                    .frame(maxWidth = Double.POSITIVE_INFINITY)
                    .padding()
                    .background(Color.blue.opacity(0.15))
                    .scrollTransitionExpr { phase ->
                        val dist = phase.abs()
                        VisualEffectsExpr(
                            opacity = 1.0 - dist * 0.7,
                            scale = 1.0 - dist * 0.1,
                            offsetY = phase * 16.0,
                        )
                    },
            )
        }
    }
}
```

```kotlin
Image(
    systemName = SFSymbol.mountain2Fill,
    modifier = Modifier.visualEffectExpr { g -> VisualEffectsExpr(offsetY = -g.minY * 0.2) },
)
```

When you need arbitrary Kotlin logic inside the effect, the lambda forms `scrollTransition { phase, isIdentity -> VisualEffects(...) }`
and `visualEffect { g -> VisualEffects(...) }` are also available; SwiftUI invokes them on each scroll or layout pass,
so keep them fast and side-effect free.

Both return `VisualEffects` / `VisualEffectsExpr` (opacity, scale, offset, rotation and blur). Other geometry-dependent
changes belong in a `GeometryReader` or `onGeometryChange` (see [Layout](layout.md)).

## TimelineView

`TimelineView` recomposes its content on a schedule, with the date in epoch seconds:

```kotlin
TimelineView(TimelineSchedule.periodic(0.0, 1.0)) { now ->
    val seconds = now.toLong() % 60
    Text("Seconds: $seconds", modifier = Modifier.monospacedDigit())
}
```

Schedules: `TimelineSchedule.everyMinute`, `TimelineSchedule.periodic(startDate, interval)` and
`TimelineSchedule.animation` (every frame, or `TimelineSchedule.animation(minimumInterval = …)`). With the animation
schedule, every frame is a Kotlin recomposition of the content: keep that content small, typically one `Canvas`.

```kotlin
TimelineView(TimelineSchedule.animation) { now ->
    val angle = (now % 4.0) / 4.0 * 360.0
    Canvas(modifier = Modifier.frame(width = 80.0, height = 80.0)) {
        translate(width / 2, height / 2)
        rotate(angle)
        fill(Color.orange) { rect(-4.0, -36.0, 8.0, 36.0) }
    }
}
```

On watchOS, the system throttles timelines in the always-on state, as it does for SwiftUI apps: prefer
`everyMinute` or `periodic` schedules for content that stays on screen.

## MeshGradient

`MeshGradient` (iOS 18, macOS 15, tvOS 18, watchOS 11; empty on older systems) takes a grid size, the control points
as flat `x, y` pairs in unit space (row by row), and one color per point:

```kotlin
MeshGradient(
    width = 3,
    height = 3,
    points = listOf(
        0.0, 0.0, 0.5, 0.0, 1.0, 0.0,
        0.0, 0.5, 0.6, 0.4, 1.0, 0.5,
        0.0, 1.0, 0.5, 1.0, 1.0, 1.0,
    ),
    colors = listOf(
        Color.red, Color.purple, Color.indigo,
        Color.orange, Color.white, Color.blue,
        Color.yellow, Color.green, Color.mint,
    ),
    modifier = Modifier.ignoresSafeArea(),
)
```

The point and color counts must equal `width * height`, otherwise nothing is drawn. Animate the mesh by changing the
points inside `withAnimation`, or drive them from a `TimelineView`.

## Performance notes

- SwiftUI interpolates animations natively; Kotlin only runs when state changes. A `withAnimation` costs one
  recomposition, not one per frame.
- Prefer `AnimatablePathShape`, `scrollTransitionExpr` and `visualEffectExpr` over the Kotlin-callback variants when
  animating shapes or scrolling: their `ScalarExpr` trees run natively in Swift with zero bridge crossings per frame.
- Callbacks that SwiftUI calls per frame or per layout (`scrollTransition`, `visualEffect`, `customAnimation`,
  `textRenderer`, `Canvas.onDraw`, `TimelineSchedule.animation`) cross into Kotlin each time. Keep them pure and
  allocation-light.
- `Modifier.drawingGroup()` flattens a complex subtree into one layer, as in SwiftUI.

See [Gestures and input](gestures-and-input.md) to drive animations from drags and pinches, and [Accessibility](accessibility.md)
for reduced motion.
