import SwiftUI

// Screen 23 — Bar chart report (pure SwiftUI twin of bench/screens/Screen23.kt).
struct Screen23View: View {
    let vm: Screen23ViewModel
    @StateObject private var state: FlowState<Screen23State>
    private let chartHeight: Double = 160

    init(vm: Screen23ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(s.period).font(.subheadline).foregroundStyle(.secondary)
                        Text(s.total).font(.system(size: 40, weight: .bold, design: .rounded)).monospacedDigit()
                        Label(s.change, systemImage: "arrow.up.forward.circle.fill")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Color.green)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.green.opacity(0.12), in: Capsule())
                    }
                    chart(s)
                    summary(s)
                }
                .padding()
            }
            .navigationTitle("Revenue")
        }
    }

    private func chart(_ s: Screen23State) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Monthly revenue").font(.headline)
                Spacer()
                Text("k$").font(.caption).foregroundStyle(.secondary)
            }
            HStack(alignment: .bottom, spacing: 8) {
                VStack(alignment: .trailing) {
                    ForEach(s.axis, id: \.id) { tick in
                        Text(tick.label).font(.caption2).monospacedDigit().foregroundStyle(.secondary)
                        if tick.id < Int32(s.axis.count) - 1 { Spacer() }
                    }
                }
                .frame(height: chartHeight + 8)
                .padding(.bottom, 22)
                ForEach(s.bars, id: \.id) { bar in
                    VStack(spacing: 4) {
                        Text(bar.valueText)
                            .font(.caption2).fontWeight(bar.highlighted ? .bold : .regular)
                            .foregroundStyle(bar.highlighted ? Color.blue : Color.secondary)
                        HStack(alignment: .bottom, spacing: 2) {
                            UnevenRoundedRectangle(topLeadingRadius: 4, topTrailingRadius: 4)
                                .fill(Color.gray.opacity(0.35))
                                .frame(width: 9, height: bar.previousHeight)
                            UnevenRoundedRectangle(topLeadingRadius: 4, topTrailingRadius: 4)
                                .fill(bar.highlighted
                                    ? LinearGradient(colors: [Color.blue, Color.cyan], startPoint: .top, endPoint: .bottom)
                                    : LinearGradient(colors: [Color.blue.opacity(0.6), Color.blue.opacity(0.35)], startPoint: .top, endPoint: .bottom))
                                .frame(width: 13, height: bar.currentHeight)
                        }
                        Text(bar.label).font(.caption2).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            Divider()
            HStack(spacing: 16) {
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 3).fill(Color.blue).frame(width: 12, height: 12)
                    Text("2026").font(.caption)
                }
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 3).fill(Color.gray.opacity(0.35)).frame(width: 12, height: 12)
                    Text("2025").font(.caption)
                }
                Spacer()
                Label("Selected", systemImage: "hand.tap.fill").font(.caption).foregroundStyle(Color.blue)
            }
        }
        .padding(16)
        .background(Color.gray.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))
    }

    private func summary(_ s: Screen23State) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Summary").font(.headline).padding(.bottom, 8)
            ForEach(s.summary, id: \.id) { row in
                HStack(spacing: 12) {
                    Image(systemName: row.icon)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(row.tint.color)
                        .frame(width: 32, height: 32)
                        .background(row.tint.color.opacity(0.15), in: Circle())
                    Text(row.title).font(.subheadline)
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(verbatim: row.value).font(.subheadline).bold().monospacedDigit()
                        Text(row.delta).font(.caption2).foregroundStyle(row.deltaTint.color)
                    }
                }
                .padding(.vertical, 10)
                Divider()
            }
        }
    }
}
