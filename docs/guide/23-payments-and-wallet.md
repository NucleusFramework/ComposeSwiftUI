<!-- group: Apple frameworks -->
# Payments and Wallet

> Take Apple Pay payments, add passes and orders to Wallet, verify identity with Wallet and let users share transactions, from Kotlin.

PassKit's SwiftUI views (`PassKit.swift`) and FinanceKitUI (`FinanceKitUI.swift`) are available from Kotlin. These are
for physical goods, services and Wallet content; digital goods and subscriptions sold inside the app go through
StoreKit instead (see [In-app purchases](in-app-purchases.md)).

```swift
@main
struct MyApp: App {
    init() {
        #if os(iOS) || os(visionOS)
        ComposeIntegrations.registerPassKit()       // PassKit.swift
        #endif
        #if os(iOS)
        ComposeIntegrations.registerFinanceKitUI()  // FinanceKitUI.swift
        #endif
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

| Kotlin API | Framework | Availability |
|---|---|---|
| `PayWithApplePayButton`, `Modifier.onApplePayChange`, `Modifier.payWithApplePayButtonStyle` | PassKit | iOS 16, visionOS |
| `AddPassToWalletButton`, `Modifier.addPassToWalletButtonStyle` | PassKit | iOS 16, visionOS |
| `PayLaterView`, `Modifier.payLaterViewStyle` | PassKit | iOS 17, visionOS |
| `VerifyIdentityWithWalletButton`, `Modifier.verifyIdentityWithWalletButtonStyle` | PassKit | iOS 16, visionOS |
| `ApplePayMerchandisingView` | PassKit | iOS 27 |
| `AddOrderToWalletButton`, `Modifier.addOrderToWalletButtonStyle` | FinanceKitUI | iOS 17.4 |
| `TransactionPicker`, `Modifier.transactionPicker` | FinanceKitUI | iOS 18 |

The PassKit renderer is compiled for iOS and visionOS only: on macOS and watchOS these views render nothing, so show
another checkout path there.

## Apple Pay

### Setup

1. In the Apple Developer portal, create a **merchant identifier** (`merchant.com.example.shop`) and the payment
   processing certificate your payment provider asks for.
2. Add the **Apple Pay** capability to the app target and select the merchant identifier
   (`com.apple.developer.in-app-payments` entitlement).
3. Test on a device with a card in Wallet, or with a sandbox tester account.

### PayWithApplePayButton

You describe the payment with an `ApplePayRequest` (a `PKPaymentRequest`); the button shows the payment sheet, and
`onAuthorize` receives the authorized payment:

```kotlin
data class CartLine(val name: String, val price: String)

class CheckoutModel {
    val lines = listOf(CartLine("Espresso beans", "18.00"), CartLine("Shipping", "4.99"))
    var paid by mutableStateOf(false)

    fun request() = ApplePayRequest(
        merchantIdentifier = "merchant.com.example.shop",
        countryCode = "US",
        currencyCode = "USD",
        supportedNetworks = listOf("Visa", "MasterCard", "AmEx"),
        items = lines.map { PaymentSummaryItem(it.name, it.price) } + PaymentSummaryItem("Example Coffee", "22.99"),
        requiredShippingContactFields = listOf("postalAddress", "emailAddress"),
        shippingMethods = listOf(
            ShippingMethod("standard", "Standard", "4.99", detail = "3-5 days"),
            ShippingMethod("express", "Express", "9.99", detail = "Tomorrow"),
        ),
    )

    fun authorize(payment: ApplePayPayment): PaymentAuthorizationStatus {
        // payment.tokenData is the base64 payment token for your payment provider.
        return if (payment.tokenData.isNotEmpty()) PaymentAuthorizationStatus.success else PaymentAuthorizationStatus.failure
    }
}

