<!-- group: Platforms and tools -->
# Running in production

> How the runtime handles Kotlin exceptions, where it logs, how to read mixed Kotlin/Swift stack traces, and how the
> lifecycle, view models and saved state of a `ComposeView` behave.

## Errors

A Kotlin exception that reaches Swift terminates the process: Kotlin/Native can't unwind through Objective-C frames.
The runtime catches every exception at the points where Swift calls Kotlin, reports it, and keeps the app running:

| Where it is thrown | `ComposeSwiftUIErrorSource` | What happens next |
|---|---|---|
| A callback Swift invokes: button action, binding setter, gesture, drop, key press, `Canvas` drawing, `CustomLayout`… | `Callback` | The callback does nothing (a neutral value where Swift needs one). The UI keeps running. |
| Composition, recomposition, or an effect of the composition (`LaunchedEffect`, `DisposableEffect`) | `Composition` | That host stops composing; its `ComposeView` shows nothing (a red message in debug builds). Other hosts keep running. |
| A `suspend` action Swift awaits (`task { }`, `refreshable { }`, async closures) | `AsyncTask` | The action ends; SwiftUI's `await` returns. The UI keeps running. |

Plug your crash reporter in once, at launch:

```kotlin
fun installCrashReporting() {
    ComposeSwiftUI.onError = { source, error ->
        // Crashlytics.recordException(error), Sentry.captureException(error)…
        println("Compose SwiftUI error in $source: $error")
    }
}
```

`onError` runs on the main thread, after the runtime logged the error. Kotlin code that catches its own exceptions can
report them the same way with `ComposeSwiftUI.reportError(source, error)`.

A screen whose composition failed doesn't recover by itself: recreate its `ComposeView` (give it a new `.id(…)`) to
start a fresh composition.

### Release builds

