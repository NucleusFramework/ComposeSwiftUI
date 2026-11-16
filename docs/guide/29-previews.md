<!-- group: Platforms and tools -->
# Previews

> Annotate a composable with @Preview to see it rendered by real SwiftUI, next to the code in IntelliJ IDEA and Android Studio or in Xcode's canvas.

Compose SwiftUI draws with SwiftUI, so its previews are rendered by SwiftUI too, never approximated. There are two
ways to see them, both driven by the same `@Preview` annotation:

| Where | How it renders | Needs |
|---|---|---|
| IntelliJ IDEA / Android Studio | The `renderComposeSwiftUIPreviews` Gradle task snapshots each preview on the iOS simulator; the IDE plugin shows the PNGs next to the code | macOS, Xcode, an `iosSimulatorArm64` (or `iosX64`) target, the IDE plugin |
| Xcode | A generated `ComposeSwiftUIPreviews.swift` holds one Swift `#Preview` per Kotlin preview | The app builds in Xcode |

## Writing previews

Annotate a top-level, parameterless `@Composable` function:

```kotlin
data class Contact(val name: String, val phone: String, val favorite: Boolean)

@Composable
fun ContactRow(contact: Contact) {
    HStack {
        Text(contact.name)
        Spacer()
        if (contact.favorite) Image(systemName = SFSymbol.starFill, modifier = Modifier.foregroundStyle(Color.yellow))
    }
}

@Preview(name = "Contact row")
@Composable
fun ContactRowPreview() {
    List { ContactRow(Contact("Ada Lovelace", "+44 20 0000 0000", favorite = true)) }
}
```

The rules come from how previews are found: the Gradle plugin scans the sources (no compiler involved), so the
function must

- be annotated with both `@Composable` and an annotation named `Preview`, in any order, on one or several lines.
  `dev.nucleusframework.composeswiftui.Preview` is provided; any other annotation named `Preview` works too, including
  `org.jetbrains.compose.ui.tooling.preview.Preview`;
- start at the beginning of a line (not indented, so not inside a class or object);
- not be `private` (nor `expect`, `suspend`, `abstract` or `override`);
- take no parameters (there is no `@PreviewParameter`; write one preview per variant).

`name` is the title shown in Xcode, in the gallery and in the IDE; it defaults to the function name.

### Variants

Configure a variant with the same modifiers your app would use. The environment of a preview is the one of a
plain SwiftUI view, so set what you want to see on the root:

```kotlin
@Composable
fun SettingsScreen() {
    Form {
        Toggle("Notifications", isOn = true, onIsOnChange = {})
        Text("Version 1.0")
    }
}

@Preview(name = "Settings, dark")
@Composable
fun SettingsPreviewDark() {
    NavigationStack(
        modifier = Modifier.preferredColorScheme(ColorScheme.dark),
    ) { SettingsScreen() }
}

@Preview(name = "Settings, accessibility size")
@Composable
fun SettingsPreviewLargeText() {
    NavigationStack(modifier = Modifier.dynamicTypeSize(DynamicTypeSize.accessibility3)) { SettingsScreen() }
}
```

### Data and view models

A preview runs your real composables in a real composition: `remember`, effects, `viewModel { }` and
`collectAsState` all work. Code that reaches the network or a database runs too, which is rarely what you want in a
preview. Hoist state so the screen takes plain values, and preview that version:

```kotlin
data class InboxState(val threads: List<String>, val loading: Boolean)

@Composable
fun InboxContent(state: InboxState, onOpen: (String) -> Unit) {
    if (state.loading) {
        ProgressView()
    } else {
        List { for (thread in state.threads) key(thread) { Button(thread) { onOpen(thread) } } }
    }
}

@Preview(name = "Inbox")
@Composable
fun InboxPreview() {
    NavigationStack { InboxContent(InboxState(listOf("Welcome", "Invoice #42"), loading = false), onOpen = {}) }
}

@Preview(name = "Inbox, loading")
@Composable
fun InboxLoadingPreview() {
    InboxContent(InboxState(emptyList(), loading = true), onOpen = {})
}
```

## In IntelliJ IDEA and Android Studio

The previews in the IDE need the Compose SwiftUI IDE plugin. It is not on the JetBrains Marketplace yet:

1. Download `compose-swiftui-intellij-plugin-<version>.zip` from the
   [GitHub releases](https://github.com/NucleusFramework/ComposeSwiftUI/releases), or build it with
   `./gradlew -p intellij-plugin buildPlugin` (the zip lands in `intellij-plugin/build/distributions/`).
2. In the IDE, open *Settings › Plugins › ⚙ › Install Plugin from Disk…*, pick the zip, and restart when asked.

Repeat these steps to update: the IDE doesn't offer updates for a plugin installed from disk.

A Kotlin file with previews opens in a split editor: code on the left, rendered previews on the right. Rendering
only runs on demand: the editor's *Render Previews* button, the gutter icon of a preview (which also scrolls to it), or
*Code › Render Compose SwiftUI Previews* (⌥⇧⌘P on macOS, Ctrl+Alt+Shift+P elsewhere). Rendering runs at most once at a
time per module; a request during a run queues one more.

### How rendering works

The IDE runs the Gradle task `:<module>:renderComposeSwiftUIPreviews` of the module that contains the file (the
Gradle path is derived from the module directory). You can run it yourself:

```bash
./gradlew :shared:renderComposeSwiftUIPreviews
```

The task:

1. Links the **debug framework of the simulator target**: `iosSimulatorArm64` on Apple silicon, `iosX64` on Intel
   Macs.
2. Builds a small host app with `swiftc`: the Swift renderer, the generated list of previews and a runner. The Swift
   object file is cached, keyed by the renderer, the preview list, the framework headers and the SDK: the first run
   compiles it (about a minute), after that a Kotlin change only costs the framework link and the app link.
3. Links the app against the framework, with `-lc++`, `-lsqlite3` and `-lz`, plus the framework's `linkerOpts` when
   it is static. Copies the module's Compose Multiplatform resources into the bundle, signs it ad hoc.
4. Installs and launches it on the simulator: the one set in `previewDevice`, otherwise the booted one, otherwise
   the last available iPhone, which it boots.
5. The app shows each preview full screen, waits for it to settle (800 ms), snapshots the window and moves on. The
   PNGs land in `build/composeSwiftUI/previews/<qualified function name>.png`, where the IDE plugin reads them.

Because the previews run on a real simulator, they render exactly like the app on that device: fonts, SF Symbols,
Liquid Glass, Dynamic Type, safe areas. Each preview gets the full screen of the device; wrap small components in a
`List`, a `Form` or a `frame` to see them in context.

### Configuration

```kts
composeSwiftUI {
    // Simulator name or UDID used for previews (default: the booted simulator, else the last available iPhone)
    previewDevice.set("iPhone 17 Pro")
}
```

The module needs an `iosSimulatorArm64()` target (`iosX64()` on an Intel Mac) with a framework binary; Kotlin creates
the debug variant of every `binaries.framework { }`. A module with only macOS, tvOS or watchOS targets has no IDE
previews; use Xcode's canvas there.

## In Xcode

On every build, the plugin writes `ComposeSwiftUIPreviews.swift` next to the renderer (`iosApp/ComposeSwiftUI`), with
one Swift `#Preview` per Kotlin preview:

```swift
#Preview("Contact row") {
    ComposeView { ComposeSwiftUIPreviews.shared.contactRowPreview() }
}
```

Open that file in Xcode to see the previews in the canvas. Xcode previews run in the canvas of whatever destination
the scheme targets, so they also work for macOS, tvOS and watchOS apps. After a Kotlin change, build once (the build
phase relinks the framework and regenerates the file), then refresh the canvas.

The same file declares `ComposeSwiftUIPreviewGallery`, a SwiftUI view that lists every preview and opens each one in a
sheet. Show it from a debug menu to browse the previews on a device:

```swift
.sheet(isPresented: $showPreviews) { ComposeSwiftUIPreviewGallery() }
```

Or show it from Kotlin, through a native view registered in Swift (see [Interop](interop.md)):

```swift
ComposeRegistry.nativeViews["previewGallery"] = { _ in AnyView(ComposeSwiftUIPreviewGallery()) }
```

```kotlin
@Composable
fun DebugMenu() {
    var showPreviews by remember { mutableStateOf(false) }
    Button("Browse previews", modifier = Modifier.sheet(isPresented = showPreviews, onIsPresentedChange = { showPreviews = it }) {
        NativeView("previewGallery")
    }) { showPreviews = true }
}
```

Under the hood, the plugin also generates a Kotlin object, `ComposeSwiftUIPreviews`, with one function per preview
returning a `ComposeSwiftUIHost`. It is compiled into every Apple framework of the module, which is why previews must
be callable from every target (next section).

## Which source sets are scanned

Only the source sets compiled into **every** Apple framework of the module are scanned, so the generated code can call
each preview from every target:

- `commonMain` and `appleMain` always;
- `iosMain` for an iOS-only module. If the module also has macOS (or tvOS, watchOS) targets, a preview in `iosMain`
  is skipped, because the macOS framework could not call it. Move it to `commonMain`, or keep iOS-only previews in an
  iOS-only module.

Generated source directories (under `build/`) are never scanned.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| "Compose SwiftUI previews need the `iosSimulatorArm64()` target with a framework binary" | Add the simulator target with `binaries.framework { }` to the module. |
| "…need a debug framework on `iosSimulatorArm64`" | The framework is declared only for release (`binaries.framework(listOf(RELEASE))`); add `DEBUG`. |
| Link error with "Undefined symbols" | The host app does not get your Xcode target's `OTHER_LDFLAGS`. Declare the system libraries the Kotlin code needs on the framework: `binaries.framework { linkerOpts("-lsqlite3") }`. They are forwarded for static frameworks; a dynamic framework already links them itself. `sqlite3` (SQLDelight) and `zlib` are linked automatically since 0.6.0. |
| "no iOS simulator found" | Install an iOS simulator runtime in Xcode › Settings › Components, or set `previewDevice` to an existing device name or UDID. |
| A preview is missing | Check the rules above: top-level at column 0, not private, no parameters, `@Composable` present, and in a scanned source set. The task log lists the previews found (`--info`). |
| A preview shows a spinner or empty content | The host snapshots after 800 ms; content loaded asynchronously after that is not captured. Preview with fake, synchronous data. |
| A preview shows "Unsupported view kind" or a red "No native view" | The renderer is older than the library (stale Xcode copy), or a `NativeView` key registered by your app is missing in the preview host, which does not run your app's `init`. Avoid `NativeView` in previews, or guard it. |
| Xcode doesn't see `ComposeSwiftUIPreviews.swift` | If the `ComposeSwiftUI` folder was added to Xcode as a group (not a folder reference), add the file to the app target once. |
| The first render is slow | It compiles the Swift renderer once (about a minute); later renders reuse it. A renderer upgrade or new previews recompile it. |
| Rendering times out | Launching the host app is limited to 5 minutes. A preview that blocks the main thread (a `runBlocking`, an infinite loop in composition) stalls all of them. |
