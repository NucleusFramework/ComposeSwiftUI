@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import kotlin.native.HiddenFromObjC

// The SwiftUI APIs that TipKit, WidgetKit / ActivityKit, HealthKitUI, ContactsUI, FamilyControls, FinanceKitUI,
// ImagePlayground, JournalingSuggestions, GameSave, ClassKitUI, SecurityUI, AutomatedDeviceEnrollment, CoreLocation
// and CoreMotion declare in their own module. Swift renderers: swift/ComposeSwiftUIIntegrations/TipKit.swift,
// WidgetKit.swift and AppServices.swift.

internal object AppFrameworkIds {
    const val TIP_VIEW = 200_340
    const val ACCESSORY_WIDGET_BACKGROUND = 200_341
    const val ACCESSORY_WIDGET_GROUP = 200_342
    const val WIDGET_IMAGE = 200_343
    const val CONTACT_ACCESS_BUTTON = 200_344
    const val FAMILY_ACTIVITY_PICKER = 200_345
    const val FAMILY_ACTIVITY_LABEL = 200_346
    const val ADD_ORDER_BUTTON = 200_347
    const val TRANSACTION_PICKER = 200_348
    const val JOURNALING_PICKER = 200_349
    const val ASSIGNED_DOCUMENT_LABEL = 200_350
    const val ASSIGNED_DOCUMENT_BUTTON = 200_351

    const val POPOVER_TIP = 200_340
    const val TIP_STYLE = 200_341
    const val TIP_ANCHOR = 200_342
    const val WIDGET_STYLE = 200_343
    const val WIDGET_LABEL = 200_344
    const val ACTIVITY_STYLE = 200_345
    const val CONTROL_WIDGET_TEXT = 200_346
    const val HEALTH_ACCESS = 200_347
    const val CONTACT_ACCESS_STYLE = 200_348
    const val CONTACT_ACCESS_PICKER = 200_349
    const val FAMILY_PICKER_SHEET = 200_350
    const val ORDER_BUTTON_STYLE = 200_351
    const val TRANSACTION_PICKER_SHEET = 200_352
    const val IMAGE_PLAYGROUND_SHEET = 200_353
    const val IMAGE_PLAYGROUND_STYLE = 200_354
    const val JOURNALING_PICKER_SHEET = 200_355
    const val GAME_SAVE_ALERT = 200_356
    const val ASSIGNED_DOCUMENT_EVENTS = 200_357
    const val CERTIFICATE_SHEET = 200_358
    const val DEVICE_ENROLLMENT = 200_359
    const val SPATIAL_ANCHORS = 200_360
}

// TipKit

/** A TipKit action button of a [Tip]: [id] comes back in the tip's action callback. */
@HiddenFromObjC
public class TipAction(public val id: String, public val title: String)

/**
 * A TipKit `Tip`. TipKit remembers a tip's status (shown, closed) by [id] across launches; call
 * `ComposeIntegrations.registerTipKit()` at launch, which runs `Tips.configure()`.
 *
 * Eligibility rules (TipKit's `#Rule` macros) are Swift types: decide in Kotlin when a tip applies and pass `null`
 * otherwise. [invalidated] = true calls `invalidate(reason: .actionPerformed)` (the user did what the tip suggests:
 * TipKit won't show it again); [maxDisplayCount], [maxDisplayDuration] (seconds, iOS 18) and
 * [ignoresDisplayFrequency] are the tip's `options`.
 */
@HiddenFromObjC
public class Tip(
    public val id: String,
    public val title: String,
    public val message: String? = null,
    public val systemImage: String? = null,
    public val actions: List<TipAction> = emptyList(),
    public val invalidated: Boolean = false,
    public val maxDisplayCount: Int? = null,
    public val maxDisplayDuration: Double? = null,
    public val ignoresDisplayFrequency: Boolean = false,
) {
    internal fun encode(): List<Any?> = listOf(id, title, message, systemImage, actions.map { listOf(it.id, it.title) },
        invalidated, maxDisplayCount, maxDisplayDuration, ignoresDisplayFrequency)
}

