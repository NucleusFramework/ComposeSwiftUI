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

- `runtime/` — Kotlin Multiplatform library (iOS, macOS, tvOS, watchOS): `SNode` tree, `SNodeApplier`,
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
./gradlew :runtime:linkDebugFrameworkMacosArm64 :runtime:linkDebugFrameworkIosSimulatorArm64 -Pcsui.coreOnly=true  # core frameworks
python3 -I generator/verify.py <sg> . <core-framework-root> --kotlin
python3 -I generator/api_index.py .         # docs/API.md — every Kotlin entry point
python3 -I generator/coverage_report.py <apple-doc-index.json> .   # docs/COVERAGE.md
```

- `docs/API.md`: the generated Kotlin API (composables, modifiers, value types).
- `docs/COVERAGE.md`: **every entry of Apple's SwiftUI documentation** with its status (generated + compiler-verified,
  covered by an equivalent overload, rejected by the compiler with the error, skipped with the reason, or n/a).

Kotlin naming notes: Kotlin can't overload on parameter names. When Swift overloads only differ by their labels, the
generator emits one label-dispatch function taking every label as an optional argument, so the Swift spelling works:
`Image(systemName = "star")`, `Image("photo")`, `Modifier.alert(…, isPresented = …)`. (The former suffixed names such as
`ImageSystemName` remain as deprecated aliases.) Generated declarations are
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

**1. Shared Kotlin module** — one plugin line (Kotlin 2.4.20+):

```kotlin
plugins {
    kotlin("multiplatform")
    id("dev.nucleusframework.compose-swiftui") version "1.0.0"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "Shared" }
    }
}
```

The plugin adds `dev.nucleusframework:compose-swiftui`, applies the Compose compiler, exports the library from your
Apple frameworks, and writes the Swift renderer into `iosApp/ComposeSwiftUI` on every Xcode build (it hooks the
standard `embedAndSignAppleFrameworkForXcode` build phase of KMP apps).

**2. Xcode, once** — add the `ComposeSwiftUI` folder to your app target (drag it into the project).

**3. Swift**

```swift
ComposeView { CounterScreenKt.counterScreen() }
```

```kotlin
import dev.nucleusframework.composeswiftui.*

