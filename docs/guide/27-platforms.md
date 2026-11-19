<!-- group: Platforms and tools -->
# Platforms

> One Kotlin UI for iOS, iPadOS, macOS, tvOS and watchOS: source sets, API availability, per-platform specifics and how to set up each target in Gradle and Xcode.

Compose SwiftUI targets every Apple platform Kotlin/Native supports: iOS 17+ (and iPadOS), macOS 14+, tvOS 17+ and
watchOS 10+. visionOS is not supported, because Kotlin/Native has no visionOS target. The same composable renders as
the native SwiftUI view of each platform: a `List` is a table on macOS, a focusable list on tvOS and a carousel-style
list on watchOS, exactly as when the same SwiftUI code is compiled for each platform.

## Source sets

The library publishes these Kotlin/Native targets: `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`,
`macosX64`, `tvosArm64`, `tvosSimulatorArm64`, `watchosArm64`, `watchosDeviceArm64` and `watchosSimulatorArm64`.
Almost the whole API lives in `commonMain`, so most screens are written once:

| Source set | What goes there |
|---|---|
| `commonMain` | Screens, view models, everything that uses only the generated SwiftUI API |
| `appleMain` | Code that uses Apple frameworks through Kotlin/Native interop (`platform.Foundation`, `platform.CoreLocation`…) on every Apple platform |
| `iosMain` | UIKit embedding (`UIKitView`, `UIViewControllerView`), `Image(uiImage)`, Compose Multiplatform content |
| `macosMain` | AppKit embedding (`AppKitView`, `NSViewControllerView`), `Image(nsImage)` |
| `tvosMain`, `watchosMain` | Platform interop specific to those systems |

With the default hierarchy template, Kotlin creates `appleMain`, `iosMain`, `macosMain`, `tvosMain` and `watchosMain`
for you as soon as the targets are declared.

`commonMain` here means "common to your Apple targets": the library only has Apple targets, and in a module whose
targets are all Apple the Gradle plugin adds it to `commonMain`. In a module that also targets Android or the JVM, the
plugin adds it to `appleMain` instead: the screens go in `appleMain` and the table's `commonMain` row becomes
`appleMain` (view models can stay in `commonMain`). See [Interop](interop.md) for both layouts.

## Availability: compile everywhere, no-op where missing

Every generated API compiles on all four platforms. When SwiftUI does not offer an API on a platform, or only on a
newer OS version, the renderer skips it at runtime:

- A modifier that does not exist on the current platform or OS version is ignored: the view renders without it.
  `Modifier.keyboardType(…)` on macOS, `Modifier.onHover { }` on tvOS, or `Modifier.glassEffect(shape = Capsule())` on iOS 17 change
  nothing.
- A view that does not exist renders nothing (`ColorPicker` on tvOS, `MeshGradient` before iOS 18).
- Callbacks of a skipped API are never called.

So you do not need `if (platform == …)` around most code. The KDoc of each generated function states its availability
(for example "Availability: iOS 17.0+, macOS 14.0+, tvOS: unavailable (no-op), watchOS: unavailable (no-op)"), and
the [reference](/reference.html) shows it for every API.

When behavior must differ, branch on the platform with an `expect`/`actual` value:

```kotlin skip
// commonMain
enum class Platform { Phone, Pad, Mac, TV, Watch }
expect val currentPlatform: Platform

// macosMain
actual val currentPlatform = Platform.Mac

// iosMain: iPhone or iPad
actual val currentPlatform =
    if (platform.UIKit.UIDevice.currentDevice.userInterfaceIdiom == platform.UIKit.UIUserInterfaceIdiomPad) Platform.Pad
    else Platform.Phone
```

For layout decisions on iPad and Mac, prefer the size class from `EnvironmentReader` over the device type: an iPad
app in Split View is compact.

## iOS and iPadOS

iOS is the reference platform: everything in this guide works there unless stated. iPad specifics:

- **Size classes.** `EnvironmentReader { env -> env.horizontalSizeClass }` tells you whether the window is compact or
  regular. `NavigationSplitView` adapts by itself: columns in regular width, a stack in compact width.
- **Pointer and keyboard.** `onHover`, `hoverEffect(HoverEffect.lift)`, `keyboardShortcut` and `onKeyPress` work with
  a trackpad or a hardware keyboard (see [Gestures and input](gestures-and-input.md)).
- **Apple Pencil.** `onPencilDoubleTap`, `onPencilSqueeze`; PencilKit through `UIKitView`.
- **Multiple windows.** Scenes are declared in Swift (below); each window gets its own `ComposeView` and its own
  composition.

```kotlin
@Composable
fun AdaptiveRoot(content: @Composable () -> Unit, sidebar: @Composable () -> Unit) {
    EnvironmentReader { env ->
        if (env.horizontalSizeClass == UserInterfaceSizeClass.regular) {
            NavigationSplitView(sidebar = sidebar, detail = content)
        } else {
            NavigationStack { content() }
        }
    }
}
```

## macOS

Kotlin writes the views; the app, its scenes, windows, menus and settings are declared in the Swift `App`, like any
SwiftUI app. SwiftUI's scene APIs (`WindowGroup`, `Window`, `Settings`, `MenuBarExtra`, `commands`) are Swift
protocols adopted by your app type, so they have no Kotlin form; each scene hosts a `ComposeView`.

```swift
import SwiftUI

@main
struct NotesApp: App {
    // A shared Kotlin view model, used by the window and by the menu commands
    private let store = NotesStore()

    var body: some Scene {
        WindowGroup {
            ComposeView { NotesKt.notesWindow(store: store) }
                .frame(minWidth: 700, minHeight: 400)
        }
        .defaultSize(width: 1000, height: 650)
        .commands {
            CommandMenu("Note") {
                Button("New Note") { store.createNote() }
                    .keyboardShortcut("n")
                Button("Delete Note") { store.deleteSelected() }
                    .keyboardShortcut(.delete)
            }
        }

        Settings {
            ComposeView { NotesKt.settingsScreen(store: store) }
        }

        MenuBarExtra("Notes", systemImage: "note.text") {
            ComposeView { NotesKt.menuBarPanel(store: store) }
        }
        .menuBarExtraStyle(.window)
    }
}
```

```kotlin
class NotesStore {
    private val _notes = kotlinx.coroutines.flow.MutableStateFlow(listOf("First note"))
    val notes: kotlinx.coroutines.flow.StateFlow<List<String>> = _notes
    fun createNote() { _notes.value += "Note ${_notes.value.size + 1}" }
    fun deleteSelected() { _notes.value = _notes.value.dropLast(1) }
}

fun notesWindow(store: NotesStore) = composeSwiftUI {
    val notes by store.notes.collectAsState()
    var showInspector by remember { mutableStateOf(false) }
    NavigationSplitView(
        sidebar = {
            List {
                for (note in notes) key(note) {
                    Text(note, modifier = Modifier.contextMenu { Button("Delete") { store.deleteSelected() } })
                }
            }
        },
        detail = {
            Text("Select a note")
        },
        modifier = Modifier
            .inspector(isPresented = showInspector, onIsPresentedChange = { showInspector = it }) {
                Text("Details")
            }
            .toolbar {
                Button(action = { showInspector = !showInspector }) {
                    Label("Inspector", systemImage = SFSymbol.sidebarRight)
                }
            },
    )
}
```

A plain Kotlin class such as `NotesStore` is visible from Swift through the framework, so menu commands can call it
directly; the window sees the change through its `StateFlow`. For Swift code that must observe a Kotlin `StateFlow`
(to enable or disable a menu item, say), use `watchFlow(flow) { value in … }`, which returns a `FlowWatcher` to
`cancel()`.

macOS-specific views and modifiers available from Kotlin include:

| Feature | API |
|---|---|
| Tables with columns, sorting and selection | `Table(rows, key, selection, onSelectionChange) { column("Name") { … } }` (see [Lists](lists.md)) |
| Inspector panel | `Modifier.inspector(isPresented, onIsPresentedChange) { }`, `inspectorColumnWidth` |
| Tooltips | `Modifier.help("…")` |
| Context menus | `Modifier.contextMenu { Button(…) }` |
| Pointer | `Modifier.onHover`, `onContinuousHover`, `pointerStyle(PointerStyle.link)` (macOS 15) |
| Menu commands on the focused view | `onCopyCommand`, `onCutCommand`, `onPasteCommand`, `onDeleteCommand`, `onMoveCommand`, `onExitCommand` |
| Keyboard | `onKeyPress`, `keyboardShortcut`, `onModifierKeysChanged` (macOS 15), `focusSection`, `focusable` |
| Window behavior | `windowResizeBehavior`, `windowMinimizeBehavior`, `windowFullScreenBehavior`, `windowDismissBehavior` |
| Settings link | `SettingsLink { Text("Settings…") }` |
| Services, file panels | `exportableToServices`, `importableFromServices`, `fileImporter`, `fileExporter`, `fileMover` |
| AppKit views | `AppKitView`, `NSViewControllerView`, `Image(nsImage)` in `macosMain` |

```kotlin
// macosMain: an AppKit control inside a Compose SwiftUI screen
@Composable
fun SearchFieldRow(query: String) {
    AppKitView(
        sizing = NativeViewSizing.FitContent,
        update = { (it as platform.AppKit.NSSearchField).stringValue = query },
    ) { platform.AppKit.NSSearchField() }
}
```

Environment actions that SwiftUI only exposes through `@Environment` in a view (`openWindow`, `openSettings`,
`dismissWindow`) are not readable from Kotlin. Call them from Swift: a command, a `NativeView` button, or a Kotlin
interface implemented in Swift and passed with `ComposeView(input:)` (see [Interop](interop.md)).

## tvOS

tvOS is driven by the focus engine and the Siri Remote; there is no touch on the screen.

- Interactive elements must be focusable. `Button`, `NavigationLink`, `Toggle`, list rows and `TextField` are; for a
  custom view add `Modifier.focusable()`. Tap gestures and drags are not available: use buttons.
- `Modifier.buttonStyle(PrimitiveButtonStyle.card)` gives the lifted card look of tvOS. `hoverEffect` controls the
  focus effect, `focusEffectDisabled()` removes it.
- `Modifier.focusSection()` groups views so focus moves into the group as a whole (rows of a shelf).
  `focusScope` + `prefersDefaultFocus` choose the initial focus in a region; `focused(…)` moves focus from Kotlin.
- Remote buttons: `onMoveCommand { direction -> }`, `onPlayPauseCommand { }`, `onExitCommand { }` (Menu / Back).
  `onLongPressGesture` has a tvOS variant for long presses on the focused view.

```kotlin
@Composable
fun Shelf(title: String, items: List<String>, onPlay: (String) -> Unit) {
    VStack(alignment = HorizontalAlignment.leading) {
        Text(title, modifier = Modifier.font(Font.title2))
        ScrollView(axes = Axis.Set.horizontal) {
            LazyHStack(spacing = 40.0) {
                for (item in items) key(item) {
                    Button(action = { onPlay(item) }, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.card)) {
                        Text(item, modifier = Modifier.frame(width = 300.0, height = 170.0))
                    }
                }
            }
        }
    }
}
```

Text input on tvOS uses the system keyboard screen; `keyboardType` and `textContentType` apply there too.

## watchOS

- **Navigation.** `NavigationStack` and `TabView` work; `Modifier.tabViewStyle(TabViewStyle.verticalPage)` gives the
  vertically paged layout of watchOS 10. `Modifier.containerBackground(style, ContainerBackgroundPlacement.navigation)`
  tints the navigation background.
- **Digital Crown.** `digitalCrownRotation` binds the crown to a Kotlin `Double` or `Int` (see
  [Gestures and input](gestures-and-input.md)); give the view `focusable()`.
