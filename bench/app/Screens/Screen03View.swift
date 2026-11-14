import SwiftUI

// Screen 03 — Analytics dashboard (pure SwiftUI twin of bench/screens/Screen03.kt).
struct Screen03View: View {
    let vm: Screen03ViewModel
    @StateObject private var state: FlowState<Screen03State>

    init(vm: Screen03ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Picker("Period", selection: Binding(get: { s.period }, set: { vm.setPeriod(value: $0) })) {
                        ForEach(Array(s.periods.enumerated()), id: \.offset) { index, label in
                            Text(label).tag(Int32(index))
                        }
                    }
                    .pickerStyle(.segmented)
                    LazyVGrid(columns: [GridItem(spacing: 12), GridItem(spacing: 12)], spacing: 12) {
                        ForEach(s.kpis, id: \.id) { kpi in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Image(systemName: kpi.icon).font(.title3).foregroundStyle(Color.white)
                                    Spacer()
                                    HStack(spacing: 2) {
                                        Image(systemName: kpi.up ? "arrow.up.right" : "arrow.down.right")
                                        Text(verbatim: kpi.delta)
                                    }
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(Color.white)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(Color.white.opacity(0.22), in: Capsule())
                                }
                                Text(verbatim: kpi.value).font(.title2.bold()).foregroundStyle(Color.white).monospacedDigit()
                                Text(kpi.title).font(.caption).foregroundStyle(Color.white.opacity(0.85))
                                ProgressView(value: kpi.progress).tint(Color.white)
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                LinearGradient(colors: [kpi.from.color, kpi.to.color], startPoint: .topLeading, endPoint: .bottomTrailing),
                                in: RoundedRectangle(cornerRadius: 16)
                            )
                        }
                    }
                    Text("System health").font(.headline)
                    HStack(spacing: 0) {
                        ForEach(s.gauges, id: \.id) { gauge in
                            VStack(spacing: 6) {
                                Gauge(value: gauge.value, in: 0...1) {
                                    Text(gauge.label)
                                } currentValueLabel: {
                                    Text(gauge.display)
                                }
                                .gaugeStyle(.accessoryCircularCapacity)
                                .tint(gauge.tint.color)
                                Text(gauge.label).font(.caption).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 12)
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                    Text("Traffic sources").font(.headline)
                    VStack(spacing: 10) {
                        ForEach(s.channels, id: \.id) { channel in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Circle().fill(channel.tint.color).frame(width: 8, height: 8)
                                    Text(channel.name).font(.subheadline)
                                    Spacer()
                                    Text(verbatim: channel.display).font(.subheadline.monospacedDigit()).foregroundStyle(.secondary)
                                }
                                ProgressView(value: channel.share).tint(channel.tint.color)
                            }
                        }
                    }
                    .padding()
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                    Text(s.updated).font(.footnote).foregroundStyle(.secondary).frame(maxWidth: .infinity)
                }
                .padding()
            }
            .navigationTitle("Analytics")
        }
    }
}
