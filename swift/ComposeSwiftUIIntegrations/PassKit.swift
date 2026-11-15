// PassKit for Compose SwiftUI: Apple Pay and Wallet buttons, Pay Later, merchandising, identity verification and the
// Apple Pay change handlers (runtime Frameworks.kt). Add this file to the app target and call
// `ComposeIntegrations.registerPassKit()` at launch.
#if os(iOS) || os(visionOS)
import SwiftUI
import PassKit

public extension ComposeIntegrations {
    @MainActor static func registerPassKit() {
        guard #available(iOS 16.0, macOS 13.0, watchOS 9.0, *) else { return }
        ComposeRegistry.views[200_308] = { m in ModChain.apply(KotlinApplePayButton(m: m), m, 0) }
        ComposeRegistry.views[200_309] = { m in ModChain.apply(KotlinAddPassButton(m: m), m, 0) }
        ComposeRegistry.modifiers[200_314] = { v, a, _ in PassModifiers.style(v, a) }
        ComposeRegistry.modifiers[200_315] = { v, a, _ in PassModifiers.changes(v, a) }
        #if os(iOS) || os(macOS) || os(visionOS)
        if #available(iOS 17.0, macOS 14.0, *) {
            ComposeRegistry.views[200_310] = { m in
                ModChain.apply(PayLaterView(amount: Decimal(string: m.args.string(0)) ?? 0, currency: Locale.Currency(m.args.string(1))), m, 0)
            }
        }
        #endif
        #if os(iOS) || os(visionOS)
        ComposeRegistry.views[200_312] = { m in ModChain.apply(KotlinVerifyIdentity(m: m), m, 0) }
        if #available(iOS 27.0, *) {
            ComposeRegistry.views[200_311] = { m in ModChain.apply(KotlinMerchandising(m: m), m, 0) }
        }
        #endif
    }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
enum PassValues {
    static func items(_ v: Any?) -> [PKPaymentSummaryItem] {
        (v as? [Any] ?? []).compactMap { $0 as? [Any] }.map { l in
            PKPaymentSummaryItem(label: l.first as? String ?? "", amount: NSDecimalNumber(string: l.count > 1 ? l[1] as? String : "0"),
                                 type: (l.count > 2 && (l[2] as? NSNumber)?.boolValue == true) ? .pending : .final)
        }
    }

    static func contactFields(_ v: Any?) -> Set<PKContactField> {
        Set((v as? [Any] ?? []).compactMap { $0 as? String }.map { PKContactField(rawValue: $0) })
    }

    static func request(_ l: [Any]) -> PKPaymentRequest {
        let r = PKPaymentRequest()
        func s(_ i: Int) -> String { i < l.count ? l[i] as? String ?? "" : "" }
        r.merchantIdentifier = s(0)
        r.countryCode = s(1)
        r.currencyCode = s(2)
        r.supportedNetworks = (l.count > 3 ? l[3] as? [Any] ?? [] : []).compactMap { $0 as? String }.map { PKPaymentNetwork($0) }
        r.paymentSummaryItems = items(l.count > 4 ? l[4] : nil)
        var caps: PKMerchantCapability = []
        for c in (l.count > 5 ? l[5] as? [Any] ?? [] : []).compactMap({ $0 as? String }) {
            switch c {
            case "EMV": caps.insert(.emv)
            case "Credit": caps.insert(.credit)
            case "Debit": caps.insert(.debit)
            default: caps.insert(.threeDSecure)
            }
        }
        r.merchantCapabilities = caps
        r.requiredShippingContactFields = contactFields(l.count > 6 ? l[6] : nil)
        r.requiredBillingContactFields = contactFields(l.count > 7 ? l[7] : nil)
        r.shippingMethods = (l.count > 8 ? l[8] as? [Any] ?? [] : []).compactMap { $0 as? [Any] }.map { m in
            let method = PKShippingMethod(label: m.count > 1 ? m[1] as? String ?? "" : "", amount: NSDecimalNumber(string: m.count > 2 ? m[2] as? String : "0"))
            method.identifier = m.first as? String
            method.detail = m.count > 3 ? m[3] as? String : nil
            return method
        }
        if #available(iOS 15.0, macOS 12.0, watchOS 8.0, *) {
            r.supportsCouponCode = l.count > 9 && (l[9] as? NSNumber)?.boolValue == true
        }
        return r
    }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
