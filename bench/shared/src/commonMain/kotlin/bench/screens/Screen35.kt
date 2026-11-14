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

// Screen 35 — Online course: course header with progress, modules as DisclosureGroups, lesson rows with lock/check icons.

data class Screen35Lesson(val id: Int, val title: String, val duration: String, val kind: String, val done: Boolean, val locked: Boolean)

data class Screen35Module(
    val id: Int,
    val title: String,
    val count: String,
    val progress: Double,
    val expanded: Boolean,
    val lessons: List<Screen35Lesson>,
)

data class Screen35State(
    val course: String,
    val instructor: String,
    val progressLabel: String,
    val progress: Double,
    val accent: Rgb,
    val modules: List<Screen35Module>,
)

private fun screen35Build(modules: List<Screen35Module>): Screen35State {
    val all = modules.flatMap { it.lessons }
    val done = all.count { it.done }
    return Screen35State(
        course = "SwiftUI Foundations",
        instructor = "with Maya Patel · 4h 20m",
        progressLabel = "$done of ${all.size} lessons completed",
        progress = done.toDouble() / all.size,
        accent = Rgb(0.95, 0.4, 0.25),
        modules = modules.map { m ->
            val d = m.lessons.count { it.done }
            m.copy(count = "$d/${m.lessons.size}", progress = d.toDouble() / m.lessons.size)
        },
    )
}

private fun screen35Module(id: Int, title: String, expanded: Boolean, lessons: List<Triple<String, String, Int>>, doneCount: Int, locked: Boolean) =
    Screen35Module(
        id = id,
        title = title,
        count = "",
        progress = 0.0,
        expanded = expanded,
        lessons = lessons.mapIndexed { i, (t, d, k) ->
            Screen35Lesson(id * 10 + i, t, d, listOf("play.rectangle.fill", "doc.text.fill", "pencil.and.list.clipboard")[k], i < doneCount, locked)
        },
    )

class Screen35ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        screen35Build(
            listOf(
                screen35Module(1, "1. Getting Started", true, listOf(Triple("Welcome", "3 min", 0), Triple("Project setup", "8 min", 0), Triple("Reading guide", "5 min", 1)), 3, false),
                screen35Module(2, "2. Layout Basics", true, listOf(Triple("Stacks", "12 min", 0), Triple("Spacers & frames", "10 min", 0), Triple("Grids", "14 min", 0), Triple("Quiz", "6 min", 2)), 1, false),
                screen35Module(3, "3. State & Data Flow", false, listOf(Triple("@State", "9 min", 0), Triple("Bindings", "11 min", 0), Triple("Observable", "15 min", 0)), 0, false),
                screen35Module(4, "4. Navigation", false, listOf(Triple("Stacks & links", "13 min", 0), Triple("Sheets", "7 min", 0)), 0, true),
            ),
        ),
    )
    val state: StateFlow<Screen35State> = _state
    private var tick = 0

    fun setExpanded(id: Int, value: Boolean) = _state.update { s ->
        s.copy(modules = s.modules.map { if (it.id == id) it.copy(expanded = value) else it })
    }

    override fun mutate() = _state.update { s ->
        // Toggles the completion of a lesson in module 2.
        val target = 21 + tick++ % 3
        screen35Build(s.modules.map { m -> m.copy(lessons = m.lessons.map { if (it.id == target) it.copy(done = !it.done) else it }) })
    }
}

@Composable
fun Screen35(vm: Screen35ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Course")) {
            Section {
                HStack(spacing = 14.0) {
                    Image(systemName = 
                        "swift",
                        modifier = Modifier.font(Font.system(size = 28.0, weight = Font.Weight.bold))
                            .foregroundStyle(Color.white)
                            .frame(width = 60.0, height = 60.0)
                            .background(
                                LinearGradient(colors = listOf(state.accent.color(), Color.pink), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                                RoundedRectangle(cornerRadius = 14.0),
                            ),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text(verbatim = state.course, modifier = Modifier.font(Font.headline))
                        Text(verbatim = state.instructor, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    ProgressView(value = state.progress, modifier = Modifier.tint(state.accent.color()))
                    Text(verbatim = state.progressLabel, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                }
            }
            Section(title = "Curriculum") {
                for (module in state.modules) key(module.id) {
                    DisclosureGroup(
                        isExpanded = module.expanded,
                        onIsExpandedChange = { vm.setExpanded(module.id, it) },
                        content = {
                            for (lesson in module.lessons) key(lesson.id) {
                                HStack(spacing = 12.0) {
                                    Image(systemName = lesson.kind, modifier = Modifier.foregroundStyle(ShapeStyle.secondary).frame(width = 22.0))
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 1.0) {
                                        Text(verbatim = lesson.title, modifier = Modifier.font(Font.subheadline).foregroundStyle(if (lesson.locked) ShapeStyle.secondary else ShapeStyle.primary))
                                        Text(verbatim = lesson.duration, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.tertiary))
                                    }
                                    Spacer()
                                    Image(systemName = 
                                        when {
                                            lesson.locked -> "lock.fill"
                                            lesson.done -> "checkmark.circle.fill"
                                            else -> "circle"
                                        },
                                        modifier = Modifier.foregroundStyle(
                                            when {
                                                lesson.locked -> Color.gray
                                                lesson.done -> Color.green
                                                else -> Color.gray.opacity(0.4)
                                            },
                                        ),
                                    )
                                }
                            }
                        },
                        label = {
                            HStack(spacing = 10.0) {
                                VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                    Text(verbatim = module.title, modifier = Modifier.font(Font.body).fontWeight(Font.Weight.semibold))
                                    ProgressView(value = module.progress, modifier = Modifier.tint(state.accent.color()).frame(width = 120.0))
                                }
                                Spacer()
                                Text(verbatim = module.count, modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(ShapeStyle.secondary).padding(Edge.Set.horizontal, 8.0).padding(Edge.Set.vertical, 3.0).background(Color.gray.opacity(0.15), Capsule()))
                            }
                        },
                    )
                }
            }
        }
    }
}

val screen35 = BenchScreen("35 · Online course", ::Screen35ViewModel) { Screen35(it as Screen35ViewModel) }
