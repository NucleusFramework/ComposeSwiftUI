package sample.mac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import compose_swiftui_macos_sample.shared.generated.resources.Res
import compose_swiftui_macos_sample.shared.generated.resources.count_label
import compose_swiftui_macos_sample.shared.generated.resources.counter_title
import dev.nucleusframework.composeswiftui.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// A module with only the macOS target: its previews render on the Mac.

@Composable
fun CounterScreen() {
    var count by remember { mutableStateOf(0) }
    VStack(spacing = 16.0) {
        Text(stringResource(Res.string.counter_title), modifier = Modifier.font(Font.largeTitle))
        Text(stringResource(Res.string.count_label, count), modifier = Modifier.font(Font.title).monospacedDigit())
        Button("Increment") { count++ }
    }
}

// Compose Multiplatform's stringResource reads the density, which Compose SwiftUI doesn't provide.
@Composable
private fun WithComposeResources(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides Density(2f), content = content)
}

@Preview(name = "Counter", width = 400, height = 300)
@Composable
fun CounterPreview() {
    WithComposeResources { CounterScreen() }
}

@PreviewLightDark
@Composable
fun SettingsPreview() {
    Form {
        Toggle("Notifications", isOn = true, onIsOnChange = {})
        Picker("Theme", selection = "System", onSelectionChange = {}) {
            Text("System", modifier = Modifier.tag("System"))
            Text("Light", modifier = Modifier.tag("Light"))
        }
    }
}

@Preview(name = "Sidebar")
@Composable
fun SidebarPreview() {
    NavigationSplitView(sidebar = { List { Text("Inbox"); Text("Archive") } }, detail = { Text("Select a mailbox") })
}
