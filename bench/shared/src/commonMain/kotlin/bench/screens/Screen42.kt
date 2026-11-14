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

// Screen 42 — Task manager: summary header, sectioned task list with check circles, priority flags and due dates.

data class Screen42Task(
    val id: Int,
    val title: String,
    val notes: String,
    val due: String,
    val overdue: Boolean,
    val done: Boolean,
    val priority: Int,
    val tag: String,
    val tagTint: Rgb,
)

data class Screen42Section(val id: Int, val title: String, val icon: String, val count: String, val tasks: List<Screen42Task>)

data class Screen42State(
    val sections: List<Screen42Section>,
    val summary: String,
    val progress: Double,
    val progressText: String,
    val footer: String,
)

private val screen42Blue = Rgb(0.0, 0.48, 1.0)
private val screen42Green = Rgb(0.2, 0.78, 0.35)
private val screen42Purple = Rgb(0.69, 0.32, 0.87)
private val screen42Orange = Rgb(1.0, 0.58, 0.0)

private fun screen42Build(sections: List<Screen42Section>): Screen42State {
    val counted = sections.map { sec -> sec.copy(count = "${sec.tasks.count { !it.done }} open") }
    val all = sections.flatMap { it.tasks }
    val done = all.count { it.done }
    return Screen42State(
        sections = counted,
        summary = "$done of ${all.size} tasks completed",
        progress = done.toDouble() / all.size,
        progressText = "${done * 100 / all.size}%",
        footer = "${all.count { it.overdue && !it.done }} overdue · ${all.count { it.priority == 3 && !it.done }} high priority",
    )
}

class Screen42ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        screen42Build(
            listOf(
                Screen42Section(
                    1, "Today", "sun.max.fill", "", listOf(
                        Screen42Task(1, "Review pull request #482", "Runtime diffing for lists", "9:30 AM", false, true, 3, "Work", screen42Blue),
                        Screen42Task(2, "Call the dentist", "Reschedule cleaning", "Overdue · 8:00 AM", true, false, 2, "Personal", screen42Green),
                        Screen42Task(3, "Write release notes", "Version 2.4 highlights", "2:00 PM", false, false, 3, "Work", screen42Blue),
                        Screen42Task(4, "Pick up groceries", "Milk, eggs, basil, lemons", "6:30 PM", false, false, 1, "Errands", screen42Orange),
                    )
                ),
                Screen42Section(
                    2, "Upcoming", "calendar", "", listOf(
                        Screen42Task(5, "Plan sprint 19", "Capacity and goals", "Tomorrow", false, false, 2, "Work", screen42Blue),
                        Screen42Task(6, "Book flights to Lisbon", "Window seat, morning", "Fri, Nov 20", false, false, 1, "Travel", screen42Purple),
                        Screen42Task(7, "Renew passport", "Photos already taken", "Mon, Nov 23", false, true, 2, "Personal", screen42Green),
                    )
                ),
                Screen42Section(
                    3, "Someday", "tray.fill", "", listOf(
                        Screen42Task(8, "Learn Metal shaders", "Chapter 3 onwards", "No date", false, false, 0, "Learning", screen42Purple),
                        Screen42Task(9, "Organize photo library", "2019 – 2021", "No date", false, false, 0, "Personal", screen42Green),
                    )
                ),
            )
        )
    )
    val state: StateFlow<Screen42State> = _state

    fun toggle(id: Int) = _state.update { s ->
        screen42Build(s.sections.map { sec -> sec.copy(tasks = sec.tasks.map { if (it.id == id) it.copy(done = !it.done) else it }) })
    }

    private var tick = 0

    override fun mutate() {
        tick += 1
        toggle(listOf(2, 3, 5, 8, 4)[tick % 5])
    }
}

private fun screen42FlagColor(priority: Int): Color = when (priority) {
    3 -> Color.red
    2 -> Color.orange
    1 -> Color.blue
    else -> Color.gray
}

@Composable
fun Screen42(vm: Screen42ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Tasks")) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0), alignment = HorizontalAlignment.leading, spacing = 10.0) {
                    HStack {
                        Label(title = "Progress", systemImage = "chart.bar.fill", modifier = Modifier.font(Font.headline).foregroundStyle(Color.indigo))
                        Spacer()
                        Text(state.progressText, modifier = Modifier.font(Font.title2).bold().monospacedDigit())
                    }
                    ProgressView(value = state.progress, modifier = Modifier.tint(Color.indigo))
                    Text(state.summary, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                }
            }
            for (section in state.sections) key(section.id) {
                Section(header = {
                    HStack {
                        Label(title = section.title, systemImage = section.icon)
                        Spacer()
                        Text(section.count, modifier = Modifier.font(Font.caption).monospacedDigit())
                    }
                }, content = {
                    for (task in section.tasks) key(task.id) {
                        HStack(alignment = VerticalAlignment.top, spacing = 12.0) {
                            Image(systemName = 
                                if (task.done) "checkmark.circle.fill" else "circle",
                                modifier = Modifier.font(Font.title2).foregroundStyle(if (task.done) Color.green else Color.gray),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                Text(
                                    task.title,
                                    modifier = Modifier.font(Font.body).strikethrough(isActive = task.done, color = Color.gray)
                                        .foregroundStyle(if (task.done) ShapeStyle.secondary else ShapeStyle.primary),
                                )
                                Text(task.notes, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
                                HStack(spacing = 8.0) {
                                    Label(
                                        title = task.due,
                                        systemImage = "clock",
                                        modifier = Modifier.font(Font.caption2).foregroundStyle(if (task.overdue) Color.red else Color.secondary),
                                    )
                                    Text(
                                        task.tag,
                                        modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold)
                                            .foregroundStyle(task.tagTint.color())
                                            .padding(Edge.Set.horizontal, 6.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(task.tagTint.color().opacity(0.15), Capsule()),
                                    )
                                }
                            }
                            Spacer()
                            if (task.priority > 0) {
                                Image(systemName = "flag.fill", modifier = Modifier.font(Font.caption).foregroundStyle(screen42FlagColor(task.priority)))
                            }
                        }
                    }
                })
            }
            Section {
                HStack {
                    Image(systemName = "exclamationmark.triangle.fill", modifier = Modifier.foregroundStyle(Color.orange))
                    Text(state.footer, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                }
            }
        }
    }
}

val screen42 = BenchScreen("42 · Task manager", ::Screen42ViewModel) { Screen42(it as Screen42ViewModel) }
