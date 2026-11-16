<!-- group: Platforms and tools -->
# Performance

> What Compose SwiftUI costs compared with plain SwiftUI, measured on 50 screens and list scenarios, and the practices that keep screens fast.

Compose SwiftUI adds a Kotlin composition in front of SwiftUI. Updates are as fast as plain SwiftUI or faster, because
only the views whose data changed re-render; creating views costs somewhat more. This chapter gives the numbers, then
what to do about them.

## The benchmarks

Two benchmarks compare Compose SwiftUI with plain SwiftUI, in release builds. The full tables, per screen and per
platform, are in [docs/bench/RESULTS.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/bench/RESULTS.md).

### 50 screens

Fifty typical screens (profile, inbox, dashboard, chat, forms, settings, grids, charts…), each written once in plain
SwiftUI and once in Compose SwiftUI, both reading the same Kotlin `ViewModel`. The SwiftUI version observes the screen
state through one `ObservableObject`, the usual way to bridge a `StateFlow` in a KMP app. Numbers are the mean of
per-screen medians.

| | Pixel-identical | Mount: SwiftUI → Compose SwiftUI | Update: SwiftUI → Compose SwiftUI |
|---|---|---|---|
| iOS simulator | 48 / 50 | 24.1 → 26.1 ms (×1.08) | 1.92 → 1.48 ms (×0.77) |
| macOS | 50 / 50 | 14.4 → 17.1 ms (×1.18) | 2.11 → 2.07 ms (×0.98) |

"Pixel-identical" means at most 0.01% of pixels differ, before and after 30 updates. The two iOS screens that differ
do so by less than a pixel (one SF Symbol, and a search field placeholder after updates).

### List and animation scenarios

Scenarios the 50 screens don't cover, against plain SwiftUI with native `@Observable` models (one object per row, so
a change re-evaluates only the views that read it): the best case for SwiftUI. Compose SwiftUI keeps an immutable
list in one Kotlin state, as a `ViewModel` would. iOS simulator; rows have an icon, two lines of text and an unread
dot; every case is pixel-identical.

| | Plain SwiftUI | Compose SwiftUI, `LazyForEach` | Compose SwiftUI, Kotlin loop |
|---|---|---|---|
| 1,000-row `List`: mount | 23.9 ms | 27.8 ms | 55.9 ms |
| 1,000 rows: one row changes | 0.17 ms | 0.16 ms | 0.56 ms |
| 10,000-row `List`: mount | 23.5 ms | 28.2 ms | 818.8 ms |
| 10,000 rows: one row changes | 0.16 ms | 0.41 ms | 3.24 ms |
| 10,000 rows: every row changes | 4.0 ms | 4.5 ms | 78.5 ms |
| Scrolling, per page of rows | 9.7 ms | 13.8 ms | 22.9 ms |
| 60 animated bars: apply one change | 5.1 ms | 7.5 ms | — |
| 60 animated bars: frames / hitches over 10 animations | 254 / 0 | 254 / 0 | — |

On macOS the picture is the same, with a larger mount and scrolling overhead: 10,000 rows mount in 26.5 ms (SwiftUI)
vs 37.8 ms (`LazyForEach`) vs 1,251 ms (Kotlin loop).

## Reading the numbers

- **Updates are cheap.** A state change recomposes the Kotlin scopes that read it, and only the nodes whose arguments
  changed notify Swift. SwiftUI then re-evaluates those views alone. With an `ObservableObject` bridge, plain SwiftUI
  re-evaluates the whole screen on every change, which is why Compose SwiftUI often wins on updates.
- **Mounting costs 8% to 18% more.** The tree is built at run time, so each node is one type-erased SwiftUI view
  (`AnyView`) built by a factory, and the composition itself has a cost. A screen that took 24 ms takes about 26 ms.
- **A new row costs 40% (iOS) to 70% (macOS) more while scrolling**, for the same reason: each row that scrolls in is
  composed, then built from its nodes. Scrolling stays smooth on the benchmark rows, but very heavy rows feel it first.
- **A Kotlin loop in a long list is the one real trap.** `for (item in items)` composes every row up front and keeps
  every row up to date, visible or not: 819 ms to mount 10,000 rows. `LazyForEach` composes only the visible rows.

## Use LazyForEach for long lists

Inside a `List`, `LazyForEach` composes a row only while SwiftUI shows it, and disposes it when it scrolls away:

```kotlin
data class Message(val id: Long, val sender: String, val preview: String, val unread: Boolean)

@Composable
fun Inbox(messages: List<Message>) {
    List {
        LazyForEach(messages, key = { it.id }) { message ->
            MessageRow(message)
        }
    }
}

@Composable
fun MessageRow(message: Message) {
    HStack {
        VStack(alignment = HorizontalAlignment.leading) {
            Text(message.sender, modifier = Modifier.font(Font.headline))
            Text(message.preview, modifier = Modifier.foregroundStyle(ShapeStyle.secondary).lineLimit(2))
        }
        if (message.unread) {
            Spacer()
            CircleView(Circle(), modifier = Modifier.foregroundStyle(ShapeStyle.blue).frame(width = 10.0, height = 10.0))
        }
    }
}
```

To get its numbers:

