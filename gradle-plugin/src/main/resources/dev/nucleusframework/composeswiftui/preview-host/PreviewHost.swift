// Host app run by the `renderComposeSwiftUIPreviews` Gradle task on the iOS simulator. It stays running between
// renders: it watches a request file (written by the task, or by the IDE for live literal edits), renders a batch of
// previews at once, each in its own window, and writes `<output>/<key>.png` as soon as a preview stops changing, with
// `<key>.layout.json` (the frame and source of every view, for the IDE's click-to-code). The task relaunches it when
// the app changes. Interactive requests run a live preview streamed to the IDE (PreviewInteractive.swift).
// These files never change with the previews (they are looked up by name), so the host is only compiled once.
//
// Protocol: the app writes `{"pid": …}` to the state file once started. For each request (a new `seq`), its stdout
// and stderr go to the request's `response` file: `CSUI-BEGIN <key>` before a preview is composed, `CSUI-DONE <json>`
// once its image is written, `CSUI-ERROR <key> <message>` (newlines as `\n`) for a preview that can't be rendered, and `CSUI-END` at the
// end. A Kotlin exception ends the app: its message is the end of the response.

import SwiftUI
import UIKit

@main
struct ComposeSwiftUIPreviewHost: App {
    init() {
        ComposeInspection.enabled = true
    }

    var body: some Scene {
        WindowGroup { HostRoot() }
    }
}

struct PreviewJob: Decodable {
    let key: String
    let id: String
    let title: String
    let parameterized: Bool
    /// Only this value of the preview parameter, or -1 for all of them.
    let index: Int
    let width: Int
    let height: Int
    let sizeThatFits: Bool
    let colorScheme: String
    let dynamicTypeSize: String
    let locale: String
    let layoutDirection: String
    let backgroundColor: Int64
}

/// A literal edited in the IDE, shown without recompiling (see the Kotlin `LiteralPatch`).
struct PatchSpec: Decodable {
    let file: String
    let offset: Int
    let function: String?
    let kind: String
    let old: String
    let new: String
}

struct PreviewRequest: Decodable {
    let seq: Int64
    let response: String
    let output: String
    let jobs: [PreviewJob]
    let batchSize: Int
    let settleMilliseconds: Int
    let interactive: Bool
    /// Interactive mode in the IDE: the port the live preview streams on.
    let port: Int?
    let patches: [PatchSpec]?
}

/// One host to render: a job, and the value of its preview parameter.
struct PreviewUnit {
    let job: PreviewJob
    let index: Int
    let key: String
    let title: String

    /// Only called for units made by `Host.units`, which skips previews missing from the framework.
    func makeHost() -> ComposeSwiftUIHost {
        let host = ComposeSwiftUIPreviews.shared.host(id: job.id, index: Int32(index))!
        host.inspectable = true
        return host
    }
}

@MainActor
enum Patches {
    /// Installs the literal patches of a request (`[PatchSpec]` or the JSON of the interactive command).
    static func apply(_ specs: Any?) {
        let decoded: [PatchSpec]
        switch specs {
        case let specs as [PatchSpec]: decoded = specs
        case let json as [[String: Any]]:
            decoded = (try? JSONDecoder().decode([PatchSpec].self, from: JSONSerialization.data(withJSONObject: json))) ?? []
        default: decoded = []
        }
        ComposeSwiftUIHostKt.setLiteralPatches(patches: decoded.map {
            LiteralPatch(file: $0.file, offset: Int32($0.offset), function: $0.function, kind: $0.kind, oldText: $0.old, newText: $0.new)
        })
    }
}

private enum Host {
    static let environment = ProcessInfo.processInfo.environment
    static let requestPath = environment["COMPOSE_SWIFTUI_PREVIEW_REQUEST"] ?? ""
    static let statePath = environment["COMPOSE_SWIFTUI_PREVIEW_STATE"]

    static func readRequest() -> PreviewRequest? {
        FileManager.default.contents(atPath: requestPath).flatMap { try? JSONDecoder().decode(PreviewRequest.self, from: $0) }
    }

