import SwiftUI

// Screen 13 — Stocks watchlist (pure SwiftUI twin of bench/screens/Screen13.kt).
struct Screen13View: View {
    let vm: Screen13ViewModel
    @StateObject private var state: FlowState<Screen13State>

    init(vm: Screen13ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Portfolio Value").font(.caption).textCase(.uppercase).foregroundStyle(.secondary)
                        Text(s.portfolioValue).font(.system(size: 34, weight: .bold, design: .rounded)).monospacedDigit()
                        HStack(spacing: 4) {
                            Image(systemName: s.portfolioUp ? "arrow.up.right" : "arrow.down.right").font(.caption.bold())
                            Text(verbatim: s.portfolioChange).font(.subheadline.weight(.medium)).monospacedDigit()
                        }
                        .foregroundStyle(s.portfolioUp ? Color.green : Color.red)
                        HStack(spacing: 6) {
                            ForEach(s.ranges, id: \.self) { range in
                                let selected = range == s.selectedRange
                                Text(range)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(selected ? Color.white : Color.secondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                                    .background(selected ? Color.blue : Color.gray.opacity(0.12), in: Capsule())
                            }
                        }
                        .padding(.top, 8)
                    }
                    .padding(.vertical, 4)
                }
                Section {
                    ForEach(s.stocks, id: \.id) { stock in
                        let tint = stock.isUp ? Color(red: 0.2, green: 0.78, blue: 0.35) : Color(red: 1.0, green: 0.23, blue: 0.19)
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(stock.symbol).font(.headline)
                                Text(stock.name).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                            }
                            .frame(width: 96, alignment: .leading)
                            HStack(alignment: .bottom, spacing: 2) {
                                ForEach(Array(stock.bars.enumerated()), id: \.offset) { index, height in
                                    RoundedRectangle(cornerRadius: 1)
                                        .fill(tint.opacity(0.35 + Double(index) * 0.04))
                                        .frame(width: 3, height: height.doubleValue)
                                }
                            }
                            .frame(height: 28, alignment: .bottom)
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(stock.price).font(.subheadline.weight(.semibold)).monospacedDigit()
                                Text(verbatim: stock.change)
                                    .font(.caption.weight(.bold))
                                    .monospacedDigit()
                                    .foregroundStyle(Color.white)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .frame(minWidth: 64)
                                    .background(tint, in: Capsule())
                            }
                        }
                    }
                } header: {
                    Text("Symbols")
                } footer: {
                    Text(s.marketNote)
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Watchlist")
        }
    }
}
