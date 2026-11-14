import SwiftUI

// Screen 02 — Mail inbox (pure SwiftUI twin of bench/screens/Screen02.kt).
struct Screen02View: View {
    let vm: Screen02ViewModel
    @StateObject private var state: FlowState<Screen02State>

    init(vm: Screen02ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section("Mailboxes") {
                    ForEach(s.mailboxes, id: \.id) { box in
                        HStack(spacing: 12) {
                            Image(systemName: box.icon).foregroundStyle(box.tint.color).frame(width: 26)
                            Text(box.title)
                        }
                        .badge(Int(box.count))
                    }
                }
                Section {
                    ForEach(s.messages, id: \.id) { msg in
                        HStack(alignment: .top, spacing: 10) {
                            Circle()
                                .fill(msg.unread ? Color.blue : Color.clear)
                                .frame(width: 10, height: 10)
                                .padding(.top, 6)
                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 6) {
                                    Text(msg.sender).font(.headline).lineLimit(1)
                                    if msg.flagged {
                                        Image(systemName: "flag.fill").font(.caption).foregroundStyle(Color.orange)
                                    }
                                    Spacer()
                                    if msg.attachment {
                                        Image(systemName: "paperclip").font(.caption).foregroundStyle(.secondary)
                                    }
                                    Text(msg.time).font(.subheadline).foregroundStyle(.secondary)
                                    Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
                                }
                                Text(msg.subject).font(.subheadline).fontWeight(msg.unread ? .semibold : .regular).lineLimit(1)
                                Text(msg.preview).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                } header: {
                    HStack {
                        Text("Today")
                        Spacer()
                        Text(s.unreadSummary).foregroundStyle(Color.blue)
                    }
                } footer: {
                    Text(s.updated).font(.caption).frame(maxWidth: .infinity)
                }
            }
            .listStyle(.plain)
            .navigationTitle("Mail")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Compose", systemImage: "square.and.pencil", action: {})
                }
            }
        }
    }
}
