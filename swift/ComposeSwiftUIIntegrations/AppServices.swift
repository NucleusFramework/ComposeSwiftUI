// HealthKitUI, ContactsUI, FamilyControls, FinanceKitUI, ImagePlayground, JournalingSuggestions, GameSave,
// ClassKitUI, SecurityUI, AutomatedDeviceEnrollment, CoreLocation and CoreMotion for Compose SwiftUI (runtime
// AppFrameworks.kt). Each framework registers on its own (`ComposeIntegrations.registerHealthKitUI()`…): remove the
// sections of frameworks the app doesn't link.
import SwiftUI

#if os(iOS) || os(watchOS)
import HealthKit
import HealthKitUI

public extension ComposeIntegrations {
    @MainActor static func registerHealthKitUI() {
        guard #available(iOS 17.0, watchOS 10.2, *) else { return }
        ComposeRegistry.modifiers[200_347] = { v, a, _ in
            let report = a.callback(4, box: { (s: String?) -> Any? in s ?? NSNull() })
            let done: @Sendable (Result<Bool, any Error>) -> Void = { r in
                Task { @MainActor in
                    if case .failure(let e) = r { report(e.localizedDescription) } else { report(nil) }
                }
            }
            let trigger = a.hashableOpt(2)
            #if os(iOS)
            if let id = a.stringOpt(3), let type = HealthTypes.type(id) {
                return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, objectType: type, trigger: trigger, completion: done))
            }
            #endif
            let read = Set(a.list(0).compactMap { ($0 as? String).flatMap(HealthTypes.type) })
            let share = Set(a.list(1).compactMap { ($0 as? String).flatMap(HealthTypes.type) as? HKSampleType })
            if share.isEmpty {
                return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, readTypes: read, trigger: trigger, completion: done))
            }
            return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, shareTypes: share, readTypes: read, trigger: trigger, completion: done))
        }
    }
}

enum HealthTypes {
    static let store = HKHealthStore()

    /// The HealthKit type of an identifier (quantity, category, characteristic, correlation or workout).
    static func type(_ id: String) -> HKObjectType? {
        if id == HKWorkoutTypeIdentifier { return HKObjectType.workoutType() }
        return HKObjectType.quantityType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.categoryType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.characteristicType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.correlationType(forIdentifier: .init(rawValue: id))
    }
}
#endif

#if os(iOS)
import ContactsUI
import FamilyControls
import ManagedSettings
import FinanceKit
import FinanceKitUI
import AutomatedDeviceEnrollment

public extension ComposeIntegrations {
    @MainActor static func registerContactsUI() {
        guard #available(iOS 18.0, *) else { return }
        ComposeRegistry.views[200_344] = { m in
            let a = m.args
            let report = a.callback(3, box: { (ids: [String]) -> Any? in ids as NSArray })
            let emails = a.isNil(1) ? nil : Set(a.list(1).compactMap { $0 as? String })
            let phones = a.isNil(2) ? nil : Set(a.list(2).compactMap { $0 as? String })
            return ModChain.apply(ContactAccessButton(queryString: a.string(0), ignoredEmails: emails, ignoredPhoneNumbers: phones,
                                                      approvalCallback: { report($0) }), m, 0)
        }
        ComposeRegistry.modifiers[200_348] = { view, a, _ in
            var v = view
            if let i = a.intOpt(0) {
                let captions: [ContactAccessButton.Caption] = [.defaultText, .email, .phone]
                v = AnyView(v.contactAccessButtonCaption(captions[min(max(i, 0), 2)]))
            }
            if !a.isNil(1) || !a.isNil(2) || !a.isNil(3) {
                v = AnyView(v.contactAccessButtonStyle(ContactAccessButton.Style(imageTrailingEdgePadding: a.cgFloatOpt(1), imageWidth: a.cgFloatOpt(2),
                                                                                  imageColor: d_Color(a.expr(3)))))
            }
            return v
        }
        ComposeRegistry.modifiers[200_349] = { v, a, _ in
            let report = a.callback(2, box: { (ids: [String]) -> Any? in ids as NSArray })
            return AnyView(v.contactAccessPicker(isPresented: a.boolBinding(0)) { report($0) })
        }
    }

