import SwiftUI

@main
struct BenchApp: App {
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
        .task { await autoRun() }
    }

    /// `-runBench [-only N] [-out PATH]`: run the identity check + benchmarks, then exit.
    private func autoRun() async {
        let args = ProcessInfo.processInfo.arguments
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
        runner.log = { line in print(line); status = line }
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
