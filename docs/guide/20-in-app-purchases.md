<!-- group: Apple frameworks -->
# In-app purchases

> Sell products and subscriptions with StoreKit's SwiftUI views from Kotlin: ProductView, StoreView, SubscriptionStoreView, purchase callbacks, offers, store sheets and testing.

StoreKit ships SwiftUI views that load products from the App Store, show them with localized prices, and run the
purchase flow: `ProductView`, `StoreView` and `SubscriptionStoreView`. Compose SwiftUI exposes them to Kotlin, with the
store modifiers, the purchase callbacks, subscription offers and the StoreKit sheets. Products are identified by their
App Store Connect ids; products, transactions and subscription statuses come back as Kotlin data classes.

StoreKit is an opt-in integration (see [Apple frameworks](apple-frameworks.md)): add
`ComposeSwiftUIIntegrations/StoreKit.swift` to your target, enable the **In-App Purchase** capability, and register it
at launch.

```swift
@main
struct MyApp: App {
    init() { ComposeIntegrations.registerStoreKit() }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

## Platform availability

| Platform | What works |
|---|---|
| iOS 17, macOS 14 | everything in this chapter, with the OS versions noted per API |
| tvOS 17, watchOS 10 | `ProductView(id, prefersPromotionalIcon)`, `StoreView(ids, prefersPromotionalIcon)` and `SubscriptionStoreView(groupId or productIds)` with their default look; icons, store content, store modifiers, callbacks, tasks and sheets are ignored |

StoreKit doesn't offer the store modifiers and sheets on tvOS and watchOS, so `StoreKit.swift` only registers the three
views there. Purchases made in these views still complete normally.

## Products

`ProductView` loads one product and shows its name, description, price and buy button:

```kotlin
@Composable
fun ProUpgrade() {
    VStack(spacing = 16.0) {
        ProductView(
            "com.example.app.pro",
            prefersPromotionalIcon = true,
            icon = { Image(systemName = SFSymbol("crown.fill"), modifier = Modifier.font(Font.largeTitle)) },
            modifier = Modifier.productViewStyle(ProductViewStyle.large),
        )
        ProductView("com.example.app.tips.small")
    }
}
```

- `ProductViewStyle` is `automatic`, `compact`, `regular` or `large`. Set it on the view or on a container to style
  every product inside.
- `icon` is shown once the product is loaded and `placeholderIcon` while it loads. For full control, `iconPhase`
  receives the `ProductIconPhase` (`loading`, `success`, `failure`, `unavailable`); in the `success` branch, place
  `StyleContent()` to show the App Store icon.
- `prefersPromotionalIcon` uses the promoted icon from App Store Connect when there is one.

`StoreView` merchandises a list of products, with an icon per product id:

```kotlin
@Composable
fun TipJar() {
    StoreView(
        ids = listOf("tip.small", "tip.medium", "tip.large"),
        icons = mapOf<String, @Composable () -> Unit>(
            "tip.small" to { Image(systemName = SFSymbol("cup.and.saucer")) },
            "tip.medium" to { Image(systemName = SFSymbol("birthday.cake")) },
            "tip.large" to { Image(systemName = SFSymbol("gift")) },
        ),
        style = StoreContentStyle(storeButtons = mapOf(StoreButtonKind.restorePurchases to Visibility.visible)),
    )
}
```

## Subscriptions

`SubscriptionStoreView` shows the subscriptions of a group (`groupId`, from App Store Connect) or an explicit list
(`productIds`), with a marketing header above the plans:

```kotlin
@Composable
fun Paywall(onClose: () -> Unit) {
    SubscriptionStoreView(
        groupId = "21482456",
        style = StoreContentStyle(
            controlStyle = SubscriptionStoreControlStyle.prominentPicker,
            buttonLabel = SubscriptionStoreButtonLabel.multiline,
            storeButtons = mapOf(
                StoreButtonKind.restorePurchases to Visibility.visible,
                StoreButtonKind.redeemCode to Visibility.visible,
                StoreButtonKind.cancellation to Visibility.visible,
            ),
        ),
        modifier = Modifier
            .subscriptionStorePolicy(SubscriptionStorePolicyKind.termsOfService, url = "https://example.com/terms")
            .subscriptionStorePolicy(SubscriptionStorePolicyKind.privacyPolicy, url = "https://example.com/privacy")
            .containerBackground(Color.indigo, ContainerBackgroundPlacement.subscriptionStoreHeader),
        marketingContent = {
            VStack(spacing = 8.0) {
                Image(systemName = SFSymbol("sparkles"), modifier = Modifier.font(Font.largeTitle))
                Text("Pro", modifier = Modifier.font(Font.largeTitle))
                Text("Unlimited projects, sync and themes.")
            }
        },
    )
}
```

The `cancellation` button (the close button) dismisses the presentation; show the paywall in a sheet and the dismissal
works like any other sheet.

### Store style

`StoreContentStyle` groups the store modifiers. Pass it as `style` to a store view, or apply it to any container with
`Modifier.storeStyle(…)`:

| Field | StoreKit modifier |
|---|---|
| `productDescription` | `productDescription(_:)` |
| `storeButtons` | `storeButton(_:for:)` per `StoreButtonKind` (`cancellation`, `policies`, `redeemCode`, `restorePurchases`, `signIn`) |
| `buttonLabel` | `subscriptionStoreButtonLabel(_:)` |
| `controlStyle`, `controlPlacement` | `subscriptionStoreControlStyle(_:placement:)` |
| `controlBackground`, `controlBackgroundStyle` | `subscriptionStoreControlBackground(_:)` |
| `pickerItemBackground`, `pickerItemShape` | `subscriptionStorePickerItemBackground(_:in:)` |
| `optionGroupStyle` | `subscriptionStoreOptionGroupStyle(_:)` |

The control styles `buttons`, `picker` and `prominentPicker` work everywhere; `compactPicker`, `pagedPicker`,
`pagedProminentPicker`, the placements, `optionGroupStyle` and `pickerItemShape` need iOS 18 / macOS 15. The
`redeemCode` button needs macOS 15 on the Mac; on macOS 14 its visibility is ignored and the other buttons
(`restorePurchases` included) keep theirs.

Use `Modifier.containerBackground(style, ContainerBackgroundPlacement.subscriptionStore)` (or
`subscriptionStoreHeader`, `subscriptionStoreFullHeight`) to color the store, and
`Modifier.subscriptionStoreControlIcon(icons)` to give each plan an icon in the picker.

### Grouping plans

The `content` of `SubscriptionStoreView` holds StoreKit's store content: `SubscriptionOptionGroup`,
`SubscriptionOptionSection`, `SubscriptionPeriodGroupSet` and `SubscriptionOptionGroupSet`.

```kotlin
SubscriptionStoreView(
    groupId = "21482456",
    style = StoreContentStyle(optionGroupStyle = SubscriptionOptionGroupStyle.tabs),
) {
    SubscriptionOptionGroup(label = "Individual", include = { id -> id.startsWith("pro.individual") })
    SubscriptionOptionGroup(label = "Family", include = { id -> id.startsWith("pro.family") })
}
```

`SubscriptionPeriodGroupSet` makes one group per period (monthly, yearly…), and `SubscriptionOptionGroupSet(groupedBy)`
groups by any key computed from the product id.

### Sign-in and offers views

- `Modifier.subscriptionStoreSignInAction { … }` shows a sign-in button in the store (for subscribers who bought on
  another platform); show your sign-in flow from the action.
- `SubscriptionOfferView(id = …)` or `SubscriptionOfferView(groupId = …)` promotes a single subscription offer inline
  (iOS 26), styled with `Modifier.subscriptionOfferViewStyle`.

## Purchase callbacks

The store views run the purchase themselves. `Modifier.onInAppPurchase` observes it, for every store view inside the
modified view:

```kotlin
class StoreModel {
    var isPurchasing by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun onCompletion(productId: String, result: StorePurchaseResult) {
        isPurchasing = false
        message = when (result) {
            is StorePurchaseResult.Success ->
                if (result.transaction.verified) "Thanks for buying $productId!" else "The purchase could not be verified."
            StorePurchaseResult.Pending -> "Waiting for approval."
            StorePurchaseResult.UserCancelled -> null
            is StorePurchaseResult.Failure -> result.message
        }
    }
}

