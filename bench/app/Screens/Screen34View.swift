import SwiftUI

// Screen 34 — Video library (pure SwiftUI twin of bench/screens/Screen34.kt).
struct Screen34View: View {
    let vm: Screen34ViewModel
    @StateObject private var state: FlowState<Screen34State>

    init(vm: Screen34ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    @ViewBuilder private func thumb(_ video: Screen34Video, wide: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .bottomLeading) {
                Rectangle().fill(LinearGradient(colors: [video.from.color, video.to.color], startPoint: .topLeading, endPoint: .bottomTrailing))
                Image(systemName: video.icon)
                    .font(.system(size: wide ? 40 : 34)).foregroundStyle(Color.white.opacity(0.85))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                HStack {
                    if video.isNew {
                        Text("NEW").font(.caption2).bold().foregroundStyle(Color.white).padding(.horizontal, 6).padding(.vertical, 2).background(Color.red, in: Capsule())
                    }
                    Spacer()
                    Text(verbatim: video.duration).font(.caption2).monospacedDigit().foregroundStyle(Color.white).padding(.horizontal, 5).padding(.vertical, 2).background(Color.black.opacity(0.5), in: RoundedRectangle(cornerRadius: 4))
                }
                .padding(8)
                if video.progress > 0.0 {
                    ProgressView(value: video.progress).tint(Color.white).padding(.horizontal, 8).offset(y: 6)
                }
            }
            .frame(width: wide ? 220 : 120, height: wide ? 124 : 170)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            Text(verbatim: video.title).font(.subheadline).fontWeight(.medium).lineLimit(1)
            Text(verbatim: video.meta).font(.caption).foregroundStyle(.secondary).lineLimit(1)
        }
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    ZStack(alignment: .bottomLeading) {
                        Rectangle().fill(LinearGradient(colors: [Color.indigo, Color.purple, Color.pink], startPoint: .topLeading, endPoint: .bottomTrailing))
                        Image(systemName: "sparkles").font(.system(size: 90)).foregroundStyle(Color.white.opacity(0.25)).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing).padding(16)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("FEATURED").font(.caption).bold().kerning(1.5).foregroundStyle(Color.white.opacity(0.8))
                            Text(verbatim: s.featuredTitle).font(.title).bold().foregroundStyle(Color.white)
                            HStack(spacing: 10) {
                                Text(verbatim: s.featuredMeta).font(.subheadline).foregroundStyle(Color.white.opacity(0.9))
                                Label("Play", systemImage: "play.fill").font(.caption).bold().foregroundStyle(Color.black).padding(.horizontal, 10).padding(.vertical, 5).background(Color.white, in: Capsule())
                            }
                        }
                        .padding(18)
                    }
                    .frame(height: 190)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .padding(.horizontal)
                    ForEach(s.shelves, id: \.id) { shelf in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .firstTextBaseline) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(verbatim: shelf.title).font(.title3).bold()
                                    Text(verbatim: shelf.subtitle).font(.footnote).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").font(.subheadline.weight(.semibold)).foregroundStyle(.tertiary)
                            }
                            .padding(.horizontal)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(alignment: .top, spacing: 12) {
                                    ForEach(shelf.videos, id: \.id) { video in
                                        thumb(video, wide: shelf.wide)
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Search", systemImage: "magnifyingglass") {}
                }
            }
        }
    }
}
