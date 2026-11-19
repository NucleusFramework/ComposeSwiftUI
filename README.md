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

**Documentation:** the [guide](https://nucleusframework.github.io/ComposeSwiftUI/guide) covers everything in 32 chapters,
from installation and your first app to lists, navigation, animation, accessibility, every Apple framework
integration, previews, performance and troubleshooting. Its Kotlin examples are compiled on every change
(`scripts/check_docs.py`).

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
       id("dev.nucleusframework.compose-swiftui") version "0.9.1"
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
| `.environment(\.colorScheme, .dark)` | `Modifier.environment(colorScheme = ColorScheme.dark)` |
| `Int8` … `UInt64` | `Int` (truncated on the Swift side) |
| `URL`, `UTType`, `Locale`, `Locale.Language`, `Character` | `String`: the URL, the type identifier (`"public.png"`), the locale or language identifier, one character |
| `ColorPicker("Tint", selection: $color)`, `NavigationSplitView(columnVisibility: $v)` | `ColorPicker("Tint", selection = color, onSelectionChange = { color = it })`, same pattern for every binding |
| `listRowBackground(Color.red)`, `mask(Circle())` (a view passed as a value) | `Modifier.listRowBackground { ColorView(Color.red) }`, `Modifier.mask { … }` |
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
| `AsyncImage(url:) { phase in … }` | `AsyncImage(url) { state -> if (state == AsyncImageState.Success) AsyncImageContent(Modifier.resizable()) }` |
| `ScrollViewReader { proxy in … }` | `ScrollViewReader { proxy -> Button("Top") { proxy.scrollTo(0) } }`, rows marked `Modifier.id(…)` |
| `TimelineView(.periodic(from:by:)) { context in … }` | `TimelineView(TimelineSchedule.periodic(0.0, 1.0)) { date -> }` (epoch seconds) |
| `Canvas { context, size in … }` | `Canvas { fill(Color.orange) { ellipse(0.0, 0.0, width, height) }; drawText("Hi", 10.0, 10.0) }` |
| `Table(rows) { TableColumn("Name") { … } }` | `Table(rows, key = { it.id }, selection, onSelectionChange) { column("Name") { Text(it.name) } }` |
| `OutlineGroup(data, children:)`, `List(data, children:)` | `List { OutlineGroup(items, key = { it.id }, children = { it.children }) { Text(it.name) } }` |
| `onKeyPress(keys:phases:action:)` | `Modifier.onKeyPress(keys = setOf("a")) { press -> true }` |
| `scrollTransition { content, phase in … }`, `visualEffect { … }` | `Modifier.scrollTransition { phase, _ -> VisualEffects(opacity = 1 - abs(phase)) }` |
| `alignmentGuide(.leading) { d in … }` | `Modifier.alignmentGuide(HorizontalAlignment.leading) { width, height -> width / 2 }` |
| `navigationDestination(item:)` | `Modifier.navigationDestination(item, onDismiss) { item -> }` |
| Custom `ButtonStyle`, `ToggleStyle`, `LabelStyle` | `Modifier.buttonStyle { isPressed -> StyleLabel(…) }`, `toggleStyle { isOn -> }`, `labelStyle { StyleIcon(); StyleLabel() }` |
| `List(selection: $set)` (multiple) | `List(selectedKeys, onSelectedKeysChange) { Text("A", Modifier.tag("a")) }` |
| `searchable(text:tokens:token:)` | `Modifier.searchable(text, onTextChange, tokens, onTokensChange, key = { it.id }) { Text(it.name) }` |
| `TextField(_:value:format:)` | `NumberField("Amount", value, onValueChange, format = NumberFormat.Currency("EUR"))` |
| `focused($field, equals: .email)` | `Modifier.focused(value = Field.Email, focusedValue = field, onFocusChange = { field = it })` |
| `glassEffectID(_:in:)`, `glassEffectUnion` | `NamespaceScope { ns -> … Modifier.glassEffectID("a", ns) }` |
| `onGeometryChange`, `onScrollGeometryChange`, `onScrollTargetVisibilityChange` | `Modifier.onGeometryChange { frame -> }`, `onScrollGeometryChange { g -> }`, `onScrollTargetVisibilityChange { ids -> }` |
| `fileImporter`, `fileExporter` (text), `PasteButton`, `draggable`, `dropDestination` (text) | `Modifier.fileImporter(…) { urls -> }`, `PasteButton { strings -> }`, `Modifier.dropDestination { items, location -> true }` |
| `phaseAnimator`, `symbolEffect`, `MeshGradient` | `Modifier.phaseAnimator(listOf(VisualEffects(), VisualEffects(scale = 1.2)))`, `Modifier.symbolEffect(SymbolEffectKind.Bounce, value = count)`, `MeshGradient(2, 2, points, colors)` |
| `keyboardType`, `textContentType` | `Modifier.keyboardType(KeyboardType.EmailAddress)`, `Modifier.textContentType(TextContentType.OneTimeCode)` |
| `Image(uiImage:)`, `Image(nsImage:)`, `MultiDatePicker` | `Image(uiImage)` in `iosMain`, `Image(nsImage)` in `macosMain`, `MultiDatePicker("Days", selection, onSelectionChange)` |
| `rotation3DEffect(_:axis:)`, `dynamicTypeSize(...xLarge)` | `Modifier.rotation3DEffect(Angle.degrees(45.0), x = 0.0, y = 1.0, z = 0.0)`, `Modifier.dynamicTypeSize(max = DynamicTypeSize.xLarge)` |
| `containerRelativeFrame(.horizontal) { length, axis in … }` | `Modifier.containerRelativeFrame(Axis.Set.horizontal) { length, axis -> length / 2 }` |
| `presentationDetents(_:selection:)` | `Modifier.presentationDetents(detents, detent) { detent = it }` |
| `Text("Rated \(Image(systemName: "star.fill"))")` | `Text(buildStyledString { append("Rated "); appendSymbol(SFSymbol.starFill) })` |
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
Compose SwiftUI IDE plugin, or in Xcode's canvas. Annotate a top-level composable with `@Preview`, repeat it or use a
multipreview such as `@PreviewLightDark` for variants, and `@PreviewParameter` for sample data:

```kotlin
@Preview(name = "Counter", group = "Screens")
@PreviewLightDark
@Composable
fun CounterPreview() {
    NavigationStack { CounterScreen() }
}
```

`@Preview` also takes `device`, `width` / `height`, `sizeThatFits`, `colorScheme`, `dynamicTypeSize`, `locale`,
`layoutDirection` and `backgroundColor`.

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
Compose's own preview: they render when the file opens and on save (or while typing, or only on demand), in a list,
grid or gallery with zoom and group filter. The caret and the previews follow each other, a preview runs live on the
simulator from its context menu, and compile errors and crashing previews show next to the previews.

Rendering runs the `renderComposeSwiftUIPreviews` Gradle task with the previews of the file: it links the debug
framework of the simulator target (`iosSimulatorArm64`, or `iosX64` on Intel Macs), and a host app that stays running
on the simulator composes the previews in parallel and captures each one as soon as it stops changing, into
`build/composeSwiftUI/previews/`. It needs macOS with Xcode. The host is compiled once per library version (about a
minute); after that a Kotlin change costs the framework link and about two seconds, and re-rendering without a change
about one. Choose the simulator with `composeSwiftUI { previewDevice.set("iPhone 17 Pro") }`. A macOS app's previews
render on the Mac itself (`@Preview(device = "mac")`, or every preview of a module with only the macOS target).

#### From Linux or Windows

The IDE doesn't have to run on the Mac. Set a Mac reached over SSH (key authentication, *Remote Login* on) in
*Settings › Tools › Compose SwiftUI Previews › Remote Mac*, and the plugin keeps the IDE, indexing and your other builds
on your machine while the Mac does what needs Apple's tools: the project is synchronized there with rsync (only what
changed), previews render there and come back, and live previews stream over SSH. The *iOS Simulator* tool window
shows a simulator of the Mac live, with touches, keyboard, Home and the app switcher, and an *iOS App* run
configuration builds the app with Xcode on the Mac and runs it on the simulator picked in the toolbar. See
[Previews › From Linux or Windows](docs/guide/29-previews.md#from-linux-or-windows-a-remote-mac).

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
  indented), must not be private and must take no parameters except one `@PreviewParameter`.