/**
 * TipKit `TipView(_:isPresented:arrowEdge:anchorID:action:)`: the tip inline. [onAction] receives the id of the
 * tapped [TipAction]. With [isPresented], Kotlin controls whether it shows.
 */
@HiddenFromObjC
@Composable
public fun TipView(
    tip: Tip?,
    modifier: Modifier = Modifier,
    arrowEdge: Edge? = null,
    isPresented: Boolean? = null,
    onPresentedChange: (Boolean) -> Unit = {},
    anchorId: String? = null,
    onAction: (actionId: String) -> Unit = {},
) {
    SwiftView(AppFrameworkIds.TIP_VIEW, listOf(tip?.encode(), arrowEdge, isPresented, SValueCallback { onPresentedChange(it as Boolean) },
        anchorId, SValueCallback { onAction(it as String) }), modifier)
}

/**
 * TipKit `popoverTip(_:isPresented:attachmentAnchor:arrowEdge:action:)` and its `arrowEdges` variant: the tip in a
 * popover attached to this view.
 */
@HiddenFromObjC
public fun Modifier.popoverTip(
    tip: Tip?,
    arrowEdge: Edge? = null,
    arrowEdges: Edge.Set? = null,
    attachmentAnchor: PopoverAttachmentAnchor? = null,
    isPresented: Boolean? = null,
    onPresentedChange: (Boolean) -> Unit = {},
    onAction: (actionId: String) -> Unit = {},
): Modifier = element(AppFrameworkIds.POPOVER_TIP, tip?.encode(), arrowEdge, arrowEdges, attachmentAnchor, isPresented,
    SValueCallback { onPresentedChange(it as Boolean) }, SValueCallback { onAction(it as String) })

/**
 * The TipKit style modifiers: `tipBackground`, `tipCornerRadius(_:antialiased:)`, `tipImageSize`, `tipImageStyle`
 * (one to three styles), `tipBackgroundInteraction` and `tipViewStyle(.miniTip)`.
 */
@HiddenFromObjC
public fun Modifier.tipStyle(
    background: ShapeStyle? = null,
    cornerRadius: Double? = null,
    antialiased: Boolean = true,
    imageSize: CGSize? = null,
    imageStyles: List<ShapeStyle> = emptyList(),
    backgroundInteraction: PresentationBackgroundInteraction? = null,
    miniTip: Boolean = false,
): Modifier = element(AppFrameworkIds.TIP_STYLE, background, cornerRadius, antialiased, imageSize, imageStyles, backgroundInteraction, miniTip)

/** TipKit `tipAnchor(_:)` (iOS 26): the view a `TipView(anchorId = id)` points at. */
@HiddenFromObjC
public fun Modifier.tipAnchor(id: String): Modifier = element(AppFrameworkIds.TIP_ANCHOR, id)

// WidgetKit and ActivityKit: view APIs for widget and Live Activity content.

/** WidgetKit `AccessoryWidgetGroupStyle`. */
@HiddenFromObjC
public enum class AccessoryWidgetGroupStyle { automatic, circular, roundedSquare }

/** WidgetKit `WidgetAccentedRenderingMode`. */
@HiddenFromObjC
public enum class WidgetAccentedRenderingMode { accented, desaturated, accentedDesaturated, fullColor }

/**
 * The WidgetKit view modifiers: `widgetAccentable`, `widgetURL`, `widgetCurvesContent`,
 * `containerBackground(_:for: .widget)` and, on watchOS, `accessoryWidgetGroupStyle`.
 */
@HiddenFromObjC
public fun Modifier.widgetStyle(
    accentable: Boolean? = null,
    url: String? = null,
    curvesContent: Boolean? = null,
    containerBackground: ShapeStyle? = null,
    accessoryGroupStyle: AccessoryWidgetGroupStyle? = null,
): Modifier = element(AppFrameworkIds.WIDGET_STYLE, accentable, url, curvesContent, containerBackground, accessoryGroupStyle?.ordinal)

