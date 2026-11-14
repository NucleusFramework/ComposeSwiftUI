import SwiftUI

// Screen 08 — Chat conversation (pure SwiftUI twin of bench/screens/Screen08.kt).
struct Screen08View: View {
    let vm: Screen08ViewModel
    @StateObject private var state: FlowState<Screen08State>

    init(vm: Screen08ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(spacing: 8) {
                    VStack(spacing: 4) {
                        Text(s.initials)
                            .font(.title3.bold())
                            .foregroundStyle(Color.white)
                            .frame(width: 56, height: 56)
                            .background(LinearGradient(colors: [Color.teal, Color.blue], startPoint: .top, endPoint: .bottom), in: Circle())
                        Text(s.contact).font(.headline)
                        Text(s.status).font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(.bottom, 6)
                    Text(s.dayLabel).font(.caption2.weight(.medium)).foregroundStyle(.secondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.gray.opacity(0.12), in: Capsule())
                    ForEach(s.messages, id: \.id) { msg in
                        HStack {
                            if msg.mine {
                                Spacer(minLength: 50)
                            }
                            VStack(alignment: msg.mine ? .trailing : .leading, spacing: 3) {
                                Text(msg.text)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 9)
                                    .foregroundStyle(msg.mine ? Color.white : Color.primary)
                                    .background(msg.mine ? Color.blue : Color.gray.opacity(0.18), in: RoundedRectangle(cornerRadius: 18))
                                HStack(spacing: 3) {
                                    Text(msg.time)
                                    if msg.mine {
                                        Image(systemName: msg.read ? "checkmark.circle.fill" : "checkmark.circle")
                                            .foregroundStyle(msg.read ? Color.blue : Color.gray)
                                    }
                                }
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            }
                            if !msg.mine {
                                Spacer(minLength: 50)
                            }
                        }
                    }
                    if s.typing {
                        HStack(spacing: 4) {
                            ForEach(0..<3, id: \.self) { i in
                                Circle().fill(Color.gray.opacity(0.4 + 0.2 * Double(i))).frame(width: 7, height: 7)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color.gray.opacity(0.18), in: Capsule())
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding()
            }
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 10) {
                    Image(systemName: "plus.circle.fill").font(.title2).foregroundStyle(Color.gray)
                    TextField("Message", text: Binding(get: { s.draft }, set: { vm.setDraft(value: $0) }))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.gray.opacity(0.15), in: Capsule())
                    Image(systemName: "arrow.up.circle.fill").font(.title2).foregroundStyle(Color.blue)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.gray.opacity(0.12))
            }
            .navigationTitle(s.contact)
            .toolbarTitleDisplayMode(.inline)
        }
    }
}
