@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// StoreKit for SwiftUI (merchandising views, purchase callbacks, store sheets). Rendered by
// swift/ComposeSwiftUIIntegrations/StoreKit.swift: add that file to the app and call
// `ComposeIntegrations.registerStoreKit()` once at launch. Products are identified by their App Store Connect ids.

internal object StoreKitIds {
    const val PRODUCT_VIEW = 200_200
    const val STORE_VIEW = 200_201
    const val SUBSCRIPTION_STORE_VIEW = 200_202
    const val STORE_CONTENT = 200_203
    const val OFFER_VIEW = 200_204

    const val PRODUCT_VIEW_STYLE = 200_200
    const val STORE_STYLE = 200_201
    const val OFFER_VIEW_STYLE = 200_202
    const val SIGN_IN_ACTION = 200_203
    const val POLICY = 200_204
    const val CONTROL_ICON = 200_205
    const val SHEETS = 200_206
    const val PURCHASE_CALLBACKS = 200_207
    const val OFFERS = 200_208
    const val TASKS = 200_209
    const val ACTIONS = 200_210
    const val ICON_BORDER = 200_211
}

internal fun kfn(body: (List<Any?>) -> Any?): KotlinFunction = object : KotlinFunction {
    override fun invoke(args: List<Any?>): Any? = body(args)
}

/** A StoreKit `Product` as seen from Kotlin. */
@HiddenFromObjC
public data class StoreProduct(
    val id: String,
    val displayName: String,
    val description: String,
    val displayPrice: String,
    val price: Double,
    /** `Product.ProductType` raw value: "Consumable", "Non-Consumable", "Auto-Renewable Subscription"… */
    val type: String,
    val subscriptionGroupId: String?,
    val isFamilyShareable: Boolean,
) {
    internal companion object {
        fun decode(v: Any?): StoreProduct? {
            val l = v as? List<*> ?: return null
            return StoreProduct(l[0] as String, l[1] as String, l[2] as String, l[3] as String, (l[4] as Number).toDouble(),
                l[5] as String, l[6] as String?, l[7] as Boolean)
        }
    }
}

/** A StoreKit `Transaction`: ids, dates in epoch seconds, and whether StoreKit verified it. */
@HiddenFromObjC
public data class StoreTransaction(
    val id: Long,
    val originalId: Long,
    val productId: String,
    val purchaseDate: Double,
    val expirationDate: Double?,
    val revocationDate: Double?,
    val isUpgraded: Boolean,
    val ownershipType: String,
    val verified: Boolean,
) {
    internal companion object {
        fun decode(v: Any?): StoreTransaction? {
            val l = v as? List<*> ?: return null
            return StoreTransaction((l[0] as Number).toLong(), (l[1] as Number).toLong(), l[2] as String, (l[3] as Number).toDouble(),
                (l[4] as Number?)?.toDouble(), (l[5] as Number?)?.toDouble(), l[6] as Boolean, l[7] as String, l[8] as Boolean)
        }
    }
}

/** StoreKit `Product.PurchaseResult`, or the error of a failed purchase. */
@HiddenFromObjC
public sealed class StorePurchaseResult {
    public data class Success(val transaction: StoreTransaction) : StorePurchaseResult()
    public data object UserCancelled : StorePurchaseResult()
    public data object Pending : StorePurchaseResult()
    public data class Failure(val message: String) : StorePurchaseResult()

    internal companion object {
        fun decode(v: Any?): StorePurchaseResult {
            val l = v as? List<*> ?: return Failure("unknown")
            return when ((l[0] as Number).toInt()) {
                0 -> StoreTransaction.decode(l[1])?.let { Success(it) } ?: Failure("unverified transaction")
                1 -> UserCancelled
                2 -> Pending
                else -> Failure(l.getOrNull(1) as? String ?: "unknown")
            }
        }
    }
}

/** StoreKit `Product.TaskState` / `Product.CollectionTaskState` / `EntitlementTaskState`. */
@HiddenFromObjC
public sealed class StoreTaskState<out T> {
    public data object Loading : StoreTaskState<Nothing>()
    public data object Unavailable : StoreTaskState<Nothing>()
    public data class Failure(val message: String) : StoreTaskState<Nothing>()
    public data class Success<T>(val value: T, val unavailableIds: List<String> = emptyList()) : StoreTaskState<T>()
}

