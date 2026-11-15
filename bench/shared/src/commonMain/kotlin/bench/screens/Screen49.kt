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

// Screen 49 — Sleep tracker: hourly stacked stage bars built from Rectangles, summary stats, stage breakdown and vitals rows.

data class Screen49Segment(val id: Int, val tint: Rgb, val height: Double)

data class Screen49Hour(val id: Int, val label: String, val segments: List<Screen49Segment>)

data class Screen49Stage(val id: Int, val name: String, val tint: Rgb, val duration: String, val share: String)

data class Screen49Stat(val id: Int, val icon: String, val value: String, val label: String)

data class Screen49Vital(val id: Int, val title: String, val icon: String, val value: String)

data class Screen49State(
    val night: String,
    val asleep: String,
    val window: String,
    val score: String,
    val scoreTint: Rgb,
    val hours: List<Screen49Hour>,
    val stages: List<Screen49Stage>,
    val stats: List<Screen49Stat>,
    val vitals: List<Screen49Vital>,
)

private val screen49Colors = listOf(
    Rgb(1.0, 0.45, 0.35), // awake
    Rgb(0.35, 0.78, 0.98), // REM
    Rgb(0.2, 0.48, 1.0), // core
    Rgb(0.35, 0.2, 0.75), // deep
)
private val screen49Names = listOf("Awake", "REM", "Core", "Deep")
private val screen49Labels = listOf("11P", "12A", "1A", "2A", "3A", "4A", "5A", "6A")

// Minutes per stage (awake, REM, core, deep) for each hour of the night.
private val screen49BaseMinutes = listOf(
    listOf(18, 0, 30, 12),
    listOf(4, 6, 22, 28),
    listOf(2, 12, 26, 20),
    listOf(6, 14, 30, 10),
    listOf(3, 20, 28, 9),
    listOf(5, 18, 34, 3),
    listOf(8, 22, 28, 2),
    listOf(14, 16, 30, 0),
)

private fun screen49Duration(minutes: Int) = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

private fun screen49Build(minutes: List<List<Int>>): Screen49State {
    val totals = (0 until 4).map { s -> minutes.sumOf { it[s] } }
    val all = totals.sum()
    val asleep = all - totals[0]
    val score = 100 - totals[0] / 2 - (if (totals[3] < 90) 6 else 0)
    return Screen49State(
        night = "Night of Fri, Nov 13",
        asleep = screen49Duration(asleep),
        window = "In bed 11:04 PM – 7:02 AM",
        score = "$score",
        scoreTint = if (score >= 80) Rgb(0.2, 0.78, 0.35) else Rgb(1.0, 0.58, 0.0),
        hours = minutes.mapIndexed { h, m ->
            Screen49Hour(h, screen49Labels[h], (3 downTo 0).filter { m[it] > 0 }.map { s -> Screen49Segment(s, screen49Colors[s], m[s] * 1.8) })
        },
        stages = (0 until 4).map { s ->
            Screen49Stage(s + 1, screen49Names[s], screen49Colors[s], screen49Duration(totals[s]), "${totals[s] * 100 / all}%")
        },
        stats = listOf(
            Screen49Stat(21, "bed.double.fill", screen49Duration(all), "Time in bed"),
            Screen49Stat(22, "moon.zzz.fill", "14 min", "Fell asleep"),
            Screen49Stat(23, "alarm.fill", "${minutes.count { it[0] >= 8 }}×", "Wake-ups"),
        ),
        vitals = listOf(
            Screen49Vital(11, "Heart rate", "heart.fill", "52 – 61 bpm"),
            Screen49Vital(12, "HRV", "waveform.path.ecg", "64 ms"),
            Screen49Vital(13, "Respiratory rate", "lungs.fill", "14.2 br/min"),
            Screen49Vital(14, "Blood oxygen", "drop.fill", "97%"),
        ),
    )
}

class Screen49ViewModel : BenchViewModel() {
    private var minutes = screen49BaseMinutes
    private val _state = MutableStateFlow(screen49Build(minutes))
    val state: StateFlow<Screen49State> = _state

