// StoreKit for Compose SwiftUI: renders the Kotlin ProductView, StoreView, SubscriptionStoreView, store content,
// store modifiers and sheets (runtime StoreKit.kt). Add this file to the app target and call
// `ComposeIntegrations.registerStoreKit()` at launch.
import SwiftUI
import StoreKit

#if os(iOS) || os(macOS) || os(visionOS)
public extension ComposeIntegrations {
    @MainActor static func registerStoreKit() {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return }
        ComposeRegistry.views[200_200] = { m in ModChain.apply(KotlinProductView(m: m), m, 0) }
        ComposeRegistry.views[200_201] = { m in ModChain.apply(KotlinStoreView(m: m), m, 0) }
        ComposeRegistry.views[200_202] = { m in ModChain.apply(KotlinSubscriptionStoreView(m: m), m, 0) }
        #if os(iOS) || os(visionOS)
        if #available(iOS 26.0, *) {
            ComposeRegistry.views[200_204] = { m in ModChain.apply(KotlinSubscriptionOfferView(m: m), m, 0) }
            ComposeRegistry.modifiers[200_202] = { v, a, _ in StoreModifiers.offerViewStyle(v, a) }
        }
        #endif
        ComposeRegistry.modifiers[200_200] = { v, a, _ in
            let styles: [ProductViewStyleKind] = [.automatic, .compact, .regular, .large]
            return StoreModifiers.productViewStyle(v, styles[min(max(a.int(0), 0), 3)])
        }
        ComposeRegistry.modifiers[200_201] = { v, a, _ in StoreModifiers.storeStyle(v, a.list(0)) }
        ComposeRegistry.modifiers[200_203] = { v, a, _ in AnyView(v.subscriptionStoreSignInAction(a.actionOpt(0))) }
        ComposeRegistry.modifiers[200_204] = { v, a, _ in StoreModifiers.policy(v, a) }
        ComposeRegistry.modifiers[200_205] = { v, a, _ in StoreModifiers.controlIcon(v, a) }
        ComposeRegistry.modifiers[200_206] = { v, a, _ in StoreModifiers.sheet(v, a) }
        ComposeRegistry.modifiers[200_207] = { v, a, _ in StoreModifiers.purchaseCallbacks(v, a) }
        ComposeRegistry.modifiers[200_208] = { v, a, _ in StoreModifiers.offers(v, a) }
        ComposeRegistry.modifiers[200_209] = { v, a, _ in AnyView(v.modifier(StoreTaskModifier(a: a))) }
        ComposeRegistry.modifiers[200_210] = { v, a, _ in AnyView(v.modifier(StoreActionModifier(a: a))) }
        ComposeRegistry.modifiers[200_211] = { v, _, _ in AnyView(v.productIconBorder()) }
        ComposeRegistry.values[900] = { _ in ContainerBackgroundPlacement.subscriptionStore }
        ComposeRegistry.values[901] = { _ in ContainerBackgroundPlacement.subscriptionStoreHeader }
        ComposeRegistry.values[902] = { _ in ContainerBackgroundPlacement.subscriptionStoreFullHeight }
    }
}

// MARK: - Encoding for Kotlin

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
enum StoreEncoding {
    /// `RenewalState` by name, as the Kotlin `StoreSubscriptionStatus.state` documents it.
    static func renewalState(_ s: Product.SubscriptionInfo.RenewalState) -> String {
        switch s {
        case .subscribed: return "subscribed"
        case .expired: return "expired"
        case .inBillingRetryPeriod: return "inBillingRetryPeriod"
        case .inGracePeriod: return "inGracePeriod"
        case .revoked: return "revoked"
        default: return "unknown(\(s.rawValue))"
        }
    }

    static func product(_ p: Product) -> NSArray {
        [p.id, p.displayName, p.description, p.displayPrice, KotlinDouble(value: NSDecimalNumber(decimal: p.price).doubleValue),
         p.type.rawValue, (p.subscription?.subscriptionGroupID as Any?) ?? NSNull(), KotlinBoolean(value: p.isFamilyShareable)] as NSArray
    }

    static func transaction(_ v: VerificationResult<StoreKit.Transaction>) -> NSArray {
        let t: StoreKit.Transaction, verified: Bool
        switch v {
        case .verified(let x): t = x; verified = true
        case .unverified(let x, _): t = x; verified = false
        }
        return [NSNumber(value: t.id), NSNumber(value: t.originalID), t.productID, KotlinDouble(value: t.purchaseDate.timeIntervalSince1970),
                t.expirationDate.map { KotlinDouble(value: $0.timeIntervalSince1970) } ?? NSNull(),
                t.revocationDate.map { KotlinDouble(value: $0.timeIntervalSince1970) } ?? NSNull(),
                KotlinBoolean(value: t.isUpgraded), t.ownershipType.rawValue, KotlinBoolean(value: verified)] as NSArray
    }