In builds without the `DEBUG` Swift flag (Xcode's Release configuration), the renderer never shows its diagnostics to
users: a view kind the renderer doesn't know ("Unsupported view kind"), a `NativeView` key that isn't registered and a
failed composition render nothing, and are logged once.

## Logging

The renderer writes the runtime's log lines to the unified log (`os_log`), subsystem `dev.nucleusframework.composeswiftui`,
category `runtime`. In Console.app, filter with `subsystem:dev.nucleusframework.composeswiftui`; on the command line:

```bash
log stream --predicate 'subsystem == "dev.nucleusframework.composeswiftui"'
```

To send them elsewhere, set your own logger after `ComposeGenerated.setUp()`:

```kotlin
fun installLogger() {
    ComposeSwiftUI.logger = ComposeSwiftUILogger { level, message -> println("[$level] $message") }
}
```

## Symbolicating mixed Kotlin/Swift stack traces

A crash report of a Compose SwiftUI app mixes Swift frames (SwiftUI, the renderer) and Kotlin frames (your code, the
runtime). Kotlin/Native frames are native code like any other: they symbolicate from the same dSYM, and their names
read like `kfun:com.example.HomeScreen#internal` (`kfun:` + package, class and function).

1. **Keep the debug information.** With a static framework (`isStatic = true`, the default of the templates), the
   Kotlin code is linked into the app binary, and Xcode's `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym` puts it in the
   app's dSYM. With a dynamic framework, Kotlin writes `Shared.framework.dSYM` next to the framework
   (`build/bin/iosArm64/releaseFramework/`): upload it with the app's.
2. **Upload the dSYMs** to your crash reporter (Crashlytics `upload-symbols`, `sentry-cli debug-files upload`), for the
   app and, with a dynamic framework, for `Shared.framework.dSYM`.
3. **Read a raw address** with `atos`, using the load address of the binary from the crash report:

   ```bash
   atos -arch arm64 -o MyApp.app.dSYM/Contents/Resources/DWARF/MyApp -l 0x100000000 0x1000a1b2c
   ```

4. **File and line numbers in Kotlin traces.** `Throwable.stackTraceToString()` (what `onError` and the logs show)
   lists addresses and function names. To get file and line numbers in it as well, add to the framework:

   ```kts
   binaries.framework {
       binaryOption("sourceInfoType", "libbacktrace")
   }
   ```

   It costs binary size; most apps keep it for debug builds only.

## Lifecycle

Each `ComposeView` is the `LifecycleOwner` of its composition (`androidx.lifecycle.compose.LocalLifecycleOwner`):

| SwiftUI | Lifecycle state |
|---|---|
| The composition started, view not on screen | `CREATED` |
| On screen (`onAppear`), scene `inactive` | `STARTED` |
| On screen, scene `active` | `RESUMED` |
| `onDisappear`, or scene in the `background` | `CREATED` |
| The `ComposeView` is gone | `DESTROYED` |

So the lifecycle-aware APIs of AndroidX work as on Android:

```kotlin
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun Clock(ticks: StateFlow<Int>) {
    // Collects only while the screen is visible in an active scene
    val tick by ticks.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        println("resumed")
        onPauseOrDispose { println("paused") }
    }
    Text("tick $tick")
}
```

The `suspend` callbacks of a host (`task { }`, `refreshable { }`) run in the host's scope: they are cancelled when the
`ComposeView` goes away, even if SwiftUI never cancelled them.

## View models and dependency injection

The host is also the `ViewModelStoreOwner` (the official `androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner`)
and the `SavedStateRegistryOwner`, with non-empty `CreationExtras`: the official `viewModel { }`, `SavedStateHandle`
and DI libraries (`koinViewModel()`) work unchanged.

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel

class NoteViewModel(private val handle: SavedStateHandle) : ViewModel() {
    var draft: String
        get() = handle["draft"] ?: ""
        set(value) { handle["draft"] = value }
}

@Composable
fun NoteScreen() {
    val vm = viewModel { NoteViewModel(createSavedStateHandle()) }
    Text(vm.draft)
}
```

## Saved state and process death

Give a `ComposeView` a `restorationID` and its `rememberSaveable` values and `SavedStateHandle`s are kept in
`@SceneStorage`: they survive the process, as on Android.

```swift
ComposeView(restorationID: "home") { HomeKt.homeScreen() }
```

The state is saved when the scene leaves the foreground and when the view disappears. A saved value must be a
primitive, a `String`, a list or map of those, a primitive array or a `MutableState` holding one (what Android can put
in a `Bundle` without `Parcelable`). For other types, give `rememberSaveable` a `Saver`.

Without SwiftUI's scene storage (a test, your own persistence), save and restore by hand:
`host.saveState()` returns a string, `host.restoreState(state)` before the host starts restores it.

## Compose Multiplatform resources

When the module depends on Compose UI (Compose Multiplatform resources, foundation…), the Gradle plugin makes every host
provide Compose UI's `LocalDensity` from the SwiftUI `displayScale`, so `stringResource` works with nothing to set up.
Other composition locals your libraries expect can be provided to every host the same way, with
`ComposeSwiftUI.addHostDecorator`.

## Testing

### Kotlin tests

`runComposeSwiftUITest` composes a screen in a Kotlin/Native test, without SwiftUI, and acts on it as SwiftUI would
(the button's action, the binding's setter):

```kotlin
import androidx.compose.runtime.mutableIntStateOf
import dev.nucleusframework.composeswiftui.testing.runComposeSwiftUITest

@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    VStack {
        Button("Increment", action = { count++ })
        TextField("Name", text = "", onTextChange = { }, modifier = Modifier.accessibilityIdentifier("name"))
        Text("count=$count")
    }
}

@OptIn(ExperimentalComposeSwiftUIApi::class)
fun counterIncrements() = runComposeSwiftUITest {
    setContent { Counter() }
    onNodeWithText("Increment").performClick()
    onNodeWithText("count=1").assertExists()
    onNodeWithAccessibilityIdentifier("name").performTextInput("Ada")
}
```

Effects and coroutines run while the test waits (`waitForIdle()`, `waitUntil { … }`); `printTree()` shows the nodes
when a finder fails.

### XCUITest

The views are real SwiftUI views, so XCUITest finds them as in any app. Give the controls you drive an
`accessibilityIdentifier` in Kotlin and query it in Swift:

```swift
let app = XCUIApplication()
app.launch()
app.buttons["increment"].tap()
XCTAssertTrue(app.staticTexts["count=1"].exists)
```

`e2e/app` in this repository drives a whole app the same way, in process, through the accessibility API
(`AXUIElement`).

### Snapshot tests

Render a `ComposeView` with SwiftUI's `ImageRenderer` and compare it with a reference image; the composition runs in
the first render pass, so the image is complete:

```swift
let renderer = ImageRenderer(content: ComposeView { CounterKt.counterScreen() }.frame(width: 320, height: 240))
renderer.scale = 2
let image = renderer.cgImage   // compare with the reference PNG, with a small per-pixel tolerance
```

`e2e/RendererTests` is a complete example (XCTest, recorded references, tolerance, and a check that disposed screens
are released). Snapshot libraries such as swift-snapshot-testing work the same way on the `ComposeView`.
