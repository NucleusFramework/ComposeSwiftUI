import SwiftUI

// Screen 25 — Onboarding (pure SwiftUI twin of bench/screens/Screen25.kt).
struct Screen25View: View {
    let vm: Screen25ViewModel
    @StateObject private var state: FlowState<Screen25State>

    init(vm: Screen25ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        let page = s.page
        NavigationStack {
            VStack(spacing: 22) {
                Text(s.step).font(.caption).fontWeight(.semibold).textCase(.uppercase).foregroundStyle(.secondary)
                hero(page)
                VStack(spacing: 12) {
                    Text(page.title).font(.largeTitle.bold()).multilineTextAlignment(.center)
                    Text(page.text).font(.body).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .padding(.horizontal, 32)
                HStack(spacing: 12) {
                    ForEach(page.highlights, id: \.id) { h in
                        VStack(spacing: 6) {
                            Image(systemName: h.icon)
                                .font(.title3)
                                .symbolRenderingMode(.palette)
                                .foregroundStyle(page.tint.color, page.accent.color)
                            Text(h.text).font(.caption).fontWeight(.medium)
                        }
                        .frame(width: 96, height: 72)
                        .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 14))
                    }
                }
                HStack(spacing: 8) {
                    ForEach(s.dots, id: \.id) { dot in
                        Capsule().fill(dot.tint.color).frame(width: dot.width, height: 8)
                    }
                }
                Spacer()
                VStack(spacing: 12) {
                    Button(action: {}) {
                        Text(s.primary)
                            .font(.headline)
                            .foregroundStyle(Color.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(page.tint.color, in: RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    Button("Skip for now", action: {}).font(.subheadline).tint(page.tint.color)
                }
                .padding(.horizontal, 24)
            }
            .padding(.bottom, 24)
            .navigationTitle("Welcome")
            .screen25InlineTitle()
        }
    }

    private func hero(_ page: Screen25Page) -> some View {
        ZStack {
            Circle().stroke(page.tint.color.opacity(0.15), lineWidth: 10).frame(width: 250, height: 250)
            Circle()
                .fill(LinearGradient(colors: [page.tint.color.opacity(0.25), page.accent.color.opacity(0.1)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 200, height: 200)
            Image(systemName: page.symbol)
                .font(.system(size: 92, weight: .medium))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(page.tint.color)
            Image(systemName: "sparkle")
                .font(.system(size: 22)).foregroundStyle(page.accent.color).offset(x: 96, y: -84)
        }
    }
}

private extension View {
    /// `.navigationBarTitleDisplayMode(.inline)` on iOS; no-op on macOS (as the generated Kotlin binding).
    @ViewBuilder func screen25InlineTitle() -> some View {
        #if os(iOS)
        navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
}
