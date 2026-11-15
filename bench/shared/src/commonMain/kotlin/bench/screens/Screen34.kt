package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 34 — Video library: featured banner and several horizontal carousels of gradient thumbnails.

data class Screen34Video(
    val id: Int,
    val title: String,
    val meta: String,
    val duration: String,
    val icon: String,
    val from: Rgb,
    val to: Rgb,
    val progress: Double,
    val isNew: Boolean,
)

data class Screen34Shelf(val id: Int, val title: String, val subtitle: String, val wide: Boolean, val videos: List<Screen34Video>)

data class Screen34State(
    val featuredTitle: String,
    val featuredMeta: String,
    val shelves: List<Screen34Shelf>,
)

private val screen34Palette = listOf(
    Rgb(0.98, 0.36, 0.35) to Rgb(0.6, 0.1, 0.45),
    Rgb(0.2, 0.6, 1.0) to Rgb(0.1, 0.2, 0.6),
    Rgb(0.3, 0.85, 0.6) to Rgb(0.0, 0.45, 0.45),
    Rgb(1.0, 0.75, 0.2) to Rgb(0.9, 0.35, 0.1),
    Rgb(0.7, 0.45, 1.0) to Rgb(0.3, 0.15, 0.65),
    Rgb(0.45, 0.45, 0.5) to Rgb(0.15, 0.15, 0.2),
)
private val screen34Icons = listOf("mountain.2.fill", "film.fill", "leaf.fill", "sun.max.fill", "sparkles", "music.note")

private fun screen34Shelf(id: Int, title: String, subtitle: String, wide: Boolean, names: List<String>, watched: Int): Screen34Shelf {
    val videos = names.mapIndexed { i, name ->
        val n = id * 10 + i
        val (from, to) = screen34Palette[(id + i) % screen34Palette.size]
        Screen34Video(
            id = n,
            title = name,
            meta = "${2018 + (n % 7)} · ${listOf("Documentary", "Drama", "Nature", "Travel", "Music")[n % 5]}",
            duration = "${20 + (n * 7) % 90}:${((n * 13) % 60).toString().padStart(2, '0')}",
            icon = screen34Icons[(id + i) % screen34Icons.size],
            from = from,
            to = to,
            progress = if (i < watched) (0.2 + 0.15 * i) else 0.0,
            isNew = (n % 4) == 1,
        )
    }
    return Screen34Shelf(id, title, subtitle, wide, videos)
}

class Screen34ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen34State(
            featuredTitle = "Northern Lights",
            featuredMeta = "New season · 8 episodes",
            shelves = listOf(
                screen34Shelf(1, "Continue Watching", "Pick up where you left off", true, listOf("Alpine Trails", "City at Night", "Deep Blue", "Desert Bloom"), 4),
                screen34Shelf(2, "Trending Now", "Most watched this week", false, listOf("Wild Coast", "Neon", "Forest Song", "Solar", "Aurora", "Echoes"), 0),
                screen34Shelf(3, "Because you watched Deep Blue", "Ocean & nature", false, listOf("Reef Life", "Tides", "Whale Road", "Kelp", "Icebound"), 0),
                screen34Shelf(4, "Short Films", "Under 30 minutes", true, listOf("Paper Moon", "Stillness", "Last Train"), 0),
            ),
        )
    )
    val state: StateFlow<Screen34State> = _state
    private var tick = 0

    override fun mutate() = _state.update { s ->
        tick++
        val first = s.shelves[0]
        val videos = first.videos.mapIndexed { i, v ->
            if (i == tick % first.videos.size) v.copy(progress = ((v.progress * 100).toInt() + 5) % 100 / 100.0) else v
        }
        s.copy(
            featuredMeta = "New season · ${8 + tick % 3} episodes",
            shelves = listOf(first.copy(videos = videos)) + s.shelves.drop(1),
        )
    }
}

