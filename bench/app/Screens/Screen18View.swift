import SwiftUI

// Screen 18 — Banking (pure SwiftUI twin of bench/screens/Screen18.kt).
struct Screen18View: View {
    let vm: Screen18ViewModel
    @StateObject private var state: FlowState<Screen18State>

    init(vm: Screen18ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text(s.accountName).font(.subheadline.weight(.semibold)).opacity(0.85)
                            Spacer()
                            Image(systemName: "creditcard.fill").font(.title3)
                        }
                        Text(s.balance).font(.system(size: 36, weight: .bold, design: .rounded)).monospacedDigit()
                        Text(s.cardNumber).font(.footnote).monospaced().opacity(0.8)
                        HStack(spacing: 24) {
                            VStack(alignment: .leading, spacing: 2) {
                                Label("Income", systemImage: "arrow.down.circle.fill").font(.caption).opacity(0.8)
                                Text(s.income).font(.headline).monospacedDigit()
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Label("Spent", systemImage: "arrow.up.circle.fill").font(.caption).opacity(0.8)
                                Text(s.spent).font(.headline).monospacedDigit()
                            }
                        }
                    }
                    .padding(20)
                    .foregroundStyle(Color.white)
                    .background(
                        LinearGradient(
                            colors: [Color(red: 0.1, green: 0.2, blue: 0.55), Color(red: 0.35, green: 0.25, blue: 0.85)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        ignoresSafeAreaEdges: .all
                    )
                    .listRowInsets(EdgeInsets())
                    HStack {
                        ForEach(s.actions, id: \.id) { action in
                            VStack(spacing: 6) {
                                Image(systemName: action.icon)
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundStyle(Color.blue)
                                    .frame(width: 44, height: 44)
                                    .background(Color.blue.opacity(0.12), in: Circle())
                                Text(action.title).font(.caption)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 4)
                }
                Section("Savings goals") {
                    ForEach(s.goals, id: \.id) { goal in
                        HStack(spacing: 12) {
                            Image(systemName: goal.icon)
                                .foregroundStyle(Color.white)
                                .frame(width: 32, height: 32)
                                .background(goal.tint.color, in: RoundedRectangle(cornerRadius: 8))
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(goal.name).font(.subheadline.weight(.medium))
                                    Spacer()
                                    Text(goal.saved).font(.caption).monospacedDigit().foregroundStyle(.secondary)
                                }
                                ProgressView(value: goal.progress).tint(goal.tint.color)
                            }
                        }
                    }
                }
                ForEach(s.days, id: \.id) { day in
                    Section {
                        ForEach(day.transactions, id: \.id) { tx in
                            HStack(spacing: 12) {
                                Image(systemName: tx.icon)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(tx.tint.color)
                                    .frame(width: 36, height: 36)
                                    .background(tx.tint.color.opacity(0.14), in: Circle())
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(tx.merchant).font(.subheadline.weight(.medium))
                                    Text(tx.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(tx.amount)
                                    .font(.subheadline.weight(.semibold))
                                    .monospacedDigit()
                                    .foregroundStyle(tx.isCredit ? Color.green : Color.primary)
                            }
                        }
                    } header: {
                        HStack {
                            Text(day.title)
                            Spacer()
                            Text(day.total).monospacedDigit()
                        }
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Wallet")
        }
    }
}