/** One status of a subscription group (StoreKit `Product.SubscriptionInfo.Status`). */
@HiddenFromObjC
public data class StoreSubscriptionStatus(
    /** `RenewalState` raw value: subscribed, expired, inBillingRetryPeriod, inGracePeriod, revoked. */
    val state: String,
    val transaction: StoreTransaction?,
    val willAutoRenew: Boolean?,
    val autoRenewProductId: String?,
)

/** StoreKit `Product.SubscriptionRelationship`. */
@HiddenFromObjC
public data class SubscriptionRelationship(val rawValue: Int) {
    public operator fun plus(other: SubscriptionRelationship): SubscriptionRelationship = SubscriptionRelationship(rawValue or other.rawValue)

    public companion object {
        public val current: SubscriptionRelationship = SubscriptionRelationship(1)
        public val upgrade: SubscriptionRelationship = SubscriptionRelationship(2)
        public val downgrade: SubscriptionRelationship = SubscriptionRelationship(4)
        public val crossgrade: SubscriptionRelationship = SubscriptionRelationship(8)
        public val all: SubscriptionRelationship = SubscriptionRelationship(15)
    }
}

/** StoreKit `ProductViewStyle`. */
@HiddenFromObjC
public enum class ProductViewStyle { automatic, compact, regular, large }

/** StoreKit `SubscriptionStoreControlStyle`. */
@HiddenFromObjC
public enum class SubscriptionStoreControlStyle { automatic, buttons, picker, prominentPicker, compactPicker, pagedPicker, pagedProminentPicker }

/** The placement of a [SubscriptionStoreControlStyle]. */
@HiddenFromObjC
public enum class SubscriptionStoreControlPlacement { automatic, scrollView, bottomBar, buttonsInBottomBar }

/** StoreKit `ContainerBackgroundPlacement.subscriptionStore`: `Modifier.containerBackground(style, for = …)` in a store. */
public val ContainerBackgroundPlacement.Companion.subscriptionStore: ContainerBackgroundPlacement get() = ContainerBackgroundPlacement(900, emptyList())

/** StoreKit `ContainerBackgroundPlacement.subscriptionStoreHeader`. */
public val ContainerBackgroundPlacement.Companion.subscriptionStoreHeader: ContainerBackgroundPlacement get() = ContainerBackgroundPlacement(901, emptyList())

/** StoreKit `ContainerBackgroundPlacement.subscriptionStoreFullHeight`. */
public val ContainerBackgroundPlacement.Companion.subscriptionStoreFullHeight: ContainerBackgroundPlacement get() = ContainerBackgroundPlacement(902, emptyList())

/** StoreKit `SubscriptionOptionGroupStyle`. */
@HiddenFromObjC
public enum class SubscriptionOptionGroupStyle { automatic, links, tabs }

/** StoreKit `SubscriptionStoreButtonLabel`. */
@HiddenFromObjC
public enum class SubscriptionStoreButtonLabel { automatic, action, displayName, price, multiline, singleLine }

/** StoreKit `StoreButtonKind`. */
@HiddenFromObjC
public enum class StoreButtonKind { cancellation, policies, redeemCode, restorePurchases, signIn }

/** StoreKit `SubscriptionStoreControlBackground`, or a [ShapeStyle] in [StoreContentStyle.controlBackgroundStyle]. */
@HiddenFromObjC
public enum class SubscriptionStoreControlBackground { automatic, gradientMaterial, gradientMaterialOnScroll }

/** StoreKit `SubscriptionStorePolicyKind`. */
@HiddenFromObjC
public enum class SubscriptionStorePolicyKind { privacyPolicy, termsOfService }

/** StoreKit `SubscriptionOfferViewStyle`. */
@HiddenFromObjC
public enum class SubscriptionOfferViewStyle { automatic, compact }

/**
 * The StoreKit store modifiers. On a store view (`style` or [storeStyle]) they all apply; on a [SubscriptionOptionGroup]
 * or a section (`StoreContent`) only [productDescription], [storeButtons] and [buttonLabel] do.
 */