@Composable
fun Store(model: StoreModel, accountToken: String) {
    VStack {
        StoreView(
            ids = listOf("pro.lifetime", "themes.pack"),
            modifier = Modifier.onInAppPurchase(
                onStart = { model.isPurchasing = true },
                options = { StorePurchaseOptions(appAccountToken = accountToken) },
                onCompletion = model::onCompletion,
            ),
        )
        model.message?.let { Text(it) }
    }
}
```

- `onStart` is called when the user taps a buy button, before the system sheet.
- `options` returns the `StorePurchaseOptions` of the purchase: `quantity`, `appAccountToken` (a UUID string that
  ties the transaction to your user account) and `simulatesAskToBuyInSandbox`.
- `onCompletion` receives a `StorePurchaseResult`: `Success` with the `StoreTransaction`, `Pending` (Ask to Buy or a
  bank verification; the transaction arrives later), `UserCancelled`, or `Failure` with the error message.
- `StoreTransaction.verified` tells whether StoreKit's signature check passed. Only grant content for verified
  transactions, and verify on your server if you have one.

## Custom store UI

When the built-in views don't fit your design, load the products yourself and buy with `Modifier.storeAction`:

```kotlin
@Composable
fun CustomPlans(ids: List<String>) {
    var products by remember { mutableStateOf<List<StoreProduct>>(emptyList()) }
    var buying by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    VStack(
        spacing = 12.0,
        modifier = Modifier
            .storeProductsTask(ids) { state ->
                if (state is StoreTaskState.Success) products = state.value.sortedBy { it.price }
            }
            .storeAction(
                trigger = attempt,
                purchaseProductId = buying,
                onPurchase = { result -> buying = null },
            ),
    ) {
        for (product in products) {
            Button("${product.displayName}: ${product.displayPrice}") {
                buying = product.id
                attempt++
            }
        }
    }
}
```

- `storeProductTask(id)` and `storeProductsTask(ids)` load products while the view is visible and report a
  `StoreTaskState`: `Loading`, `Unavailable`, `Failure(message)` or `Success(value, unavailableIds)`.
- `StoreProduct` holds the id, the localized `displayName`, `description` and `displayPrice`, the numeric `price`, the
  product type, the subscription group id and whether it is family-shareable.
- `storeAction` runs when `trigger` changes: it buys `purchaseProductId` (result in `onPurchase`), and can also
  `requestReview` (the App Store rating prompt) or display pending StoreKit messages (`displayMessages`, iOS).

```kotlin
var reviewRequests by remember { mutableStateOf(0) }

