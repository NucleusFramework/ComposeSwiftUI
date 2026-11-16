@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// The SwiftUI APIs of PhotosUI, WebKit, AVKit, QuickLook, AuthenticationServices, CoreLocationUI, Translation,
// LocalAuthentication, SpriteKit, SceneKit, GameController, PassKit, MusicKit and DataDetection. Each framework has
// its own Swift renderer in swift/ComposeSwiftUIIntegrations (PhotosUI.swift…): add the files the app uses and call
// their `ComposeIntegrations.register…()`.

internal object FrameworkIds {
    const val PHOTOS_PICKER = 200_300
    const val WEB_VIEW = 200_301
    const val VIDEO_PLAYER = 200_302
    const val SIGN_IN_WITH_APPLE = 200_303
    const val LOCATION_BUTTON = 200_304
    const val LOCAL_AUTHENTICATION = 200_305
    const val SPRITE_VIEW = 200_306
    const val SCENE_VIEW = 200_307
    const val APPLE_PAY_BUTTON = 200_308
    const val ADD_PASS_BUTTON = 200_309
    const val PAY_LATER = 200_310
    const val MERCHANDISING = 200_311
    const val VERIFY_IDENTITY = 200_312

    const val PHOTOS_PICKER_SHEET = 200_300
    const val PHOTOS_PICKER_STYLE = 200_301
    const val SHARED_ALBUM = 200_302
    const val REFERENCE_IMAGE = 200_303
    const val WEB_VIEW_STYLE = 200_304
    const val CAMERA_CAPTURE = 200_305
    const val CONTINUITY_PICKER = 200_306
    const val QUICK_LOOK = 200_307
    const val SIGN_IN_STYLE = 200_308
    const val AUTHORIZATION = 200_309
    const val WEB_AUTHENTICATION = 200_310
    const val TRANSLATION_PRESENTATION = 200_311
    const val TRANSLATION_TASK = 200_312
    const val GAME_CONTROLLER = 200_313
    const val APPLE_PAY_STYLE = 200_314
    const val APPLE_PAY_CHANGES = 200_315
    const val MUSIC_SUBSCRIPTION = 200_316
    const val MUSIC_PICKER = 200_317
    const val DATA_DETECTION = 200_318
}

// ---- PhotosUI ---------------------------------------------------------------------------------------------------

/** A photo or video the user picked: its Photos identifier and a copy in the app's temporary directory. */
@HiddenFromObjC
public data class PickedMedia(val itemIdentifier: String?, val fileUrl: String?, val contentType: String?)

/** PhotosUI `PHPickerFilter`: the kinds of assets the picker shows (all when empty). */
@HiddenFromObjC
public enum class PhotosFilter { images, videos, livePhotos, screenshots, screenRecordings, slomoVideos, timelapseVideos, panoramas, bursts, depthEffectPhotos, cinematicVideos, spatialMedia }

/** PhotosUI `PhotosPickerSelectionBehavior`. */
@HiddenFromObjC
public enum class PhotosPickerSelectionBehavior { default, ordered, continuous, continuousAndOrdered }

/** PhotosUI `PhotosPickerItem.EncodingDisambiguationPolicy`. */
@HiddenFromObjC
public enum class PhotosPickerEncoding { automatic, current, compatible }

/** PhotosUI `PhotosPickerStyle`. */
@HiddenFromObjC
public enum class PhotosPickerStyle { presentation, inline, compact }

private fun photosArgs(
    selection: List<String>, onPick: (List<PickedMedia>) -> Unit, maxSelectionCount: Int?, behavior: PhotosPickerSelectionBehavior,
    filter: List<PhotosFilter>, encoding: PhotosPickerEncoding,
): List<Any?> = listOf(selection, SValueCallback { v ->
    onPick((v as List<*>).map { e -> (e as List<*>).let { PickedMedia(it[0] as String?, it[1] as String?, it[2] as String?) } })
}, maxSelectionCount, behavior.ordinal, filter.map { it.ordinal }, encoding.ordinal)

/**
 * PhotosUI `PhotosPicker(_:selection:maxSelectionCount:selectionBehavior:matching:preferredItemEncoding:photoLibrary:)`:
 * [selection] holds the picked Photos identifiers; [onPick] receives the new selection, each item copied to a
 * temporary file. [maxSelectionCount] 1 is the single-selection picker; null is unlimited.
 */
@HiddenFromObjC
@Composable
public fun PhotosPicker(
    selection: List<String>,
    onPick: (List<PickedMedia>) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    maxSelectionCount: Int? = 1,
    selectionBehavior: PhotosPickerSelectionBehavior = PhotosPickerSelectionBehavior.default,
    filter: List<PhotosFilter> = emptyList(),
    preferredItemEncoding: PhotosPickerEncoding = PhotosPickerEncoding.automatic,
    label: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.PHOTOS_PICKER, photosArgs(selection, onPick, maxSelectionCount, selectionBehavior, filter, preferredItemEncoding) +
        listOf(title, label?.let { SSlotRef(0) }), modifier, slots = listOfNotNull(label))
}

/** PhotosUI `photosPicker(isPresented:selection:maxSelectionCount:selectionBehavior:matching:preferredItemEncoding:photoLibrary:)`. */
@HiddenFromObjC
public fun Modifier.photosPicker(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    selection: List<String>,
    onPick: (List<PickedMedia>) -> Unit,
    maxSelectionCount: Int? = 1,
    selectionBehavior: PhotosPickerSelectionBehavior = PhotosPickerSelectionBehavior.default,
    filter: List<PhotosFilter> = emptyList(),
    preferredItemEncoding: PhotosPickerEncoding = PhotosPickerEncoding.automatic,
): Modifier = element(FrameworkIds.PHOTOS_PICKER_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() },
    *photosArgs(selection, onPick, maxSelectionCount, selectionBehavior, filter, preferredItemEncoding).toTypedArray())

/** PhotosUI `PHPickerCapabilities`. */
@HiddenFromObjC
public enum class PhotosPickerCapability { search, collectionNavigation, stagingArea, selectionActions, sensitivityAnalysisIntervention }

/** PhotosUI `PHPickerMetadataOptions`. */
@HiddenFromObjC
public enum class PhotosPickerMetadata { removeLocation, removeCaptions }

