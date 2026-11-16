<!-- group: Apple frameworks -->
# Photos and media

> Pick photos and videos, play video, preview files, react to camera buttons, offer Apple Music and translate text from Kotlin.

This chapter covers the media-related framework integrations: PhotosUI, AVKit and QuickLook, MusicKit (with
DataDetection) and Translation. Like every framework integration, each one is an opt-in Swift renderer file in
`ComposeSwiftUIIntegrations` that you add to the app target and register once at launch (see
[Apple frameworks](apple-frameworks.md) for the general setup). The full list of Kotlin signatures is on
`/frameworks.html`.

```swift
@main
struct MyApp: App {
    init() {
        #if os(iOS) || os(macOS) || os(visionOS)
        ComposeIntegrations.registerPhotosUI()      // PhotosUI.swift
        ComposeIntegrations.registerTranslation()   // Translation.swift
        #endif
        ComposeIntegrations.registerAVKit()          // AVKit.swift (every platform)
        #if os(iOS) || os(visionOS)
        ComposeIntegrations.registerMusicKit()       // MusicKit.swift
        ComposeIntegrations.registerDataDetection()  // MusicKit.swift, iOS 27
        #endif
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

A register function only exists on the platforms its framework supports, so wrap the call in the same `#if os(…)` as
the file. Inside, each function checks the OS version itself: on an older OS the views it would render are simply not
registered.

| Framework | File | Register | Platforms |
|---|---|---|---|
| PhotosUI | `PhotosUI.swift` | `registerPhotosUI()` | iOS 17, macOS 14, visionOS |
| AVKit, QuickLook | `AVKit.swift` | `registerAVKit()` | all (some APIs per platform, see below) |
| MusicKit | `MusicKit.swift` | `registerMusicKit()` | iOS, visionOS |
| DataDetection | `MusicKit.swift` | `registerDataDetection()` | iOS 27, visionOS |
| Translation | `Translation.swift` | `registerTranslation()` | iOS 18, macOS 15, visionOS |

## PhotosUI

### PhotosPicker

SwiftUI's `PhotosPicker` binds to `[PhotosPickerItem]`, a Swift type that loads its data asynchronously. In Kotlin the
selection is a list of Photos item identifiers (`String`), and the renderer does the loading for you: when the user
picks, every item is copied to a file in the app's temporary directory and `onPick` receives a `PickedMedia` per item.

| SwiftUI | Compose SwiftUI |
|---|---|
| `PhotosPicker("Pick", selection: $item, matching: .images)` | `PhotosPicker(selection, onPick, title = "Pick", filter = listOf(PhotosFilter.images))` |
| `PhotosPicker(selection: $items, maxSelectionCount: 5) { label }` | `PhotosPicker(selection, onPick, maxSelectionCount = 5, label = { … })` |
| `.photosPicker(isPresented: $shown, selection: $items)` | `Modifier.photosPicker(isPresented, onDismiss, selection, onPick)` |
| `PhotosPickerItem.loadTransferable(type:)` | done for you: `PickedMedia.fileUrl` |

```kotlin
var selection by remember { mutableStateOf(emptyList<String>()) }
var picked by remember { mutableStateOf<PickedMedia?>(null) }

VStack(spacing = 16.0) {
    PhotosPicker(
        selection = selection,
        onPick = { media ->
            selection = media.mapNotNull { it.itemIdentifier }
            picked = media.firstOrNull()
        },
        title = "Choose a photo",
        filter = listOf(PhotosFilter.images),
    )
    picked?.fileUrl?.let { url ->
        AsyncImage(url) { state ->
            if (state == AsyncImageState.Success) AsyncImageContent(Modifier.resizable().scaledToFit())
        }
    }
}
```

`PickedMedia` carries three nullable fields:

- `itemIdentifier`: the Photos identifier, which you feed back into `selection` so the picker shows the item as selected.
- `fileUrl`: a `file://` URL of the copy in the temporary directory. Move or upload the file if you need it later; the
  system can purge the temporary directory.