    static let initialRequest = readRequest()

    /// Serves requests until the app is terminated: each new `seq` is rendered, its output in its response file.
    @MainActor
    static func serve(scene: UIWindowScene) async {
        // Only the foreground app of a simulator renders: the state tells the caller to activate the host again.
        func writeState(active: Bool) {
            guard let statePath else { return }
            try? "{\"pid\": \(getpid()), \"active\": \(active)}".write(toFile: statePath, atomically: true, encoding: .utf8)
        }
        writeState(active: true)
        for (name, active) in [(UIApplication.didEnterBackgroundNotification, false), (UIApplication.didBecomeActiveNotification, true)] {
            NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { _ in writeState(active: active) }
        }
        var last: Int64 = -1
        while true {
            if let request = readRequest(), request.seq != last {
                last = request.seq
                // Kotlin's uncaught exception message goes to stderr: into the response too.
                freopen(request.response, "w", stdout)
                setvbuf(stdout, nil, _IOLBF, 0)
                dup2(fileno(stdout), STDERR_FILENO)
                Patches.apply(request.patches ?? [])
                if request.interactive, let port = request.port, let unit = units(request.jobs).first {
                    InteractiveSession.start(unit, port: UInt16(port), scene: scene)
                } else {
                    await BatchRenderer(request: request, scene: scene).run()
                }
                print("CSUI-END")
                fflush(stdout)
            }
            try? await Task.sleep(for: .milliseconds(25))
        }
    }

    /// Expands parameterized previews into one unit per value of their provider.
    static func units(_ jobs: [PreviewJob]) -> [PreviewUnit] {
        jobs.flatMap { job -> [PreviewUnit] in
            let count = Int(ComposeSwiftUIPreviews.shared.count(id: job.id))
            if count < 0 {
                let failure = ComposeSwiftUIPreviews.shared.countFailure ?? "The PreviewParameterProvider failed"
                print("CSUI-ERROR \(job.key) \(failure.replacingOccurrences(of: "\n", with: "\\n"))")
                return []
            }
            if count == 0 && !job.parameterized {
                // The framework was linked before this preview was added.
                print("CSUI-ERROR \(job.key) not in the framework yet")
                return []
            }
            guard job.parameterized else { return [PreviewUnit(job: job, index: 0, key: job.key, title: job.title)] }
            if count == 0 {
                print("CSUI-ERROR \(job.key) The PreviewParameterProvider has no values")
                return []
            }
            let indices = job.index >= 0 ? job.index..<min(job.index + 1, count) : 0..<count
            return indices.map { i in
                PreviewUnit(job: job, index: i, key: "\(job.key)#\(i)", title: "\(job.title) (\(i + 1) of \(count))")
            }
        }
    }
}

private struct HostRoot: View {
    var body: some View {
        // `Run on Simulator`: the preview, live, full screen on the simulator.
        if let request = Host.initialRequest, request.interactive, request.port == nil, let unit = Host.units(request.jobs).first {
            PreviewContent(unit: unit, host: unit.makeHost())
        } else {
            Text("Rendering previews…")
                .foregroundStyle(.secondary)
                .task {
                    guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first else { exit(1) }
                    await Host.serve(scene: scene)
                }
        }
    }
}

/// The preview with the environment of its `@Preview` arguments.
struct PreviewContent: View {
    let unit: PreviewUnit
    let host: ComposeSwiftUIHost

    var body: some View {
        let job = unit.job
        let base = ComposeView { host }
            .environment(\.locale, job.locale.isEmpty ? Locale.current : Locale(identifier: job.locale))
            .modifier(OptionalLayoutDirection(direction: job.layoutDirection))
            .modifier(OptionalDynamicTypeSize(size: job.dynamicTypeSize))
        if job.backgroundColor != 0 {
            base.background(Color(argb: job.backgroundColor))
        } else {
            base
        }
    }
}

private struct OptionalLayoutDirection: ViewModifier {
    let direction: String

