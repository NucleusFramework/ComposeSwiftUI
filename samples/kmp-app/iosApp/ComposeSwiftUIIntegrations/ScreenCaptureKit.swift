// ScreenCaptureKit for Compose SwiftUI: recordingEditor (macOS 27, iOS / tvOS 27 devices) (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerScreenCaptureKit()` at launch.
import SwiftUI

#if canImport(_ScreenCaptureKit_SwiftUI)
import ScreenCaptureKit

public extension ComposeIntegrations {
    @MainActor static func registerScreenCaptureKit() {
        guard #available(iOS 27.0, macOS 27.0, tvOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_323] = { v, a, _ in
            let set = a.callback(1, box: { (s: String?) -> Any? in s ?? NSNull() })
            let item = Binding(get: { a.stringOpt(0).flatMap(URL.init(string:)) }, set: { set($0?.absoluteString) })
            #if os(tvOS)
            if let mode = a.boolOpt(2) { return AnyView(v.recordingEditor(item, mode: mode ? .share : .preview)) }
            #endif
            return AnyView(v.recordingEditor(item))
        }
    }
}
#endif
