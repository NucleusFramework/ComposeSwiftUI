import SwiftUI

// Screen 48 — EV car dashboard (pure SwiftUI twin of bench/screens/Screen48.kt).
struct Screen48View: View {
    let vm: Screen48ViewModel
    @StateObject private var state: FlowState<Screen48State>

    init(vm: Screen48ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "car.side.fill").font(.system(size: 40)).foregroundStyle(Color.teal)
                            Spacer()
                            Label(s.status, systemImage: "bolt.fill")
                                .font(.caption).fontWeight(.semibold).foregroundStyle(Color.green)
                                .padding(.horizontal, 8).padding(.vertical, 4)
                                .background(Color.green.opacity(0.15), in: Capsule())
                        }
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Text(s.range).font(.system(size: 40, weight: .bold, design: .rounded)).monospacedDigit()
                            Text("est. range").font(.subheadline).foregroundStyle(.secondary)
                        }
                        Gauge(value: s.battery, in: 0...1) {
                            Label("Battery", systemImage: "battery.75percent")
                        } currentValueLabel: {
                            Text(s.batteryText).monospacedDigit()
                        } minimumValueLabel: {
                            Text("0")
                        } maximumValueLabel: {
                            Text("100")
                        }
                        .gaugeStyle(.linearCapacity).tint(Color.green)
                        Text(s.chargeTime).font(.footnote).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                }
                Section {
                    Grid(horizontalSpacing: 10, verticalSpacing: 10) {
                        ForEach(0..<2, id: \.self) { rowIndex in
                            GridRow {
                                ForEach(Array(s.stats.dropFirst(rowIndex * 2).prefix(2)), id: \.id) { stat in
                                    HStack(spacing: 10) {
                                        Image(systemName: stat.icon).font(.title3).foregroundStyle(stat.tint.color).frame(width: 28)
                                        VStack(alignment: .leading, spacing: 1) {
                                            Text(stat.value).font(.subheadline).bold().monospacedDigit()
                                            Text(stat.label).font(.caption2).foregroundStyle(.secondary)
                                        }
                                    }
                                    .padding(10)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(stat.tint.color.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                                }
                            }
                        }
                    }
                }
                Section {
                    Toggle("Climate control", systemImage: "thermometer.medium", isOn: Binding(get: { s.climateOn }, set: { vm.setClimate(value: $0) }))
                    Stepper(value: Binding(get: { s.cabinTemp }, set: { vm.setCabin(value: $0) }), in: 16...28) {
                        HStack {
                            Text("Cabin")
                            Spacer()
                            Text(s.cabinText).font(.headline).monospacedDigit().foregroundStyle(Color.orange)
                        }
                    }
                    Grid(horizontalSpacing: 10, verticalSpacing: 10) {
                        ForEach(0..<2, id: \.self) { rowIndex in
                            GridRow {
                                ForEach(Array(s.controls.dropFirst(rowIndex * 2).prefix(2)), id: \.id) { control in
                                    Toggle(control.title, systemImage: control.icon, isOn: Binding(get: { control.on }, set: { vm.setControl(id: control.id, value: $0) }))
                                        .toggleStyle(.button).font(.footnote).frame(maxWidth: .infinity)
                                }
                            }
                        }
                    }
                } header: {
                    Text("Climate")
                } footer: {
                    Text(s.outsideText)
                }
                Section("Charging") {
                    ProgressView(value: s.chargeLimit) {
                        HStack {
                            Text("Charge limit")
                            Spacer()
                            Text(s.chargeLimitText).foregroundStyle(.secondary)
                        }
                    }
                    .tint(Color.green)
                    LabeledContent("Charger", value: s.charger)
                    LabeledContent("Power", value: s.power)
                    LabeledContent("This session") {
                        Text(s.added).foregroundStyle(Color.green).monospacedDigit()
                    }
                }
            }
            .insetGroupedListStyle()
            .navigationTitle(s.carName)
        }
    }
}
