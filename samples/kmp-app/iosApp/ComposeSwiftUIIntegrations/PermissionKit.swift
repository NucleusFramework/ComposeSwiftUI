// PermissionKit for Compose SwiftUI: CommunicationLimitsButton (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerPermissionKit()` at launch.
import SwiftUI

#if os(iOS) || os(macOS)
import PermissionKit

public extension ComposeIntegrations {
    @MainActor static func registerPermissionKit() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        ComposeRegistry.views[200_325] = { m in
            let a = m.args
            let handles = a.list(0).compactMap { $0 as? [Any] }.map { h -> CommunicationHandle in
                let kinds: [CommunicationHandle.Kind] = [.phoneNumber, .emailAddress, .custom]
                return CommunicationHandle(value: h.first as? String ?? "", kind: kinds[min(max((h.count > 1 ? h[1] as? NSNumber : nil)?.intValue ?? 0, 0), 2)])
            }
            let slot = a.slotModel(1)
            return ModChain.apply(CommunicationLimitsButton(question: PermissionQuestion(handles: handles)) { SlotView(model: slot) }, m, 0)
        }
    }
}
#endif
