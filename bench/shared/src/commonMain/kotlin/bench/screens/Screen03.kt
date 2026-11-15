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

// Screen 03 — Analytics dashboard: period picker, 2-column KPI grid with gradients, gauges, channel bars.

data class Screen03Kpi(
    val id: Int,
    val title: String,
    val value: String,
    val delta: String,
    val up: Boolean,
    val icon: String,
    val from: Rgb,
    val to: Rgb,
    val progress: Double,
)

data class Screen03Gauge(val id: Int, val label: String, val value: Double, val display: String, val tint: Rgb)

data class Screen03Channel(val id: Int, val name: String, val share: Double, val display: String, val tint: Rgb)

data class Screen03State(
    val period: Int,
    val periods: List<String>,
    val kpis: List<Screen03Kpi>,
    val gauges: List<Screen03Gauge>,
    val channels: List<Screen03Channel>,
    val updated: String,
)

class Screen03ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen03State(
            period = 1,
            periods = listOf("Day", "Week", "Month", "Year"),
            kpis = listOf(
                Screen03Kpi(1, "Revenue", "$48.2k", "+12.4%", true, "dollarsign.circle.fill", Rgb(0.2, 0.55, 1.0), Rgb(0.35, 0.3, 0.95), 0.72),
                Screen03Kpi(2, "Visitors", "128,940", "+4.1%", true, "person.3.fill", Rgb(0.1, 0.75, 0.6), Rgb(0.05, 0.55, 0.75), 0.58),
                Screen03Kpi(3, "Bounce rate", "38.6%", "-2.3%", false, "arrow.uturn.left.circle.fill", Rgb(1.0, 0.55, 0.25), Rgb(0.95, 0.3, 0.35), 0.39),
                Screen03Kpi(4, "Conversion", "3.42%", "+0.6%", true, "cart.fill", Rgb(0.7, 0.35, 0.95), Rgb(0.45, 0.25, 0.85), 0.34),
                Screen03Kpi(5, "Sessions", "212,400", "+8.9%", true, "waveform.path.ecg", Rgb(0.95, 0.3, 0.55), Rgb(0.75, 0.2, 0.6), 0.81),
                Screen03Kpi(6, "Avg. time", "4m 12s", "-0.4%", false, "clock.fill", Rgb(0.3, 0.6, 0.85), Rgb(0.2, 0.4, 0.65), 0.46),
            ),
            gauges = listOf(
                Screen03Gauge(1, "CPU", 0.64, "64", Rgb(0.2, 0.7, 0.4)),
                Screen03Gauge(2, "Memory", 0.82, "82", Rgb(1.0, 0.6, 0.0)),
                Screen03Gauge(3, "Disk", 0.37, "37", Rgb(0.0, 0.48, 1.0)),
            ),
            channels = listOf(
                Screen03Channel(1, "Organic search", 0.46, "46%", Rgb(0.0, 0.48, 1.0)),
                Screen03Channel(2, "Direct", 0.24, "24%", Rgb(0.2, 0.78, 0.35)),
                Screen03Channel(3, "Social", 0.18, "18%", Rgb(1.0, 0.18, 0.33)),
                Screen03Channel(4, "Referral", 0.12, "12%", Rgb(0.69, 0.32, 0.87)),
            ),
            updated = "Last sync 09:41",
        )
    )
    val state: StateFlow<Screen03State> = _state

    fun setPeriod(value: Int) = _state.update { it.copy(period = value) }

    override fun mutate() = _state.update { s ->
        tick++
        val cpu = (40 + (tick * 7) % 55)
        s.copy(
            period = tick % 4,
            gauges = s.gauges.map { if (it.id == 1) it.copy(value = cpu / 100.0, display = cpu.toString()) else it },
            kpis = s.kpis.map {
                if (it.id == 2) it.copy(value = "128,${(940 + tick).toString().padStart(3, '0')}", progress = (58 + tick % 40) / 100.0) else it
            },
            updated = "Last sync 09:${(41 + tick % 18).toString().padStart(2, '0')}",
        )
    }
}