private struct KotlinApplePayButton: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let labels: [PayWithApplePayButtonLabel] = [.plain, .buy, .addMoney, .book, .checkout, .continue, .contribute, .donate, .inStore,
                                                    .order, .reload, .rent, .setUp, .subscribe, .support, .tip, .topUp]
        let label = labels[min(max(a.int(0), 0), labels.count - 1)]
        let fallback = a.slotModel(6)
        if a.isNil(1) {
            if let fallback {
                PayWithApplePayButton(label, action: a.action(5)) { NodeView(model: fallback) }
            } else {
                PayWithApplePayButton(label, action: a.action(5))
            }
        } else {
            let request = PassValues.request(a.list(1))
            let authorize = a.raw(2)
            let finish = a.action(3)
            let onChange = { (phase: PayWithApplePayButtonPaymentAuthorizationPhase) in
                switch phase {
                case .willAuthorize: break
                case .didAuthorize(let payment, let resultHandler):
                    let info: NSArray = [payment.token.paymentData.base64EncodedString(), payment.token.transactionIdentifier,
                                         (payment.token.paymentMethod.network?.rawValue as Any?) ?? NSNull(),
                                         (payment.shippingMethod?.identifier as Any?) ?? NSNull(),
                                         (payment.shippingContact?.emailAddress as Any?) ?? NSNull()]
                    let status = (kotlinInvoke(authorize, [info]) as? NSNumber)?.intValue ?? 1
                    resultHandler(PKPaymentAuthorizationResult(status: status == 0 ? .success : .failure, errors: nil))
                case .didFinish: finish()
                @unknown default: break
                }
            }
            if !a.isNil(4) {
                let session = a.raw(4)
                let merchant = { () async -> PKPaymentRequestMerchantSessionUpdate in
                    let json = await MainActor.run { kotlinInvoke(session, []) as? String ?? "{}" }
                    let dict = (try? JSONSerialization.jsonObject(with: Data(json.utf8))) as? [String: Any] ?? [:]
                    return PKPaymentRequestMerchantSessionUpdate(status: .success, merchantSession: PKPaymentMerchantSession(dictionary: dict))
                }
                if let fallback {
                    PayWithApplePayButton(label, request: request, onPaymentAuthorizationChange: onChange, onMerchantSessionRequested: merchant) { NodeView(model: fallback) }
                } else {
                    PayWithApplePayButton(label, request: request, onPaymentAuthorizationChange: onChange, onMerchantSessionRequested: merchant)
                }
            } else if let fallback {
                PayWithApplePayButton(label, request: request, onPaymentAuthorizationChange: onChange) { NodeView(model: fallback) }
            } else {
                PayWithApplePayButton(label, request: request, onPaymentAuthorizationChange: onChange)
            }
        }
    }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
private struct KotlinAddPassButton: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let passes = a.list(0).compactMap { ($0 as? String).flatMap { Data(base64Encoded: $0) } }.compactMap { try? PKPass(data: $0) }
        let done = a.callback(1, box: { (b: Bool) -> Any? in KotlinBoolean(value: b) })
        let fallback = a.slotModel(3)
        if passes.isEmpty {
            AddPassToWalletButton(action: a.action(2))
        } else if let fallback {
            AddPassToWalletButton(passes) { done($0) } fallback: { NodeView(model: fallback) }
        } else {
            AddPassToWalletButton(passes) { done($0) }
        }
    }
}

#if os(iOS) || os(visionOS)
private struct KotlinVerifyIdentity: View {
    let m: NodeModel

    static func request(merchant: String, nonce: String?, elements: [String]) -> PKIdentityRequest {
        let request = PKIdentityRequest()
        request.merchantIdentifier = merchant
        request.nonce = Data(base64Encoded: nonce ?? "") ?? Data()
        let descriptor = PKIdentityDriversLicenseDescriptor()
        for e in elements {
            let element: PKIdentityElement?
            switch e {
            case "givenName": element = .givenName
            case "familyName": element = .familyName
            case "portrait": element = .portrait
            case "address": element = .address
            case "issuingAuthority": element = .issuingAuthority
            case "documentNumber": element = .documentNumber
            case "dateOfBirth": element = .dateOfBirth
            case "documentExpirationDate": element = .documentExpirationDate
            case "documentIssueDate": element = .documentIssueDate
            case "drivingPrivileges": element = .drivingPrivileges
            default:
                if e.hasPrefix("age:"), let n = Int(e.dropFirst(4)) { element = .age(atLeast: n) } else { element = nil }
            }
            if let element { descriptor.addElements([element], intentToStore: .willNotStore) }
        }
        request.descriptor = descriptor
        return request
    }

    var body: some View {
        let a = m.args
        let labels: [VerifyIdentityWithWalletButtonLabel] = [.verifyIdentity, .verifyAge, .verify, .continue]
        let label = labels[min(max(a.int(0), 0), 3)]
        let done = a.callback(4, box: { (x: NSArray) -> Any? in x })
        let fallback = a.slotModel(6)
        if let merchant = a.stringOpt(1) {
            let request = Self.request(merchant: merchant, nonce: a.stringOpt(2), elements: a.list(3).compactMap { $0 as? String })
            let completion = { (r: Result<PKIdentityDocument, any Error>) in
                switch r {
                case .success(let doc): done([doc.encryptedData.base64EncodedString(), NSNull()] as NSArray)
                case .failure(let e): done([NSNull(), e.localizedDescription] as NSArray)
                }
            }
            if let fallback {
                VerifyIdentityWithWalletButton(label, request: request, onCompletion: completion) { NodeView(model: fallback) }
            } else {
                VerifyIdentityWithWalletButton(label, request: request, onCompletion: completion)
            }
        } else {
            VerifyIdentityWithWalletButton(label, action: a.action(5))
        }
    }
}

