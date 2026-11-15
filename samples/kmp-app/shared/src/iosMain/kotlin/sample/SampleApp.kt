package sample

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import compose_swiftui_sample.shared.generated.resources.Res
import compose_swiftui_sample.shared.generated.resources.count_label
import compose_swiftui_sample.shared.generated.resources.counter_title
import compose_swiftui_sample.shared.generated.resources.examples_title
import dev.nucleusframework.composeswiftui.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Navigation 3 keys
sealed interface Route : NavKey {
    data object Home : Route
    data object Counter : Route
    data object Chart : Route
}

class CounterViewModel : ViewModel() {
    val count = MutableStateFlow(0)
    fun increment() = count.update { it + 1 }
}

/** Entry point shown from Swift with `ComposeView { SampleAppKt.sampleApp() }`. */
fun sampleApp() = composeSwiftUI {
    WithComposeResources { SampleNavigation() }
}

/**
 * Compose Multiplatform resources (`stringResource`) read Compose UI's LocalDensity, which a Compose SwiftUI
 * composition doesn't provide: provide it once at the root.
 */
@androidx.compose.runtime.Composable
private fun WithComposeResources(content: @androidx.compose.runtime.Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides
            androidx.compose.ui.unit.Density(platform.UIKit.UIScreen.mainScreen.scale.toFloat()),
        content = content,
    )
}

@androidx.compose.runtime.Composable
private fun SampleNavigation() {
    // Navigation 3 runtime: back stack + entry provider, displayed with SwiftUI navigation.
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val entries = entryProvider<Route> {
        entry<Route.Home> { HomeScreen(onOpen = { backStack.add(it) }) }
        entry<Route.Counter> { CounterScreen() }
        entry<Route.Chart> { ChartScreen() }
    }
    NavDisplay(backStack) { route -> entries(route).Content() }
}

private fun HomeScreenRows() = listOf(
    Triple(Route.Counter, "Counter", "plusminus.circle.fill"),
    Triple(Route.Chart, "Compose Multiplatform chart", "chart.bar.fill"),
)

@androidx.compose.runtime.Composable
private fun HomeScreen(onOpen: (Route) -> Unit) {
    List(modifier = Modifier.navigationTitle("Compose SwiftUI")) {
        Section(title = stringResource(Res.string.examples_title)) {
            for ((route, title, icon) in HomeScreenRows()) {
                Button(action = { onOpen(route) }) {
                    Label(title, systemImage = icon)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CounterScreen() {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    VStack(spacing = 16.0, modifier = Modifier.navigationTitle(stringResource(Res.string.counter_title))) {
        Image(systemName = "swift", modifier = Modifier.font(Font.system(size = 64.0)).foregroundStyle(Color.orange))
        Text(stringResource(Res.string.count_label, count), modifier = Modifier.font(Font.title).monospacedDigit())
        Button("Increment", action = vm::increment)
    }
}

@androidx.compose.runtime.Composable
private fun ChartScreen() {
    VStack(alignment = HorizontalAlignment.leading, spacing = 12.0, modifier = Modifier.padding().navigationTitle("Chart")) {
        Text("This chart is drawn by Compose Multiplatform inside a Compose SwiftUI screen.",
            modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
        // Compose Multiplatform content, embedded as a UIKit view controller.
        UIViewControllerView(modifier = Modifier.frame(height = 260.0)) { salesChartController() }
        Spacer()
    }
}