/** WidgetKit `widgetLabel(_:)` (iOS / watchOS accessory widgets): [text], or the [label] view. */
@HiddenFromObjC
public fun Modifier.widgetLabel(text: String): Modifier = element(AppFrameworkIds.WIDGET_LABEL, text, null)

/** WidgetKit `widgetLabel(label:)`. */
@HiddenFromObjC
public fun Modifier.widgetLabel(label: @Composable () -> Unit): Modifier =
    element(AppFrameworkIds.WIDGET_LABEL, null, slotContent(label))

/**
 * ActivityKit Live Activity modifiers: `activityBackgroundTint`, `activitySystemActionForegroundColor` and
 * `dynamicIsland(verticalPlacement: .belowIfTooWide)` (iOS).
 */
@HiddenFromObjC
public fun Modifier.activityStyle(
    backgroundTint: Color? = null,
    systemActionForegroundColor: Color? = null,
    belowIfTooWide: Boolean = false,
): Modifier = element(AppFrameworkIds.ACTIVITY_STYLE, backgroundTint, systemActionForegroundColor, belowIfTooWide)

/** WidgetKit `controlWidgetStatus(_:)` / `controlWidgetActionHint(_:)`: texts of a control's view. */
@HiddenFromObjC
public fun Modifier.controlWidgetText(status: String? = null, actionHint: String? = null): Modifier =
    element(AppFrameworkIds.CONTROL_WIDGET_TEXT, status, actionHint)

/** WidgetKit `AccessoryWidgetBackground()`. */
@HiddenFromObjC
@Composable
public fun AccessoryWidgetBackground(modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.ACCESSORY_WIDGET_BACKGROUND, emptyList(), modifier)
}

/**
 * WidgetKit `AccessoryWidgetGroup(_:systemImage:content:)` / `(_:image:content:)` / `(label:content:)` (watchOS):
 * [title] with an SF Symbol or asset image, or a [label] view.
 */
@HiddenFromObjC
@Composable
public fun AccessoryWidgetGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    systemImage: String? = null,
    image: String? = null,
    label: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    SwiftView(AppFrameworkIds.ACCESSORY_WIDGET_GROUP, listOf(title, systemImage, image, label?.let { SSlotRef(0) }), modifier,
        slots = listOfNotNull(label), content = content)
}

/** An `Image` with WidgetKit `widgetAccentedRenderingMode(_:)`: an asset [name] or an SF Symbol [systemName]. */
@HiddenFromObjC
@Composable
public fun WidgetImage(name: String? = null, systemName: String? = null, renderingMode: WidgetAccentedRenderingMode, modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.WIDGET_IMAGE, listOf(name, systemName, renderingMode.ordinal), modifier)
}

// HealthKitUI

/**
 * HealthKitUI `healthDataAccessRequest(store:shareTypes:readTypes:trigger:completion:)` (iOS 17 / watchOS 10): asks
 * for access to the HealthKit types named by their identifiers (`HKQuantityTypeIdentifierStepCount`,
 * `HKCategoryTypeIdentifierSleepAnalysis`, `HKWorkoutTypeIdentifier`…) each time [trigger] changes. With
 * [objectType], `healthDataAccessRequest(store:objectType:predicate:trigger:completion:)` asks per-object access.
 * [onResult] receives null on success or the error message.
 */
@HiddenFromObjC
public fun Modifier.healthDataAccessRequest(
    readTypes: List<String>,
    shareTypes: List<String> = emptyList(),
    trigger: Any?,
    objectType: String? = null,
    onResult: (error: String?) -> Unit,
): Modifier = element(AppFrameworkIds.HEALTH_ACCESS, readTypes, shareTypes, trigger, objectType, SValueCallback { onResult(it as String?) })

// ContactsUI

/** ContactsUI `ContactAccessButton.Caption`. */
@HiddenFromObjC
public enum class ContactAccessCaption { defaultText, email, phone }