    static func purchase(_ r: Result<Product.PurchaseResult, any Error>) -> NSArray {
        switch r {
        case .success(.success(let v)): return [KotlinInt(value: 0), transaction(v)] as NSArray
        case .success(.userCancelled): return [KotlinInt(value: 1)] as NSArray
        case .success(.pending): return [KotlinInt(value: 2)] as NSArray
        case .success: return [KotlinInt(value: 3), "unknown purchase result"] as NSArray
        case .failure(let e): return [KotlinInt(value: 3), e.localizedDescription] as NSArray
        }
    }

    static func options(_ v: Any?) -> Set<Product.PurchaseOption> {
        guard let l = v as? [Any] else { return [] }
        var set = Set<Product.PurchaseOption>()
        if l.count > 0, let q = l[0] as? NSNumber { set.insert(.quantity(q.intValue)) }
        if l.count > 1, let s = l[1] as? String, let uuid = UUID(uuidString: s) { set.insert(.appAccountToken(uuid)) }
        if l.count > 2, let b = l[2] as? NSNumber { set.insert(.simulatesAskToBuyInSandbox(b.boolValue)) }
        return set
    }

    static func relationship(_ raw: Int) -> Product.SubscriptionRelationship {
        var r: Product.SubscriptionRelationship = []
        if raw & 1 != 0 { r.insert(.current) }
        if raw & 2 != 0 { r.insert(.upgrade) }
        if raw & 4 != 0 { r.insert(.downgrade) }
        if raw & 8 != 0 { r.insert(.crossgrade) }
        return r
    }

    /// Calls a synchronous Kotlin function.
    static func call(_ f: Any?, _ args: [Any]) -> Any? {
        kotlinInvoke(f, args)
    }
}

// MARK: - Views

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinProductView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let id = a.string(0), promo = a.bool(1)
        if let phased = a.slotModel(4) {
            let report = a.callback(5, box: { (i: Int) -> Any? in KotlinInt(value: Int32(i)) })
            ProductView(id: id) { phase in
                ProductIconPhaseView(phase: phase, slot: phased, report: report)
            } placeholderIcon: {
                SlotView(model: a.slotModel(3))
            }
        } else if let icon = a.slotModel(2) {
            if let placeholder = a.slotModel(3) {
                ProductView(id: id, prefersPromotionalIcon: promo) { NodeView(model: icon) } placeholderIcon: { NodeView(model: placeholder) }
            } else {
                ProductView(id: id, prefersPromotionalIcon: promo) { NodeView(model: icon) }
            }
        } else {
            ProductView(id: id, prefersPromotionalIcon: promo)
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct ProductIconPhaseView: View {
    let phase: ProductIconPhase
    let slot: NodeModel
    let report: (Int) -> Void

    var body: some View {
        let index: Int
        let image: Image?
        switch phase {
        case .success(let i): index = 1; image = i
        case .failure: index = 2; image = nil
        case .unavailable: index = 3; image = nil
        default: index = 0; image = nil
        }
        return NodeView(model: slot)
            .environment(\.composeStyleContent, image.map { AnyView($0.resizable().scaledToFit()) })
            .onChange(of: index, initial: true) { _, i in report(i) }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinStoreView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let ids = a.list(0).compactMap { $0 as? String }
        let keys = a.list(2).compactMap { $0 as? String }
        let view: AnyView
        if keys.isEmpty {
            view = AnyView(StoreView(ids: ids, prefersPromotionalIcon: a.bool(1)))
        } else if let placeholder = a.slotModel(3) {
            view = AnyView(StoreView(ids: ids, prefersPromotionalIcon: a.bool(1)) { (product: Product) in
                iconView(product.id, keys)
            } placeholderIcon: { NodeView(model: placeholder) })
        } else {
            view = AnyView(StoreView(ids: ids, prefersPromotionalIcon: a.bool(1)) { (product: Product) in iconView(product.id, keys) })
        }
        return StoreModifiers.storeStyle(view, a.list(4))
    }

    @ViewBuilder private func iconView(_ id: String, _ keys: [String]) -> some View {
        if let i = keys.firstIndex(of: id), let slot = m.args.slotModel(i) { NodeView(model: slot) }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinSubscriptionStoreView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let group = a.stringOpt(0)
        let ids = a.list(1).compactMap { $0 as? String }
        let relationships = StoreEncoding.relationship(a.int(2))
        let marketing = a.slotModel(3)
        let view: AnyView
        if a.bool(4), #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            let children = m.children
            if let group {
                view = AnyView(SubscriptionStoreView(groupID: group, visibleRelationships: relationships) { StoreContentList(children: children) })
            } else {
                view = AnyView(SubscriptionStoreView(productIDs: ids) { StoreContentList(children: children) })
            }
        } else if let marketing {
            if let group {
                view = AnyView(SubscriptionStoreView(groupID: group, visibleRelationships: relationships) { NodeView(model: marketing) })
            } else {
                view = AnyView(SubscriptionStoreView(productIDs: ids) { NodeView(model: marketing) })
            }
        } else if let group {
            view = AnyView(SubscriptionStoreView(groupID: group, visibleRelationships: relationships))
        } else {
            view = AnyView(SubscriptionStoreView(productIDs: ids))
        }
        return StoreModifiers.storeStyle(view, a.list(5))
    }
}

