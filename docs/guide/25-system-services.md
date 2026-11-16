<!-- group: Apple frameworks -->
# System services

> Health data access, contacts, Screen Time, Image Playground, journaling suggestions, game saves, schoolwork, certificates, spatial anchors and the other system framework views, from Kotlin.

This chapter covers the smaller framework integrations: the system services whose SwiftUI views are mostly one button,
picker or sheet. They live in two files:

- `AppServices.swift`: HealthKitUI, ContactsUI, FamilyControls, ImagePlayground, JournalingSuggestions, GameSave,
  ClassKitUI, SecurityUI, AutomatedDeviceEnrollment and the spatial anchors of CoreLocation, CoreMotion and
  NearbyInteraction (plus FinanceKitUI, covered in [Payments and Wallet](payments-and-wallet.md)).
- `SystemServices.swift`: AppIntents, DeviceActivity, WatchKit, ManagedAppDistribution, PermissionKit,
  ScreenCaptureKit, WorkoutKit and DeviceDiscoveryUI.

Each framework has its own register function, and each file is split into `#if os(…)` / `#if canImport(…)` sections.
Every `import` in a file you add links that framework into the app. Linking frameworks you don't use (HealthKit,
FamilyControls…) costs launch time and can draw questions in App Review, so trim the file to the sections you need.
The Gradle plugin rewrites `ComposeSwiftUIIntegrations` on every build, so don't edit the file in place: copy it to your
own sources, delete the unused sections there, and add the copy to the target instead of the original. Then register
only what you use:

```swift
@main
struct MyApp: App {
    init() {
        #if os(iOS)
        ComposeIntegrations.registerHealthKitUI()
        ComposeIntegrations.registerContactsUI()
        ComposeIntegrations.registerFamilyControls()
        #endif
        #if os(iOS) || os(macOS)
        ComposeIntegrations.registerImagePlayground()
        #endif
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

| Register | Kotlin APIs | Platforms |
|---|---|---|
| `registerHealthKitUI()` | `Modifier.healthDataAccessRequest` | iOS 17, watchOS 10.2 |
| `registerContactsUI()` | `ContactAccessButton`, `Modifier.contactAccessButtonStyle`, `Modifier.contactAccessPicker` | iOS 18 |
| `registerFamilyControls()` | `FamilyActivityPicker`, `Modifier.familyActivityPicker`, `FamilyActivityLabel` | iOS 16 |
| `registerImagePlayground()` | `Modifier.imagePlaygroundSheet`, `Modifier.imagePlaygroundStyle` | iOS 18.1, macOS 15.1 |
| `registerJournalingSuggestions()` | `JournalingSuggestionsPicker`, `Modifier.journalingSuggestionsPicker` | iOS 17.2, devices only |
| `registerGameSave()` | `Modifier.gameSaveSyncingAlert` | iOS 27, macOS 27 |
| `registerClassKitUI()` | `AssignedDocumentLabel`, `AssignedDocumentSubmissionButton`, `Modifier.onAssignedDocument` | iOS 27, macOS 27 |
| `registerSecurityUI()` | `Modifier.certificateSheet` | iOS 27, macOS 27, tvOS 27, watchOS 27 |
| `registerAutomatedDeviceEnrollment()` | `Modifier.automatedDeviceEnrollmentAddition` | iOS 18 |
| `registerSpatialAnchors()` | `Modifier.spatialAnchors` | iOS 27.2, devices |
| `registerAppIntents()` | `ShortcutsLink`, `Modifier.shortcutsStyle` | iOS, macOS, watchOS, visionOS |
| `registerDeviceActivity()` | `DeviceActivityReport` | iOS |
| `registerWatchKit()` | `NowPlayingView` | watchOS |
| `registerManagedAppDistribution()` | `ManagedContentView`, `Modifier.managedContentStyle` | macOS 26.4 |
| `registerPermissionKit()` | `CommunicationLimitsButton` | iOS 26, macOS 26 |
| `registerScreenCaptureKit()` | `Modifier.recordingEditor` | iOS 27, macOS 27, tvOS 27 |
| `registerWorkoutKit()` | `Modifier.workoutPreview` | iOS 17, watchOS 11 |
| `registerDeviceDiscoveryUI()` | `DevicePicker` | tvOS 16 |

## HealthKitUI

`Modifier.healthDataAccessRequest` asks for HealthKit authorization. Types are given by their identifier strings, the
raw values of `HKQuantityTypeIdentifier`, `HKCategoryTypeIdentifier`, `HKCharacteristicTypeIdentifier`,
`HKCorrelationTypeIdentifier`, or `HKWorkoutTypeIdentifier`:

```kotlin
var askHealth by remember { mutableStateOf(false) }
var status by remember { mutableStateOf("Not connected") }

