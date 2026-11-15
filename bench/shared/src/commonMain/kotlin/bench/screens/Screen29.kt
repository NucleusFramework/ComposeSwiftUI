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

// Screen 29 — Podcast episodes: show header, List of episodes with artwork tiles, duration, progress, download state, Menu.

data class Screen29Episode(
    val id: Int,
    val date: String,
    val title: String,
    val summary: String,
    val duration: String,
    val progress: Double,
    val played: Boolean,
    val symbol: String,
    val top: Rgb,
    val bottom: Rgb,
    val downloadIcon: String,
    val downloadTint: Rgb,
)

data class Screen29State(
    val show: String,
    val author: String,
    val meta: String,
    val subscribed: Boolean,
    val episodes: List<Screen29Episode>,
)

private data class Screen29Raw(val id: Int, val date: String, val title: String, val summary: String, val minutes: Int, val symbol: String, val top: Rgb, val bottom: Rgb)

private fun screen29State(raw: List<Screen29Raw>, played: List<Int>, downloads: List<Int>, subscribed: Boolean): Screen29State {
    val icons = listOf("arrow.down.circle", "arrow.down.circle.dotted", "checkmark.circle.fill")
    val tints = listOf(Rgb(0.0, 0.48, 1.0), Rgb(1.0, 0.58, 0.0), Rgb(0.2, 0.7, 0.35))
    return Screen29State(
        show = "Swift by Night",
        author = "Indie Dev Collective",
        meta = "142 episodes · Weekly · Technology",
        subscribed = subscribed,
        episodes = raw.mapIndexed { i, r ->
            val minutesPlayed = played[i]
            val remaining = r.minutes - minutesPlayed
            Screen29Episode(
                id = r.id,
                date = r.date,
                title = r.title,
                summary = r.summary,
                duration = if (minutesPlayed == 0) "${r.minutes} min" else if (remaining <= 0) "Played" else "$remaining min left",
                progress = minutesPlayed.toDouble() / r.minutes,
                played = remaining <= 0,
                symbol = r.symbol,
                top = r.top,
                bottom = r.bottom,
                downloadIcon = icons[downloads[i]],
                downloadTint = tints[downloads[i]],
            )
        },
    )
}

class Screen29ViewModel : BenchViewModel() {
    private val raw = listOf(
        Screen29Raw(1, "Today", "Structured concurrency in practice", "Task groups, cancellation and the patterns we actually ship.", 48, "bolt.horizontal.fill", Rgb(1.0, 0.45, 0.2), Rgb(0.9, 0.2, 0.4)),
        Screen29Raw(2, "Nov 7", "Designing APIs people love", "Naming, defaults and progressive disclosure for library authors.", 62, "hammer.fill", Rgb(0.2, 0.6, 1.0), Rgb(0.35, 0.34, 0.84)),
        Screen29Raw(3, "Oct 31", "Shipping a Mac app in 30 days", "From prototype to notarized build, a candid retrospective.", 55, "macbook", Rgb(0.2, 0.75, 0.55), Rgb(0.1, 0.5, 0.6)),
        Screen29Raw(4, "Oct 24", "Profiling memory like a pro", "Instruments, allocation graphs and finding the leak at 2 am.", 41, "memorychip.fill", Rgb(0.69, 0.32, 0.87), Rgb(0.4, 0.2, 0.7)),
        Screen29Raw(5, "Oct 17", "The state of server-side Swift", "Frameworks, deployment and what changed this year.", 73, "server.rack", Rgb(0.95, 0.75, 0.1), Rgb(0.9, 0.5, 0.0)),
    )
    private val played = mutableListOf(0, 20, 55, 0, 10)
    private val downloads = mutableListOf(1, 2, 2, 0, 0)
    private var subscribed = true
    private var tick = 0
    private val _state = MutableStateFlow(screen29State(raw, played, downloads, subscribed))
    val state: StateFlow<Screen29State> = _state