    @MainActor static func registerFamilyControls() {
        guard #available(iOS 16.0, *) else { return }
        ComposeRegistry.views[200_345] = { m in
            let a = m.args
            return ModChain.apply(FamilyActivityPicker(headerText: a.stringOpt(2), footerText: a.stringOpt(3), selection: FamilyJSON.binding(a, 0)), m, 0)
        }
        ComposeRegistry.modifiers[200_350] = { v, a, _ in
            if #available(iOS 26.2, *), !a.isNil(4) {
                return AnyView(v.familyActivityPicker(title: a.stringOpt(4), headerText: a.stringOpt(5), footerText: a.stringOpt(6),
                                                      isPresented: a.boolBinding(0), selection: FamilyJSON.binding(a, 2)))
            }
            return AnyView(v.familyActivityPicker(headerText: a.stringOpt(5), footerText: a.stringOpt(6), isPresented: a.boolBinding(0),
                                                  selection: FamilyJSON.binding(a, 2)))
        }
        ComposeRegistry.views[200_346] = { m in
            let a = m.args
            let data = Data(a.string(0).utf8)
            let label: AnyView
            switch a.int(1) {
            case 0: label = (try? JSONDecoder().decode(ApplicationToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            case 1: label = (try? JSONDecoder().decode(ActivityCategoryToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            default: label = (try? JSONDecoder().decode(WebDomainToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            }
            return ModChain.apply(label, m, 0)
        }
    }

    @MainActor static func registerFinanceKitUI() {
        guard #available(iOS 17.4, *) else { return }
        ComposeRegistry.views[200_347] = { m in
            let a = m.args
            let report = a.callback(1, box: { (s: String) -> Any? in s })
            return ModChain.apply(AddOrderToWalletButton(signedArchive: Data(base64Encoded: a.string(0)) ?? Data()) { result in
                switch result {
                case .success(let r): report("\(r)")
                case .failure(let e): report(e.localizedDescription)
                }
            }, m, 0)
        }
        ComposeRegistry.modifiers[200_351] = { v, a, _ in
            AnyView(v.addOrderToWalletButtonStyle(a.int(0) == 1 ? .blackOutline : .black))
        }
        if #available(iOS 18.0, *) {
            ComposeRegistry.views[200_348] = { m in ModChain.apply(KotlinTransactionPicker(m: m), m, 0) }
            ComposeRegistry.modifiers[200_352] = { v, a, _ in AnyView(v.modifier(TransactionPickerSheet(a: a))) }
        }
    }

    @MainActor static func registerAutomatedDeviceEnrollment() {
        guard #available(iOS 18.0, *) else { return }
        ComposeRegistry.modifiers[200_359] = { v, a, _ in AnyView(v.automatedDeviceEnrollmentAddition(isPresented: a.boolBinding(0))) }
    }
}

/// A FamilyActivitySelection as its JSON encoding, which Kotlin keeps opaque.
@available(iOS 16.0, *)
@MainActor enum FamilyJSON {
    static func binding(_ a: Args, _ i: Int) -> Binding<FamilyActivitySelection> {
        let report = a.callback(i + 1, box: { (s: String) -> Any? in s })
        return Binding(get: {
            a.stringOpt(i).flatMap { try? JSONDecoder().decode(FamilyActivitySelection.self, from: Data($0.utf8)) } ?? FamilyActivitySelection()
        }, set: { selection in
            if let data = try? JSONEncoder().encode(selection) { report(String(decoding: data, as: UTF8.self)) }
        })
    }
}

@available(iOS 18.0, *)
enum TransactionEncoding {
    static func encode(_ list: [FinanceKit.Transaction]) -> NSArray {
        list.map { t -> NSArray in
            [t.id.uuidString, t.transactionDescription, (t.merchantName as Any?) ?? NSNull(),
             KotlinDouble(value: NSDecimalNumber(decimal: t.transactionAmount.amount).doubleValue), t.transactionAmount.currencyCode,
             KotlinDouble(value: t.transactionDate.timeIntervalSince1970), KotlinBoolean(value: t.creditDebitIndicator == .credit)] as NSArray
        } as NSArray
    }
}

@available(iOS 18.0, *)
private struct KotlinTransactionPicker: View {
    let m: NodeModel
    @State private var selection: [FinanceKit.Transaction] = []

    var body: some View {
        let a = m.args
        TransactionPicker(selection: $selection) { SlotView(model: a.slotModel(1)) }
            .onChange(of: selection.map(\.id)) { _, _ in a.callback(0, box: { (x: NSArray) -> Any? in x })(TransactionEncoding.encode(selection)) }
    }
}

@available(iOS 18.0, *)
private struct TransactionPickerSheet: ViewModifier {
    let a: Args
    @State private var selection: [FinanceKit.Transaction] = []

    func body(content: Content) -> some View {
        content.transactionPicker(isPresented: a.boolBinding(0), selection: $selection)
            .onChange(of: selection.map(\.id)) { _, _ in a.callback(2, box: { (x: NSArray) -> Any? in x })(TransactionEncoding.encode(selection)) }
    }
}
#endif

#if os(iOS) || os(macOS)
import ImagePlayground

public extension ComposeIntegrations {
    @MainActor static func registerImagePlayground() {
        guard #available(iOS 18.1, macOS 15.1, *) else { return }
        ComposeRegistry.modifiers[200_353] = { v, a, _ in PlaygroundModifiers.sheet(v, a) }
        if #available(iOS 18.4, macOS 15.4, *) {
            ComposeRegistry.modifiers[200_354] = { v, a, _ in PlaygroundModifiers.style(v, a) }
        }
    }
}

@available(iOS 18.1, macOS 15.1, *)
@MainActor private enum PlaygroundModifiers {
    static func sheet(_ v: AnyView, _ a: Args) -> AnyView {
        let concepts = a.list(2).compactMap { $0 as? [Any] }.map { c -> ImagePlaygroundConcept in
            let text = c.count > 1 ? c[1] as? String ?? "" : ""
            return (c.first as? NSNumber)?.intValue == 1 ? .extracted(from: text, title: c.count > 2 ? c[2] as? String : nil) : .text(text)
        }
        let done = a.callback(5, box: { (s: String) -> Any? in s })
        let cancel = a.action(6)
        let complete: (URL) -> Void = { done($0.absoluteString) }
        let presented = a.boolBinding(0)
        if !a.isNil(7), #available(iOS 18.4, macOS 15.4, *) {
            let glyph = a.callback(7, box: { (s: String) -> Any? in s })
            let onGlyph: (NSAdaptiveImageGlyph) -> Void = { g in
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(g.contentIdentifier).appendingPathExtension("heic")
                try? g.imageContent.write(to: url)
                glyph(url.absoluteString)
            }
            if let s = a.stringOpt(4), let url = URL(string: s) {
                return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImageURL: url, onCompletion: complete,
                                                      onAdaptiveImageGlyphCreation: onGlyph, onCancellation: cancel))
            }
            return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImage: a.stringOpt(3).map { Image($0) },
                                                  onCompletion: complete, onAdaptiveImageGlyphCreation: onGlyph, onCancellation: cancel))
        }
        if let s = a.stringOpt(4), let url = URL(string: s) {
            return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImageURL: url, onCompletion: complete, onCancellation: cancel))
        }
        return AnyView(v.imagePlaygroundSheet(isPresented: presented, concepts: concepts, sourceImage: a.stringOpt(3).map { Image($0) },
                                              onCompletion: complete, onCancellation: cancel))
    }

    @available(iOS 18.4, macOS 15.4, *)
    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        func style(_ i: Int) -> ImagePlaygroundStyle? {
            switch i {
            case 0: return .animation
            case 1: return .illustration
            case 2: return .sketch
            case 3: return .emoji
            case 4: if #available(iOS 26.0, macOS 26.0, *) { return .externalProvider } else { return nil }
            default: if #available(iOS 27.0, macOS 27.0, *) { return .any } else { return nil }
            }
        }
        if let i = a.intOpt(0), let s = style(i) {
            let allowed = a.isNil(1) ? ImagePlaygroundStyle.all : a.list(1).compactMap { ($0 as? NSNumber).flatMap { style($0.intValue) } }
            v = AnyView(v.imagePlaygroundGenerationStyle(s, in: allowed))
        }
        if let i = a.intOpt(2) {
            v = AnyView(v.imagePlaygroundPersonalizationPolicy(ImagePlaygroundPersonalizationPolicy(rawValue: i) ?? .automatic))
        }
        if #available(iOS 26.4, macOS 26.4, *), !a.isNil(3) {
            let o = a.list(3)
            func int(_ k: Int) -> Int? { k < o.count ? (o[k] as? NSNumber)?.intValue : nil }
            var options = ImagePlaygroundOptions()
            if let p = int(2) { options.personalization = .init(rawValue: p) ?? .automatic }
            if #available(iOS 27.0, macOS 27.0, *) {
                if let s = int(0) { options.creationStrategy = .init(rawValue: s) ?? .automatic }
                if let s = int(1) { options.creationVariety = .init(rawValue: s) ?? .automatic }
                if o.count > 3, let size = d_CGSize(o[3] as? SExpr) { options.sizeSpecification = .closest(to: size) }
            }
            v = AnyView(v.imagePlaygroundOptions(options))
        }
        return v
    }
}

