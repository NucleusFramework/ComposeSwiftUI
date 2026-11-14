import SwiftUI

// Screen 39 — Analytics table (pure SwiftUI twin of bench/screens/Screen39.kt).
struct Screen39View: View {
    let vm: Screen39ViewModel
    @StateObject private var state: FlowState<Screen39State>

    init(vm: Screen39ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Label(s.range, systemImage: "calendar").font(.subheadline).foregroundStyle(.secondary)
                    HStack(spacing: 10) {
                        ForEach(s.kpis, id: \.id) { kpi in
                            VStack(alignment: .leading, spacing: 6) {
                                Image(systemName: kpi.icon).font(.subheadline).foregroundStyle(kpi.tint.color)
                                Text(verbatim: kpi.value).font(.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
                                Text(verbatim: kpi.title).font(.caption2).foregroundStyle(.secondary)
                                Text(verbatim: kpi.delta).font(.caption2).bold().foregroundStyle(kpi.up ? Color.green : Color.red)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(kpi.tint.color.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    Text("Top pages").font(.headline)
                    Grid(alignment: .leading, horizontalSpacing: 0, verticalSpacing: 0) {
                        GridRow {
                            Text("PAGE")
                            Text("VIEWS").gridColumnAlignment(.trailing)
                            Text("USERS").gridColumnAlignment(.trailing)
                            Text("BOUNCE").gridColumnAlignment(.trailing)
                            Text("CHANGE").gridColumnAlignment(.trailing)
                        }
                        .font(.caption).fontWeight(.semibold).foregroundStyle(.secondary)
                        .padding(.horizontal, 8).padding(.vertical, 8)
                        Divider().gridCellUnsizedAxes(.horizontal)
                        ForEach(s.rows, id: \.id) { row in
                            GridRow {
                                Text(verbatim: row.page).fontWeight(.medium).lineLimit(1)
                                Text(verbatim: row.views)
                                Text(verbatim: row.visitors).foregroundStyle(.secondary)
                                Text(verbatim: row.bounce).foregroundStyle(.secondary)
                                HStack(spacing: 2) {
                                    Image(systemName: row.up ? "arrow.up.right" : "arrow.down.right").imageScale(.small)
                                    Text(verbatim: row.change)
                                }
                                .foregroundStyle(row.up ? Color.green : Color.red)
                            }
                            .font(.footnote).monospacedDigit()
                            .padding(.horizontal, 8).padding(.vertical, 9)
                            .background(row.striped ? Color.gray.opacity(0.08) : Color.clear)
                        }
                    }
                    .padding(.vertical, 4)
                    .background { RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.25), lineWidth: 1) }
                    Text(verbatim: s.footnote).font(.caption).foregroundStyle(.secondary)
                }
                .padding()
            }
            .navigationTitle("Analytics")
        }
    }
}
