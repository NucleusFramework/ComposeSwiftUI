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

// Screen 12 — Fitness activity: ring gauges, stats grid, recent workouts list.

data class Screen12Ring(val id: Int, val title: String, val icon: String, val progress: Double, val valueText: String, val goalText: String, val tint: Rgb)

data class Screen12Stat(val id: Int, val label: String, val value: String, val unit: String, val icon: String, val tint: Rgb)

data class Screen12Workout(val id: Int, val name: String, val icon: String, val detail: String, val duration: String, val calories: String, val tint: Rgb)

data class Screen12State(
    val dateLine: String,
    val rings: List<Screen12Ring>,
    val stats: List<Screen12Stat>,
    val workouts: List<Screen12Workout>,
    val streak: String,
    val tick: Int,
)

private val ringMove = Rgb(0.98, 0.07, 0.31)
private val ringExercise = Rgb(0.6, 0.95, 0.0)
private val ringStand = Rgb(0.1, 0.85, 0.95)

private fun screen12Rings(tick: Int): List<Screen12Ring> {
    val move = 420 + tick * 7
    val exercise = 22 + tick
    val stand = 8 + tick % 5
    return listOf(
        Screen12Ring(1, "Move", "flame.fill", minOf(move / 600.0, 1.0), "$move", "/600 kcal", ringMove),
        Screen12Ring(2, "Exercise", "figure.run", minOf(exercise / 30.0, 1.0), "$exercise", "/30 min", ringExercise),
        Screen12Ring(3, "Stand", "figure.stand", minOf(stand / 12.0, 1.0), "$stand", "/12 hr", ringStand),
    )
}

private fun screen12Stats(tick: Int): List<Screen12Stat> = listOf(
    Screen12Stat(1, "Steps", "${8_412 + tick * 37}", "steps", "shoeprints.fill", Rgb(1.0, 0.58, 0.0)),
    Screen12Stat(2, "Distance", "${6 + tick / 10}.${(tick * 3) % 10}", "km", "map.fill", Rgb(0.2, 0.78, 0.35)),
    Screen12Stat(3, "Heart rate", "${72 + tick % 9}", "bpm", "heart.fill", Rgb(1.0, 0.23, 0.19)),
    Screen12Stat(4, "Flights", "${11 + tick % 4}", "floors", "stairs", Rgb(0.35, 0.34, 0.84)),
)

class Screen12ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen12State(
            dateLine = "Saturday, 14 November",
            rings = screen12Rings(0),
            stats = screen12Stats(0),
            workouts = listOf(
                Screen12Workout(1, "Outdoor Run", "figure.run", "Riverside loop", "42:18", "412 kcal", Rgb(0.6, 0.95, 0.0)),
                Screen12Workout(2, "Cycling", "bicycle", "Hill intervals", "1:05:40", "655 kcal", Rgb(1.0, 0.8, 0.0)),
                Screen12Workout(3, "Yoga", "figure.yoga", "Morning flow", "30:00", "118 kcal", Rgb(0.1, 0.85, 0.95)),
                Screen12Workout(4, "Pool Swim", "figure.pool.swim", "25 m lanes", "38:12", "390 kcal", Rgb(0.0, 0.48, 1.0)),
                Screen12Workout(5, "Strength", "dumbbell.fill", "Upper body", "47:55", "301 kcal", Rgb(0.98, 0.07, 0.31)),
            ),
            streak = "12-day streak",
            tick = 0,
        )
    )
    val state: StateFlow<Screen12State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(tick = t, rings = screen12Rings(t), stats = screen12Stats(t), streak = "${12 + t}-day streak")
    }
}

@Composable
fun Screen12(vm: Screen12ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.background(Color.gray.opacity(0.1)).navigationTitle("Activity")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                HStack {
                    Text(state.dateLine, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    Spacer()
                    Label(state.streak, systemImage = "bolt.heart.fill", modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).foregroundStyle(Color.orange))
                }
                HStack(modifier = Modifier.padding(Edge.Set.vertical, 16.0), spacing = 12.0) {
                    for (ring in state.rings) key(ring.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 8.0) {
                            Gauge(
                                value = ring.progress,
                                bounds = 0.0..1.0,
                                modifier = Modifier.gaugeStyle(GaugeStyle.accessoryCircularCapacity).tint(ring.tint.color()).scaleEffect(1.2),
                                label = { Text(ring.title) },
                                currentValueLabel = { ImageSystemName(ring.icon, modifier = Modifier.foregroundStyle(ring.tint.color())) },
                            )
                            VStack(spacing = 0.0) {
                                Text(ring.valueText, modifier = Modifier.font(Font.headline).monospacedDigit())
                                Text(ring.goalText, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                    }
                }
                Text("Today", modifier = Modifier.font(Font.title3).bold())
                Grid(horizontalSpacing = 12.0, verticalSpacing = 12.0) {
                    for (row in state.stats.chunked(2)) key(row.first().id) {
                        GridRow {
                            for (stat in row) key(stat.id) {
                                VStack(
                                    modifier = Modifier.padding(12.0)
                                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                                        .background(ShapeStyle.background, RoundedRectangle(cornerRadius = 14.0)),
                                    alignment = HorizontalAlignment.leading,
                                    spacing = 6.0,
                                ) {
                                    HStack(spacing = 6.0) {
                                        ImageSystemName(stat.icon, modifier = Modifier.font(Font.caption.weight(Font.Weight.bold)).foregroundStyle(stat.tint.color()))
                                        Text(stat.label, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                    }
                                    HStack(alignment = VerticalAlignment.firstTextBaseline, spacing = 4.0) {
                                        Text(stat.value, modifier = Modifier.font(Font.system(size = 24.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                                        Text(stat.unit, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                    }
                                }
                            }
                        }
                    }
                }
                HStack {
                    Text("Workouts", modifier = Modifier.font(Font.title3).bold())
                    Spacer()
                    Text("Show More", modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.green))
                }
                VStack(modifier = Modifier.background(ShapeStyle.background, RoundedRectangle(cornerRadius = 14.0)), spacing = 0.0) {
                    for ((index, workout) in state.workouts.withIndex()) key(workout.id) {
                        HStack(modifier = Modifier.padding(Edge.Set.horizontal, 12.0).padding(Edge.Set.vertical, 10.0), spacing = 12.0) {
                            ImageSystemName(
                                workout.icon,
                                modifier = Modifier.font(Font.system(size = 18.0, weight = Font.Weight.semibold))
                                    .foregroundStyle(workout.tint.color())
                                    .frame(width = 40.0, height = 40.0)
                                    .background(workout.tint.color().opacity(0.18), Circle()),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(workout.name, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)))
                                Text(workout.detail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                            Spacer()
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) {
                                Text(workout.duration, modifier = Modifier.font(Font.subheadline).monospacedDigit())
                                Text(workout.calories, modifier = Modifier.font(Font.caption).foregroundStyle(workout.tint.color()))
                            }
                        }
                        if (index < state.workouts.size - 1) {
                            Divider(modifier = Modifier.padding(Edge.Set.leading, 64.0))
                        }
                    }
                }
            }
        }
    }
}

val screen12 = BenchScreen("12 · Fitness activity", ::Screen12ViewModel) { Screen12(it as Screen12ViewModel) }