/**
 * PhotosUI picker modifiers: `photosPickerStyle(_:)`, `photosPickerAccessoryVisibility(_:edges:)`,
 * `photosPickerDisabledCapabilities(_:)`, `photosPickerMetadataOptions(_:)`, `photosPickerSearchText(_:)`.
 */
@HiddenFromObjC
public fun Modifier.photosPickerStyle(
    style: PhotosPickerStyle? = null,
    accessoryVisibility: Visibility? = null,
    accessoryEdges: Edge.Set? = null,
    disabledCapabilities: List<PhotosPickerCapability> = emptyList(),
    metadata: List<PhotosPickerMetadata> = emptyList(),
    searchText: String? = null,
): Modifier = element(FrameworkIds.PHOTOS_PICKER_STYLE, style?.ordinal, accessoryVisibility, accessoryEdges,
    disabledCapabilities.map { it.ordinal }, metadata.map { it.ordinal }, searchText)

/**
 * PhotosUI shared album sheets (iOS 27): `photosSharedAlbumCreationSheet`, `photosSharedAlbumCustomizationSheet` (with
 * [albumIdentifier]) and `photosSharedAlbumPostingSheet` / `postToPhotosSharedAlbumSheet` (posting [itemIdentifiers]).
 * [onCompletion] receives the error message, or null.
 */
@HiddenFromObjC
public fun Modifier.photosSharedAlbumSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    albumIdentifier: String? = null,
    itemIdentifiers: List<String>? = null,
    defaultTitle: String? = null,
    onCompletion: (error: String?) -> Unit = {},
): Modifier = element(FrameworkIds.SHARED_ALBUM, isPresented, SValueCallback { if (it == false) onDismiss() }, albumIdentifier,
    itemIdentifiers, defaultTitle, SValueCallback { onCompletion(it as String?) })

/** PhotosUI `photosReferenceImageViewer(fileURL:onProcessingCompletion:)` (iOS 27): shows the image at [fileUrl]. */
@HiddenFromObjC
public fun Modifier.photosReferenceImageViewer(fileUrl: String?, onDismiss: () -> Unit, onCompletion: (resultUrl: String?) -> Unit = {}): Modifier =
    element(FrameworkIds.REFERENCE_IMAGE, fileUrl, SValueCallback { if (it == null) onDismiss() }, SValueCallback { onCompletion(it as String?) })

// ---- WebKit ------------------------------------------------------------------------------------------------------

/** Drives a [WebView] (WebKit `WebPage`): navigation and the page state. */
@HiddenFromObjC
public class WebViewController {
    internal var handle: KotlinFunction? = null
    public var title: String by mutableStateOf("")
        internal set
    public var url: String? by mutableStateOf(null)
        internal set
    public var isLoading: Boolean by mutableStateOf(false)
        internal set
    public var estimatedProgress: Double by mutableStateOf(0.0)
        internal set
    public var canGoBack: Boolean by mutableStateOf(false)
        internal set
    public var canGoForward: Boolean by mutableStateOf(false)
        internal set

    public fun load(url: String) { handle?.invoke(listOf(0, url)) }
    public fun loadHtml(html: String, baseUrl: String? = null) { handle?.invoke(listOf(1, html, baseUrl)) }
    public fun reload() { handle?.invoke(listOf(2)) }
    public fun stopLoading() { handle?.invoke(listOf(3)) }
    public fun goBack() { handle?.invoke(listOf(4)) }
    public fun goForward() { handle?.invoke(listOf(5)) }

    /** Runs JavaScript in the page; [onResult] receives the result as a string, or the error. */
    public fun evaluateJavaScript(script: String, onResult: (String?) -> Unit = {}) {
        handle?.invoke(listOf(6, script, SValueCallback { onResult(it as String?) }))
    }

    internal fun update(v: Any?) {
        val l = v as? List<*> ?: return
        title = l[0] as String? ?: ""
        url = l[1] as String?
        isLoading = l[2] as Boolean
        estimatedProgress = (l[3] as Number).toDouble()
        canGoBack = l[4] as Boolean
        canGoForward = l[5] as Boolean
    }
}

/**
 * WebKit `WebView(url:)` / `WebView(_ page:)` (iOS 26 / macOS 26): shows [url]; a [controller] loads pages and reads the
 * title, URL and loading state. [contextMenu] builds the menu for a link (`webViewContextMenu`, macOS).
 */
@HiddenFromObjC
@Composable
public fun WebView(
    url: String?,
    modifier: Modifier = Modifier,
    controller: WebViewController? = null,
    contextMenu: (@Composable (linkUrl: String?) -> Unit)? = null,
) {
    var link by remember { mutableStateOf<String?>(null) }
    val menu: (@Composable () -> Unit)? = contextMenu?.let { c -> { c(link) } }
    SwiftView(FrameworkIds.WEB_VIEW, listOf(url, controller?.let { c -> SValueCallback { h -> c.handle = h as? KotlinFunction } },
        controller?.let { c -> SValueCallback { c.update(it) } }, menu?.let { SSlotRef(0) }, menu?.let { SValueCallback { link = it as String? } }),
        modifier, slots = listOfNotNull(menu))
}

/** WebKit gesture and preview behaviors (`WebView.…Behavior`). */
@HiddenFromObjC
public enum class WebViewBehavior { automatic, enabled, disabled }

/**
 * WebKit view modifiers: `webViewBackForwardNavigationGestures`, `webViewMagnificationGestures`, `webViewLinkPreviews`,
 * `webViewElementFullscreenBehavior`, `webViewTextSelection`, `webViewContentBackground`, `webViewScrollInputBehavior`.
 */
@HiddenFromObjC
public fun Modifier.webViewStyle(
    backForwardNavigationGestures: WebViewBehavior? = null,
    magnificationGestures: WebViewBehavior? = null,
    linkPreviews: WebViewBehavior? = null,
    elementFullscreen: WebViewBehavior? = null,
    textSelectionEnabled: Boolean? = null,
    contentBackground: Visibility? = null,
    scrollInputBehavior: ScrollInputBehavior? = null,
    scrollInputKind: ScrollInputKind? = null,
): Modifier = element(FrameworkIds.WEB_VIEW_STYLE, backForwardNavigationGestures?.ordinal, magnificationGestures?.ordinal,
    linkPreviews?.ordinal, elementFullscreen?.ordinal, textSelectionEnabled, contentBackground, scrollInputBehavior, scrollInputKind, null, null)

