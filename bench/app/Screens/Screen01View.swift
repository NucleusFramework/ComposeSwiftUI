import SwiftUI

// Screen 01 — Profile & settings (pure SwiftUI twin of bench/screens/Screen01.kt).
struct Screen01View: View {
    let vm: Screen01ViewModel
    @StateObject private var state: FlowState<Screen01State>

    init(vm: Screen01ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 16) {
                        ZStack {
                            Circle().fill(Color(red: 0.0, green: 0.48, blue: 1.0))
                            Text(s.initials).font(.title2).fontWeight(.bold).foregroundStyle(Color.white)
                        }
                        .frame(width: 64, height: 64)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(s.name).font(.headline)
                            Text(s.handle).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    HStack {
                        ForEach(s.stats, id: \.label) { stat in
                            VStack(spacing: 2) {
                                Text(stat.value).font(.title3).bold().monospacedDigit()
                                Text(stat.label).font(.caption).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                }
                Section("Preferences") {
                    Toggle("Notifications", isOn: Binding(get: { s.notifications }, set: { vm.setNotifications(value: $0) }))
                    Toggle("iCloud Sync", isOn: Binding(get: { s.syncEnabled }, set: { vm.setSync(value: $0) }))
                }
                Section {
                    ForEach(s.rows, id: \.id) { row in
                        HStack(spacing: 12) {
                            Image(systemName: row.icon)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Color.white)
                                .frame(width: 30, height: 30)
                                .background(row.tint.color, in: RoundedRectangle(cornerRadius: 7))
                            Text(row.title)
                            Spacer()
                            Text(row.detail).foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text("General")
                } footer: {
                    Text(s.footer)
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Profile")
        }
    }
}