- Only source sets compiled into every Apple framework are scanned: `commonMain`, `appleMain`, and `iosMain` for an
  iOS-only app. A preview in `iosMain` is skipped if the module also has macOS targets.
- If you added the `ComposeSwiftUI` folder to Xcode as a group before this version, add the new
  `ComposeSwiftUIPreviews.swift` file to the app target once.
- If the folder is a group, add `Interactive.swift`, `Advanced.swift`, `Completions.swift` and
  `Generated/GenEncoders.swift` to the app target once when you upgrade past 0.4.1.

AI agents (Claude Code, Junie, Codex, Gemini…) get the previews as tools, like Android Studio's agent: on the IDE's MCP
server (render a preview or throwaway code, UI checks, compare with a design), on the plugin's own MCP server in
Android Studio (add `"compose-swiftui": { "httpUrl": "http://127.0.0.1:64360/mcp" }` to its `mcp.json`, in
*Settings › Tools › AI › MCP Servers*), or on the command line
(`listComposeSwiftUIPreviews`, and `renderComposeSwiftUIPreviews`, which writes a `report.md` with each image, its
component tree and its UI checks). See [Previews › For AI agents](docs/guide/29-previews.md#for-ai-agents).

## Apple frameworks: maps, charts, in-app purchases

The SwiftUI views and modifiers of MapKit, Swift Charts and StoreKit are available from Kotlin. Each framework has its
own renderer file, which the Gradle plugin writes to `iosApp/ComposeSwiftUIIntegrations` next to the renderer. Add
only the files you need to the app target, so your app links only those frameworks, and register each once at launch.
To keep only those files in the folder (and add the whole folder), list them in Gradle:
`composeSwiftUI { integrations.set(listOf("MapKit", "Charts", "StoreKit")) }`.

```swift
@main
struct MyApp: App {
    init() {
        ComposeIntegrations.registerMapKit()     // MapKit.swift
        ComposeIntegrations.registerCharts()     // Charts.swift
        ComposeIntegrations.registerStoreKit()   // StoreKit.swift
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

```kotlin
// MapKit
var position by remember { mutableStateOf(MapCameraPosition.region(MKCoordinateRegion(paris, 0.05, 0.05))) }
Map(position = position, onPositionChange = { position = it }, modifier = Modifier.mapStyle(MapStyle.hybrid)) {
    Marker("Eiffel Tower", eiffel, systemImage = "star.fill")
    MapCircle(paris, radius = 800.0, style = MapContentStyle(foregroundStyle = Color.blue.opacity(0.2)))
}

// Swift Charts
Chart(modifier = Modifier.frame(height = 200.0)) {
    for ((month, sales) in data) {
        BarMark(x = PlottableValue.value("Month", month), y = PlottableValue.value("Sales", sales))
    }
}

// StoreKit
SubscriptionStoreView(groupId = "21482456", modifier = Modifier.storeStyle(StoreContentStyle(controlStyle = SubscriptionStoreControlStyle.picker)))
```

The other frameworks follow the same pattern:

| File | Register | Kotlin APIs |
|---|---|---|
| `PhotosUI.swift` | `registerPhotosUI()` | `PhotosPicker`, `Modifier.photosPicker`, picker styles, shared albums |
| `WebKit.swift` | `registerWebKit()` | `WebView` with a `WebViewController`, `Modifier.webViewStyle` |
| `AVKit.swift` | `registerAVKit()` | `VideoPlayer`, camera capture events, `quickLookPreview` |
| `AuthenticationServices.swift` | `registerAuthenticationServices()` | `SignInWithAppleButton`, passkey / password requests, `LocationButton` |
| `PassKit.swift` | `registerPassKit()` | `PayWithApplePayButton`, `AddPassToWalletButton` |
| `MusicKit.swift` | `registerMusicKit()`, `registerDataDetection()` | `musicSubscriptionOffer`, music picker, data detection |
| `Translation.swift` | `registerTranslation()` | `translationPresentation`, `translationTask` |
| `SpriteKit.swift` | `registerSpriteKit()` | `SpriteView`, `SceneView`, game controller events |
| `RealityKit.swift` | `registerRealityKit()` | `RealityView` with bundled models, `Modifier.realityViewStyle` |
| `AppIntents.swift` | `registerAppIntents()` | `ShortcutsLink`, App Intents views |
| `DeviceActivity.swift` | `registerDeviceActivity()` | `DeviceActivityReport` |
| `WatchKit.swift` | `registerWatchKit()` | `NowPlayingView` |
| `ManagedAppDistribution.swift` | `registerManagedAppDistribution()` | `ManagedContentView` |
| `PermissionKit.swift` | `registerPermissionKit()` | `CommunicationLimitsButton` |
| `ScreenCaptureKit.swift` | `registerScreenCaptureKit()` | `Modifier.recordingEditor` |
| `WorkoutKit.swift` | `registerWorkoutKit()` | `Modifier.workoutPreview` |
| `DeviceDiscoveryUI.swift` | `registerDeviceDiscoveryUI()` | `DevicePicker` |
| `TipKit.swift` | `registerTipKit()` | `TipView`, `Modifier.popoverTip`, `Modifier.tipStyle`, `Modifier.tipAnchor` |
| `WidgetKit.swift` | `registerWidgetKit()` | `Modifier.widgetStyle`, `widgetLabel`, `activityStyle`, `controlWidgetText`, `AccessoryWidgetGroup`, `WidgetImage` |
| `HealthKitUI.swift` | `registerHealthKitUI()` | `Modifier.healthDataAccessRequest` |
| `ContactsUI.swift` | `registerContactsUI()` | `ContactAccessButton`, `Modifier.contactAccessPicker` |
| `FamilyControls.swift` | `registerFamilyControls()` | `FamilyActivityPicker` |
| `FinanceKitUI.swift` | `registerFinanceKitUI()` | `AddOrderToWalletButton`, `TransactionPicker` |
| `AutomatedDeviceEnrollment.swift` | `registerAutomatedDeviceEnrollment()` | `Modifier.automatedDeviceEnrollmentAddition` |
| `ImagePlayground.swift` | `registerImagePlayground()` | `Modifier.imagePlaygroundSheet` |
| `GameSave.swift` | `registerGameSave()` | `Modifier.gameSaveSyncingAlert` |
| `ClassKitUI.swift` | `registerClassKitUI()` | `AssignedDocumentLabel` |
| `SecurityUI.swift` | `registerSecurityUI()` | `Modifier.certificateSheet` |
| `JournalingSuggestions.swift` | `registerJournalingSuggestions()` | `JournalingSuggestionsPicker` |
| `SpatialAnchors.swift` | `registerSpatialAnchors()` | `Modifier.spatialAnchors` |

Widgets can be written in Kotlin too: the extension keeps its few-line Swift `@main` (WidgetKit requires it), and
`ComposeTimelineProvider` / `ComposeWidgetView` (WidgetKit.swift) take the timeline and the views from Kotlin:

```swift
@main struct WeatherWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "weather", provider: ComposeTimelineProvider { family in WeatherKt.timeline(family: family) }) { entry in
            ComposeWidgetView(entry) { input, family in WeatherKt.widget(input: input, family: family) }
        }
    }
}
```

A register function exists only on the platforms its framework supports, so call it under the same `#if os(…)`.

