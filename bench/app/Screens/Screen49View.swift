import SwiftUI

// Screen 49 — Sleep tracker (pure SwiftUI twin of bench/screens/Screen49.kt).
struct Screen49View: View {
    let vm: Screen49ViewModel
    @StateObject private var state: FlowState<Screen49State>

    init(vm: Screen49ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("ASLEEP").font(.caption).fontWeight(.semibold).foregroundStyle(.secondary)
                            Text(s.asleep).font(.system(size: 34, weight: .bold, design: .rounded)).monospacedDigit()
                            Text(s.window).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        VStack(spacing: 0) {
                            Text(s.score).font(.title2).bold().monospacedDigit().foregroundStyle(s.scoreTint.color)
                            Text("SCORE").font(.system(size: 9, weight: .semibold)).foregroundStyle(.secondary)
                        }
                        .frame(width: 64, height: 64)
                        .background(s.scoreTint.color.opacity(0.15), in: Circle())
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .bottom, spacing: 6) {
                            ForEach(s.hours, id: \.id) { hour in
                                VStack(spacing: 4) {
                                    VStack(spacing: 1) {
                                        ForEach(hour.segments, id: \.id) { segment in
                                            Rectangle().fill(segment.tint.color).frame(height: segment.height)
                                        }
                                    }
                                    .clipShape(RoundedRectangle(cornerRadius: 4))
                                    Text(hour.label).font(.system(size: 9)).foregroundStyle(.secondary)
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                        .frame(height: 132, alignment: .bottom)
                        HStack(spacing: 12) {
                            ForEach(s.stages, id: \.id) { stage in
                                HStack(spacing: 4) {
                                    RoundedRectangle(cornerRadius: 2).fill(stage.tint.color).frame(width: 10, height: 10)
                                    Text(stage.name).font(.caption2).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    HStack {
                        ForEach(s.stats, id: \.id) { stat in
                            VStack(spacing: 3) {
                                Image(systemName: stat.icon).font(.subheadline).foregroundStyle(Color.indigo)
                                Text(stat.value).font(.headline).monospacedDigit()
                                Text(stat.label).font(.caption2).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                } header: {
                    Text(s.night)
                }
                Section("Stages") {
                    ForEach(s.stages, id: \.id) { stage in
                        LabeledContent {
                            HStack(spacing: 8) {
                                Text(stage.duration).monospacedDigit()
                                Text(stage.share)
                                    .font(.caption).monospacedDigit().foregroundStyle(stage.tint.color)
                                    .frame(width: 40, alignment: .trailing)
                            }
                        } label: {
                            HStack(spacing: 8) {
                                Circle().fill(stage.tint.color).frame(width: 10, height: 10)
                                Text(stage.name)
                            }
                        }
                    }
                }
                Section("Vitals") {
                    ForEach(s.vitals, id: \.id) { vital in
                        LabeledContent {
                            Text(vital.value).monospacedDigit()
                        } label: {
                            Label(vital.title, systemImage: vital.icon)
                        }
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle("Sleep")
        }
    }
}
