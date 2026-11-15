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

// Screen 26 — Leaderboard: podium for the top 3 with medals, ranked List rows with avatars, scores and progress bars.

data class Screen26Player(
    val id: Int,
    val rank: String,
    val name: String,
    val initials: String,
    val color: Rgb,
    val score: String,
    val progress: Double,
    val trendIcon: String,
    val trendTint: Rgb,
    val medalTint: Rgb,
    val podiumHeight: Double,
)

data class Screen26State(val season: String, val podium: List<Screen26Player>, val players: List<Screen26Player>, val you: String)

private data class Screen26Raw(val id: Int, val name: String, val color: Rgb, val score: Int, val previousRank: Int)

private fun screen26State(raw: List<Screen26Raw>): Screen26State {
    val sorted = raw.sortedByDescending { it.score }
    val top = sorted.first().score.toDouble()
    val medals = listOf(Rgb(1.0, 0.76, 0.03), Rgb(0.66, 0.68, 0.72), Rgb(0.8, 0.5, 0.2))
    val players = sorted.mapIndexed { i, p ->
        val trend = p.previousRank - i
        Screen26Player(
            id = p.id,
            rank = "${i + 1}",
            name = p.name,
            initials = p.name.split(" ").joinToString("") { it.take(1) },
            color = p.color,
            score = p.score.toString().reversed().chunked(3).joinToString(",").reversed(),
            progress = p.score / top,
            trendIcon = if (trend > 0) "arrowtriangle.up.fill" else if (trend < 0) "arrowtriangle.down.fill" else "minus",
            trendTint = if (trend > 0) Rgb(0.2, 0.7, 0.35) else if (trend < 0) Rgb(0.9, 0.25, 0.2) else Rgb(0.6, 0.6, 0.63),
            medalTint = if (i < 3) medals[i] else Rgb(0.6, 0.6, 0.63),
            podiumHeight = listOf(110.0, 80.0, 60.0).getOrElse(i) { 0.0 },
        )
    }
    return Screen26State(
        season = "Season 7 · Week 12",
        podium = listOf(players[1], players[0], players[2]),
        players = players,
        you = "You are ranked #${players.indexOfFirst { it.id == 6 } + 1}",
    )
}

class Screen26ViewModel : BenchViewModel() {
    private val raw = mutableListOf(
        Screen26Raw(1, "Maya Chen", Rgb(0.0, 0.48, 1.0), 18420, 1),
        Screen26Raw(2, "Leo Martin", Rgb(1.0, 0.58, 0.0), 17950, 0),
        Screen26Raw(3, "Sara Okafor", Rgb(0.69, 0.32, 0.87), 16310, 2),
        Screen26Raw(4, "Tom Weber", Rgb(0.2, 0.7, 0.35), 15020, 4),
        Screen26Raw(5, "Ines Duarte", Rgb(1.0, 0.18, 0.33), 14780, 3),
        Screen26Raw(6, "Kai Tanaka", Rgb(0.19, 0.69, 0.78), 13990, 6),
        Screen26Raw(7, "Noah Levi", Rgb(0.35, 0.34, 0.84), 12550, 5),
        Screen26Raw(8, "Zoe Adams", Rgb(0.6, 0.4, 0.2), 11200, 7),
    )
    private var tick = 0
    private val _state = MutableStateFlow(screen26State(raw))
    val state: StateFlow<Screen26State> = _state

    override fun mutate() {
        tick++
        val i = tick % raw.size
        raw[i] = raw[i].copy(score = raw[i].score + 950 + tick * 7)
        _state.update { screen26State(raw) }
    }
}

@Composable
private fun Screen26Podium(players: List<Screen26Player>) {
    HStack(alignment = VerticalAlignment.bottom, spacing = 10.0) {
        for (p in players) key(p.id) {
            VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 6.0) {
                ZStack(modifier = Modifier.frame(width = 56.0, height = 56.0), alignment = Alignment.bottomTrailing) {
                    Text(
                        p.initials,
                        modifier = Modifier.font(Font.headline)
                            .foregroundStyle(Color.white)
                            .frame(width = 56.0, height = 56.0)
                            .background(p.color.color(), Circle())
                            .overlay { ShapeView(Circle(), stroke = p.medalTint.color(), lineWidth = 3.0) },
                    )
                    Image(systemName = 
                        SFSymbol.medalFill,
                        modifier = Modifier.font(Font.system(size = 16.0))
                            .symbolRenderingMode(SymbolRenderingMode.palette)
                            .foregroundStyle(p.medalTint.color(), Color.red)
                            .offset(x = 4.0, y = 4.0),
                    )
                }
                Text(p.name, modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).lineLimit(1))
                Text(p.score, modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                Text(
                    p.rank,
                    modifier = Modifier.font(Font.system(size = 28.0, weight = Font.Weight.heavy, design = Font.Design.rounded))
                        .foregroundStyle(Color.white)
                        .frame(maxWidth = Double.POSITIVE_INFINITY)
                        .frame(height = p.podiumHeight)
                        .background(
                            LinearGradient(colors = listOf(p.medalTint.color(), p.medalTint.color().opacity(0.6)), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                            UnevenRoundedRectangle(topLeadingRadius = 10.0, topTrailingRadius = 10.0),
                        ),
                )
            }
        }
    }
}

@Composable
fun Screen26(vm: Screen26ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.navigationTitle("Leaderboard")) {
            Section(header = { Text(state.season) }, footer = { Text(state.you) }, content = {
                Screen26Podium(state.podium)
            })
            Section(title = "Rankings") {
                for (p in state.players) key(p.id) {
                    HStack(spacing = 12.0) {
                        Text(p.rank, modifier = Modifier.font(Font.subheadline.monospacedDigit()).fontWeight(Font.Weight.bold).foregroundStyle(p.medalTint.color()).frame(width = 22.0, alignment = Alignment.leading))
                        Text(
                            p.initials,
                            modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.bold)
                                .foregroundStyle(Color.white)
                                .frame(width = 36.0, height = 36.0)
                                .background(LinearGradient(colors = listOf(p.color.color().opacity(0.7), p.color.color()), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom), Circle()),
                        )
                        VStack(alignment = HorizontalAlignment.leading, spacing = 5.0) {
                            HStack(spacing = 4.0) {
                                Text(p.name, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold))
                                Image(systemName = SFSymbol(p.trendIcon), modifier = Modifier.font(Font.system(size = 8.0)).foregroundStyle(p.trendTint.color()))
                            }
                            ProgressView(value = p.progress, modifier = Modifier.tint(p.color.color()))
                        }
                        Text(p.score, modifier = Modifier.font(Font.subheadline.monospacedDigit()).fontWeight(Font.Weight.semibold).frame(width = 64.0, alignment = Alignment.trailing))
                    }
                }
            }
        }
    }
}

val screen26 = BenchScreen("26 · Leaderboard", ::Screen26ViewModel) { Screen26(it as Screen26ViewModel) }