    func body(content: Content) -> some View {
        switch direction {
        case "rightToLeft": content.environment(\.layoutDirection, .rightToLeft)
        case "leftToRight": content.environment(\.layoutDirection, .leftToRight)
        default: content
        }
    }
}

private struct OptionalDynamicTypeSize: ViewModifier {
    let size: String

    private static let sizes: [String: DynamicTypeSize] = [
        "xSmall": .xSmall, "small": .small, "medium": .medium, "large": .large, "xLarge": .xLarge,
        "xxLarge": .xxLarge, "xxxLarge": .xxxLarge, "accessibility1": .accessibility1,
        "accessibility2": .accessibility2, "accessibility3": .accessibility3, "accessibility4": .accessibility4,
        "accessibility5": .accessibility5,
    ]

    func body(content: Content) -> some View {
        if let size = Self.sizes[size] {
            content.dynamicTypeSize(size)
        } else {
            content
        }
    }
}

private extension Color {
    init(argb: Int64) {
        func channel(_ shift: Int64) -> Double { Double((argb >> shift) & 0xFF) / 255 }
        self.init(.sRGB, red: channel(16), green: channel(8), blue: channel(0), opacity: channel(24))
    }
}

/// The window of a preview, set up from its `@Preview` arguments: size, size classes, appearance, direction.
@MainActor
enum PreviewWindow {
    /// Configures [window] and [controller] for [job] and returns the window size.
    static func configure(_ controller: UIHostingController<PreviewContent>, window: UIWindow, job: PreviewJob, screen: CGSize) -> CGSize {
        if job.width > 0 || job.height > 0 || job.sizeThatFits {
            // A component, not a screen: no status bar or home indicator insets.
            controller.safeAreaRegions = []
            let width = CGFloat(job.width > 0 ? job.width : Int(screen.width))
            controller.traitOverrides.horizontalSizeClass = width >= 700 ? .regular : .compact
            if job.height > 0 { controller.traitOverrides.verticalSizeClass = job.height < 500 ? .compact : .regular }
        }
        switch job.colorScheme {
        case "dark": window.overrideUserInterfaceStyle = .dark
        case "light": window.overrideUserInterfaceStyle = .light
        default: break
        }
        if job.layoutDirection == "rightToLeft" { controller.traitOverrides.layoutDirection = .rightToLeft }
        controller.view.backgroundColor = .systemBackground
        return size(of: job, controller: controller, screen: screen)
    }

    /// Size of the window: the `@Preview` size, else the screen; the content's height with `sizeThatFits`.
    static func size(of job: PreviewJob, controller: UIHostingController<PreviewContent>, screen: CGSize) -> CGSize {
        let width = job.width > 0 ? CGFloat(job.width) : screen.width
        var height = job.height > 0 ? CGFloat(job.height) : screen.height
        if job.sizeThatFits {
            let fitting = controller.sizeThatFits(in: CGSize(width: width, height: job.height > 0 ? height : .greatestFiniteMagnitude))
            if fitting.height.isFinite && fitting.height > 0 { height = min(fitting.height, job.height > 0 ? height : 4 * screen.height) }
            let fittingWidth = job.width > 0 ? width : min(max(fitting.width, 1), width)
            return CGSize(width: ceil(fittingWidth), height: ceil(height))
        }
        return CGSize(width: width, height: height)
    }
}

/// The views of a preview for the IDE: each node's frame in the window with the call sites that produced it.
@MainActor
enum PreviewLayoutCapture {
    static func capture(host: ComposeSwiftUIHost, window: UIWindow) -> [String: Any] {
        let sources = (try? JSONSerialization.jsonObject(with: Data(host.inspect().utf8)) as? [[String: Any]]) ?? []
        let bounds = window.bounds
        var nodes: [[String: Any]] = []
        for source in sources {
            guard let id = (source["id"] as? NSNumber)?.int64Value, let frame = ComposeInspection.frames[id],
                  frame.width > 0, frame.height > 0, frame.intersects(bounds) else { continue }
            nodes.append([
                "x": frame.minX, "y": frame.minY, "width": frame.width, "height": frame.height,
                "calls": source["calls"] ?? [],
            ])
        }
        return ["width": bounds.width, "height": bounds.height, "nodes": nodes]
    }
}