/**
 * WebKit `webViewScrollPosition(_:)` and `webViewOnScrollGeometryChange(for:of:action:)`: scrolls the page to
 * [position] and reports the content offset (x, y) and size (width, height).
 */
@HiddenFromObjC
public fun Modifier.webViewScroll(position: ScrollPosition? = null, onScrollGeometryChange: ((offset: CGPoint, contentSize: CGSize) -> Unit)? = null): Modifier =
    element(FrameworkIds.WEB_VIEW_STYLE, null, null, null, null, null, null, null, null, position, onScrollGeometryChange?.let { cb ->
        SValueCallback { v -> (v as List<*>).map { (it as Number).toDouble() }.let { cb(CGPoint(it[0], it[1]), CGSize(it[2], it[3])) } }
    })

// ---- AVKit, QuickLook ----------------------------------------------------------------------------------------------

/**
 * AVKit `VideoPlayer(player:videoOverlay:)`: plays [url]; [isPlaying] plays or pauses it, [onPlayingChange] reports the
 * user's play / pause. [videoOverlay] draws over the video.
 */
@HiddenFromObjC
@Composable
public fun VideoPlayer(
    url: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onPlayingChange: ((Boolean) -> Unit)? = null,
    isMuted: Boolean = false,
    loops: Boolean = false,
    videoOverlay: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.VIDEO_PLAYER, listOf(url, isPlaying, onPlayingChange?.let { SValueCallback { v -> it(v as Boolean) } }, isMuted, loops,
        videoOverlay?.let { SSlotRef(0) }), modifier, slots = listOfNotNull(videoOverlay))
}

/** The phase of a camera capture event (AVKit `AVCaptureEventPhase`). */
@HiddenFromObjC
public enum class CaptureEventPhase { began, ended, cancelled }

/**
 * AVKit `onCameraCaptureEvent(isEnabled:defaultSoundDisabled:primaryAction:secondaryAction:)` (iOS 26): the hardware
 * camera buttons; [secondaryAction] handles the secondary button when set.
 */
@HiddenFromObjC
public fun Modifier.onCameraCaptureEvent(
    isEnabled: Boolean = true,
    defaultSoundDisabled: Boolean = false,
    secondaryAction: ((CaptureEventPhase) -> Unit)? = null,
    action: (CaptureEventPhase) -> Unit,
): Modifier = element(FrameworkIds.CAMERA_CAPTURE, isEnabled, defaultSoundDisabled, SValueCallback { action(CaptureEventPhase.entries[(it as Number).toInt()]) },
    secondaryAction?.let { f -> SValueCallback { f(CaptureEventPhase.entries[(it as Number).toInt()]) } })

/** AVKit `continuityDevicePicker(isPresented:onDidConnect:)` (tvOS): [onDidConnect] receives the device name, or null. */
@HiddenFromObjC
public fun Modifier.continuityDevicePicker(isPresented: Boolean, onDismiss: () -> Unit, onDidConnect: (deviceName: String?) -> Unit = {}): Modifier =
    element(FrameworkIds.CONTINUITY_PICKER, isPresented, SValueCallback { if (it == false) onDismiss() }, SValueCallback { onDidConnect(it as String?) })

/** QuickLook `quickLookPreview(_:in:)`: previews [selection] (a file URL) among [items]; null closes the preview. */
@HiddenFromObjC
public fun Modifier.quickLookPreview(selection: String?, onSelectionChange: (String?) -> Unit, items: List<String> = emptyList()): Modifier =
    element(FrameworkIds.QUICK_LOOK, selection, SValueCallback { onSelectionChange(it as String?) }, items)

// ---- AuthenticationServices ---------------------------------------------------------------------------------

/** AuthenticationServices `SignInWithAppleButton.Label`. */
@HiddenFromObjC
public enum class SignInWithAppleLabel { signIn, `continue`, signUp }

/** AuthenticationServices `SignInWithAppleButton.Style`. */
@HiddenFromObjC
public enum class SignInWithAppleStyle { black, white, whiteOutline }

/** An Apple ID credential (AuthenticationServices `ASAuthorizationAppleIDCredential`); tokens are UTF-8 strings. */
@HiddenFromObjC
public data class AppleIdCredential(
    val user: String,
    val email: String?,
    val givenName: String?,
    val familyName: String?,
    val identityToken: String?,
    val authorizationCode: String?,
)

/** The result of a Sign in with Apple, password or passkey request (AuthenticationServices `ASAuthorizationResult`). */
@HiddenFromObjC
public sealed class AuthorizationResult {
    public data class AppleId(val credential: AppleIdCredential) : AuthorizationResult()
    public data class Password(val user: String, val password: String) : AuthorizationResult()
    /** A passkey assertion or registration: ids, client data and signatures in base64. */
    public data class Passkey(val isRegistration: Boolean, val credentialId: String, val rawClientDataJson: String,
                              val authenticatorData: String?, val signature: String?, val userId: String?, val attestationObject: String?) : AuthorizationResult()
    public data class Failure(val message: String) : AuthorizationResult()

    internal companion object {
        fun decode(v: Any?): AuthorizationResult {
            val l = v as? List<*> ?: return Failure("unknown")
            fun s(i: Int) = l.getOrNull(i) as String?
            return when ((l[0] as Number).toInt()) {
                0 -> AppleId(AppleIdCredential(s(1) ?: "", s(2), s(3), s(4), s(5), s(6)))
                1 -> Password(s(1) ?: "", s(2) ?: "")
                2 -> Passkey(l[1] == true, s(2) ?: "", s(3) ?: "", s(4), s(5), s(6), s(7))
                else -> Failure(s(1) ?: "unknown")
            }
        }
    }
}

/**
 * AuthenticationServices `SignInWithAppleButton(_:onRequest:onCompletion:)`: asks for [requestedScopes] ("fullName",
 * "email") with an optional [nonce]; [onCompletion] receives the credential or the error.
 */