/**
 * ContactsUI `ContactAccessButton(queryString:ignoredEmails:ignoredPhoneNumbers:approvalCallback:)` (iOS 18): grants
 * the app access to the contacts matching [query]; [onApproval] receives their identifiers.
 */
@HiddenFromObjC
@Composable
public fun ContactAccessButton(
    query: String,
    modifier: Modifier = Modifier,
    ignoredEmails: List<String>? = null,
    ignoredPhoneNumbers: List<String>? = null,
    onApproval: (contactIds: List<String>) -> Unit = {},
) {
    @Suppress("UNCHECKED_CAST")
    SwiftView(AppFrameworkIds.CONTACT_ACCESS_BUTTON, listOf(query, ignoredEmails, ignoredPhoneNumbers,
        SValueCallback { onApproval(it as List<String>) }), modifier)
}

/** ContactsUI `contactAccessButtonCaption(_:)` and `contactAccessButtonStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.contactAccessButtonStyle(
    caption: ContactAccessCaption? = null,
    imageTrailingEdgePadding: Double? = null,
    imageWidth: Double? = null,
    imageColor: Color? = null,
): Modifier = element(AppFrameworkIds.CONTACT_ACCESS_STYLE, caption?.ordinal, imageTrailingEdgePadding, imageWidth, imageColor)

/** ContactsUI `contactAccessPicker(isPresented:completionHandler:)` (iOS 18): [onCompletion] receives contact ids. */
@HiddenFromObjC
public fun Modifier.contactAccessPicker(isPresented: Boolean, onDismiss: () -> Unit, onCompletion: (contactIds: List<String>) -> Unit): Modifier {
    @Suppress("UNCHECKED_CAST")
    return element(AppFrameworkIds.CONTACT_ACCESS_PICKER, isPresented, SValueCallback { if (it == false) onDismiss() },
        SValueCallback { onCompletion(it as List<String>) })
}

// FamilyControls: a FamilyActivitySelection travels as its JSON encoding (it is Codable), opaque to Kotlin.

/** FamilyControls `FamilyActivityPicker(headerText:footerText:selection:)` (iOS): Screen Time apps and websites. */
@HiddenFromObjC
@Composable
public fun FamilyActivityPicker(
    selection: String?,
    onSelectionChange: (selectionJson: String) -> Unit,
    modifier: Modifier = Modifier,
    headerText: String? = null,
    footerText: String? = null,
) {
    SwiftView(AppFrameworkIds.FAMILY_ACTIVITY_PICKER, listOf(selection, SValueCallback { onSelectionChange(it as String) }, headerText, footerText), modifier)
}

/** FamilyControls `familyActivityPicker(title:headerText:footerText:isPresented:selection:)` (iOS; `title` needs iOS 26.2). */
@HiddenFromObjC
public fun Modifier.familyActivityPicker(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    selection: String?,
    onSelectionChange: (selectionJson: String) -> Unit,
    title: String? = null,
    headerText: String? = null,
    footerText: String? = null,
): Modifier = element(AppFrameworkIds.FAMILY_PICKER_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() }, selection,
    SValueCallback { onSelectionChange(it as String) }, title, headerText, footerText)

/** The kind of a FamilyControls token. */
@HiddenFromObjC
public enum class FamilyActivityTokenKind { application, category, webDomain }

/** FamilyControls `Label(_:)` of an application, category or web domain token ([tokenJson]: its JSON encoding). */
@HiddenFromObjC
@Composable
public fun FamilyActivityLabel(tokenJson: String, kind: FamilyActivityTokenKind, modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.FAMILY_ACTIVITY_LABEL, listOf(tokenJson, kind.ordinal), modifier)
}

// FinanceKitUI

/** FinanceKitUI `AddOrderToWalletButtonStyle`. */
@HiddenFromObjC
public enum class AddOrderToWalletButtonStyle { black, blackOutline }

/**
 * FinanceKitUI `AddOrderToWalletButton(signedArchive:onCompletion:)` (iOS): adds the signed order package
 * [signedArchive] (base64) to Wallet. [onCompletion] receives "added", "cancelled", "newerExisting" or the error.
 */