- `contentType`: the uniform type identifier of the data (`"public.jpeg"`, `"public.heic"`, `"com.apple.quicktime-movie"`…).

`selection` is not updated for you: `onPick` reports the new selection and you decide what to keep, as with any
Compose SwiftUI binding. The copy runs off the main thread, so `onPick` arrives shortly after the picker closes, not
synchronously.

`maxSelectionCount = 1` (the default) is the single-selection picker; any other number allows several items, and `null`
means unlimited. `selectionBehavior` (`ordered`, `continuous`, `continuousAndOrdered`) and `preferredItemEncoding`
(`current` keeps HEIC/HEVC, `compatible` transcodes to JPEG/H.264) map to the SwiftUI parameters of the same name.

`filter` takes any number of `PhotosFilter` values and matches any of them (`PHPickerFilter.any(of:)`); an empty list
shows everything. `spatialMedia` needs iOS 26 / macOS 26.

The picker runs out of process, so picking needs no photo library permission and no Info.plist key. You only need
`NSPhotoLibraryUsageDescription` if you also use PhotoKit directly.

### The picker as a sheet

```kotlin
var showPicker by remember { mutableStateOf(false) }
var selection by remember { mutableStateOf(emptyList<String>()) }

VStack(
    modifier = Modifier.photosPicker(
        isPresented = showPicker,
        onDismiss = { showPicker = false },
        selection = selection,
        onPick = { media -> selection = media.mapNotNull { it.itemIdentifier } },
        maxSelectionCount = 10,
        selectionBehavior = PhotosPickerSelectionBehavior.ordered,
        filter = listOf(PhotosFilter.images, PhotosFilter.livePhotos),
    ),
) {
    Button("Add photos") { showPicker = true }
    Text("${selection.size} selected")
}
```

### Picker styles: inline and compact pickers

`Modifier.photosPickerStyle` groups the picker configuration modifiers. Apply it to the `PhotosPicker` (or to the view
carrying `Modifier.photosPicker`):

```kotlin
var selection by remember { mutableStateOf(emptyList<String>()) }

PhotosPicker(
    selection = selection,
    onPick = { media -> selection = media.mapNotNull { it.itemIdentifier } },
    maxSelectionCount = null,
    modifier = Modifier
        .photosPickerStyle(
            style = PhotosPickerStyle.inline,
            accessoryVisibility = Visibility.hidden,
            accessoryEdges = Edge.Set.all,
            disabledCapabilities = listOf(PhotosPickerCapability.selectionActions, PhotosPickerCapability.stagingArea),
        )
        .frame(height = 300.0),
)
```

| Parameter | SwiftUI modifier | Availability |
|---|---|---|
| `style` | `photosPickerStyle(.presentation / .inline / .compact)` | iOS 17, macOS 14 |
| `accessoryVisibility`, `accessoryEdges` | `photosPickerAccessoryVisibility(_:edges:)` | iOS 17, macOS 14 |
| `disabledCapabilities` | `photosPickerDisabledCapabilities(_:)` | iOS 17, macOS 14 |
| `metadata` (`removeLocation`, `removeCaptions`) | `photosPickerMetadataOptions(_:)` | iOS 27, macOS 27 |
| `searchText` | `photosPickerSearchText(_:)` | iOS 27, macOS 27 |

An inline or compact picker lives in your layout, so give it a frame. Hiding the accessories and disabling
`selectionActions` lets you supply your own "Done" button.

### Shared albums (iOS 27)

`Modifier.photosSharedAlbumSheet` covers the three iCloud Shared Album sheets; which one shows depends on the arguments:

- `itemIdentifiers` set: `photosSharedAlbumPostingSheet`, posting those items (to `albumIdentifier` when given).
- `albumIdentifier` set alone: `photosSharedAlbumCustomizationSheet` for that album.
- neither: `photosSharedAlbumCreationSheet`, with `defaultTitle`.