import GameSave
import ClassKitUI

public extension ComposeIntegrations {
    @MainActor static func registerGameSave() {
        guard #available(iOS 27.0, macOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_356] = { v, a, _ in AnyView(v.modifier(GameSaveAlert(a: a))) }
    }

    @MainActor static func registerClassKitUI() {
        guard #available(iOS 27.0, macOS 27.0, *) else { return }
        func url(_ s: String) -> URL { s.contains("://") ? URL(string: s) ?? URL(fileURLWithPath: s) : URL(fileURLWithPath: s) }
        ComposeRegistry.views[200_350] = { m in
            ModChain.apply(AssignedDocumentLabel(role: m.args.int(0) == 1 ? .date : .status, documentURL: url(m.args.string(1))), m, 0)
        }
        ComposeRegistry.views[200_351] = { m in ModChain.apply(AssignedDocumentSubmissionButton(documentURL: url(m.args.string(0))), m, 0) }
        ComposeRegistry.modifiers[200_357] = { view, a, _ in
            var v = view
            func ask(_ i: Int) -> @Sendable (URL) async -> Bool {
                let f = UncheckedBox(a.raw(i))
                return { u in await MainActor.run { (kotlinInvoke(f.value, [u.absoluteString]) as? NSNumber)?.boolValue ?? true } }
            }
            if !a.isNil(0) { v = AnyView(v.onAssignedDocumentWillSubmit(ask(0))) }
            if !a.isNil(1) { let r = a.callback(1, box: { (s: String) -> Any? in s }); v = AnyView(v.onAssignedDocumentDidSubmit { r($0.absoluteString) }) }
            if !a.isNil(2) { v = AnyView(v.onAssignedDocumentWillWithdraw(ask(2))) }
            if !a.isNil(3) { let r = a.callback(3, box: { (s: String) -> Any? in s }); v = AnyView(v.onAssignedDocumentDidWithdraw { r($0.absoluteString) }) }
            return v
        }
    }
}

