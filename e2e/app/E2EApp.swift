import AppKit
import ApplicationServices
import SwiftUI

/// End-to-end tests: mounts the Kotlin scenarios of e2e/shared in a real SwiftUI window, drives them through the
/// accessibility tree (as VoiceOver or XCUITest would) and checks what the user would see. Run by scripts/run_e2e.sh;
/// exits with the number of failed checks.
@main
struct E2EApp: App {
    @NSApplicationDelegateAdaptor private var delegate: E2EAppDelegate

    init() {
        ComposeGenerated.setUp()
        CapturingLogger.install()
        E2E.shared.installErrorHook()
    }

    var body: some Scene {
        WindowGroup {
            StageView()
                .frame(width: 600, height: 800)
        }
    }
}

final class E2EAppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
        Task { @MainActor in
            await E2ERunner().run()
        }
    }
}

/// What the window shows: the views the runner mounted, top to bottom.
@MainActor
final class Stage: ObservableObject {
    static let shared = Stage()
    @Published var views: [(id: String, view: AnyView)] = []
}

struct StageView: View {
    @ObservedObject private var stage = Stage.shared

    var body: some View {
        VStack(spacing: 8) {
            ForEach(stage.views, id: \.id) { $0.view }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

/// Keeps the runtime's log lines (and still writes them to os_log), so checks can read them.
final class CapturingLogger: NSObject, ComposeSwiftUILogger {
    static var lines: [String] = []
    private let next: ComposeSwiftUILogger

    init(next: ComposeSwiftUILogger) { self.next = next }

    static func install() {
        ComposeSwiftUI.shared.logger = CapturingLogger(next: ComposeSwiftUI.shared.logger)
    }

    func log(level: ComposeSwiftUILogLevel, message: String) {
        CapturingLogger.lines.append(message)
        next.log(level: level, message: message)
    }
}

// MARK: - Accessibility driver

/// Reads and presses the app's UI through the accessibility API (AXUIElement), as VoiceOver or XCUITest do.
@MainActor
enum AX {
    typealias Element = AXUIElement

    /// Without the accessibility permission (a CI runner), the checks read and drive the Kotlin node tree instead:
    /// the renderer is still exercised, the OS accessibility layer is not.
    static let trusted = AXIsProcessTrusted() && ProcessInfo.processInfo.environment["E2E_NO_AX"] == nil

    static func attribute(_ e: Element, _ name: String) -> CFTypeRef? {
        var value: CFTypeRef?
        return AXUIElementCopyAttributeValue(e, name as CFString, &value) == .success ? value : nil
    }

    static func elements() -> [Element] {
        var out: [Element] = []
        func walk(_ e: Element, depth: Int) {
            guard depth < 80 else { return }
            out.append(e)
            for child in (attribute(e, kAXChildrenAttribute) as? [Element]) ?? [] { walk(child, depth: depth + 1) }
        }
        // The windows only: the menu bar has hundreds of items
        let app = AXUIElementCreateApplication(getpid())
        for window in (attribute(app, kAXWindowsAttribute) as? [Element]) ?? [] { walk(window, depth: 0) }
        return out
    }

    static func strings(_ e: Element) -> [String] {
        [kAXTitleAttribute, kAXValueAttribute, kAXDescriptionAttribute].compactMap { attribute(e, $0) as? String }
    }

    static func role(_ e: Element) -> String { attribute(e, kAXRoleAttribute) as? String ?? "" }

    static func texts() -> [String] { trusted ? elements().flatMap(strings) : E2E.shared.nodeTexts().compactMap { $0 as? String } }

    static func find(_ text: String) -> Element? { elements().first { strings($0).contains(text) } }

    static func contains(_ text: String) -> Bool { trusted ? find(text) != nil : texts().contains(text) }

    @discardableResult
    static func press(_ text: String) -> Bool {
        if !trusted { return E2E.shared.pressNode(text: text) }
        guard let e = elements().first(where: { strings($0).contains(text) && role($0) != kAXStaticTextRole }) else {
            print("  (nothing to press named '\(text)')")
            return false
        }
        return AXUIElementPerformAction(e, kAXPressAction as CFString) == .success
    }
}

// MARK: - Runner

@MainActor
final class E2ERunner {
    private var failures = 0
    private var passes = 0
    private let only = ProcessInfo.processInfo.environment["E2E_ONLY"]
    private let phase = ProcessInfo.processInfo.environment["E2E_PHASE"] ?? "1"
    private let stateFile = ProcessInfo.processInfo.environment["E2E_STATE_FILE"] ?? "/tmp/csui_e2e_state.txt"

    func check(_ name: String, _ ok: Bool, _ detail: @autoclosure () -> String = "") {
        if ok {
            passes += 1
            print("PASS \(name)")
        } else {
            failures += 1
            print("FAIL \(name) \(detail())")
        }
    }

    func settle(_ seconds: Double = 0.4) async {
        try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
    }

    /// Waits until `condition` holds (up to `timeout`), letting SwiftUI and Kotlin run.
    func eventually(_ timeout: Double = 3, _ condition: () -> Bool) async -> Bool {
        let end = Date().addingTimeInterval(timeout)
        while Date() < end {
            if condition() { return true }
            await settle(0.05)
        }
        return condition()
    }

    func events() -> [String] { E2E.shared.events.compactMap { $0 as? String } }

    func hasEvent(_ e: String) -> Bool { events().contains(e) }

    func mount(_ id: String, _ view: some View) async {
        Stage.shared.views.append((id: id, view: AnyView(view)))
        await settle()
    }

    func unmountAll() async {
        Stage.shared.views = []
        await settle(0.6)
        E2E.shared.reset()
        CapturingLogger.lines = []
    }

    func run() async {
        print("accessibility: \(AX.trusted ? "AXUIElement" : "not permitted, driving the node tree")")
        _ = await eventually(5) { NSApp.windows.contains { $0.isVisible } }
        if ProcessInfo.processInfo.environment["E2E_DEBUG_AX"] != nil {
            await mount("callbacks", ComposeView { E2E.shared.host(name: "callbacks") })
            print(AX.texts())
            exit(0)
        }
        let scenarios: [(String, () async -> Void)] = phase == "2" ? [("saveableRestore", saveableRestore)] : [
            ("callbacks", callbacks), ("composition", composition), ("effect", effect), ("task", task),
            ("taskCancel", taskCancel), ("bridges", bridges), ("integrations", integrations), ("unsupported", unsupported), ("lifecycle", lifecycle),
            ("viewModels", viewModels), ("saveable", saveable), ("environment", environment),
            ("resources", resources),
        ]
        for (name, scenario) in scenarios where only == nil || only!.split(separator: ",").contains(Substring(name)) {
            print("== \(name)")
            await scenario()
            await unmountAll()
        }
        print("E2E: \(passes) passed, \(failures) failed")
        fflush(stdout)
        exit(Int32(min(failures, 100)))
    }

    // MARK: Error handling

    func callbacks() async {
        await mount("callbacks", ComposeView { E2E.shared.host(name: "callbacks") })
        check("button shown", AX.contains("Throw in action"), AX.texts().joined(separator: "|"))
        AX.press("Throw in action")
        check("action exception reported, app alive", await eventually { self.hasEvent("error:Callback:action boom") }, events().description)
        AX.press("Throw in binding")
        check("binding exception reported", await eventually { self.hasEvent("error:Callback:binding boom") }, events().description)
        AX.press("Increment")
        check("UI still works after callback exceptions", await eventually { AX.contains("count=1") }, AX.texts().description)
    }

    func composition() async {
        var broken: ComposeSwiftUIHost?
        await mount("broken", ComposeView { let h = E2E.shared.host(name: "composition"); broken = h; return h })
        await mount("healthy", ComposeView { E2E.shared.host(name: "callbacks") })
        AX.press("Break composition")
        check("composition exception reported", await eventually { self.hasEvent("error:Composition:composition boom") }, events().description)
        check("failing host marked failed", broken?.failure != nil)
        #if DEBUG
        if AX.trusted {
            check("debug build shows the failure", await eventually { AX.texts().contains { $0.hasPrefix("Composition failed") } }, AX.texts().description)
        }
        #else
        check("release build shows nothing", !AX.texts().contains { $0.contains("Composition failed") } && !AX.contains("composition ok"))
        #endif
        AX.press("Increment")
        check("other hosts keep working", await eventually { AX.contains("count=1") }, AX.texts().description)
    }

    func effect() async {
        await mount("effect", ComposeView { E2E.shared.host(name: "effect") })
        check("effect exception reported", await eventually { self.events().contains { $0.hasPrefix("error:Composition:effect boom") } }, events().description)
    }

    func task() async {
        await mount("task", ComposeView { E2E.shared.host(name: "task") })
        check("task exception reported", await eventually { self.hasEvent("error:AsyncTask:task boom") }, events().description)
        AX.press("Increment")
        check("UI works after a task exception", await eventually { AX.contains("count=1") }, AX.texts().description)
    }

    func taskCancel() async {
        var host: ComposeSwiftUIHost?
        await mount("taskCancel", ComposeView { let h = E2E.shared.host(name: "taskCancel"); host = h; return h })
        check("task started", await eventually { self.hasEvent("task:started") }, events().description)
        // A second run of the action that SwiftUI doesn't know about: only the host can cancel it.
        E2E.shared.startTaskOutsideSwiftUI(host: host!)
        check("detached run started", await eventually { self.events().filter { $0 == "task:started" }.count == 2 }, events().description)
        host = nil
        Stage.shared.views = []
        check("disposing the host cancels its coroutines", await eventually {
            self.events().filter { $0 == "task:cancelled" }.count == 2 && self.hasEvent("task:done")
        }, events().description)
    }

    func bridges() async {
        await mount("bridges", ComposeView { E2E.shared.host(name: "bridges") })
        check("canvas exception reported", await eventually { self.hasEvent("error:Callback:canvas boom") }, events().description)
        check("layout exception reported", await eventually { self.hasEvent("error:Callback:layout boom") }, events().description)
        check("app alive after layout/drawing exceptions", AX.contains("bridges alive"), AX.texts().description)
    }

    func integrations() async {
        // composeSwiftUI { integrations = listOf("Charts") }: registered by ComposeGenerated.setUp(), the app calls nothing
        check("listed integration registered", ComposeRegistry.views[E2E.shared.chartKind()] != nil)
        await mount("chart", ComposeView { E2E.shared.host(name: "chart") }.frame(height: 200))
        check("chart renders (no unsupported kind)", !AX.texts().contains { $0.hasPrefix("Unsupported view kind") }, AX.texts().description)
    }

    func unsupported() async {
        await mount("unsupported", ComposeView { E2E.shared.host(name: "unsupported") })
        #if DEBUG
        if AX.trusted { check("debug build shows unsupported kinds", AX.contains("Unsupported view kind 999999"), AX.texts().description) }
        #else
        check("release build hides unsupported kinds", !AX.contains("Unsupported view kind 999999"))
        check("release build logs unsupported kinds", CapturingLogger.lines.contains { $0.contains("Unsupported view kind 999999") }, CapturingLogger.lines.description)
        #endif
    }

    // MARK: Lifecycle

    func lifecycle() async {
        await mount("lifecycle", ComposeView { E2E.shared.host(name: "lifecycle") })
        check("ON_START and ON_RESUME", await eventually { self.hasEvent("lifecycle:ON_START") && self.hasEvent("lifecycle:ON_RESUME") }, events().description)
        check("LocalLifecycleOwner is RESUMED", await eventually { AX.contains("ticks=0 state=RESUMED") }, AX.texts().description)
        E2E.shared.setTicker(value: 5)
        check("collectAsStateWithLifecycle collects", await eventually { AX.contains("ticks=5 state=RESUMED") }, AX.texts().description)
        NSApp.hide(nil)
        let paused = await eventually(4) { self.hasEvent("lifecycle:ON_PAUSE") }
        check("hiding the app pauses", paused, events().description)
        NSApp.unhide(nil)
        NSApp.activate(ignoringOtherApps: true)
        check("showing it again resumes", await eventually(4) { self.events().filter { $0 == "lifecycle:ON_RESUME" }.count >= 2 }, events().description)
        Stage.shared.views = []
        check("unmounting stops and destroys", await eventually { self.hasEvent("lifecycle:ON_STOP") && self.hasEvent("lifecycle:ON_DESTROY") }, events().description)
    }

    func viewModels() async {
        await mount("vm", ComposeView(restorationID: "vm") { E2E.shared.host(name: "viewModels") })
        check("koinViewModel() resolves", await eventually { self.hasEvent("koin:koin works") }, events().description)
        check("SavedStateHandle starts empty", hasEvent("handle:note="), events().description)
        AX.press("Set note")
        check("SavedStateHandle written", await eventually { AX.contains("note=saved in handle") }, AX.texts().description)
        // Unmounting saves into @SceneStorage; the next ComposeView with the same restorationID restores it
        Stage.shared.views = []
        await settle(0.6)
        E2E.shared.reset()
        await mount("vm2", ComposeView(restorationID: "vm") { E2E.shared.host(name: "viewModels") })
        check("SavedStateHandle restored from @SceneStorage", await eventually { AX.contains("note=saved in handle") }, AX.texts().description + events().description)
    }

    func saveable() async {
        var host: ComposeSwiftUIHost?
        await mount("saveable", ComposeView(restorationID: "saveable") { let h = E2E.shared.host(name: "saveable"); host = h; return h })
        AX.press("Increment saved")
        AX.press("Increment saved")
        check("rememberSaveable counts", await eventually { AX.contains("saved=2") }, AX.texts().description)
        // Process death: the next run (E2E_PHASE=2) restores from this file
        try? host!.saveState().write(toFile: stateFile, atomically: true, encoding: .utf8)
        check("state saved", FileManager.default.fileExists(atPath: stateFile))
        Stage.shared.views = []
        await settle(0.6)
        await mount("saveable2", ComposeView(restorationID: "saveable") { E2E.shared.host(name: "saveable") })
        check("rememberSaveable restored from @SceneStorage", await eventually { AX.contains("saved=2") }, AX.texts().description)
    }

    func saveableRestore() async {
        let saved = try? String(contentsOfFile: stateFile, encoding: .utf8)
        check("saved state found", saved != nil)
        await mount("restored", ComposeView {
            let h = E2E.shared.host(name: "saveable")
            h.restoreState(state: saved)
            return h
        })
        check("rememberSaveable survives process death", await eventually { AX.contains("saved=2") }, AX.texts().description)
        check("Long keeps its precision", hasEvent("saveable:count=2 big=9007199254740993"), events().description)
    }

    // MARK: Functional defects

    func environment() async {
        NSApp.appearance = NSAppearance(named: .darkAqua)
        await mount("env", ComposeView { E2E.shared.host(name: "environment") })
        check("first composition already dark", await eventually { self.hasEvent("env:first=dark") }, events().description)
        check("EnvironmentReader starts dark", hasEvent("env:reader=dark") && !hasEvent("env:reader=light"), events().description)
        NSApp.appearance = nil
    }

    func resources() async {
        await mount("resources", ComposeView { E2E.shared.host(name: "resources") })
        check("stringResource without providing LocalDensity", await eventually { AX.contains("Hello from resources") },
              AX.texts().description + events().description)
    }
}
