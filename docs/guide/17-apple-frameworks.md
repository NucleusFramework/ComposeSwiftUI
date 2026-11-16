<!-- group: Apple frameworks -->
# Apple framework integrations

> MapKit, Swift Charts, StoreKit and the other SwiftUI framework APIs are opt-in: add one Swift file per framework to your target and register it at launch.

SwiftUI is more than the `SwiftUI` module. `Map` lives in MapKit, `Chart` in Swift Charts, `SubscriptionStoreView` in
StoreKit, `PhotosPicker` in PhotosUI, and so on. Compose SwiftUI exposes these APIs to Kotlin like the rest of SwiftUI,
but their Swift renderers are not part of the core renderer. Each framework has its own Swift file that you add to your
app only when you use it.

This chapter explains how that works and lists every integration. The big ones have their own chapters:
[Maps](maps.md), [Charts](charts.md) and [In-app purchases](in-app-purchases.md).

## Why the integrations are opt-in

The Kotlin side costs nothing: `Map`, `Chart`, `ProductView` and the rest are always in the runtime library, for every
Apple platform. They only build a node tree, like any other composable.

The Swift side is different. A Swift file that does `import HealthKitUI` or `import FamilyControls` links that framework
into your app, and that has consequences:

- Some frameworks need an entitlement or a capability (HealthKit, Family Controls, Apple Pay, Sign in with Apple), and
  the build or App Review fails without it.
- Some frameworks trigger App Store questions or privacy manifest requirements just by being linked.
- Every framework adds launch-time and binary-size cost.
- Many exist on one platform only, or only on a recent OS.

So the core renderer imports only SwiftUI, and each framework renderer is a separate file. Your app links exactly the
frameworks it uses.

## How it works

Every framework composable emits a view kind (or a modifier id) that the core renderer does not know. A framework file
adds a factory for it to `ComposeRegistry` when you call its register function:

```swift
public extension ComposeIntegrations {
    @MainActor static func registerMapKit() {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return }
        ComposeRegistry.views[200_000] = { m in ModChain.apply(KotlinMap(m: m), m, 0) }
        // …
    }
}
```

If you forget to register a framework:

- a framework **view** (`Map`, `Chart`, `ProductView`…) renders as a red `Unsupported view kind 200000` text, which is
  easy to spot;
- a framework **modifier** (`Modifier.mapStyle`, `Modifier.chartLegend`, `Modifier.onInAppPurchase`…) is silently
  ignored. If a framework modifier seems to do nothing, check the registration first.

## Setting up an integration

1. Build the app once. Next to the renderer folder (`iosApp/ComposeSwiftUI`), the Gradle plugin writes
   `iosApp/ComposeSwiftUIIntegrations`, one Swift file per framework. If you changed `swiftOutputDir`, the folder is
   its sibling. Like the renderer, it is rewritten on every build: don't edit the files.
2. Add the files you need to your app target in Xcode (add them as references, without copying, so updates reach
   you). Don't add the whole folder: you would link every framework.
3. Call the register function of each file once at launch, before the first `ComposeView` is shown. `App.init` is the
   natural place.

```swift
import SwiftUI

@main
struct MyApp: App {
    init() {
        ComposeIntegrations.registerMapKit()     // MapKit.swift
        ComposeIntegrations.registerCharts()     // Charts.swift
        ComposeIntegrations.registerStoreKit()   // StoreKit.swift
    }

    var body: some Scene {
        WindowGroup { ComposeView { MainKt.app() } }
    }
}
```

Registering is cheap (it fills a dictionary) and idempotent, but there is no reason to call it more than once.

### Platforms and OS versions

A register function exists only on the platforms its framework supports: the files are wrapped in `#if os(…)` (or
`#if canImport(…)` for frameworks that only ship in recent SDKs). In a multiplatform target, call it under the same
condition, or the Swift build fails on the other platforms:

```swift
init() {
    ComposeIntegrations.registerMapKit()          // everywhere
    #if os(iOS)
    ComposeIntegrations.registerPassKit()         // iOS only
    ComposeIntegrations.registerFamilyControls()
    #endif
    #if os(iOS) || os(macOS)
    ComposeIntegrations.registerPhotosUI()
    ComposeIntegrations.registerTranslation()
    #endif
}
```

