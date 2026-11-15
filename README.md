# Compose SwiftUI

Compose SwiftUI lets you write SwiftUI screens in Kotlin. It uses the Compose runtime, not Compose UI: your
composables build a tree of SwiftUI views, and the app renders them with Apple's SwiftUI. Compose HTML does the same
thing for the browser DOM.

Your Kotlin `ViewModel`s, `StateFlow`s and `remember` state work unchanged. You only rewrite the UI layer.

```kotlin
fun counterScreen() = composeSwiftUI {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    NavigationStack {
        VStack(spacing = 16.0, modifier = Modifier.navigationTitle("Counter")) {
            Image(systemName = "swift", modifier = Modifier.font(Font.largeTitle).foregroundStyle(Color.orange))
            Text("Count: $count", modifier = Modifier.font(Font.title))
            Button("Increment", action = vm::increment)
        }
    }
}
```

```swift
ComposeView { CounterScreenKt.counterScreen() }
```

Supported platforms: iOS 17+, macOS 14+, tvOS 17+ and watchOS 10+. Requires Kotlin 2.4.20 or later.

## Quick start

1. Apply the plugin in your shared KMP module:

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

   The plugin adds the `dev.nucleusframework:compose-swiftui` library, applies the Compose compiler and exports the
   library from your frameworks.

2. Build the app once in Xcode. The plugin writes the Swift renderer to `iosApp/ComposeSwiftUI` during the standard
   `embedAndSignAppleFrameworkForXcode` build phase, and rewrites it on every build after that.

3. Drag the `iosApp/ComposeSwiftUI` folder into your app target. You only do this once.

4. Write a screen in Kotlin and show it from Swift:

   ```kotlin
   import dev.nucleusframework.composeswiftui.*

   fun helloScreen() = composeSwiftUI {
       Text("Hello from Kotlin", modifier = Modifier.padding())
   }
   ```

   ```swift
   ComposeView { HelloScreenKt.helloScreen() }
   ```

`ComposeView` is a regular SwiftUI view, so you can put it in a `TabView`, a `sheet` or next to SwiftUI views.
[samples/kmp-app](samples/kmp-app) is a complete project you can build and run.

To change where the renderer is written or which framework it imports:

```kotlin
composeSwiftUI {
    swiftOutputDir = layout.projectDirectory.dir("../iosApp/ComposeSwiftUI")
    frameworkName = "Shared"
}
```

## Writing screens

The Kotlin API is generated from Apple's SwiftUI SDK, so names match SwiftUI. These rules cover most of the
translation:

| SwiftUI | Compose SwiftUI |
|---|---|
| `Text("Hi").font(.title).padding()` | `Text("Hi", modifier = Modifier.font(Font.title).padding())` |
| `VStack(spacing: 8) { … }` | `VStack(spacing = 8.0) { … }` |
| `Image(systemName: "star")` | `Image(systemName = "star")` |
| `Section { … } header: { Text("A") }` | `Section(header = { Text("A") }, content = { … })` |
| `Toggle("Wi-Fi", isOn: $on)` | `Toggle("Wi-Fi", isOn = on, onIsOnChange = { on = it })` |
| `ForEach(items, id: \.id) { … }` | `for (item in items) key(item.id) { … }` or `ForEach(items, key = { it.id }) { … }` |
| `Color.red.opacity(0.5)`, `.secondary` | `Color.red.opacity(0.5)`, `ShapeStyle.secondary` |
| `CGFloat` | `Double` |
| `"**Bold**"` or a localization key | `LocalizedKey("hello.title")`. A plain `String` is shown verbatim. |

Modifiers apply in the order you chain them, exactly as in Swift.

A few SwiftUI features need closures, property wrappers or Swift types that Kotlin can't express. They have their own
API:

| SwiftUI | Compose SwiftUI |
|---|---|
| `NavigationStack(path:)` + `navigationDestination(for:)` | `NavigationStack(path, onPathChange, destination = { value -> }) { … }` |
| `sheet(item:)`, `fullScreenCover(item:)`, `popover(item:)` | `Modifier.sheet(item, onDismiss) { item -> }`, same for the others |
| `alert(_:isPresented:presenting:)`, `confirmationDialog(…presenting:)` | `Modifier.alert(title, item, onDismiss, actions, message)` |
| `@Environment(\.colorScheme)`, `\.dismiss`, size classes, locale… | `EnvironmentReader { env -> }` |
| `@FocusState` + `.focused($x)` | `Modifier.focused(isFocused, onFocusChange)` |
| `DragGesture`, `MagnifyGesture`, `RotateGesture`, `SpatialTapGesture` | `Modifier.dragGesture(…)`, `magnifyGesture`, `rotateGesture`, `onTapGesture { x, y -> }` |
| `@Namespace` + `matchedGeometryEffect` | `NamespaceScope { ns -> }` + `Modifier.matchedGeometryEffect(id, ns)` |
| `withAnimation(.spring) { … }` | `withAnimation(Animation.spring) { … }` |
| `GeometryReader { proxy in … }` | `GeometryReader { size -> }` |
| Custom `Layout` | `CustomLayout(policy) { … }` with a Kotlin `LayoutPolicy` |
| Custom `Shape` | `PathShape { moveTo(0.0, 0.0); lineTo(1.0, 0.5); close() }` |
| `Circle().trim(from:to:).stroke(…)` | `ShapeView(Circle(), stroke = …, trimFrom = 0.0, trimTo = 0.7)` |
| A Swift view (MapKit, `UIViewRepresentable`…) | `NativeView("map")` + `ComposeRegistry.nativeViews["map"] = { args in … }` |
| Values passed from Swift | `ComposeView(input: value) { … }` + `hostInput<T>()` in Kotlin |

Reference:

- [docs/API.md](docs/API.md) lists every generated composable, modifier and value type.
- [docs/COVERAGE.md](docs/COVERAGE.md) lists every entry of Apple's SwiftUI documentation with its status: generated,
  covered by another overload, or skipped with the reason.

## How it works

```
Kotlin                                         Swift
@Composable VStack / Text / …                  ComposeView
  → one node per view, with its arguments  ──▶   one NodeView per node
    and modifier chain                             → builds the SwiftUI view and applies its modifiers
Recomposition updates changed nodes        ──▶   only those NodeViews re-render
Kotlin callbacks                           ◀──   actions and bindings
```

Each node has an observable model on the Swift side. When state changes, Compose recomposes and updates only the
nodes whose arguments changed, and SwiftUI re-renders only those views. Children keep stable identities, so SwiftUI
animations, transitions and the state of native controls behave as in a SwiftUI app.

Bindings such as `TextField`, `Toggle` and `Slider` are controlled by Kotlin state. A user edit writes the new value,
calls your Kotlin callback and recomposes before SwiftUI draws the next frame, so typing doesn't jump.

The Kotlin API and the matching Swift renderer are generated from the symbol graphs of the iOS, macOS, tvOS and watchOS
SDKs. Every generated API is compiled for all four platforms before it ships.

## Performance

A benchmark of 50 screens, each written once in plain SwiftUI and once in Compose SwiftUI with the same `ViewModel`
(release builds, mean of per-screen medians):

| | Pixel-identical | Mount: SwiftUI → Compose SwiftUI | Update: SwiftUI → Compose SwiftUI |
|---|---|---|---|
| iOS simulator | 48 / 50 | 24.3 → 25.8 ms | 1.81 → 1.49 ms |
| macOS | 50 / 50 | 15.0 → 17.2 ms | 2.15 → 2.13 ms |

Updates are as fast as plain SwiftUI or faster, because only the views whose data changed re-render. Mounting a screen
costs 6% more on iOS and 14% more on macOS. Composition itself takes about 0.3 ms; the rest comes from SwiftUI building
one type-erased view per node, because the tree is built at runtime. Full results are in
[docs/bench/RESULTS.md](docs/bench/RESULTS.md).

## Limitations

- visionOS isn't supported: Kotlin/Native has no visionOS target.
- APIs that take Swift-only types (`UTType`, `Transferable`, `KeyPath`, `AttributedString`…) aren't available.
  [docs/COVERAGE.md](docs/COVERAGE.md) gives the reason for each one.
- Data-driven containers such as `List(data)` and `Table` take Kotlin loops instead of a collection.
- On iOS, two benchmark screens differ from plain SwiftUI by less than a pixel (one SF Symbol, and the search field
  placeholder after updates).

## Contributing

[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) explains how to regenerate the bindings, run the benchmark and publish a
release.