#if os(iOS) || os(visionOS)
@available(iOS 26.0, *)
private struct KotlinSubscriptionOfferView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let icon = a.slotModel(5), placeholder = a.slotModel(6)
        if let id = a.stringOpt(0) {
            if let icon, let placeholder {
                SubscriptionOfferView(id: id, prefersPromotionalIcon: a.bool(3)) { NodeView(model: icon) } placeholderIcon: { NodeView(model: placeholder) }
            } else if let icon {
                SubscriptionOfferView(id: id, prefersPromotionalIcon: a.bool(3)) { NodeView(model: icon) }
            } else {
                SubscriptionOfferView(id: id, prefersPromotionalIcon: a.bool(3))
            }
        } else {
            let group = a.stringOpt(1) ?? ""
            let relationship = StoreEncoding.relationship(a.int(2))
            if let icon, let placeholder {
                SubscriptionOfferView(groupID: group, visibleRelationship: relationship) { NodeView(model: icon) } placeholderIcon: { NodeView(model: placeholder) }
            } else if let icon {
                SubscriptionOfferView(groupID: group, visibleRelationship: relationship) { NodeView(model: icon) }
            } else {
                SubscriptionOfferView(groupID: group, visibleRelationship: relationship, useAppIcon: a.bool(4))
            }
        }
    }
}
#endif

// MARK: - Store content

/// Up to eight Kotlin store content nodes (StoreContent has no type erasure or ForEach).
@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
@StoreContentBuilder @MainActor private func StoreContentList(children: [NodeModel]) -> some StoreContent {
    if children.count > 0 { StoreNode.top(children[0]) }
    if children.count > 1 { StoreNode.top(children[1]) }
    if children.count > 2 { StoreNode.top(children[2]) }
    if children.count > 3 { StoreNode.top(children[3]) }
    if children.count > 4 { StoreNode.top(children[4]) }
    if children.count > 5 { StoreNode.top(children[5]) }
    if children.count > 6 { StoreNode.top(children[6]) }
    if children.count > 7 { StoreNode.top(children[7]) }
}

@available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *)
@MainActor private enum StoreNode {
    static func include(_ f: Any?) -> (Product) -> Bool {
        { product in (StoreEncoding.call(f, [product.id]) as? NSNumber)?.boolValue ?? true }
    }

    @StoreContentBuilder static func top(_ m: NodeModel) -> some StoreContent {
        let a = m.args
        switch a.int(0) {
        case 0: styled(group(m), a.list(3))
        case 1: styled(section(m), a.list(3))
        case 2: styled(periodSet(m), a.list(3))
        default: styled(groupSet(m), a.list(3))
        }
    }

    /// Sections nested in a group.
    @StoreContentBuilder static func nested(_ children: [NodeModel]) -> some StoreContent {
        if children.count > 0 { styled(section(children[0]), children[0].args.list(3)) }
        if children.count > 1 { styled(section(children[1]), children[1].args.list(3)) }
        if children.count > 2 { styled(section(children[2]), children[2].args.list(3)) }
        if children.count > 3 { styled(section(children[3]), children[3].args.list(3)) }
        if children.count > 4 { styled(section(children[4]), children[4].args.list(3)) }
        if children.count > 5 { styled(section(children[5]), children[5].args.list(3)) }
    }

    @StoreContentBuilder static func group(_ m: NodeModel) -> some StoreContent {
        let a = m.args
        let label = a.stringOpt(1)
        let labelSlot = a.slotModel(4), marketing = a.slotModel(5)
        let filter = include(a.raw(2))
        if a.bool(6) {
            let children = m.children.filter { $0.args.int(0) == 1 }
            if let labelSlot, let marketing {
                SubscriptionOptionGroup { nested(children) } label: { NodeView(model: labelSlot) } marketingContent: { NodeView(model: marketing) }
            } else if let labelSlot {
                SubscriptionOptionGroup { nested(children) } label: { NodeView(model: labelSlot) }
            } else if let label {
                if let marketing {
                    SubscriptionOptionGroup(label) { nested(children) } marketingContent: { NodeView(model: marketing) }
                } else {
                    SubscriptionOptionGroup(label) { nested(children) }
                }
            } else if let marketing {
                SubscriptionOptionGroup { nested(children) } marketingContent: { NodeView(model: marketing) }
            } else {
                SubscriptionOptionGroup { nested(children) }
            }
        } else if let labelSlot, let marketing {
            SubscriptionOptionGroup(isIncluded: filter) { NodeView(model: labelSlot) } marketingContent: { NodeView(model: marketing) }
        } else if let labelSlot {
            SubscriptionOptionGroup(isIncluded: filter, label: { NodeView(model: labelSlot) })
        } else if let label {
            if let marketing {
                SubscriptionOptionGroup(label, isIncluded: filter) { NodeView(model: marketing) }
            } else {
                SubscriptionOptionGroup(label, isIncluded: filter)
            }
        } else if let marketing {
            SubscriptionOptionGroup(isIncluded: filter, marketingContent: { NodeView(model: marketing) })
        } else {
            SubscriptionOptionGroup(isIncluded: filter)
        }
    }

