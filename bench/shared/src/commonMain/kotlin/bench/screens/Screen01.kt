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

// Screen 01 — Profile & settings: header card, stats row, settings sections with icons and toggles.

data class Screen01Stat(val label: String, val value: String)

data class Screen01Row(val id: Int, val title: String, val icon: String, val tint: Rgb, val detail: String)

data class Screen01State(
    val name: String,
    val handle: String,
    val initials: String,
    val stats: List<Screen01Stat>,
    val notifications: Boolean,
    val syncEnabled: Boolean,
    val rows: List<Screen01Row>,
    val footer: String,
)

class Screen01ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen01State(
            name = "Ada Lovelace",
            handle = "@ada · Analytical Engine",
            initials = "AL",
            stats = listOf(Screen01Stat("Posts", "128"), Screen01Stat("Followers", "4.2k"), Screen01Stat("Following", "312")),
            notifications = true,
            syncEnabled = false,
            rows = listOf(
                Screen01Row(1, "Account", "person.crop.circle", Rgb(0.0, 0.48, 1.0), "Personal"),
                Screen01Row(2, "Privacy", "hand.raised.fill", Rgb(0.2, 0.78, 0.35), "Strict"),
                Screen01Row(3, "Storage", "internaldrive.fill", Rgb(1.0, 0.58, 0.0), "12.4 GB"),
                Screen01Row(4, "Appearance", "paintpalette.fill", Rgb(0.69, 0.32, 0.87), "Automatic"),
                Screen01Row(5, "Language", "globe", Rgb(0.35, 0.34, 0.84), "English"),
            ),
            footer = "Version 1.0 (42)",
        )
    )
    val state: StateFlow<Screen01State> = _state

    fun setNotifications(value: Boolean) = _state.update { it.copy(notifications = value) }
    fun setSync(value: Boolean) = _state.update { it.copy(syncEnabled = value) }

    override fun mutate() = _state.update {
        val posts = it.stats[0].value.toInt() + 1
        it.copy(stats = listOf(it.stats[0].copy(value = posts.toString())) + it.stats.drop(1), notifications = !it.notifications)
    }
}

@Composable
fun Screen01(vm: Screen01ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Profile")) {
            Section {
                HStack(spacing = 16.0) {
                    ZStack(modifier = Modifier.frame(width = 64.0, height = 64.0)) {
                        ShapeView(Circle(), fill = Color(red = 0.0, green = 0.48, blue = 1.0))
                        Text(state.initials, modifier = Modifier.font(Font.title2).fontWeight(Font.Weight.bold).foregroundStyle(Color.white))
                    }
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text(state.name, modifier = Modifier.font(Font.headline))
                        Text(state.handle, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                }
                HStack {
                    for (stat in state.stats) key(stat.label) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 2.0) {
                            Text(stat.value, modifier = Modifier.font(Font.title3).bold().monospacedDigit())
                            Text(stat.label, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
            }
            Section(title = "Preferences") {
                Toggle(title = "Notifications", isOn = state.notifications, onIsOnChange = vm::setNotifications)
                Toggle(title = "iCloud Sync", isOn = state.syncEnabled, onIsOnChange = vm::setSync)
            }
            Section(header = { Text("General") }, footer = { Text(state.footer) }, content = {
                for (row in state.rows) key(row.id) {
                    HStack(spacing = 12.0) {
                        ImageSystemName(
                            row.icon,
                            modifier = Modifier.font(Font.system(size = 15.0, weight = Font.Weight.semibold))
                                .foregroundStyle(Color.white)
                                .frame(width = 30.0, height = 30.0)
                                .background(row.tint.color(), RoundedRectangle(cornerRadius = 7.0)),
                        )
                        Text(row.title)
                        Spacer()
                        Text(row.detail, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                    }
                }
            })
        }
    }
}

val screen01 = BenchScreen("01 · Profile & settings", ::Screen01ViewModel) { Screen01(it as Screen01ViewModel) }