@HiddenFromObjC
@Composable
public fun AddOrderToWalletButton(signedArchive: String, onCompletion: (result: String) -> Unit, modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.ADD_ORDER_BUTTON, listOf(signedArchive, SValueCallback { onCompletion(it as String) }), modifier)
}

/** FinanceKitUI `addOrderToWalletButtonStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.addOrderToWalletButtonStyle(style: AddOrderToWalletButtonStyle): Modifier =
    element(AppFrameworkIds.ORDER_BUTTON_STYLE, style.ordinal)

/** A FinanceKit `Transaction` picked by the user (amount in [currencyCode], date in epoch seconds). */
@HiddenFromObjC
public class FinanceTransaction(
    public val id: String,
    public val description: String,
    public val merchantName: String?,
    public val amount: Double,
    public val currencyCode: String,
    public val date: Double,
    public val isCredit: Boolean,
)

@Suppress("UNCHECKED_CAST")
private fun transactions(v: Any?): List<FinanceTransaction> = (v as List<List<Any?>>).map {
    FinanceTransaction(it[0] as String, it[1] as String, it[2] as String?, (it[3] as Number).toDouble(), it[4] as String,
        (it[5] as Number).toDouble(), it[6] as Boolean)
}

/** FinanceKitUI `TransactionPicker(selection:label:)` (iOS): [onSelection] receives the picked transactions. */
@HiddenFromObjC
@Composable
public fun TransactionPicker(onSelection: (List<FinanceTransaction>) -> Unit, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    SwiftView(AppFrameworkIds.TRANSACTION_PICKER, listOf(SValueCallback { onSelection(transactions(it)) }, SSlotRef(0)), modifier, slots = listOf(label))
}

/** FinanceKitUI `transactionPicker(isPresented:selection:)` (iOS). */
@HiddenFromObjC
public fun Modifier.transactionPicker(isPresented: Boolean, onDismiss: () -> Unit, onSelection: (List<FinanceTransaction>) -> Unit): Modifier =
    element(AppFrameworkIds.TRANSACTION_PICKER_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() },
        SValueCallback { onSelection(transactions(it)) })

// ImagePlayground

/** An ImagePlayground `ImagePlaygroundConcept`: a short [text], or a concept extracted from a longer text. */
@HiddenFromObjC
public class ImagePlaygroundConcept private constructor(internal val encoded: List<Any?>) {
    public companion object {
        public fun text(text: String): ImagePlaygroundConcept = ImagePlaygroundConcept(listOf(0, text, null))
        public fun extracted(from: String, title: String? = null): ImagePlaygroundConcept = ImagePlaygroundConcept(listOf(1, from, title))
    }
}

/** ImagePlayground `ImagePlaygroundStyle` (`any` needs iOS 27, `externalProvider` iOS 26). */
@HiddenFromObjC
public enum class ImagePlaygroundStyle { animation, illustration, sketch, emoji, externalProvider, any }

/** ImagePlayground `ImagePlaygroundPersonalizationPolicy` / `ImagePlaygroundOptions.Personalization`. */
@HiddenFromObjC
public enum class ImagePlaygroundPersonalization { automatic, enabled, disabled }

/** ImagePlayground `ImagePlaygroundOptions` (iOS 26). */
@HiddenFromObjC
public class ImagePlaygroundOptions(
    /** `CreationStrategy`: 0 automatic, 1 editExisting, 2 generateNew (iOS 27). */
    public val creationStrategy: Int? = null,
    /** `CreationVariety`: 0 automatic, 1 high, 2 low (iOS 27). */
    public val creationVariety: Int? = null,
    public val personalization: ImagePlaygroundPersonalization? = null,
    /** `SizeSpecification.closest(to:)` (iOS 27). */
    public val closestSize: CGSize? = null,
) {
    internal fun encode(): List<Any?> = listOf(creationStrategy, creationVariety, personalization?.ordinal, closestSize)
}

