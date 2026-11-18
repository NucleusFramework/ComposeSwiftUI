// A live preview driven from the IDE (interactive mode): the preview runs in its own window, its frames stream to the
// IDE over a local TCP connection, and the IDE's mouse and keyboard come back as touches (clicks on the Mac), scrolls
// and typing.
//
// Frames (host → IDE): 1 byte type, 4 bytes big-endian length, payload. Type 1: JPEG of the window, sent when it
// changes. Type 2: a JSON message (`info`, `layout`).
// Commands (IDE → host): one JSON object per line, `t` being down / move / up (x, y in points), scroll (x, y, dx, dy),
// text (s), backspace, speed (v: animation speed, 1 = normal), inspect, patches (patches), stop.

import Foundation
import Network
import QuartzCore

@MainActor
final class InteractiveSession {
    private static var current: InteractiveSession?

    private let unit: PreviewUnit
    private let host: ComposeSwiftUIHost
    private let surface: PreviewSurface
    private let listener: NWListener
    private var connection: NWConnection?
    private var timer: Timer?
    private var lastFrame: Data?
    private var lastSent = 0.0
    private var buffer = Data()

    /// Starts the session of [unit] on [port], replacing the current one.
    static func start(_ unit: PreviewUnit, port: UInt16) {
        current?.stop()
        current = InteractiveSession(unit, port: port)
    }

    private init?(_ unit: PreviewUnit, port: UInt16) {
        self.unit = unit
        host = unit.makeHost()
        host.inspectable = true
        surface = PreviewHostPlatform.makeSurface(unit: unit, host: host, interactive: true)

        let parameters = NWParameters.tcp
        parameters.requiredLocalEndpoint = .hostPort(host: "127.0.0.1", port: NWEndpoint.Port(rawValue: port) ?? .any)
        parameters.allowLocalEndpointReuse = true
        guard let listener = try? NWListener(using: parameters) else {
            surface.close()
            return nil
        }
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
        let bounds = surface.bounds
        sendJSON(["t": "info", "width": bounds.width, "height": bounds.height, "touches": surface.pointerAvailable])
        receive(on: connection)
        timer?.invalidate()
        let timer = Timer(timeInterval: 1.0 / 60, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.tick() }
        }
        RunLoop.main.add(timer, forMode: .common)
        self.timer = timer
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
        case "down": surface.pointer(.began, at: point)
        case "move": surface.pointer(.moved, at: point)
        case "up": surface.pointer(.ended, at: point)
        case "scroll": surface.scroll(at: point, by: CGPoint(x: number("dx"), y: number("dy")))
        case "text": surface.type(command["s"] as? String ?? "")
        case "backspace": surface.deleteBackward()
        case "speed": PreviewHostPlatform.setAnimationSpeed(Double(number("v")))
        case "inspect": sendJSON(["t": "layout", "layout": PreviewLayoutCapture.capture(host: host, bounds: surface.bounds)])
        case "patches": Patches.apply(command["patches"])
        case "stop": stop()
        default: break
        }
        lastSent = 0
    }

    /// Sends the window when it changed, at most 60 times a second.
    private func tick() {
        guard let connection, connection.state == .ready else { return }
        let now = CACurrentMediaTime()
        guard now - lastSent >= 1.0 / 60 else { return }
        lastSent = now
        guard let image = surface.snapshot(scale: min(surface.scale, 2), afterScreenUpdates: false),
              let jpeg = ImageEncoding.jpeg(image, quality: 0.85), jpeg != lastFrame else { return }
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
        timer?.invalidate()
        timer = nil
        connection?.cancel()
        connection = nil
        listener.cancel()
        surface.close()
        if Self.current === self { Self.current = nil }
    }
}
