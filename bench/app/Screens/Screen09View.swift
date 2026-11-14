import SwiftUI

// Screen 09 — Sign-up form (pure SwiftUI twin of bench/screens/Screen09.kt).
struct Screen09View: View {
    let vm: Screen09ViewModel
    @StateObject private var state: FlowState<Screen09State>

    init(vm: Screen09ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            Form {
                Section {
                    TextField("Full name", text: Binding(get: { s.name }, set: { vm.setName(value: $0) }))
                    TextField("Email", text: Binding(get: { s.email }, set: { vm.setEmail(value: $0) })).autocorrectionDisabled()
                    SecureField("Password", text: Binding(get: { s.password }, set: { vm.setPassword(value: $0) }))
                    SecureField("Confirm password", text: Binding(get: { s.confirm }, set: { vm.setConfirm(value: $0) }))
                    HStack(spacing: 6) {
                        Text("Strength").font(.footnote).foregroundStyle(.secondary)
                        ForEach(s.strengthBars, id: \.id) { bar in
                            Capsule().fill(bar.tint.color).frame(height: 6)
                        }
                        Text(s.strengthLabel).font(.footnote.bold()).foregroundStyle(s.strengthTint.color).frame(width: 54, alignment: .trailing)
                    }
                } header: {
                    Text("Account")
                }
                Section {
                    Picker("Country", selection: Binding(get: { s.country }, set: { vm.setCountry(value: $0) })) {
                        ForEach(s.countries, id: \.id) { option in
                            Text(option.label).tag(option.id)
                        }
                    }
                    Picker("Role", selection: Binding(get: { s.role }, set: { vm.setRole(value: $0) })) {
                        ForEach(s.roles, id: \.id) { option in
                            Text(option.label).tag(option.id)
                        }
                    }
                    .pickerStyle(.segmented)
                    Stepper(value: Binding(get: { Int(s.age) }, set: { vm.setAge(value: Int32($0)) }), in: 13...99) {
                        Text(s.ageLabel)
                    }
                    Toggle("Product newsletter", systemImage: "envelope.badge", isOn: Binding(get: { s.newsletter }, set: { vm.setNewsletter(value: $0) }))
                } header: {
                    Text("Profile")
                }
                Section {
                    LabeledContent("Plan", value: s.plan)
                    LabeledContent("Referral code") {
                        Text(s.referral).font(.body.monospaced()).foregroundStyle(Color.purple)
                    }
                    Toggle("I accept the terms", isOn: Binding(get: { s.terms }, set: { vm.setTerms(value: $0) }))
                } header: {
                    Text("Plan")
                } footer: {
                    Text(s.footer)
                }
                Section {
                    Button(action: {}) {
                        Text("Create Account").font(.headline).frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!s.terms)
                }
            }
            .formStyle(.grouped)
            .navigationTitle("Sign Up")
        }
    }
}
