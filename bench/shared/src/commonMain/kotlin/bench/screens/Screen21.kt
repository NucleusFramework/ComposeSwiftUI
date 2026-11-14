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

// Screen 21 — Notification center: wallpaper gradient, focus banner, grouped notification stacks on materials.

data class Screen21Notification(val id: Int, val title: String, val text: String, val time: String)

data class Screen21Group(
    val id: Int,
    val app: String,
    val icon: String,
    val tint: Rgb,
    val notifications: List<Screen21Notification>,
    val moreLabel: String,
    val stacked: Boolean,
)

data class Screen21State(
    val clock: String,
    val date: String,
    val focus: String,
    val groups: List<Screen21Group>,
    val tick: Int,
)

private fun screen21Groups(tick: Int): List<Screen21Group> = listOf(
    Screen21Group(
        1, "Messages", "message.fill", Rgb(0.2, 0.78, 0.35),
        listOf(
            Screen21Notification(11, "Grace Hopper", "Found the bug — it was a moth in relay #70. Bringing it to the logbook now.", if (tick == 0) "now" else "${tick}m ago"),
            Screen21Notification(12, "Alan Turing", "Are we still on for chess at six?", "8m ago"),
        ),
        "${2 + tick % 4} more notifications", true,
    ),
    Screen21Group(
        2, "Calendar", "calendar", Rgb(1.0, 0.23, 0.19),
        listOf(Screen21Notification(21, "Design review", "Today at 11:00 · Room 4B · 3 attendees", "15m ago")),
        "", false,
    ),
    Screen21Group(
        3, "Fitness", "figure.run", Rgb(0.6, 0.85, 0.0),
        listOf(Screen21Notification(31, "Close your rings", "You're ${120 - tick % 60} kcal away from your Move goal. A brisk walk would do it!", "1h ago")),
        "1 more notification", true,
    ),
    Screen21Group(
        4, "Weather", "cloud.sun.fill", Rgb(0.0, 0.6, 0.95),
        listOf(Screen21Notification(41, "Rain expected", "Light rain starting around 14:00. Take an umbrella.", "2h ago")),
        "", false,
    ),
    Screen21Group(
        5, "Mail", "envelope.fill", Rgb(0.0, 0.48, 1.0),
        listOf(
            Screen21Notification(51, "Weekly digest", "12 new posts in Compose SwiftUI discussions.", "3h ago"),
            Screen21Notification(52, "Invoice #2026-114", "Your receipt from Cloud Hosting is attached.", "5h ago"),
        ),
        "", false,
    ),
)

class Screen21ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen21State(clock = "09:41", date = "Saturday 14 November", focus = "Do Not Disturb · until 10:00", groups = screen21Groups(0), tick = 0)
    )
    val state: StateFlow<Screen21State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(tick = t, groups = screen21Groups(t), clock = "09:${(41 + t % 19).toString().padStart(2, '0')}")
    }
}

@Composable
fun Screen21(vm: Screen21ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(
            modifier = Modifier.background(
                LinearGradient(
                    colors = listOf(Color(red = 0.25, green = 0.2, blue = 0.55), Color(red = 0.85, green = 0.4, blue = 0.5), Color(red = 1.0, green = 0.7, blue = 0.45)),
                    startPoint = UnitPoint.top,
                    endPoint = UnitPoint.bottom,
                ),
            ).navigationTitle("Notifications"),
        ) {
            VStack(modifier = Modifier.padding(Edge.Set.horizontal, 12.0).padding(Edge.Set.bottom, 20.0), spacing = 10.0) {
                VStack(modifier = Modifier.foregroundStyle(Color.white).padding(Edge.Set.vertical, 12.0), spacing = 0.0) {
                    Text(state.date, modifier = Modifier.font(Font.headline).opacity(0.9))
                    Text(state.clock, modifier = Modifier.font(Font.system(size = 72.0, weight = Font.Weight.semibold, design = Font.Design.rounded)).monospacedDigit())
                }
                HStack(
                    modifier = Modifier.padding(Edge.Set.horizontal, 14.0)
                        .padding(Edge.Set.vertical, 10.0)
                        .background(ShapeStyle.ultraThinMaterial, Capsule()),
                    spacing = 8.0,
                ) {
                    ImageSystemName("moon.fill", modifier = Modifier.foregroundStyle(Color.indigo))
                    Text(state.focus, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium)))
                }
                HStack(modifier = Modifier.padding(Edge.Set.top, 6.0)) {
                    Text("Notification Centre", modifier = Modifier.font(Font.title3.bold()).foregroundStyle(Color.white))
                    Spacer()
                    ImageSystemName(
                        "xmark",
                        modifier = Modifier.font(Font.caption.weight(Font.Weight.bold))
                            .frame(width = 28.0, height = 28.0)
                            .background(ShapeStyle.ultraThinMaterial, Circle()),
                    )
                }
                for (group in state.groups) key(group.id) {
                    VStack(spacing = 0.0) {
                        VStack(
                            modifier = Modifier.padding(14.0).background(ShapeStyle.regularMaterial, RoundedRectangle(cornerRadius = 22.0)),
                            alignment = HorizontalAlignment.leading,
                            spacing = 10.0,
                        ) {
                            HStack(spacing = 8.0) {
                                ImageSystemName(
                                    group.icon,
                                    modifier = Modifier.font(Font.system(size = 11.0, weight = Font.Weight.semibold))
                                        .foregroundStyle(Color.white)
                                        .frame(width = 22.0, height = 22.0)
                                        .background(group.tint.color(), RoundedRectangle(cornerRadius = 6.0)),
                                )
                                Text(group.app, modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).textCase(Text.Case.uppercase).foregroundStyle(ShapeStyle.secondary))
                                Spacer()
                            }
                            for ((index, item) in group.notifications.withIndex()) key(item.id) {
                                if (index > 0) {
                                    Divider()
                                }
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                    HStack(alignment = VerticalAlignment.firstTextBaseline) {
                                        Text(item.title, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)))
                                        Spacer()
                                        Text(item.time, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                    }
                                    Text(item.text, modifier = Modifier.font(Font.subheadline).lineLimit(2).frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading))
                                }
                            }
                            if (group.moreLabel.isNotEmpty()) {
                                Text(group.moreLabel, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                        if (group.stacked) {
                            ShapeView(
                                UnevenRoundedRectangle(bottomLeadingRadius = 14.0, bottomTrailingRadius = 14.0),
                                modifier = Modifier.frame(height = 8.0).padding(Edge.Set.horizontal, 14.0).opacity(0.8),
                                fill = ShapeStyle.thinMaterial,
                            )
                        }
                    }
                }
            }
        }
    }
}

val screen21 = BenchScreen("21 · Notification center", ::Screen21ViewModel) { Screen21(it as Screen21ViewModel) }
