<!-- group: Getting started -->
# Introduction

> Compose SwiftUI lets Kotlin Multiplatform apps write real SwiftUI screens in Kotlin, with the Compose runtime driving native SwiftUI views.

Compose SwiftUI is a Kotlin library and a Gradle plugin. Your composables are named after SwiftUI views (`Text`,
`VStack`, `List`, `NavigationStack`…) and take SwiftUI modifiers as a `Modifier` chain. They don't draw anything:
they build a tree that a Swift renderer, compiled into your app, turns into the matching SwiftUI views. What ends up
on screen is Apple's SwiftUI, laid out, animated and made accessible by the system.

It uses the Compose **runtime** only, not Compose UI. Compose HTML does the same thing for the browser DOM.

## What it looks like

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class CounterViewModel : ViewModel() {
    val count = MutableStateFlow(0)
    fun increment() = count.update { it + 1 }
}

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

The Swift side is one line, anywhere a SwiftUI view goes:

```swift
ComposeView { CounterScreenKt.counterScreen() }
```

`composeSwiftUI { }` returns a host that owns the composition and its view models. `ComposeView` is a regular SwiftUI
view: put it in a `WindowGroup`, a `TabView`, a `sheet`, or next to hand-written SwiftUI.

## Why

Kotlin Multiplatform already shares the model layer: repositories, use cases, `ViewModel`s and `StateFlow`s. The UI
on Apple platforms is then usually written twice: once in Kotlin for Android, once in Swift for iOS, with a bridge from
`StateFlow` to `ObservableObject` in between. Compose SwiftUI removes the Swift half of that work without giving up
the native UI:

- Your view models, flows and `remember` state work unchanged. `collectAsState()`, `LaunchedEffect`, `derivedStateOf`
  and the rest of the Compose runtime behave as on Android.
- The screen is written once in Kotlin, against the SwiftUI API you already know from Apple's documentation.
- No glue code: no `ObservableObject` wrappers or Swift mirrors of Kotlin state for the UI layer.

## What you get

- **Real SwiftUI views.** `List` is SwiftUI's `List`, `TextField` is a native text field, `NavigationStack` pushes
  with the system animation and swipe back, `sheet` is a system sheet. Liquid Glass, Dynamic Type, Dark Mode,
  VoiceOver, keyboard navigation and the platform look come from SwiftUI itself.
- **The whole SwiftUI API.** The Kotlin API is generated from the iOS, macOS, tvOS and watchOS SDK symbol graphs:
  every view, modifier and value that builds UI has a Kotlin form, with its Swift declaration and availability in the
  KDoc. Features that need Swift closures or property wrappers (custom layouts, `Canvas`, gestures, `TimelineView`,
  custom styles…) have hand-written Kotlin APIs. [docs/COVERAGE.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/COVERAGE.md)
  classifies every entry of Apple's SwiftUI documentation.
- **Apple frameworks.** MapKit, Swift Charts, StoreKit, PhotosUI, WebKit, AVKit, Sign in with Apple, Apple Pay,
  TipKit, WidgetKit and more, through opt-in Swift renderer files (see [Apple frameworks](apple-frameworks.md)).
- **Previews.** Annotate a composable with `@Preview` and see it rendered by SwiftUI in IntelliJ IDEA / Android
  Studio, in Xcode's canvas, or in a gallery inside your app (see [Previews](previews.md)).
- **Interop both ways.** Embed Swift views, UIKit / AppKit views and Compose Multiplatform screens in a Kotlin screen,
  and pass values from Swift into Kotlin (see [Interop](interop.md)).

## Platforms

| Platform | Minimum OS | Kotlin targets |
|---|---|---|
| iOS / iPadOS | 17 | `iosArm64`, `iosSimulatorArm64`, `iosX64` |
| macOS | 14 | `macosArm64`, `macosX64` |
| tvOS | 17 | `tvosArm64`, `tvosSimulatorArm64` |
| watchOS | 10 | `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64` |