VStack(
    modifier = Modifier.healthDataAccessRequest(
        readTypes = listOf("HKQuantityTypeIdentifierStepCount", "HKCategoryTypeIdentifierSleepAnalysis", "HKWorkoutTypeIdentifier"),
        shareTypes = listOf("HKQuantityTypeIdentifierBodyMass"),
        trigger = askHealth,
    ) { error -> status = error ?: "Connected" },
) {
    Text(status)
    Button("Connect Apple Health") { askHealth = !askHealth }
}
```

The request runs each time `trigger` changes. `onResult` receives `null` once the user has answered the sheet, which does
not mean every type was granted: HealthKit never tells an app whether read access was denied. Unknown identifiers are
skipped silently. `objectType` switches to the per-object authorization variant (iOS).

The app needs the **HealthKit** capability and Info.plist strings: `NSHealthShareUsageDescription` (reading) and
`NSHealthUpdateUsageDescription` (writing). Reading the data afterwards (`HKHealthStore` queries) is done in Swift or
through `platform.HealthKit` in `iosMain`.

## ContactsUI

With iOS 18 limited contacts access, users can share some contacts instead of the whole address book.
`ContactAccessButton` shows contacts matching a search query that the app can't see yet; tapping one grants access to
it:

```kotlin
var query by remember { mutableStateOf("") }
var granted by remember { mutableStateOf(emptyList<String>()) }

VStack {
    TextField("Search contacts", text = query, onTextChange = { query = it })
    ContactAccessButton(
        query = query,
        onApproval = { ids -> granted = granted + ids },
        modifier = Modifier.contactAccessButtonStyle(caption = ContactAccessCaption.email, imageColor = Color.blue),
    )
    Text("${granted.size} contacts shared")
}
```

`ignoredEmails` and `ignoredPhoneNumbers` hide contacts the app already knows about. `onApproval` and
`Modifier.contactAccessPicker(isPresented, onDismiss, onCompletion)` (the system sheet for picking more contacts) return
`CNContact` identifiers; fetch the contacts with `CNContactStore`. The app needs `NSContactsUsageDescription`.

## FamilyControls (Screen Time)

`FamilyActivityPicker` lets the user choose apps, categories and websites, for Screen Time APIs (ManagedSettings,
DeviceActivity). The selection is a `FamilyActivitySelection`, which holds opaque tokens; Kotlin keeps it as its JSON
encoding, a `String` you can store and hand back:

```kotlin
class ScreenTimeSettings {
    var selectionJson by mutableStateOf<String?>(null)
}

@Composable
fun BlockedAppsScreen(settings: ScreenTimeSettings) {
    var picking by remember { mutableStateOf(false) }
    Button(
        "Choose apps to limit",
        modifier = Modifier.familyActivityPicker(
            isPresented = picking,
            onDismiss = { picking = false },
            selection = settings.selectionJson,
            onSelectionChange = { settings.selectionJson = it },
            title = "Apps to limit",
        ),
    ) { picking = true }
}
```

Apply the selection with ManagedSettings (`ManagedSettingsStore().shield.applications`) in Swift: decode the JSON with
`JSONDecoder().decode(FamilyActivitySelection.self, …)`. To show one token, `FamilyActivityLabel(tokenJson, kind)` draws
its name and icon from the JSON of an `ApplicationToken`, `ActivityCategoryToken` or `WebDomainToken`.

Requirements: the **Family Controls** capability (`com.apple.developer.family-controls`; distribution needs Apple's
approval), and authorization requested in Swift before showing the picker, with
`AuthorizationCenter.shared.requestAuthorization(for: .individual)` (or `.child`). The sheet's `title` needs iOS 26.2.

## ImagePlayground

`Modifier.imagePlaygroundSheet` opens the Image Playground sheet (Apple Intelligence) to generate an image from text
concepts and an optional source image. `onCompletion` receives the file URL of the created image, in a temporary
location:

```kotlin
var showPlayground by remember { mutableStateOf(false) }
var imageUrl by remember { mutableStateOf<String?>(null) }