@HiddenFromObjC
@Composable
public fun SignInWithAppleButton(
    onCompletion: (AuthorizationResult) -> Unit,
    modifier: Modifier = Modifier,
    label: SignInWithAppleLabel = SignInWithAppleLabel.signIn,
    requestedScopes: List<String> = emptyList(),
    nonce: String? = null,
    state: String? = null,
) {
    SwiftView(FrameworkIds.SIGN_IN_WITH_APPLE, listOf(label.ordinal, requestedScopes, nonce, state,
        SValueCallback { onCompletion(AuthorizationResult.decode(it)) }), modifier)
}

/** AuthenticationServices `signInWithAppleButtonStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.signInWithAppleButtonStyle(style: SignInWithAppleStyle): Modifier = element(FrameworkIds.SIGN_IN_STYLE, style.ordinal)

/** A request of [authorizationRequest] (AuthenticationServices `ASAuthorizationRequest`). */
@HiddenFromObjC
public sealed class AuthorizationRequest {
    internal abstract fun encode(): List<Any?>

    /** Sign in with Apple. */
    public data class AppleId(val requestedScopes: List<String> = emptyList(), val nonce: String? = null) : AuthorizationRequest() {
        override fun encode() = listOf(0, requestedScopes, nonce)
    }

    /** A saved password (iCloud Keychain). */
    public data object Password : AuthorizationRequest() {
        override fun encode() = listOf(1)
    }

    /** A passkey assertion for [relyingParty] with a base64 [challenge]. */
    public data class PasskeyAssertion(val relyingParty: String, val challenge: String) : AuthorizationRequest() {
        override fun encode() = listOf(2, relyingParty, challenge)
    }

    /** A passkey registration for [userName] / base64 [userId] with a base64 [challenge]. */
    public data class PasskeyRegistration(val relyingParty: String, val challenge: String, val userName: String, val userId: String) : AuthorizationRequest() {
        override fun encode() = listOf(3, relyingParty, challenge, userName, userId)
    }
}

/**
 * AuthenticationServices `AuthorizationController.performRequests(_:)` (environment `authorizationController`): when
 * [trigger] changes, performs [requests] (or `performAutoFillAssistedRequests` when [autoFillAssisted]).
 */
@HiddenFromObjC
public fun Modifier.authorizationRequest(
    trigger: Any?,
    requests: List<AuthorizationRequest>,
    autoFillAssisted: Boolean = false,
    preferImmediatelyAvailableCredentials: Boolean = false,
    onResult: (AuthorizationResult) -> Unit,
): Modifier = element(FrameworkIds.AUTHORIZATION, trigger, requests.map { it.encode() }, autoFillAssisted, preferImmediatelyAvailableCredentials,
    SValueCallback { onResult(AuthorizationResult.decode(it)) })

/**
 * AuthenticationServices `WebAuthenticationSession.authenticate(using:callbackURLScheme:preferredBrowserSession:)`
 * (environment `webAuthenticationSession`): when [trigger] changes, opens [url]; [onResult] receives the callback URL,
 * or null and the error.
 */
@HiddenFromObjC
public fun Modifier.webAuthenticationSession(
    trigger: Any?,
    url: String,
    callbackUrlScheme: String,
    ephemeral: Boolean = false,
    onResult: (callbackUrl: String?, error: String?) -> Unit,
): Modifier = element(FrameworkIds.WEB_AUTHENTICATION, trigger, url, callbackUrlScheme, ephemeral,
    SValueCallback { v -> (v as List<*>).let { onResult(it[0] as String?, it[1] as String?) } })

// ---- CoreLocationUI, LocalAuthentication, Translation, GameController ------------------------------------------

/** CoreLocationUI `LocationButton.Title`. */
@HiddenFromObjC
public enum class LocationButtonTitle { currentLocation, sendCurrentLocation, sendMyCurrentLocation, shareCurrentLocation, shareMyCurrentLocation }

/** CoreLocationUI `LocationButton(_:action:)`: grants one-time location access, then calls [action]. Null [title]: icon only. */
@HiddenFromObjC
@Composable
public fun LocationButton(action: () -> Unit, modifier: Modifier = Modifier, title: LocationButtonTitle? = LocationButtonTitle.currentLocation) {
    SwiftView(FrameworkIds.LOCATION_BUTTON, listOf(title?.ordinal, SAction(action)), modifier)
}

/**
 * LocalAuthentication `LocalAuthenticationView(_:reason:context:result:)` (macOS 15): Touch ID /
 * passcode; [onResult] receives null on success or the error message. [label] replaces the [title].
 */
@HiddenFromObjC
@Composable
public fun LocalAuthenticationView(
    reason: String,
    onResult: (error: String?) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    label: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.LOCAL_AUTHENTICATION, listOf(reason, SValueCallback { onResult(it as String?) }, title, label?.let { SSlotRef(0) }),
        modifier, slots = listOfNotNull(label))
}

/**
 * Translation `translationPresentation(isPresented:text:attachmentAnchor:arrowEdge:replacementAction:)`: the system
 * translation popover; [replacementAction] shows a "Replace with Translation" button.
 */
@HiddenFromObjC
public fun Modifier.translationPresentation(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    text: String,
    arrowEdge: Edge = Edge.top,
    replacementAction: ((String) -> Unit)? = null,
): Modifier = element(FrameworkIds.TRANSLATION_PRESENTATION, isPresented, SValueCallback { if (it == false) onDismiss() }, text, arrowEdge,
    replacementAction?.let { f -> SValueCallback { f(it as String) } })

/** Translation `TranslationSession.Strategy`. */
@HiddenFromObjC
public enum class TranslationStrategy { highFidelity, lowLatency }

/**
 * Translation `translationTask(source:target:preferredStrategy:action:)`: translates [texts] from [source] to
 * [target] (BCP 47 language codes, null to detect / use the user's language) whenever they change.
 */
@HiddenFromObjC
public fun Modifier.translationTask(
    texts: List<String>,
    source: String? = null,
    target: String? = null,
    preferredStrategy: TranslationStrategy? = null,
    onTranslated: (translations: List<String>, error: String?) -> Unit,
): Modifier = element(FrameworkIds.TRANSLATION_TASK, texts, source, target, preferredStrategy?.ordinal,
    SValueCallback { v -> (v as List<*>).let { onTranslated((it[0] as List<*>).map { s -> s as String }, it[1] as String?) } })