@Composable
private fun Screen34Thumb(video: Screen34Video, wide: Boolean) {
    VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
        ZStack(
            modifier = Modifier.frame(width = if (wide) 220.0 else 120.0, height = if (wide) 124.0 else 170.0)
                .clipShape(RoundedRectangle(cornerRadius = 12.0)),
            alignment = Alignment.bottomLeading,
        ) {
            ShapeView(Rectangle(), fill = LinearGradient(colors = listOf(video.from.color(), video.to.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing))
            Image(systemName = 
                SFSymbol(video.icon),
                modifier = Modifier.font(Font.system(size = if (wide) 40.0 else 34.0)).foregroundStyle(Color.white.opacity(0.85))
                    .frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY),
            )
            HStack(modifier = Modifier.padding(8.0)) {
                if (video.isNew) {
                    Text("NEW", modifier = Modifier.font(Font.caption2).bold().foregroundStyle(Color.white).padding(Edge.Set.horizontal, 6.0).padding(Edge.Set.vertical, 2.0).background(Color.red, Capsule()))
                }
                Spacer()
                Text(verbatim = video.duration, modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(Color.white).padding(Edge.Set.horizontal, 5.0).padding(Edge.Set.vertical, 2.0).background(Color.black.opacity(0.5), RoundedRectangle(cornerRadius = 4.0)))
            }
            if (video.progress > 0.0) {
                ProgressView(value = video.progress, modifier = Modifier.tint(Color.white).padding(Edge.Set.horizontal, 8.0).offset(y = 6.0))
            }
        }
        Text(verbatim = video.title, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.medium).lineLimit(1))
        Text(verbatim = video.meta, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
    }
}

@Composable
fun Screen34(vm: Screen34ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(
            modifier = Modifier.navigationTitle("Library")
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) { Button(title = "Search", systemImage = SFSymbol.magnifyingglass, action = {}) },
        ) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 24.0) {
                ZStack(
                    modifier = Modifier.frame(height = 190.0).clipShape(RoundedRectangle(cornerRadius = 20.0)).padding(Edge.Set.horizontal),
                    alignment = Alignment.bottomLeading,
                ) {
                    ShapeView(
                        Rectangle(),
                        fill = LinearGradient(
                            colors = listOf(Color.indigo, Color.purple, Color.pink),
                            startPoint = UnitPoint.topLeading,
                            endPoint = UnitPoint.bottomTrailing,
                        ),
                    )
                    Image(systemName = SFSymbol.sparkles, modifier = Modifier.font(Font.system(size = 90.0)).foregroundStyle(Color.white.opacity(0.25)).frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY, alignment = Alignment.topTrailing).padding(16.0))
                    VStack(modifier = Modifier.padding(18.0), alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text("FEATURED", modifier = Modifier.font(Font.caption).bold().kerning(1.5).foregroundStyle(Color.white.opacity(0.8)))
                        Text(verbatim = state.featuredTitle, modifier = Modifier.font(Font.title).bold().foregroundStyle(Color.white))
                        HStack(spacing = 10.0) {
                            Text(verbatim = state.featuredMeta, modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.white.opacity(0.9)))
                            Label("Play", systemImage = SFSymbol.playFill, modifier = Modifier.font(Font.caption).bold(isActive = true).foregroundStyle(Color.black).padding(Edge.Set.horizontal, 10.0).padding(Edge.Set.vertical, 5.0).background(Color.white, Capsule()))
                        }
                    }
                }
                for (shelf in state.shelves) key(shelf.id) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 10.0) {
                        HStack(modifier = Modifier.padding(Edge.Set.horizontal), alignment = VerticalAlignment.firstTextBaseline) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(verbatim = shelf.title, modifier = Modifier.font(Font.title3).bold())
                                Text(verbatim = shelf.subtitle, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                            }
                            Spacer()
                            Image(systemName = SFSymbol.chevronRight, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).foregroundStyle(ShapeStyle.tertiary))
                        }
                        ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                            HStack(modifier = Modifier.padding(Edge.Set.horizontal), alignment = VerticalAlignment.top, spacing = 12.0) {
                                for (video in shelf.videos) key(video.id) {
                                    Screen34Thumb(video, shelf.wide)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen34 = BenchScreen("34 · Video library", ::Screen34ViewModel) { Screen34(it as Screen34ViewModel) }
