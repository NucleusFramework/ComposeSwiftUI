<!-- group: Platforms and tools -->
# Troubleshooting

> Symptoms you may hit when setting up or using Compose SwiftUI, what causes each one, and how to fix it.

Most problems come from one of three places: the Xcode project (renderer files, build phase, linking), the
integration registry (a Swift renderer that isn't registered), or the controlled-state model (a callback that doesn't
update state). The sections below go from build time to run time.

## Build and Xcode

### "Cannot find 'ComposeView' in scope"

The renderer isn't compiled into the app target. Build once so the plugin writes `ComposeSwiftUI/` next to your
Xcode project, then add that folder to the app target (see [Installation](installation.md)). Check the file inspector:
*Target Membership* must include the app.

If the folder is there but empty, the build phase didn't run the sync: make sure the *Run Script* phase calls
`embedAndSignAppleFrameworkForXcode` and runs before *Compile Sources*.

### "Cannot find 'InteractiveRenderers' in scope" (or another renderer type) after upgrading

A release added files to the renderer, and the `ComposeSwiftUI` folder was added to Xcode as a group: groups don't
pick up new files. Add the missing files to the app target, or remove the group and add the folder again as a
synchronized folder so this doesn't happen on the next upgrade. The CHANGELOG lists new renderer files; for example
0.5.0 added `Interactive.swift`, `Advanced.swift`, `Completions.swift` and `Generated/GenEncoders.swift`.

### The build phase fails with a sandbox error

Xcode's user script sandboxing (on by default in new projects) blocks the Gradle build phase from writing the
renderer into the project directory. Set `ENABLE_USER_SCRIPT_SANDBOXING` to `NO` for the app target.

### Swift errors about missing Kotlin classes, or "Unsupported view kind" everywhere

The renderer and the Kotlin library come from different versions. The plugin writes the renderer of its own version
and adds the library of its own version, so this only happens when:

- `composeSwiftUI { libraryVersion = … }` is set to another version than the plugin;
- the library is also declared by hand (or pulled transitively) with a different version, and Gradle resolves the
  higher one;
- the Xcode project doesn't use `embedAndSignAppleFrameworkForXcode` (an XCFramework, CocoaPods), so the renderer
  is never re-synced after an upgrade.

`ComposeSwiftUI/README.txt` states the version of the renderer in the project. Compare it with
`./gradlew :shared:dependencyInsight --dependency compose-swiftui --configuration iosArm64CompileKlibraries`. Remove the
explicit dependency or `libraryVersion`, or run `./gradlew :shared:syncComposeSwiftUIRenderer` after upgrading.

### "Compose SwiftUI needs the Compose compiler plugin"

The plugin applies `org.jetbrains.kotlin.plugin.compose` for you, but applying it failed, typically because of a
Kotlin version conflict on the build classpath. Add `kotlin("plugin.compose")` to the module's
`plugins { }` with the same version as Kotlin.

### "Compose SwiftUI: no Apple framework binary found"

The module has no `binaries.framework { }` on an Apple target, so the plugin can't tell which framework the renderer
must import. Declare one (`iosArm64 { binaries.framework { baseName = "Shared" } }`) or set
`composeSwiftUI { frameworkName = "Shared" }`.

### The Kotlin function isn't visible from Swift

- Kotlin/Native exposes top-level functions on a class named after the file: `fun app()` in `Main.kt` is
  `MainKt.app()`. Rename the file or look for the generated header (`Shared.framework/Headers/Shared.h`).
- Expose plain functions that return `composeSwiftUI { }`, not composables. `@Composable` functions and most of the
  library are hidden from Objective-C (`@HiddenFromObjC`) and can't be called from Swift.
- The function must be in a source set compiled into the framework the app links (`commonMain`, `appleMain`,
  `iosMain`…), and must not be `internal` or `private`.

### "ComposeIntegrations has no member 'registerPhotosUI'" on some platforms