/** GameController `GCUIEventTypes`. */
@HiddenFromObjC
public enum class GameControllerEventType { gamepad }

/** GameController `handlesGameControllerEvents(matching:withOptions:)`. */
@HiddenFromObjC
public fun Modifier.handlesGameControllerEvents(types: List<GameControllerEventType> = listOf(GameControllerEventType.gamepad), receivesEventsInView: Boolean? = null): Modifier =
    element(FrameworkIds.GAME_CONTROLLER, types.map { it.ordinal }, receivesEventsInView)

// ---- SpriteKit, SceneKit -----------------------------------------------------------------------------------------

/** SpriteKit `SpriteView.Options`. */
@HiddenFromObjC
public enum class SpriteViewOption { ignoresSiblingOrder, shouldCullNonVisibleNodes, allowsTransparency }

/** SpriteKit `SpriteView.DebugOptions`. */
@HiddenFromObjC
public enum class SpriteViewDebugOption { showsFPS, showsDrawCount, showsNodeCount, showsQuadCount, showsPhysics, showsFields }

/**
 * SpriteKit `SpriteView(scene:transition:isPaused:preferredFramesPerSecond:options:debugOptions:)`: the scene of the
 * `.sks` file [sceneFileName], or of the `SKScene` subclass named [sceneClassName] (an `@objc` Swift class or an
 * Objective-C class) sized to the view.
 */
@HiddenFromObjC
@Composable
public fun SpriteView(
    modifier: Modifier = Modifier,
    sceneFileName: String? = null,
    sceneClassName: String? = null,
    isPaused: Boolean = false,
    preferredFramesPerSecond: Int = 60,
    options: List<SpriteViewOption> = listOf(SpriteViewOption.shouldCullNonVisibleNodes),
    debugOptions: List<SpriteViewDebugOption> = emptyList(),
) {
    SwiftView(FrameworkIds.SPRITE_VIEW, listOf(sceneFileName, sceneClassName, isPaused, preferredFramesPerSecond,
        options.map { it.ordinal }, debugOptions.map { it.ordinal }), modifier)
}

/** SceneKit `SceneView.Options`. */
@HiddenFromObjC
public enum class SceneViewOption { allowsCameraControl, autoenablesDefaultLighting, jitteringEnabled, rendersContinuously, temporalAntialiasingEnabled }

/** SceneKit `SCNAntialiasingMode`. */
@HiddenFromObjC
public enum class SceneAntialiasing { none, multisampling2X, multisampling4X }

/**
 * SceneKit `SceneView(scene:pointOfView:options:preferredFramesPerSecond:antialiasingMode:)`: the scene of
 * [sceneFileName] (`.scn`, `.usdz`… in the app bundle), seen from the node named [pointOfView].
 */
@HiddenFromObjC
@Composable
public fun SceneView(
    sceneFileName: String,
    modifier: Modifier = Modifier,
    pointOfView: String? = null,
    options: List<SceneViewOption> = emptyList(),
    preferredFramesPerSecond: Int = 60,
    antialiasingMode: SceneAntialiasing = SceneAntialiasing.multisampling4X,
) {
    SwiftView(FrameworkIds.SCENE_VIEW, listOf(sceneFileName, pointOfView, options.map { it.ordinal }, preferredFramesPerSecond, antialiasingMode.ordinal), modifier)
}

// ---- PassKit -----------------------------------------------------------------------------------------------------

/** PassKit `PayWithApplePayButtonLabel`. */
@HiddenFromObjC
public enum class ApplePayButtonLabel { plain, buy, addMoney, book, checkout, `continue`, contribute, donate, inStore, order, reload, rent, setUp, subscribe, support, tip, topUp }

/** PassKit `PayWithApplePayButtonStyle`. */
@HiddenFromObjC
public enum class ApplePayButtonStyle { automatic, black, white, whiteOutline }

/** A line of the payment sheet (PassKit `PKPaymentSummaryItem`): the last one is the total, labelled with your company. */
@HiddenFromObjC
public data class PaymentSummaryItem(val label: String, val amount: String, val pending: Boolean = false)

/** A shipping option (PassKit `PKShippingMethod`). */
@HiddenFromObjC
public data class ShippingMethod(val identifier: String, val label: String, val amount: String, val detail: String? = null)

/** A PassKit `PKPaymentRequest`. Amounts are decimal strings ("9.99"); networks are `PKPaymentNetwork` raw values ("Visa"). */
@HiddenFromObjC
public data class ApplePayRequest(
    val merchantIdentifier: String,
    val countryCode: String,
    val currencyCode: String,
    val supportedNetworks: List<String>,
    val items: List<PaymentSummaryItem>,
    val merchantCapabilities: List<String> = listOf("3DS"),
    /** `requiredShippingContactFields` / `requiredBillingContactFields`: "name", "postalAddress", "emailAddress", "phoneNumber". */
    val requiredShippingContactFields: List<String> = emptyList(),
    val requiredBillingContactFields: List<String> = emptyList(),
    val shippingMethods: List<ShippingMethod> = emptyList(),
    val supportsCouponCode: Boolean = false,
) {
    internal fun encode(): List<Any?> = listOf(merchantIdentifier, countryCode, currencyCode, supportedNetworks, items.map { listOf(it.label, it.amount, it.pending) },
        merchantCapabilities, requiredShippingContactFields, requiredBillingContactFields,
        shippingMethods.map { listOf(it.identifier, it.label, it.amount, it.detail) }, supportsCouponCode)
}

/** An authorized payment (PassKit `PKPayment`): the token's data in base64 and the selected contact details. */
@HiddenFromObjC
public data class ApplePayPayment(val tokenData: String, val transactionIdentifier: String, val network: String?, val shippingMethodId: String?, val email: String?)

/** The result of [PayWithApplePayButton]'s authorization (PassKit `PKPaymentAuthorizationResult`). */
@HiddenFromObjC
public enum class PaymentAuthorizationStatus { success, failure }

/**
 * PassKit `PayWithApplePayButton(_:request:onPaymentAuthorizationChange:onMerchantSessionRequested:)`: shows the Apple
 * Pay sheet for [request]; [onAuthorize] processes the payment and returns its status, [onFinish] runs when the sheet
 * closes. [merchantSession] returns the merchant session JSON for Apple Pay on the web. Without a [request], the
 * button calls [action] (`PayWithApplePayButton(_:action:)`). [fallback] shows where Apple Pay isn't available.
 */
