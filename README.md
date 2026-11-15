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
            Image(systemName = SFSymbol.swift, modifier = Modifier.font(Font.largeTitle).foregroundStyle(Color.orange))
            Text("Count: $count", modifier = Modifier.font(Font.title))
            Button("Increment", action = vm::increment)
        }
    }
}
```

```swift
ComposeView { CounterScreenKt.counterScreen() }
```

## Requirements

| Tool | Minimum |
|---|---|
| Xcode | 27.2 (Swift 6.4) |
| Kotlin | 2.4.20 |
| JDK | 17 |
| Deployment target | iOS 17, macOS 14, tvOS 17, watchOS 10 |

Xcode 27.2 is needed to build, because the Swift renderer calls the newest SwiftUI APIs behind `#available` checks.
Apps still run on the older OS versions above.

## Quick start

1. Apply the plugin in your shared KMP module:

   ```kotlin
   plugins {
       kotlin("multiplatform")
       id("dev.nucleusframework.compose-swiftui") version "0.4.0"
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

To try it without setting anything up, open `samples/kmp-app/iosApp/iosApp.xcodeproj` and run it. The sample builds
the library and the plugin from the sources of this repository and shows Navigation 3, a `ViewModel`, Compose
Multiplatform content and Compose Multiplatform string resources.

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
| `Image(systemName: "star")` | `Image(systemName = SFSymbol.star)` |
| `Section { … } header: { Text("A") }` | `Section(header = { Text("A") }, content = { … })` |
| `Toggle("Wi-Fi", isOn: $on)` | `Toggle("Wi-Fi", isOn = on, onIsOnChange = { on = it })` |
| `ForEach(items, id: \.id) { … }` | `for (item in items) key(item.id) { … }`, or `LazyForEach(items, key = { it.id }) { … }` for long lists |
| `Color.red.opacity(0.5)`, `.secondary` | `Color.red.opacity(0.5)`, `ShapeStyle.secondary` |
| `CGFloat` | `Double` |
| `"**Bold**"` or a localization key | `LocalizedKey("hello.title")`. A plain `String` is shown verbatim. |

Modifiers apply in the order you chain them, exactly as in Swift.

A few SwiftUI features need closures, property wrappers or Swift types that Kotlin can't express. They have their own
API:

| SwiftUI | Compose SwiftUI |
|---|---|
| `TabView(selection:)` + `Tab(_:systemImage:value:)` | `TabView(selection, onSelectionChange) { Tab("Home", systemImage = SFSymbol.house, value = AppTab.Home) { … } }` |
| `Text` concatenation, `AttributedString` | `Text(buildStyledString { withStyle(SpanStyle(color = Color.gray, fontSize = 13.0)) { append("…") } })` |
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

### Long lists

A Kotlin loop composes every row up front. For a long list, use `LazyForEach`: inside a `List`, each row is composed
only while SwiftUI shows it, and disposed when it scrolls away.

```kotlin
List {
    LazyForEach(messages, key = { it.id }) { message ->
        MessageRow(message)
    }
}
```

A row recomposes when its item changes, so prefer immutable items such as data classes. Each row should be one view
(wrap several in an `HStack` or `VStack`). The list is fastest when the `LazyForEach` is the only child of its `List`.

## Previews

Compose SwiftUI draws with real SwiftUI, so previews render with SwiftUI: in IntelliJ IDEA / Android Studio with the
Compose SwiftUI IDE plugin, or in Xcode's canvas. Annotate a
top-level, parameterless composable with `@Preview`:

```kotlin
@Preview(name = "Counter")
@Composable
fun CounterPreview() {
    NavigationStack { CounterScreen() }
}
```

### In IntelliJ IDEA and Android Studio

Previews in the IDE need the Compose SwiftUI IDE plugin. It is not on the JetBrains Marketplace yet, so for now it is
installed manually:

1. Download `compose-swiftui-intellij-plugin-<version>.zip` from the
   [GitHub releases](https://github.com/NucleusFramework/ComposeSwiftUI/releases), or build it with
   `./gradlew -p intellij-plugin buildPlugin` (the zip lands in `intellij-plugin/build/distributions/`).
2. In the IDE, open *Settings › Plugins › ⚙ › Install Plugin from Disk…*, pick the zip, and restart the IDE when it
   asks.

Repeat these steps to update: the IDE won't offer updates for a plugin installed from disk.

A Kotlin file with previews opens in a split editor, code on the left and the rendered previews on the right, like
Compose's own preview. Each preview has a gutter icon that renders it and scrolls to it, and the previews are rendered
again every time the file is saved.

Rendering runs the `renderComposeSwiftUIPreviews` Gradle task: it links the debug framework of the simulator target
(`iosSimulatorArm64`, or `iosX64` on Intel Macs), builds a small host app with the renderer, and snapshots every
preview on the booted simulator (or boots the last available iPhone) into `build/composeSwiftUI/previews/`. It needs
macOS with Xcode. The first run compiles the renderer (about a minute); after that, a Kotlin change only costs the
framework link. Choose the simulator with `composeSwiftUI { previewDevice.set("iPhone 17 Pro") }`.

### In Xcode

On every build, the plugin writes `ComposeSwiftUIPreviews.swift` next to the renderer (`iosApp/ComposeSwiftUI`), with
one Swift `#Preview` per annotated function. Open that file in Xcode to see the previews in the canvas. The same file
declares `ComposeSwiftUIPreviewGallery`, a SwiftUI view that lists every preview, so you can also browse them on a
simulator or device, for example from a debug menu:

```swift
.sheet(isPresented: $showPreviews) { ComposeSwiftUIPreviewGallery() }
```

- Previews are found by scanning the sources, so any annotation named `Preview` works, including
  `org.jetbrains.compose.ui.tooling.preview.Preview`. The function must be declared at the start of a line (not
  indented), must not be private and must take no parameters.
- Only source sets compiled into every Apple framework are scanned: `commonMain`, `appleMain`, and `iosMain` for an
  iOS-only app. A preview in `iosMain` is skipped if the module also has macOS targets.
- If you added the `ComposeSwiftUI` folder to Xcode as a group before this version, add the new
  `ComposeSwiftUIPreviews.swift` file to the app target once.

## Using it with other Compose libraries

### Navigation 3

`NavDisplay` shows any observable back stack with SwiftUI navigation, so you can keep the Navigation 3 runtime
(`NavKey`, `entryProvider`, your back stack) and get the native push animation and swipe back:

```kotlin
val backStack = remember { mutableStateListOf<Route>(Route.Home) }
val entries = entryProvider<Route> {
    entry<Route.Home> { HomeScreen(onOpen = { backStack.add(it) }) }
    entry<Route.Detail> { key -> DetailScreen(key.id) }
}
NavDisplay(backStack) { route -> entries(route).Content() }
```

Push with `backStack.add(...)`. The SwiftUI back button and swipe remove keys from the back stack. `NavDisplay` from
`navigation3-ui` draws with Compose UI, so use this one instead.

### Compose Multiplatform content (iOS)

Embed a Compose Multiplatform screen in a Compose SwiftUI screen the same way you would in SwiftUI, through its view
controller:

```kotlin
// iosMain
UIViewControllerView(modifier = Modifier.frame(height = 260.0)) {
    ComposeUIViewController { SalesChart() }
}
```

By default the embedded view takes the size SwiftUI proposes, so give it a `frame`. For content of variable height,
such as a multi-line label, pass `sizing = NativeViewSizing.FitHeight`: the view takes the proposed width and the
height its content needs. `update` runs on every recomposition, so the view follows Kotlin state:

```kotlin
UIKitView(
    sizing = NativeViewSizing.FitHeight,
    update = { (it as UILabel).text = paragraph },
) { UILabel().apply { numberOfLines = 0 } }
```