VStack(
    modifier = Modifier
        .imagePlaygroundSheet(
            isPresented = showPlayground,
            onDismiss = { showPlayground = false },
            concepts = listOf(
                ImagePlaygroundConcept.text("a lighthouse"),
                ImagePlaygroundConcept.extracted(from = "A stormy night on the Breton coast, waves crashing", title = "Brittany"),
            ),
        ) { url -> imageUrl = url }
        .imagePlaygroundStyle(
            generationStyle = ImagePlaygroundStyle.illustration,
            allowedStyles = listOf(ImagePlaygroundStyle.illustration, ImagePlaygroundStyle.sketch),
        ),
) {
    Button("Create an illustration") { showPlayground = true }
    imageUrl?.let { AsyncImage(it) }
}
```

- `sourceImage` is an asset name, `sourceImageUrl` a file URL of a starting image.
- `onAdaptiveImageGlyph` (iOS 18.4) lets the sheet create a Genmoji, delivered as a HEIC file URL.
- `imagePlaygroundStyle` sets `imagePlaygroundGenerationStyle(_:in:)` and `imagePlaygroundPersonalizationPolicy` (iOS
  18.4, macOS 15.4) and `ImagePlaygroundOptions` (iOS 26.4, macOS 26.4; strategy, variety and size need iOS 27).
  `ImagePlaygroundStyle.externalProvider` needs iOS 26 and `any` iOS 27.

Image Playground needs a device with Apple Intelligence enabled. On other devices the sheet does not appear, so offer
the button only where it makes sense.

## JournalingSuggestions

`JournalingSuggestionsPicker` shows the system picker of journaling moments (workouts, places, photos, music):

```kotlin
var suggestion by remember { mutableStateOf<JournalingSuggestion?>(null) }

VStack {
    JournalingSuggestionsPicker(title = "Add a moment", onCompletion = { suggestion = it })
    suggestion?.let { Text("${it.title}: ${it.itemCount} items") }
}
```

`JournalingSuggestion` carries the `title`, the `start` / `end` of its date interval (epoch seconds, nullable) and the
`itemCount`; the items themselves (photos, workouts…) stay in Swift. `Modifier.journalingSuggestionsPicker` is the sheet
variant.

The framework exists only in the iOS device SDK (not the Simulator), which is why its section is wrapped in
`#if canImport(JournalingSuggestions)`. The app needs the Journaling Suggestions capability
(`com.apple.developer.journal.allow`).

## GameSave (iOS 27, macOS 27)

`Modifier.gameSaveSyncingAlert` opens the iCloud-synced save directory of a game and shows the system alert while it
syncs. `onFinishedLoading` receives the directory URL once it is ready (also when offline or local-only):

```kotlin
var saveDirectory by remember { mutableStateOf<String?>(null) }

Text(
    if (saveDirectory == null) "Loading saves" else "Ready",
    modifier = Modifier.gameSaveSyncingAlert(open = true, containerIdentifier = null) { url -> saveDirectory = url },
)
```

`containerIdentifier = null` uses the app's default iCloud container. The app needs the iCloud capability with
iCloud Documents and that container. Set `open` back to `false` to close the directory.

## ClassKitUI (iOS 27, macOS 27)

For Schoolwork assignments: `AssignedDocumentLabel` shows the status or due date of an assigned document,
`AssignedDocumentSubmissionButton` submits it, and `Modifier.onAssignedDocument` observes submissions:

```kotlin
val doc = "file:///path/to/Essay.pages"

VStack(
    modifier = Modifier.onAssignedDocument(
        willSubmit = { url -> url.endsWith(".pages") },
        didSubmit = { url -> println("Submitted $url") },
    ),
) {
    AssignedDocumentLabel(role = AssignedDocumentRole.date, documentUrl = doc)
    AssignedDocumentSubmissionButton(documentUrl = doc)
}
```

The `willSubmit` / `willWithdraw` handlers return synchronously whether to proceed. The document URL can be a file URL
or a path. The app needs the ClassKit capability (`com.apple.developer.ClassKit-environment`).

## SecurityUI: certificate sheet (27)

`Modifier.certificateSheet` shows the system certificate viewer for a chain of certificates, for example the server
certificate your networking layer rejected. Certificates are base64 DER, leaf first; the sheet shows while the list is
non-null:

```kotlin
var chain by remember { mutableStateOf<List<String>?>(null) }

Button(
    "Show certificate",
    modifier = Modifier.certificateSheet(chain, onDismiss = { chain = null }, title = "api.example.com", message = "This certificate is not trusted."),
) { chain = listOf("MIIDdzCCAl+gAwIBAgIE…") }
```

The trust is evaluated with the basic X.509 policy. Available on iOS, macOS, tvOS and watchOS 27, in SDKs that ship
SecurityUI (`#if canImport(SecurityUI)`).

## AutomatedDeviceEnrollment

`Modifier.automatedDeviceEnrollmentAddition(isPresented, onDismiss)` (iOS 18) presents the system flow that adds the
device to the organization's Apple Business Manager or Apple School Manager. It is meant for device management apps.

## Spatial anchors (iOS 27.2)

`Modifier.spatialAnchors` attaches the iOS 27.2 spatial anchor modifiers: `locationHeadingAnchor` (CoreLocation),
`deviceMotionAnchor` (CoreMotion) and `nearbyInteractionOriginAnchor` (NearbyInteraction). The location and motion
managers default to app-wide instances; to share your own, or for NearbyInteraction, which needs a running session,
create them in `iosMain` with the platform interop and pass them in:

```kotlin skip
import platform.CoreLocation.CLLocationManager
import platform.CoreMotion.CMMotionManager
import platform.NearbyInteraction.NISession

class SpatialSession {
    val location = CLLocationManager()
    val motion = CMMotionManager()
    val nearby = NISession() // run it with a peer's discovery token before use

    fun start() {
        location.requestWhenInUseAuthorization()
    }
}

@Composable
fun FindMyFriend(session: SpatialSession) {
    Text(
        "Point your phone",
        modifier = Modifier.spatialAnchors(
            locationHeading = true,
            deviceMotion = true,
            locationManager = session.location,
            motionManager = session.motion,
            nearbyInteractionSession = session.nearby,
        ),
    )
}
```

`locationHeading` and `deviceMotion` left `null` don't attach their modifier; the NearbyInteraction one is attached
whenever a session is passed (`nearbyInteraction` toggles `isEnabled`). Each needs its usage string:
`NSLocationWhenInUseUsageDescription`, `NSMotionUsageDescription` and `NSNearbyInteractionUsageDescription`. Device only.

## AppIntents: ShortcutsLink

`ShortcutsLink` (iOS, visionOS) opens your app's page in the Shortcuts app; `Modifier.shortcutsStyle` sets
`shortcutsLinkStyle` and, outside macOS, `siriTipViewStyle`:

```kotlin
ShortcutsLink(modifier = Modifier.shortcutsStyle(shortcutsLink = ShortcutsStyle.darkOutline))
```

The shortcuts themselves (`AppIntent`, `AppShortcutsProvider`) are Swift types: App Intents are extracted from Swift at
build time.

## DeviceActivity

`DeviceActivityReport(context)` (iOS) embeds a report rendered by your app's Device Activity Report extension, identified
by its context name. The extension is Swift, and the app needs the Family Controls authorization described above.

```kotlin
DeviceActivityReport(context = "TotalActivity", modifier = Modifier.frame(height = 300.0))
```

## WatchKit: NowPlayingView

`NowPlayingView()` (watchOS) shows the system now playing controls for the audio your app or the paired iPhone plays.

## ManagedAppDistribution (macOS 26.4)

`ManagedContentView` shows an offer row for apps distributed by an organization, and `Modifier.managedContentStyle` its
layout:

```kotlin
var state by remember { mutableStateOf(ManagedContentOfferState.notInstalled) }

ManagedContentView(
    primaryLabel = "Design Tools",
    secondaryLabel = "Internal",
    offerState = state,
    offerAction = { current -> if (current == ManagedContentOfferState.notInstalled) state = ManagedContentOfferState.installing },
    modifier = Modifier.managedContentStyle(ManagedContentStyle.compact),
) {
    Image(systemName = SFSymbol("paintbrush"))
}
```

## PermissionKit (iOS 26, macOS 26)

`CommunicationLimitsButton` lets a child ask a parent for permission to communicate with someone, under Communication
Limits:

```kotlin
CommunicationLimitsButton(
    handles = listOf(CommunicationHandle("friend@example.com", CommunicationHandle.Kind.emailAddress)),
) {
    Text("Ask to message")
}
```

## ScreenCaptureKit: recording editor (27)

`Modifier.recordingEditor(fileUrl, onDismiss, shareMode)` opens the system editor for a screen recording while `fileUrl`
is non-null. On tvOS, `shareMode` picks the share (`true`) or preview (`false`) mode. Available on macOS 27 and on iOS /
tvOS 27 devices, in SDKs that ship it.

## WorkoutKit: workout preview

`Modifier.workoutPreview` previews a single-goal workout and lets the user send it to Apple Watch. The activity type is
an `HKWorkoutActivityType` raw value (37 is running, 13 cycling, 52 walking):

```kotlin
var preview by remember { mutableStateOf(false) }

Button(
    "5 km run",
    modifier = Modifier.workoutPreview(preview, onDismiss = { preview = false }, activityType = 37, goal = WorkoutGoal.DistanceKilometers(5.0)),
) { preview = true }
```

Goals are `WorkoutGoal.Open`, `DistanceKilometers`, `TimeMinutes` and `EnergyKilocalories`. Registered on iOS 17 and
watchOS 11.

## DeviceDiscoveryUI (tvOS)

`DevicePicker` (tvOS 16) browses nearby iPhones and iPads running your app's companion service, so the TV app can connect
to them:

```kotlin
DevicePicker(
    serviceName = "com.example.remote",
    onSelect = { endpoint -> println("Connect to $endpoint") },
    label = { Label("Connect a phone", systemImage = SFSymbol("iphone")) },
    fallback = { Text("Not available") },
)
```

`onSelect` receives a description of the selected `NWEndpoint`; open the connection with Network.framework in Swift.
The service must be declared in both apps' Info.plist under `NSApplicationServices`.