@HiddenFromObjC
public data class StoreContentStyle(
    val productDescription: Visibility? = null,
    /** `storeButton(_:for:)`: the visibility of each button kind. */
    val storeButtons: Map<StoreButtonKind, Visibility> = emptyMap(),
    val buttonLabel: SubscriptionStoreButtonLabel? = null,
    val controlStyle: SubscriptionStoreControlStyle? = null,
    val controlPlacement: SubscriptionStoreControlPlacement = SubscriptionStoreControlPlacement.automatic,
    val controlBackground: SubscriptionStoreControlBackground? = null,
    val controlBackgroundStyle: ShapeStyle? = null,
    val pickerItemBackground: ShapeStyle? = null,
    val pickerItemShape: Shape? = null,
    val optionGroupStyle: SubscriptionOptionGroupStyle? = null,
) {
    internal fun encode(): List<Any?> = listOf(productDescription, storeButtons.entries.map { listOf(it.key.ordinal, it.value) },
        buttonLabel?.ordinal, controlStyle?.ordinal, controlPlacement.ordinal, controlBackground?.ordinal, controlBackgroundStyle,
        pickerItemBackground, pickerItemShape, optionGroupStyle?.ordinal)
}

/** The phase of a product icon (StoreKit `ProductIconPhase`); place [StyleContent] for the loaded image. */
@HiddenFromObjC
public enum class ProductIconPhase { loading, success, failure, unavailable }

/**
 * StoreKit `ProductView(id:prefersPromotionalIcon:icon:placeholderIcon:)`: loads and merchandises one product.
 * [iconPhase] replaces [icon] / [placeholderIcon] with content per load phase (`ProductView(id:icon:placeholderIcon:)`).
 */
@HiddenFromObjC
@Composable
public fun ProductView(
    id: String,
    modifier: Modifier = Modifier,
    prefersPromotionalIcon: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    placeholderIcon: (@Composable () -> Unit)? = null,
    iconPhase: (@Composable (ProductIconPhase) -> Unit)? = null,
) {
    var phase by remember { mutableStateOf(ProductIconPhase.loading) }
    val phased: (@Composable () -> Unit)? = iconPhase?.let { c -> { c(phase) } }
    val slots = listOfNotNull(icon, placeholderIcon, phased)
    var next = 0
    SwiftView(StoreKitIds.PRODUCT_VIEW, listOf(id, prefersPromotionalIcon, icon?.let { SSlotRef(next++) },
        placeholderIcon?.let { SSlotRef(next++) }, phased?.let { SSlotRef(next++) },
        phased?.let { SValueCallback { v -> phase = ProductIconPhase.entries[(v as Number).toInt()] } }), modifier, slots = slots)
}

/**
 * StoreKit `StoreView(ids:prefersPromotionalIcon:icon:placeholderIcon:)`: a store of [ids]. [icons] gives the icon
 * of each product id.
 */
@HiddenFromObjC
@Composable
public fun StoreView(
    ids: List<String>,
    modifier: Modifier = Modifier,
    prefersPromotionalIcon: Boolean = false,
    icons: Map<String, @Composable () -> Unit> = emptyMap(),
    placeholderIcon: (@Composable () -> Unit)? = null,
    style: StoreContentStyle = StoreContentStyle(),
) {
    val keys = icons.keys.toList()
    val slots = keys.map { icons.getValue(it) } + listOfNotNull(placeholderIcon)
    SwiftView(StoreKitIds.STORE_VIEW, listOf(ids, prefersPromotionalIcon, keys, placeholderIcon?.let { SSlotRef(keys.size) }, style.encode()),
        modifier, slots = slots)
}

/**
 * StoreKit `SubscriptionStoreView(groupID:visibleRelationships:…)` or `(productIDs:…)`: [marketingContent] is the
 * header; [content] holds [SubscriptionOptionGroup], [SubscriptionOptionSection], [SubscriptionPeriodGroupSet] and
 * [SubscriptionOptionGroupSet] (`StoreContent`, up to eight at the top level).
 */
@HiddenFromObjC
@Composable
public fun SubscriptionStoreView(
    modifier: Modifier = Modifier,
    groupId: String? = null,
    productIds: List<String> = emptyList(),
    visibleRelationships: SubscriptionRelationship = SubscriptionRelationship.all,
    style: StoreContentStyle = StoreContentStyle(),
    marketingContent: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    SwiftView(StoreKitIds.SUBSCRIPTION_STORE_VIEW, listOf(groupId, productIds, visibleRelationships.rawValue,
        marketingContent?.let { SSlotRef(0) }, content != null, style.encode()), modifier, slots = listOfNotNull(marketingContent),
        content = content ?: {})
}