/**
 * ImagePlayground `imagePlaygroundSheet(isPresented:concepts:sourceImage:onCompletion:onAdaptiveImageGlyphCreation:onCancellation:)`
 * and its `concept:` / `sourceImageURL:` variants (iOS / macOS): [onCompletion] receives the file URL of the created
 * image. [sourceImage] is an asset name, [sourceImageUrl] a file URL. With [onAdaptiveImageGlyph], the sheet can
 * also create a Genmoji, received as the file URL of its image.
 */
@HiddenFromObjC
public fun Modifier.imagePlaygroundSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    concepts: List<ImagePlaygroundConcept> = emptyList(),
    sourceImage: String? = null,
    sourceImageUrl: String? = null,
    onCancellation: () -> Unit = {},
    onAdaptiveImageGlyph: ((url: String) -> Unit)? = null,
    onCompletion: (url: String) -> Unit,
): Modifier = element(AppFrameworkIds.IMAGE_PLAYGROUND_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() },
    concepts.map { it.encoded }, sourceImage, sourceImageUrl, SValueCallback { onCompletion(it as String) }, SValueCallback { onCancellation() },
    onAdaptiveImageGlyph?.let { f -> SValueCallback { f(it as String) } })

/**
 * ImagePlayground `imagePlaygroundGenerationStyle(_:in:)`, `imagePlaygroundPersonalizationPolicy(_:)` and
 * `imagePlaygroundOptions(_:)` for the sheets below this view.
 */
@HiddenFromObjC
public fun Modifier.imagePlaygroundStyle(
    generationStyle: ImagePlaygroundStyle? = null,
    allowedStyles: List<ImagePlaygroundStyle>? = null,
    personalizationPolicy: ImagePlaygroundPersonalization? = null,
    options: ImagePlaygroundOptions? = null,
): Modifier = element(AppFrameworkIds.IMAGE_PLAYGROUND_STYLE, generationStyle?.ordinal, allowedStyles?.map { it.ordinal },
    personalizationPolicy?.ordinal, options?.encode())

// JournalingSuggestions

/** A JournalingSuggestions `JournalingSuggestion`: its title, date interval (epoch seconds) and item count. */
@HiddenFromObjC
public class JournalingSuggestion(public val title: String, public val start: Double?, public val end: Double?, public val itemCount: Int)

private fun suggestion(v: Any?): JournalingSuggestion {
    val l = v as List<*>
    return JournalingSuggestion(l[0] as String, (l[1] as Number?)?.toDouble(), (l[2] as Number?)?.toDouble(), (l[3] as Number).toInt())
}

/** JournalingSuggestions `JournalingSuggestionsPicker(_:onCompletion:)` / `(label:onCompletion:)` (iOS devices). */
@HiddenFromObjC
@Composable
public fun JournalingSuggestionsPicker(
    title: String? = null,
    onCompletion: (JournalingSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
) {
    SwiftView(AppFrameworkIds.JOURNALING_PICKER, listOf(title, SValueCallback { onCompletion(suggestion(it)) }, label?.let { SSlotRef(0) }),
        modifier, slots = listOfNotNull(label))
}

/** JournalingSuggestions `journalingSuggestionsPicker(isPresented:onCompletion:)` (iOS devices). */
@HiddenFromObjC
public fun Modifier.journalingSuggestionsPicker(isPresented: Boolean, onDismiss: () -> Unit, onCompletion: (JournalingSuggestion) -> Unit): Modifier =
    element(AppFrameworkIds.JOURNALING_PICKER_SHEET, isPresented, SValueCallback { if (it == false) onDismiss() },
        SValueCallback { onCompletion(suggestion(it)) })

// GameSave, ClassKitUI, SecurityUI, AutomatedDeviceEnrollment, CoreLocation, CoreMotion

/**
 * GameSave `gameSaveSyncingAlert(directory:finishedLoading:)` (iOS / macOS 27): while [open], opens the synced
 * save directory of [containerIdentifier] and shows the syncing alert; [onFinishedLoading] receives its file URL.
 */
@HiddenFromObjC
public fun Modifier.gameSaveSyncingAlert(open: Boolean, containerIdentifier: String? = null, onFinishedLoading: (directoryUrl: String) -> Unit): Modifier =
    element(AppFrameworkIds.GAME_SAVE_ALERT, open, containerIdentifier, SValueCallback { onFinishedLoading(it as String) })

/** ClassKitUI `AssignedDocumentLabel.Role`. */
@HiddenFromObjC
public enum class AssignedDocumentRole { status, date }

/** ClassKitUI `AssignedDocumentLabel(role:documentURL:)` (iOS / macOS 27). */
@HiddenFromObjC
@Composable
public fun AssignedDocumentLabel(role: AssignedDocumentRole, documentUrl: String, modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.ASSIGNED_DOCUMENT_LABEL, listOf(role.ordinal, documentUrl), modifier)
}

