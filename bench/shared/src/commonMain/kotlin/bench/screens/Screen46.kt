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

// Screen 46 — Sports scores: match cards with team color dots, scores and LIVE badge, plus a mini standings table.

data class Screen46Team(val name: String, val short: String, val tint: Rgb)

data class Screen46Match(
    val id: Int,
    val home: Screen46Team,
    val away: Screen46Team,
    val homeScore: Int,
    val awayScore: Int,
    val homeScoreText: String,
    val awayScoreText: String,
    val live: Boolean,
    val status: String,
    val venue: String,
)

data class Screen46Standing(
    val id: Int,
    val rank: String,
    val team: String,
    val tint: Rgb,
    val played: String,
    val won: String,
    val drawn: String,
    val lost: String,
    val points: String,
    val trendUp: Boolean,
)

data class Screen46State(val league: String, val matchday: String, val matches: List<Screen46Match>, val standings: List<Screen46Standing>)

private val screen46Lions = Screen46Team("Lions", "LIO", Rgb(0.85, 0.2, 0.2))
private val screen46Waves = Screen46Team("Waves", "WAV", Rgb(0.0, 0.48, 1.0))
private val screen46Falcons = Screen46Team("Falcons", "FAL", Rgb(0.2, 0.2, 0.2))
private val screen46Comets = Screen46Team("Comets", "COM", Rgb(1.0, 0.7, 0.0))
private val screen46Pines = Screen46Team("Pines", "PIN", Rgb(0.13, 0.6, 0.3))
private val screen46Royals = Screen46Team("Royals", "ROY", Rgb(0.5, 0.3, 0.8))

private fun screen46Match(id: Int, h: Screen46Team, a: Screen46Team, hs: Int, as_: Int, live: Boolean, status: String, venue: String) =
    Screen46Match(id, h, a, hs, as_, "$hs", "$as_", live, status, venue)

class Screen46ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen46State(
            league = "Premier Division",
            matchday = "Matchday 14 · Saturday",
            matches = listOf(
                screen46Match(1, screen46Lions, screen46Waves, 2, 1, true, "67'", "Lion Park"),
                screen46Match(2, screen46Falcons, screen46Comets, 0, 0, true, "23'", "Eyrie Arena"),
                screen46Match(3, screen46Pines, screen46Royals, 3, 2, false, "FT", "Forest Ground"),
                screen46Match(4, screen46Waves, screen46Pines, 0, 0, false, "20:45", "Harbour Stadium"),
            ),
            standings = listOf(
                Screen46Standing(1, "1", "Lions", screen46Lions.tint, "13", "10", "2", "1", "32", true),
                Screen46Standing(2, "2", "Royals", screen46Royals.tint, "13", "9", "2", "2", "29", false),
                Screen46Standing(3, "3", "Waves", screen46Waves.tint, "13", "8", "3", "2", "27", true),
                Screen46Standing(4, "4", "Pines", screen46Pines.tint, "13", "7", "2", "4", "23", true),
                Screen46Standing(5, "5", "Comets", screen46Comets.tint, "13", "5", "3", "5", "18", false),
                Screen46Standing(6, "6", "Falcons", screen46Falcons.tint, "13", "3", "1", "9", "10", false),
            ),
        )
    )
    val state: StateFlow<Screen46State> = _state

    private var tick = 0

    override fun mutate() {
        tick += 1
        _state.update { s ->
            s.copy(matches = s.matches.map { m ->
                when (m.id) {
                    1 -> {
                        val hs = if (tick % 3 == 0) m.homeScore + 1 else m.homeScore
                        m.copy(homeScore = hs, homeScoreText = "$hs", status = "${67 + tick % 23}'")
                    }
                    2 -> {
                        val aw = if (tick % 4 == 0) m.awayScore + 1 else m.awayScore
                        m.copy(awayScore = aw, awayScoreText = "$aw", status = "${23 + tick % 60}'")
                    }
                    else -> m
                }
            })
        }
    }
}

@Composable
private fun Screen46TeamColumn(team: Screen46Team, alignment: HorizontalAlignment) {
    VStack(modifier = Modifier.frame(width = 80.0), alignment = alignment, spacing = 6.0) {
        ZStack(modifier = Modifier.frame(width = 36.0, height = 36.0)) {
            ShapeView(Circle(), fill = team.tint.color())
            Text(team.short, modifier = Modifier.font(Font.system(size = 10.0, weight = Font.Weight.heavy)).foregroundStyle(Color.white))
        }
        Text(team.name, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.medium).lineLimit(1))
    }
}

@Composable
fun Screen46(vm: Screen46ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Scores")) {
            Section(header = {
                HStack {
                    Text(state.league)
                    Spacer()
                    Text(state.matchday, modifier = Modifier.font(Font.caption))
                }
            }, content = {
                for (match in state.matches) key(match.id) {
                    VStack(spacing = 8.0) {
                        HStack {
                            Screen46TeamColumn(match.home, HorizontalAlignment.center)
                            Spacer()
                            VStack(spacing = 4.0) {
                                HStack(spacing = 10.0) {
                                    Text(match.homeScoreText, modifier = Modifier.font(Font.system(size = 30.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                                    Text("–", modifier = Modifier.font(Font.title2).foregroundStyle(ShapeStyle.tertiary))
                                    Text(match.awayScoreText, modifier = Modifier.font(Font.system(size = 30.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                                }
                                if (match.live) {
                                    HStack(
                                        modifier = Modifier.foregroundStyle(Color.white)
                                            .padding(Edge.Set.horizontal, 7.0)
                                            .padding(Edge.Set.vertical, 3.0)
                                            .background(Color.red, Capsule()),
                                        spacing = 4.0,
                                    ) {
                                        ShapeView(Circle(), modifier = Modifier.frame(width = 6.0, height = 6.0), fill = Color.white)
                                        Text("LIVE", modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.black))
                                        Text(match.status, modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.bold).monospacedDigit())
                                    }
                                } else {
                                    Text(match.status, modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(ShapeStyle.secondary))
                                }
                            }
                            Spacer()
                            Screen46TeamColumn(match.away, HorizontalAlignment.center)
                        }
                        Label(title = match.venue, systemImage = "sportscourt", modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                    }
                }
            })
            Section(title = "Standings") {
                Grid(alignment = Alignment.leading, horizontalSpacing = 12.0, verticalSpacing = 10.0) {
                    GridRow(modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(ShapeStyle.secondary)) {
                        Text("#")
                        Text("Team")
                        Text("P", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("W", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("D", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("L", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("Pts", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                    }
                    Divider()
                    for (standing in state.standings) key(standing.id) {
                        GridRow(modifier = Modifier.font(Font.subheadline).monospacedDigit()) {
                            Text(standing.rank, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                            HStack(spacing = 8.0) {
                                ShapeView(Circle(), modifier = Modifier.frame(width = 10.0, height = 10.0), fill = standing.tint.color())
                                Text(standing.team, modifier = Modifier.fontWeight(Font.Weight.medium))
                                ImageSystemName(
                                    if (standing.trendUp) "arrowtriangle.up.fill" else "arrowtriangle.down.fill",
                                    modifier = Modifier.font(Font.system(size = 8.0)).foregroundStyle(if (standing.trendUp) Color.green else Color.red),
                                )
                            }
                            Text(standing.played)
                            Text(standing.won)
                            Text(standing.drawn)
                            Text(standing.lost)
                            Text(standing.points, modifier = Modifier.bold())
                        }
                    }
                }
            }
        }
    }
}

val screen46 = BenchScreen("46 · Sports scores", ::Screen46ViewModel) { Screen46(it as Screen46ViewModel) }