Building requires Xcode 27.2 (Swift 6.4), because the renderer calls the newest SwiftUI APIs behind `#available`
checks. The app still runs on the minimum OS versions above: an API newer than the device's OS is skipped at run time
(a modifier does nothing, a view renders as an empty view). The KDoc of each generated function states its
availability per platform.

visionOS is not supported: Kotlin/Native has no visionOS target. See [Limitations](limitations.md).

## Compared with Compose Multiplatform

Compose Multiplatform (CMP) and Compose SwiftUI share the Compose runtime but make opposite choices for the UI layer.

| | Compose Multiplatform on iOS | Compose SwiftUI |
|---|---|---|
| Rendering | Compose UI, drawn with Skia into one Metal view | Native SwiftUI views |
| UI API | Compose UI (`Column`, `LazyColumn`, Material 3) | SwiftUI (`VStack`, `List`, `NavigationStack`) |
| Shared with Android | Runtime, state and UI code | Runtime and state; UI code is Apple-only |
| Look and behavior | Reimplemented; close to native where the library does the work | The system's own controls, animations and accessibility |
| New OS designs (Liquid Glass…) | When the library reimplements them | As soon as SwiftUI ships them |
| Apple frameworks (MapKit, StoreKit…) | Through UIKit interop | SwiftUI APIs of the framework, from Kotlin |
| Platforms | Android, iOS, desktop, web | iOS, macOS, tvOS, watchOS |

The honest trade-offs:

- **UI code is not shared with Android.** You share view models, state and logic; the Apple screens are written
  against SwiftUI's API, the Android ones against Compose UI. If one UI codebase for every platform is the goal, CMP
  is the better fit.
- **Performance is close to plain SwiftUI, not identical.** Updates are as fast or faster; mounting a screen costs
  8% to 18% more, and long lists need `LazyForEach`. See [Performance](performance.md).
- **The build needs a recent Xcode**, and the app has a Swift side: the renderer files, `ComposeView`, and the few
  things Apple only lets you declare in Swift (the `@main` app, widget entry points, App Intents).
- **You can mix both.** A Compose SwiftUI screen can embed a CMP screen through its view controller, for example to
  reuse a Compose UI chart (see [Interop](interop.md)).

## How this guide is organized

**Getting started.** [Installation](installation.md) sets up the Gradle plugin and Xcode.
[Your first app](first-app.md) builds a small task list with a view model, navigation and a sheet.
[How it works](how-it-works.md) explains the node tree, the Swift renderer and recomposition.
[API mapping](api-mapping.md) gives the rules that turn any SwiftUI API into its Kotlin form.

**Building screens.** [Layout](layout.md), [Text and images](text-and-images.md), [Controls](controls.md),
[State and data](state-and-data.md), [Lists](lists.md), [Navigation](navigation.md),
[Presentation](presentation.md), [Styling](styling.md), [Drawing and animation](drawing-and-animation.md),
[Gestures and input](gestures-and-input.md), [Accessibility](accessibility.md).

**Apple frameworks.** [Overview](apple-frameworks.md), [Maps](maps.md), [Charts](charts.md),
[In-app purchases](in-app-purchases.md), [Photos and media](photos-and-media.md),
[Web and sign-in](web-and-sign-in.md), [Payments and Wallet](payments-and-wallet.md),
[Tips and widgets](tips-and-widgets.md), [System services](system-services.md), [Games and 3D](games-and-3d.md).

**Platforms and tools.** [Platforms](platforms.md), [Interop](interop.md), [Previews](previews.md),
[Performance](performance.md), [Troubleshooting](troubleshooting.md), [Limitations](limitations.md).

For a single API, the [reference](/reference.html) has a page per SwiftUI symbol with its Kotlin form, and
[Kotlin APIs](/kotlin-apis.html) lists the hand-written ones.
