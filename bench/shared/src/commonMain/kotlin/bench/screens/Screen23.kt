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

// Screen 23 — Bar chart report: hand-built grouped bars (heights from state), axis labels, legend, summary rows.

data class Screen23Bar(
    val id: Int,
    val label: String,
    val valueText: String,
    val currentHeight: Double,
    val previousHeight: Double,
    val highlighted: Boolean,
)

data class Screen23Axis(val id: Int, val label: String)

data class Screen23Summary(val id: Int, val title: String, val icon: String, val tint: Rgb, val value: String, val delta: String, val deltaTint: Rgb)

data class Screen23State(
    val total: String,
    val change: String,
    val period: String,
    val bars: List<Screen23Bar>,
    val axis: List<Screen23Axis>,
    val summary: List<Screen23Summary>,
)

private const val SCREEN23_CHART_HEIGHT = 160.0
private val screen23Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")

private fun screen23State(current: List<Int>, previous: List<Int>, highlight: Int): Screen23State {
    val max = 200.0
    val total = current.sum()
    val prevTotal = previous.sum()
    val pct = (total - prevTotal) * 1000 / prevTotal
    val pctText = "${if (pct >= 0) "+" else "−"}${kotlin.math.abs(pct) / 10}.${kotlin.math.abs(pct) % 10}%"
    val green = Rgb(0.2, 0.7, 0.35)
    val red = Rgb(0.9, 0.25, 0.2)
    return Screen23State(
        total = "$${total}k",
        change = "$pctText vs last year",
        period = "Jan – Jul 2026",
        bars = screen23Months.mapIndexed { i, m ->
            Screen23Bar(i, m, "${current[i]}", current[i] / max * SCREEN23_CHART_HEIGHT, previous[i] / max * SCREEN23_CHART_HEIGHT, i == highlight)
        },
        axis = listOf(200, 150, 100, 50, 0).mapIndexed { i, v -> Screen23Axis(i, "$v") },
        summary = listOf(
            Screen23Summary(1, "Best month", "trophy.fill", Rgb(1.0, 0.75, 0.0), screen23Months[current.indexOf(current.max())], "$${current.max()}k", green),
            Screen23Summary(2, "Average", "chart.line.flattrend.xyaxis", Rgb(0.0, 0.48, 1.0), "$${total / current.size}k", "per month", Rgb(0.55, 0.55, 0.58)),
            Screen23Summary(3, "Growth", "arrow.up.right", green, pctText, "year over year", if (pct >= 0) green else red),
            Screen23Summary(4, "Refunds", "arrow.uturn.backward", red, "$${current.size * 3}k", "−0.4%", green),
        ),
    )
}

class Screen23ViewModel : BenchViewModel() {
    private val current = mutableListOf(92, 118, 104, 146, 131, 172, 158)
    private val previous = listOf(80, 95, 110, 120, 125, 140, 150)
    private var highlight = 5
    private val _state = MutableStateFlow(screen23State(current, previous, highlight))
    val state: StateFlow<Screen23State> = _state

    override fun mutate() {
        highlight = (highlight + 1) % current.size
        current[highlight] = 60 + (current[highlight] + 37) % 135
        _state.update { screen23State(current, previous, highlight) }
    }
}