Button(
    "Rate the app",
    modifier = Modifier.storeAction(trigger = reviewRequests, requestReview = true),
) { reviewRequests++ }
```

The system decides whether the review prompt actually appears (at most three times a year per user).

## Entitlements and subscription status

Two tasks report what the user owns, and update when it changes:

```kotlin
@Composable
fun ProGate(content: @Composable (isPro: Boolean) -> Unit) {
    var lifetime by remember { mutableStateOf(false) }
    var subscribed by remember { mutableStateOf(false) }
    VStack(
        modifier = Modifier
            .currentEntitlementTask("pro.lifetime") { state ->
                if (state is StoreTaskState.Success) lifetime = state.value?.let { it.verified && it.revocationDate == null } == true
            }
            .subscriptionStatusTask("21482456") { state ->
                if (state is StoreTaskState.Success) subscribed = state.value.any { it.state == "subscribed" || it.state == "inGracePeriod" }
            },
    ) {
        content(lifetime || subscribed)
    }
}
```

- `currentEntitlementTask(productId)` gives the current `StoreTransaction` of a non-consumable or a subscription, or
  `null` when the user doesn't own it.
- `subscriptionStatusTask(groupId)` gives one `StoreSubscriptionStatus` per subscription of the user in the group (family
  sharing can add one): the renewal `state`, the latest transaction, `willAutoRenew` and the `autoRenewProductId`.
  `state` is one of `"subscribed"`, `"expired"`, `"inBillingRetryPeriod"`, `"inGracePeriod"` or `"revoked"`
  (StoreKit's `Product.SubscriptionInfo.RenewalState` names).

These tasks run while the view is on screen. For app-wide state, keep the result in a shared model (or compute
entitlements in Swift from `Transaction.currentEntitlements` and pass them to Kotlin).

## Store sheets

Each sheet is a modifier with `isPresented` and an `onDismiss` callback, like other presentations:

```kotlin
@Composable
fun AccountSettings(lastTransactionId: Long?) {
    var manage by remember { mutableStateOf(false) }
    var refund by remember { mutableStateOf(false) }
    var redeem by remember { mutableStateOf(false) }

    List(
        modifier = Modifier
            .manageSubscriptionsSheet(isPresented = manage, onDismiss = { manage = false }, subscriptionGroupId = "21482456")
            .offerCodeRedemption(isPresented = redeem, onDismiss = { redeem = false })
            .refundRequestSheet(
                transactionId = lastTransactionId ?: 0L,
                isPresented = refund,
                onDismiss = { refund = false },
                onResult = { result -> println("Refund request: $result") },
            ),
    ) {
        Button("Manage subscription") { manage = true }
        Button("Redeem a code") { redeem = true }
        if (lastTransactionId != null) Button("Request a refund") { refund = true }
    }
}
```

| Modifier | Platforms |
|---|---|
| `manageSubscriptionsSheet` | iOS (`subscriptionGroupId`: iOS 18) |
| `offerCodeRedemption` | iOS 17, macOS 15 |
| `refundRequestSheet` | iOS, macOS; `onResult` gets `"success"`, `"userCancelled"` or the error |
| `appStoreOverlay` | iOS: shows another app's App Store overlay |

On macOS, manage subscriptions in the App Store app: there is no in-app sheet.

## Subscription offers

`Modifier.subscriptionOffers` decides which offers the store views apply. Offers are chosen by their App Store Connect
offer id:

```kotlin
Modifier.subscriptionOffers(
    preferredOffer = { productId, offerIds -> offerIds.firstOrNull { it.startsWith("winback") } },
    promotionalOffer = { productId, offerIds -> offerIds.firstOrNull() },
    promotionalOfferJWS = { productId, offerId -> "<compact JWS signed by your server>" },
)
```

| Parameters | StoreKit | Availability |
|---|---|---|
| `promotionalOffer` + `promotionalOfferSignature` | `subscriptionPromotionalOffer(offer:signature:)` | iOS 18, macOS 15 |
| `promotionalOffer` + `promotionalOfferJWS` | `subscriptionPromotionalOffer(offer:compactJWS:)` | iOS 26, macOS 26 |
| `applyIntroductoryOffer` + `introductoryOfferJWS` | `subscriptionIntroductoryOffer(applyOffer:compactJWS:)` | iOS 26, macOS 26 |
| `preferredOffer` | `preferredSubscriptionOffer(_:)` (win-back offers) | iOS 18.4, macOS 15.4 |

Promotional offer signatures and JWS must be created by your server with your In-App Purchase key; never ship the key
in the app. The callbacks are synchronous: fetch the signatures before the store is shown and return them from memory.

## What stays in Swift

### Finishing transactions

The integration reports transactions but never calls `finish()`. Every StoreKit app should listen to
`Transaction.updates` from launch, deliver the content and finish the transaction. The listener also catches renewals,
Ask to Buy approvals, refunds and purchases made on other devices:

```swift
import StoreKit