@HiddenFromObjC
@Composable
public fun PayWithApplePayButton(
    modifier: Modifier = Modifier,
    label: ApplePayButtonLabel = ApplePayButtonLabel.plain,
    request: ApplePayRequest? = null,
    onAuthorize: (ApplePayPayment) -> PaymentAuthorizationStatus = { PaymentAuthorizationStatus.success },
    onFinish: () -> Unit = {},
    merchantSession: (() -> String)? = null,
    action: () -> Unit = {},
    fallback: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.APPLE_PAY_BUTTON, listOf(label.ordinal, request?.encode(),
        kfn { a -> (a[0] as List<*>).let { p -> onAuthorize(ApplePayPayment(p[0] as String, p[1] as String, p[2] as String?, p[3] as String?, p[4] as String?)).ordinal } },
        SAction(onFinish), merchantSession?.let { f -> kfn { f() } }, SAction(action), fallback?.let { SSlotRef(0) }), modifier, slots = listOfNotNull(fallback))
}

/** PassKit `payWithApplePayButtonStyle(_:)` and `payWithApplePayButtonDisableCardArt()`. */
@HiddenFromObjC
public fun Modifier.payWithApplePayButtonStyle(style: ApplePayButtonStyle? = null, disableCardArt: Boolean = false): Modifier =
    element(FrameworkIds.APPLE_PAY_STYLE, 0, style?.ordinal, disableCardArt)

/** PassKit `addPassToWalletButtonStyle(_:)`: black (true) or black outline. */
@HiddenFromObjC
public fun Modifier.addPassToWalletButtonStyle(blackOutline: Boolean): Modifier = element(FrameworkIds.APPLE_PAY_STYLE, 1, if (blackOutline) 1 else 0, false)

/** PassKit `verifyIdentityWithWalletButtonStyle(_:)`: black (false) or black outline (true). */
@HiddenFromObjC
public fun Modifier.verifyIdentityWithWalletButtonStyle(blackOutline: Boolean): Modifier = element(FrameworkIds.APPLE_PAY_STYLE, 2, if (blackOutline) 1 else 0, false)

/** PassKit `PayLaterViewDisplayStyle`. */
@HiddenFromObjC
public enum class PayLaterDisplayStyle { standard, badge, checkout, price }

/** PassKit `PayLaterViewAction`. */
@HiddenFromObjC
public enum class PayLaterAction { learnMore, calculator }

/** PassKit `payLaterViewDisplayStyle(_:)` and `payLaterViewAction(_:)`. */
@HiddenFromObjC
public fun Modifier.payLaterViewStyle(displayStyle: PayLaterDisplayStyle? = null, action: PayLaterAction? = null): Modifier =
    element(FrameworkIds.APPLE_PAY_STYLE, 3, listOf(displayStyle?.ordinal, action?.ordinal), false)

/**
 * PassKit `onApplePayShippingContactChange`, `onApplePayShippingMethodChange`, `onApplePayPaymentMethodChange` and
 * `onApplePayCouponCodeChange`: each returns the updated summary items (null keeps them).
 */
@HiddenFromObjC
public fun Modifier.onApplePayChange(
    shippingContact: ((postalCode: String?, country: String?) -> List<PaymentSummaryItem>?)? = null,
    shippingMethod: ((identifier: String) -> List<PaymentSummaryItem>?)? = null,
    paymentMethod: ((network: String?) -> List<PaymentSummaryItem>?)? = null,
    couponCode: ((code: String) -> List<PaymentSummaryItem>?)? = null,
): Modifier {
    fun items(l: List<PaymentSummaryItem>?) = l?.map { listOf(it.label, it.amount, it.pending) }
    return element(FrameworkIds.APPLE_PAY_CHANGES,
        shippingContact?.let { f -> kfn { a -> items(f(a[0] as String?, a[1] as String?)) } },
        shippingMethod?.let { f -> kfn { a -> items(f(a[0] as String)) } },
        paymentMethod?.let { f -> kfn { a -> items(f(a[0] as String?)) } },
        couponCode?.let { f -> kfn { a -> items(f(a[0] as String)) } })
}

/**
 * PassKit `AddPassToWalletButton(_:onCompletion:)` with the `.pkpass` files [passes] (base64), or
 * `AddPassToWalletButton(action:)` without.
 */
@HiddenFromObjC
@Composable
public fun AddPassToWalletButton(
    modifier: Modifier = Modifier,
    passes: List<String> = emptyList(),
    onCompletion: (added: Boolean) -> Unit = {},
    action: () -> Unit = {},
    fallback: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.ADD_PASS_BUTTON, listOf(passes, SValueCallback { onCompletion(it as Boolean) }, SAction(action), fallback?.let { SSlotRef(0) }),
        modifier, slots = listOfNotNull(fallback))
}

/** PassKit `PayLaterView(amount:currency:)`: Apple Pay Later merchandising for [amount] ("199.99") in [currency] ("USD"). */
@HiddenFromObjC
@Composable
public fun PayLaterView(amount: String, currency: String, modifier: Modifier = Modifier) {
    SwiftView(FrameworkIds.PAY_LATER, listOf(amount, currency), modifier)
}

/** PassKit `ApplePayMerchandisingPartnerConfiguration`. */
@HiddenFromObjC
public enum class ApplePayMerchandisingPartners { all, limited, none }

/**
 * PassKit `ApplePayMerchandisingView(amount:currency:region:action:style:partners:fallback:)` (iOS 27);
 * [ApplePayMerchandisingPartners.limited] shows the partners of [limitedPartnerIdentifiers].
 */
@HiddenFromObjC
@Composable
public fun ApplePayMerchandisingView(
    amount: String,
    currency: String,
    region: String,
    modifier: Modifier = Modifier,
    action: String = "learnMore",
    style: String = "standard",
    partners: ApplePayMerchandisingPartners = ApplePayMerchandisingPartners.all,
    limitedPartnerIdentifiers: List<String> = emptyList(),
    fallback: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.MERCHANDISING, listOf(amount, currency, region, action, style, partners.ordinal, fallback?.let { SSlotRef(0) }, limitedPartnerIdentifiers),
        modifier, slots = listOfNotNull(fallback))
}

