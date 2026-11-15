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

// Screen 04 — Music player: gradient artwork, title/artist, scrubber, transport controls, volume, up-next row.

data class Screen04Action(val id: Int, val icon: String, val label: String)

data class Screen04State(
    val title: String,
    val artist: String,
    val album: String,
    val artFrom: Rgb,
    val artTo: Rgb,
    val progress: Double,
    val elapsed: String,
    val remaining: String,
    val playing: Boolean,
    val liked: Boolean,
    val shuffle: Boolean,
    val volume: Double,
    val actions: List<Screen04Action>,
    val upNext: String,
)

class Screen04ViewModel : BenchViewModel() {
    private var tick = 0
    private val total = 245
    private val _state = MutableStateFlow(
        Screen04State(
            title = "Midnight City Lights",
            artist = "The Analog Engines",
            album = "Signals · 2026",
            artFrom = Rgb(0.98, 0.36, 0.45),
            artTo = Rgb(0.36, 0.22, 0.85),
            progress = 0.0,
            elapsed = "",
            remaining = "",
            playing = true,
            liked = false,
            shuffle = true,
            volume = 0.6,
            actions = listOf(
                Screen04Action(1, "quote.bubble", "Lyrics"),
                Screen04Action(2, "airplayaudio", "AirPlay"),
                Screen04Action(3, "list.bullet", "Queue"),
            ),
            upNext = "Up next: Neon Rivers",
        ).withTime(83, 245)
    )
    val state: StateFlow<Screen04State> = _state

    fun setProgress(value: Double) = _state.update { it.copy(progress = value) }
    fun setVolume(value: Double) = _state.update { it.copy(volume = value) }
    fun togglePlay() = _state.update { it.copy(playing = !it.playing) }

    override fun mutate() = _state.update { s ->
        tick++
        s.withTime(83 + tick * 3, total).copy(playing = tick % 2 == 0, liked = tick % 3 == 0)
    }
}

private fun Screen04State.withTime(seconds: Int, total: Int): Screen04State {
    fun fmt(t: Int) = "${t / 60}:${(t % 60).toString().padStart(2, '0')}"
    return copy(progress = seconds.toDouble() / total, elapsed = fmt(seconds), remaining = "-" + fmt(total - seconds))
}

@Composable
fun Screen04(vm: Screen04ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        VStack(spacing = 22.0, modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 28.0)
            .padding(edges = Edge.Set.top, length = 12.0)
            .navigationTitle("Now Playing")
            .toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline)) {
            ZStack(modifier = Modifier.frame(width = 260.0, height = 260.0).shadow(color = Color.black.opacity(0.25), radius = 16.0, y = 8.0)) {
                ShapeView(
                    RoundedRectangle(cornerRadius = 24.0),
                    fill = LinearGradient(colors = listOf(state.artFrom.color(), state.artTo.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                )
                ShapeView(Circle(), stroke = Color.white.opacity(0.25), lineWidth = 2.0, modifier = Modifier.frame(width = 170.0, height = 170.0))
                Image(systemName = "music.note", modifier = Modifier.font(Font.system(size = 84.0, weight = Font.Weight.semibold)).foregroundStyle(Color.white.opacity(0.9)))
                Text(state.album, modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).foregroundStyle(Color.white.opacity(0.8))
                    .frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY, alignment = Alignment.bottomLeading)
                    .padding(16.0))
            }
            HStack(alignment = VerticalAlignment.center) {
                VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                    Text(state.title, modifier = Modifier.font(Font.title2.bold()).lineLimit(1))
                    Text(state.artist, modifier = Modifier.font(Font.title3).foregroundStyle(ShapeStyle.secondary))
                }
                Spacer()
                Image(systemName = if (state.liked) "heart.fill" else "heart", modifier = Modifier.font(Font.title2).foregroundStyle(Color.pink))
            }
            VStack(spacing = 6.0) {
                Slider(value = state.progress, bounds = 0.0..1.0, onValueChange = vm::setProgress, modifier = Modifier.tint(Color.primary))
                HStack(modifier = Modifier.font(Font.caption.monospacedDigit()).foregroundStyle(ShapeStyle.secondary)) {
                    Text(state.elapsed)
                    Spacer()
                    Text(state.remaining)
                }
            }
            HStack(spacing = 44.0) {
                Image(systemName = "shuffle", modifier = Modifier.font(Font.body).foregroundStyle(if (state.shuffle) Color.pink else Color.secondary))
                Image(systemName = "backward.fill", modifier = Modifier.font(Font.title))
                Image(systemName = if (state.playing) "pause.circle.fill" else "play.circle.fill", modifier = Modifier.font(Font.system(size = 64.0)).symbolRenderingMode(SymbolRenderingMode.hierarchical))
                Image(systemName = "forward.fill", modifier = Modifier.font(Font.title))
                Image(systemName = "repeat", modifier = Modifier.font(Font.body).foregroundStyle(ShapeStyle.secondary))
            }
            HStack(spacing = 12.0, modifier = Modifier.foregroundStyle(ShapeStyle.secondary)) {
                Image(systemName = "speaker.fill", modifier = Modifier.font(Font.caption))
                Slider(value = state.volume, bounds = 0.0..1.0, onValueChange = vm::setVolume, modifier = Modifier.tint(Color.gray))
                Image(systemName = "speaker.wave.3.fill", modifier = Modifier.font(Font.caption))
            }
            HStack {
                for (action in state.actions) key(action.id) {
                    VStack(spacing = 4.0, modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY)) {
                        Image(systemName = action.icon, modifier = Modifier.font(Font.title3))
                        Text(action.label, modifier = Modifier.font(Font.caption2))
                    }
                }
            }
            Text(state.upNext, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary)
                .padding(edges = Edge.Set.horizontal, length = 14.0)
                .padding(edges = Edge.Set.vertical, length = 8.0)
                .background(Color.gray.opacity(0.15), Capsule()))
            Spacer()
        }
    }
}

val screen04 = BenchScreen("04 · Music player", ::Screen04ViewModel) { Screen04(it as Screen04ViewModel) }
