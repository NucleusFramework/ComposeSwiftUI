import SwiftUI

// Screen 10 — Preferences (pure SwiftUI twin of bench/screens/Screen10.kt).
struct Screen10View: View {
    let vm: Screen10ViewModel
    @StateObject private var state: FlowState<Screen10State>

    init(vm: Screen10ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            Form {
                Section("Appearance") {
                    Picker("Theme", selection: Binding(get: { s.theme }, set: { vm.setTheme(value: $0) })) {
                        ForEach(s.themes, id: \.id) { option in
                            Label(option.label, systemImage: option.icon).tag(option.id)
                        }
                    }
                    .pickerStyle(.segmented)
                    Picker("Accent color", selection: Binding(get: { s.accent }, set: { vm.setAccent(value: $0) })) {
                        ForEach(s.accents, id: \.id) { option in
                            Text(option.label).tag(option.id)
                        }
                    }
                    .pickerStyle(.menu)
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("Text size")
                            Spacer()
                            Text(s.textSizeLabel).foregroundStyle(.secondary)
                        }
                        Slider(value: Binding(get: { s.textSize }, set: { vm.setTextSize(value: $0) }), in: 0...1) {
                            Text("Text size")
                        } minimumValueLabel: {
                            Image(systemName: "textformat.size.smaller")
                        } maximumValueLabel: {
                            Image(systemName: "textformat.size.larger")
                        }
                    }
                    LabeledContent("Preview") {
                        Text("Aa").font(.system(size: s.previewSize, weight: .semibold)).foregroundStyle(s.accentTint.color)
                    }
                }
                Section {
                    Picker("Streaming quality", selection: Binding(get: { s.quality }, set: { vm.setQuality(value: $0) })) {
                        ForEach(s.qualities, id: \.id) { option in
                            Label(option.label, systemImage: option.icon).tag(option.id)
                        }
                    }
                    .pickerStyle(.menu)
                    HStack(spacing: 10) {
                        Image(systemName: "speaker.fill").foregroundStyle(.secondary)
                        Slider(value: Binding(get: { s.volume }, set: { vm.setVolume(value: $0) }), in: 0...1)
                        Text(s.volumeLabel).monospacedDigit().frame(width: 32, alignment: .trailing)
                    }
                    Toggle("Autoplay", systemImage: "play.circle", isOn: Binding(get: { s.autoplay }, set: { vm.setAutoplay(value: $0) }))
                    LabeledContent("Downloaded cache", value: s.cache)
                } header: {
                    Text("Playback")
                } footer: {
                    Text("Lossless streaming uses significantly more data.")
                }
                Section("Notifications") {
                    Picker("Alert sound", systemImage: "bell.badge", selection: Binding(get: { s.sound }, set: { vm.setSound(value: $0) })) {
                        ForEach(s.sounds, id: \.id) { option in
                            Text(option.label).tag(option.id)
                        }
                    }
                    LabeledContent("Quiet hours", value: s.quietHours)
                }
                Section("About") {
                    LabeledContent("Version", value: s.version)
                    LabeledContent("License") {
                        Label("Pro", systemImage: "checkmark.seal.fill").foregroundStyle(Color.green)
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle("Preferences")
            .tint(s.accentTint.color)
        }
    }
}