```kotlin
var sharing by remember { mutableStateOf(false) }
var selection by remember { mutableStateOf(emptyList<String>()) }

Button(
    "Share ${selection.size} photos",
    modifier = Modifier.photosSharedAlbumSheet(
        isPresented = sharing,
        onDismiss = { sharing = false },
        itemIdentifiers = selection,
        onCompletion = { error -> if (error != null) println("Sharing failed: $error") },
    ),
) { sharing = true }
```

These sheets are iOS and visionOS only and registered on iOS 27 and later.

### Reference image viewer (iOS 27)

`Modifier.photosReferenceImageViewer(fileUrl, onDismiss, onCompletion)` shows the image at a file URL while `fileUrl` is
non-null; set it back to `null` in `onDismiss`. `onCompletion` receives the URL of the processed result, or `null`.

## AVKit and QuickLook

### VideoPlayer

`VideoPlayer` wraps SwiftUI's `VideoPlayer` around an `AVPlayer` owned by the renderer. Instead of handing a player
object around, you describe the playback state from Kotlin:

```kotlin
var playing by remember { mutableStateOf(false) }

VStack {
    VideoPlayer(
        url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        isPlaying = playing,
        onPlayingChange = { playing = it },
        loops = true,
        modifier = Modifier.frame(height = 240.0),
        videoOverlay = {
            Text("LIVE", modifier = Modifier.padding())
        },
    )
    Button(if (playing) "Pause" else "Play") { playing = !playing }
}
```

- `url` is any URL `AVPlayerItem` accepts: HTTPS, HLS or a `file://` URL (for example a video from `PhotosPicker`).
  Changing it replaces the current item.
- `isPlaying` plays or pauses. When the user uses the system controls, `onPlayingChange` reports it: hoist the state, as
  above, or the next recomposition will undo the user's action.
- `isMuted` and `loops` are applied live. `videoOverlay` is drawn above the video and below the controls.

`VideoPlayer` works on every platform. Plain `http://` URLs need an App Transport Security exception in Info.plist.

### Camera capture events (iOS 26)

`Modifier.onCameraCaptureEvent` handles the hardware camera buttons (volume buttons, Camera Control) in a camera
screen:

```kotlin
var recording by remember { mutableStateOf(false) }

Text(
    if (recording) "Recording" else "Ready",
    modifier = Modifier.onCameraCaptureEvent(
        secondaryAction = { phase -> if (phase == CaptureEventPhase.ended) println("Secondary button") },
    ) { phase ->
        when (phase) {
            CaptureEventPhase.began -> recording = true
            CaptureEventPhase.ended, CaptureEventPhase.cancelled -> recording = false
        }
    },
)
```

The system only delivers these events while your app runs a camera capture session, which you set up yourself (with
`AVCaptureSession` through Kotlin/Native interop or in Swift); the camera needs `NSCameraUsageDescription`.
`defaultSoundDisabled = true` silences the shutter sound, and `isEnabled = false` turns the handling off. iOS only.

### Continuity device picker (tvOS)

`Modifier.continuityDevicePicker(isPresented, onDismiss, onDidConnect)` presents the tvOS picker for a nearby iPhone or
iPad camera (Continuity Camera). `onDidConnect` receives a description of the connected device, or `null`. tvOS 17 only.

### quickLookPreview

`Modifier.quickLookPreview(selection, onSelectionChange, items)` previews a file. `selection` is the file URL shown
(null hides the preview); with `items`, the user can page between several files.

```kotlin
val files = listOf("file:///tmp/report.pdf", "file:///tmp/photo.heic")
var previewed by remember { mutableStateOf<String?>(null) }

List(
    modifier = Modifier.quickLookPreview(previewed, onSelectionChange = { previewed = it }, items = files),
) {
    for (file in files) key(file) {
        Button(file.substringAfterLast('/')) { previewed = file }
    }
}
```

iOS, macOS and visionOS. The URLs must be local files the app can read.

## MusicKit