- **Double tap.** `Modifier.handGestureShortcut(HandGestureShortcut.primaryAction)` on a button (watchOS 11).
- **Lists** are the main layout; keep rows short and use `LazyForEach` for long lists.
- No `onKeyPress`, hover or drag and drop. `onTapGesture`, long press and drag gestures work.
- `NowPlayingView` comes from the WatchKit integration (`ComposeIntegrations.registerWatchKit()`, see
  [System services](system-services.md)).

```kotlin skip
// watchosMain or commonMain
@Composable
fun WorkoutPages(heartRate: Int, onPause: () -> Unit) {
    TabView(modifier = Modifier.tabViewStyle(TabViewStyle.verticalPage)) {
        Text("$heartRate BPM", modifier = Modifier.font(Font.largeTitle))
        Button("Pause", action = onPause, modifier = Modifier.handGestureShortcut(HandGestureShortcut.primaryAction))
    }
}
```

## Framework integrations per platform

The opt-in framework renderers (MapKit, Charts, StoreKit…) are compiled only where their framework exists. Each
`register…()` function is declared under the same `#if os(…)` as its file, so call it under that condition:

```swift
init() {
    ComposeIntegrations.registerMapKit()        // every platform
    ComposeIntegrations.registerTipKit()        // every platform
    #if os(iOS) || os(macOS)
    ComposeIntegrations.registerCharts()
    ComposeIntegrations.registerStoreKit()
    ComposeIntegrations.registerPhotosUI()
    #endif
    #if os(watchOS)
    ComposeIntegrations.registerWatchKit()
    #endif
}
```

The Kotlin side of an integration compiles everywhere; without its renderer the view renders nothing. The table in
[Apple frameworks](apple-frameworks.md) lists each file's platforms.

## Setting up the targets

### Gradle

Declare the targets you ship, with one framework per target. All frameworks must share the same `baseName`, because
the Swift renderer imports a single module name:

```kts
plugins {
    kotlin("multiplatform") version "2.4.20"
    kotlin("plugin.compose") version "2.4.20"
    id("dev.nucleusframework.compose-swiftui") version "0.9.0"
}

kotlin {
    listOf(
        iosArm64(), iosSimulatorArm64(),
        macosArm64(),
        tvosArm64(), tvosSimulatorArm64(),
        watchosArm64(), watchosDeviceArm64(), watchosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
```

- `iosX64()` and `macosX64()` are only needed for Intel Macs (the simulator on an Intel Mac, or an Intel build of
  the Mac app).
- watchOS devices need both `watchosArm64` (arm64_32 watches) and `watchosDeviceArm64` (arm64, the newest
  watches).
- If the framework name differs from what the renderer should import, set
  `composeSwiftUI { frameworkName = "Shared" }`.

### Xcode

Each platform is its own app target (or one multiplatform target with several destinations). For every target:

1. Add a **Run Script** build phase before *Compile Sources*, the standard Kotlin Multiplatform one:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

2. Build once: the plugin writes the renderer into `$SRCROOT/ComposeSwiftUI` (and the integrations into
   `$SRCROOT/ComposeSwiftUIIntegrations`).
3. Add the `ComposeSwiftUI` folder to every app target that shows Kotlin screens, and the integration files you use.
   The renderer sources compile for all four platforms; the `#if os` guards inside handle the differences.
4. Set the deployment targets to at least iOS 17, macOS 14, tvOS 17 and watchOS 10.

`embedAndSignAppleFrameworkForXcode` picks the Kotlin target from Xcode's SDK and architecture, so the same script
works for the iOS, macOS, tvOS and watchOS targets. The renderer folder is rewritten on every build; since all targets
share it, keep a single copy at `$SRCROOT/ComposeSwiftUI` (or set `swiftOutputDir`) and reference it from each target.

A watchOS app target and a widget extension are separate Xcode targets: each one that shows Kotlin views needs the
framework and the renderer. See [Tips and widgets](tips-and-widgets.md) for widget extensions.