/**
 * StoreKit `SubscriptionOptionGroup`: the subscriptions [include] accepts (all when null), or the nested [sections],
 * under [label] / [labelContent], with [marketingContent].
 */
@HiddenFromObjC
@Composable
public fun SubscriptionOptionGroup(
    label: String? = null,
    include: ((productId: String) -> Boolean)? = null,
    style: StoreContentStyle = StoreContentStyle(),
    labelContent: (@Composable () -> Unit)? = null,
    marketingContent: (@Composable () -> Unit)? = null,
    sections: (@Composable () -> Unit)? = null,
) {
    val slots = listOfNotNull(labelContent, marketingContent)
    var next = 0
    SwiftView(StoreKitIds.STORE_CONTENT, listOf(0, label, include?.let { f -> kfn { f(it[0] as String) } }, style.encode(),
        labelContent?.let { SSlotRef(next++) }, marketingContent?.let { SSlotRef(next++) }, sections != null), Modifier, slots = slots,
        content = sections ?: {})
}

/** StoreKit `SubscriptionOptionSection(_:isIncluded:footer:)` / `(isIncluded:header:footer:)`. */
@HiddenFromObjC
@Composable
public fun SubscriptionOptionSection(
    title: String? = null,
    include: (productId: String) -> Boolean = { true },
    style: StoreContentStyle = StoreContentStyle(),
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val slots = listOfNotNull(header, footer)
    var next = 0
    SwiftView(StoreKitIds.STORE_CONTENT, listOf(1, title, kfn { include(it[0] as String) }, style.encode(),
        header?.let { SSlotRef(next++) }, footer?.let { SSlotRef(next++) }), Modifier, slots = slots)
}

/** The period of a [SubscriptionPeriodGroupSet] group (null: subscriptions without a period). */
@HiddenFromObjC
public enum class SubscriptionPeriodUnit { day, week, month, year }

/**
 * StoreKit `SubscriptionPeriodGroupSet(marketingContent:label:)`: one group per subscription period, with the
 * [marketingContent] and [label] of each period (absent ones use the default).
 */
@HiddenFromObjC
@Composable
public fun SubscriptionPeriodGroupSet(
    style: StoreContentStyle = StoreContentStyle(),
    marketingContent: Map<SubscriptionPeriodUnit?, @Composable () -> Unit> = emptyMap(),
    label: Map<SubscriptionPeriodUnit?, @Composable () -> Unit> = emptyMap(),
) {
    val periods = listOf(null) + SubscriptionPeriodUnit.entries
    val slots = periods.mapNotNull { marketingContent[it] } + periods.mapNotNull { label[it] }
    var next = 0
    SwiftView(StoreKitIds.STORE_CONTENT, listOf(2, null, null, style.encode(),
        periods.map { p -> marketingContent[p]?.let { SSlotRef(next++) } }, periods.map { p -> label[p]?.let { SSlotRef(next++) } }),
        Modifier, slots = slots)
}

/**
 * StoreKit `SubscriptionOptionGroupSet(groupedBy:label:marketingContent:)`: groups the subscriptions by the key
 * [groupedBy] returns; [label] and [marketingText] give each group's texts.
 */
@HiddenFromObjC
@Composable
public fun SubscriptionOptionGroupSet(
    groupedBy: (productId: String) -> String,
    label: (group: String) -> String = { it },
    marketingText: ((group: String) -> String)? = null,
    style: StoreContentStyle = StoreContentStyle(),
) {
    SwiftView(StoreKitIds.STORE_CONTENT, listOf(3, null, kfn { groupedBy(it[0] as String) }, style.encode(),
        kfn { label(it[0] as String) }, marketingText?.let { f -> kfn { f(it[0] as String) } }), Modifier)
}

/**
 * StoreKit `SubscriptionOfferView(id:prefersPromotionalIcon:…)` (a product) or `(groupID:visibleRelationship:…)`
 * (the best offer of a group) (iOS 26 / macOS 26).
 */