    @StoreContentBuilder static func section(_ m: NodeModel) -> some StoreContent {
        let a = m.args
        let filter = include(a.raw(2))
        let header = a.slotModel(4), footer = a.slotModel(5)
        if let title = a.stringOpt(1) {
            if let footer {
                SubscriptionOptionSection(title, isIncluded: filter) { NodeView(model: footer) }
            } else {
                SubscriptionOptionSection(title, isIncluded: filter)
            }
        } else {
            SubscriptionOptionSection(isIncluded: filter) { SlotView(model: header) } footer: { SlotView(model: footer) }
        }
    }

    private static func periodIndex(_ p: Product.SubscriptionPeriod?) -> Int {
        guard let p else { return 0 }
        switch p.unit {
        case .day: return 1
        case .week: return 2
        case .month: return 3
        case .year: return 4
        @unknown default: return 0
        }
    }

    @StoreContentBuilder static func periodSet(_ m: NodeModel) -> some StoreContent {
        let a = m.args
        let marketingRefs = a.list(4)
        let labelRefs = a.list(5)
        // slot index of each period, in Kotlin order: marketing slots first, then label slots
        let marketing = marketingRefs.indices.filter { !(marketingRefs[$0] is NSNull) }
        let labels = labelRefs.indices.filter { !(labelRefs[$0] is NSNull) }
        let slots = m.node
        let marketingSlot = { (i: Int) -> NodeModel? in
            marketing.firstIndex(of: i).flatMap { slots.slot(index: Int32($0)) }.map(NodeModel.of)
        }
        let labelSlot = { (i: Int) -> NodeModel? in
            labels.firstIndex(of: i).flatMap { slots.slot(index: Int32(marketing.count + $0)) }.map(NodeModel.of)
        }
        if labels.isEmpty {
            SubscriptionPeriodGroupSet { period in SlotView(model: marketingSlot(periodIndex(period))) }
        } else {
            SubscriptionPeriodGroupSet { period in SlotView(model: marketingSlot(periodIndex(period))) } label: { period in
                SlotView(model: labelSlot(periodIndex(period)))
            }
        }
    }

    @StoreContentBuilder static func groupSet(_ m: NodeModel) -> some StoreContent {
        let a = m.args
        let key = a.raw(2), label = a.raw(4), marketing = a.raw(5)
        if marketing != nil && !(marketing is NSNull) {
            SubscriptionOptionGroupSet { (p: Product) in StoreEncoding.call(key, [p.id]) as? String ?? "" } label: { (g: String) in
                Text(StoreEncoding.call(label, [g]) as? String ?? g)
            } marketingContent: { (g: String) in
                Text(StoreEncoding.call(marketing, [g]) as? String ?? "")
            }
        } else {
            SubscriptionOptionGroupSet { (p: Product) in StoreEncoding.call(key, [p.id]) as? String ?? "" } label: { (g: String) in
                Text(StoreEncoding.call(label, [g]) as? String ?? g)
            }
        }
    }

    // MARK: StoreContent modifiers

    /// The store modifiers of a content node. They are applied unconditionally with neutral defaults (`.automatic`):
    /// nesting conditional modifiers makes the result type explode and the type checker never finishes.
    static func styled<C: StoreContent>(_ c: C, _ s: [Any]) -> some StoreContent {
        let pairs = (at(s, 1) as? [Any] ?? []).compactMap { $0 as? [Any] }
        func vis(_ kind: Int) -> Visibility {
            pairs.first { ($0.first as? NSNumber)?.intValue == kind }.flatMap { $0.count == 2 ? d_Visibility($0[1] as? SExpr) : nil } ?? .automatic
        }
        let label = (at(s, 2) as? NSNumber).map { StoreModifiers.buttonLabels[min(max($0.intValue, 0), 5)] } ?? .automatic
        return c.productDescription(d_Visibility(at(s, 0) as? SExpr) ?? .automatic)
            .storeButton(vis(0), for: .cancellation)
            .storeButton(vis(1), for: .policies)
            .storeButton(StoreModifiers.redeemCodeAvailable ? vis(2) : vis(3), for: StoreModifiers.buttonKindList[2])
            .storeButton(vis(3), for: .restorePurchases)
            .storeButton(vis(4), for: .signIn)
            .subscriptionStoreButtonLabel(label)
    }

