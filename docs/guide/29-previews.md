<!-- group: Platforms and tools -->
# Previews

> Annotate a composable with @Preview to see it rendered by real SwiftUI, next to the code in IntelliJ IDEA and Android Studio or in Xcode's canvas.

Compose SwiftUI draws with SwiftUI, so its previews are rendered by SwiftUI too, never approximated. There are two
ways to see them, both driven by the same `@Preview` annotation:

| Where | How it renders | Needs |
|---|---|---|
| IntelliJ IDEA / Android Studio | The `renderComposeSwiftUIPreviews` Gradle task snapshots each preview on the iOS simulator or on the Mac; the IDE plugin shows the PNGs next to the code | macOS (or, from Linux or Windows, a [remote Mac](#from-linux-or-windows-a-remote-mac)), Xcode, an `iosSimulatorArm64` (or `iosX64`) or `macosArm64` (or `macosX64`) target, the IDE plugin |
| Xcode | A generated `ComposeSwiftUIPreviews.swift` holds one Swift `#Preview` per Kotlin preview | The app builds in Xcode |

## Writing previews

Annotate a top-level `@Composable` function:

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

- be annotated with both `@Composable` and an annotation named `Preview` (or a multipreview annotation, below), in any
  order, on one or several lines. `dev.nucleusframework.composeswiftui.Preview` is provided; any other annotation named
  `Preview` works too, including `org.jetbrains.compose.ui.tooling.preview.Preview`;
- start at the beginning of a line (not indented, so not inside a class or object);
- not be `private` (nor `expect`, `suspend`, `abstract` or `override`);
- take no parameters, except one annotated with `@PreviewParameter`.

The IDE plugin flags a `@Preview` that breaks these rules, with a quick fix for `private` and a missing `@Composable`.

### Preview options

The arguments of `@Preview` set up the preview the way Android's `@Preview` does, with SwiftUI's vocabulary. Every one
is optional, and they must be literals (or enum entries):

| Argument | Effect |
|---|---|
| `name` | Title in the IDE, in Xcode and in the gallery. Default: the function name |
| `group` | Group, to filter the previews in the IDE |
| `device` | Simulator (name or UDID) that renders it in the IDE, or `"mac"` to render it on the Mac. Default: `previewDevice` |
| `width`, `height` | Size in points. Default: the device screen. A custom size drops the safe areas and sets the size classes (regular from 700 points wide) |
| `sizeThatFits` | Sizes the preview to its content, like Xcode's `.sizeThatFitsLayout` |
| `colorScheme` | `PreviewColorScheme.Light` or `Dark` |
| `dynamicTypeSize` | `PreviewDynamicTypeSize.XSmall` … `Accessibility5` |
| `locale` | Locale identifier, e.g. `"fr_FR"` |
| `layoutDirection` | `PreviewLayoutDirection.LeftToRight` or `RightToLeft` |
| `backgroundColor` | `0xAARRGGBB` behind the preview |

```kotlin
@Composable
fun Badge(count: Int) {
    HStack(modifier = Modifier.padding()) {
        Image(systemName = SFSymbol.bellFill, modifier = Modifier.foregroundStyle(Color.orange))
        Text("$count new messages")
    }
}

@Preview(name = "Badge", group = "Inbox", sizeThatFits = true, dynamicTypeSize = PreviewDynamicTypeSize.Accessibility3)
@Composable
fun BadgePreview() {
    Badge(3)
}
```

### Variants

Repeat `@Preview` to render the same function several ways. Each variant is titled `Function - name`:

```kotlin
@Composable
fun SettingsScreen() {
    Form {
        Toggle("Notifications", isOn = true, onIsOnChange = {})
        Text("Version 1.0")
    }
}

@Preview(name = "Light", colorScheme = PreviewColorScheme.Light)
@Preview(name = "Dark", colorScheme = PreviewColorScheme.Dark)
@Preview(name = "Arabic", locale = "ar", layoutDirection = PreviewLayoutDirection.RightToLeft)
@Composable
fun SettingsPreview() {
    NavigationStack { SettingsScreen() }
}
```

A set of variants you use often goes on an annotation class, a *multipreview*, which you then put on previews like
`@Preview`. The library provides four: `@PreviewLightDark`, `@PreviewDynamicTypeSizes` (extra small, default and two
accessibility sizes), `@PreviewScreenSizes` (small and large iPhone, iPad in portrait and landscape) and
`@PreviewLayoutDirections`. Declare your own anywhere in the scanned sources, and combine them:

```kotlin
@Preview(name = "Phone", width = 393, height = 852)
@Preview(name = "Tablet", width = 1024, height = 1366)
annotation class DevicePreviews

@DevicePreviews
@PreviewLightDark
@Composable
fun SettingsAllPreview() {
    NavigationStack { SettingsScreen() }
}
```

Modifiers still work for anything the arguments don't cover, as the environment of a preview is the one of a plain
SwiftUI view: `NavigationStack(modifier = Modifier.environment(…)) { … }`.

### Preview parameters

`@PreviewParameter` renders a preview once per value of a `PreviewParameterProvider`, instead of one function per
sample. The provider is a class with a public no-argument constructor; `limit` caps the number of values:

```kotlin
class ContactProvider : PreviewParameterProvider<Contact> {
    override val values = sequenceOf(
        Contact("Ada Lovelace", "+44 20 0000 0000", favorite = true),
        Contact("Grace Hopper", "+1 202 000 0000", favorite = false),
    )
}

@Preview(name = "Contact", sizeThatFits = true)
@Composable
fun ContactPreview(@PreviewParameter(ContactProvider::class) contact: Contact) {
    ContactRow(contact)
}
```

The IDE shows them as `Contact (1 of 2)`, `Contact (2 of 2)`; Xcode shows one `#Preview` that pages through the values.

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

A Kotlin file with previews opens in a split editor: code on the left, rendered previews on the right (the editor's
*Code*, *Split* and *Preview* buttons switch between them). Like Android Studio's Compose preview:

- **Rendering is automatic.** A file opens with up-to-date previews, and they render again when a Kotlin file of the
  project is saved, only while the pane is visible. *Render while typing* saves the file after a pause instead.
  Choose in the ⚡ menu of the pane or in *Settings › Tools › Compose SwiftUI Previews*; *Only when asked* leaves the
  *Render Previews* button, the gutter icons and *Code › Render Compose SwiftUI Previews* (⌥⇧⌘P on macOS,
  Ctrl+Alt+Shift+P elsewhere). The status shows *Out of date* when the code changed since the images.
- **Layouts:** a list, a grid, or a gallery of one preview at a time with previous and next buttons. Zoom with the
  toolbar or Ctrl/⌘ + wheel; *Zoom to Fit* follows the width of the pane. The group menu shows one `group`.
- **Code and previews follow each other.** Moving the caret into a preview function selects its preview; clicking a
  preview moves the caret to its function, double-clicking focuses the code.
- **Click a view to go to its code.** Hovering a preview outlines the view under the mouse and names its call
  (`Text · Settings.kt:22`); a click moves the caret to that call, a double-click focuses the code.
- **Live literals.** Editing a string, a number or a boolean shows in the previews within half a second, without a
  build: the host already running on the simulator patches the views that come from that literal. Saving builds as
  usual and replaces the patch. Any other edit waits for the build (the status says *Out of date*).
- **Interactive mode** (right-click a preview, or its gutter icon): the preview runs live in the pane. The mouse taps,
  drags and scrolls, the keyboard types into the focused field, and literal edits apply as you type. In its toolbar:
  - *Select Component* goes to the code of the view you click;
  - *Slow Animations* runs animations ten times slower (the simulator's *Slow Animations*);
  - *Record Animation* captures the frames until you stop it, then a timeline replays them at their pace or frame by
    frame: the animation inspector.
- **Run Preview** opens a preview in the Simulator app instead, full screen, with the variant's settings (a Mac
  preview opens in a window of its own).
- **Errors stay next to the previews.** A compile error lists its file and line, as links; a preview that throws shows
  the Kotlin exception in its place while the others render.
- Right-click a preview to copy its image, save it as PNG, or render only that preview. *Code › Render All Compose
  SwiftUI Previews of the Module* renders every file at once.

### Source information

Click-to-code and live literals use the source information the Compose compiler records for every composable call.
The Gradle plugin turns it on (`composeCompiler { includeSourceInformation }`) except in Xcode *Release* builds, so the
IDE and Xcode *Debug* builds share their compilations. It costs nothing at run time unless a preview host collects it,
and a few strings in the binary; set `composeCompiler { includeSourceInformation = false }` to turn it off.

### How rendering works

The IDE runs the Gradle task `:<module>:renderComposeSwiftUIPreviews` of the module that contains the file, with the
previews of that file. You can run it yourself, for all of them or a few:

```bash
./gradlew :shared:renderComposeSwiftUIPreviews
./gradlew :shared:renderComposeSwiftUIPreviews -PcomposeSwiftUI.previews=sample.ContactPreview,sample.InboxPreview
```

The task:

1. Links the **debug framework of the simulator target**: `iosSimulatorArm64` on Apple silicon, `iosX64` on Intel
   Macs. The IDE turns on Kotlin/Native incremental compilation (`-Pkotlin.incremental.native=true`), which shortens
   this link; turn it off in the settings if your build doesn't support it.
   A Mac preview uses the debug framework of the macOS target (`macosArm64`, or `macosX64`) instead (see
   [Previews on the Mac](#previews-on-the-mac)).
2. Builds a small host app with `swiftc`: the Swift renderer and a runner that looks previews up by name. It is
   compiled once per library version (about a minute), not when previews or your app's API change. The app is
   relinked only when the framework changed (never for a dynamic framework, whose copy in the app is replaced), with
   `-lc++`, `-lsqlite3`, `-lz` and the framework's `linkerOpts` when it is static, and the module's Compose
   Multiplatform resources.
3. Installs it on the simulator of each preview when it changed, and keeps it running between renders: a render
   without a Kotlin change doesn't relaunch it. The simulator is the `device` argument, else `previewDevice`, else
   one of its own, *Compose SwiftUI Previews*, created like your booted iPhone and booted in the background: an iOS
   app only draws in the foreground of its device, so the previews don't take over the simulator you run your app
   on. Previews with different devices render on their simulators in parallel.
4. The app composes up to 8 previews at once, each in its own window at its size, and snapshots each one as soon as
   two frames 100 ms apart are identical, so a static preview takes a few hundred milliseconds. A preview that keeps
   changing (a spinner, an endless animation) is captured at the settle limit, 1.5 s by default.
5. The PNGs land in `build/composeSwiftUI/previews/<key>.png` with `results.json`, where the IDE plugin reads them.
   Rendering one file keeps the images of the others.

If a preview crashes the app (a Kotlin exception), the previews it was composing are rendered again one at a time:
only the culprit fails, with its exception.

The host also writes `<key>.layout.json` next to each image: the frame of every view with the calls that produced
it. Live literals, interactive mode and the inspector talk to the running host directly (no Gradle): the IDE writes
its request file, and a live preview streams its frames over a local TCP port while the IDE sends touches back. Taps
are real touches (synthesized `IOHIDEvent`s routed to the preview's window), so buttons, toggles, lists and gestures
behave as on a device.

Because the previews run on a real simulator, they render exactly like the app on that device: fonts, SF Symbols,
Liquid Glass, Dynamic Type, safe areas. By default each preview gets the full screen of the device; use `width` and
`height`, or `sizeThatFits`, for components.

### Previews on the Mac

A macOS app's previews render on the Mac itself, with AppKit's SwiftUI: a `List` is a table, a `Picker` a pop-up
button, a `Form` is aligned in columns, exactly like the Mac app.

- In a module with a `macosArm64()` target (`macosX64()` on an Intel Mac) and no iOS simulator target, every preview
  renders on the Mac.
- In a module with both, previews render on the simulator; `device = "mac"` sends one to the Mac, and
  `previewDevice.set("mac")` the whole module. A multipreview can have variants on both:

```kotlin
@Preview(name = "iPhone", sizeThatFits = true)
@Preview(name = "Mac", device = "mac", width = 400, sizeThatFits = true)
@Composable
fun MessageCountPreview() {
    Text("5 new messages", modifier = Modifier.padding())
}
```

The macOS framework is only linked when a preview needs it: when a preview's source says `device = "mac"` (written
as a literal), `previewDevice` is `"mac"`, or the module has no iOS simulator target.

The Mac host runs in the background, without a Dock icon and without taking the focus, each preview in a window off
the screen, captured as the WindowServer draws it (materials and vibrancy included). It doesn't need the screen
recording permission. A preview without `width` or `height` gets a typical window, 800 × 600 points; with
`sizeThatFits`, its content's size. The images have the scale of your screen. Live literals, click-to-code, the agent
tools and interactive mode work the same: in interactive mode the mouse clicks, drags and scrolls and the keyboard
types, as real mouse and key events. *Slow Animations* has no effect on the Mac, and `dynamicTypeSize` is ignored
there, as macOS has no Dynamic Type. *Run Preview* opens the preview in a regular window.

### Configuration

```kts
composeSwiftUI {
    // Simulator name or UDID used for previews (default: "Compose SwiftUI Previews", created and booted in the
    // background), or "mac" to render them on the Mac
    previewDevice.set("iPhone 17 Pro")
    // The app's asset catalogs, for named colors and images (default: the .xcassets of the app's sources, the folder
    // of swiftOutputDir)
    previewAssetCatalogs.from("../iosApp/iosApp/Assets.xcassets")
}
```

`-PcomposeSwiftUI.previewSettleMs=3000` raises the longest wait for a preview to settle (also in the IDE settings).

The preview host is an app of its own, so it only has the resources it is given: the module's Compose Multiplatform
resources, and the app's asset catalogs, compiled into it as Xcode does for the app. `Color("Brand")` and
`Image("logo")` then render as in the app, with their dark variants. By default the catalogs are the `.xcassets`
folders found in the app's sources: the folder `swiftOutputDir` is in, and its subfolders. Set `previewAssetCatalogs`
when they are elsewhere.

Previews render in the language of the previews' simulator (English, by default), not the one of the simulator you
run the app on: set `locale` on `@Preview` to see another one.

The module needs an `iosSimulatorArm64()` target (`iosX64()` on an Intel Mac) or a `macosArm64()` target (`macosX64()`)
with a framework binary; Kotlin creates the debug variant of every `binaries.framework { }`. A module with only tvOS or
watchOS targets has no IDE previews; use Xcode's canvas there.

### From Linux or Windows: a remote Mac

The IDE doesn't have to run on the Mac. On Linux or Windows, the plugin uses a Mac reached over SSH for everything
that needs Apple's tools, and keeps the rest on your machine: the IDE, indexing, the Gradle sync, Android and JVM
builds. Set it in *Settings › Tools › Compose SwiftUI Previews › Remote Mac*:

- **SSH destination**: an alias of `~/.ssh/config` or `user@host`. Authentication must work without a prompt (a key),
  and the Mac needs *Remote Login* on (*System Settings › General › Sharing*), Xcode, and `rsync` here.
- *Test Connection* shows the Mac's macOS and Xcode versions.

Then previews work as on a Mac:

- Before a build, the project is synchronized to `~/.compose-swiftui/projects/` on the Mac with rsync, only what
  changed (a fraction of a second once copied). The whole Git repository is sent, so builds it includes from elsewhere
  in the repository are there; `build/`, `.gradle/`, `.git/` and `local.properties` never are.
- Gradle runs in the Mac's daemon, kept warm between renders, with the Mac's Android SDK when the project has an
  Android target.
- The images, results and component trees come back; compiler errors and the previews' sources have this machine's
  paths, so their links open your files.
- Live literals and renders without a code change talk to the running host directly, one SSH round trip each.
  Interactive mode streams over a port forwarded by SSH.

All the commands share one SSH connection (OpenSSH connection sharing), so each costs a round trip on the network, not
a handshake. *Use it on a Mac too* sends the work to the remote Mac from a Mac IDE as well, to offload a small Mac.

### The simulator in the IDE

The *iOS Simulator* tool window shows a simulator of the Mac, live, like Android Studio's *Running Devices*: it
connects to the booted simulator when it opens, or pick one and press ▶ (it boots if needed). A click is a touch, a
drag a swipe, the wheel scrolls, and the keyboard types. Beside the screen are the device's controls: *Home* and the
*App Switcher* (the gestures of Face ID iPhones), *Lock*, *Volume Up* and *Volume Down*, *Rotate Left* and *Rotate
Right*, a *Screenshot* (copied, and saved
where you choose), *Shake*, and light or dark appearance. It works on the Mac and, through the remote Mac, from Linux
or Windows. A foldable simulator (iPhone Duo) shows the screen its posture lights, view only: its touchscreens go
through CoreDevice, which the tool window can't reach yet.

It doesn't need the Simulator app: a small helper, compiled on the Mac on first use, reads the simulator's
framebuffer and sends touches and keys as hardware events, the way the Simulator app does. No window to keep open and
no screen recording permission. The hardware buttons go through Xcode's CoreDevice framework, with a second helper
compiled on the Mac on first use.

### Running the app

From Linux or Windows, the plugin adds an *iOS App* run configuration for the project's Xcode project when it opens
the project, as the Kotlin Multiplatform plugin does on a Mac, and the toolbar lists the Mac's simulators next to it.
▶ builds the app on the Mac with Xcode, installs it on the simulator picked there and runs it, with its output in the
Run tool window, and shows the *iOS Simulator* tool window. *Stop* ends the app on the simulator. The Xcode project
builds the Kotlin framework as usual (`embedAndSignAppleFrameworkForXcode`), on the Mac. Add more configurations in
*Run › Edit Configurations › + › iOS App* (another scheme, configuration or simulator).

## For AI agents

Coding agents (Claude Code, Junie, Codex, Gemini…) use the previews the way Android Studio's agent uses Compose
previews: render, look at the image and the component tree, edit, render again until it's right. Everything an agent
needs is text and PNG files, from the IDE or the command line.

### In the IDE, over MCP

When the IDE's *MCP Server* plugin is on (bundled in IntelliJ IDEA, *Settings › Tools › MCP Server*), the Compose
SwiftUI plugin adds its tools to it. An agent connected to the IDE gets the tools below.

Android Studio doesn't have that plugin, and its agent only relays its own tools: there, the Compose SwiftUI plugin
serves the same tools on an MCP server of its own, `http://127.0.0.1:64360/mcp` (streamable HTTP, local connections
only). Add it to Android Studio's agent in *Settings › Tools › AI › MCP Servers*, in `mcp.json`:

```json
{
  "mcpServers": {
    "compose-swiftui": {
      "httpUrl": "http://127.0.0.1:64360/mcp"
    }
  }
}
```

Any other MCP client works the same, for example Claude Code:

```bash
claude mcp add --transport http compose-swiftui http://127.0.0.1:64360/mcp
```

*Settings › Tools › Compose SwiftUI Previews › AI Agents* shows the address and copies both. The server runs while
the IDE does: when the IDE has no MCP server of its own by default, or always, or never. When port 64360 is taken
(another IDE), the next free one is used and shown there. With several projects open, the tools take the project
from their file paths, or from a `projectPath` argument.

| Tool | Does | Android Studio's equivalent |
|---|---|---|
| `list_swiftui_previews` | The previews of a file (or of the project) and their variants, numbered | `android preview list` |
| `render_swiftui_preview` | Renders a preview (all variants or one `previewIndex`), or any composable without parameters: image path, component tree, UI checks, or the compiler errors and exception | `render_compose_preview` |
| `render_swiftui_preview_code` | Renders throwaway code in the module of a file, without editing the project | `show_in_memory_compose_preview` |
| `check_swiftui_preview` | UI checks of a preview: touch targets, text contrast, small text, overlaps, clipping | `visual_lint_service_tool`, UI Check |
| `compare_swiftui_preview_to_image` | Renders and compares with an expected image (a design, a screenshot): mismatch, PSNR, the views of the regions that differ, a side-by-side image | Match UI to Target Image, `compose_visual_comparison_judge` |
| `compare_images` | Compares two images: mismatch %, MSE, RMSE, PSNR, a diff image | `compare_images` |
| `verify_batch_visual_parity` | Compares two folders of screenshots by name, writes a Markdown report | `verify_batch_visual_parity` |
| `run_apple_gradle` | Compiles, links, or tests for iOS and macOS via Gradle on the Mac (locally on macOS, or via the Remote Mac from Linux/Windows) | — |
| `build_xcode_project` | Compiles or tests an iOS or macOS `.xcodeproj`/`.xcworkspace` with `xcodebuild` on the Mac (locally or via the Remote Mac) | — |

Renders go through the same renderer as the preview pane: open files are saved, the running host renders without
Gradle when no code changed, and only one build runs per module at a time.

### On the command line

Without an IDE, the Gradle tasks do the same, like Android's `android preview` commands:

```bash
./gradlew :shared:listComposeSwiftUIPreviews -PcomposeSwiftUI.previewFile=shared/src/commonMain/kotlin/app/Inbox.kt
./gradlew :shared:renderComposeSwiftUIPreviews -PcomposeSwiftUI.previewFile=shared/src/commonMain/kotlin/app/Inbox.kt
./gradlew :shared:renderComposeSwiftUIPreviews -PcomposeSwiftUI.previews=app.InboxPreview -PcomposeSwiftUI.golden=design/inbox.png
```

Each render writes, in `build/composeSwiftUI/previews`, next to the images:

- `report.md`: for each preview rendered, its image, its UI checks, its component tree, and its comparison with the
  expected image;
- `<key>.tree.txt`: the component tree, one view per line with its call site, frame in points and text:

```text
Preview 402×73 pt
  HStack · Contacts.kt:21 · x=0 y=0 w=402 h=73
    VStack · Contacts.kt:22 · x=16 y=16 w=132 h=41
      Text "Ada Lovelace" · Contacts.kt:23 · x=16 y=16 w=105 h=20
      Text "+44 20 0000 0000" · Contacts.kt:24 · x=16 y=39 w=132 h=18
    Image "star.fill" · Contacts.kt:27 · x=364 y=27 w=22 h=20
```

`-PcomposeSwiftUI.golden` takes a PNG, compared with the first image rendered, or a folder of `<key>.png` (a copy of
`build/composeSwiftUI/previews` from before a change), compared image by image: a visual regression check.
`listComposeSwiftUIPreviews -PcomposeSwiftUI.format=json` prints the previews as JSON.

Throwaway previews go to `build/composeSwiftUI/scratch/` (any file and package): they are compiled into the module
and rendered like the others, and never into Xcode *Release* builds. Delete the file when done.

### UI checks

The checks follow Apple's Human Interface Guidelines and WCAG, from the views and the image of each preview. They are
heuristics, worth a look rather than proofs:

| Category | Check |
|---|---|
| Accessibility | A button, toggle, link, picker or text field smaller than 44×44 points (not in a list or form row) |
| Contrast | A text whose contrast with its background, estimated from the image, is under 4.5:1 (3:1 from 24 points tall) |
| Typography | A text under 11 points tall |
| Layout | Texts drawn over each other; a view past the edge of the preview (not in a scrolling container) |

Render variants to check adaptive layouts: `@PreviewDynamicTypeSizes`, `@PreviewScreenSizes`,
`@PreviewLayoutDirections`, `@PreviewLightDark`.

## In Xcode

On every build, the plugin writes `ComposeSwiftUIPreviews.swift` next to the renderer (`iosApp/ComposeSwiftUI`), with
one Swift `#Preview` per Kotlin preview variant:

```swift
#Preview("Contact row") {
    ComposeView { ComposeSwiftUIPreviews.shared.contactRowPreview() }
}
```

Open that file in Xcode to see the previews in the canvas. The `@Preview` arguments become the matching `#Preview`
traits (`.fixedLayout`, `.sizeThatFitsLayout`) and modifiers. Xcode previews run in the canvas of whatever destination
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
| "Compose SwiftUI previews need the `iosSimulatorArm64()` or `macosArm64()` target…" | Add the simulator or macOS target with `binaries.framework { }` to the module. |
| "previews on the Mac need the `macosArm64()` target…" | Add the macOS target, or, if it is there and the device comes from a constant, write `device = "mac"` as a literal or set `previewDevice`. |
| "…need a debug framework on `iosSimulatorArm64`" | The framework is declared only for release (`binaries.framework(listOf(RELEASE))`); add `DEBUG`. |
| Link error with "Undefined symbols" | The host app does not get your Xcode target's `OTHER_LDFLAGS`. Declare the system libraries the Kotlin code needs on the framework: `binaries.framework { linkerOpts("-lsqlite3") }`. They are forwarded for static frameworks; a dynamic framework already links them itself. `sqlite3` (SQLDelight) and `zlib` are linked automatically since 0.6.0. |
| "no iOS simulator found" | Install an iOS simulator runtime in Xcode › Settings › Components, or set `previewDevice` to an existing device name or UDID. |
| A preview is missing | Check the rules above (the IDE plugin highlights a `@Preview` that breaks them): top-level at column 0, not private, no parameter except one `@PreviewParameter`, `@Composable` present, and in a scanned source set. The task log lists the previews found (`--info`). |
| A preview shows a spinner or empty content | A preview is captured once it stops changing, or at the settle limit (1.5 s). Content loaded asynchronously after that is not captured: preview with fake, synchronous data, or raise the limit in the IDE settings (`-PcomposeSwiftUI.previewSettleMs`). |
| Clicking a view goes nowhere, or live literals don't apply | Both need the source information (see above) and a render since it was turned on. Live literals only cover string, number and boolean literals; other edits wait for the build. |
| Interactive mode says "touches unavailable" | The simulator runtime lacks the private touch API the host uses: the preview still streams; use *Run Preview* to interact. |
| A preview shows a Kotlin exception | It threw while composing: the other previews still render. The stack trace has no line numbers; reproduce it in the app or a test. |
| "not in the framework yet" | The preview was added after the framework was linked by another task: render again. |
| A preview shows "Unsupported view kind" or a red "No native view" | The renderer is older than the library (stale Xcode copy), or a `NativeView` key registered by your app is missing in the preview host, which does not run your app's `init`. Avoid `NativeView` in previews, or guard it. |
| Xcode doesn't see `ComposeSwiftUIPreviews.swift` | If the `ComposeSwiftUI` folder was added to Xcode as a group (not a folder reference), add the file to the app target once. |
| The first render is slow | It compiles the Swift renderer once per library version (about a minute), and the first render after an IDE restart warms up Gradle; later renders reuse both. |
| Rendering times out | A render is limited to 5 minutes. A preview that blocks the main thread (a `runBlocking`, an infinite loop in composition) stalls all of them. |
