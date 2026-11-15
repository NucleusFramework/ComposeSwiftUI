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

// Screen 48 — EV car dashboard: battery Gauge and range, quick stats grid, climate controls with Stepper and Toggles, charging details.

data class Screen48Stat(val id: Int, val icon: String, val value: String, val label: String, val tint: Rgb)

data class Screen48Control(val id: Int, val title: String, val icon: String, val on: Boolean)

data class Screen48State(
    val carName: String,
    val status: String,
    val battery: Double,
    val batteryText: String,
    val range: String,
    val chargeTime: String,
    val stats: List<Screen48Stat>,
    val cabinTemp: Int,
    val cabinText: String,
    val outsideText: String,
    val controls: List<Screen48Control>,
    val climateOn: Boolean,
    val chargeLimit: Double,
    val chargeLimitText: String,
    val charger: String,
    val power: String,
    val added: String,
)

private fun screen48Battery(pct: Int) = Triple(pct / 100.0, "$pct%", "${pct * 49 / 10} km")

class Screen48ViewModel : BenchViewModel() {
    private var pct = 64

    private val _state = MutableStateFlow(
        screen48Battery(pct).let { (b, bt, r) ->
            Screen48State(
                carName = "Model Aurora",
                status = "Charging · Parked at Home",
                battery = b,
                batteryText = bt,
                range = r,
                chargeTime = "1 h 20 min to 80%",
                stats = listOf(
                    Screen48Stat(1, "speedometer", "14.8", "kWh/100 km", Rgb(0.0, 0.48, 1.0)),
                    Screen48Stat(2, "road.lanes", "12,480", "Odometer km", Rgb(0.35, 0.34, 0.84)),
                    Screen48Stat(3, "tirepressure", "2.9 bar", "Tire pressure", Rgb(0.2, 0.78, 0.35)),
                    Screen48Stat(4, "lock.fill", "Locked", "Doors", Rgb(1.0, 0.58, 0.0)),
                ),
                cabinTemp = 21,
                cabinText = "21 °C",
                outsideText = "Outside 8 °C · Cloudy",
                controls = listOf(
                    Screen48Control(1, "Defrost", "windshield.front.and.heat.waves", false),
                    Screen48Control(2, "Seat heat", "carseat.left.and.heat.waves", true),
                    Screen48Control(3, "Wheel heat", "steeringwheel.and.heat.waves", false),
                    Screen48Control(4, "Fan", "fan.fill", true),
                ),
                climateOn = true,
                chargeLimit = 0.8,
                chargeLimitText = "80%",
                charger = "Home Wall Box",
                power = "11 kW · 16 A",
                added = "+186 km added",
            )
        }
    )
    val state: StateFlow<Screen48State> = _state

    fun setClimate(value: Boolean) = _state.update { it.copy(climateOn = value) }

    fun setCabin(value: Int) = _state.update { it.copy(cabinTemp = value, cabinText = "$value °C") }

    fun setControl(id: Int, value: Boolean) = _state.update { s ->
        s.copy(controls = s.controls.map { if (it.id == id) it.copy(on = value) else it })
    }

    private var tick = 0

    override fun mutate() {
        tick += 1
        pct = if (pct >= 100) 20 else pct + 1
        val (b, bt, r) = screen48Battery(pct)
        _state.update { it.copy(battery = b, batteryText = bt, range = r) }
        val control = _state.value.controls[tick % 4]
        setControl(control.id, !control.on)
        setCabin(16 + tick % 10)
    }
}

@Composable
fun Screen48(vm: Screen48ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.insetGrouped).navigationTitle(state.carName)) {
            Section {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 6.0), alignment = HorizontalAlignment.leading, spacing = 12.0) {
                    HStack {
                        Image(systemName = "car.side.fill", modifier = Modifier.font(Font.system(size = 40.0)).foregroundStyle(Color.teal))
                        Spacer()
                        Label(
                            title = state.status,
                            systemImage = "bolt.fill",
                            modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(Color.green)
                                .padding(Edge.Set.horizontal, 8.0).padding(Edge.Set.vertical, 4.0)
                                .background(Color.green.opacity(0.15), Capsule()),
                        )
                    }
                    HStack(alignment = VerticalAlignment.firstTextBaseline, spacing = 6.0) {
                        Text(state.range, modifier = Modifier.font(Font.system(size = 40.0, weight = Font.Weight.bold, design = Font.Design.rounded)).monospacedDigit())
                        Text("est. range", modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                    Gauge(
                        value = state.battery,
                        bounds = 0.0..1.0,
                        modifier = Modifier.gaugeStyle(GaugeStyle.linearCapacity).tint(Color.green),
                        label = { Label(title = "Battery", systemImage = "battery.75percent") },
                        currentValueLabel = { Text(state.batteryText, modifier = Modifier.monospacedDigit()) },
                        minimumValueLabel = { Text("0") },
                        maximumValueLabel = { Text("100") },
                    )
                    Text(state.chargeTime, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                }
            }
            Section {
                Grid(horizontalSpacing = 10.0, verticalSpacing = 10.0) {
                    for (rowIndex in 0 until 2) key(rowIndex) {
                        GridRow {
                            for (stat in state.stats.drop(rowIndex * 2).take(2)) key(stat.id) {
                                HStack(
                                    modifier = Modifier.padding(10.0)
                                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                                        .background(stat.tint.color().opacity(0.1), RoundedRectangle(cornerRadius = 12.0)),
                                    spacing = 10.0,
                                ) {
                                    Image(systemName = stat.icon, modifier = Modifier.font(Font.title3).foregroundStyle(stat.tint.color()).frame(width = 28.0))
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 1.0) {
                                        Text(stat.value, modifier = Modifier.font(Font.subheadline).bold().monospacedDigit())
                                        Text(stat.label, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Section(header = { Text("Climate") }, footer = { Text(state.outsideText) }, content = {
                Toggle(title = "Climate control", systemImage = "thermometer.medium", isOn = state.climateOn, onIsOnChange = vm::setClimate)
                Stepper(value = state.cabinTemp, bounds = 16..28, onValueChange = vm::setCabin) {
                    HStack {
                        Text("Cabin")
                        Spacer()
                        Text(state.cabinText, modifier = Modifier.font(Font.headline).monospacedDigit().foregroundStyle(Color.orange))
                    }
                }
                Grid(horizontalSpacing = 10.0, verticalSpacing = 10.0) {
                    for (rowIndex in 0 until 2) key(rowIndex) {
                        GridRow {
                            for (control in state.controls.drop(rowIndex * 2).take(2)) key(control.id) {
                                Toggle(
                                    title = control.title,
                                    systemImage = control.icon,
                                    isOn = control.on,
                                    modifier = Modifier.toggleStyle(ToggleStyle.button).font(Font.footnote).frame(maxWidth = Double.POSITIVE_INFINITY),
                                    onIsOnChange = { vm.setControl(control.id, it) },
                                )
                            }
                        }
                    }
                }
            })
            Section(title = "Charging") {
                ProgressView(value = state.chargeLimit, modifier = Modifier.tint(Color.green)) {
                    HStack {
                        Text("Charge limit")
                        Spacer()
                        Text(state.chargeLimitText, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                    }
                }
                LabeledContent(title = "Charger", value = state.charger)
                LabeledContent(title = "Power", value = state.power)
                LabeledContent(title = "This session") {
                    Text(state.added, modifier = Modifier.foregroundStyle(Color.green).monospacedDigit())
                }
            }
        }
    }
}

val screen48 = BenchScreen("48 · EV dashboard", ::Screen48ViewModel) { Screen48(it as Screen48ViewModel) }