Inside a platform, OS versions are handled for you: each register function starts with a `guard #available(…)` and
registers newer APIs only on the OS versions that have them. On an older OS, a newer view renders as unsupported and a
newer modifier is ignored, so guard the Kotlin side when it matters (the Kotlin KDoc and the tables below give the
versions).

Kotlin code is shared across platforms, so a composable may call a framework API whose Swift renderer doesn't exist on
the current platform (a `MapPitchSlider` on iOS, for example). Platform-specific pieces inside a framework degrade to
an empty view or a no-op, as noted in each chapter.

### Several targets

Each target that shows Compose SwiftUI content registers what it uses. A widget extension, for example, adds
`WidgetKit.swift` (and `Charts.swift` if its widgets draw charts) to the extension target and registers in its own
entry point. See [Tips and widgets](tips-and-widgets.md).

## All integrations

| File | Register function | Platforms | Main Kotlin APIs | Chapter |
|---|---|---|---|---|
| `MapKit.swift` | `registerMapKit()` | iOS 17, macOS 14, tvOS 17, watchOS 10 | `Map`, `Marker`, `Annotation`, `MapCircle`, `MapPolyline`, `MapPolygon`, map controls, `MapReader`, `LookAroundPreview` | [Maps](maps.md) |
| `Charts.swift` | `registerCharts()` | iOS 17, macOS 14, tvOS 17, watchOS 10 (3D charts: iOS 26, macOS 26) | `Chart`, marks, `ChartPlot`, `FunctionPlot`, `Chart3D`, `chart*` modifiers | [Charts](charts.md) |
| `StoreKit.swift` | `registerStoreKit()` | iOS 17, macOS 14 (full); tvOS 17, watchOS 10 (store views only) | `ProductView`, `StoreView`, `SubscriptionStoreView`, `SubscriptionOfferView`, store modifiers and sheets | [In-app purchases](in-app-purchases.md) |
| `PhotosUI.swift` | `registerPhotosUI()` | iOS 17, macOS 14 | `PhotosPicker`, `Modifier.photosPicker`, picker styles | [Photos and media](photos-and-media.md) |
| `AVKit.swift` | `registerAVKit()` | all (QuickLook: iOS, macOS; camera capture events: iOS 26; continuity device picker: tvOS 17) | `VideoPlayer`, `quickLookPreview`, `onCameraCaptureEvent` | [Photos and media](photos-and-media.md) |
| `MusicKit.swift` | `registerMusicKit()` | iOS | `musicSubscriptionOffer`, `musicPicker` | [Photos and media](photos-and-media.md) |
| `MusicKit.swift` | `registerDataDetection()` | iOS 27 | `Modifier.dataDetection` | [System services](system-services.md) |
| `WebKit.swift` | `registerWebKit()` | iOS 26, macOS 26 | `WebView`, `WebViewController`, `Modifier.webViewStyle` | [Web and sign-in](web-and-sign-in.md) |
| `AuthenticationServices.swift` | `registerAuthenticationServices()` | all (web authentication: not tvOS; `LocationButton`: iOS, watchOS; `LocalAuthenticationView`: macOS 15) | `SignInWithAppleButton`, passkey and password requests, web authentication sessions, `LocationButton` | [Web and sign-in](web-and-sign-in.md) |
| `PassKit.swift` | `registerPassKit()` | iOS 16 | `PayWithApplePayButton`, `AddPassToWalletButton`, Pay Later, identity verification | [Payments and Wallet](payments-and-wallet.md) |
| `AppServices.swift` | `registerFinanceKitUI()` | iOS 17.4 | `AddOrderToWalletButton`, `TransactionPicker` | [Payments and Wallet](payments-and-wallet.md) |
| `TipKit.swift` | `registerTipKit(_ options:)` | iOS 17, macOS 14, tvOS 17, watchOS 10 | `TipView`, `Modifier.popoverTip`, `Modifier.tipStyle`, `Modifier.tipAnchor` | [Tips and widgets](tips-and-widgets.md) |
| `WidgetKit.swift` | `registerWidgetKit()` | iOS, macOS, watchOS | `Modifier.widgetStyle`, `widgetLabel`, `activityStyle`, `AccessoryWidgetGroup`, `WidgetImage`, `ComposeTimelineProvider` | [Tips and widgets](tips-and-widgets.md) |
| `Translation.swift` | `registerTranslation()` | iOS 18, macOS 15 | `translationPresentation`, `translationTask` | [System services](system-services.md) |
| `SystemServices.swift` | `registerAppIntents()` | iOS, macOS, watchOS | `ShortcutsLink` and the App Intents views | [System services](system-services.md) |
| `SystemServices.swift` | `registerDeviceActivity()` | iOS | `DeviceActivityReport` | [System services](system-services.md) |
| `SystemServices.swift` | `registerWatchKit()` | watchOS | `NowPlayingView` | [System services](system-services.md) |
| `SystemServices.swift` | `registerManagedAppDistribution()` | macOS 26.4 | `ManagedContentView` | [System services](system-services.md) |
| `SystemServices.swift` | `registerPermissionKit()` | iOS 26, macOS 26 | `CommunicationLimitsButton` | [System services](system-services.md) |
| `SystemServices.swift` | `registerScreenCaptureKit()` | iOS 27, macOS 27, tvOS 27 (SDKs that ship it) | `Modifier.recordingEditor` | [System services](system-services.md) |
| `SystemServices.swift` | `registerWorkoutKit()` | iOS 17, watchOS 11 | `Modifier.workoutPreview` | [System services](system-services.md) |
| `SystemServices.swift` | `registerDeviceDiscoveryUI()` | tvOS 16 | `DevicePicker` | [System services](system-services.md) |
| `AppServices.swift` | `registerHealthKitUI()` | iOS 17, watchOS 10.2 | `Modifier.healthDataAccessRequest` | [System services](system-services.md) |
| `AppServices.swift` | `registerContactsUI()` | iOS 18 | `ContactAccessButton` | [System services](system-services.md) |
| `AppServices.swift` | `registerFamilyControls()` | iOS 16 | `FamilyActivityPicker` | [System services](system-services.md) |
| `AppServices.swift` | `registerAutomatedDeviceEnrollment()` | iOS 18 | `Modifier.automatedDeviceEnrollmentAddition` | [System services](system-services.md) |
| `AppServices.swift` | `registerImagePlayground()` | iOS 18.1, macOS 15.1 | `Modifier.imagePlaygroundSheet` | [System services](system-services.md) |
| `AppServices.swift` | `registerJournalingSuggestions()` | iOS 17.2 (SDKs that ship it) | `JournalingSuggestionsPicker` | [System services](system-services.md) |
| `AppServices.swift` | `registerClassKitUI()` | iOS 27, macOS 27 | `AssignedDocumentLabel` | [System services](system-services.md) |
| `AppServices.swift` | `registerSecurityUI()` | iOS 27, macOS 27, tvOS 27, watchOS 27 (SDKs that ship it) | `Modifier.certificateSheet` | [System services](system-services.md) |
| `AppServices.swift` | `registerSpatialAnchors()` | iOS 27.2 (SDKs that ship it) | `Modifier.spatialAnchors` (location heading, device motion, Nearby Interaction) | [System services](system-services.md) |
| `AppServices.swift` | `registerGameSave()` | iOS 27, macOS 27 | `Modifier.gameSaveSyncingAlert` | [Games and 3D](games-and-3d.md) |
| `SpriteKit.swift` | `registerSpriteKit()` | all (game controller events: iOS 26) | `SpriteView`, `SceneView`, `handlesGameControllerEvents` | [Games and 3D](games-and-3d.md) |
| `RealityKit.swift` | `registerRealityKit()` | iOS 18, macOS 15 | `RealityView` with bundled models, `Modifier.realityViewStyle` | [Games and 3D](games-and-3d.md) |

