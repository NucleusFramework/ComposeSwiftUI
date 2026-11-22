import ComposeSwiftUI
import SwiftUI

/// Shows two scenarios, then checks them from the Kotlin node tree and prints `IOS-E2E PASS|FAIL …` lines.
@main
struct E2EiOSApp: App {
    var body: some Scene {
        WindowGroup {
            VStack {
                ComposeView { E2E.shared.host(name: "callbacks") }
                ComposeView { E2E.shared.host(name: "chart") }.frame(height: 200)
            }
            .task { await SelfCheck.run() }
        }
    }
}

@MainActor
enum SelfCheck {
    static func run() async {
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        func texts() -> [String] { E2E.shared.nodeTexts().compactMap { $0 as? String } }
        report("renders Kotlin content from the package", texts().contains("count=0"))
        report("action through the package", E2E.shared.pressNode(text: "Increment"))
        try? await Task.sleep(nanoseconds: 300_000_000)
        report("recomposes", texts().contains("count=1"))
        report("Charts integration registered automatically", ComposeRegistry.views[E2E.shared.chartKind()] != nil)
        print("IOS-E2E DONE")
        fflush(stdout)
    }

    static func report(_ name: String, _ ok: Bool) {
        print("IOS-E2E \(ok ? "PASS" : "FAIL") \(name)")
    }
}
