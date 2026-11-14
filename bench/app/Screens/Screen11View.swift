import SwiftUI

// Screen 11 — Photo grid (pure SwiftUI twin of bench/screens/Screen11.kt).
struct Screen11View: View {
    let vm: Screen11ViewModel
    @StateObject private var state: FlowState<Screen11State>

    init(vm: Screen11ViewModel) {
        self.vm = vm
        _state = StateObject(wrappedValue: FlowState(vm.state))
    }

    var body: some View {
        let s = state.value
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 8) {
                        ForEach(s.albums, id: \.id) { album in
                            HStack(spacing: 4) {
                                Image(systemName: album.icon)
                                Text(album.title)
                                Text(album.count).font(.caption).opacity(0.7)
                            }
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(album.id == s.activeAlbum ? Color.white : Color.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(album.id == s.activeAlbum ? Color.blue : Color.gray.opacity(0.15), in: Capsule())
                        }
                    }
                    .padding(.horizontal, 16)
                    Text(s.selectionLabel).font(.headline).padding(.horizontal, 16)
                    LazyVGrid(columns: [GridItem(spacing: 2), GridItem(spacing: 2), GridItem(spacing: 2)], spacing: 2) {
                        ForEach(s.tiles, id: \.id) { tile in
                            Rectangle()
                                .fill(LinearGradient(colors: [tile.from.color, tile.to.color], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .aspectRatio(1, contentMode: .fit)
                                .overlay {
                                    Image(systemName: tile.icon).font(.title).foregroundStyle(Color.white.opacity(0.85))
                                }
                                .overlay(alignment: .topTrailing) {
                                    ZStack {
                                        Circle()
                                            .fill(tile.selected ? Color.blue : Color.black.opacity(0.15))
                                            .overlay(Circle().stroke(Color.white, lineWidth: 1.5))
                                        if tile.selected {
                                            Image(systemName: "checkmark").font(.caption2.bold()).foregroundStyle(Color.white)
                                        }
                                    }
                                    .frame(width: 22, height: 22)
                                    .padding(6)
                                }
                                .overlay(alignment: .bottomLeading) {
                                    HStack(spacing: 4) {
                                        if tile.favorite {
                                            Image(systemName: "heart.fill")
                                        }
                                        if !tile.duration.isEmpty {
                                            Image(systemName: "video.fill")
                                            Text(tile.duration)
                                        }
                                    }
                                    .font(.caption2.bold())
                                    .foregroundStyle(Color.white)
                                    .padding(6)
                                }
                                .opacity(tile.selected ? 0.75 : 1.0)
                        }
                    }
                    Text(s.footer).font(.footnote).foregroundStyle(.secondary).frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Select", action: {})
                }
            }
        }
    }
}