fun counterScreen() = composeSwiftUI {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    VStack(spacing = 16.0) {
        Text("Count: $count", modifier = Modifier.font(Font.title))
        Button("Increment", action = vm::increment)
    }
}
```

Options: `composeSwiftUI { swiftOutputDir = …; frameworkName = "Shared" }`. A complete project lives in
[samples/kmp-app](samples/kmp-app) (`xcodegen` spec in `iosApp/project.yml`).

### Publishing (maintainers)

Tag `vX.Y.Z`: `publish-maven.yaml` publishes the library to Maven Central, `publish-plugin.yaml` the plugin to the
Gradle Plugin Portal. Repository secrets: `MAVENCENTRALUSERNAME`, `MAVENCENTRALPASSWORD`, `SIGNINGINMEMORYKEY`,
`SIGNINGKEYID`, `SIGNINGPASSWORD`, `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`.

## Beyond the generated API (hand-written, `Extras.kt` / `Handwritten.swift`)

| SwiftUI | Compose SwiftUI |
|---|---|
| `ForEach(items, id:)` | `ForEach(items, key = { it.id }) { item -> }` (or a Kotlin `for` + `key`) |
| `NavigationStack(path:)` + `navigationDestination(for:)` | `NavigationStack(path, onPathChange, destination = { value -> }) { root }` |
| `sheet(item:)`, `fullScreenCover(item:)`, `popover(item:)`, `alert(…presenting:)`, `confirmationDialog(…presenting:)` | `Modifier.sheet(item, onDismiss) { item -> }`… |
| `@Environment(\.colorScheme / .dynamicTypeSize / .horizontalSizeClass / .locale / .layoutDirection / .isEnabled / .isPresented / .dismiss)` | `EnvironmentReader { env -> }` |
| `@FocusState` + `.focused($x)` | `Modifier.focused(isFocused, onFocusChange)` |
| `DragGesture`, `MagnifyGesture`, `RotateGesture`, `SpatialTapGesture` | `Modifier.dragGesture(…)`, `magnifyGesture`, `rotateGesture`, `onTapGesture { x, y -> }` |
| `@Namespace` + `matchedGeometryEffect`, `matchedTransitionSource`, `.navigationTransition(.zoom)` | `NamespaceScope { ns -> }` + `Modifier.matchedGeometryEffect(id, ns)`, `matchedTransitionSource`, `zoomTransition` |
| `withAnimation(.spring) { }` | `withAnimation(Animation.spring) { state = … }` (recomposition lands in the animated transaction) |
| custom `Layout` | `CustomLayout(policy: LayoutPolicy) { content }` |
| custom `Shape` | `PathShape { moveTo(0.0, 0.0); lineTo(1.0, 0.5); close() }` (unit coordinates) |
| `GeometryReader { proxy in }` | `GeometryReader { size -> }` |
| any Swift view (MapKit, `UIViewRepresentable`…) | `NativeView("map", modifier, args…)` + `ComposeRegistry.nativeViews["map"] = { args in … }` |
| `Text("**Hello**")` / localized keys | `Text(LocalizedKey("hello.title"))` or `"hello.title".localized` (plain `String` = verbatim) |
| `Circle().trim(from:to:).stroke(style:)` | `ShapeView(Circle(), stroke = …, strokeStyle = StrokeStyle(…), trimFrom, trimTo)` |
| reactive inputs from Swift | `ComposeView(input: value) { … }` + Kotlin `hostInput<T>()` |

## Performance

The renderer is built so that Compose's targeted recomposition pays off on the SwiftUI side:

- one Observation-tracked model per node: a state change re-evaluates only the nodes whose arguments changed
  (plain SwiftUI re-evaluates the whole `body` that reads the state);
- modifiers are applied with a generic recursive chain (`ModChain`): the concrete `ModifiedContent<…>` type is kept at
  every step and a node costs a single `AnyView`;
- arguments are read through typed Kotlin accessors (`SArgs`): no NSArray bridging, no Swift dynamic casts;
- methods of the concrete type at the head of a chain (`Text.font`, `Image.resizable`…) are applied as Swift would.

Release numbers for the 50 screens ([docs/bench/RESULTS.md](docs/bench/RESULTS.md), mean of per-screen medians):

| | Pixel-identical | Mount (SwiftUI → Compose SwiftUI) | Update (SwiftUI → Compose SwiftUI) |
|---|---|---|---|
| iOS simulator | 48 / 50 | 24.3 → 25.8 ms (×1.06) | 1.81 → 1.49 ms (**×0.82, faster**) |
| macOS | 50 / 50 | 15.0 → 17.2 ms (×1.14) | 2.15 → 2.13 ms (×0.99) |

Updates are as fast or faster than plain SwiftUI: only the nodes whose arguments changed are re-evaluated. Mounting
costs a few percent more: Kotlin composition is ~0.3 ms per screen, and the rest is SwiftUI building one extra
type-erased layer per dynamic node (`AnyView` + `ForEach` element), the price of a tree decided at runtime instead of at
compile time.

## Platforms

iOS (arm64, simulator arm64/x64), macOS (arm64, x64), tvOS (arm64, simulator), watchOS (arm64, deviceArm64, simulator).
The generator reads the symbol graphs of every platform and emits `#if os(...)` / `#available` guards; `verify.py`
typechecks the generated Swift for iOS, macOS, tvOS and watchOS. visionOS has no Kotlin/Native target.

## Installing the Swift renderer in an app

```bash
./gradlew :runtime:installSwiftRenderer -Pdest=/path/to/App/ComposeSwiftUI -Pframework=Shared
```

copies the renderer sources and writes `KotlinModule.swift` re-exporting your Kotlin framework (`Shared`).

## Known differences

- Two benchmark screens show sub-pixel differences on iOS only (an SF Symbol vertically centred 1 px apart in one
  column, and the Liquid Glass search field placeholder after updates); macOS is pixel-identical on all 50 screens.
- APIs whose Swift types have no Kotlin representation (`UTType`, `Transferable`, `KeyPath`, `AttributedString`…) are
  listed with the reason in `docs/COVERAGE.md`.
