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

// Screen 32 — Habit tracker: summary card, habit rows with 7-day dot history, streaks and completion checkmarks.

data class Screen32Day(val id: Int, val done: Boolean)

data class Screen32Habit(
    val id: Int,
    val name: String,
    val icon: String,
    val tint: Rgb,
    val goal: String,
    val days: List<Screen32Day>,
    val streak: String,
    val doneToday: Boolean,
)

data class Screen32Weekday(val id: Int, val letter: String)

data class Screen32State(
    val dateLine: String,
    val summary: String,
    val progress: Double,
    val bestStreak: String,
    val weekdays: List<Screen32Weekday>,
    val habits: List<Screen32Habit>,
    val tip: String,
)

private fun screen32Habit(id: Int, name: String, icon: String, tint: Rgb, goal: String, pattern: String, streak: Int): Screen32Habit {
    val days = pattern.mapIndexed { i, c -> Screen32Day(i, c == '1') }
    return Screen32Habit(id, name, icon, tint, goal, days, "$streak days", days.last().done)
}

private fun screen32Summary(habits: List<Screen32Habit>): Triple<String, Double, String> {
    val done = habits.count { it.doneToday }
    val best = habits.maxOf { it.streak.substringBefore(' ').toInt() }
    return Triple("$done of ${habits.size} habits done", done.toDouble() / habits.size, "Best streak · $best days")
}

class Screen32ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(run {
        val habits = listOf(
            screen32Habit(1, "Morning run", "figure.run", Rgb(1.0, 0.45, 0.2), "5 km", "1101111", 4),
            screen32Habit(2, "Reading", "book.fill", Rgb(0.35, 0.34, 0.84), "20 pages", "1111110", 0),
            screen32Habit(3, "Meditate", "brain.head.profile", Rgb(0.2, 0.7, 0.65), "10 min", "0111111", 6),
            screen32Habit(4, "Drink water", "drop.fill", Rgb(0.0, 0.48, 1.0), "8 cups", "1111111", 21),
            screen32Habit(5, "No sugar", "leaf.fill", Rgb(0.2, 0.78, 0.35), "All day", "1010010", 0),
        )
        val (summary, progress, best) = screen32Summary(habits)
        Screen32State(
            dateLine = "Thursday, 14 November",
            summary = summary,
            progress = progress,
            bestStreak = best,
            weekdays = listOf("F", "S", "S", "M", "T", "W", "T").mapIndexed { i, l -> Screen32Weekday(i, l) },
            habits = habits,
            tip = "Tap a habit to mark it done. Streaks reset at midnight.",
        )
    })
    val state: StateFlow<Screen32State> = _state
    private var tick = 0

    override fun mutate() = _state.update { s ->
        val target = tick++ % s.habits.size
        val habits = s.habits.map { h ->
            if (h.id - 1 != target) return@map h
            val done = !h.doneToday
            val streak = h.streak.substringBefore(' ').toInt() + if (done) 1 else -1
            h.copy(
                doneToday = done,
                days = h.days.dropLast(1) + Screen32Day(6, done),
                streak = "${streak.coerceAtLeast(0)} days",
            )
        }
        val (summary, progress, best) = screen32Summary(habits)
        s.copy(habits = habits, summary = summary, progress = progress, bestStreak = best)
    }
}

@Composable
fun Screen32(vm: Screen32ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Habits")) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0), alignment = HorizontalAlignment.leading, spacing = 10.0) {
                    HStack {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Text(verbatim = state.dateLine, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).textCase(Text.Case.uppercase))
                            Text(verbatim = state.summary, modifier = Modifier.font(Font.title3).fontWeight(Font.Weight.semibold))
                        }
                        Spacer()
                        ZStack(modifier = Modifier.frame(width = 44.0, height = 44.0)) {
                            ShapeView(Circle(), fill = Color.orange.opacity(0.15))
                            Image(systemName = "flame.fill", modifier = Modifier.font(Font.title3).foregroundStyle(Color.orange))
                        }
                    }
                    ProgressView(value = state.progress, modifier = Modifier.tint(Color.green))
                    Label(state.bestStreak, systemImage = "trophy.fill", modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                }
            }
            Section(header = {
                HStack {
                    Text("Today")
                    Spacer()
                    HStack(modifier = Modifier.padding(Edge.Set.trailing, 44.0), spacing = 5.0) {
                        for (day in state.weekdays) key(day.id) {
                            Text(verbatim = day.letter, modifier = Modifier.font(Font.caption2).monospaced().frame(width = 8.0))
                        }
                    }
                }
            }, footer = { Text(verbatim = state.tip) }, content = {
                for (habit in state.habits) key(habit.id) {
                    HStack(spacing = 12.0) {
                        Image(systemName = 
                            habit.icon,
                            modifier = Modifier.font(Font.system(size = 16.0, weight = Font.Weight.semibold))
                                .foregroundStyle(habit.tint.color())
                                .frame(width = 36.0, height = 36.0)
                                .background(habit.tint.color().opacity(0.15), RoundedRectangle(cornerRadius = 9.0)),
                        )
                        VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                            Text(verbatim = habit.name, modifier = Modifier.font(Font.body).lineLimit(1))
                            HStack(modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1), spacing = 4.0) {
                                Image(systemName = "flame", modifier = Modifier.imageScale(Image.Scale.small))
                                Text(verbatim = habit.streak, modifier = Modifier.monospacedDigit())
                                Text(verbatim = "· ${habit.goal}")
                            }
                        }
                        Spacer(minLength = 4.0)
                        HStack(spacing = 5.0) {
                            for (day in habit.days) key(day.id) {
                                ShapeView(
                                    Circle(),
                                    modifier = Modifier.frame(width = 8.0, height = 8.0),
                                    fill = if (day.done) habit.tint.color() else Color.gray.opacity(0.25),
                                )
                            }
                        }
                        Image(systemName = 
                            if (habit.doneToday) "checkmark.circle.fill" else "circle",
                            modifier = Modifier.font(Font.title2)
                                .foregroundStyle(if (habit.doneToday) Color.green else Color.gray.opacity(0.5))
                                .frame(width = 32.0),
                        )
                    }
                }
            })
        }
    }
}

val screen32 = BenchScreen("32 · Habit tracker", ::Screen32ViewModel) { Screen32(it as Screen32ViewModel) }
