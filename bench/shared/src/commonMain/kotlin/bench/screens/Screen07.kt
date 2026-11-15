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

// Screen 07 — Calendar agenda: month grid (Grid/GridRow) with selection and event dots, agenda rows with color bars.

/** kind: 0 = other month, 1 = regular, 2 = today, 3 = selected. */
data class Screen07Day(val id: Int, val label: String, val kind: Int, val hasEvent: Boolean)

data class Screen07Week(val id: Int, val days: List<Screen07Day>)

data class Screen07Event(
    val id: Int,
    val title: String,
    val time: String,
    val location: String,
    val duration: String,
    val tint: Rgb,
    val attendees: String,
)

data class Screen07State(
    val month: String,
    val weekdays: List<String>,
    val weeks: List<Screen07Week>,
    val selectedTitle: String,
    val events: List<Screen07Event>,
)

private fun screen07Weeks(selected: Int): List<Screen07Week> {
    // November 2026 starts on a Sunday; the grid shows 5 weeks (35 cells), starting Monday Oct 26.
    val eventDays = setOf(3, 9, 12, 14, 18, 20, 25, 27)
    return (0 until 5).map { w ->
        Screen07Week(w, (0 until 7).map { d ->
            val index = w * 7 + d
            val day = index - 5
            if (day < 1 || day > 30) {
                val label = if (day < 1) (31 + day).toString() else (day - 30).toString()
                Screen07Day(index, label, 0, false)
            } else {
                val kind = when (day) {
                    selected -> 3
                    14 -> 2
                    else -> 1
                }
                Screen07Day(index, day.toString(), kind, day in eventDays)
            }
        })
    }
}

class Screen07ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen07State(
            month = "November 2026",
            weekdays = listOf("M", "T", "W", "T", "F", "S", "S"),
            weeks = screen07Weeks(18),
            selectedTitle = "Wednesday, November 18",
            events = listOf(
                Screen07Event(1, "Design review", "09:00 – 10:00", "Room 4B", "1h", Rgb(0.0, 0.48, 1.0), "6"),
                Screen07Event(2, "1:1 with Grace", "11:30 – 12:00", "Video call", "30m", Rgb(0.2, 0.78, 0.35), "2"),
                Screen07Event(3, "Lunch & learn: SwiftUI", "12:30 – 13:30", "Cafeteria", "1h", Rgb(1.0, 0.58, 0.0), "24"),
                Screen07Event(4, "Sprint planning", "15:00 – 16:30", "Main hall", "1h 30m", Rgb(0.69, 0.32, 0.87), "11"),
                Screen07Event(5, "Gym", "18:00 – 19:00", "Downtown", "1h", Rgb(1.0, 0.18, 0.33), "1"),
            ),
        )
    )
    val state: StateFlow<Screen07State> = _state

    override fun mutate() = _state.update { s ->
        tick++
        val day = 16 + tick % 7
        s.copy(
            weeks = screen07Weeks(day),
            selectedTitle = "November $day",
            events = s.events.map { if (it.id == 1) it.copy(attendees = (6 + tick % 5).toString()) else it },
        )
    }
}

@Composable
fun Screen07(vm: Screen07ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Calendar").toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline)) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 14.0, modifier = Modifier.padding()) {
                HStack {
                    Text(state.month, modifier = Modifier.font(Font.title2.bold()))
                    Spacer()
                    HStack(spacing = 18.0, modifier = Modifier.font(Font.headline).foregroundStyle(Color.red)) {
                        Image(systemName = SFSymbol.chevronLeft)
                        Image(systemName = SFSymbol.chevronRight)
                    }
                }
                Grid(horizontalSpacing = 4.0, verticalSpacing = 6.0) {
                    GridRow {
                        for ((index, label) in state.weekdays.withIndex()) key(index) {
                            Text(label, modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).foregroundStyle(ShapeStyle.secondary).frame(maxWidth = Double.POSITIVE_INFINITY))
                        }
                    }
                    for (week in state.weeks) key(week.id) {
                        GridRow {
                            for (day in week.days) key(day.id) {
                                Text(
                                    day.label,
                                    modifier = Modifier.font(Font.callout)
                                        .fontWeight(if (day.kind >= 2) Font.Weight.bold else Font.Weight.regular)
                                        .foregroundStyle(
                                            when (day.kind) {
                                                0 -> Color.secondary
                                                2 -> Color.red
                                                3 -> Color.white
                                                else -> Color.primary
                                            }
                                        )
                                        .frame(width = 36.0, height = 36.0)
                                        .background(if (day.kind == 3) Color.red else Color.clear, Circle())
                                        .overlay(alignment = Alignment.bottom) {
                                            ShapeView(
                                                Circle(),
                                                fill = if (day.hasEvent) Color.gray else Color.clear,
                                                modifier = Modifier.frame(width = 4.0, height = 4.0).offset(y = 5.0),
                                            )
                                        }
                                        .frame(maxWidth = Double.POSITIVE_INFINITY),
                                )
                            }
                        }
                    }
                }
                Divider()
                Text(state.selectedTitle, modifier = Modifier.font(Font.headline))
                for (event in state.events) key(event.id) {
                    HStack(spacing = 12.0, modifier = Modifier.padding(10.0).background(event.tint.color().opacity(0.12), RoundedRectangle(cornerRadius = 10.0))) {
                        ShapeView(RoundedRectangle(cornerRadius = 2.0), fill = event.tint.color(), modifier = Modifier.frame(width = 4.0))
                        VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                            Text(event.title, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)))
                            Label(title = event.time, systemImage = SFSymbol.clock, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            Label(title = event.location, systemImage = SFSymbol.mappinAndEllipse, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                        Spacer()
                        VStack(alignment = HorizontalAlignment.trailing, spacing = 6.0) {
                            Text(event.duration, modifier = Modifier.font(Font.caption.bold()).foregroundStyle(event.tint.color()))
                            HStack(spacing = 2.0, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary)) {
                                Image(systemName = SFSymbol.person2Fill)
                                Text(event.attendees)
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen07 = BenchScreen("07 · Calendar agenda", ::Screen07ViewModel) { Screen07(it as Screen07ViewModel) }
