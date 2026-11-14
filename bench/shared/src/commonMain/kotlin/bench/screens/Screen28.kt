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

// Screen 28 — Smart home: scene chips, thermostat Slider, Grid of device tiles with Toggles.

data class Screen28Scene(val id: Int, val name: String, val icon: String, val bg: Rgb, val fg: Rgb)

data class Screen28Device(
    val id: Int,
    val name: String,
    val room: String,
    val icon: String,
    val on: Boolean,
    val status: String,
    val tint: Rgb,
    val iconBg: Rgb,
    val tileBg: Rgb,
)

data class Screen28Row(val id: Int, val left: Screen28Device, val right: Screen28Device)

data class Screen28State(
    val greeting: String,
    val summary: String,
    val scenes: List<Screen28Scene>,
    val temperature: Double,
    val temperatureText: String,
    val humidity: String,
    val rows: List<Screen28Row>,
)

private data class Screen28Raw(val id: Int, val name: String, val room: String, val icon: String, val tint: Rgb, val on: Boolean, val onText: String)

private fun screen28State(raw: List<Screen28Raw>, scene: Int, temperature: Double): Screen28State {
    val names = listOf("Morning" to "sunrise.fill", "Away" to "figure.walk", "Movie" to "film.fill", "Night" to "moon.stars.fill")
    val devices = raw.map {
        Screen28Device(
            id = it.id,
            name = it.name,
            room = it.room,
            icon = it.icon,
            on = it.on,
            status = if (it.on) it.onText else "Off",
            tint = it.tint,
            iconBg = if (it.on) it.tint else Rgb(0.88, 0.88, 0.9),
            tileBg = if (it.on) Rgb(1.0, 1.0, 1.0) else Rgb(0.95, 0.95, 0.97),
        )
    }
    val tenths = (temperature * 10).toInt()
    return Screen28State(
        greeting = "Good evening",
        summary = "${raw.count { it.on }} of ${raw.size} devices on",
        scenes = names.mapIndexed { i, (n, icon) ->
            Screen28Scene(i, n, icon, if (i == scene) Rgb(0.0, 0.48, 1.0) else Rgb(0.9, 0.9, 0.93), if (i == scene) Rgb(1.0, 1.0, 1.0) else Rgb(0.2, 0.2, 0.22))
        },
        temperature = temperature,
        temperatureText = "${tenths / 10}.${tenths % 10}°",
        humidity = "Humidity 46%",
        rows = devices.chunked(2).mapIndexed { i, pair -> Screen28Row(i, pair[0], pair[1]) },
    )
}

class Screen28ViewModel : BenchViewModel() {
    private val raw = mutableListOf(
        Screen28Raw(1, "Ceiling Light", "Living room", "lightbulb.fill", Rgb(1.0, 0.75, 0.0), true, "80% brightness"),
        Screen28Raw(2, "Air Purifier", "Bedroom", "air.purifier.fill", Rgb(0.19, 0.69, 0.78), false, "Auto mode"),
        Screen28Raw(3, "Smart TV", "Living room", "tv.fill", Rgb(0.35, 0.34, 0.84), true, "Netflix"),
        Screen28Raw(4, "Front Door", "Entrance", "lock.fill", Rgb(0.2, 0.7, 0.35), true, "Locked"),
        Screen28Raw(5, "Speaker", "Kitchen", "hifispeaker.fill", Rgb(1.0, 0.18, 0.33), false, "Playing jazz"),
        Screen28Raw(6, "Fan", "Office", "fan.fill", Rgb(0.0, 0.48, 1.0), true, "Speed 2"),
    )
    private var scene = 2
    private var temperature = 21.5
    private var tick = 0
    private val _state = MutableStateFlow(screen28State(raw, scene, temperature))
    val state: StateFlow<Screen28State> = _state

    fun setDevice(id: Int, on: Boolean) {
        val i = raw.indexOfFirst { it.id == id }
        raw[i] = raw[i].copy(on = on)
        _state.update { screen28State(raw, scene, temperature) }
    }

