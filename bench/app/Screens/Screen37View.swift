import SwiftUI

// Screen 37 — Boarding pass (pure SwiftUI twin of bench/screens/Screen37.kt).
struct Screen37View: View {
    let vm: Screen37ViewModel
    @StateObject private var state: FlowState<Screen37State>

    init(vm: Screen37ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 0) {
                        VStack(spacing: 16) {
                            HStack {
                                Label(s.airline, systemImage: "airplane.circle.fill").font(.headline).kerning(1)
                                Spacer()
                                Text(verbatim: s.flight).font(.caption).opacity(0.85)
                            }
                            HStack(alignment: .center) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(verbatim: s.fromCode).font(.system(size: 40, weight: .heavy)).fontDesign(.rounded)
                                    Text(verbatim: s.fromCity).font(.caption).opacity(0.85)
                                    Text(verbatim: s.fromTime).font(.title3).bold().monospacedDigit()
                                }
                                Spacer()
                                VStack(spacing: 6) {
                                    HStack(spacing: 4) {
                                        ForEach(0..<3, id: \.self) { _ in
                                            Circle().fill(Color.white.opacity(0.6)).frame(width: 4, height: 4)
                                        }
                                        Image(systemName: "airplane").font(.title2)
                                        ForEach(3..<6, id: \.self) { _ in
                                            Circle().fill(Color.white.opacity(0.6)).frame(width: 4, height: 4)
                                        }
                                    }
                                    Text(verbatim: s.duration).font(.caption2).fontWeight(.semibold).opacity(0.85)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(verbatim: s.toCode).font(.system(size: 40, weight: .heavy)).fontDesign(.rounded)
                                    Text(verbatim: s.toCity).font(.caption).opacity(0.85)
                                    Text(verbatim: s.toTime).font(.title3).bold().monospacedDigit()
                                }
                            }
                        }
                        .foregroundStyle(Color.white)
                        .padding(20)
                        .background(LinearGradient(colors: [Color.indigo, Color.blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                        HStack(spacing: 5) {
                            ForEach(0..<40, id: \.self) { _ in
                                Rectangle().fill(Color.gray.opacity(0.5)).frame(width: 4, height: 1)
                            }
                        }
                        .padding(.horizontal, 12)
                        .frame(height: 1)
                        VStack(spacing: 18) {
                            Grid(alignment: .leading, horizontalSpacing: 24, verticalSpacing: 14) {
                                ForEach(s.rows, id: \.first!.id) { row in
                                    GridRow {
                                        ForEach(row, id: \.id) { field in
                                            VStack(alignment: .leading, spacing: 3) {
                                                Text(verbatim: field.label).font(.caption2).fontWeight(.semibold).foregroundStyle(.secondary)
                                                Text(verbatim: field.value).font(.headline)
                                            }
                                        }
                                    }
                                }
                            }
                            HStack {
                                Label(s.status, systemImage: s.onTime ? "checkmark.seal.fill" : "clock.badge.exclamationmark.fill")
                                    .font(.subheadline).fontWeight(.semibold)
                                    .foregroundStyle(s.onTime ? Color.green : Color.orange)
                                Spacer()
                            }
                            VStack(spacing: 6) {
                                HStack(alignment: .bottom, spacing: 2) {
                                    ForEach(s.bars, id: \.id) { bar in
                                        Rectangle().fill(Color.primary).frame(width: bar.width, height: bar.tall ? 64 : 56)
                                    }
                                }
                                Text(verbatim: s.code).font(.caption2).monospaced().kerning(2).foregroundStyle(.secondary)
                            }
                        }
                        .padding(20)
                    }
                    .background(.background)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .shadow(color: Color.black.opacity(0.12), radius: 10, y: 4)
                    Label("Add to Apple Wallet", systemImage: "wallet.pass.fill")
                        .font(.subheadline).fontWeight(.semibold).foregroundStyle(Color.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.black, in: RoundedRectangle(cornerRadius: 12))
                }
                .padding()
            }
            .background(Color.indigo.opacity(0.08))
            .navigationTitle("Boarding Pass")
        }
    }
}