@Composable
fun CheckoutButton(model: CheckoutModel) {
    PayWithApplePayButton(
        label = ApplePayButtonLabel.checkout,
        request = model.request(),
        onAuthorize = model::authorize,
        onFinish = { model.paid = true },
        modifier = Modifier.payWithApplePayButtonStyle(ApplePayButtonStyle.black).frame(height = 50.0),
        fallback = { Text("Apple Pay is not available on this device") },
    )
}
```

How the values travel:

- Amounts are decimal strings (`"9.99"`), never floating point. The last summary item is the total and should be
  labelled with your company name, as Apple requires.
- `supportedNetworks` are `PKPaymentNetwork` raw values (`"Visa"`, `"MasterCard"`, `"AmEx"`, `"Discover"`…).
- `merchantCapabilities` take `"3DS"` (the default), `"EMV"`, `"Credit"` and `"Debit"`.
- Contact fields take `"name"`, `"postalAddress"`, `"emailAddress"` and `"phoneNumber"`.
- `ApplePayPayment` carries `tokenData` (base64), `transactionIdentifier`, the card `network`, the chosen
  `shippingMethodId` and the shipping `email`.

`onAuthorize` is a `suspend` function: the Apple Pay sheet keeps its spinner while it runs, so you can send the token
to your server and return the status it answers. If the coroutine fails or is cancelled, the sheet shows a failure.

`onFinish` runs when the sheet closes, whatever the outcome. `fallback` shows instead of the button when Apple Pay is
not available.

Without a `request`, the button only calls `action`: use this to start your own flow, or to send the user to set up
Apple Pay.

For Apple Pay on the web flows that need a merchant session, `merchantSession` returns the merchant session JSON
(the object your server got from Apple Pay's `paymentSession` endpoint); it is a `suspend` function too, so it can
fetch it.

### Updating the sheet

`Modifier.onApplePayChange` updates the summary when the user changes the shipping contact, shipping method, card or
coupon code. Each handler returns the new summary items, or `null` to keep them:

```kotlin
@Composable
fun CheckoutWithShipping(model: CheckoutModel) {
    PayWithApplePayButton(
        request = model.request().copy(supportsCouponCode = true),
        onAuthorize = model::authorize,
        modifier = Modifier.onApplePayChange(
            shippingMethod = { id ->
                val shipping = if (id == "express") "9.99" else "4.99"
                val total = (18.00 + shipping.toDouble()).toString()
                listOf(PaymentSummaryItem("Espresso beans", "18.00"), PaymentSummaryItem("Shipping", shipping), PaymentSummaryItem("Example Coffee", total))
            },
            couponCode = { code -> if (code == "WELCOME") listOf(PaymentSummaryItem("Example Coffee", "15.00")) else null },
        ),
    )
}
```

Use your own decimal arithmetic for money; the `toDouble()` above keeps the example short.

### Button styles

| Kotlin | SwiftUI |
|---|---|
| `Modifier.payWithApplePayButtonStyle(ApplePayButtonStyle.white)` | `payWithApplePayButtonStyle(.white)` |
| `Modifier.payWithApplePayButtonStyle(disableCardArt = true)` | `payWithApplePayButtonDisableCardArt()` (iOS 26) |
| `Modifier.addPassToWalletButtonStyle(blackOutline = true)` | `addPassToWalletButtonStyle(.blackOutline)` |
| `Modifier.verifyIdentityWithWalletButtonStyle(blackOutline = false)` | `verifyIdentityWithWalletButtonStyle(.black)` |
| `Modifier.payLaterViewStyle(PayLaterDisplayStyle.badge, PayLaterAction.calculator)` | `payLaterViewDisplayStyle(.badge)`, `payLaterViewAction(.calculator)` |

`ApplePayButtonLabel` has every `PayWithApplePayButtonLabel` (`buy`, `checkout`, `donate`, `subscribe`, `tip`…).

## Pay Later and merchandising

`PayLaterView` (iOS 17) shows the Apple Pay Later messaging for an amount:

```kotlin
PayLaterView(
    amount = "199.99",
    currency = "USD",
    modifier = Modifier.payLaterViewStyle(displayStyle = PayLaterDisplayStyle.price, action = PayLaterAction.learnMore),
)
```

`ApplePayMerchandisingView` (iOS 27) is the newer merchandising view for installment partners. `action` and `style` are
the raw values of `ApplePayMerchandisingAction` and `ApplePayMerchandisingStyle` (`"learnMore"`, `"standard"`…);
`partners = ApplePayMerchandisingPartners.limited` restricts it to `limitedPartnerIdentifiers`:

```kotlin
ApplePayMerchandisingView(
    amount = "499.00",
    currency = "USD",
    region = "US",
    partners = ApplePayMerchandisingPartners.all,
    fallback = { Text("Pay in installments at checkout") },
)
```

Both only show in regions where Apple Pay installments are offered; elsewhere they render empty or show `fallback`.

## Adding passes to Wallet

`AddPassToWalletButton` adds signed `.pkpass` files. Passes are signed on your server with a Pass Type ID
certificate; download them and pass their bytes in base64:

```kotlin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun BoardingPassButton(pkpass: ByteArray) {
    var added by remember { mutableStateOf(false) }
    VStack {
        AddPassToWalletButton(
            passes = listOf(Base64.encode(pkpass)),
            onCompletion = { added = it },
            modifier = Modifier.addPassToWalletButtonStyle(blackOutline = false).frame(height = 48.0),
        )
        if (added) Text("Added to Wallet")
    }
}
```

`onCompletion` receives `true` when the user added the passes. With an empty `passes` list, the button calls `action`
instead, so you can fetch the pass first. Adding a signed pass needs no entitlement; reading passes from the user's
Wallet (`PKPassLibrary`) does.

## Verify with Wallet

`VerifyIdentityWithWalletButton` requests elements of the driver's license or state ID stored in Wallet:

```kotlin
@Composable
fun AgeCheck(nonce: String, onDocument: (String) -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    VStack {
        VerifyIdentityWithWalletButton(
            label = VerifyIdentityLabel.verifyAge,
            merchantIdentifier = "merchant.com.example.shop",
            nonce = nonce,
            elements = listOf("age:21", "portrait"),
            onCompletion = { encrypted, failure ->
                if (encrypted != null) onDocument(encrypted) else error = failure
            },
            fallback = { Text("Verify with Wallet is not available") },
        )
        error?.let { Text(it) }
    }
}
```

Elements are `"givenName"`, `"familyName"`, `"portrait"`, `"address"`, `"issuingAuthority"`, `"documentNumber"`,
`"dateOfBirth"`, `"documentExpirationDate"`, `"documentIssueDate"`, `"drivingPrivileges"` and `"age:N"` for an age
threshold. Elements are requested with "will not store" intent. The result is the encrypted document in base64, which
only your server can decrypt.

This API requires an entitlement that Apple grants on request (Verify with Wallet), a merchant identifier and an
identity encryption certificate. Without `merchantIdentifier`, the button only calls `action`.

## FinanceKitUI

### Add orders to Wallet

`AddOrderToWalletButton` (iOS 17.4) adds a signed order package (`.order`), produced by your server with an Order Type
ID certificate, so the user can track the order in Wallet:

```kotlin
var status by remember { mutableStateOf<String?>(null) }

