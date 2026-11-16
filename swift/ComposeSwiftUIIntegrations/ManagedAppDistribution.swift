// ManagedAppDistribution for Compose SwiftUI: ManagedContentView and its style (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerManagedAppDistribution()` at launch.
import SwiftUI

#if os(macOS)
import ManagedAppDistribution

public extension ComposeIntegrations {
    @MainActor static func registerManagedAppDistribution() {
        guard #available(macOS 26.4, *) else { return }
        ComposeRegistry.views[200_324] = { m in
            let a = m.args
            let labels = a.list(0).map { $0 as? String ?? "" }
            let states: [ManagedContentOfferState] = [.notInstalled, .neverInstalled, .installing(progress: nil), .installed, .noninteractive]
            let state = states[min(max(a.int(1), 0), 4)]
            let report = a.callback(2, box: { (i: Int) -> Any? in KotlinInt(value: Int32(i)) })
            let index = { (s: ManagedContentOfferState) -> Int in
                if s == .notInstalled { return 0 }
                if s == .neverInstalled { return 1 }
                if s == .installed { return 3 }
                if s == .noninteractive { return 4 }
                return 2
            }
            func label(_ i: Int) -> String { i < labels.count ? labels[i] : "" }
            return ModChain.apply(ManagedContentView(primaryLabel: label(0), secondaryLabel: label(1), tertiaryLabel: label(2), quaternaryLabel: label(3),
                                                     offerState: state, offerAction: { report(index($0)) }) { SlotView(model: a.slotModel(3)) }, m, 0)
        }
        ComposeRegistry.modifiers[200_322] = { v, a, _ in
            let styles: [ManagedContentStyle] = [.automatic, .compact, .header, .vertical]
            return AnyView(v.managedContentStyle(styles[min(max(a.int(0), 0), 3)]))
        }
    }
}
#endif