/// Carries a Kotlin object into a `@Sendable` closure that only touches it on the main actor.
struct UncheckedBox: @unchecked Sendable {
    let value: Any?
    init(_ value: Any?) { self.value = value }
}

@available(iOS 27.0, macOS 27.0, *)
private struct GameSaveAlert: ViewModifier {
    let a: Args
    @State private var directory: GameSaveSyncedDirectory?

    func body(content: Content) -> some View {
        let report = a.callback(2, box: { (s: String) -> Any? in s })
        content
            .gameSaveSyncingAlert(directory: $directory) {
                switch directory?.state {
                case .ready(let url), .offline(let url), .local(let url): report(url.absoluteString)
                default: break
                }
            }
            .onChange(of: a.bool(0), initial: true) { _, open in
                directory = open ? GameSaveSyncedDirectory.openDirectory(containerIdentifier: a.stringOpt(1)) : nil
            }
    }
}
#endif

#if canImport(SecurityUI)
import SecurityUI

public extension ComposeIntegrations {
    @MainActor static func registerSecurityUI() {
        guard #available(iOS 27.0, macOS 27.0, tvOS 27.0, watchOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_358] = { v, a, _ in AnyView(v.modifier(CertificateSheet(a: a))) }
    }
}
#endif

#if canImport(SecurityUI)
/// Keeps the SecTrust built from the Kotlin certificates, so the sheet sees one value until they change.
@available(iOS 27.0, macOS 27.0, tvOS 27.0, watchOS 27.0, *)
private struct CertificateSheet: ViewModifier {
    let a: Args
    @State private var trust: SecTrust?