@HiddenFromObjC
@Composable
public fun SubscriptionOfferView(
    modifier: Modifier = Modifier,
    id: String? = null,
    groupId: String? = null,
    visibleRelationship: SubscriptionRelationship = SubscriptionRelationship.all,
    prefersPromotionalIcon: Boolean = false,
    useAppIcon: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    placeholderIcon: (@Composable () -> Unit)? = null,
) {
    val slots = listOfNotNull(icon, placeholderIcon)
    var next = 0
    SwiftView(StoreKitIds.OFFER_VIEW, listOf(id, groupId, visibleRelationship.rawValue, prefersPromotionalIcon, useAppIcon,
        icon?.let { SSlotRef(next++) }, placeholderIcon?.let { SSlotRef(next++) }), modifier, slots = slots)
}

/** StoreKit `productViewStyle(_:)`. */
@HiddenFromObjC
public fun Modifier.productViewStyle(style: ProductViewStyle): Modifier = element(StoreKitIds.PRODUCT_VIEW_STYLE, style.ordinal)

/** The store modifiers (`storeButton`, `subscriptionStoreControlStyle`, `subscriptionStoreButtonLabel`…) on a view. */
@HiddenFromObjC
public fun Modifier.storeStyle(style: StoreContentStyle): Modifier = element(StoreKitIds.STORE_STYLE, style.encode())

/** StoreKit `subscriptionOfferViewStyle(_:)`, `subscriptionOfferViewButtonVisibility(_:for:)`, `subscriptionOfferViewDetailAction(_:)`. */
@HiddenFromObjC
public fun Modifier.subscriptionOfferViewStyle(
    style: SubscriptionOfferViewStyle? = null,
    detailLinkVisibility: Visibility? = null,
    detailAction: (() -> Unit)? = null,
): Modifier = element(StoreKitIds.OFFER_VIEW_STYLE, style?.ordinal, detailLinkVisibility, detailAction?.let { SAction(it) })

/** StoreKit `subscriptionStoreSignInAction(_:)`. */
@HiddenFromObjC
public fun Modifier.subscriptionStoreSignInAction(action: (() -> Unit)?): Modifier =
    element(StoreKitIds.SIGN_IN_ACTION, action?.let { SAction(it) })

/**
 * StoreKit `subscriptionStorePolicyDestination(url:for:)` / `(for:destination:)` and
 * `subscriptionStorePolicyForegroundStyle(_:_:)`.
 */
@HiddenFromObjC
public fun Modifier.subscriptionStorePolicy(
    kind: SubscriptionStorePolicyKind? = null,
    url: String? = null,
    destination: (@Composable () -> Unit)? = null,
    foregroundStyle: ShapeStyle? = null,
    secondaryForegroundStyle: ShapeStyle? = null,
): Modifier = element(StoreKitIds.POLICY, kind?.ordinal, url, destination?.let { slotContent(it) }, foregroundStyle, secondaryForegroundStyle)

/** StoreKit `subscriptionStoreControlIcon(icon:)`: the icon of each subscription, by product id. */
@HiddenFromObjC
public fun Modifier.subscriptionStoreControlIcon(icons: Map<String, @Composable () -> Unit>, default: (@Composable () -> Unit)? = null): Modifier {
    val keys = icons.keys.toList()
    return element(StoreKitIds.CONTROL_ICON, keys, default?.let { slotContent(it) }, *keys.map { slotContent(icons.getValue(it)) }.toTypedArray())
}

/** The App Store overlay position (StoreKit `SKOverlay.Position`). */
@HiddenFromObjC
public enum class AppStoreOverlayPosition { bottom, bottomRaised }

/**
 * StoreKit sheets: `manageSubscriptionsSheet(isPresented:subscriptionGroupID:)`, `offerCodeRedemption(isPresented:)`,
 * `refundRequestSheet(for:isPresented:)`, `appStoreOverlay(isPresented:configuration:)`. Each sheet calls its
 * dismiss callback when it closes.
 */
@HiddenFromObjC
public fun Modifier.manageSubscriptionsSheet(isPresented: Boolean, onDismiss: () -> Unit, subscriptionGroupId: String? = null): Modifier =
    element(StoreKitIds.SHEETS, 0, isPresented, SValueCallback { if (it == false) onDismiss() }, subscriptionGroupId, null)

