import SwiftUI

// Screen 04 — Music player (pure SwiftUI twin of bench/screens/Screen04.kt).
struct Screen04View: View {
    let vm: Screen04ViewModel
    @StateObject private var state: FlowState<Screen04State>

    init(vm: Screen04ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            VStack(spacing: 22) {
                ZStack {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(LinearGradient(colors: [s.artFrom.color, s.artTo.color], startPoint: .topLeading, endPoint: .bottomTrailing))
                    Circle().stroke(Color.white.opacity(0.25), lineWidth: 2).frame(width: 170, height: 170)
                    Image(systemName: "music.note").font(.system(size: 84, weight: .semibold)).foregroundStyle(Color.white.opacity(0.9))
                    Text(s.album).font(.caption.weight(.semibold)).foregroundStyle(Color.white.opacity(0.8))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                        .padding(16)
                }
                .frame(width: 260, height: 260)
                .shadow(color: Color.black.opacity(0.25), radius: 16, y: 8)
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(s.title).font(.title2.bold()).lineLimit(1)
                        Text(s.artist).font(.title3).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: s.liked ? "heart.fill" : "heart").font(.title2).foregroundStyle(Color.pink)
                }
                VStack(spacing: 6) {
                    Slider(value: Binding(get: { s.progress }, set: { vm.setProgress(value: $0) }), in: 0...1).tint(Color.primary)
                    HStack {
                        Text(s.elapsed)
                        Spacer()
                        Text(s.remaining)
                    }
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
                }
                HStack(spacing: 44) {
                    Image(systemName: "shuffle").font(.body).foregroundStyle(s.shuffle ? Color.pink : Color.secondary)
                    Image(systemName: "backward.fill").font(.title)
                    Image(systemName: s.playing ? "pause.circle.fill" : "play.circle.fill").font(.system(size: 64)).symbolRenderingMode(.hierarchical)
                    Image(systemName: "forward.fill").font(.title)
                    Image(systemName: "repeat").font(.body).foregroundStyle(.secondary)
                }
                HStack(spacing: 12) {
                    Image(systemName: "speaker.fill").font(.caption)
                    Slider(value: Binding(get: { s.volume }, set: { vm.setVolume(value: $0) }), in: 0...1).tint(Color.gray)
                    Image(systemName: "speaker.wave.3.fill").font(.caption)
                }
                .foregroundStyle(.secondary)
                HStack {
                    ForEach(s.actions, id: \.id) { action in
                        VStack(spacing: 4) {
                            Image(systemName: action.icon).font(.title3)
                            Text(action.label).font(.caption2)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                Text(s.upNext).font(.footnote).foregroundStyle(.secondary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.gray.opacity(0.15), in: Capsule())
                Spacer()
            }
            .padding(.horizontal, 28)
            .padding(.top, 12)
            .navigationTitle("Now Playing")
            .toolbarTitleDisplayMode(.inline)
        }
    }
}