    func body(content: Content) -> some View {
        let certs: [String]? = a.isNil(0) ? nil : a.list(0).compactMap { $0 as? String }
        let report = a.callback(1, box: { (_: Bool) -> Any? in NSNull() })
        content
            .certificateSheet(trust: Binding(get: { trust }, set: { t in
                trust = t
                if t == nil { report(false) }
            }), title: a.stringOpt(2), message: a.stringOpt(3), help: a.stringOpt(4).flatMap(URL.init(string:)))
            .onChange(of: certs, initial: true) { _, list in trust = list.flatMap(Self.makeTrust) }
    }

    private static func makeTrust(_ base64: [String]) -> SecTrust? {
        let certs = base64.compactMap { Data(base64Encoded: $0) }.compactMap { SecCertificateCreateWithData(nil, $0 as CFData) }
        var t: SecTrust?
        SecTrustCreateWithCertificates(certs as CFArray, SecPolicyCreateBasicX509(), &t)
        return t
    }
}
#endif

#if canImport(JournalingSuggestions)
import JournalingSuggestions

public extension ComposeIntegrations {
    @MainActor static func registerJournalingSuggestions() {
        guard #available(iOS 17.2, *) else { return }
        ComposeRegistry.views[200_349] = { m in
            let a = m.args
            let done = Journaling.report(a, 1)
            if let label = a.slotModel(2) {
                return ModChain.apply(JournalingSuggestionsPicker { SlotView(model: label) } onCompletion: { await done($0) }, m, 0)
            }
            return ModChain.apply(JournalingSuggestionsPicker(LocalizedStringKey(a.string(0))) { await done($0) }, m, 0)
        }
        ComposeRegistry.modifiers[200_355] = { v, a, _ in
            let done = Journaling.report(a, 2)
            return AnyView(v.journalingSuggestionsPicker(isPresented: a.boolBinding(0)) { await done($0) })
        }
    }
}

@available(iOS 17.2, *)
@MainActor private enum Journaling {
    static func report(_ a: Args, _ i: Int) -> (JournalingSuggestion) async -> Void {
        let cb = a.callback(i, box: { (x: NSArray) -> Any? in x })
        return { s in
            let start: Any = s.date.map { KotlinDouble(value: $0.start.timeIntervalSince1970) } ?? NSNull()
            let end: Any = s.date.map { KotlinDouble(value: $0.end.timeIntervalSince1970) } ?? NSNull()
            await MainActor.run { cb([s.title, start, end, KotlinInt(value: Int32(s.items.count))] as NSArray) }
        }
    }
}
#endif

#if canImport(_CoreLocation_SwiftUI) && canImport(_CoreMotion_SwiftUI)
import CoreLocation
import CoreMotion

public extension ComposeIntegrations {
    @MainActor static func registerSpatialAnchors() {
        guard #available(iOS 27.2, *) else { return }
        ComposeRegistry.modifiers[200_360] = { view, a, _ in
            var v = view
            if let on = a.boolOpt(0) { v = AnyView(v.locationHeadingAnchor(manager: Anchors.location, isEnabled: on)) }
            if let on = a.boolOpt(1) { v = AnyView(v.deviceMotionAnchor(manager: Anchors.motion, isEnabled: on)) }
            return v
        }
    }
}

@MainActor private enum Anchors {
    static let location = CLLocationManager()
    static let motion = CMMotionManager()
}
#endif
