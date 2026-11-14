import SwiftUI

// Screen 26 — Leaderboard (pure SwiftUI twin of bench/screens/Screen26.kt).
struct Screen26View: View {
    let vm: Screen26ViewModel
    @StateObject private var state: FlowState<Screen26State>

    init(vm: Screen26ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    podium(s.podium)
                } header: {
                    Text(s.season)
                } footer: {
                    Text(s.you)
                }
                Section("Rankings") {
                    ForEach(s.players, id: \.id) { p in
                        row(p)
                    }
                }
            }
            .navigationTitle("Leaderboard")
        }
    }

    private func row(_ p: Screen26Player) -> some View {
        HStack(spacing: 12) {
            Text(p.rank).font(.subheadline.monospacedDigit()).fontWeight(.bold).foregroundStyle(p.medalTint.color).frame(width: 22, alignment: .leading)
            Text(p.initials)
                .font(.caption).fontWeight(.bold)
                .foregroundStyle(Color.white)
                .frame(width: 36, height: 36)
                .background(LinearGradient(colors: [p.color.color.opacity(0.7), p.color.color], startPoint: .top, endPoint: .bottom), in: Circle())
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 4) {
                    Text(p.name).font(.subheadline).fontWeight(.semibold)
                    Image(systemName: p.trendIcon).font(.system(size: 8)).foregroundStyle(p.trendTint.color)
                }
                ProgressView(value: p.progress).tint(p.color.color)
            }
            Text(p.score).font(.subheadline.monospacedDigit()).fontWeight(.semibold).frame(width: 64, alignment: .trailing)
        }
    }

    private func podium(_ players: [Screen26Player]) -> some View {
        HStack(alignment: .bottom, spacing: 10) {
            ForEach(players, id: \.id) { p in
                VStack(spacing: 6) {
                    ZStack(alignment: .bottomTrailing) {
                        Text(p.initials)
                            .font(.headline)
                            .foregroundStyle(Color.white)
                            .frame(width: 56, height: 56)
                            .background(p.color.color, in: Circle())
                            .overlay { Circle().stroke(p.medalTint.color, lineWidth: 3) }
                        Image(systemName: "medal.fill")
                            .font(.system(size: 16))
                            .symbolRenderingMode(.palette)
                            .foregroundStyle(p.medalTint.color, Color.red)
                            .offset(x: 4, y: 4)
                    }
                    .frame(width: 56, height: 56)
                    Text(p.name).font(.caption).fontWeight(.semibold).lineLimit(1)
                    Text(p.score).font(.caption2).monospacedDigit().foregroundStyle(.secondary)
                    Text(p.rank)
                        .font(.system(size: 28, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: p.podiumHeight)
                        .background(
                            LinearGradient(colors: [p.medalTint.color, p.medalTint.color.opacity(0.6)], startPoint: .top, endPoint: .bottom),
                            in: UnevenRoundedRectangle(topLeadingRadius: 10, topTrailingRadius: 10)
                        )
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}
