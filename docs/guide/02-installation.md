<!-- group: Getting started -->
# Installation

> Apply the Gradle plugin to your shared KMP module, add the generated Swift renderer to your Xcode target once, and show your first Kotlin screen with ComposeView.

Compose SwiftUI has two halves: a Kotlin library (`dev.nucleusframework:compose-swiftui`) compiled into your shared
framework, and a Swift renderer compiled into your app. The Gradle plugin `dev.nucleusframework.compose-swiftui` sets
up the first and writes the second into your Xcode project on every build, so both always come from the same version.

## Requirements

| Tool | Minimum |
|---|---|
| Xcode | 27.2 (Swift 6.4) |
| Kotlin | 2.4.20 |
| JDK | 17 |
| Gradle project | Kotlin Multiplatform with at least one Apple framework binary |
| Deployment target | iOS 17, macOS 14, tvOS 17, watchOS 10 |

Xcode 27.2 is needed to build because the renderer references the newest SwiftUI APIs behind `#available` checks. It
does not raise your deployment target.

## Gradle setup

Make sure the Gradle Plugin Portal and Maven Central are in your repositories (the plugin comes from the Portal, the
library from Maven Central):

```kts
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

Apply the plugin in the shared module and declare framework binaries for the Apple targets you ship:

```kts
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.4.20"
    id("dev.nucleusframework.compose-swiftui") version "0.6.1"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
```

The plugin does three things when the Kotlin Multiplatform plugin is present:

1. Adds `dev.nucleusframework:compose-swiftui` (same version as the plugin) as an `api` dependency of `commonMain`, or
   of `appleMain` when the module also has non-Apple targets (Android, JVM…). It brings the Compose runtime, `kotlinx-coroutines-core` and `androidx.lifecycle:lifecycle-viewmodel` with it.
2. Applies the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`). You don't need to declare it; if you
   do, use the same version as Kotlin.
3. Exports the library from every Apple framework binary, so the Swift renderer can see its classes through your
   framework. You don't need `export(...)` yourself.

Screens are plain Kotlin, so they can live in `commonMain` (in `appleMain` if the module also targets Android or the
JVM). Code that touches UIKit or AppKit (`Image(uiImage)`,
`UIKitView`) goes in `iosMain` / `macosMain`.

### More platforms

Add the targets you need; each Apple framework binary gets the library exported:

```kts
kotlin {
    listOf(
        iosArm64(), iosSimulatorArm64(),
        macosArm64(),
        tvosArm64(), tvosSimulatorArm64(),
        watchosArm64(), watchosDeviceArm64(), watchosSimulatorArm64(),
    ).forEach {
        it.binaries.framework { baseName = "Shared" }
    }
}
```

The library is published for `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`, `macosX64`, `tvosArm64`,
`tvosSimulatorArm64`, `watchosArm64`, `watchosDeviceArm64` and `watchosSimulatorArm64` only.

### A shared module with Android or JVM targets

You can apply the plugin to a shared module that also targets Android, the JVM or the web. The plugin then adds the
library to `appleMain` instead of `commonMain` (to each Apple target's main source set if you disabled the default
hierarchy template), so the other targets still compile. Write the Compose SwiftUI screens in `appleMain` (or
`iosMain`…); `commonMain` keeps your view models and domain code, shared with Android:

```kts
kotlin {
    androidTarget()
    iosArm64(); iosSimulatorArm64()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "Shared" } }
}
```

A separate Apple-only module that depends on the shared one and produces the framework still works, if you prefer to
keep the SwiftUI screens out of the shared module.

### Plugin options

Everything is optional:

```kts
composeSwiftUI {
    // Where the Swift renderer is written. Default: $SRCROOT/ComposeSwiftUI when Gradle runs from an
    // Xcode build phase, otherwise <root>/iosApp/ComposeSwiftUI.
    swiftOutputDir = layout.projectDirectory.dir("../iosApp/ComposeSwiftUI")
    // Kotlin framework imported by the renderer. Default: the baseName of the first Apple framework.
    frameworkName = "Shared"
    // Library version added to commonMain (appleMain when the module also has non-Apple targets).
    // Default: the plugin version. Keep them equal.
    libraryVersion = "0.6.1"
    // Integration files written to ComposeSwiftUIIntegrations, by name without .swift. Default: all of them.
    integrations.set(listOf("MapKit", "Charts", "StoreKit"))
    // Simulator used to render previews in the IDE. Default: the booted one, else the first iPhone.
    previewDevice = "iPhone 17 Pro"
}
```

## What the plugin generates

The plugin registers these tasks in the `compose swiftui` group:

| Task | What it does |
|---|---|
| `syncComposeSwiftUIRenderer` | Writes the Swift renderer into `swiftOutputDir` and the integrations next to it |
| `generateComposeSwiftUIPreviews` | Scans the sources for `@Preview` functions and generates their Kotlin and Swift entry points |
| `renderComposeSwiftUIPreviews` | Renders the previews to PNG on the simulator (used by the IDE plugin) |

`syncComposeSwiftUIRenderer` runs automatically before `embedAndSign…AppleFrameworkForXcode` (and
`embedSwiftExportForXcode`), the task of the standard KMP Xcode build phase. After a build you get:

```bash
iosApp/
  ComposeSwiftUI/                  # rewritten on every build: don't edit
    Core.swift, Args.swift, Handwritten.swift, Interactive.swift, Advanced.swift, Completions.swift
    Generated/                     # GenViews.swift, GenModifiers*.swift, GenDecoders*.swift, GenEncoders.swift…
    KotlinModule.swift             # @_exported import Shared
    ComposeSwiftUIPreviews.swift   # one #Preview per @Preview function, and ComposeSwiftUIPreviewGallery
    README.txt
  ComposeSwiftUIIntegrations/      # opt-in framework renderers (all, or the `integrations` list)
    MapKit.swift, Charts.swift, StoreKit.swift, PhotosUI.swift, WebKit.swift, AVKit.swift, …
```

`KotlinModule.swift` re-exports your framework, so every Swift file of the app target sees the Kotlin classes without
an `import Shared`. Files of an older renderer version are deleted from `ComposeSwiftUI/` on sync; nothing outside
that folder and `ComposeSwiftUIIntegrations/` is touched.

If your Xcode project doesn't use `embedAndSignAppleFrameworkForXcode` (for example an XCFramework built by
`assembleXCFramework`, or CocoaPods), run the sync yourself whenever you upgrade:

```bash
./gradlew :shared:syncComposeSwiftUIRenderer
```

## Xcode setup

These steps are done once per app target.

### 1. Build the Kotlin framework from Xcode

If the project was created by the KMP wizard, this is already there. Otherwise add a *Run Script* build phase before
*Compile Sources*:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

In the target's build settings:

| Setting | Value |
|---|---|
| `ENABLE_USER_SCRIPT_SANDBOXING` | `NO` (the build phase writes the renderer into the project) |
| `FRAMEWORK_SEARCH_PATHS` | `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` |
| `OTHER_LDFLAGS` | `-framework Shared` |

### 2. Add the renderer folder

Build once (⌘B) so the plugin writes `ComposeSwiftUI/`. Then drag the `ComposeSwiftUI` folder into the Xcode project navigator and add it to the app target.

Add it as a **folder** (a synchronized folder, the default in Xcode 16 and later), not as a group: new renderer files
added by a later version are then picked up automatically. With a group, you must add new files by hand when you
upgrade (see Upgrading below).

The sample app compiles the renderer in the Swift 5 language mode (`SWIFT_VERSION = 5.0`), as do the preview tools.

### 3. Show a Kotlin screen

Write a top-level function that returns `composeSwiftUI { }`:

```kotlin
// shared/src/commonMain/kotlin/Hello.kt
fun helloScreen() = composeSwiftUI {
    Text("Hello from Kotlin", modifier = Modifier.padding())
}
```

