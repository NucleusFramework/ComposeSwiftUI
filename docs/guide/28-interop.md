<!-- group: Platforms and tools -->
# Interop

> Embed Swift, UIKit and AppKit views in Kotlin screens, put Kotlin screens in an existing SwiftUI or UIKit app, pass values both ways, and combine Compose SwiftUI with Compose Multiplatform, Navigation 3 and an Android app.

Compose SwiftUI is designed to be adopted one screen at a time, in either direction: Kotlin screens inside a Swift
app, and Swift views inside Kotlin screens. This chapter covers each boundary.

| Direction | Tool |
|---|---|
| A Kotlin screen in SwiftUI | `ComposeView { MyScreenKt.myScreen() }` |
| A Kotlin screen in UIKit / AppKit | `UIHostingController(rootView: ComposeView { … })`, `NSHostingView` |
| Swift values into a Kotlin screen | `ComposeView(input: value)` + `hostInput<T>()` |
| A Kotlin `StateFlow` observed from Swift | `watchFlow(flow) { value in … }` |
| A custom SwiftUI view in a Kotlin screen | `NativeView("key", …)` + `ComposeRegistry.nativeViews["key"]` |
| A UIKit / AppKit view in a Kotlin screen | `UIKitView`, `UIViewControllerView`, `AppKitView`, `NSViewControllerView` |
| Compose Multiplatform content | `UIViewControllerView { ComposeUIViewController { … } }` (iOS) |

## Kotlin screens in a Swift app

A screen is a Kotlin function that returns a `ComposeSwiftUIHost`, built with `composeSwiftUI { }`:

```kotlin
fun profileScreen(userId: String) = composeSwiftUI {
    ProfileScreen(userId)
}

@Composable
fun ProfileScreen(userId: String) {
    Text("Profile of $userId", modifier = Modifier.padding())
}
```

`ComposeView` is a regular SwiftUI view, so it goes anywhere a view goes: a `TabView`, a `NavigationStack`
destination, a `sheet`, next to Swift views in a `VStack`.

```swift
struct ContentView: View {
    @State private var showProfile = false

    var body: some View {
        TabView {
            HomeView()                                        // plain SwiftUI
                .tabItem { Label("Home", systemImage: "house") }
            ComposeView { ProfileScreenKt.profileScreen(userId: "42") }  // Kotlin
                .tabItem { Label("Profile", systemImage: "person") }
        }
    }
}
```

In a UIKit app, wrap it in a hosting controller (`NSHostingController` / `NSHostingView` on macOS):

```swift
let controller = UIHostingController(rootView: ComposeView { ProfileScreenKt.profileScreen(userId: user.id) })
navigationController?.pushViewController(controller, animated: true)
```

Lifecycle: the closure passed to `ComposeView` runs once per view identity. The host lives as long as the SwiftUI view
(it is held in a `@StateObject`): its composition, its `remember` state and the view models created with
`viewModel { }` are disposed when the view leaves the hierarchy. Two consequences:

- Changing the Swift value captured by the closure (`userId` above) does **not** recreate the screen. Either pass it
  with `input:` (next section), or give the view a new identity with `.id(userId)`.
- Each `ComposeView` is an independent composition. Share state between two of them through a Kotlin object both
  receive (a view model, a repository, a `StateFlow`), not through `remember`.

## Passing values from Swift

`ComposeView(input:)` pushes a value into the composition, and pushes it again each time it changes (it must be
`Hashable`). Kotlin reads the latest value with `hostInput<T>()`:

```kotlin
data class ProfileInput(val userId: String, val compact: Boolean)

fun profileCard() = composeSwiftUI {
    val input = hostInput<ProfileInput?>() ?: return@composeSwiftUI
    Text(input.userId, modifier = Modifier.font(if (input.compact) Font.body else Font.title))
}
```

```swift
struct ProfileCardView: View {
    let userId: String
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        // A Kotlin data class is Hashable from Swift (equals / hashCode)
        ComposeView(input: ProfileInput(userId: userId, compact: sizeClass == .compact)) {
            ProfileCardKt.profileCard()
        }
    }
}
```

The first composition already sees the initial input. Pass Kotlin types (data classes, `String`), or wrap Swift
scalars in their Kotlin boxes (`KotlinBoolean(value:)`, `KotlinInt(value:)`) so they arrive as Kotlin `Boolean` and
`Int`. `hostInput` reads a `CompositionLocal`, so only the composables that read it recompose when it changes.

### Calling Swift from Kotlin

To call Swift code (an environment action such as `openWindow`, an SDK only available in Swift), declare a Kotlin
interface, implement it in Swift, and hand the implementation to Kotlin, as an input or through your dependency
container:

```kotlin
interface AppActions {
    fun openWindow(id: String)
    fun rateApp()
}

val LocalAppActions = staticCompositionLocalOf<AppActions?> { null }

fun mainScreen() = composeSwiftUI {
    CompositionLocalProvider(LocalAppActions provides hostInput<AppActions?>()) {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    val actions = LocalAppActions.current
    Button("Open the inspector window") { actions?.openWindow("inspector") }
}
```

```swift
import StoreKit

// Kotlin interfaces are Objective-C protocols: implement them on an NSObject subclass.
final class SwiftAppActions: NSObject, AppActions {
    var open: (String) -> Void = { _ in }
    var requestReview: () -> Void = {}
    func openWindow(id: String) { open(id) }
    func rateApp() { requestReview() }
}

struct RootView: View {
    @Environment(\.openWindow) private var openWindow
    @Environment(\.requestReview) private var requestReview
    @State private var actions = SwiftAppActions()

    var body: some View {
        ComposeView(input: actions) { MainScreenKt.mainScreen() }
            .onAppear {
                actions.open = { openWindow(id: $0) }
                actions.requestReview = { requestReview() }
            }
    }
}
```

### Observing Kotlin state from Swift

Swift code next to a Kotlin screen (a toolbar, a menu command, a Swift view) can observe a Kotlin `StateFlow` with
`watchFlow`, which calls back on the main thread and returns a `FlowWatcher`:

```swift
@MainActor final class CartBadge: ObservableObject {
    @Published var count = 0
    private var watcher: FlowWatcher?

    init(cart: CartViewModel) {
        watcher = FlowWatchKt.watchFlow(flow: cart.itemCount) { [weak self] value in
            self?.count = (value as? KotlinInt)?.intValue ?? 0
        }
    }

    deinit { watcher?.cancel() }
}
```

## Custom SwiftUI views with NativeView

Any SwiftUI view, including ones from your Swift code or a Swift package, can be shown from Kotlin. Register a factory
under a key in Swift, once at launch, and call `NativeView(key, modifier, args…)` in Kotlin:

```swift
@main
struct MyApp: App {
    init() {
        ComposeRegistry.nativeViews["confetti"] = { args in
            // args[0] is the key; the Kotlin arguments start at index 1
            AnyView(ConfettiView(count: args.int(1), colors: args.list(2).compactMap { $0 as? String }))
        }
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

```kotlin
@Composable
fun Celebration(won: Boolean) {
    ZStack {
        Text("You won!")
        if (won) {
            NativeView("confetti", Modifier.allowsHitTesting(false), 120, listOf("red", "blue", "yellow"))
        }
    }
}
```

The Swift factory receives an `Args` with typed accessors: `string(i)`, `int(i)`, `double(i)`, `bool(i)`, `list(i)`,
`date(i)` (from epoch seconds), their `…Opt` variants for nullable values, and `raw(i)` for any Kotlin object. The
factory runs again when the Kotlin arguments change, and the Kotlin modifiers (frame, padding, `allowsHitTesting`…)
are applied to the returned view. A missing key renders a red "No native view" text, which makes typos easy to spot.

Arguments travel as Objective-C objects: strings, numbers, booleans, lists and Kotlin objects (data classes,
interfaces) all work; pass a Kotlin object and read its properties in Swift for anything structured.

### Callbacks from a native view

Wrap Kotlin lambdas with `nativeAction { }` (no argument) or `nativeCallback { value -> }` (one value) and pass them
in the `args`. Swift reads them with `args.action(i)` and `args.callback(i)`, which call Kotlin and then flush
Compose, so the resulting recomposition lands in the same SwiftUI transaction, like the built-in controls. The
wrappers are updated in place on recomposition: no `remember` or `rememberUpdatedState` needed.

```kotlin
@Composable
fun StarPicker(stars: Int, onStarsChange: (Int) -> Unit, onReset: () -> Unit) {
    NativeView(
        "starPicker", Modifier, stars,
        nativeCallback { value -> onStarsChange((value as Number).toInt()) },
        nativeAction(onReset),
    )
}
```

```swift
ComposeRegistry.nativeViews["starPicker"] = { args in
    AnyView(StarPickerView(
        stars: args.int(1),
        onChange: args.callback(2) as (Int) -> Void,
        onReset: args.action(3)
    ))
}
```

The callback value arrives as its Kotlin representation: a Swift `String` as `String`, numbers and booleans boxed
(read them as `Number` / `Boolean`), arrays as lists, and `NSNull` as `null`.

When the Swift side needs an answer that takes time (a SwiftUI `async` closure, a network call made in Kotlin), pass a
`suspend` function with `nativeSuspendFunction { args -> … }`. Swift awaits its result with `kotlinAwait`; cancelling
the Swift task cancels the coroutine, and a failure or cancellation returns `nil`:

```kotlin
@Composable
fun PriceTag(productId: String, loadPrice: suspend (String) -> String) {
    NativeView("priceTag", Modifier, productId, nativeSuspendFunction { args -> loadPrice(args[0] as String) })
}
```

```swift
ComposeRegistry.nativeViews["priceTag"] = { args in
    let load = UncheckedSendable(args.raw(2))
    let id = args.string(1)
    return AnyView(Text("…").task { _ = await kotlinAwait(load.value, [id]) })
}
```

Keep the value in Kotlin (state hoisting): the Swift view displays `stars` and reports changes, Kotlin decides. A
`NativeView` keeps its SwiftUI identity while its key and position stay the same, so `@State` inside the Swift view
survives recompositions.

## UIKit and AppKit views

`UIKitView` and `UIViewControllerView` (in `iosMain`) and `AppKitView` and `NSViewControllerView` (in `macosMain`)
embed platform views directly from Kotlin, without Swift code. `factory` runs once; `update` runs after it and on every
recomposition, so the view follows Kotlin state:

```kotlin skip
// iosMain
@Composable
fun MultilineLabel(paragraph: String) {
    UIKitView(
        sizing = NativeViewSizing.FitHeight,
        update = { (it as platform.UIKit.UILabel).text = paragraph },
    ) { platform.UIKit.UILabel().apply { numberOfLines = 0 } }
}
```

```kotlin
// macosMain
@Composable
fun LevelIndicator(level: Double) {
    AppKitView(
        sizing = NativeViewSizing.FitContent,
        update = { (it as platform.AppKit.NSLevelIndicator).doubleValue = level },
    ) {
        platform.AppKit.NSLevelIndicator().apply { maxValue = 10.0 }
    }
}
```

`NativeViewSizing` decides how SwiftUI sizes the embedded view:

| Sizing | Behavior |
|---|---|
| `Fill` (default) | takes the size SwiftUI proposes; give it a `Modifier.frame` |
| `FitHeight` | takes the proposed width and the height its content needs at that width (multi-line labels) |
| `FitContent` | takes its intrinsic or Auto Layout fitting size |

The `factory` and `update` lambdas are Kotlin code calling UIKit/AppKit through Kotlin/Native interop, so anything the
Apple SDK exposes works: `MKMapView`, `PKCanvasView`, `WKWebView`, a third-party Objective-C view. Delegates are
Kotlin objects implementing the Objective-C protocol (`NSObject(), UITextViewDelegateProtocol`); keep them in
`remember`, since UIKit holds delegates weakly. For UIKit views that only exist in Swift (a Swift package), use
`NativeView` with a `UIViewRepresentable` written in Swift.

`Image(uiImage)` (iOS) and `Image(nsImage)` (macOS) show an image you decoded or downloaded in Kotlin, with the usual
image modifiers (`resizable()`, `aspectRatio`…).

## Compose Multiplatform content (iOS)

A Compose Multiplatform screen is a UIKit view controller (`ComposeUIViewController`), so it embeds like any other.
This is the way to reuse an existing Compose UI component (a chart, a canvas-heavy widget) inside a Compose SwiftUI
screen:

```kotlin skip
// iosMain, with org.jetbrains.compose.foundation (or material3) as an iosMain dependency
UIViewControllerView(modifier = Modifier.frame(height = 260.0)) {
    ComposeUIViewController { SalesChart() }
}
```

- The embedded content takes the size SwiftUI proposes, so give it a `frame`.
- It has its own composition, separate from the Compose SwiftUI one: share state through a `ViewModel` or a
  `StateFlow` both read, not through `remember`.
- The app's `Info.plist` needs `CADisableMinimumFrameDurationOnPhone` set to `true`, as for any Compose Multiplatform
  app.
- Compose UI composables cannot be called directly inside a Compose SwiftUI tree: the two use different appliers
  (UI nodes versus SwiftUI nodes). The embedding above is the boundary.

### Compose Multiplatform resources

`stringResource(Res.string.title)` works in Compose SwiftUI screens once Compose UI's `LocalDensity` is provided,
because Compose Multiplatform resources read it:

```kotlin skip
fun app() = composeSwiftUI {
    CompositionLocalProvider(LocalDensity provides Density(UIScreen.mainScreen.scale.toFloat())) {
        Text(stringResource(Res.string.title))
    }
}
```

The suspending `getString(Res.string.title)` works without it, for example in a `ViewModel`. The preview renderer
copies the module's Compose resources into its host app, so previews show the real strings (see
[Previews](previews.md)).

## Navigation 3 and other Compose libraries

Libraries built only on the Compose **runtime** work unchanged: they don't know what the composition renders.
Libraries built on Compose **UI** (foundation, material, most UI kits) draw with Compose UI and can only be embedded
as above.

Navigation 3 splits along that line: its runtime (`NavKey`, `entryProvider`, back stacks) works, its `NavDisplay`
draws with Compose UI. Compose SwiftUI's own `NavDisplay` shows any observable back stack with SwiftUI navigation, so
you keep the Navigation 3 model and get the native push animation and swipe back:

```kotlin skip
// iosMain or commonMain, with androidx.navigation3:navigation3-runtime
val backStack = remember { mutableStateListOf<Route>(Route.Home) }
val entries = entryProvider<Route> {
    entry<Route.Home> { HomeScreen(onOpen = { backStack.add(it) }) }
    entry<Route.Detail> { key -> DetailScreen(key.id) }
}
NavDisplay(backStack) { route -> entries(route).Content() }
```

Push with `backStack.add(…)`; the SwiftUI back button and swipe remove keys. `NavDisplay` takes any `MutableList`
backed by snapshot state, so it works without Navigation 3 too:

```kotlin
sealed interface Screen {
    data object Inbox : Screen
    data class Thread(val id: String) : Screen
}

