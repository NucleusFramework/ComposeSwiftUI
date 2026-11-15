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

// Screen 39 — Analytics table: KPI tiles and a Grid with header row, aligned numeric columns and alternating row backgrounds.

data class Screen39Kpi(val id: Int, val title: String, val value: String, val delta: String, val up: Boolean, val icon: String, val tint: Rgb)

data class Screen39Row(
    val id: Int,
    val page: String,
    val views: String,
    val visitors: String,
    val bounce: String,
    val change: String,
    val up: Boolean,
    val striped: Boolean,
)

data class Screen39State(val range: String, val kpis: List<Screen39Kpi>, val rows: List<Screen39Row>, val footnote: String)

private data class Screen39Raw(val page: String, val views: Int, val visitors: Int, val bounce: Int, val change: Int)

private fun screen39Thousands(n: Int): String {
    val s = n.toString()
    return s.reversed().chunked(3).joinToString(",").reversed()
}

private fun screen39Pct(tenths: Int): String {
    val sign = if (tenths < 0) "−" else "+"
    val abs = if (tenths < 0) -tenths else tenths
    return "$sign${abs / 10}.${abs % 10}%"
}

private fun screen39Build(raw: List<Screen39Raw>): Screen39State {
    val views = raw.sumOf { it.views }
    val visitors = raw.sumOf { it.visitors }
    return Screen39State(
        range = "Last 7 days · Nov 8 – Nov 14",
        kpis = listOf(
            Screen39Kpi(1, "Page views", screen39Thousands(views), screen39Pct(124), true, "eye.fill", Rgb(0.0, 0.48, 1.0)),
            Screen39Kpi(2, "Visitors", screen39Thousands(visitors), screen39Pct(57), true, "person.2.fill", Rgb(0.2, 0.7, 0.45)),
            Screen39Kpi(3, "Avg. time", "2m 41s", screen39Pct(-31), false, "timer", Rgb(1.0, 0.58, 0.0)),
        ),
        rows = raw.mapIndexed { i, r ->
            Screen39Row(i, r.page, screen39Thousands(r.views), screen39Thousands(r.visitors), "${r.bounce}%", screen39Pct(r.change), r.change >= 0, i % 2 == 1)
        },
        footnote = "Totals include all traffic sources. Bounce = single-page sessions.",
    )
}

class Screen39ViewModel : BenchViewModel() {
    private var raw = listOf(
        Screen39Raw("/home", 48210, 31544, 34, 82),
        Screen39Raw("/pricing", 21980, 15402, 41, 155),
        Screen39Raw("/blog/swiftui", 17433, 12960, 58, -24),
        Screen39Raw("/docs/start", 12087, 8311, 29, 37),
        Screen39Raw("/changelog", 6544, 5102, 63, -108),
        Screen39Raw("/careers", 3901, 3377, 47, 12),
        Screen39Raw("/contact", 2215, 1984, 52, -5),
    )
    private val _state = MutableStateFlow(screen39Build(raw))
    val state: StateFlow<Screen39State> = _state
    private var tick = 0

    override fun mutate() {
        val target = tick++ % raw.size
        raw = raw.mapIndexed { i, r -> if (i == target) r.copy(views = r.views + 137, visitors = r.visitors + 61, change = r.change + 9) else r }
        _state.update { screen39Build(raw) }
    }
}

@Composable
fun Screen39(vm: Screen39ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Analytics")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                Label(state.range, systemImage = "calendar", modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                HStack(spacing = 10.0) {
                    for (kpi in state.kpis) key(kpi.id) {
                        VStack(
                            modifier = Modifier.padding(12.0).frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                                .background(kpi.tint.color().opacity(0.1), RoundedRectangle(cornerRadius = 12.0)),
                            alignment = HorizontalAlignment.leading,
                            spacing = 6.0,
                        ) {
                            Image(systemName = kpi.icon, modifier = Modifier.font(Font.subheadline).foregroundStyle(kpi.tint.color()))
                            Text(verbatim = kpi.value, modifier = Modifier.font(Font.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7))
                            Text(verbatim = kpi.title, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                            Text(verbatim = kpi.delta, modifier = Modifier.font(Font.caption2).bold().foregroundStyle(if (kpi.up) Color.green else Color.red))
                        }
                    }
                }
                Text("Top pages", modifier = Modifier.font(Font.headline))
                Grid(
                    modifier = Modifier.padding(Edge.Set.vertical, 4.0)
                        .background { ShapeView(RoundedRectangle(cornerRadius = 12.0), stroke = Color.gray.opacity(0.25), lineWidth = 1.0) },
                    alignment = Alignment.leading,
                    horizontalSpacing = 0.0,
                    verticalSpacing = 0.0,
                ) {
                    GridRow(modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(ShapeStyle.secondary).padding(Edge.Set.horizontal, 8.0).padding(Edge.Set.vertical, 8.0)) {
                        Text("PAGE")
                        Text("VIEWS", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("USERS", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("BOUNCE", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                        Text("CHANGE", modifier = Modifier.gridColumnAlignment(HorizontalAlignment.trailing))
                    }
                    Divider(modifier = Modifier.gridCellUnsizedAxes(Axis.Set.horizontal))
                    for (row in state.rows) key(row.id) {
                        GridRow(
                            modifier = Modifier.font(Font.footnote).monospacedDigit().padding(Edge.Set.horizontal, 8.0).padding(Edge.Set.vertical, 9.0)
                                .background(if (row.striped) Color.gray.opacity(0.08) else Color.clear),
                        ) {
                            Text(verbatim = row.page, modifier = Modifier.fontWeight(Font.Weight.medium).lineLimit(1))
                            Text(verbatim = row.views)
                            Text(verbatim = row.visitors, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                            Text(verbatim = row.bounce, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                            HStack(modifier = Modifier.foregroundStyle(if (row.up) Color.green else Color.red), spacing = 2.0) {
                                Image(systemName = if (row.up) "arrow.up.right" else "arrow.down.right", modifier = Modifier.imageScale(Image.Scale.small))
                                Text(verbatim = row.change)
                            }
                        }
                    }
                }
                Text(verbatim = state.footnote, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
            }
        }
    }
}

val screen39 = BenchScreen("39 · Analytics table", ::Screen39ViewModel) { Screen39(it as Screen39ViewModel) }
