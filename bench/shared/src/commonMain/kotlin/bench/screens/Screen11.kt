package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 11 — Photo grid: album chips, 3-column grid of gradient tiles with symbols, selection badges, video durations.

data class Screen11Tile(
    val id: Int,
    val from: Rgb,
    val to: Rgb,
    val icon: String,
    val selected: Boolean,
    val duration: String,
    val favorite: Boolean,
)

data class Screen11Album(val id: Int, val title: String, val icon: String, val count: String)

data class Screen11State(
    val albums: List<Screen11Album>,
    val activeAlbum: Int,
    val tiles: List<Screen11Tile>,
    val selectionLabel: String,
    val footer: String,
)

private val screen11Icons = listOf("mountain.2.fill", "sun.horizon.fill", "leaf.fill", "building.2.fill", "pawprint.fill", "camera.macro", "airplane", "fork.knife", "figure.walk")

private val screen11Palette = listOf(
    Rgb(0.98, 0.45, 0.4) to Rgb(0.95, 0.75, 0.35),
    Rgb(0.3, 0.6, 0.95) to Rgb(0.2, 0.3, 0.75),
    Rgb(0.3, 0.8, 0.55) to Rgb(0.1, 0.5, 0.45),
    Rgb(0.75, 0.45, 0.95) to Rgb(0.95, 0.45, 0.7),
    Rgb(0.95, 0.8, 0.3) to Rgb(0.95, 0.5, 0.15),
    Rgb(0.45, 0.5, 0.6) to Rgb(0.2, 0.22, 0.3),
)

private fun screen11Tiles(selected: Set<Int>): List<Screen11Tile> = (0 until 21).map { i ->
    val (from, to) = screen11Palette[(i * 5) % screen11Palette.size]
    Screen11Tile(
        id = i,
        from = from,
        to = to,
        icon = screen11Icons[(i * 4) % screen11Icons.size],
        selected = i in selected,
        duration = if (i % 4 == 2) "0:${(10 + i * 3).toString().padStart(2, '0')}" else "",
        favorite = i % 5 == 1,
    )
}

class Screen11ViewModel : BenchViewModel() {
    private var tick = 0
    private var selected = setOf(1, 4, 9)
    private val _state = MutableStateFlow(
        Screen11State(
            albums = listOf(
                Screen11Album(1, "Recents", "clock", "1,284"),
                Screen11Album(2, "Favorites", "heart.fill", "86"),
                Screen11Album(3, "Videos", "video.fill", "142"),
            ),
            activeAlbum = 1,
            tiles = screen11Tiles(selected),
            selectionLabel = "3 Photos Selected",
            footer = "1,284 Photos, 142 Videos",
        )
    )
    val state: StateFlow<Screen11State> = _state

    fun toggle(id: Int) {
        selected = if (id in selected) selected - id else selected + id
        _state.update { it.copy(tiles = screen11Tiles(selected), selectionLabel = "${selected.size} Photos Selected") }
    }

    override fun mutate() {
        tick++
        val id = (tick * 7) % 21
        selected = if (id in selected) selected - id else selected + id
        _state.update {
            it.copy(tiles = screen11Tiles(selected), selectionLabel = "${selected.size} Photos Selected", activeAlbum = 1 + tick % 3)
        }
    }
}

@Composable
fun Screen11(vm: Screen11ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(
            modifier = Modifier.navigationTitle("Library")
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) {
                    Button(title = "Select", action = {})
                },
        ) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) {
                HStack(spacing = 8.0, modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 16.0)) {
                    for (album in state.albums) key(album.id) {
                        HStack(spacing = 4.0, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium))
                            .foregroundStyle(if (album.id == state.activeAlbum) Color.white else Color.primary)
                            .padding(edges = Edge.Set.horizontal, length = 12.0)
                            .padding(edges = Edge.Set.vertical, length = 7.0)
                            .background(if (album.id == state.activeAlbum) Color.blue else Color.gray.opacity(0.15), Capsule())) {
                            Image(systemName = album.icon)
                            Text(album.title)
                            Text(album.count, modifier = Modifier.font(Font.caption).opacity(0.7))
                        }
                    }
                }
                Text(state.selectionLabel, modifier = Modifier.font(Font.headline).padding(edges = Edge.Set.horizontal, length = 16.0))
                LazyVGrid(columns = listOf(GridItem(spacing = 2.0), GridItem(spacing = 2.0), GridItem(spacing = 2.0)), spacing = 2.0) {
                    for (tile in state.tiles) key(tile.id) {
                        ShapeView(
                            Rectangle(),
                            fill = LinearGradient(colors = listOf(tile.from.color(), tile.to.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                            modifier = Modifier.aspectRatio(1.0, contentMode = ContentMode.fit)
                                .overlay {
                                    Image(systemName = tile.icon, modifier = Modifier.font(Font.title).foregroundStyle(Color.white.opacity(0.85)))
                                }
                                .overlay(alignment = Alignment.topTrailing) {
                                    ZStack(modifier = Modifier.frame(width = 22.0, height = 22.0).padding(6.0)) {
                                        ShapeView(Circle(), fill = if (tile.selected) Color.blue else Color.black.opacity(0.15), stroke = Color.white, lineWidth = 1.5)
                                        if (tile.selected) {
                                            Image(systemName = "checkmark", modifier = Modifier.font(Font.caption2.bold()).foregroundStyle(Color.white))
                                        }
                                    }
                                }
                                .overlay(alignment = Alignment.bottomLeading) {
                                    HStack(spacing = 4.0, modifier = Modifier.font(Font.caption2.bold()).foregroundStyle(Color.white).padding(6.0)) {
                                        if (tile.favorite) {
                                            Image(systemName = "heart.fill")
                                        }
                                        if (tile.duration.isNotEmpty()) {
                                            Image(systemName = "video.fill")
                                            Text(tile.duration)
                                        }
                                    }
                                }
                                .opacity(if (tile.selected) 0.75 else 1.0),
                        )
                    }
                }
                Text(state.footer, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary).frame(maxWidth = Double.POSITIVE_INFINITY))
            }
        }
    }
}

val screen11 = BenchScreen("11 · Photo grid", ::Screen11ViewModel) { Screen11(it as Screen11ViewModel) }
