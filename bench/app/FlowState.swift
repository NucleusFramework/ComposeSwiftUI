import SwiftUI

/// Exposes a Kotlin `StateFlow` (e.g. a shared ViewModel's state) to plain SwiftUI code.
@MainActor
final class FlowState<T>: ObservableObject {
    @Published private(set) var value: T
    private var watcher: FlowWatcher?

    init(_ flow: any Kotlinx_coroutines_coreStateFlow) {
        value = flow.value as! T
        watcher = FlowWatchKt.watchFlow(flow: flow) { [weak self] newValue in
            self?.value = newValue as! T
        }
    }

    deinit {
        watcher?.cancel()
    }
}

extension Rgb {
    var color: Color { Color(red: r, green: g, blue: b) }
}

extension View {
    /// `.listStyle(.insetGrouped)` on iOS; no-op on macOS (as the generated Kotlin binding).
    @ViewBuilder func insetGroupedListStyle() -> some View {
        #if os(iOS)
        listStyle(.insetGrouped)
        #else
        self
        #endif
    }
}
