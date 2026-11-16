// DeviceActivity for Compose SwiftUI: DeviceActivityReport (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerDeviceActivity()` at launch.
import SwiftUI

#if os(iOS)
import DeviceActivity

public extension ComposeIntegrations {
    @MainActor static func registerDeviceActivity() {
        ComposeRegistry.views[200_322] = { m in ModChain.apply(DeviceActivityReport(DeviceActivityReport.Context(m.args.string(0))), m, 0) }
    }
}
#endif