Swift-only data stays on the Swift side: map items are coordinates and names, chart values are strings, numbers or
`ChartDate`s, and products are identified by their App Store Connect ids (purchases, transactions and subscription
statuses arrive as Kotlin data classes). Vectorized chart plots (`ChartPlot`) are drawn as one mark per item, and 3D
charts take numeric values.

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
- Swift protocols with a Kotlin implementation are hand-written (preference keys, container values, custom
  animations, transitions, text renderers, drop delegates, alignment IDs, presentation detents). Those tied to Swift
  data models aren't available: `FileDocument`, `Transferable` types other than text and URLs, `Publisher`,
  `ObservableObject`, key paths. An `AttributedString` parameter or binding takes a plain `String` (attributes are
  dropped); for mixed-style text, use `Text(StyledString)`. [docs/COVERAGE.md](docs/COVERAGE.md) gives the reason for
  each one.
- Data-driven containers take Kotlin lists: a loop or `LazyForEach` for `List(data)` / `ForEach(data)`, and the Kotlin
  `Table(rows)`, `OutlineGroup(items, children)` and `searchable(tokens)`.
- On iOS, two benchmark screens differ from plain SwiftUI by less than a pixel (one SF Symbol, and the search field
  placeholder after updates).

## Contributing

[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) explains how to regenerate the bindings, run the benchmark and publish a
release.
