import SwiftUI

@main
struct SpikeApp: App {
    init() { ComposeGenerated.setUp() }
    var body: some Scene {
        WindowGroup {
            ComposeView { SpikeKt.spikeScreen() }.frame(minWidth: 300, minHeight: 300)
        }
    }
}
