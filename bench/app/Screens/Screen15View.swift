import SwiftUI

// Screen 15 — File browser (pure SwiftUI twin of bench/screens/Screen15.kt).
struct Screen15View: View {
    let vm: Screen15ViewModel
    @StateObject private var state: FlowState<Screen15State>

    init(vm: Screen15ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section("Locations") {
                    ForEach(s.locations, id: \.id) { location in
                        HStack(spacing: 12) {
                            Image(systemName: location.icon)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(location.tint.color)
                                .frame(width: 32, height: 32)
                                .background(location.tint.color.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(location.name).font(.subheadline.weight(.semibold))
                                    Spacer()
                                    Text(location.usage).font(.caption).monospacedDigit().foregroundStyle(.secondary)
                                }
                                ProgressView(value: location.fraction).tint(location.tint.color)
                            }
                        }
                    }
                }
                Section {
                    ForEach(s.folders, id: \.id) { folder in
                        DisclosureGroup(isExpanded: Binding(get: { folder.expanded }, set: { vm.setFolderExpanded(id: folder.id, value: $0) })) {
                            ForEach(folder.subFolders, id: \.id) { sub in
                                DisclosureGroup(isExpanded: Binding(get: { sub.expanded }, set: { vm.setSubFolderExpanded(id: sub.id, value: $0) })) {
                                    ForEach(sub.files, id: \.id) { file in
                                        Screen15FileRow(file: file)
                                    }
                                } label: {
                                    HStack {
                                        Label(sub.name, systemImage: "folder").font(.subheadline)
                                        Spacer()
                                        Text(sub.count).font(.caption).foregroundStyle(.tertiary)
                                    }
                                }
                            }
                            ForEach(folder.files, id: \.id) { file in
                                Screen15FileRow(file: file)
                            }
                        } label: {
                            HStack {
                                Label {
                                    Text(folder.name)
                                } icon: {
                                    Image(systemName: folder.icon).foregroundStyle(folder.tint.color)
                                }
                                .font(.headline)
                                Spacer()
                                Text(folder.count)
                                    .font(.caption.weight(.medium))
                                    .foregroundStyle(.secondary)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Color.gray.opacity(0.15), in: Capsule())
                            }
                        }
                    }
                } header: {
                    Text("Folders")
                } footer: {
                    Text(s.footer)
                }
            }
            .navigationTitle("Browse")
        }
    }
}

private struct Screen15FileRow: View {
    let file: Screen15File

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: file.icon).font(.title3).foregroundStyle(file.tint.color).frame(width: 28)
            VStack(alignment: .leading, spacing: 1) {
                Text(file.name).font(.subheadline).lineLimit(1).truncationMode(.middle)
                Text(file.date).font(.caption2).foregroundStyle(.secondary)
            }
            Spacer()
            Text(file.size).font(.caption).monospacedDigit().foregroundStyle(.secondary)
        }
    }
}
