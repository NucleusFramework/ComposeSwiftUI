// A live preview driven from the IDE (interactive mode): the preview runs in its own window, its frames stream to the
// IDE over a local TCP connection, and the IDE's mouse and keyboard come back as touches, scrolls and typing.
//
// Frames (host → IDE): 1 byte type, 4 bytes big-endian length, payload. Type 1: JPEG of the window, sent when it
// changes. Type 2: a JSON message (`info`, `layout`).
// Commands (IDE → host): one JSON object per line, `t` being down / move / up (x, y in points), scroll (x, y, dx, dy),
// text (s), backspace, speed (v: animation speed, 1 = normal), inspect, patches (patches), stop.

import Network
import notify
import SwiftUI
import UIKit

@MainActor
final class InteractiveSession {
    private static var current: InteractiveSession?

    private let unit: PreviewUnit
    private let host: ComposeSwiftUIHost
    private let window: UIWindow
    private let listener: NWListener
    private var connection: NWConnection?
    private var displayLink: CADisplayLink?
    private let touches = TouchSynthesizer()
    private var lastFrame: Data?
    private var lastSent = 0.0
    private var buffer = Data()

    /// Starts the session of [unit] on [port], replacing the current one.
    static func start(_ unit: PreviewUnit, port: UInt16, scene: UIWindowScene) {
        current?.stop()
        current = InteractiveSession(unit, port: port, scene: scene)
    }

    private init?(_ unit: PreviewUnit, port: UInt16, scene: UIWindowScene) {
        self.unit = unit
        host = unit.makeHost()
        host.inspectable = true
        let controller = UIHostingController(rootView: PreviewContent(unit: unit, host: host))
        window = UIWindow(windowScene: scene)
        let screen = scene.screen.bounds.size
        let size = PreviewWindow.configure(controller, window: window, job: unit.job, screen: screen)
        window.rootViewController = controller
        window.frame = CGRect(origin: .zero, size: size)
        window.windowLevel = .normal + 1
        window.makeKeyAndVisible()

        let parameters = NWParameters.tcp
        parameters.requiredLocalEndpoint = .hostPort(host: "127.0.0.1", port: NWEndpoint.Port(rawValue: port) ?? .any)
        parameters.allowLocalEndpointReuse = true
        guard let listener = try? NWListener(using: parameters) else { return nil }
        self.listener = listener
        listener.newConnectionHandler = { [weak self] connection in
            MainActor.assumeIsolated { self?.accept(connection) }
        }
        listener.start(queue: .main)
    }

    private func accept(_ connection: NWConnection) {
        self.connection?.cancel()
        self.connection = connection
        lastFrame = nil
        connection.stateUpdateHandler = { [weak self] state in
            MainActor.assumeIsolated {
                switch state {
                case .failed, .cancelled: if self?.connection === connection { self?.stop() }
                default: break
                }
            }
        }
        connection.start(queue: .main)
        sendJSON(["t": "info", "width": window.bounds.width, "height": window.bounds.height, "touches": TouchSynthesizer.isAvailable])
        receive(on: connection)
        let link = CADisplayLink(target: DisplayLinkTarget { [weak self] in self?.tick() }, selector: #selector(DisplayLinkTarget.fire))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    private func receive(on connection: NWConnection) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, complete, error in
            MainActor.assumeIsolated {
                guard let self else { return }
                if let data { self.buffer.append(data) }
                while let newline = self.buffer.firstIndex(of: 0x0A) {
                    let line = self.buffer[self.buffer.startIndex..<newline]
                    self.buffer.removeSubrange(self.buffer.startIndex...newline)
                    if let command = try? JSONSerialization.jsonObject(with: line) as? [String: Any] { self.handle(command) }
                }
                if complete || error != nil {
                    self.stop()
                } else {
                    self.receive(on: connection)
                }
            }
        }
    }

