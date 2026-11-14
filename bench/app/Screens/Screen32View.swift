import SwiftUI

// Screen 32 — Habit tracker (pure SwiftUI twin of bench/screens/Screen32.kt).
struct Screen32View: View {
    let vm: Screen32ViewModel
    @StateObject private var state: FlowState<Screen32State>

    init(vm: Screen32ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(verbatim: s.dateLine).font(.caption).foregroundStyle(.secondary).textCase(.uppercase)
                                Text(verbatim: s.summary).font(.title3).fontWeight(.semibold)
                            }
                            Spacer()
                            ZStack {
                                Circle().fill(Color.orange.opacity(0.15))
                                Image(systemName: "flame.fill").font(.title3).foregroundStyle(Color.orange)
                            }
                            .frame(width: 44, height: 44)
                        }
                        ProgressView(value: s.progress).tint(Color.green)
                        Label(s.bestStreak, systemImage: "trophy.fill").font(.footnote).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
                Section {
                    ForEach(s.habits, id: \.id) { habit in
                        HStack(spacing: 12) {
                            Image(systemName: habit.icon)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(habit.tint.color)
                                .frame(width: 36, height: 36)
                                .background(habit.tint.color.opacity(0.15), in: RoundedRectangle(cornerRadius: 9))
                            VStack(alignment: .leading, spacing: 3) {
                                Text(verbatim: habit.name).font(.body).lineLimit(1)
                                HStack(spacing: 4) {
                                    Image(systemName: "flame").imageScale(.small)
                                    Text(verbatim: habit.streak).monospacedDigit()
                                    Text(verbatim: "· \(habit.goal)")
                                }
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                            }
                            Spacer(minLength: 4)
                            HStack(spacing: 5) {
                                ForEach(habit.days, id: \.id) { day in
                                    Circle()
                                        .fill(day.done ? habit.tint.color : Color.gray.opacity(0.25))
                                        .frame(width: 8, height: 8)
                                }
                            }
                            Image(systemName: habit.doneToday ? "checkmark.circle.fill" : "circle")
                                .font(.title2)
                                .foregroundStyle(habit.doneToday ? Color.green : Color.gray.opacity(0.5))
                                .frame(width: 32)
                        }
                    }
                } header: {
                    HStack {
                        Text("Today")
                        Spacer()
                        HStack(spacing: 5) {
                            ForEach(s.weekdays, id: \.id) { day in
                                Text(verbatim: day.letter).font(.caption2).monospaced().frame(width: 8)
                            }
                        }
                        .padding(.trailing, 44)
                    }
                } footer: {
                    Text(verbatim: s.tip)
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Habits")
        }
    }
}