@available(iOS 27.0, *)
private struct KotlinMerchandising: View {
    let m: NodeModel

    var body: some View {
        let a = m.args
        let ids = a.list(7).compactMap { $0 as? String }
        let partners: ApplePayMerchandisingPartnerConfiguration = a.int(5) == 2 ? .none : a.int(5) == 1 ? .limited(partnerIdentifiers: ids) : .all
        let view = ApplePayMerchandisingView(amount: Decimal(string: a.string(0)) ?? 0, currency: Locale.Currency(a.string(1)),
                                             region: Locale.Region(a.string(2)), action: ApplePayMerchandisingAction(rawValue: a.string(3)),
                                             style: ApplePayMerchandisingStyle(rawValue: a.string(4)), partners: partners) {
            SlotView(model: a.slotModel(6))
        }
        return view
    }
}
#endif

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@MainActor private enum PassModifiers {
    static func style(_ v: AnyView, _ a: Args) -> AnyView {
        switch a.int(0) {
        case 0:
            var view = v
            if let i = a.intOpt(1) {
                let styles: [PayWithApplePayButtonStyle] = [.automatic, .black, .white, .whiteOutline]
                view = AnyView(view.payWithApplePayButtonStyle(styles[min(max(i, 0), 3)]))
            }
            #if os(iOS) || os(visionOS)
            if a.bool(2), #available(iOS 26.0, *) { view = AnyView(view.payWithApplePayButtonDisableCardArt()) }
            #endif
            return view
        case 1:
            return AnyView(v.addPassToWalletButtonStyle(a.int(1) == 1 ? .blackOutline : .black))
        case 2:
            #if os(iOS) || os(visionOS)
            return AnyView(v.verifyIdentityWithWalletButtonStyle(a.int(1) == 1 ? .blackOutline : .black))
            #else
            return v
            #endif
        default:
            #if os(iOS) || os(macOS) || os(visionOS)
            guard #available(iOS 17.0, macOS 14.0, *) else { return v }
            var view = v
            let l = a.list(1)
            if let s = l.first as? NSNumber {
                let styles: [PayLaterViewDisplayStyle] = [.standard, .badge, .checkout, .price]
                view = AnyView(view.payLaterViewDisplayStyle(styles[min(max(s.intValue, 0), 3)]))
            }
            if l.count > 1, let s = l[1] as? NSNumber {
                view = AnyView(view.payLaterViewAction(s.intValue == 1 ? .calculator : .learnMore))
            }
            return view
            #else
            return v
            #endif
        }
    }

    static func changes(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        func items(_ f: Any?, _ args: [Any]) -> [PKPaymentSummaryItem]? {
            guard let r = kotlinInvoke(f, args) else { return nil }
            if r is NSNull { return nil }
            return PassValues.items(r)
        }
        if !a.isNil(0) {
            let f = a.raw(0)
            v = AnyView(v.onApplePayShippingContactChange { contact in
                let r = await MainActor.run { items(f, [(contact.postalAddress?.postalCode as Any?) ?? NSNull(), (contact.postalAddress?.isoCountryCode as Any?) ?? NSNull()]) }
                return PKPaymentRequestShippingContactUpdate(errors: nil, paymentSummaryItems: r ?? [], shippingMethods: [])
            })
        }
        if !a.isNil(1) {
            let f = a.raw(1)
            v = AnyView(v.onApplePayShippingMethodChange { method in
                let r = await MainActor.run { items(f, [method.identifier ?? ""]) }
                return PKPaymentRequestShippingMethodUpdate(paymentSummaryItems: r ?? [])
            })
        }
        if !a.isNil(2) {
            let f = a.raw(2)
            v = AnyView(v.onApplePayPaymentMethodChange { method in
                let r = await MainActor.run { items(f, [(method.network?.rawValue as Any?) ?? NSNull()]) }
                return PKPaymentRequestPaymentMethodUpdate(paymentSummaryItems: r ?? [])
            })
        }
        if !a.isNil(3) {
            let f = a.raw(3)
            v = AnyView(v.onApplePayCouponCodeChange { code in
                let r = await MainActor.run { items(f, [code]) }
                return PKPaymentRequestCouponCodeUpdate(paymentSummaryItems: r ?? [])
            })
        }
        return v
    }
}
#endif