/** ClassKitUI `AssignedDocumentSubmissionButton(documentURL:)` (iOS / macOS 27). */
@HiddenFromObjC
@Composable
public fun AssignedDocumentSubmissionButton(documentUrl: String, modifier: Modifier = Modifier) {
    SwiftView(AppFrameworkIds.ASSIGNED_DOCUMENT_BUTTON, listOf(documentUrl), modifier)
}

/**
 * ClassKitUI `onAssignedDocumentWillSubmit` / `DidSubmit` / `WillWithdraw` / `DidWithdraw`: the "will" handlers
 * return whether to go on, synchronously.
 */
@HiddenFromObjC
public fun Modifier.onAssignedDocument(
    willSubmit: ((url: String) -> Boolean)? = null,
    didSubmit: ((url: String) -> Unit)? = null,
    willWithdraw: ((url: String) -> Boolean)? = null,
    didWithdraw: ((url: String) -> Unit)? = null,
): Modifier = element(AppFrameworkIds.ASSIGNED_DOCUMENT_EVENTS,
    willSubmit?.let { f -> kfn { f(it[0] as String) } }, didSubmit?.let { f -> SValueCallback { f(it as String) } },
    willWithdraw?.let { f -> kfn { f(it[0] as String) } }, didWithdraw?.let { f -> SValueCallback { f(it as String) } })

/**
 * SecurityUI `certificateSheet(trust:title:message:help:)`: shows the trust built from [certificates] (base64 DER,
 * leaf first, basic X.509 policy) while non-null.
 */
@HiddenFromObjC
public fun Modifier.certificateSheet(
    certificates: List<String>?,
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    helpUrl: String? = null,
): Modifier = element(AppFrameworkIds.CERTIFICATE_SHEET, certificates, SValueCallback { onDismiss() }, title, message, helpUrl)

/** AutomatedDeviceEnrollment `automatedDeviceEnrollmentAddition(isPresented:)` (iOS). */
@HiddenFromObjC
public fun Modifier.automatedDeviceEnrollmentAddition(isPresented: Boolean, onDismiss: () -> Unit): Modifier =
    element(AppFrameworkIds.DEVICE_ENROLLMENT, isPresented, SValueCallback { if (it == false) onDismiss() })

/**
 * CoreLocation `locationHeadingAnchor(manager:isEnabled:)`, CoreMotion `deviceMotionAnchor(manager:isEnabled:)` and
 * NearbyInteraction `nearbyInteractionOriginAnchor(session:isEnabled:)` (iOS 27.2 devices). Pass the app's own
 * `CLLocationManager` / `CMMotionManager` (app-wide ones otherwise) and a running `NISession`, created from Kotlin
 * through the platform interop.
 */
@HiddenFromObjC
public fun Modifier.spatialAnchors(
    locationHeading: Boolean? = null,
    deviceMotion: Boolean? = null,
    locationManager: Any? = null,
    motionManager: Any? = null,
    nearbyInteractionSession: Any? = null,
    nearbyInteraction: Boolean = true,
): Modifier = element(AppFrameworkIds.SPATIAL_ANCHORS, locationHeading, deviceMotion, locationManager, motionManager, nearbyInteractionSession, nearbyInteraction)