/// Renders the request in batches: every preview of a batch is composed at once in its own window, and each one is
/// snapshotted as soon as two consecutive frames are identical (or at the settle limit, for endless animations).
@MainActor
private final class BatchRenderer {
    private let request: PreviewRequest
    private let scene: UIWindowScene

    init(request: PreviewRequest, scene: UIWindowScene) {
        self.request = request
        self.scene = scene
    }

    private final class Slot {
        let unit: PreviewUnit
        let window: UIWindow
        let controller: UIHostingController<PreviewContent>
        let host: ComposeSwiftUIHost
        let start = Date()
        var previous: Data?
        var done = false

        init(unit: PreviewUnit, window: UIWindow, controller: UIHostingController<PreviewContent>, host: ComposeSwiftUIHost) {
            self.unit = unit
            self.window = window
            self.controller = controller
            self.host = host
        }
    }

    func run() async {
        let units = Host.units(request.jobs)
        var start = 0
        while start < units.count {
            let batch = Array(units[start..<min(start + max(request.batchSize, 1), units.count)])
            await render(batch)
            start += batch.count
        }
    }

    private func render(_ batch: [PreviewUnit]) async {
        let screen = scene.screen.bounds.size
        let slots = batch.map { unit -> Slot in
            print("CSUI-BEGIN \(unit.key)")
            fflush(stdout)
            let host = unit.makeHost()
            let controller = UIHostingController(rootView: PreviewContent(unit: unit, host: host))
            let window = UIWindow(windowScene: scene)
            let size = PreviewWindow.configure(controller, window: window, job: unit.job, screen: screen)
            window.rootViewController = controller
            window.frame = CGRect(origin: .zero, size: size)
            window.isHidden = false
            return Slot(unit: unit, window: window, controller: controller, host: host)
        }

        let limit = Double(request.settleMilliseconds) / 1000
        while slots.contains(where: { !$0.done }) {
            try? await Task.sleep(for: .milliseconds(100))
            for slot in slots where !slot.done {
                let job = slot.unit.job
                if job.sizeThatFits {
                    slot.window.frame.size = PreviewWindow.size(of: job, controller: slot.controller, screen: screen)
                }
                let elapsed = Date().timeIntervalSince(slot.start)
                let probe = snapshot(slot.window, scale: 0.25).pngData()
                let stable = probe != nil && probe == slot.previous && elapsed >= 0.2
                slot.previous = probe
                if stable || elapsed >= limit { finish(slot, elapsed: elapsed) }
            }
        }
        for slot in slots {
            slot.window.isHidden = true
            slot.window.rootViewController = nil
        }
    }

    private func snapshot(_ window: UIWindow, scale: CGFloat) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        return UIGraphicsImageRenderer(bounds: window.bounds, format: format).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
    }

    private func finish(_ slot: Slot, elapsed: TimeInterval) {
        slot.done = true
        let output = URL(fileURLWithPath: request.output)
        let file = output.appendingPathComponent("\(slot.unit.key).png")
        let image = snapshot(slot.window, scale: slot.window.screen.scale)
        do {
            try image.pngData()?.write(to: file)
            let layout = PreviewLayoutCapture.capture(host: slot.host, window: slot.window)
            try JSONSerialization.data(withJSONObject: layout).write(to: output.appendingPathComponent("\(slot.unit.key).layout.json"))
        } catch {
            print("CSUI-ERROR \(slot.unit.key) \(error)")
        }
        let result: [String: Any] = [
            "key": slot.unit.key,
            "id": slot.unit.job.id,
            "title": slot.unit.title,
            "file": file.lastPathComponent,
            "width": Int(slot.window.bounds.width),
            "height": Int(slot.window.bounds.height),
            "milliseconds": Int(elapsed * 1000),
        ]
        if let data = try? JSONSerialization.data(withJSONObject: result), let json = String(data: data, encoding: .utf8) {
            print("CSUI-DONE \(json)")
            fflush(stdout)
        }
    }
}

