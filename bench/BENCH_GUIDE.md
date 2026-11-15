# Writing a benchmark screen (SwiftUI twin + Compose SwiftUI twin)

Every screen exists twice and must render **pixel-identical** on iOS and macOS:

| File | Role |
|---|---|
| `bench/staging/ScreenNN.kt` | Kotlin: `ScreenNNState` (data classes), `ScreenNNViewModel : BenchViewModel`, `@Composable fun ScreenNN(vm)`, and `val screenNN = BenchScreen("NN · Title", ::ScreenNNViewModel) { ScreenNN(it as ScreenNNViewModel) }` |
| `bench/staging/ScreenNNView.swift` | Pure SwiftUI twin: `struct ScreenNNView: View { init(vm: ScreenNNViewModel) }` observing the same Kotlin view model through `FlowState` |

Template: `bench/shared/src/commonMain/kotlin/bench/screens/Screen01.kt` + `bench/app/Screens/Screen01View.swift`.

## Check loop

```bash
python3 -I scripts/check_screens.py 7 8      # integrates staged screens 7 and 8, builds, runs the bench
```

It prints, per platform, `diff=` (initial pixel difference) and `after=` (after 30 `mutate()` calls) plus timings, and
writes `../bench-results/check/{macos,ios}/screenNN-{swiftui,compose,diff}.png` (open them with the Read tool to compare).
Target: `diff=0.0000% after=0.0000%` on both platforms. If the build fails the screen is removed from the tree again
and compiler errors are printed — fix the staged file and re-run. Never edit `Registry.kt` / `SwiftRegistry.swift`
(generated), the runtime, the generator or the harness.

## Rules for identical output

- **All display strings are computed in Kotlin** (state), both versions only display them. Never format numbers or
  dates in Swift or in the composable. Colors: `Rgb(r, g, b)` in state → Kotlin `rgb.color()`, Swift `rgb.color`.
- Deterministic: no randomness, no current date/time, no animations, no indeterminate `ProgressView()` (spinner),
  no `AsyncImage`, no focused text fields, no timers. Use SF Symbols, shapes, gradients, colors.
- `mutate()` must change something visible (counters, toggles, selection, list contents...).
- Same structure in both: the Kotlin modifier chain is the Swift modifier chain, in the same order.
  `Text("a").font(.title).padding()` ⇔ `Text("a", modifier = Modifier.font(Font.title).padding())`.
- Swift `ForEach(items, id: \.id)` ⇔ Kotlin `for (item in items) key(item.id) { ... }`.
- Bindings: Kotlin `Toggle(title = "x", isOn = s.flag, onIsOnChange = vm::setFlag)`;
  Swift `Toggle("x", isOn: Binding(get: { s.flag }, set: { vm.setFlag(value: $0) }))`.
- Platform-specific APIs: the Kotlin binding of an iOS-only API is a no-op on macOS, so wrap the Swift call in a helper
  that is a no-op on macOS (see `insetGroupedListStyle()` in `bench/app/FlowState.swift`; add your own private helper
  in your Swift screen file, do not edit shared files).
- Root of every screen: `NavigationStack { ... .navigationTitle("…") }`.

## Kotlin API cheat-sheet (full list: `docs/API.md`, grep it)

- Package: `import dev.nucleusframework.composeswiftui.*`. Views are `@Composable` functions named like SwiftUI; `modifier` param;
  trailing lambda = `@ViewBuilder content`; extra builders are named lambdas (`header = { }`, `label = { }`).
- Overloads that differ only by Swift labels are reached by naming the label, like in Swift:
  `Image(systemName = "star")`, `Label(title, image = "asset")`.
- Values: `Font.title`, `Font.system(size = 15.0, weight = Font.Weight.semibold)`, `Color.red`,
  `Color(red =, green =, blue =)`, `Color.blue.opacity(0.2)`, `ShapeStyle.secondary`, `ShapeStyle.tint`,
  `Alignment.leading`, `HorizontalAlignment.leading`, `Edge.Set.horizontal`, `ListStyle.insetGrouped`,
  `PrimitiveButtonStyle.borderedProminent`, `GridItem(size = GridItem.Size.flexible())`, `LinearGradient(...)`.
- Shapes as views: `ShapeView(Circle(), fill = Color.red, stroke = ..., lineWidth = 2.0, strokeStyle = StrokeStyle(...), trimFrom = 0.0, trimTo = 0.7)`;
  shapes as values: `Modifier.clipShape(RoundedRectangle(cornerRadius = 12.0))`, `.background(style, shape)`.
- Toolbar: `Modifier.toolbarItem(placement = ToolbarItemPlacement.topBarTrailing) { Button(...) }`.
- `Double.POSITIVE_INFINITY` ⇔ `.infinity`. Kotlin `Int` is `Int32` in Swift; `List<T>` is `[T]`.
- Avoid Kotlin property names `description`, `hash`, `copy`, and C keywords (`short`, `long`, `int`, `char`, `default`...) in state classes: ObjC renames them with a trailing `_`.
- `GeometryReader { size -> ... }` (size arrives after the first layout pass).
- Not available (design around it): `ForEach` (use Kotlin loops), custom `ViewModifier`s.
