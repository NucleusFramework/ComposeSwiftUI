import SwiftUI

// Screen 46 — Sports scores (pure SwiftUI twin of bench/screens/Screen46.kt).
struct Screen46View: View {
    let vm: Screen46ViewModel
    @StateObject private var state: FlowState<Screen46State>

    init(vm: Screen46ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    private func teamColumn(_ team: Screen46Team, _ alignment: HorizontalAlignment) -> some View {
        VStack(alignment: alignment, spacing: 6) {
            ZStack {
                Circle().fill(team.tint.color)
                Text(team.short_).font(.system(size: 10, weight: .heavy)).foregroundStyle(Color.white)
            }
            .frame(width: 36, height: 36)
            Text(team.name).font(.subheadline).fontWeight(.medium).lineLimit(1)
        }
        .frame(width: 80)
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    ForEach(s.matches, id: \.id) { match in
                        VStack(spacing: 8) {
                            HStack {
                                teamColumn(match.home, .center)
                                Spacer()
                                VStack(spacing: 4) {
                                    HStack(spacing: 10) {
                                        Text(match.homeScoreText).font(.system(size: 30, weight: .bold, design: .rounded)).monospacedDigit()
                                        Text("–").font(.title2).foregroundStyle(.tertiary)
                                        Text(match.awayScoreText).font(.system(size: 30, weight: .bold, design: .rounded)).monospacedDigit()
                                    }
                                    if match.live {
                                        HStack(spacing: 4) {
                                            Circle().fill(Color.white).frame(width: 6, height: 6)
                                            Text("LIVE").font(.caption2).fontWeight(.black)
                                            Text(match.status).font(.caption2).fontWeight(.bold).monospacedDigit()
                                        }
                                        .foregroundStyle(Color.white)
                                        .padding(.horizontal, 7)
                                        .padding(.vertical, 3)
                                        .background(Color.red, in: Capsule())
                                    } else {
                                        Text(match.status).font(.caption).fontWeight(.semibold).foregroundStyle(.secondary)
                                    }
                                }
                                Spacer()
                                teamColumn(match.away, .center)
                            }
                            Label(match.venue, systemImage: "sportscourt").font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    HStack {
                        Text(s.league)
                        Spacer()
                        Text(s.matchday).font(.caption)
                    }
                }
                Section("Standings") {
                    Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 10) {
                        GridRow {
                            Text("#")
                            Text("Team")
                            Text("P").gridColumnAlignment(.trailing)
                            Text("W").gridColumnAlignment(.trailing)
                            Text("D").gridColumnAlignment(.trailing)
                            Text("L").gridColumnAlignment(.trailing)
                            Text("Pts").gridColumnAlignment(.trailing)
                        }
                        .font(.caption).fontWeight(.semibold).foregroundStyle(.secondary)
                        Divider()
                        ForEach(s.standings, id: \.id) { standing in
                            GridRow {
                                Text(standing.rank).foregroundStyle(.secondary)
                                HStack(spacing: 8) {
                                    Circle().fill(standing.tint.color).frame(width: 10, height: 10)
                                    Text(standing.team).fontWeight(.medium)
                                    Image(systemName: standing.trendUp ? "arrowtriangle.up.fill" : "arrowtriangle.down.fill")
                                        .font(.system(size: 8)).foregroundStyle(standing.trendUp ? Color.green : Color.red)
                                }
                                Text(standing.played)
                                Text(standing.won)
                                Text(standing.drawn)
                                Text(standing.lost)
                                Text(standing.points).bold()
                            }
                            .font(.subheadline).monospacedDigit()
                        }
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Scores")
        }
    }
}
