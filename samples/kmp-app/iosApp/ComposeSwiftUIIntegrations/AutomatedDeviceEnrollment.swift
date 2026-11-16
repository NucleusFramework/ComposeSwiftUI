// AutomatedDeviceEnrollment for Compose SwiftUI: automatedDeviceEnrollmentAddition (runtime AppFrameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerAutomatedDeviceEnrollment()` at launch.
import SwiftUI

#if os(iOS)
import AutomatedDeviceEnrollment

public extension ComposeIntegrations {
    @MainActor static func registerAutomatedDeviceEnrollment() {
        guard #available(iOS 18.0, *) else { return }
        ComposeRegistry.modifiers[200_359] = { v, a, _ in AnyView(v.automatedDeviceEnrollmentAddition(isPresented: a.boolBinding(0))) }
    }
}

#endif