    override fun mutate() {
        tick++
        played[0] = (played[0] + 7) % 49
        downloads[tick % raw.size] = (downloads[tick % raw.size] + 1) % 3
        subscribed = !subscribed
        _state.update { screen29State(raw, played, downloads, subscribed) }
    }
}

@Composable
private fun Screen29Row(ep: Screen29Episode) {
    HStack(alignment = VerticalAlignment.top, spacing = 12.0) {
        ZStack(modifier = Modifier.frame(width = 60.0, height = 60.0)) {
            ShapeView(
                RoundedRectangle(cornerRadius = 12.0),
                fill = LinearGradient(colors = listOf(ep.top.color(), ep.bottom.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
            )
            Image(systemName = ep.symbol, modifier = Modifier.font(Font.title2).foregroundStyle(Color.white).shadow(radius = 2.0))
        }
        VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
            Text(ep.date, modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold).textCase(Text.Case.uppercase).foregroundStyle(ShapeStyle.secondary))
            Text(ep.title, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).lineLimit(2).foregroundStyle(if (ep.played) Color.secondary else Color.primary))
            Text(ep.summary, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(2))
            HStack(spacing = 6.0) {
                Image(systemName = if (ep.played) "checkmark.circle" else "play.circle.fill", modifier = Modifier.font(Font.title3).foregroundStyle(Color.purple))
                Text(ep.duration, modifier = Modifier.font(Font.caption.monospacedDigit()).fontWeight(Font.Weight.medium))
                if (ep.progress > 0.0 && !ep.played) {
                    ProgressView(value = ep.progress, modifier = Modifier.frame(width = 54.0).tint(Color.purple))
                }
                Spacer()
                Image(systemName = ep.downloadIcon, modifier = Modifier.font(Font.body).foregroundStyle(ep.downloadTint.color()))
                Menu(
                    modifier = Modifier.menuIndicator(Visibility.hidden).fixedSize(),
                    content = {
                        Button(title = "Play Next", systemImage = "text.line.first.and.arrowtriangle.forward", action = {})
                        Button(title = "Mark as Played", systemImage = "checkmark.circle", action = {})
                        Button(title = "Share Episode", systemImage = "square.and.arrow.up", action = {})
                        Divider()
                        Button(title = "Remove Download", systemImage = "trash", role = ButtonRole.destructive, action = {})
                    },
                    label = {
                        Image(systemName = "ellipsis", modifier = Modifier.font(Font.body).frame(width = 26.0, height = 26.0).background(Color.gray.opacity(0.12), Circle()))
                    },
                )
            }
        }
    }
}

@Composable
fun Screen29(vm: Screen29ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.plain).navigationTitle("Podcasts")) {
            Section {
                HStack(spacing = 14.0) {
                    Image(systemName = 
                        "waveform.and.mic",
                        modifier = Modifier.font(Font.system(size = 38.0, weight = Font.Weight.semibold))
                            .foregroundStyle(Color.white)
                            .frame(width = 96.0, height = 96.0)
                            .background(
                                LinearGradient(colors = listOf(Color.purple, Color.indigo), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                                RoundedRectangle(cornerRadius = 18.0),
                            ),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text(state.show, modifier = Modifier.font(Font.title3.bold()))
                        Text(state.author, modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.purple))
                        Text(state.meta, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        if (state.subscribed) {
                            Button(title = "Following", systemImage = "checkmark", action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered).controlSize(ControlSize.small).tint(Color.purple))
                        } else {
                            Button(title = "Follow", systemImage = "plus", action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).controlSize(ControlSize.small).tint(Color.purple))
                        }
                    }
                }
            }
            Section(header = { Text("Latest Episodes") }, content = {
                for (ep in state.episodes) key(ep.id) {
                    Screen29Row(ep)
                }
            })
        }
    }
}

val screen29 = BenchScreen("29 · Podcast episodes", ::Screen29ViewModel) { Screen29(it as Screen29ViewModel) }