    fun setTemperature(value: Double) {
        temperature = value
        _state.update { screen28State(raw, scene, temperature) }
    }

    override fun mutate() {
        tick++
        val i = tick % raw.size
        raw[i] = raw[i].copy(on = !raw[i].on)
        scene = tick % 4
        temperature = 18.0 + (tick % 9)
        _state.update { screen28State(raw, scene, temperature) }
    }
}

@Composable
private fun Screen28Tile(device: Screen28Device, vm: Screen28ViewModel) {
    VStack(
        modifier = Modifier.padding(14.0)
            .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
            .background(device.tileBg.color(), RoundedRectangle(cornerRadius = 18.0))
            .shadow(color = Color.black.opacity(0.06), radius = 4.0, y = 2.0),
        alignment = HorizontalAlignment.leading,
        spacing = 10.0,
    ) {
        HStack {
            ImageSystemName(
                device.icon,
                modifier = Modifier.font(Font.system(size = 18.0, weight = Font.Weight.semibold))
                    .foregroundStyle(Color.white)
                    .frame(width = 40.0, height = 40.0)
                    .background(device.iconBg.color(), Circle()),
            )
            Spacer()
            Toggle(
                title = device.name,
                isOn = device.on,
                modifier = Modifier.labelsHidden().toggleStyle(ToggleStyle.switch).tint(device.tint.color()),
                onIsOnChange = { vm.setDevice(device.id, it) },
            )
        }
        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
            Text(device.name, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).lineLimit(1))
            Text(device.room, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
        }
        Text(
            verbatim = device.status,
            modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.medium).foregroundStyle(if (device.on) device.tint.color() else Color.secondary),
        )
    }
}

@Composable
fun Screen28(vm: Screen28ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.background(Color(red = 0.95, green = 0.95, blue = 0.97)).navigationTitle("Home")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 18.0) {
                HStack {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                        Text(state.greeting, modifier = Modifier.font(Font.title2.bold()))
                        Text(state.summary, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                    Spacer()
                    ImageSystemName(
                        "house.lodge.fill",
                        modifier = Modifier.font(Font.title).symbolRenderingMode(SymbolRenderingMode.multicolor),
                    )
                }
                ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                    HStack(spacing = 8.0) {
                        for (scene in state.scenes) key(scene.id) {
                            Label(
                                scene.name,
                                systemImage = scene.icon,
                                modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium))
                                    .foregroundStyle(scene.fg.color())
                                    .padding(Edge.Set.horizontal, 14.0)
                                    .padding(Edge.Set.vertical, 8.0)
                                    .background(scene.bg.color(), Capsule()),
                            )
                        }
                    }
                }
                VStack(
                    modifier = Modifier.padding(16.0).background(Color.white, RoundedRectangle(cornerRadius = 18.0)),
                    alignment = HorizontalAlignment.leading,
                    spacing = 10.0,
                ) {
                    HStack {
                        Label("Thermostat", systemImage = "thermometer.medium", modifier = Modifier.font(Font.headline).foregroundStyle(Color.orange))
                        Spacer()
                        Text(state.temperatureText, modifier = Modifier.font(Font.system(size = 28.0, weight = Font.Weight.semibold, design = Font.Design.rounded)).monospacedDigit())
                    }
                    Slider(value = state.temperature, bounds = 16.0..28.0, step = 0.5, modifier = Modifier.tint(Color.orange), onValueChange = vm::setTemperature)
                    HStack {
                        Text("16°", modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                        Spacer()
                        Text(verbatim = state.humidity, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                        Spacer()
                        Text("28°", modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                    }
                }
                Text("Devices", modifier = Modifier.font(Font.headline))
                Grid(horizontalSpacing = 12.0, verticalSpacing = 12.0) {
                    for (row in state.rows) key(row.id) {
                        GridRow {
                            Screen28Tile(row.left, vm)
                            Screen28Tile(row.right, vm)
                        }
                    }
                }
            }
        }
    }
}

val screen28 = BenchScreen("28 · Smart home", ::Screen28ViewModel) { Screen28(it as Screen28ViewModel) }
