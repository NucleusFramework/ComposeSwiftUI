// MusicKit and DataDetection for Compose SwiftUI: musicSubscriptionOffer, musicPicker and dataDetection (runtime
// Frameworks.kt). Add this file to the app target and call `ComposeIntegrations.registerMusicKit()` at launch.
#if os(iOS) || os(visionOS)
import SwiftUI
import MusicKit

public extension ComposeIntegrations {
    @MainActor static func registerMusicKit() {
        ComposeRegistry.modifiers[200_316] = { v, a, _ in
            let l = a.list(2)
            let messages: [MusicSubscriptionOffer.MessageIdentifier] = [.join, .addMusic, .playMusic]
            var options = MusicSubscriptionOffer.Options()
            options.messageIdentifier = messages[min(max((l.first as? NSNumber)?.intValue ?? 0, 0), 2)]
            if l.count > 1, let id = l[1] as? String { options.itemID = MusicItemID(id) }
            if l.count > 2, let t = l[2] as? String { options.affiliateToken = t }
            if l.count > 3, let t = l[3] as? String { options.campaignToken = t }
            let done = a.callback(3, box: { (s: String?) -> Any? in s ?? NSNull() })
            return AnyView(v.musicSubscriptionOffer(isPresented: a.boolBinding(0), options: options) { done($0?.localizedDescription) })
        }
        #if compiler(>=6.4)
        if #available(iOS 27.0, *) {
            ComposeRegistry.modifiers[200_317] = { v, a, _ in MusicModifiers.picker(v, a) }
        }
        #endif
    }
}

#if compiler(>=6.4)
@available(iOS 27.0, *)
@MainActor private enum MusicModifiers {
    static func picker(_ v: AnyView, _ a: Args) -> AnyView {
        let report = a.callback(4, box: { (l: [String]) -> Any? in l as NSArray })
        let title = a.stringOpt(3).map { Text($0) }
        if a.bool(2) {
            return AnyView(v.musicPicker(isPresented: a.boolBinding(0), title: title,
                                         selection: Binding<[Song]>(get: { [] }, set: { report($0.map { $0.id.rawValue }) })))
        }
        return AnyView(v.musicPicker(isPresented: a.boolBinding(0), title: title,
                                     selection: Binding<Song?>(get: { nil }, set: { report($0.map { [$0.id.rawValue] } ?? []) })))
    }
}
#endif
#endif

#if os(iOS) || os(visionOS)
import SwiftUI
import DataDetection

public extension ComposeIntegrations {
    /// DataDetection: `Modifier.dataDetection(types)`.
    @MainActor static func registerDataDetection() {
        #if compiler(>=6.4)
        guard #available(iOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_318] = { v, a, _ in
            let all: [DataDetector.MatchType] = [.all, .link, .emailAddress, .phoneNumber, .postalAddress, .calendarEvent, .moneyAmount,
                                                  .measurement, .flightNumber, .shipmentTrackingNumber, .paymentIdentifier]
            var types: DataDetector.MatchType = []
            for i in a.list(0).compactMap({ ($0 as? NSNumber)?.intValue }) where i >= 0 && i < all.count { types.formUnion(all[i]) }
            return AnyView(v.dataDetection(types.isEmpty ? .all : types))
        }
        #endif
    }
}
#endif
