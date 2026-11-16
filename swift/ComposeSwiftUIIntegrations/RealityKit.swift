// RealityKit for Compose SwiftUI: RealityView with bundled models and its camera / layout modifiers (runtime
// Frameworks.kt). Add this file to the app target and call `ComposeIntegrations.registerRealityKit()` at launch.
#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import RealityKit

public extension ComposeIntegrations {
    @MainActor static func registerRealityKit() {
        guard #available(iOS 18.0, macOS 15.0, *) else { return }
        ComposeRegistry.views[200_320] = { m in ModChain.apply(KotlinRealityView(m: m), m, 0) }
        ComposeRegistry.modifiers[200_320] = { view, a, _ in
            var v = view
            #if os(iOS) || os(macOS)
            if let i = a.intOpt(0) {
                let controls: [CameraControls] = [.none, .orbit, .dolly, .pan, .tilt]
                v = AnyView(v.realityViewCameraControls(controls[min(max(i, 0), 4)]))
            }
            #endif
            if let i = a.intOpt(1), #available(iOS 26.0, macOS 26.0, *) {
                let layouts: [RealityViewLayoutOption] = [.flexible, .centered, .fixedSize]
                v = AnyView(v.realityViewLayoutBehavior(layouts[min(max(i, 0), 2)]))
            }
            return v
        }
    }
}

@available(iOS 18.0, macOS 15.0, *)
private struct KotlinRealityView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let models = m.args.list(0).compactMap { $0 as? String }
        let placeholder = m.args.slotModel(1)
        RealityView { content in
            for name in models {
                if let entity = try? await Entity(named: name) { content.add(entity) }
            }
        } placeholder: {
            if let placeholder { NodeView(model: placeholder) } else { ProgressView() }
        }
        .id(models)
    }
}
#endif
