package sample

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    data object Paragraph : Route
}

enum class AppTab { Examples, Text }

class CounterViewModel : ViewModel() {
    val count = MutableStateFlow(0)
    fun increment() = count.update { it + 1 }
}

/** Entry point shown from Swift with `ComposeView { SampleAppKt.sampleApp() }`. */
fun sampleApp() = composeSwiftUI {
    WithComposeResources { AppTabs() }
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
private fun AppTabs() {
    // The selected tab is Kotlin state: the Examples tab can switch to the Styled text tab.
    var tab by remember { mutableStateOf(AppTab.Examples) }
    TabView(selection = tab, onSelectionChange = { tab = it }) {
        Tab("Examples", systemImage = "list.bullet", value = AppTab.Examples) {
            SampleNavigation(onShowText = { tab = AppTab.Text })
        }
        Tab("Styled text", systemImage = "textformat", value = AppTab.Text) {
            StyledTextScreen()
        }
    }
}

@androidx.compose.runtime.Composable
private fun SampleNavigation(onShowText: () -> Unit) {
    // Navigation 3 runtime: back stack + entry provider, displayed with SwiftUI navigation.
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val entries = entryProvider<Route> {
        entry<Route.Home> { HomeScreen(onOpen = { backStack.add(it) }, onShowText = onShowText) }
        entry<Route.Counter> { CounterScreen() }
        entry<Route.Chart> { ChartScreen() }
        entry<Route.Paragraph> { ParagraphScreen() }
    }
    NavDisplay(backStack) { route -> entries(route).Content() }
}

private fun HomeScreenRows() = listOf(
    Triple(Route.Counter, "Counter", "plusminus.circle.fill"),
    Triple(Route.Chart, "Compose Multiplatform chart", "chart.bar.fill"),
    Triple(Route.Paragraph, "UIKit paragraph", "text.alignleft"),
)

@androidx.compose.runtime.Composable
private fun HomeScreen(onOpen: (Route) -> Unit, onShowText: () -> Unit) {
    List(modifier = Modifier.navigationTitle("Compose SwiftUI")) {
        Section(title = stringResource(Res.string.examples_title)) {
            for ((route, title, icon) in HomeScreenRows()) {
                Button(action = { onOpen(route) }) {
                    Label(title, systemImage = icon)
                }
            }
            Button(action = onShowText) {
                Label("Open the Styled text tab", systemImage = "arrow.right.circle.fill")
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

@androidx.compose.runtime.Composable
private fun StyledTextScreen() {
    val paragraph = buildStyledString {
        withStyle(SpanStyle(fontWeight = Font.Weight.bold)) { append("Bold opening. ") }
        withStyle(SpanStyle(fontSize = 13.0, color = Color.gray, italic = true)) { append("(a small grey instruction) ") }
        append("regular text, ")
        withStyle(SpanStyle(color = Color.orange, fontWeight = Font.Weight.semibold)) { append("a coloured word") }
        append(", and ")
        withStyle(SpanStyle(font = Font.title3, underline = true)) { append("a larger underlined one") }
        append(".")
    }
    NavigationStack {
        ScrollView {
            Text(paragraph, modifier = Modifier.font(Font.body).padding().navigationTitle("Styled text"))
        }
    }
}

@androidx.compose.runtime.Composable
private fun ParagraphScreen() {
    var sentences by remember { mutableStateOf(1) }
    val text = (1..sentences).joinToString(" ") { "Sentence $it of a paragraph drawn by a UIKit label." }
    ScrollView(modifier = Modifier.navigationTitle("UIKit paragraph")) {
        VStack(alignment = HorizontalAlignment.leading, spacing = 12.0, modifier = Modifier.padding()) {
            Text("Above the UIKit label", modifier = Modifier.font(Font.headline))
            // A multi-line UILabel sized by its content: the height follows the text.
            UIKitView(
                sizing = NativeViewSizing.FitHeight,
                modifier = Modifier.background(Color.yellow.opacity(0.3)),
                update = { (it as platform.UIKit.UILabel).text = text },
            ) {
                platform.UIKit.UILabel().apply { numberOfLines = 0 }
            }
            Text("Below the UIKit label", modifier = Modifier.font(Font.headline))
            HStack(spacing = 12.0) {
                Button("Add a sentence", action = { sentences++ })
                Button("Reset", action = { sentences = 1 })
            }
        }
    }
}