    private static func at(_ s: [Any], _ i: Int) -> Any? { i < s.count && !(s[i] is NSNull) ? s[i] : nil }








}

// MARK: - Modifiers

enum ProductViewStyleKind { case automatic, compact, regular, large }

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor enum StoreModifiers {
    static let buttonLabels: [SubscriptionStoreButtonLabel] = [.automatic, .action, .displayName, .price, .multiline, .singleLine]
    /// `.redeemCode` needs macOS 15: before, its slot repeats `.restorePurchases` with the restore visibility.
    static let redeemCodeAvailable: Bool = { if #available(iOS 17.0, macOS 15.0, *) { return true } else { return false } }()

    static let buttonKindList: [StoreButtonKind] = {
        if #available(iOS 17.0, macOS 15.0, *) { return [.cancellation, .policies, .redeemCode, .restorePurchases, .signIn] }
        return [.cancellation, .policies, .restorePurchases, .restorePurchases, .signIn]
    }()

    static func productViewStyle(_ v: AnyView, _ s: ProductViewStyleKind) -> AnyView {
        switch s {
        case .compact: return AnyView(v.productViewStyle(.compact))
        case .regular: return AnyView(v.productViewStyle(.regular))
        case .large: return AnyView(v.productViewStyle(.large))
        case .automatic: return AnyView(v.productViewStyle(.automatic))
        }
    }

    private static func at(_ s: [Any], _ i: Int) -> Any? { i < s.count && !(s[i] is NSNull) ? s[i] : nil }

    static func storeStyle(_ view: AnyView, _ s: [Any]) -> AnyView {
        var v = view
        if let vis = d_Visibility(at(s, 0) as? SExpr) { v = AnyView(v.productDescription(vis)) }
        for p in (at(s, 1) as? [Any] ?? []).compactMap({ $0 as? [Any] }) where p.count == 2 {
            let kind = buttonKindList[min(max((p[0] as? NSNumber)?.intValue ?? 0, 0), 4)]
            v = AnyView(v.storeButton(d_Visibility(p[1] as? SExpr) ?? .automatic, for: kind))
        }
        if let i = (at(s, 2) as? NSNumber)?.intValue { v = AnyView(v.subscriptionStoreButtonLabel(buttonLabels[min(max(i, 0), 5)])) }
        if let i = (at(s, 3) as? NSNumber)?.intValue { v = controlStyle(v, i, (at(s, 4) as? NSNumber)?.intValue ?? 0) }
        if let style = dp_ShapeStyle(at(s, 6) as? SExpr) {
            v = AnyView(v.subscriptionStoreControlBackground(AnyShapeStyle(style)))
        } else if let i = (at(s, 5) as? NSNumber)?.intValue {
            #if os(macOS)
            v = AnyView(v.subscriptionStoreControlBackground(i == 0 ? .automatic : .gradientMaterial))
            #else
            v = AnyView(v.subscriptionStoreControlBackground(i == 1 ? .gradientMaterial : i == 2 ? .gradientMaterialOnScroll : .automatic))
            #endif
        }
        if let style = dp_ShapeStyle(at(s, 7) as? SExpr) {
            if let shape = dp_Shape(at(s, 8) as? SExpr), #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
                v = AnyView(v.subscriptionStorePickerItemBackground(AnyShapeStyle(style), in: AnyShape(shape)))
            } else {
                v = AnyView(v.subscriptionStorePickerItemBackground(AnyShapeStyle(style)))
            }
        }
        if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            switch (at(s, 9) as? NSNumber)?.intValue {
            case 0?: v = AnyView(v.subscriptionStoreOptionGroupStyle(.automatic))
            case 1?: v = AnyView(v.subscriptionStoreOptionGroupStyle(.links))
            case 2?: v = AnyView(v.subscriptionStoreOptionGroupStyle(.tabs))
            default: break
            }
        }
        return v
    }

    private static func controlStyle(_ v: AnyView, _ style: Int, _ placement: Int) -> AnyView {
        guard #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) else {
            switch style {
            case 1: return AnyView(v.subscriptionStoreControlStyle(.buttons))
            case 2: return AnyView(v.subscriptionStoreControlStyle(.picker))
            case 3: return AnyView(v.subscriptionStoreControlStyle(.prominentPicker))
            default: return AnyView(v.subscriptionStoreControlStyle(.automatic))
            }
        }
        switch style {
        case 1:
            switch placement {
            case 1: return AnyView(v.subscriptionStoreControlStyle(.buttons, placement: .scrollView))
            case 2: return AnyView(v.subscriptionStoreControlStyle(.buttons, placement: .bottomBar))
            default: return AnyView(v.subscriptionStoreControlStyle(.buttons))
            }
        case 2:
            switch placement {
            case 1: return AnyView(v.subscriptionStoreControlStyle(.picker, placement: .scrollView))
            case 3: return AnyView(v.subscriptionStoreControlStyle(.picker, placement: .buttonsInBottomBar))
            default: return AnyView(v.subscriptionStoreControlStyle(.picker))
            }
        case 3: return AnyView(v.subscriptionStoreControlStyle(.prominentPicker))
        case 4:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 18.0, macOS 15.0, *) {
                switch placement {
                case 1: return AnyView(v.subscriptionStoreControlStyle(.compactPicker, placement: .scrollView))
                case 2: return AnyView(v.subscriptionStoreControlStyle(.compactPicker, placement: .bottomBar))
                case 3: return AnyView(v.subscriptionStoreControlStyle(.compactPicker, placement: .buttonsInBottomBar))
                default: return AnyView(v.subscriptionStoreControlStyle(.compactPicker))
                }
            }
            #endif
            return v
        case 5:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 18.0, macOS 15.0, *) {
                switch placement {
                case 1: return AnyView(v.subscriptionStoreControlStyle(.pagedPicker, placement: .scrollView))
                case 2: return AnyView(v.subscriptionStoreControlStyle(.pagedPicker, placement: .bottomBar))
                case 3: return AnyView(v.subscriptionStoreControlStyle(.pagedPicker, placement: .buttonsInBottomBar))
                default: return AnyView(v.subscriptionStoreControlStyle(.pagedPicker))
                }
            }
            #endif
            return v
        case 6:
            #if os(iOS) || os(macOS) || os(visionOS)
            if #available(iOS 18.0, macOS 15.0, *) { return AnyView(v.subscriptionStoreControlStyle(.pagedProminentPicker)) }
            #endif
            return v
        default:
            return AnyView(v.subscriptionStoreControlStyle(.automatic))
        }
    }

    #if os(iOS) || os(visionOS)
    @available(iOS 26.0, *)
    static func offerViewStyle(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if let s = a.intOpt(0) { v = AnyView(s == 1 ? AnyView(v.subscriptionOfferViewStyle(.compact)) : AnyView(v.subscriptionOfferViewStyle(.automatic))) }
        if let vis = d_Visibility(a.expr(1)) { v = AnyView(v.subscriptionOfferViewButtonVisibility(vis, for: .detailLink)) }
        if let action = a.actionOpt(2) { v = AnyView(v.subscriptionOfferViewDetailAction(action)) }
        return v
    }
    #endif

    static func policy(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if let k = a.intOpt(0) {
            let kind: SubscriptionStorePolicyKind = k == 1 ? .termsOfService : .privacyPolicy
            if let s = a.stringOpt(1), let url = URL(string: s) {
                v = AnyView(v.subscriptionStorePolicyDestination(url: url, for: kind))
            } else if let slot = a.slotModel(2) {
                v = AnyView(v.subscriptionStorePolicyDestination(for: kind) { NodeView(model: slot) })
            }
        }
        if let primary = dp_ShapeStyle(a.expr(3)) {
            if let secondary = dp_ShapeStyle(a.expr(4)) {
                v = AnyView(v.subscriptionStorePolicyForegroundStyle(AnyShapeStyle(primary), AnyShapeStyle(secondary)))
            } else {
                v = AnyView(v.subscriptionStorePolicyForegroundStyle(AnyShapeStyle(primary)))
            }
        }
        return v
    }

    static func controlIcon(_ v: AnyView, _ a: Args) -> AnyView {
        let keys = a.list(0).compactMap { $0 as? String }
        return AnyView(v.subscriptionStoreControlIcon { product, _ in
            ControlIcon(a: a, index: keys.firstIndex(of: product.id))
        })
    }

    static func sheet(_ v: AnyView, _ a: Args) -> AnyView {
        switch a.int(0) {
        case 0:
            #if os(iOS) || os(visionOS)
            if let group = a.stringOpt(3), #available(iOS 18.0, *) {
                return AnyView(v.manageSubscriptionsSheet(isPresented: a.boolBinding(1), subscriptionGroupID: group))
            }
            #endif
            #if os(iOS) || os(visionOS)
            return AnyView(v.manageSubscriptionsSheet(isPresented: a.boolBinding(1)))
            #else
            return v
            #endif
        case 1:
            #if os(iOS) || os(macOS) || os(visionOS)
            guard #available(iOS 17.0, macOS 15.0, *) else { return v }
            let done = a.callback(4, box: { (s: String?) -> Any? in s ?? NSNull() })
            return AnyView(v.offerCodeRedemption(isPresented: a.boolBinding(1)) { result in
                if case .failure(let e) = result { done(e.localizedDescription) } else { done(nil) }
            })
            #else
            return v
            #endif
        case 2:
            #if os(iOS) || os(macOS) || os(visionOS)
            let done = a.callback(4, box: { (s: String) -> Any? in s })
            return AnyView(v.refundRequestSheet(for: UInt64(a.double(3)), isPresented: a.boolBinding(1)) { result in
                switch result {
                case .success(.success): done("success")
                case .success(.userCancelled): done("userCancelled")
                case .success: done("unknown")
                case .failure(let e): done(e.localizedDescription)
                }
            })
            #else
            return v
            #endif
        default:
            #if os(iOS)
            let l = a.list(3)
            let app = l.first as? String ?? ""
            let raised = l.count > 1 && (l[1] as? NSNumber)?.intValue == 1
            return AnyView(v.appStoreOverlay(isPresented: a.boolBinding(1)) {
                SKOverlay.AppConfiguration(appIdentifier: app, position: raised ? .bottomRaised : .bottom)
            })
            #else
            return v
            #endif
        }
    }

    static func purchaseCallbacks(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if !a.isNil(0) {
            let start = a.callback(0, box: { (s: String) -> Any? in s })
            v = AnyView(v.onInAppPurchaseStart { product in await MainActor.run { start(product.id) } })
        }
        if !a.isNil(1) {
            let f = a.raw(1)
            v = AnyView(v.inAppPurchaseOptions { product in
                await MainActor.run { StoreEncoding.options(StoreEncoding.call(f, [product.id])) }
            })
        }
        if !a.isNil(2) {
            let done = a.callback(2, box: { (x: NSArray) -> Any? in x })
            v = AnyView(v.onInAppPurchaseCompletion { product, result in
                await MainActor.run { done([product.id, StoreEncoding.purchase(result)] as NSArray) }
            })
        }
        return v
    }

    static func offers(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        let promo = a.raw(2), signature = a.raw(3), jws = a.raw(4), preferred = a.raw(5)
        #if os(iOS) || os(macOS) || os(visionOS)
        if !a.isNil(2), !a.isNil(3), #available(iOS 18.0, macOS 15.0, *) {
            v = AnyView(v.subscriptionPromotionalOffer { product, info in
                let ids = info.promotionalOffers.compactMap { $0.id }
                let chosen = StoreEncoding.call(promo, [product.id, ids]) as? String
                return info.promotionalOffers.first { $0.id == chosen }
            } signature: { product, _, offer in
                let l = await MainActor.run { StoreEncoding.call(signature, [product.id, offer.id ?? ""]) as? [Any] ?? [] }
                return Product.SubscriptionOffer.Signature(keyID: l.first as? String ?? "",
                                                           nonce: UUID(uuidString: l.count > 1 ? l[1] as? String ?? "" : "") ?? UUID(),
                                                           timestamp: l.count > 2 ? (l[2] as? NSNumber)?.intValue ?? 0 : 0,
                                                           signature: Data(base64Encoded: l.count > 3 ? l[3] as? String ?? "" : "") ?? Data())
            })
        }
        if #available(iOS 26.0, macOS 26.0, *) {
            if !a.isNil(2), !a.isNil(4) {
                v = AnyView(v.subscriptionPromotionalOffer { product, info in
                    let chosen = StoreEncoding.call(promo, [product.id, info.promotionalOffers.compactMap { $0.id }]) as? String
                    return info.promotionalOffers.first { $0.id == chosen }
                } compactJWS: { product, _, offer in
                    await MainActor.run { StoreEncoding.call(jws, [product.id, offer.id ?? ""]) as? String ?? "" }
                })
            }
            if !a.isNil(0), !a.isNil(1) {
                let apply = a.raw(0), introJWS = a.raw(1)
                v = AnyView(v.subscriptionIntroductoryOffer { product, _ in
                    (StoreEncoding.call(apply, [product.id]) as? NSNumber)?.boolValue ?? false
                } compactJWS: { product, _ in
                    await MainActor.run { StoreEncoding.call(introJWS, [product.id]) as? String ?? "" }
                })
            }
        }
        if !a.isNil(5), #available(iOS 18.4, macOS 15.4, *) {
            v = AnyView(v.preferredSubscriptionOffer { product, _, offers in
                let chosen = StoreEncoding.call(preferred, [product.id, offers.compactMap { $0.id }]) as? String
                return offers.first { $0.id == chosen }
            })
        }
        #endif
        _ = (promo, signature, jws, preferred)
        return v
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct ControlIcon: View {
    let a: Args
    let index: Int?

    var body: some View {
        // slots: 1 = default icon, 2… = one per product id
        SlotView(model: index.flatMap { a.slotModel(2 + $0) } ?? a.slotModel(1))
    }
}

