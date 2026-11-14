import SwiftUI

// Screen 50 — System status admin (pure SwiftUI twin of bench/screens/Screen50.kt).
struct Screen50View: View {
    let vm: Screen50ViewModel
    @StateObject private var state: FlowState<Screen50State>

    init(vm: Screen50ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Image(systemName: s.bannerIcon).font(.title).foregroundStyle(s.bannerTint.color)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(s.bannerTitle).font(.headline)
                            Text(s.bannerDetail).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(s.updated).font(.caption2).monospacedDigit().foregroundStyle(.secondary)
                    }
                    .padding(14)
                    .background(s.bannerTint.color.opacity(0.14), in: RoundedRectangle(cornerRadius: 14))
                    ForEach(s.groups, id: \.id) { group in
                        GroupBox {
                            VStack(alignment: .leading, spacing: 10) {
                                ForEach(group.services, id: \.id) { service in
                                    HStack(spacing: 10) {
                                        Circle().fill(service.tint.color).frame(width: 9, height: 9)
                                        VStack(alignment: .leading, spacing: 1) {
                                            Text(service.name).font(.subheadline).fontWeight(.medium)
                                            Text(service.status).font(.caption2).foregroundStyle(service.tint.color)
                                        }
                                        Spacer()
                                        VStack(alignment: .trailing, spacing: 1) {
                                            Text(service.uptime).font(.subheadline).monospacedDigit()
                                            Text(service.latency).font(.caption2).monospacedDigit().foregroundStyle(.secondary)
                                        }
                                    }
                                }
                                Divider()
                                ForEach(group.resources, id: \.id) { resource in
                                    ProgressView(value: resource.value) {
                                        Text(resource.name).font(.caption)
                                    } currentValueLabel: {
                                        Text(resource.text).font(.caption2).monospacedDigit()
                                    }
                                    .tint(resource.tint.color)
                                }
                            }
                            .padding(.top, 6)
                        } label: {
                            HStack {
                                Label(group.title, systemImage: group.icon)
                                Spacer()
                                Text(group.region).font(.caption).monospaced().foregroundStyle(.secondary)
                            }
                        }
                    }
                    Text("Recent incidents").font(.title3).bold().padding(.top, 4)
                    VStack(spacing: 0) {
                        ForEach(s.incidents, id: \.id) { incident in
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: incident.icon)
                                    .font(.subheadline).foregroundStyle(Color.white)
                                    .frame(width: 32, height: 32)
                                    .background(incident.tint.color, in: RoundedRectangle(cornerRadius: 8))
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(incident.title).font(.subheadline).fontWeight(.semibold)
                                    Text(incident.detail).font(.caption).foregroundStyle(.secondary)
                                    Text(incident.time).font(.caption2).foregroundStyle(.tertiary)
                                }
                                Spacer()
                                Text(incident.state)
                                    .font(.caption2).fontWeight(.semibold).foregroundStyle(incident.tint.color)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(incident.tint.color.opacity(0.15), in: Capsule())
                            }
                            .padding(.vertical, 10)
                            Divider()
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("System Status")
        }
    }
}