The embedded content has its own composition. Share state between the two through a `ViewModel` or a `StateFlow`.
Your app's `Info.plist` needs `CADisableMinimumFrameDurationOnPhone` set to `true`, as for any Compose Multiplatform
app. `UIKitView`, `NSViewControllerView` and `AppKitView` embed other UIKit and AppKit views the same way.

### Compose Multiplatform resources

`stringResource(Res.string.title)` works in Compose SwiftUI screens once you provide Compose UI's `LocalDensity`,
which Compose Multiplatform resources read:

```kotlin
fun app() = composeSwiftUI {
    CompositionLocalProvider(LocalDensity provides Density(UIScreen.mainScreen.scale.toFloat())) {
        Text(stringResource(Res.string.title))
    }
}
```

The suspending `getString(Res.string.title)` works without it, for example in a `ViewModel`.

## Reference

- The [documentation website](https://nucleusframework.github.io/ComposeSwiftUI/) has a page for every supported SwiftUI
  API, with its Swift declaration, the Kotlin equivalent and a link to Apple's documentation.
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

Two benchmarks compare Compose SwiftUI with plain SwiftUI (release builds). Full results are in
[docs/bench/RESULTS.md](docs/bench/RESULTS.md).

**50 screens**, each written once in plain SwiftUI and once in Compose SwiftUI, reading the same Kotlin `ViewModel`
(mean of per-screen medians). The SwiftUI version observes the whole screen state through one `ObservableObject`, the
usual way to bridge a `StateFlow` in a KMP app:

| | Pixel-identical | Mount: SwiftUI → Compose SwiftUI | Update: SwiftUI → Compose SwiftUI |
|---|---|---|---|
| iOS simulator | 48 / 50 | 24.1 → 26.1 ms | 1.92 → 1.48 ms |
| macOS | 50 / 50 | 14.4 → 17.1 ms | 2.11 → 2.07 ms |

**Scenarios** that the 50 screens don't cover, against plain SwiftUI with native `@Observable` models (one object per
row, so a change re-evaluates only the views that read it). iOS simulator, list rows with an icon, two lines of text and
an unread dot, all pixel-identical (macOS results are in the full results):

| | Plain SwiftUI | Compose SwiftUI, `LazyForEach` | Compose SwiftUI, Kotlin loop |
|---|---|---|---|
| 10,000-row `List`: mount | 23.5 ms | 28.2 ms | 818.8 ms |
| 10,000 rows: one row changes | 0.16 ms | 0.41 ms | 3.24 ms |
| 10,000 rows: every row changes | 4.0 ms | 4.5 ms | 78.5 ms |
| Scrolling, per page of rows | 9.7 ms | 13.8 ms | 22.9 ms |
| 60 bars, animated change: apply | 5.1 ms | 7.5 ms (same frame count, no hitches) | — |

Updates of a regular screen are as fast as plain SwiftUI or faster, because only the views whose data changed
re-render. Mounting costs 8% to 18% more, and a new row costs 40% (iOS) to 70% (macOS) more while scrolling: SwiftUI builds one
type-erased view per node because the tree is built at runtime. For long lists, use `LazyForEach`: with a Kotlin loop,
every row is composed and kept up to date even off screen.

## Limitations

- visionOS isn't supported: Kotlin/Native has no visionOS target.
- APIs that take Swift-only types (`UTType`, `Transferable`, `KeyPath`…) aren't available. For mixed-style
  text, use `Text(StyledString)` instead of `AttributedString`. [docs/COVERAGE.md](docs/COVERAGE.md) gives the reason for each one.
- Data-driven containers such as `List(data)` and `Table` take a Kotlin loop or `LazyForEach` instead of a collection.
- On iOS, two benchmark screens differ from plain SwiftUI by less than a pixel (one SF Symbol, and the search field
  placeholder after updates).

## Contributing

[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) explains how to regenerate the bindings, run the benchmark and publish a
release.