The versions are the minimum for the register function to do anything. Individual APIs can need more: the chapters and
the KDoc give them. The site pages [/mapkit.html](/mapkit.html), [/charts.html](/charts.html),
[/storekit.html](/storekit.html), [/frameworks.html](/frameworks.html) and [/app-frameworks.html](/app-frameworks.html)
list every API.

### Files with several frameworks

`SystemServices.swift` and `AppServices.swift` group many small frameworks, and `MusicKit.swift` holds MusicKit and
DataDetection. Each framework is a separate `#if` section with its own register function, but the whole file is
compiled, so it imports every framework it contains. Add the file, then delete the sections of the frameworks you don't
use, or Xcode links them anyway. Since the plugin rewrites the file on every build, copy it into your project (under
another name) instead of editing the generated one.

## What stays in Swift

The integrations cover views and modifiers. Everything Apple requires you to declare at the project level stays in
Xcode, and some framework services are not exposed to Kotlin at all.

### Capabilities and entitlements

Turn them on in the target's **Signing & Capabilities** tab. Without them, the views render but the system refuses the
request at runtime (or the app fails to sign).

| Integration | Capability |
|---|---|
| StoreKit | In-App Purchase |
| PassKit (Apple Pay) | Apple Pay, with your merchant identifiers |
| PassKit (Wallet passes) | Wallet, with your pass type identifiers |
| AuthenticationServices | Sign in with Apple; Associated Domains (`webcredentials:`) for passkeys and passwords |
| HealthKitUI | HealthKit |
| FamilyControls, DeviceActivity | Family Controls |
| FinanceKitUI | FinanceKit (requires an Apple-approved entitlement) |
| MusicKit | MusicKit (App Service, enabled on the App ID) |