Kotlin/Native exposes top-level functions of `Hello.kt` on a `HelloKt` class. Show it from Swift:

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView { HelloKt.helloScreen() }
        }
    }
}
```

`ComposeView` takes a closure rather than a host so that the host is created once, when SwiftUI first creates the
view's state, and disposed (with its view models) when the view goes away. Don't create the host outside and pass it
in: each `ComposeView` must own its own.

To pass values from Swift into the composition, use `ComposeView(input:)`; every new value recomposes the screen:

```swift
ComposeView(input: userId) { ProfileKt.profileScreen() }
```

```kotlin
fun profileScreen() = composeSwiftUI {
    val userId: String = hostInput()
    Text("User $userId")
}
```

## Registering integrations

The SwiftUI APIs of other Apple frameworks (MapKit, Swift Charts, StoreKit, PhotosUI, WebKit…) are always present in
the Kotlin library, but their renderers are opt-in, so your app only links the frameworks it uses. For each framework:

1. Add its file from `ComposeSwiftUIIntegrations/` to the app target. With the default settings the folder holds every
   integration, so add only the files you use; if you set `integrations` in the `composeSwiftUI { }` block, the folder
   holds only those and you can add the whole folder.
2. Call its register function once at launch, before the first `ComposeView` is shown.

```swift
@main
struct MyApp: App {
    init() {
        ComposeIntegrations.registerMapKit()     // MapKit.swift
        ComposeIntegrations.registerCharts()     // Charts.swift
        ComposeIntegrations.registerStoreKit()   // StoreKit.swift
        #if os(iOS) || os(macOS)
        ComposeIntegrations.registerPhotosUI()   // PhotosUI.swift
        #endif
    }

    var body: some Scene {
        WindowGroup { ComposeView { MainKt.app() } }
    }
}
```

A register function only exists on the platforms its framework supports, so wrap the call in the same `#if os(…)` as
the file (each file's header comment says which platforms it covers). There is one file per framework
(`HealthKitUI.swift` for `registerHealthKitUI()`, `AppIntents.swift` for `registerAppIntents()`…); `MusicKit.swift`
also holds DataDetection.

If you forget to register, the framework's views render as a red "Unsupported view kind" text and its modifiers do
nothing. The full list of files and functions is in [Apple frameworks](apple-frameworks.md).

## Trying the sample

`samples/kmp-app` builds the library and the plugin from the repository sources. Open
`samples/kmp-app/iosApp/iosApp.xcodeproj` and run: it shows Navigation 3, a `ViewModel`, an embedded Compose
Multiplatform screen, Compose Multiplatform string resources and the preview gallery. Its `iosApp/project.yml`
(XcodeGen) is a compact reference for every build setting above.

## Upgrading

1. Bump the plugin version. The library follows automatically (unless you set `libraryVersion`, which you should then
   bump too: the Swift renderer and the Kotlin library must come from the same release).
2. Build in Xcode. The build phase rewrites `ComposeSwiftUI/` and `ComposeSwiftUIIntegrations/`.
3. If you added `ComposeSwiftUI` as a group rather than a folder, add any new file to the app target. Read the
   CHANGELOG for the release; for example, upgrading past 0.4.1 added `Interactive.swift`, `Advanced.swift`,
   `Completions.swift` and `Generated/GenEncoders.swift`, and previews added `ComposeSwiftUIPreviews.swift`.
4. Since 0.5.0 the integrations are opt-in: if you upgrade from an older version and use maps, charts or StoreKit, add
   their files and register them.
5. In 0.6.1, `AppServices.swift` and `SystemServices.swift` were split into one file per framework (`HealthKitUI.swift`,
   `AppIntents.swift`…). The sync deletes the old files: add the new files you use to the target. The register
   functions are unchanged.

Read the "Changed" sections of the CHANGELOG for source changes, such as the `SFSymbol` type for `systemImage`
parameters in 0.4.0 or the trailing lambda binding the main closure in 0.5.0.

Next: [build your first app](first-app.md).