@Composable
private fun Screen23Chart(state: Screen23State) {
    VStack(
        modifier = Modifier.padding(16.0).background(Color.gray.opacity(0.08), RoundedRectangle(cornerRadius = 16.0)),
        alignment = HorizontalAlignment.leading,
        spacing = 12.0,
    ) {
        HStack {
            Text("Monthly revenue", modifier = Modifier.font(Font.headline))
            Spacer()
            Text("k$", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
        }
        HStack(alignment = VerticalAlignment.bottom, spacing = 8.0) {
            VStack(modifier = Modifier.frame(height = SCREEN23_CHART_HEIGHT + 8.0).padding(Edge.Set.bottom, 22.0), alignment = HorizontalAlignment.trailing) {
                for (tick in state.axis) key(tick.id) {
                    Text(tick.label, modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                    if (tick.id < state.axis.size - 1) Spacer()
                }
            }
            for (bar in state.bars) key(bar.id) {
                VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 4.0) {
                    Text(
                        bar.valueText,
                        modifier = Modifier.font(Font.caption2).fontWeight(if (bar.highlighted) Font.Weight.bold else Font.Weight.regular)
                            .foregroundStyle(if (bar.highlighted) Color.blue else Color.secondary),
                    )
                    HStack(alignment = VerticalAlignment.bottom, spacing = 2.0) {
                        ShapeView(
                            UnevenRoundedRectangle(topLeadingRadius = 4.0, topTrailingRadius = 4.0),
                            modifier = Modifier.frame(width = 9.0, height = bar.previousHeight),
                            fill = Color.gray.opacity(0.35),
                        )
                        ShapeView(
                            UnevenRoundedRectangle(topLeadingRadius = 4.0, topTrailingRadius = 4.0),
                            modifier = Modifier.frame(width = 13.0, height = bar.currentHeight),
                            fill = if (bar.highlighted) {
                                LinearGradient(colors = listOf(Color.blue, Color.cyan), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom)
                            } else {
                                LinearGradient(colors = listOf(Color.blue.opacity(0.6), Color.blue.opacity(0.35)), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom)
                            },
                        )
                    }
                    Text(bar.label, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                }
            }
        }
        Divider()
        HStack(spacing = 16.0) {
            HStack(spacing = 6.0) {
                ShapeView(RoundedRectangle(cornerRadius = 3.0), modifier = Modifier.frame(width = 12.0, height = 12.0), fill = Color.blue)
                Text("2026", modifier = Modifier.font(Font.caption))
            }
            HStack(spacing = 6.0) {
                ShapeView(RoundedRectangle(cornerRadius = 3.0), modifier = Modifier.frame(width = 12.0, height = 12.0), fill = Color.gray.opacity(0.35))
                Text("2025", modifier = Modifier.font(Font.caption))
            }
            Spacer()
            Label("Selected", systemImage = "hand.tap.fill", modifier = Modifier.font(Font.caption).foregroundStyle(Color.blue))
        }
    }
}

@Composable
fun Screen23(vm: Screen23ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Revenue")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 20.0) {
                VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    Text(state.period, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    Text(state.total, modifier = Modifier.font(Font.system(size = 40.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                    Label(
                        state.change,
                        systemImage = "arrow.up.forward.circle.fill",
                        modifier = Modifier.font(Font.footnote.weight(Font.Weight.semibold))
                            .foregroundStyle(Color.green)
                            .padding(Edge.Set.horizontal, 10.0)
                            .padding(Edge.Set.vertical, 5.0)
                            .background(Color.green.opacity(0.12), Capsule()),
                    )
                }
                Screen23Chart(state)
                VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                    Text("Summary", modifier = Modifier.font(Font.headline).padding(Edge.Set.bottom, 8.0))
                    for (row in state.summary) key(row.id) {
                        HStack(modifier = Modifier.padding(Edge.Set.vertical, 10.0), spacing = 12.0) {
                            Image(systemName = 
                                row.icon,
                                modifier = Modifier.font(Font.system(size = 14.0, weight = Font.Weight.semibold))
                                    .foregroundStyle(row.tint.color())
                                    .frame(width = 32.0, height = 32.0)
                                    .background(row.tint.color().opacity(0.15), Circle()),
                            )
                            Text(row.title, modifier = Modifier.font(Font.subheadline))
                            Spacer()
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) {
                                Text(verbatim = row.value, modifier = Modifier.font(Font.subheadline).bold().monospacedDigit())
                                Text(row.delta, modifier = Modifier.font(Font.caption2).foregroundStyle(row.deltaTint.color()))
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

val screen23 = BenchScreen("23 · Bar chart report", ::Screen23ViewModel) { Screen23(it as Screen23ViewModel) }
