import SwiftUI

@main
struct BenchApp: App {
    #if os(macOS)
    @NSApplicationDelegateAdaptor private var delegate: BenchAppDelegate
    #endif

    init() {
        ComposeGenerated.setUp()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}

/// Interactive browser: pick a screen, switch between the SwiftUI and Compose SwiftUI versions.
struct RootView: View {
    @State private var selection: Int? = 0
    @State private var impl: Impl = .compose
    @State private var status = ""

    var body: some View {
        NavigationSplitView {
            List(0..<ScreenFactory.count, id: \.self, selection: $selection) { i in
                Text(ScreenFactory.title(i))
            }
            .navigationTitle("Bench")
        } detail: {
            VStack(spacing: 0) {
                Picker("Implementation", selection: $impl) {
                    ForEach(Impl.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(8)
                if let selection {
                    ScreenHost(index: selection, impl: impl).id("\(selection)-\(impl.rawValue)")
                }
                if !status.isEmpty { Text(status).font(.caption).padding(4) }
            }
        }
        .task { await AutoRun.start { status = $0 } }
    }
}

#if os(macOS)
/// On macOS the command-line runs start when the app has launched, not with the main window: a window SwiftUI
/// doesn't open (no state to restore, app not activated) would otherwise keep them from starting.
final class BenchAppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        Task { @MainActor in await AutoRun.start { _ in } }
    }
}
#endif

/// Command-line runs (`-runBench`, `-runScenarios`, `-profile`); started once, from whichever entry point comes first.
@MainActor
enum AutoRun {
    private static var started = false

    static func start(status: @escaping (String) -> Void) async {
        guard !started else { return }
        started = true
        await run(status: status)
    }

    /// `-runBench [-only N] [-out PATH]`: run the identity check + benchmarks, then exit.
    private static func run(status: @escaping (String) -> Void) async {
        let args = ProcessInfo.processInfo.arguments
        if let i = args.firstIndex(of: "-profile"), i + 2 < args.count, let screen = Int(args[i + 1]) {
            // -profile <screen> <mount|update> [compose|swift]: loops forever, for sampling profilers
            let impl: Impl = args.contains("swift") ? .swiftUI : .compose
            let renderer = OffscreenRenderer(size: CGSize(width: 700, height: 900))
            while true {
                let vm = BenchRegistry.shared.createViewModel(index: Int32(screen - 1))
                _ = renderer.mount { ScreenFactory.view(screen - 1, impl, vm) }
                if args[i + 2] == "update" {
                    for _ in 0..<200 { vm.mutate(); ComposeSwiftUIHostKt.flushCompose(); renderer.relayout() }
                }
                renderer.unmount()
                try? await Task.sleep(nanoseconds: 1_000_000)
            }
        }
        if args.contains("-runScenarios") {
            // -runScenarios [-sizes 1000,10000] [-lazy] [-out PATH]
            var sizes = [1000, 10000]
            if let i = args.firstIndex(of: "-sizes"), i + 1 < args.count {
                sizes = args[i + 1].split(separator: ",").compactMap { Int($0) }
            }
            var out = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("scenarios")
            if let i = args.firstIndex(of: "-out"), i + 1 < args.count { out = URL(fileURLWithPath: args[i + 1]) }
            #if os(iOS)
            let size = CGSize(width: 402, height: 874)
            #else
            let size = CGSize(width: 700, height: 900)
            #endif
            let runner = ScenarioRunner(outDir: out, size: size)
            runner.log = { line in print(line); status(line) }
            _ = await runner.run(listSizes: sizes, lazy: args.contains("-lazy"))
            exit(0)
        }
        guard args.contains("-runBench") else { return }
        var only: [Int]?
        if let i = args.firstIndex(of: "-only"), i + 1 < args.count {
            only = args[i + 1].split(separator: ",").compactMap { Int($0) }.map { $0 - 1 }
        }
        var out = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("bench")
        if let i = args.firstIndex(of: "-out"), i + 1 < args.count { out = URL(fileURLWithPath: args[i + 1]) }
        #if os(iOS)
        let size = CGSize(width: 402, height: 874)
        #else
        let size = CGSize(width: 700, height: 900)
        #endif
        let runner = BenchRunner(outDir: out, size: size)
        runner.log = { line in print(line); status(line) }
        _ = await runner.run(only: only)
        exit(0)
    }
}

/// Owns one fresh view model per displayed screen.
struct ScreenHost: View {
    let index: Int
    let impl: Impl
    @State private var vm: BenchViewModel

    init(index: Int, impl: Impl) {
        self.index = index
        self.impl = impl
        _vm = State(initialValue: BenchRegistry.shared.createViewModel(index: Int32(index)))
    }

    var body: some View {
        ScreenFactory.view(index, impl, vm)
    }
}