/** PassKit `VerifyIdentityWithWalletButtonLabel`. */
@HiddenFromObjC
public enum class VerifyIdentityLabel { verifyIdentity, verifyAge, verify, `continue` }

/**
 * PassKit `VerifyIdentityWithWalletButton(_:request:onCompletion:)`: requests the driver's license [elements]
 * ("givenName", "familyName", "portrait", "address", "issuingAuthority", "documentNumber", "dateOfBirth",
 * "documentExpirationDate", "age:21" for an age threshold) for [merchantIdentifier] with [nonce] (base64);
 * [onCompletion] receives the encrypted document (base64) or the error. Without [merchantIdentifier] the button calls
 * [action].
 */
@HiddenFromObjC
@Composable
public fun VerifyIdentityWithWalletButton(
    modifier: Modifier = Modifier,
    label: VerifyIdentityLabel = VerifyIdentityLabel.verifyIdentity,
    merchantIdentifier: String? = null,
    nonce: String? = null,
    elements: List<String> = emptyList(),
    onCompletion: (encryptedData: String?, error: String?) -> Unit = { _, _ -> },
    action: () -> Unit = {},
    fallback: (@Composable () -> Unit)? = null,
) {
    SwiftView(FrameworkIds.VERIFY_IDENTITY, listOf(label.ordinal, merchantIdentifier, nonce, elements,
        SValueCallback { v -> (v as List<*>).let { onCompletion(it[0] as String?, it[1] as String?) } }, SAction(action), fallback?.let { SSlotRef(0) }),
        modifier, slots = listOfNotNull(fallback))
}

// ---- MusicKit, DataDetection ----------------------------------------------------------------------------------

/** MusicKit `MusicSubscriptionOffer.MessageIdentifier`. */
@HiddenFromObjC
public enum class MusicOfferMessage { join, addMusic, playMusic }

/**
 * MusicKit `musicSubscriptionOffer(isPresented:options:onLoadCompletion:)`: the Apple Music subscription sheet, for
 * the catalog item [itemId] when given. [onLoadCompletion] receives the error, or null.
 */
@HiddenFromObjC
public fun Modifier.musicSubscriptionOffer(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    messageIdentifier: MusicOfferMessage = MusicOfferMessage.join,
    itemId: String? = null,
    affiliateToken: String? = null,
    campaignToken: String? = null,
    onLoadCompletion: (error: String?) -> Unit = {},
): Modifier = element(FrameworkIds.MUSIC_SUBSCRIPTION, isPresented, SValueCallback { if (it == false) onDismiss() },
    listOf(messageIdentifier.ordinal, itemId, affiliateToken, campaignToken), SValueCallback { onLoadCompletion(it as String?) })

/**
 * MusicKit `musicPicker(isPresented:title:selection:)` (iOS 27): the user picks Apple Music songs; [onPick] receives
 * their catalog ids. [multiple] allows several.
 */
@HiddenFromObjC
public fun Modifier.musicPicker(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    multiple: Boolean = false,
    title: String? = null,
    onPick: (ids: List<String>) -> Unit,
): Modifier = element(FrameworkIds.MUSIC_PICKER, isPresented, SValueCallback { if (it == false) onDismiss() }, multiple, title,
    SValueCallback { v -> onPick((v as List<*>).map { it as String }) })

/** DataDetection `DataDetector.MatchType`. */
@HiddenFromObjC
public enum class DataDetectorType { all, link, emailAddress, phoneNumber, postalAddress, calendarEvent, moneyAmount, measurement, flightNumber, shipmentTrackingNumber, paymentIdentifier }

/** DataDetection `dataDetection(_:options:)` (iOS 27): detects data in the text below and makes it actionable. */
@HiddenFromObjC
public fun Modifier.dataDetection(types: List<DataDetectorType> = listOf(DataDetectorType.all)): Modifier =
    element(FrameworkIds.DATA_DETECTION, types.map { it.ordinal })

// ---- RealityKit, AppIntents, DeviceActivity, WatchKit, ManagedAppDistribution, PermissionKit, ScreenCaptureKit,
// WorkoutKit, DeviceDiscoveryUI --------------------------------------------------------------------------------

internal object MoreFrameworkIds {
    const val REALITY_VIEW = 200_320
    const val SHORTCUTS_LINK = 200_321
    const val ACTIVITY_REPORT = 200_322
    const val NOW_PLAYING = 200_323
    const val MANAGED_CONTENT = 200_324
    const val PERMISSION_BUTTON = 200_325
    const val DEVICE_PICKER = 200_326

    const val REALITY_STYLE = 200_320
    const val SHORTCUTS_STYLE = 200_321
    const val MANAGED_STYLE = 200_322
    const val RECORDING_EDITOR = 200_323
    const val WORKOUT_PREVIEW = 200_324
}

/** RealityKit `CameraControls` of a [RealityView]. */
@HiddenFromObjC
public enum class RealityCameraControls { none, orbit, dolly, pan, tilt }

/** RealityKit `RealityViewLayoutOption`. */
@HiddenFromObjC
public enum class RealityViewLayout { flexible, centered, fixedSize }

/**
 * RealityKit `RealityView(make:update:placeholder:)` (iOS 18 / macOS 15): loads the Reality Composer / USDZ models
 * [models] from the app bundle (`Entity(named:)`) into the scene; [placeholder] shows while they load.
 */
@HiddenFromObjC
@Composable
public fun RealityView(models: List<String>, modifier: Modifier = Modifier, placeholder: (@Composable () -> Unit)? = null) {
    SwiftView(MoreFrameworkIds.REALITY_VIEW, listOf(models, placeholder?.let { SSlotRef(0) }), modifier, slots = listOfNotNull(placeholder))
}

/** RealityKit `realityViewCameraControls(_:)` and `realityViewLayoutBehavior(_:)`. */
@HiddenFromObjC
public fun Modifier.realityViewStyle(cameraControls: RealityCameraControls? = null, layout: RealityViewLayout? = null): Modifier =
    element(MoreFrameworkIds.REALITY_STYLE, cameraControls?.ordinal, layout?.ordinal)