@Composable
fun MailNavigation() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Inbox) }
    NavDisplay(backStack) { screen ->
        when (screen) {
            Screen.Inbox -> Button("Open thread") { backStack.add(Screen.Thread("t1")) }
            is Screen.Thread -> Text("Thread ${screen.id}", modifier = Modifier.navigationTitle("Thread"))
        }
    }
}
```

Other runtime-level pieces that work: `androidx.lifecycle` `ViewModel` (with Compose SwiftUI's `viewModel { }`),
`collectAsState`, `produceState`, `LaunchedEffect` and the other effects, `CompositionLocal`s, and state holders
from libraries that only depend on `compose-runtime`.

## Sharing view models with Android

The usual setup keeps the logic shared and writes the UI natively on each side: Jetpack Compose on Android, Compose
SwiftUI on Apple platforms, the same `ViewModel` classes underneath.

The simplest layout applies the plugin to the shared module itself. The library is only published for Apple targets,
so when the module also targets Android or the JVM the plugin adds it to `appleMain` instead of `commonMain`: view
models stay in `commonMain`, Compose SwiftUI screens go in `appleMain` (or `iosMain`):

```
shared/         android + iOS targets, Compose SwiftUI plugin
  commonMain/   ViewModels, repositories, models
  appleMain/    Compose SwiftUI screens
androidApp/     Jetpack Compose screens, depends on :shared
iosApp/         Xcode project: links the Shared framework
```

To keep the SwiftUI screens out of the shared module, put them in a separate Apple-only module instead (`iosUi`
below, depending on `:shared`; its `commonMain` gets the library). Export `:shared` from that module's framework if
Swift code needs its classes too:

```kts
// iosUi/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("dev.nucleusframework.compose-swiftui") version "0.9.0"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            export(project(":shared"))
        }
    }
    sourceSets {
        commonMain.dependencies { api(project(":shared")) }
    }
}
```

A `ViewModel` from `:shared` (`androidx.lifecycle.ViewModel`, which is multiplatform) is used the same way on both
sides:

```kotlin
class CounterViewModel : androidx.lifecycle.ViewModel() {
    private val _count = kotlinx.coroutines.flow.MutableStateFlow(0)
    val count: kotlinx.coroutines.flow.StateFlow<Int> = _count
    fun increment() { _count.value++ }
}

// iosUi: Compose SwiftUI
@Composable
fun CounterScreen() {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    VStack(spacing = 12.0) {
        Text("Count: $count", modifier = Modifier.font(Font.title))
        Button("Increment", action = vm::increment)
    }
}
```

On Android the screen would call `androidx.lifecycle.viewmodel.compose.viewModel { CounterViewModel() }` and Jetpack
Compose's `Text` and `Button`. Compose SwiftUI's `viewModel { }` has the same contract: it returns the existing
instance for the class (and optional `key`) or creates it. View models are scoped to the `ComposeView` that hosts
the composition and cleared (`onCleared`, `viewModelScope` cancelled) when it is removed. To share one instance
between several `ComposeView`s, create it outside the composition and pass it in (for example as a parameter of the
screen function, or through `input:`).