Register functions exist only on the platforms their framework supports. Wrap the call in the same `#if os(…)` as
the integration file (each file's header comment lists its platforms):

```swift
#if os(iOS) || os(macOS)
ComposeIntegrations.registerPhotosUI()
#endif
```

If the error appears on every platform, the integration file isn't in the app target.

### "AppServices.swift" or "SystemServices.swift" is missing after upgrading

These two files were split into one file per framework (`HealthKitUI.swift`, `ContactsUI.swift`, `AppIntents.swift`,
`WatchKit.swift`…), and the sync deletes the old ones. Remove the red references from the target and add the files
of the frameworks you register; the register functions keep their names. The table in
[Apple frameworks](apple-frameworks.md) maps each function to its file.

### "Unknown Compose SwiftUI integrations [...]"

A name in `composeSwiftUI { integrations.set(…) }` doesn't match an integration file. Use the file name without
`.swift` (`"MapKit"`, `"HealthKitUI"`); the error lists the available names. An integration left out of the list is
deleted from `ComposeSwiftUIIntegrations/` on the next build, so remove it from the Xcode target as well.

### Compose SwiftUI APIs are unresolved in `commonMain` of a module with Android

When the module also has non-Apple targets (Android, JVM…), the plugin adds the library to `appleMain`, not
`commonMain`, because it is only published for Apple targets. Move the screens to `appleMain` (or `iosMain`) and keep
view models and logic in `commonMain`.

## Previews

### The preview doesn't appear

The plugin finds previews by scanning the sources. A preview is found only if:

- it is annotated `@Preview` (any annotation named `Preview` works);
- the function is top-level and declared at the start of a line, not indented;
- it is not `private` and takes no parameters;
- it lives in a source set compiled into every Apple framework of the module (`commonMain`, `appleMain`, or `iosMain`
  for an iOS-only module). A preview in `iosMain` is skipped if the module also has macOS targets.

In Xcode, the previews are in `ComposeSwiftUI/ComposeSwiftUIPreviews.swift`. If you added the renderer folder as a
group before previews existed, add that file to the target once.

### "The preview host doesn't get the app's Xcode linker flags (OTHER_LDFLAGS)"

IDE previews link your framework into a small host app built by Gradle, which doesn't see the `OTHER_LDFLAGS` of your
Xcode project. A Kotlin library that needs a system library (SQLDelight needs `sqlite3`) then fails with undefined
symbols. Since 0.6.0 the host always links `sqlite3` and `zlib`; for anything else, declare the library on the
framework itself:

```kts
kotlin {
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts("-lsqlite3", "-framework", "CoreML")
        }
    }
}
```

### "Compose SwiftUI previews need the `iosSimulatorArm64()` target with a framework binary"

IDE previews render on the iOS simulator, with the debug framework of `iosSimulatorArm64` (or `iosX64` on Intel
Macs). Add that target with a `binaries.framework { }`, even if you only ship macOS. Rendering also needs Xcode and at
least one iPhone simulator installed; pick one with `composeSwiftUI { previewDevice = "iPhone 17 Pro" }`.

More in [Previews](previews.md).

## At run time

### A red "Unsupported view kind 200000" text

The view belongs to a framework integration (MapKit, Charts, StoreKit…) whose renderer isn't registered. Add the
integration file to the app target and call its register function once at launch, before the first `ComposeView`:

```swift
@main
struct MyApp: App {
    init() { ComposeIntegrations.registerMapKit() }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

Kinds from 200,000 up are integrations. A low kind number means a renderer/library version mismatch (see above).

### A modifier does nothing

In order of likelihood:

1. It belongs to an integration that isn't registered (`mapStyle`, `chartXAxis`, `storeButton`…): unregistered
   modifiers are skipped silently.
2. The running OS is older than the API. Check the availability in the function's KDoc (`iOS 26.0+`): on an older OS
   the modifier is skipped.
3. The API doesn't exist on this platform. The KDoc says `tvOS: unavailable (no-op)`.
4. It is applied in the wrong place, as it would be in Swift: `navigationTitle` and `toolbarItem` go on a view
   inside the `NavigationStack`, not on the stack; `listRowBackground` goes on a row, not on the `List`.
5. Modifier order: `Modifier.background(…).padding()` and `Modifier.padding().background(…)` differ, as in SwiftUI.

### A view is empty

A view newer than the running OS renders as `EmptyView` (for example a view marked `iOS 26.0+` on iOS 18). Check the
KDoc availability and provide a fallback for older systems.

### A red "No native view 'key'" text

A `NativeView("key")` has no matching `ComposeRegistry.nativeViews["key"]` in Swift. Register the factory at launch,
with exactly the same key, before the view is shown.

### A control doesn't react: the toggle flips back, the text field ignores typing

Bindings are controlled by Kotlin state: the control shows the value you pass and only changes when your callback
updates that value. A missing or empty callback freezes the control.

```kotlin
var on by remember { mutableStateOf(false) }
// Wrong: the toggle snaps back, `on` never changes.
Toggle("Wi-Fi", isOn = on, onIsOnChange = { })
// Right
Toggle("Wi-Fi", isOn = on, onIsOnChange = { on = it })
```

The same applies to a view model: the callback must update the `StateFlow` that the screen collects.

### List or Picker selection doesn't work

- Selection identifies rows and options by `Modifier.tag(value)`. A row without a tag can't be selected.
- The tag must be `equals` to the selection value. Use the same type for both: an `Int` tag compared with a `Long`
  selection, or a `String` with an enum, won't match.
- On iOS, use `List(selectedKey, onSelectedKeyChange)` for single selection; it selects and highlights the tagged row
  on iOS and macOS. Multiple selection, `List(selectedKeys, onSelectedKeysChange)`, works on iOS only in edit mode, as
  in SwiftUI: add an `EditButton`.
- Versions before 0.6.0 had a bug where `List(selection, onSelectionChange)` did nothing on iOS; upgrade.

```kotlin
var selected by remember { mutableStateOf<String?>(null) }
List(selectedKey = selected, onSelectedKeyChange = { selected = it as String? }) {
    Text("Inbox", modifier = Modifier.tag("inbox"))
    Text("Archive", modifier = Modifier.tag("archive"))
}
```

### A callback receives NSNull, or `== null` never matches

Before 0.6.0, callbacks that report "no value" (dismissing a sheet, clearing a selection, a JavaScript call without a
result) delivered an `NSNull` object instead of `null`: `== null` checks failed and `as String?` casts threw. Upgrade
to 0.6.0 or later. In your own Swift renderers (`NativeView` factories), pass `nil` for "no value"; the `Args`
callback helpers convert `NSNull` to `null` for you.

### "No SwiftUI Button initializer takes this combination of arguments"

You called a combined overload (all parameters nullable) with arguments that match no SwiftUI initializer, for example
a `systemImage` and an `image` together. Pass one of the combinations SwiftUI accepts, or use a specific overload. See
[API mapping](api-mapping.md).

### "No ViewModelStoreOwner: content must run inside a ComposeSwiftUIHost"

`viewModel { }` was called outside a Compose SwiftUI composition, typically inside an embedded Compose Multiplatform
screen (`ComposeUIViewController { }`), which has its own composition. Create the view model in the Compose SwiftUI
screen and pass it, or its state, to the embedded content.

### EnvironmentReader shows the wrong color scheme or size class at first

`EnvironmentReader` gets its values from SwiftUI after the first layout pass, so the first composition sees defaults
(light, large, no size class). Design the content so that the first frame is acceptable, or show nothing until the
values arrive.

### `stringResource` crashes in a Compose SwiftUI screen

Compose Multiplatform resources read Compose UI's `LocalDensity`, which a Compose SwiftUI composition doesn't
provide. Provide it once at the root:

```kotlin skip
fun app() = composeSwiftUI {
    CompositionLocalProvider(LocalDensity provides Density(UIScreen.mainScreen.scale.toFloat())) {
        Text(stringResource(Res.string.title))
    }
}
```

The suspending `getString(Res.string.title)` works without it.

### An embedded Compose Multiplatform screen stutters or doesn't render

Compose Multiplatform on iOS needs `CADisableMinimumFrameDurationOnPhone` set to `true` in the app's `Info.plist`, and
the embedded view takes the size SwiftUI proposes: give it a `Modifier.frame(height = …)` or a sizing mode. See
[Interop](interop.md).

### A long list is slow to appear or to update

A Kotlin `for` loop inside a `List` composes every row up front and keeps all of them up to date. Use
`LazyForEach(items, key = { it.id }) { … }` as the only child of the `List`. See [Performance](performance.md).

## Still stuck

- Check the KDoc of the API: it shows the exact Swift declaration it maps and its availability.
- Search the [reference](/reference.html) and
  [COVERAGE.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/COVERAGE.md): an API marked n/a has
  a reason and usually a Kotlin replacement.
- Compare with the sample app in `samples/kmp-app`, which builds from the repository sources.
- Open an issue on [GitHub](https://github.com/NucleusFramework/ComposeSwiftUI/issues) with the Xcode and Kotlin
  versions, the platform, and a minimal composable that reproduces the problem.
