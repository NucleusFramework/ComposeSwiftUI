import SwiftUI

// Screen 29 — Podcast episodes (pure SwiftUI twin of bench/screens/Screen29.kt).
struct Screen29View: View {
    let vm: Screen29ViewModel
    @StateObject private var state: FlowState<Screen29State>

    init(vm: Screen29ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            List {
                Section {
                    header(s)
                }
                Section {
                    ForEach(s.episodes, id: \.id) { ep in
                        row(ep)
                    }
                } header: {
                    Text("Latest Episodes")
                }
            }
            .listStyle(.plain)
            .navigationTitle("Podcasts")
        }
    }

    private func header(_ s: Screen29State) -> some View {
        HStack(spacing: 14) {
            Image(systemName: "waveform.and.mic")
                .font(.system(size: 38, weight: .semibold))
                .foregroundStyle(Color.white)
                .frame(width: 96, height: 96)
                .background(
                    LinearGradient(colors: [Color.purple, Color.indigo], startPoint: .top, endPoint: .bottom),
                    in: RoundedRectangle(cornerRadius: 18)
                )
            VStack(alignment: .leading, spacing: 4) {
                Text(s.show).font(.title3.bold())
                Text(s.author).font(.subheadline).foregroundStyle(Color.purple)
                Text(s.meta).font(.caption).foregroundStyle(.secondary)
                if s.subscribed {
                    Button("Following", systemImage: "checkmark", action: {})
                        .buttonStyle(.bordered).controlSize(.small).tint(Color.purple)
                } else {
                    Button("Follow", systemImage: "plus", action: {})
                        .buttonStyle(.borderedProminent).controlSize(.small).tint(Color.purple)
                }
            }
        }
    }

    private func row(_ ep: Screen29Episode) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(LinearGradient(colors: [ep.top.color, ep.bottom.color], startPoint: .topLeading, endPoint: .bottomTrailing))
                Image(systemName: ep.symbol).font(.title2).foregroundStyle(Color.white).shadow(radius: 2)
            }
            .frame(width: 60, height: 60)
            VStack(alignment: .leading, spacing: 4) {
                Text(ep.date).font(.caption2).fontWeight(.semibold).textCase(.uppercase).foregroundStyle(.secondary)
                Text(ep.title).font(.subheadline).fontWeight(.semibold).lineLimit(2).foregroundStyle(ep.played ? Color.secondary : Color.primary)
                Text(ep.summary).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                HStack(spacing: 6) {
                    Image(systemName: ep.played ? "checkmark.circle" : "play.circle.fill").font(.title3).foregroundStyle(Color.purple)
                    Text(ep.duration).font(.caption.monospacedDigit()).fontWeight(.medium)
                    if ep.progress > 0 && !ep.played {
                        ProgressView(value: ep.progress).frame(width: 54).tint(Color.purple)
                    }
                    Spacer()
                    Image(systemName: ep.downloadIcon).font(.body).foregroundStyle(ep.downloadTint.color)
                    Menu {
                        Button("Play Next", systemImage: "text.line.first.and.arrowtriangle.forward", action: {})
                        Button("Mark as Played", systemImage: "checkmark.circle", action: {})
                        Button("Share Episode", systemImage: "square.and.arrow.up", action: {})
                        Divider()
                        Button("Remove Download", systemImage: "trash", role: .destructive, action: {})
                    } label: {
                        Image(systemName: "ellipsis").font(.body).frame(width: 26, height: 26).background(Color.gray.opacity(0.12), in: Circle())
                    }
                    .menuIndicator(.hidden).fixedSize()
                }
            }
        }
    }
}