The MusicKit integration covers the views; querying the catalog and playback (`MusicCatalogSearchRequest`,
`ApplicationMusicPlayer`) stay in Swift. Every MusicKit API needs the MusicKit app service enabled for your App ID in
the Apple Developer portal, and `NSAppleMusicUsageDescription` in Info.plist when you access the user's library.

### Subscription offer

`Modifier.musicSubscriptionOffer` shows the Apple Music subscription sheet, optionally for a catalog item:

```kotlin
var offering by remember { mutableStateOf(false) }

Button(
    "Listen on Apple Music",
    modifier = Modifier.musicSubscriptionOffer(
        isPresented = offering,
        onDismiss = { offering = false },
        messageIdentifier = MusicOfferMessage.playMusic,
        itemId = "1440818839",
        onLoadCompletion = { error -> if (error != null) offering = false },
    ),
) { offering = true }
```

`affiliateToken` and `campaignToken` are passed to `MusicSubscriptionOffer.Options` for attribution. Show the offer only
to users who can subscribe (`MusicSubscription.current.canBecomeSubscriber`, checked in Swift and passed to Kotlin, for
example through `ComposeView(input:)`).

### Music picker (iOS 27)

`Modifier.musicPicker(isPresented, onDismiss, multiple, title, onPick)` lets the user pick songs from Apple Music.
`onPick` receives the catalog ids (`MusicItemID` raw values) of the songs; `multiple = true` allows several.

```kotlin
var picking by remember { mutableStateOf(false) }
var songs by remember { mutableStateOf(emptyList<String>()) }

Button(
    "Pick songs (${songs.size})",
    modifier = Modifier.musicPicker(isPresented = picking, onDismiss = { picking = false }, multiple = true, title = "Add songs") { ids ->
        songs = songs + ids
    },
) { picking = true }
```

### Data detection (iOS 27)

`Modifier.dataDetection(types)` (DataDetection framework, registered by `registerDataDetection()`) detects links, phone
numbers, addresses, flight numbers, tracking numbers and more in the text below the modifier and makes them actionable:

```kotlin
Text(
    "Flight UA 123 lands at 4:05 PM, call +1 408 555 0100",
    modifier = Modifier.dataDetection(listOf(DataDetectorType.flightNumber, DataDetectorType.phoneNumber)),
)
```

## Translation

The Translation framework runs on device. It works on iOS 18, macOS 15 and visionOS, but not in the Simulator: test on
a real device.

### Translation popover

`Modifier.translationPresentation` shows the system translation UI for a text. With `replacementAction`, the popover gets
a "Replace with Translation" button that hands you the translated text:

```kotlin
var text by remember { mutableStateOf("Bonjour tout le monde") }
var translating by remember { mutableStateOf(false) }

Text(
    text,
    modifier = Modifier
        .translationPresentation(
            isPresented = translating,
            onDismiss = { translating = false },
            text = text,
            arrowEdge = Edge.bottom,
            replacementAction = { translated -> text = translated },
        )
        .onTapGesture { translating = true },
)
```

### Translating in code

`Modifier.translationTask` translates a list of strings in the background, again whenever the list changes, and
reports the results in the same order:

```kotlin
class ReviewsState {
    var originals by mutableStateOf(listOf("Très bon produit", "Livraison rapide"))
    var translated by mutableStateOf(emptyList<String>())
    var error by mutableStateOf<String?>(null)
}

@Composable
fun TranslatedReviews(state: ReviewsState) {
    List(
        modifier = Modifier.translationTask(texts = state.originals, source = "fr", target = "en") { translations, error ->
            state.translated = translations
            state.error = error
        },
    ) {
        for (line in state.translated.ifEmpty { state.originals }) {
            Text(line)
        }
    }
}
```

`source` and `target` are BCP 47 language codes; `null` detects the source language and translates into the user's
language. The first time a language pair is used, the system may ask the user to download the language. `error` is
non-null, with an empty list, when the session fails (unsupported pair, download declined).

Only a change of `texts` starts a new translation; changing `source` or `target` alone does not. `preferredStrategy` is
accepted but not yet forwarded to the Translation session.