/** AppIntents `ShortcutsLink(action:)`: opens the app's shortcuts in the Shortcuts app. */
@HiddenFromObjC
@Composable
public fun ShortcutsLink(modifier: Modifier = Modifier, action: () -> Unit = {}) {
    SwiftView(MoreFrameworkIds.SHORTCUTS_LINK, listOf(SAction(action)), modifier)
}

/** AppIntents `ShortcutsLinkStyle` / `SiriTipViewStyle`. */
@HiddenFromObjC
public enum class ShortcutsStyle { automatic, automaticOutline, dark, darkOutline, light, lightOutline }

/** AppIntents `shortcutsLinkStyle(_:)` and `siriTipViewStyle(_:)` (automatic, dark or light). */
@HiddenFromObjC
public fun Modifier.shortcutsStyle(shortcutsLink: ShortcutsStyle? = null, siriTip: ShortcutsStyle? = null): Modifier =
    element(MoreFrameworkIds.SHORTCUTS_STYLE, shortcutsLink?.ordinal, siriTip?.ordinal)

/** DeviceActivity `DeviceActivityReport(_:filter:)`: the report [context] of the app's device activity report extension. */
@HiddenFromObjC
@Composable
public fun DeviceActivityReport(context: String, modifier: Modifier = Modifier) {
    SwiftView(MoreFrameworkIds.ACTIVITY_REPORT, listOf(context), modifier)
}

/** WatchKit `NowPlayingView()` (watchOS): the system now playing controls. */
@HiddenFromObjC
@Composable
public fun NowPlayingView(modifier: Modifier = Modifier) {
    SwiftView(MoreFrameworkIds.NOW_PLAYING, emptyList(), modifier)
}

/** ManagedAppDistribution `ManagedContentOfferState`. */
@HiddenFromObjC
public enum class ManagedContentOfferState { notInstalled, neverInstalled, installing, installed, noninteractive }

/** ManagedAppDistribution `ManagedContentStyle`. */
@HiddenFromObjC
public enum class ManagedContentStyle { automatic, compact, header, vertical }

/**
 * ManagedAppDistribution `ManagedContentView(primaryLabel:secondaryLabel:tertiaryLabel:quaternaryLabel:offerState:offerAction:icon:)`
 * (macOS 26.4): an offer row; [offerAction] receives the state when the offer button is pressed.
 */
@HiddenFromObjC
@Composable
public fun ManagedContentView(
    primaryLabel: String,
    offerState: ManagedContentOfferState,
    offerAction: (ManagedContentOfferState) -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String = "",
    tertiaryLabel: String = "",
    quaternaryLabel: String = "",
    icon: @Composable () -> Unit,
) {
    SwiftView(MoreFrameworkIds.MANAGED_CONTENT, listOf(listOf(primaryLabel, secondaryLabel, tertiaryLabel, quaternaryLabel), offerState.ordinal,
        SValueCallback { offerAction(ManagedContentOfferState.entries[(it as Number).toInt()]) }, SSlotRef(0)), modifier, slots = listOf(icon))
}

/** ManagedAppDistribution `managedContentStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.managedContentStyle(style: ManagedContentStyle): Modifier = element(MoreFrameworkIds.MANAGED_STYLE, style.ordinal)

/** A PermissionKit `CommunicationHandle`: a phone number, an email address or a custom handle. */
@HiddenFromObjC
public data class CommunicationHandle(val value: String, val kind: Kind) {
    public enum class Kind { phoneNumber, emailAddress, custom }
}

/**
 * PermissionKit `CommunicationLimitsButton(question:label:)` / `PermissionButton(question:label:)` (iOS 26): asks a
 * parent for permission to communicate with [handles].
 */
@HiddenFromObjC
@Composable
public fun CommunicationLimitsButton(handles: List<CommunicationHandle>, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    SwiftView(MoreFrameworkIds.PERMISSION_BUTTON, listOf(handles.map { listOf(it.value, it.kind.ordinal) }, SSlotRef(0)), modifier, slots = listOf(label))
}

/** ScreenCaptureKit `recordingEditor(_:)` (macOS 27): edits the recording at [fileUrl]; null closes the editor. */
@HiddenFromObjC
public fun Modifier.recordingEditor(fileUrl: String?, onDismiss: () -> Unit): Modifier =
    element(MoreFrameworkIds.RECORDING_EDITOR, fileUrl, SValueCallback { if (it == null) onDismiss() })

/** A WorkoutKit single-goal workout goal. */
@HiddenFromObjC
public sealed class WorkoutGoal {
    internal abstract fun encode(): List<Any?>
    public data object Open : WorkoutGoal() { override fun encode() = listOf(0) }
    public data class DistanceKilometers(val value: Double) : WorkoutGoal() { override fun encode() = listOf(1, value) }
    public data class TimeMinutes(val value: Double) : WorkoutGoal() { override fun encode() = listOf(2, value) }
    public data class EnergyKilocalories(val value: Double) : WorkoutGoal() { override fun encode() = listOf(3, value) }
}

/**
 * WorkoutKit `workoutPreview(_:isPresented:)` (iOS 17 / watchOS 10): previews a single-goal workout of the HealthKit
 * activity type [activityType] (`HKWorkoutActivityType` raw value, e.g. 37 = running) and sends it to Apple Watch.
 */
@HiddenFromObjC
public fun Modifier.workoutPreview(isPresented: Boolean, onDismiss: () -> Unit, activityType: Int, goal: WorkoutGoal = WorkoutGoal.Open): Modifier =
    element(MoreFrameworkIds.WORKOUT_PREVIEW, isPresented, SValueCallback { if (it == false) onDismiss() }, activityType, goal.encode())

/**
 * DeviceDiscoveryUI `DevicePicker(_:onSelect:label:fallback:)` (tvOS): browses for the app service [serviceName];
 * [onSelect] receives the selected endpoint's description.
 */
@HiddenFromObjC
@Composable
public fun DevicePicker(
    serviceName: String,
    onSelect: (endpoint: String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    fallback: @Composable () -> Unit = {},
) {
    SwiftView(MoreFrameworkIds.DEVICE_PICKER, listOf(serviceName, SValueCallback { onSelect(it as String) }, SSlotRef(0), SSlotRef(1)), modifier,
        slots = listOf(label, fallback))
}
