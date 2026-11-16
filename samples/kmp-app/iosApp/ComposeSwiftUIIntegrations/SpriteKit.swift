// SpriteKit, SceneKit and GameController for Compose SwiftUI: SpriteView, SceneView and handlesGameControllerEvents
// (runtime Frameworks.kt). Add this file to the app target and call `ComposeIntegrations.registerSpriteKit()`.
import SwiftUI
import SpriteKit
import SceneKit
#if os(iOS) || os(visionOS)
import GameController
#endif

public extension ComposeIntegrations {
    @MainActor static func registerSpriteKit() {
        ComposeRegistry.views[200_306] = { m in ModChain.apply(KotlinSpriteView(m: m), m, 0) }
        ComposeRegistry.views[200_307] = { m in ModChain.apply(KotlinSceneView(m: m), m, 0) }
        #if os(iOS) || os(visionOS)
        if #available(iOS 26.0, *) {
            ComposeRegistry.modifiers[200_313] = { v, a, _ in
                if let receives = a.boolOpt(1) {
                    return AnyView(v.handlesGameControllerEvents(matching: .gamepad, withOptions: .receivesEventsInView(receives)))
                }
                return AnyView(v.handlesGameControllerEvents(matching: .gamepad))
            }
        }
        #endif
    }
}

private struct KotlinSpriteView: View {
    let m: NodeModel
    @State private var scene: SKScene?

    var body: some View {
        let _ = m.version
        let a = m.args
        GeometryReader { geo in
            if let scene {
                #if os(watchOS)
                SpriteView(scene: scene, isPaused: a.bool(2), preferredFramesPerSecond: a.int(3))
                #else
                SpriteView(scene: scene, isPaused: a.bool(2), preferredFramesPerSecond: a.int(3), options: options(a), debugOptions: debug(a))
                #endif
            } else {
                Color.clear.onAppear { scene = makeScene(a, size: geo.size) }
            }
        }
    }

    private func makeScene(_ a: Args, size: CGSize) -> SKScene {
        if let file = a.stringOpt(0), let s = SKScene(fileNamed: file) { return s }
        if let name = a.stringOpt(1), let type = NSClassFromString(name) as? SKScene.Type {
            let s = type.init(size: size)
            s.scaleMode = .resizeFill
            return s
        }
        return SKScene(size: size)
    }

    #if !os(watchOS)
    private func options(_ a: Args) -> SpriteView.Options {
        var o: SpriteView.Options = []
        for i in a.list(4).compactMap({ ($0 as? NSNumber)?.intValue }) {
            switch i {
            case 0: o.insert(.ignoresSiblingOrder)
            case 1: o.insert(.shouldCullNonVisibleNodes)
            default: o.insert(.allowsTransparency)
            }
        }
        return o
    }

    private func debug(_ a: Args) -> SpriteView.DebugOptions {
        var o: SpriteView.DebugOptions = []
        for i in a.list(5).compactMap({ ($0 as? NSNumber)?.intValue }) {
            switch i {
            case 0: o.insert(.showsFPS)
            case 1: o.insert(.showsDrawCount)
            case 2: o.insert(.showsNodeCount)
            case 3: o.insert(.showsQuadCount)
            case 4: o.insert(.showsPhysics)
            default: o.insert(.showsFields)
            }
        }
        return o
    }
    #endif
}

private struct KotlinSceneView: View {
    let m: NodeModel
    @State private var scene: SCNScene?
    @State private var loaded: String?

    var body: some View {
        let _ = m.version
        let a = m.args
        var options: SceneView.Options = []
        for i in a.list(2).compactMap({ ($0 as? NSNumber)?.intValue }) {
            switch i {
            case 0: options.insert(.allowsCameraControl)
            case 1: options.insert(.autoenablesDefaultLighting)
            case 2: options.insert(.jitteringEnabled)
            case 3: options.insert(.rendersContinuously)
            default: options.insert(.temporalAntialiasingEnabled)
            }
        }
        let modes: [SCNAntialiasingMode] = [.none, .multisampling2X, .multisampling4X]
        let pov = a.stringOpt(1).flatMap { scene?.rootNode.childNode(withName: $0, recursively: true) }
        return SceneView(scene: scene, pointOfView: pov, options: options, preferredFramesPerSecond: a.int(3),
                         antialiasingMode: modes[min(max(a.int(4), 0), 2)])
            .task(id: a.string(0)) {
                guard a.string(0) != loaded else { return }
                loaded = a.string(0)
                scene = SCNScene(named: a.string(0))
            }
    }
}