    private var tick = 0

    override fun mutate() {
        tick += 1
        val hour = tick % minutes.size
        // Move a few minutes from core to deep (or back) for one hour, keeping the hour at 60 minutes.
        minutes = minutes.mapIndexed { h, m ->
            if (h != hour) m else {
                val shift = if (tick % 2 == 0) 4 else -4
                val core = (m[2] - shift).coerceAtLeast(0)
                val deep = m[3] + (m[2] - core)
                listOf(m[0], m[1], core, deep)
            }
        }
        _state.update { screen49Build(minutes) }
    }
}

@Composable
fun Screen49(vm: Screen49ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Sleep")) {
            Section(header = { Text(state.night) }, content = {
                HStack(alignment = VerticalAlignment.top) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text("ASLEEP", modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(ShapeStyle.secondary))
                        Text(state.asleep, modifier = Modifier.font(Font.system(size = 34.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                        Text(state.window, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    }
                    Spacer()
                    VStack(
                        modifier = Modifier.frame(width = 64.0, height = 64.0)
                            .background(state.scoreTint.color().opacity(0.15), Circle()),
                        spacing = 0.0,
                    ) {
                        Text(state.score, modifier = Modifier.font(Font.title2).bold().monospacedDigit().foregroundStyle(state.scoreTint.color()))
                        Text("SCORE", modifier = Modifier.font(Font.system(size = 9.0, weight = Font.Weight.semibold)).foregroundStyle(ShapeStyle.secondary))
                    }
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) {
                    HStack(
                        modifier = Modifier.frame(height = 132.0, alignment = Alignment.bottom),
                        alignment = VerticalAlignment.bottom,
                        spacing = 6.0,
                    ) {
                        for (hour in state.hours) key(hour.id) {
                            VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 4.0) {
                                VStack(modifier = Modifier.clipShape(RoundedRectangle(cornerRadius = 4.0)), spacing = 1.0) {
                                    for (segment in hour.segments) key(segment.id) {
                                        ShapeView(Rectangle(), modifier = Modifier.frame(height = segment.height), fill = segment.tint.color())
                                    }
                                }
                                Text(hour.label, modifier = Modifier.font(Font.system(size = 9.0)).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                    }
                    HStack(spacing = 12.0) {
                        for (stage in state.stages) key(stage.id) {
                            HStack(spacing = 4.0) {
                                ShapeView(RoundedRectangle(cornerRadius = 2.0), modifier = Modifier.frame(width = 10.0, height = 10.0), fill = stage.tint.color())
                                Text(stage.name, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                    }
                }
                HStack {
                    for (stat in state.stats) key(stat.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 3.0) {
                            Image(systemName = stat.icon, modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.indigo))
                            Text(stat.value, modifier = Modifier.font(Font.headline).monospacedDigit())
                            Text(stat.label, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
            })
            Section(title = "Stages") {
                for (stage in state.stages) key(stage.id) {
                    LabeledContent(content = {
                        HStack(spacing = 8.0) {
                            Text(stage.duration, modifier = Modifier.monospacedDigit())
                            Text(
                                stage.share,
                                modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(stage.tint.color())
                                    .frame(width = 40.0, alignment = Alignment.trailing),
                            )
                        }
                    }, label = {
                        HStack(spacing = 8.0) {
                            ShapeView(Circle(), modifier = Modifier.frame(width = 10.0, height = 10.0), fill = stage.tint.color())
                            Text(stage.name)
                        }
                    })
                }
            }
            Section(title = "Vitals") {
                for (vital in state.vitals) key(vital.id) {
                    LabeledContent(content = {
                        Text(vital.value, modifier = Modifier.monospacedDigit())
                    }, label = {
                        Label(title = vital.title, systemImage = vital.icon)
                    })
                }
            }
        }
    }
}

val screen49 = BenchScreen("49 · Sleep tracker", ::Screen49ViewModel) { Screen49(it as Screen49ViewModel) }