VStack {
    AddOrderToWalletButton(
        signedArchive = "UEsDBBQAAAAIA…",
        onCompletion = { result -> status = result },
        modifier = Modifier.addOrderToWalletButtonStyle(AddOrderToWalletButtonStyle.blackOutline),
    )
    status?.let { Text(it) }
}
```

`onCompletion` receives `"added"`, `"cancelled"`, `"newerExisting"` or the error message.

### Transaction picker

`TransactionPicker` and `Modifier.transactionPicker` (iOS 18) let the user share individual Apple Card, Apple Cash or
Savings transactions with your app, without granting access to all their financial data:

```kotlin
var transactions by remember { mutableStateOf(emptyList<FinanceTransaction>()) }

List {
    TransactionPicker(onSelection = { transactions = it }) {
        Label("Import transactions", systemImage = SFSymbol("creditcard"))
    }
    for (t in transactions) key(t.id) {
        HStack {
            Text(t.merchantName ?: t.description)
            Spacer()
            Text("${if (t.isCredit) "+" else "-"}${t.amount} ${t.currencyCode}")
        }
    }
}
```

`FinanceTransaction` carries the `id`, `description`, `merchantName`, `amount` (a `Double`), `currencyCode`, `date`
(epoch seconds) and `isCredit`.

FinanceKit requires the FinanceKit entitlement (`com.apple.developer.financekit`), which Apple grants on request, and
`NSFinancialDataUsageDescription` in Info.plist. It is available on iPhone only, in the regions where Apple offers it.
