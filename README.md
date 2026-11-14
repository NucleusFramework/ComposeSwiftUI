# Compose SwiftUI

Write **real SwiftUI** UIs in Kotlin with the **Compose runtime** (not Compose UI), for Kotlin/Native on
**iOS and macOS** — the same idea as Compose HTML / Kobweb, where the Compose runtime drives the DOM. Your existing
Kotlin `ViewModel`s (androidx lifecycle, `StateFlow`, `collectAsState`, `remember`, `LaunchedEffect`…) are reused
as-is; only the UI layer is written with Compose SwiftUI, and what users see is native SwiftUI.

```kotlin
fun profileScreen() = composeSwiftUI {
    val vm = viewModel { ProfileViewModel() }          // androidx.lifecycle.ViewModel, unchanged
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Profile")) {
            Section(title = "Preferences") {
                Toggle(title = "Notifications", isOn = state.notifications, onIsOnChange = vm::setNotifications)
            }
            for (row in state.rows) key(row.id) {
                Label(row.title, systemImage = row.icon, modifier = Modifier.foregroundStyle(row.tint.color()))
            }
        }
    }
}
```

```swift
// Swift side: one line to display it
ComposeView { ProfileKt.profileScreen() }      // after ComposeGenerated.setUp() at app start
```

## How it works

```
 Kotlin (Compose runtime)                         Swift (SwiftUI)
 ┌──────────────────────────────┐                 ┌────────────────────────────────────────┐
 │ @Composable VStack/Text/...  │  SNode tree     │ NodeModel (ObservableObject) per SNode │
 │  → SwiftView(kind, args,     │ ──────────────▶ │ NodeView: registry[kind](args)         │
 │     modifier chain, slots)   │  change notif.  │   + modifiers applied in order         │
 │ Applier = SNodeApplier       │ ◀────────────── │ Bindings / actions call Kotlin back,   │
 │ Recomposer + frame clock     │  callbacks      │   then flushCompose() (sync recompose) │
 └──────────────────────────────┘                 └────────────────────────────────────────┘
```

- `runtime/` — Kotlin Multiplatform library (iosArm64, iosSimulatorArm64, macosArm64): `SNode` tree, `SNodeApplier`,
  `ComposeSwiftUIHost` (Recomposer, frame clock, `ViewModelStoreOwner`), `Modifier`, `viewModel { }`, `watchFlow`.
- `swift/ComposeSwiftUI/` — the Swift renderer: `ComposeView`, `NodeView`, typed argument decoding, bindings.
- Only the node that changed is invalidated on the Swift side (one `ObservableObject` per node); children keep stable
  identities (`ForEach` over node ids), so SwiftUI diffing, animations and state of native controls behave normally.
- Bindings (`TextField`, `Toggle`, `Slider`, `Picker`…) are controlled components: the Swift `Binding` writes the value
  optimistically, calls the Kotlin callback and recomposes synchronously in the same SwiftUI transaction.

## Automatic generation

`generator/` produces the whole SwiftUI surface from Apple's own SDK, not by hand:

1. `swift-symbolgraph-extract` dumps SwiftUI + SwiftUICore for iOS and macOS (every public symbol with typed signature,
   generic constraints, default values and per-platform availability).
2. `generate.py` maps Swift types to Kotlin (String/StringProtocol/LocalizedStringKey → `String`, `CGFloat` → `Double`,
   `Binding<T>` → value + `onXChange`, `@ViewBuilder` closures → `@Composable` lambdas/slots, protocols such as
   `ShapeStyle`/`ButtonStyle` → Kotlin interfaces implemented by the generated value classes, `some Shape` existentials
   opened through generic local functions, Swift defaults → Kotlin defaults or the original Swift default expression…).
   It emits Kotlin composables, `Modifier` extensions, value types (`Font`, `Color`, `Edge.Set`, `Animation`…), and the
   matching Swift decoders/renderers, with `#if os(...)` / `#available` guards from the SDK availability data.
3. `verify.py` compiles the generated Swift for **iOS and macOS** and the Kotlin (including the native link), maps every
   compiler error back to the API that produced it, rejects it (with the reason) and regenerates until everything
   compiles. Nothing unverified ships.

```bash
generator/extract.sh                        # symbol graphs from the installed Xcode SDKs
python3 -I generator/verify.py <sg> . build/corefw/macos build/corefw/ios --kotlin
python3 -I generator/api_index.py .         # docs/API.md — every Kotlin entry point
python3 -I generator/coverage_report.py <apple-doc-index.json> .   # docs/COVERAGE.md
```

- `docs/API.md`: the generated Kotlin API (composables, modifiers, value types).
- `docs/COVERAGE.md`: **every entry of Apple's SwiftUI documentation** with its status (generated + compiler-verified,
  covered by an equivalent overload, rejected by the compiler with the error, skipped with the reason, or n/a).

Kotlin naming notes: Kotlin can't overload on parameter names, so when two Swift overloads only differ by labels the
second one gets the label as suffix (`ImageSystemName("star")` for `Image(systemName:)`). Generated declarations are
`@HiddenFromObjC`, so the ObjC/Swift header of your framework only exposes the small runtime surface.

## Benchmark

`bench/` holds 50 complex screens, each written twice — pure SwiftUI (`bench/app/Screens`) and Compose SwiftUI
(`bench/shared/.../screens`) — sharing the same Kotlin `ViewModel`. The harness mounts each version off-screen at a
fixed size, captures pixels, compares them (before and after 30 state mutations), and measures mount and update times.

```bash
scripts/run_full_bench.sh        # release Kotlin + Swift -O, iOS simulator + macOS → ../bench-results/full/report.html
python3 -I scripts/check_screens.py 7    # dev loop for one screen
```

See `bench/BENCH_GUIDE.md` to write screens.

## Using it in your app

1. Add `runtime` to your shared Kotlin module and `export` it from your framework (see `bench/shared/build.gradle.kts`).
2. Add the files of `swift/ComposeSwiftUI` (+ `Generated/`) to your app target; `KotlinModule.swift` re-exports your
   framework module (rename `BenchShared` there).
3. Call `ComposeGenerated.setUp()` once at startup, then use `ComposeView { YourKt.yourScreen() }` anywhere in SwiftUI.
4. Plain SwiftUI code can observe the same view models with `watchFlow` (see `bench/app/FlowState.swift`).

## Limits (current)

- Data-driven SwiftUI containers that take Swift collections + `(Element) -> Content` builders (`ForEach`,
  `List(data)`, `Table`) are replaced by Kotlin loops with `key(...)`. Builders with parameters are not generated;
  `GeometryReader { size -> }` is provided by hand.
- `Shape` methods returning shapes are not generated; `ShapeView(shape, fill, stroke, lineWidth, strokeStyle,
  trimFrom, trimTo)` covers fill / stroke / trim.
- Types without a Kotlin representation (`UTType`, `AttributedString`, `Transferable`, `KeyPath`…) are skipped; see the
  skip reasons in `docs/COVERAGE.md`.
