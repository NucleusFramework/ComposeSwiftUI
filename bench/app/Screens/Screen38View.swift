import SwiftUI

// Screen 38 — Medication schedule (pure SwiftUI twin of bench/screens/Screen38.kt).
struct Screen38View: View {
    let vm: Screen38ViewModel
    @StateObject private var state: FlowState<Screen38State>

    init(vm: Screen38ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(verbatim: s.day).font(.caption).foregroundStyle(.secondary)
                        HStack {
                            Text(verbatim: s.summary).font(.title3).fontWeight(.semibold)
                            Spacer()
                            Image(systemName: "pills.fill").font(.title2).foregroundStyle(Color.mint).symbolRenderingMode(.hierarchical)
                        }
                        ProgressView(value: s.progress).tint(Color.mint)
                        Label(s.nextDose, systemImage: "bell.badge.fill").font(.footnote).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
                ForEach(s.slots, id: \.id) { slot in
                    Section {
                        ForEach(slot.doses, id: \.id) { dose in
                            HStack(spacing: 14) {
                                HStack(spacing: 0) {
                                    Rectangle().fill(dose.left.color)
                                    Rectangle().fill(dose.right.color)
                                }
                                .frame(width: 34, height: 14)
                                .clipShape(Capsule())
                                .background { Capsule().stroke(Color.black.opacity(0.15), lineWidth: 1) }
                                .rotationEffect(.degrees(-35))
                                .frame(width: 36, height: 36)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(verbatim: dose.name).font(.body).fontWeight(.medium).strikethrough(dose.taken, color: Color.secondary)
                                    Text(verbatim: dose.dosage).font(.caption).foregroundStyle(.secondary)
                                    HStack(spacing: 6) {
                                        Label(dose.time, systemImage: "clock")
                                        if dose.withFood {
                                            Label("With food", systemImage: "fork.knife")
                                        }
                                    }
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                                }
                                Spacer()
                                Toggle("Taken", isOn: Binding(get: { dose.taken }, set: { vm.setTaken(id: dose.id, value: $0) }))
                                    .labelsHidden()
                                    .tint(Color.mint)
                            }
                        }
                    } header: {
                        HStack {
                            Label(slot.title, systemImage: slot.icon).foregroundStyle(slot.tint.color)
                            Spacer()
                            Text(verbatim: slot.count).monospacedDigit()
                        }
                    }
                }
                Section {
                    Label(s.refill, systemImage: "exclamationmark.triangle.fill").font(.footnote).foregroundStyle(Color.orange)
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Medications")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Add", systemImage: "plus") {}
                }
            }
        }
    }
}
