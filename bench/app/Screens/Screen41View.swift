import SwiftUI

// Screen 41 — Crypto wallet (pure SwiftUI twin of bench/screens/Screen41.kt).
struct Screen41View: View {
    let vm: Screen41ViewModel
    @StateObject private var state: FlowState<Screen41State>

    init(vm: Screen41ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Total balance").font(.subheadline).foregroundStyle(.secondary)
                            Text(verbatim: s.balance).font(.system(size: 34, weight: .bold)).fontDesign(.rounded).monospacedDigit()
                            Label {
                                Text(verbatim: s.balanceChange)
                            } icon: {
                                Image(systemName: s.balanceUp ? "arrow.up.right.circle.fill" : "arrow.down.right.circle.fill")
                            }
                            .font(.subheadline).fontWeight(.semibold).foregroundStyle(s.balanceUp ? Color.green : Color.red)
                        }
                        Picker("Range", selection: Binding(get: { s.range }, set: { vm.setRange(value: $0) })) {
                            ForEach(s.ranges, id: \.self) { r in
                                Text(verbatim: r).tag(r)
                            }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                        HStack(alignment: .bottom, spacing: 4) {
                            ForEach(s.bars, id: \.id) { bar in
                                RoundedRectangle(cornerRadius: 2)
                                    .fill(bar.up ? Color.green.opacity(0.75) : Color.red.opacity(0.6))
                                    .frame(height: bar.height)
                            }
                        }
                        .frame(height: 110, alignment: .bottom)
                    }
                    .padding(.vertical, 6)
                }
                Section {
                    HStack {
                        ForEach(s.actions, id: \.id) { action in
                            VStack(spacing: 6) {
                                Image(systemName: action.icon)
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundStyle(Color.white)
                                    .frame(width: 44, height: 44)
                                    .background(Color.accentColor, in: Circle())
                                Text(verbatim: action.title).font(.caption)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                }
                Section("Assets") {
                    ForEach(s.assets, id: \.id) { asset in
                        HStack(spacing: 12) {
                            Image(systemName: asset.icon)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(Color.white)
                                .frame(width: 40, height: 40)
                                .background(asset.tint.color, in: Circle())
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 6) {
                                    Text(verbatim: asset.name).font(.body).fontWeight(.medium)
                                    Text(verbatim: asset.symbol).font(.caption2).foregroundStyle(.secondary).padding(.horizontal, 4).background(Color.gray.opacity(0.15), in: RoundedRectangle(cornerRadius: 4))
                                }
                                Text(verbatim: asset.holdings).font(.caption).monospacedDigit().foregroundStyle(.secondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 3) {
                                Text(verbatim: asset.value).font(.body).monospacedDigit()
                                Text(verbatim: asset.change)
                                    .font(.caption).fontWeight(.semibold).monospacedDigit()
                                    .foregroundStyle(asset.up ? Color.green : Color.red)
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background((asset.up ? Color.green : Color.red).opacity(0.12), in: Capsule())
                            }
                        }
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Wallet")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Scan", systemImage: "qrcode.viewfinder") {}
                }
            }
        }
    }
}
