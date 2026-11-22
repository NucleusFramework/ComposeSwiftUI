// CoreLocation, CoreMotion and NearbyInteraction anchors for Compose SwiftUI: spatialAnchors (runtime
// AppFrameworks.kt; iOS 27.2 devices). Add this file to the app target and call `ComposeIntegrations.registerSpatialAnchors()`.
import SwiftUI

#if (canImport(_CoreLocation_SwiftUI) && canImport(_CoreMotion_SwiftUI) && canImport(_NearbyInteraction_SwiftUI)) && compiler(>=6.4)  // SDK 27 only: absent with Xcode 26
import CoreLocation
import CoreMotion
import NearbyInteraction

public extension ComposeIntegrations {
    @MainActor static func registerSpatialAnchors() {
        guard #available(iOS 27.2, *) else { return }
        ComposeRegistry.modifiers[200_360] = { view, a, _ in
            var v = view
            if let on = a.boolOpt(0) {
                v = AnyView(v.locationHeadingAnchor(manager: a.raw(2) as? CLLocationManager ?? Anchors.location, isEnabled: on))
            }
            if let on = a.boolOpt(1) {
                v = AnyView(v.deviceMotionAnchor(manager: a.raw(3) as? CMMotionManager ?? Anchors.motion, isEnabled: on))
            }
            if let session = a.raw(4) as? NISession {
                v = AnyView(v.nearbyInteractionOriginAnchor(session: session, isEnabled: a.bool(5)))
            }
            return v
        }
    }
}

@MainActor private enum Anchors {
    static let location = CLLocationManager()
    static let motion = CMMotionManager()
}
#endif