/** StoreKit `offerCodeRedemption(isPresented:onCompletion:)`: [onCompletion] receives the error message or null. */
@HiddenFromObjC
public fun Modifier.offerCodeRedemption(isPresented: Boolean, onDismiss: () -> Unit, onCompletion: (error: String?) -> Unit = {}): Modifier =
    element(StoreKitIds.SHEETS, 1, isPresented, SValueCallback { if (it == false) onDismiss() }, null, SValueCallback { onCompletion(it as String?) })

/** StoreKit `refundRequestSheet(for:isPresented:onDismiss:)`: [onResult] receives "success", "userCancelled" or the error. */
@HiddenFromObjC
public fun Modifier.refundRequestSheet(transactionId: Long, isPresented: Boolean, onDismiss: () -> Unit, onResult: (String) -> Unit = {}): Modifier =
    element(StoreKitIds.SHEETS, 2, isPresented, SValueCallback { if (it == false) onDismiss() }, transactionId.toDouble(), SValueCallback { onResult(it as String) })

/** StoreKit `appStoreOverlay(isPresented:configuration:)` for the app [appIdentifier] (iOS). */
@HiddenFromObjC
public fun Modifier.appStoreOverlay(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    appIdentifier: String,
    position: AppStoreOverlayPosition = AppStoreOverlayPosition.bottom,
): Modifier = element(StoreKitIds.SHEETS, 3, isPresented, SValueCallback { if (it == false) onDismiss() }, listOf(appIdentifier, position.ordinal), null)

/** Purchase options returned by [onInAppPurchase] (StoreKit `Product.PurchaseOption`). */
@HiddenFromObjC
public data class StorePurchaseOptions(
    val quantity: Int? = null,
    /** `appAccountToken(_:)`: a UUID string. */
    val appAccountToken: String? = null,
    val simulatesAskToBuyInSandbox: Boolean? = null,
)

/**
 * StoreKit `onInAppPurchaseStart(perform:)`, `inAppPurchaseOptions(_:)` and `onInAppPurchaseCompletion(perform:)` for the
 * store views below this view.
 */
@HiddenFromObjC
public fun Modifier.onInAppPurchase(
    onStart: ((productId: String) -> Unit)? = null,
    options: ((productId: String) -> StorePurchaseOptions)? = null,
    onCompletion: ((productId: String, result: StorePurchaseResult) -> Unit)? = null,
): Modifier = element(
    StoreKitIds.PURCHASE_CALLBACKS,
    onStart?.let { f -> SValueCallback { f(it as String) } },
    options?.let { f -> kfn { a -> f(a[0] as String).let { o -> listOf(o.quantity, o.appAccountToken, o.simulatesAskToBuyInSandbox) } } },
    onCompletion?.let { f -> SValueCallback { v -> (v as List<*>).let { f(it[0] as String, StorePurchaseResult.decode(it[1])) } } },
)

/** A promotional offer signature (StoreKit `Product.SubscriptionOffer.Signature`). */
@HiddenFromObjC
public data class SubscriptionOfferSignature(val keyId: String, val nonce: String, val timestamp: Int, val signature: String)

/**
 * StoreKit subscription offers in the store views: `subscriptionIntroductoryOffer(applyOffer:compactJWS:)`,
 * `subscriptionPromotionalOffer(offer:signature:)` / `(offer:compactJWS:)` and `preferredSubscriptionOffer(_:)`.
 * Offers are chosen by their App Store Connect id.
 */
@HiddenFromObjC
public fun Modifier.subscriptionOffers(
    applyIntroductoryOffer: ((productId: String) -> Boolean)? = null,
    introductoryOfferJWS: ((productId: String) -> String)? = null,
    promotionalOffer: ((productId: String, offerIds: List<String>) -> String?)? = null,
    promotionalOfferSignature: ((productId: String, offerId: String) -> SubscriptionOfferSignature)? = null,
    promotionalOfferJWS: ((productId: String, offerId: String) -> String)? = null,
    preferredOffer: ((productId: String, offerIds: List<String>) -> String?)? = null,
): Modifier = element(
    StoreKitIds.OFFERS,
    applyIntroductoryOffer?.let { f -> kfn { f(it[0] as String) } },
    introductoryOfferJWS?.let { f -> kfn { f(it[0] as String) } },
    promotionalOffer?.let { f -> kfn { a -> f(a[0] as String, (a[1] as List<*>).map { it as String }) } },
    promotionalOfferSignature?.let { f -> kfn { a -> f(a[0] as String, a[1] as String).let { listOf(it.keyId, it.nonce, it.timestamp, it.signature) } } },
    promotionalOfferJWS?.let { f -> kfn { a -> f(a[0] as String, a[1] as String) } },
    preferredOffer?.let { f -> kfn { a -> f(a[0] as String, (a[1] as List<*>).map { it as String }) } },
)

