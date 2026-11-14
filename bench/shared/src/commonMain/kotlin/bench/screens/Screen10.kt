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

// Screen 10 — Preferences: segmented and menu pickers, labelled sliders, toggles, labeled content.

data class Screen10Option(val id: Int, val label: String, val icon: String)

data class Screen10State(
    val theme: Int,
    val themes: List<Screen10Option>,
    val accent: Int,
    val accents: List<Screen10Option>,
    val accentTint: Rgb,
    val textSize: Double,
    val textSizeLabel: String,
    val previewSize: Double,
    val quality: Int,
    val qualities: List<Screen10Option>,
    val volume: Double,
    val volumeLabel: String,
    val autoplay: Boolean,
    val sound: Int,
    val sounds: List<Screen10Option>,
    val cache: String,
    val quietHours: String,
    val version: String,
)

private val screen10Tints = listOf(Rgb(0.0, 0.48, 1.0), Rgb(0.2, 0.78, 0.35), Rgb(1.0, 0.58, 0.0), Rgb(0.69, 0.32, 0.87))

class Screen10ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen10State(
            theme = 2,
            themes = listOf(Screen10Option(1, "Light", "sun.max"), Screen10Option(2, "Dark", "moon"), Screen10Option(3, "Auto", "circle.lefthalf.filled")),
            accent = 0,
            accents = listOf(
                Screen10Option(0, "Blue", "circle.fill"),
                Screen10Option(1, "Green", "circle.fill"),
                Screen10Option(2, "Orange", "circle.fill"),
                Screen10Option(3, "Purple", "circle.fill"),
            ),
            accentTint = screen10Tints[0],
            textSize = 0.5,
            textSizeLabel = "Medium",
            previewSize = 17.0,
            quality = 2,
            qualities = listOf(Screen10Option(1, "Data Saver", "leaf"), Screen10Option(2, "High", "wifi"), Screen10Option(3, "Lossless", "waveform")),
            volume = 0.7,
            volumeLabel = "70",
            autoplay = true,
            sound = 1,
            sounds = listOf(Screen10Option(1, "Chime", "bell"), Screen10Option(2, "Glass", "bell"), Screen10Option(3, "Pulse", "bell")),
            cache = "1.8 GB",
            quietHours = "22:00 – 07:00",
            version = "4.2.0 (318)",
        )
    )
    val state: StateFlow<Screen10State> = _state

    fun setTheme(value: Int) = _state.update { it.copy(theme = value) }
    fun setAccent(value: Int) = _state.update { it.copy(accent = value, accentTint = screen10Tints[value]) }
    fun setTextSize(value: Double) = _state.update { it.withTextSize(value) }
    fun setQuality(value: Int) = _state.update { it.copy(quality = value) }
    fun setVolume(value: Double) = _state.update { it.copy(volume = value, volumeLabel = (value * 100).toInt().toString()) }
    fun setAutoplay(value: Boolean) = _state.update { it.copy(autoplay = value) }
    fun setSound(value: Int) = _state.update { it.copy(sound = value) }

    override fun mutate() = _state.update { s ->
        tick++
        val accent = tick % 4
        val volume = (tick * 13 % 100) / 100.0
        s.copy(
            theme = 1 + tick % 3,
            accent = accent,
            accentTint = screen10Tints[accent],
            volume = volume,
            volumeLabel = (tick * 13 % 100).toString(),
            autoplay = tick % 2 == 0,
        ).withTextSize((tick % 5) / 4.0)
    }
}

private fun Screen10State.withTextSize(value: Double): Screen10State {
    val label = when {
        value < 0.25 -> "Small"
        value < 0.6 -> "Medium"
        value < 0.85 -> "Large"
        else -> "Extra Large"
    }
    return copy(textSize = value, textSizeLabel = label, previewSize = 13.0 + value * 12.0)
}

@Composable
fun Screen10(vm: Screen10ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        Form(modifier = Modifier.formStyle(FormStyle.grouped).navigationTitle("Preferences").tint(state.accentTint.color())) {
            Section(title = "Appearance") {
                Picker(title = "Theme", selection = state.theme, onSelectionChange = { vm.setTheme(it as Int) }, modifier = Modifier.pickerStyle(PickerStyle.segmented)) {
                    for (option in state.themes) key(option.id) {
                        Label(title = option.label, systemImage = option.icon, modifier = Modifier.tag(option.id))
                    }
                }
                Picker(title = "Accent color", selection = state.accent, onSelectionChange = { vm.setAccent(it as Int) }, modifier = Modifier.pickerStyle(PickerStyle.menu)) {
                    for (option in state.accents) key(option.id) {
                        Text(option.label, modifier = Modifier.tag(option.id))
                    }
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    HStack {
                        Text("Text size")
                        Spacer()
                        Text(state.textSizeLabel, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                    }
                    Slider(
                        value = state.textSize,
                        bounds = 0.0..1.0,
                        onValueChange = vm::setTextSize,
                        label = { Text("Text size") },
                        minimumValueLabel = { ImageSystemName("textformat.size.smaller") },
                        maximumValueLabel = { ImageSystemName("textformat.size.larger") },
                    )
                }
                LabeledContent(title = "Preview") {
                    Text("Aa", modifier = Modifier.font(Font.system(size = state.previewSize, weight = Font.Weight.semibold)).foregroundStyle(state.accentTint.color()))
                }
            }
            Section(header = { Text("Playback") }, footer = { Text("Lossless streaming uses significantly more data.") }, content = {
                Picker(title = "Streaming quality", selection = state.quality, onSelectionChange = { vm.setQuality(it as Int) }, modifier = Modifier.pickerStyle(PickerStyle.menu)) {
                    for (option in state.qualities) key(option.id) {
                        Label(title = option.label, systemImage = option.icon, modifier = Modifier.tag(option.id))
                    }
                }
                HStack(spacing = 10.0) {
                    ImageSystemName("speaker.fill", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                    Slider(value = state.volume, bounds = 0.0..1.0, onValueChange = vm::setVolume)
                    Text(state.volumeLabel, modifier = Modifier.monospacedDigit().frame(width = 32.0, alignment = Alignment.trailing))
                }
                Toggle(title = "Autoplay", systemImage = "play.circle", isOn = state.autoplay, onIsOnChange = vm::setAutoplay)
                LabeledContent(title = "Downloaded cache", value = state.cache)
            })
            Section(title = "Notifications") {
                Picker(title = "Alert sound", systemImage = "bell.badge", selection = state.sound, onSelectionChange = { vm.setSound(it as Int) }) {
                    for (option in state.sounds) key(option.id) {
                        Text(option.label, modifier = Modifier.tag(option.id))
                    }
                }
                LabeledContent(title = "Quiet hours", value = state.quietHours)
            }
            Section(title = "About") {
                LabeledContent(title = "Version", value = state.version)
                LabeledContent(title = "License") {
                    Label(title = "Pro", systemImage = "checkmark.seal.fill", modifier = Modifier.foregroundStyle(Color.green))
                }
            }
        }
    }
}

val screen10 = BenchScreen("10 · Preferences", ::Screen10ViewModel) { Screen10(it as Screen10ViewModel) }