    private func handle(_ command: [String: Any]) {
        func number(_ key: String) -> CGFloat { CGFloat((command[key] as? NSNumber)?.doubleValue ?? 0) }
        let point = CGPoint(x: number("x"), y: number("y"))
        switch command["t"] as? String {
        case "down": touches.send(.began, at: point, in: window)
        case "move": touches.send(.moved, at: point, in: window)
        case "up": touches.send(.ended, at: point, in: window)
        case "scroll": scroll(at: point, by: CGPoint(x: number("dx"), y: number("dy")))
        case "text": KeyboardSynthesizer.type(command["s"] as? String ?? "")
        case "backspace": KeyboardSynthesizer.deleteBackward()
        case "speed": SlowMotion.set(factor: Double(number("v")))
        case "inspect": sendJSON(["t": "layout", "layout": PreviewLayoutCapture.capture(host: host, window: window)])
        case "patches": Patches.apply(command["patches"])
        case "stop": stop()
        default: break
        }
        lastSent = 0
    }

    /// The scroll view under the point scrolls by the wheel delta, within its content.
    private func scroll(at point: CGPoint, by delta: CGPoint) {
        var view = window.hitTest(point, with: nil)
        while let current = view, !(current is UIScrollView) { view = current.superview }
        guard let scrollView = view as? UIScrollView else { return }
        let inset = scrollView.adjustedContentInset
        let maxX = max(-inset.left, scrollView.contentSize.width - scrollView.bounds.width + inset.right)
        let maxY = max(-inset.top, scrollView.contentSize.height - scrollView.bounds.height + inset.bottom)
        let offset = CGPoint(
            x: min(max(scrollView.contentOffset.x + delta.x, -inset.left), maxX),
            y: min(max(scrollView.contentOffset.y + delta.y, -inset.top), maxY)
        )
        scrollView.setContentOffset(offset, animated: false)
    }

    /// Sends the window when it changed, at most 60 times a second.
    private func tick() {
        guard let connection, connection.state == .ready else { return }
        let now = CACurrentMediaTime()
        guard now - lastSent >= 1.0 / 60 else { return }
        lastSent = now
        let format = UIGraphicsImageRendererFormat()
        format.scale = min(window.screen.scale, 2)
        let image = UIGraphicsImageRenderer(bounds: window.bounds, format: format).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: false)
        }
        guard let jpeg = image.jpegData(compressionQuality: 0.85), jpeg != lastFrame else { return }
        lastFrame = jpeg
        send(type: 1, jpeg)
    }

    private func sendJSON(_ object: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: object) else { return }
        send(type: 2, data)
    }

    private func send(type: UInt8, _ payload: Data) {
        var frame = Data([type])
        var length = UInt32(payload.count).bigEndian
        withUnsafeBytes(of: &length) { frame.append(contentsOf: $0) }
        frame.append(payload)
        connection?.send(content: frame, completion: .contentProcessed { _ in })
    }

    private func stop() {
        displayLink?.invalidate()
        displayLink = nil
        connection?.cancel()
        connection = nil
        listener.cancel()
        window.isHidden = true
        window.rootViewController = nil
        if Self.current === self { Self.current = nil }
    }
}

/// CADisplayLink retains its target: a small trampoline keeps the session free.
private final class DisplayLinkTarget: NSObject {
    let action: () -> Void
    init(_ action: @escaping () -> Void) { self.action = action }
    @objc func fire() { action() }
}

/// Slow motion for the animation inspector: the simulator's *Slow Animations* (UIKit and SwiftUI animations follow its
/// `UIAnimationDragCoefficient`), on for a [factor] below 1. The slowdown is the simulator's own.
@MainActor
enum SlowMotion {
    private static let name = "com.apple.UIKit.SimulatorSlowMotionAnimationState"

    static func set(factor: Double) {
        var token: Int32 = 0
        guard notify_register_check(name, &token) == NOTIFY_STATUS_OK else { return }
        notify_set_state(token, factor < 1 ? 1 : 0)
        notify_post(name)
        notify_cancel(token)
    }
}
