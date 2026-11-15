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

// Screen 38 — Medication schedule: adherence summary, morning/noon/evening sections, two-tone pill capsules, taken toggles.

data class Screen38Dose(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String,
    val left: Rgb,
    val right: Rgb,
    val withFood: Boolean,
    val taken: Boolean,
)

data class Screen38Slot(val id: Int, val title: String, val icon: String, val tint: Rgb, val count: String, val doses: List<Screen38Dose>)

data class Screen38State(
    val day: String,
    val summary: String,
    val progress: Double,
    val nextDose: String,
    val slots: List<Screen38Slot>,
    val refill: String,
)

private fun screen38Build(day: String, slots: List<Screen38Slot>): Screen38State {
    val all = slots.flatMap { it.doses }
    val taken = all.count { it.taken }
    val next = all.firstOrNull { !it.taken }
    return Screen38State(
        day = day,
        summary = "$taken of ${all.size} doses taken",
        progress = taken.toDouble() / all.size,
        nextDose = if (next == null) "All done for today" else "Next: ${next.name} at ${next.time}",
        slots = slots.map { s -> s.copy(count = "${s.doses.count { it.taken }}/${s.doses.size}") },
        refill = "Vitamin D runs out in 5 days. Refill reminder on.",
    )
}

class Screen38ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        screen38Build(
            "Thursday, 14 November",
            listOf(
                Screen38Slot(
                    1, "Morning", "sunrise.fill", Rgb(1.0, 0.6, 0.0), "",
                    listOf(
                        Screen38Dose(11, "Lisinopril", "10 mg · 1 tablet", "08:00", Rgb(0.95, 0.3, 0.3), Rgb(1.0, 1.0, 1.0), false, true),
                        Screen38Dose(12, "Vitamin D", "1000 IU · 1 capsule", "08:00", Rgb(1.0, 0.8, 0.0), Rgb(1.0, 0.65, 0.0), true, true),
                        Screen38Dose(13, "Omega-3", "1 g · 2 capsules", "08:30", Rgb(0.95, 0.75, 0.3), Rgb(0.85, 0.55, 0.15), true, false),
                    ),
                ),
                Screen38Slot(
                    2, "Noon", "sun.max.fill", Rgb(0.95, 0.75, 0.0), "",
                    listOf(
                        Screen38Dose(21, "Metformin", "500 mg · 1 tablet", "12:30", Rgb(0.3, 0.55, 1.0), Rgb(0.85, 0.9, 1.0), true, false),
                    ),
                ),
                Screen38Slot(
                    3, "Evening", "moon.stars.fill", Rgb(0.4, 0.35, 0.85), "",
                    listOf(
                        Screen38Dose(31, "Atorvastatin", "20 mg · 1 tablet", "20:00", Rgb(0.6, 0.4, 0.9), Rgb(0.9, 0.85, 1.0), false, false),
                        Screen38Dose(32, "Magnesium", "250 mg · 1 tablet", "21:00", Rgb(0.2, 0.75, 0.6), Rgb(0.8, 0.95, 0.9), false, false),
                    ),
                ),
            ),
        ),
    )
    val state: StateFlow<Screen38State> = _state
    private val order = listOf(13, 21, 31, 32)
    private var tick = 0

    fun setTaken(id: Int, value: Boolean) = _state.update { s ->
        screen38Build(s.day, s.slots.map { slot -> slot.copy(doses = slot.doses.map { if (it.id == id) it.copy(taken = value) else it }) })
    }

    override fun mutate() {
        val id = order[tick++ % order.size]
        val current = _state.value.slots.flatMap { it.doses }.first { it.id == id }.taken
        setTaken(id, !current)
    }
}

@Composable
fun Screen38(vm: Screen38ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(
            modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle("Medications")
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) { Button(title = "Add", systemImage = "plus", action = {}) },
        ) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0), alignment = HorizontalAlignment.leading, spacing = 8.0) {
                    Text(verbatim = state.day, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    HStack {
                        Text(verbatim = state.summary, modifier = Modifier.font(Font.title3).fontWeight(Font.Weight.semibold))
                        Spacer()
                        Image(systemName = "pills.fill", modifier = Modifier.font(Font.title2).foregroundStyle(Color.mint).symbolRenderingMode(SymbolRenderingMode.hierarchical))
                    }
                    ProgressView(value = state.progress, modifier = Modifier.tint(Color.mint))
                    Label(state.nextDose, systemImage = "bell.badge.fill", modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                }
            }
            for (slot in state.slots) key(slot.id) {
                Section(header = {
                    HStack {
                        Label(slot.title, systemImage = slot.icon, modifier = Modifier.foregroundStyle(slot.tint.color()))
                        Spacer()
                        Text(verbatim = slot.count, modifier = Modifier.monospacedDigit())
                    }
                }, content = {
                    for (dose in slot.doses) key(dose.id) {
                        HStack(spacing = 14.0) {
                            HStack(
                                modifier = Modifier.frame(width = 34.0, height = 14.0).clipShape(Capsule())
                                    .background { ShapeView(Capsule(), stroke = Color.black.opacity(0.15), lineWidth = 1.0) }
                                    .rotationEffect(Angle.degrees(-35.0))
                                    .frame(width = 36.0, height = 36.0),
                                spacing = 0.0,
                            ) {
                                ShapeView(Rectangle(), fill = dose.left.color())
                                ShapeView(Rectangle(), fill = dose.right.color())
                            }
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(verbatim = dose.name, modifier = Modifier.font(Font.body).fontWeight(Font.Weight.medium).strikethrough(dose.taken, color = Color.secondary))
                                Text(verbatim = dose.dosage, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                HStack(modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.tertiary), spacing = 6.0) {
                                    Label(dose.time, systemImage = "clock")
                                    if (dose.withFood) {
                                        Label("With food", systemImage = "fork.knife")
                                    }
                                }
                            }
                            Spacer()
                            Toggle(title = "Taken", isOn = dose.taken, modifier = Modifier.labelsHidden().tint(Color.mint), onIsOnChange = { vm.setTaken(dose.id, it) })
                        }
                    }
                })
            }
            Section {
                Label(state.refill, systemImage = "exclamationmark.triangle.fill", modifier = Modifier.font(Font.footnote).foregroundStyle(Color.orange))
            }
        }
    }
}

val screen38 = BenchScreen("38 · Medication schedule", ::Screen38ViewModel) { Screen38(it as Screen38ViewModel) }
