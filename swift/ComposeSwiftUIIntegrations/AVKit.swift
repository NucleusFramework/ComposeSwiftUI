// AVKit and QuickLook for Compose SwiftUI: VideoPlayer, camera capture events, the continuity device picker and
// quickLookPreview (runtime Frameworks.kt). Add this file to the app target and call `ComposeIntegrations.registerAVKit()`.
import SwiftUI
import AVKit
import AVFoundation
#if os(iOS) || os(macOS) || os(visionOS)
import QuickLook
#endif

public extension ComposeIntegrations {
    @MainActor static func registerAVKit() {
        ComposeRegistry.views[200_302] = { m in ModChain.apply(KotlinVideoPlayer(m: m), m, 0) }
        #if os(iOS)
        if #available(iOS 26.0, *) {
            ComposeRegistry.modifiers[200_305] = { v, a, _ in AnyView(v.modifier(CameraCaptureModifier(a: a))) }
        }
        #endif
        #if os(tvOS)
        if #available(tvOS 17.0, *) {
            ComposeRegistry.modifiers[200_306] = { v, a, _ in
                let connected = a.callback(2, box: { (s: String?) -> Any? in s ?? NSNull() })
                return AnyView(v.continuityDevicePicker(isPresented: a.boolBinding(0)) { device in connected(device.map { String(describing: $0) }) })
            }
        }
        #endif
        #if os(iOS) || os(macOS) || os(visionOS)
        ComposeRegistry.modifiers[200_307] = { v, a, _ in
            let set = a.callback(1, box: { (s: String?) -> Any? in s ?? NSNull() })
            let binding = Binding<URL?>(get: { a.stringOpt(0).flatMap(URL.init(string:)) }, set: { set($0?.absoluteString) })
            let items = a.list(2).compactMap { ($0 as? String).flatMap(URL.init(string:)) }
            return items.isEmpty ? AnyView(v.quickLookPreview(binding)) : AnyView(v.quickLookPreview(binding, in: items))
        }
        #endif
    }
}

private struct KotlinVideoPlayer: View {
    let m: NodeModel
    @State private var player = AVPlayer()
    @State private var loaded: String?
    @State private var looper: Any?

    var body: some View {
        let _ = m.version
        let a = m.args
        let playing = a.bool(1)
        Group {
            if let overlay = a.slotModel(5) {
                VideoPlayer(player: player) { NodeView(model: overlay) }
            } else {
                VideoPlayer(player: player)
            }
        }
        .task(id: a.stringOpt(0)) {
            guard let s = a.stringOpt(0), s != loaded, let url = URL(string: s) else { return }
            loaded = s
            player.replaceCurrentItem(with: AVPlayerItem(url: url))
            if playing { player.play() }
        }
        .onChange(of: playing, initial: true) { _, p in if p { player.play() } else { player.pause() } }
        .onChange(of: a.bool(3), initial: true) { _, muted in player.isMuted = muted }
        .onChange(of: a.bool(4), initial: true) { _, loops in
            player.actionAtItemEnd = loops ? .none : .pause
            if loops {
                looper = NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: nil, queue: .main) { [player] note in
                    if (note.object as? AVPlayerItem) === player.currentItem { player.seek(to: .zero); player.play() }
                }
            } else if let looper {
                NotificationCenter.default.removeObserver(looper)
            }
        }
        .onReceive(player.publisher(for: \.timeControlStatus)) { status in
            guard !a.isNil(2) else { return }
            let isPlaying = status == .playing
            if isPlaying != a.bool(1) { a.callback(2, box: { (b: Bool) -> Any? in KotlinBoolean(value: b) })(isPlaying) }
        }
    }
}

#if os(iOS)
@available(iOS 26.0, *)
private struct CameraCaptureModifier: ViewModifier {
    let a: Args

    func body(content: Content) -> some View {
        let primary = a.callback(2, box: { (i: Int) -> Any? in KotlinInt(value: Int32(i)) })
        let phase = { (e: AVCaptureEvent) -> Int in e.phase == .began ? 0 : e.phase == .ended ? 1 : 2 }
        if a.isNil(3) {
            content.onCameraCaptureEvent(isEnabled: a.bool(0), defaultSoundDisabled: a.bool(1)) { event in primary(phase(event)) }
        } else {
            let secondary = a.callback(3, box: { (i: Int) -> Any? in KotlinInt(value: Int32(i)) })
            content.onCameraCaptureEvent(isEnabled: a.bool(0), defaultSoundDisabled: a.bool(1)) { event in
                primary(phase(event))
            } secondaryAction: { event in
                secondary(phase(event))
            }
        }
    }
}
#endif