@Composable
fun Screen03(vm: Screen03ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Analytics")) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0, modifier = Modifier.padding()) {
                Picker(title = "Period", selection = state.period, onSelectionChange = { vm.setPeriod(it as Int) }, modifier = Modifier.pickerStyle(PickerStyle.segmented)) {
                    for ((index, label) in state.periods.withIndex()) key(index) {
                        Text(label, modifier = Modifier.tag(index))
                    }
                }
                LazyVGrid(columns = listOf(GridItem(spacing = 12.0), GridItem(spacing = 12.0)), spacing = 12.0) {
                    for (kpi in state.kpis) key(kpi.id) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 8.0, modifier = Modifier.padding(14.0)
                            .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                            .background(
                                LinearGradient(colors = listOf(kpi.from.color(), kpi.to.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                                RoundedRectangle(cornerRadius = 16.0),
                            )) {
                            HStack {
                                Image(systemName = kpi.icon, modifier = Modifier.font(Font.title3).foregroundStyle(Color.white))
                                Spacer()
                                HStack(spacing = 2.0, modifier = Modifier.font(Font.caption2.weight(Font.Weight.bold))
                                    .foregroundStyle(Color.white)
                                    .padding(edges = Edge.Set.horizontal, length = 6.0)
                                    .padding(edges = Edge.Set.vertical, length = 3.0)
                                    .background(Color.white.opacity(0.22), Capsule())) {
                                    Image(systemName = if (kpi.up) "arrow.up.right" else "arrow.down.right")
                                    Text(verbatim = kpi.delta)
                                }
                            }
                            Text(verbatim = kpi.value, modifier = Modifier.font(Font.title2.bold()).foregroundStyle(Color.white).monospacedDigit())
                            Text(kpi.title, modifier = Modifier.font(Font.caption).foregroundStyle(Color.white.opacity(0.85)))
                            ProgressView(value = kpi.progress, modifier = Modifier.tint(Color.white))
                        }
                    }
                }
                Text("System health", modifier = Modifier.font(Font.headline))
                HStack(spacing = 0.0, modifier = Modifier.padding(edges = Edge.Set.vertical, length = 12.0)
                    .background(Color.gray.opacity(0.12), RoundedRectangle(cornerRadius = 16.0))) {
                    for (gauge in state.gauges) key(gauge.id) {
                        VStack(spacing = 6.0, modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY)) {
                            Gauge(
                                value = gauge.value,
                                bounds = 0.0..1.0,
                                modifier = Modifier.gaugeStyle(GaugeStyle.accessoryCircularCapacity).tint(gauge.tint.color()),
                                label = { Text(gauge.label) },
                                currentValueLabel = { Text(gauge.display) },
                            )
                            Text(gauge.label, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
                Text("Traffic sources", modifier = Modifier.font(Font.headline))
                VStack(spacing = 10.0, modifier = Modifier.padding().background(Color.gray.opacity(0.12), RoundedRectangle(cornerRadius = 16.0))) {
                    for (channel in state.channels) key(channel.id) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                            HStack {
                                ShapeView(Circle(), fill = channel.tint.color(), modifier = Modifier.frame(width = 8.0, height = 8.0))
                                Text(channel.name, modifier = Modifier.font(Font.subheadline))
                                Spacer()
                                Text(verbatim = channel.display, modifier = Modifier.font(Font.subheadline.monospacedDigit()).foregroundStyle(ShapeStyle.secondary))
                            }
                            ProgressView(value = channel.share, modifier = Modifier.tint(channel.tint.color()))
                        }
                    }
                }
                Text(state.updated, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary).frame(maxWidth = Double.POSITIVE_INFINITY))
            }
        }
    }
}

val screen03 = BenchScreen("03 · Analytics dashboard", ::Screen03ViewModel) { Screen03(it as Screen03ViewModel) }
