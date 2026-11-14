import SwiftUI

// Screen 20 — Contacts (pure SwiftUI twin of bench/screens/Screen20.kt).
struct Screen20View: View {
    let vm: Screen20ViewModel
    @StateObject private var state: FlowState<Screen20State>

    init(vm: Screen20ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                HStack(spacing: 14) {
                    ZStack {
                        Circle().fill(
                            LinearGradient(
                                colors: [Color(red: 0.55, green: 0.6, blue: 0.7), Color(red: 0.35, green: 0.4, blue: 0.5)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        Text("AL").font(.title2.weight(.semibold)).foregroundStyle(Color.white)
                    }
                    .frame(width: 56, height: 56)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(s.myName).font(.title3.weight(.semibold))
                        Text(s.myDetail).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
                ForEach(s.sections, id: \.letter) { section in
                    Section {
                        ForEach(section.contacts, id: \.id) { contact in
                            HStack(spacing: 12) {
                                Text(contact.initials)
                                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                                    .foregroundStyle(Color.white)
                                    .frame(width: 40, height: 40)
                                    .background(contact.tint.color, in: Circle())
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(contact.name).font(.body.weight(.medium))
                                    HStack(spacing: 6) {
                                        Label(contact.phone, systemImage: "phone.fill")
                                        Label(contact.email, systemImage: "envelope.fill")
                                    }
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                                if contact.favorite {
                                    Image(systemName: "star.fill").font(.caption).foregroundStyle(Color.yellow)
                                }
                            }
                        }
                    } header: {
                        Text(section.letter).font(.headline).foregroundStyle(Color.blue)
                    }
                }
                Text(s.countLabel).font(.footnote).foregroundStyle(.secondary).frame(maxWidth: .infinity)
            }
            .listStyle(.plain)
            .searchable(text: Binding(get: { s.query }, set: { vm.setQuery(value: $0) }), prompt: "Search contacts")
            .navigationTitle("Contacts")
        }
    }
}
