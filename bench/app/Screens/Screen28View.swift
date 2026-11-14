import SwiftUI

// Screen 28 — Smart home (pure SwiftUI twin of bench/screens/Screen28.kt).
struct Screen28View: View {
    let vm: Screen28ViewModel
    @StateObject private var state: FlowState<Screen28State>

    init(vm: Screen28ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(s.greeting).font(.title2.bold())
                            Text(s.summary).font(.subheadline).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "house.lodge.fill")
                            .font(.title).symbolRenderingMode(.multicolor)
                    }
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(s.scenes, id: \.id) { scene in
                                Label(scene.name, systemImage: scene.icon)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(scene.fg.color)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(scene.bg.color, in: Capsule())
                            }
                        }
                    }
                    thermostat(s)
                    Text("Devices").font(.headline)
                    Grid(horizontalSpacing: 12, verticalSpacing: 12) {
                        ForEach(s.rows, id: \.id) { row in
                            GridRow {
                                tile(row.left)
                                tile(row.right)
                            }
                        }
                    }
                }
                .padding()
            }
            .background(Color(red: 0.95, green: 0.95, blue: 0.97))
            .navigationTitle("Home")
        }
    }

    private func thermostat(_ s: Screen28State) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Thermostat", systemImage: "thermometer.medium").font(.headline).foregroundStyle(Color.orange)
                Spacer()
                Text(s.temperatureText).font(.system(size: 28, weight: .semibold, design: .rounded)).monospacedDigit()
            }
            Slider(value: Binding(get: { s.temperature }, set: { vm.setTemperature(value: $0) }), in: 16...28, step: 0.5)
                .tint(Color.orange)
            HStack {
                Text("16°").font(.caption2).foregroundStyle(.secondary)
                Spacer()
                Text(verbatim: s.humidity).font(.caption2).foregroundStyle(.secondary)
                Spacer()
                Text("28°").font(.caption2).foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 18))
    }

    private func tile(_ device: Screen28Device) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: device.icon)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color.white)
                    .frame(width: 40, height: 40)
                    .background(device.iconBg.color, in: Circle())
                Spacer()
                Toggle(device.name, isOn: Binding(get: { device.on }, set: { vm.setDevice(id: device.id, on: $0) }))
                    .labelsHidden().toggleStyle(.switch).tint(device.tint.color)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(device.name).font(.subheadline).fontWeight(.semibold).lineLimit(1)
                Text(device.room).font(.caption2).foregroundStyle(.secondary)
            }
            Text(verbatim: device.status)
                .font(.caption).fontWeight(.medium).foregroundStyle(device.on ? device.tint.color : Color.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(device.tileBg.color, in: RoundedRectangle(cornerRadius: 18))
        .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
    }
}
