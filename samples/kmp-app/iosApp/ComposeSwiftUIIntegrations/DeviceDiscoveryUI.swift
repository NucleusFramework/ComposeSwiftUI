// DeviceDiscoveryUI for Compose SwiftUI: DevicePicker (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerDeviceDiscoveryUI()` at launch.
import SwiftUI

#if os(tvOS)
import DeviceDiscoveryUI
import Network

public extension ComposeIntegrations {
    @MainActor static func registerDeviceDiscoveryUI() {
        guard #available(tvOS 16.0, *) else { return }
        ComposeRegistry.views[200_326] = { m in
            let a = m.args
            let report = a.callback(1, box: { (s: String) -> Any? in s })
            return ModChain.apply(DevicePicker(.applicationService(name: a.string(0)), onSelect: { report("\($0)") }) {
                SlotView(model: a.slotModel(2))
            } fallback: {
                SlotView(model: a.slotModel(3))
            }, m, 0)
        }
    }
}
#endif