- **Keep items immutable** (data classes). A visible row recomposes when its item is no longer `equals` to the
  previous one; a mutable item that changes in place is never seen as changed.
- **Give stable keys.** `key` is SwiftUI's identity: a key that changes for the same item rebuilds the row, and
  duplicate keys break scrolling and animations.
- **One view per row.** Wrap several views in an `HStack` or `VStack`.
- **Make `LazyForEach` the only child of its `List`** when you can. The renderer then hands SwiftUI the rows directly
  instead of a nested `ForEach`, which is several times faster. Put headers and fixed rows in other `Section`s.
- **Don't keep row state in `remember`.** Rows are disposed when they scroll off; state that must survive belongs in
  the item or the view model.

A loop is fine for short, fixed content (a settings screen, a dozen rows). As a rule of thumb, switch to
`LazyForEach` beyond a hundred rows, or as soon as the data is unbounded. `LazyVStack`, `LazyHStack` and the lazy
grids are SwiftUI's own lazy containers: SwiftUI creates their rows lazily, but a Kotlin loop inside them still
composes every child up front. Use `LazyForEach` inside them too: rows are composed when they appear and disposed
when they disappear.

## Keep recompositions small

The usual Compose advice applies, and pays off more here because each recomposed node may also cost a SwiftUI
update.

**Pass stable, immutable parameters.** A composable whose parameters are all equal to last time is skipped, along
with its whole subtree. Data classes of `val`s, `String`s, numbers and enums are stable. `List<T>` is treated as
unstable by the Compose compiler unless you enable strong skipping (the default since Kotlin 2.0.20) or use
`kotlinx.collections.immutable`; with strong skipping, unchanged instances are skipped by identity.

**Read state as low as possible.** Read a fast-changing value in the smallest composable that displays it, so only
that scope recomposes:

```kotlin
import kotlinx.coroutines.flow.StateFlow

@Composable
fun Player(title: String, progress: StateFlow<Double>) {
    VStack {
        Text(title, modifier = Modifier.font(Font.headline))
        // Only PlaybackBar recomposes as progress ticks; the title and the rest of the screen are skipped.
        PlaybackBar(progress)
    }
}

@Composable
fun PlaybackBar(progress: StateFlow<Double>) {
    val value by progress.collectAsState()
    ProgressView(value = value)
}
```

**Derive instead of recomputing.** Wrap expensive derived values in `remember(key) { }` or `derivedStateOf`, so that
filtering or sorting a list doesn't run on every recomposition:

```kotlin
@Composable
fun Filtered(messages: List<Message>, query: String) {
    val visible = remember(messages, query) {
        messages.filter { it.sender.contains(query, ignoreCase = true) }
    }
    List {
        LazyForEach(visible, key = { it.id }) { MessageRow(it) }
    }
}
```

**Avoid recomposition storms.** A state written on every frame (a timer, a scroll offset, a sensor) recomposes
whatever reads it on every frame. Prefer SwiftUI's own mechanisms, which run without Kotlin:

- animations: `Modifier.animation(Animation.spring, value = x)` or `withAnimation { }` instead of a Kotlin-driven
  value per frame;
- clocks: `TimelineView`, `Text(timerInterval = …)` and `ProgressView(timerInterval = …)` instead of a ticking state;
- scroll effects: `Modifier.scrollTransition` and `visualEffect` instead of reading the scroll offset into state;
- continuous gestures: update state on `onEnded`, or throttle `onChanged`, when the intermediate values aren't shown.

**Don't create new values for nothing.** Callbacks are free (a new lambda reuses the same handle on the Swift side),
but a new `Modifier` chain or value with different arguments every time is a real change. Build constant modifier
chains once, at top level or in `remember`.

## Mounting

Mounting is where Compose SwiftUI is slowest relative to SwiftUI, so:

- Show content progressively: render the skeleton first and load data in a `LaunchedEffect` or the view model.
- Prefer `TabView` and `NavigationStack` to building every screen up front; they only mount what is shown.
- Keep one `ComposeView` per screen or per tab rather than one per small widget inside a SwiftUI screen: each
  `ComposeView` has its own composition and host.

## Measuring

- **Instruments.** The SwiftUI template shows which views re-evaluate and how long their bodies take; Time Profiler
  shows Kotlin/Native frames (composition, your view models) next to the Swift ones.
- **Compose compiler reports.** Enable `reportsDestination` and `metricsDestination` in the `composeCompiler { }` block
  to see which composables are skippable and which parameters are unstable.
- **Count recompositions.** A `SideEffect { println("recomposed") }` in a suspicious composable shows how often it
  runs.
- **Measure release builds.** Debug builds of Kotlin/Native are much slower; the numbers above are release builds.
  The benchmark harness lives in `bench/`;
  [CONTRIBUTING.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/CONTRIBUTING.md) explains how
  to run it.

```kts
// build.gradle.kts of the module that applies the plugin
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
```

## Summary

- Updates: as fast as plain SwiftUI or faster.
- Mount: 8% to 18% slower.
- Long lists: always `LazyForEach`, as the only child of the `List`, with immutable items and stable keys.
- Keep state reads low, parameters stable, and let SwiftUI drive per-frame work.