// MARK: - Tasks and actions

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct StoreTaskModifier: ViewModifier {
    let a: Args

    func body(content: Content) -> some View {
        let ids = a.list(1).compactMap { $0 as? String }
        let report = a.callback(2, box: { (x: NSArray) -> Any? in x })
        func send(_ x: [Any]) { report(x as NSArray) }
        switch a.int(0) {
        case 0:
            return AnyView(content.storeProductTask(for: ids.first ?? "") { state in
                await MainActor.run {
                    switch state {
                    case .loading: send([KotlinInt(value: 0)])
                    case .unavailable: send([KotlinInt(value: 1)])
                    case .failure(let e): send([KotlinInt(value: 2), e.localizedDescription])
                    case .success(let p): send([KotlinInt(value: 3), StoreEncoding.product(p)])
                    @unknown default: send([KotlinInt(value: 0)])
                    }
                }
            })
        case 1:
            return AnyView(content.storeProductsTask(for: ids) { state in
                await MainActor.run {
                    switch state {
                    case .loading: send([KotlinInt(value: 0)])
                    case .failure(let e): send([KotlinInt(value: 2), e.localizedDescription])
                    case .success(let products, let unavailable):
                        send([KotlinInt(value: 3), products.map(StoreEncoding.product) as NSArray, unavailable as NSArray])
                    @unknown default: send([KotlinInt(value: 0)])
                    }
                }
            })
        case 2:
            return AnyView(content.currentEntitlementTask(for: ids.first ?? "") { state in
                await MainActor.run {
                    switch state {
                    case .loading: send([KotlinInt(value: 0)])
                    case .failure(let e): send([KotlinInt(value: 2), e.localizedDescription])
                    case .success(let t): send([KotlinInt(value: 3), t.map(StoreEncoding.transaction) ?? NSNull()])
                    @unknown default: send([KotlinInt(value: 0)])
                    }
                }
            })
        default:
            return AnyView(content.subscriptionStatusTask(for: ids.first ?? "") { state in
                await MainActor.run {
                    switch state {
                    case .loading: send([KotlinInt(value: 0)])
                    case .failure(let e): send([KotlinInt(value: 2), e.localizedDescription])
                    case .success(let statuses):
                        let list = statuses.map { s -> NSArray in
                            var renewal: Product.SubscriptionInfo.RenewalInfo?
                            if case .verified(let r) = s.renewalInfo { renewal = r }
                            return [StoreEncoding.renewalState(s.state), StoreEncoding.transaction(s.transaction),
                                    renewal.map { KotlinBoolean(value: $0.willAutoRenew) } ?? NSNull(),
                                    (renewal?.autoRenewPreference as Any?) ?? NSNull()] as NSArray
                        }
                        send([KotlinInt(value: 3), list as NSArray])
                    @unknown default: send([KotlinInt(value: 0)])
                    }
                }
            })
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct StoreActionModifier: ViewModifier {
    let a: Args
    @Environment(\.requestReview) private var requestReview
    @Environment(\.purchase) private var purchase
    #if os(iOS) || os(visionOS)
    @Environment(\.displayStoreKitMessage) private var displayMessage
    #endif

    func body(content: Content) -> some View {
        content
            .onChange(of: a.hashable(0)) { _, _ in run() }
            .task(id: a.bool(5)) {
                #if os(iOS) || os(visionOS)
                guard a.bool(5) else { return }
                for await message in Message.messages {
                    try? displayMessage(message)
                }
                #endif
            }
    }

    private func run() {
        if a.bool(1) { requestReview() }
        guard let id = a.stringOpt(2) else { return }
        let options = StoreEncoding.options(a.list(3))
        let report = a.isNil(4) ? nil : a.callback(4, box: { (x: NSArray) -> Any? in x })
        let purchase = self.purchase
        Task { @MainActor in
            do {
                guard let product = try await Product.products(for: [id]).first else {
                    report?([KotlinInt(value: 3), "product \(id) not found"] as NSArray)
                    return
                }
                let result = try await purchase(product, options: options)
                report?(StoreEncoding.purchase(.success(result)))
            } catch {
                report?(StoreEncoding.purchase(.failure(error)))
            }
        }
    }
}

#else
// tvOS, watchOS: the store views, without the store modifiers and sheets (StoreKit doesn't offer them there).
public extension ComposeIntegrations {
    @MainActor static func registerStoreKit() {
        guard #available(tvOS 17.0, watchOS 10.0, *) else { return }
        ComposeRegistry.views[200_200] = { m in ModChain.apply(ProductView(id: m.args.string(0), prefersPromotionalIcon: m.args.bool(1)), m, 0) }
        ComposeRegistry.views[200_201] = { m in
            ModChain.apply(StoreView(ids: m.args.list(0).compactMap { $0 as? String }, prefersPromotionalIcon: m.args.bool(1)), m, 0)
        }
        ComposeRegistry.views[200_202] = { m in
            let a = m.args
            if let group = a.stringOpt(0) { return ModChain.apply(SubscriptionStoreView(groupID: group), m, 0) }
            return ModChain.apply(SubscriptionStoreView(productIDs: a.list(1).compactMap { $0 as? String }), m, 0)
        }
    }
}
#endif