@MainActor final class TransactionObserver {
    private var updates: Task<Void, Never>?

    init() {
        updates = Task {
            for await result in Transaction.updates {
                guard case .verified(let transaction) = result else { continue }
                // Grant the content (or tell your Kotlin model), then:
                await transaction.finish()
            }
        }
    }
}
```

Create it once in your `App` (`private let transactions = TransactionObserver()`), and go through
`Transaction.unfinished` at launch as well.

### Project and server setup

- The products and subscription groups in App Store Connect, and the In-App Purchase capability.
- Your In-App Purchase key, used by your server to sign promotional offers.
- App Store Server API calls and server-side validation.

## Testing with StoreKit configuration files

You don't need App Store Connect to develop the store UI:

1. In Xcode, **File > New > File > StoreKit Configuration File**. Create it locally or synced with your App Store
   Connect app, and add the products and subscription groups with the same ids as in your Kotlin code.
2. In the scheme, **Run > Options > StoreKit Configuration**, pick the file.
3. Run the app: the store views load the local products and purchases complete instantly, without an account.
4. Use **Debug > StoreKit > Manage Transactions** to refund, approve Ask to Buy requests, expire subscriptions or clear
   purchases. Subscription renewals are accelerated (set the rate in the configuration file's editor).

`StorePurchaseOptions(simulatesAskToBuyInSandbox = true)` tests the `Pending` result in the sandbox. For end-to-end
tests against the real App Store, use a Sandbox account (Settings > App Store > Sandbox Account on the device) or
TestFlight.

The full list of mapped StoreKit APIs is on [/storekit.html](/storekit.html).
