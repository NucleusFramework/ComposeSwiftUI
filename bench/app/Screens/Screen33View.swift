import SwiftUI

// Screen 33 — Expense splitting (pure SwiftUI twin of bench/screens/Screen33.kt).
struct Screen33View: View {
    let vm: Screen33ViewModel
    @StateObject private var state: FlowState<Screen33State>

    init(vm: Screen33ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Label(s.trip, systemImage: "airplane").font(.headline).foregroundStyle(Color.white)
                        Text(verbatim: s.total).font(.system(size: 38, weight: .bold)).monospacedDigit().foregroundStyle(Color.white)
                        Text(verbatim: s.dates).font(.subheadline).foregroundStyle(Color.white.opacity(0.8))
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(colors: [Color.teal, Color.blue], startPoint: .topLeading, endPoint: .bottomTrailing),
                        in: RoundedRectangle(cornerRadius: 18)
                    )
                    Text("Balances").font(.title3).bold()
                    VStack(spacing: 0) {
                        ForEach(s.people, id: \.id) { person in
                            if person.id > 0 { Divider().padding(.leading, 64) }
                            HStack(spacing: 12) {
                                ZStack {
                                    Circle().fill(person.tint.color.opacity(0.2))
                                    Text(verbatim: person.initials).font(.subheadline).fontWeight(.semibold).foregroundStyle(person.tint.color)
                                }
                                .frame(width: 40, height: 40)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(verbatim: person.name).font(.body)
                                    Text(verbatim: person.paid).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(verbatim: person.balance)
                                    .font(.subheadline).fontWeight(.semibold).monospacedDigit()
                                    .foregroundStyle(person.owes ? Color.red : Color.green)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background((person.owes ? Color.red : Color.green).opacity(0.12), in: Capsule())
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                        }
                    }
                    .padding(.vertical, 6)
                    .background(Color.gray.opacity(0.1), in: RoundedRectangle(cornerRadius: 14))
                    Text("Expenses").font(.title3).bold()
                    VStack(spacing: 12) {
                        ForEach(s.expenses, id: \.id) { expense in
                            HStack(spacing: 12) {
                                Image(systemName: expense.icon)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(Color.white)
                                    .frame(width: 34, height: 34)
                                    .background(expense.tint.color, in: Circle())
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(verbatim: expense.title)
                                    Text(verbatim: expense.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(verbatim: expense.amount).monospacedDigit().fontWeight(.medium)
                            }
                        }
                    }
                    VStack(spacing: 10) {
                        ForEach(s.totals, id: \.label) { total in
                            HStack {
                                Text(verbatim: total.label).foregroundStyle(.secondary)
                                Spacer()
                                Text(verbatim: total.value).monospacedDigit().bold()
                            }
                        }
                        Divider()
                        Button("Settle up", systemImage: "arrow.left.arrow.right") {}
                            .buttonStyle(.borderedProminent)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .padding(16)
                    .background {
                        RoundedRectangle(cornerRadius: 14).stroke(Color.gray.opacity(0.3), lineWidth: 1)
                    }
                }
                .padding()
            }
            .navigationTitle("Split")
        }
    }
}