/** StoreKit `storeProductTask(for:priority:action:)`: loads a product while the view is visible. */
@HiddenFromObjC
public fun Modifier.storeProductTask(id: String, action: (StoreTaskState<StoreProduct>) -> Unit): Modifier =
    element(StoreKitIds.TASKS, 0, listOf(id), SValueCallback { v -> action(decodeState(v) { StoreProduct.decode(it)!! }) })

/** StoreKit `storeProductsTask(for:priority:action:)`. */
@HiddenFromObjC
public fun Modifier.storeProductsTask(ids: List<String>, action: (StoreTaskState<List<StoreProduct>>) -> Unit): Modifier =
    element(StoreKitIds.TASKS, 1, ids, SValueCallback { v -> action(decodeState(v) { l -> (l as List<*>).mapNotNull { StoreProduct.decode(it) } }) })

/** StoreKit `currentEntitlementTask(for:priority:action:)`: the current transaction of a product, null when none. */
@HiddenFromObjC
public fun Modifier.currentEntitlementTask(productId: String, action: (StoreTaskState<StoreTransaction?>) -> Unit): Modifier =
    element(StoreKitIds.TASKS, 2, listOf(productId), SValueCallback { v -> action(decodeState(v) { StoreTransaction.decode(it) }) })

/** StoreKit `subscriptionStatusTask(for:priority:action:)`: the statuses of a subscription group. */
@HiddenFromObjC
public fun Modifier.subscriptionStatusTask(groupId: String, action: (StoreTaskState<List<StoreSubscriptionStatus>>) -> Unit): Modifier =
    element(StoreKitIds.TASKS, 3, listOf(groupId), SValueCallback { v ->
        action(decodeState(v) { l ->
            (l as List<*>).map { s ->
                val e = s as List<*>
                StoreSubscriptionStatus(e[0] as String, StoreTransaction.decode(e[1]), e[2] as Boolean?, e[3] as String?)
            }
        })
    })

private fun <T> decodeState(v: Any?, value: (Any?) -> T): StoreTaskState<T> {
    val l = v as? List<*> ?: return StoreTaskState.Loading
    return when ((l[0] as Number).toInt()) {
        0 -> StoreTaskState.Loading
        1 -> StoreTaskState.Unavailable
        2 -> StoreTaskState.Failure(l.getOrNull(1) as? String ?: "")
        else -> StoreTaskState.Success(value(l[1]), (l.getOrNull(2) as? List<*>)?.map { it as String } ?: emptyList())
    }
}

/**
 * StoreKit environment actions: when [trigger] changes, the view asks for an App Store review (`requestReview`),
 * buys [purchaseProductId] (`purchase`, result in [onPurchase]) or shows pending StoreKit messages
 * (`displayStoreKitMessage`).
 */
@HiddenFromObjC
public fun Modifier.storeAction(
    trigger: Any,
    requestReview: Boolean = false,
    purchaseProductId: String? = null,
    purchaseOptions: StorePurchaseOptions = StorePurchaseOptions(),
    onPurchase: ((StorePurchaseResult) -> Unit)? = null,
    displayMessages: Boolean = false,
): Modifier = element(StoreKitIds.ACTIONS, trigger, requestReview, purchaseProductId,
    listOf(purchaseOptions.quantity, purchaseOptions.appAccountToken, purchaseOptions.simulatesAskToBuyInSandbox),
    onPurchase?.let { f -> SValueCallback { f(StorePurchaseResult.decode(it)) } }, displayMessages)

/** StoreKit `productIconBorder()`: the App Store icon border on an app icon image. */
@HiddenFromObjC
public fun Modifier.productIconBorder(): Modifier = element(StoreKitIds.ICON_BORDER)