### Info.plist usage descriptions

The system shows these strings in its permission prompts and terminates the app when one is missing:

| Key | Needed for |
|---|---|
| `NSLocationWhenInUseUsageDescription` | the user location on a `Map` (`UserAnnotation`, `MapUserLocationButton`, `MapCameraPosition.userLocation`), `Modifier.spatialAnchors` |
| `NSHealthShareUsageDescription`, `NSHealthUpdateUsageDescription` | `Modifier.healthDataAccessRequest` |
| `NSContactsUsageDescription` | `ContactAccessButton` |
| `NSAppleMusicUsageDescription` | `musicPicker`, `musicSubscriptionOffer` |
| `NSCameraUsageDescription` | the camera session behind `Modifier.onCameraCaptureEvent` |
| `NSMotionUsageDescription` | device motion anchors |
| `NSNearbyInteractionUsageDescription` | Nearby Interaction anchors |

`PhotosPicker` runs out of process and needs no photo library permission.

### Other Swift-side setup

- **Location authorization.** MapKit shows the user location only once the app is authorized. Request it with a
  `CLLocationManager` in Swift (`requestWhenInUseAuthorization()`), or use `LocationButton` from Kotlin for one-time
  access.
- **StoreKit testing.** StoreKit configuration files, the scheme's StoreKit settings and transaction finishing are
  Swift and Xcode work. See [In-app purchases](in-app-purchases.md).
- **TipKit configuration.** `registerTipKit(_:)` runs `Tips.configure(options)`: pass the `Tips.ConfigurationOption`s
  (display frequency, datastore location) from Swift.
- **Widget entry points.** WidgetKit requires a Swift `@main` widget. Its timeline and views can come from Kotlin
  through `ComposeTimelineProvider` and `ComposeWidgetView`.
- **Framework objects.** Swift-only types don't cross the bridge: map items are coordinates and names, chart values are
  strings, numbers or `ChartDate`s, products are App Store Connect ids, and transactions arrive as Kotlin data classes.
  When you need the full framework (an `MKLocalSearch`, a `Product` with all its fields, an `HKHealthStore`), write that
  code in Swift and hand the results to Kotlin. See [Interop](interop.md).

## Checklist

When a framework view or modifier doesn't show up:

1. Is the file added to the target that runs the code (app, extension, widget)?
2. Is the register function called before the first `ComposeView` is created?
3. Is the platform and OS version supported (see the table above and the KDoc)?
4. For a permission-based API: is the capability enabled and the Info.plist key present?
