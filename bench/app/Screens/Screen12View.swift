import SwiftUI

// Screen 12 — Fitness activity (pure SwiftUI twin of bench/screens/Screen12.kt).
struct Screen12View: View {
    let vm: Screen12ViewModel
    @StateObject private var state: FlowState<Screen12State>

    init(vm: Screen12ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text(s.dateLine).font(.subheadline).foregroundStyle(.secondary)
                        Spacer()
                        Label(s.streak, systemImage: "bolt.heart.fill").font(.caption.weight(.semibold)).foregroundStyle(Color.orange)
                    }
                    HStack(spacing: 12) {
                        ForEach(s.rings, id: \.id) { ring in
                            VStack(spacing: 8) {
                                Gauge(value: ring.progress, in: 0...1) {
                                    Text(ring.title)
                                } currentValueLabel: {
                                    Image(systemName: ring.icon).foregroundStyle(ring.tint.color)
                                }
                                .gaugeStyle(.accessoryCircularCapacity)
                                .tint(ring.tint.color)
                                .scaleEffect(1.2)
                                VStack(spacing: 0) {
                                    Text(ring.valueText).font(.headline).monospacedDigit()
                                    Text(ring.goalText).font(.caption2).foregroundStyle(.secondary)
                                }
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 16)
                    Text("Today").font(.title3).bold()
                    Grid(horizontalSpacing: 12, verticalSpacing: 12) {
                        ForEach(chunked(s.stats), id: \.first!.id) { row in
                            GridRow {
                                ForEach(row, id: \.id) { stat in
                                    VStack(alignment: .leading, spacing: 6) {
                                        HStack(spacing: 6) {
                                            Image(systemName: stat.icon).font(.caption.weight(.bold)).foregroundStyle(stat.tint.color)
                                            Text(stat.label).font(.caption).foregroundStyle(.secondary)
                                        }
                                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                                            Text(stat.value).font(.system(size: 24, weight: .bold, design: .rounded)).monospacedDigit()
                                            Text(stat.unit).font(.caption).foregroundStyle(.secondary)
                                        }
                                    }
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(.background, in: RoundedRectangle(cornerRadius: 14))
                                }
                            }
                        }
                    }
                    HStack {
                        Text("Workouts").font(.title3).bold()
                        Spacer()
                        Text("Show More").font(.subheadline).foregroundStyle(Color.green)
                    }
                    VStack(spacing: 0) {
                        ForEach(Array(s.workouts.enumerated()), id: \.element.id) { index, workout in
                            HStack(spacing: 12) {
                                Image(systemName: workout.icon)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundStyle(workout.tint.color)
                                    .frame(width: 40, height: 40)
                                    .background(workout.tint.color.opacity(0.18), in: Circle())
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(workout.name).font(.subheadline.weight(.semibold))
                                    Text(workout.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(workout.duration).font(.subheadline).monospacedDigit()
                                    Text(workout.calories).font(.caption).foregroundStyle(workout.tint.color)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            if index < s.workouts.count - 1 {
                                Divider().padding(.leading, 64)
                            }
                        }
                    }
                    .background(.background, in: RoundedRectangle(cornerRadius: 14))
                }
                .padding()
            }
            .background(Color.gray.opacity(0.1))
            .navigationTitle("Activity")
        }
    }

    private func chunked(_ items: [Screen12Stat]) -> [[Screen12Stat]] {
        stride(from: 0, to: items.count, by: 2).map { Array(items[$0..<min($0 + 2, items.count)]) }
    }
}
