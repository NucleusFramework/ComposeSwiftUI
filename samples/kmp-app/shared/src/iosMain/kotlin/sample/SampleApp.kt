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
    data object LongList : Route
    data object RichEditor : Route
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
        Tab("Examples", systemImage = SFSymbol.listBullet, value = AppTab.Examples) {
            SampleNavigation(onShowText = { tab = AppTab.Text })
        }
        Tab("Styled text", systemImage = SFSymbol.textformat, value = AppTab.Text) {
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
        entry<Route.LongList> { LongListScreen() }
        entry<Route.RichEditor> { RichEditorScreen() }
    }
    NavDisplay(backStack) { route -> entries(route).Content() }
}

private fun HomeScreenRows() = listOf(
    Triple(Route.Counter, "Counter", SFSymbol.plusminusCircleFill),
    Triple(Route.Chart, "Compose Multiplatform chart", SFSymbol.chartBarFill),
    Triple(Route.Paragraph, "UIKit paragraph", SFSymbol.textAlignleft),
    Triple(Route.LongList, "Long list (5,000 rows)", SFSymbol.listNumber),
    Triple(Route.RichEditor, "Rich text editor", SFSymbol.textformat),
)

@androidx.compose.runtime.Composable
private fun HomeScreen(onOpen: (Route) -> Unit, onShowText: () -> Unit) {
    var showPreviews by remember { mutableStateOf(false) }
    List(
        modifier = Modifier
            .navigationTitle("Compose SwiftUI")
            // The generated ComposeSwiftUIPreviewGallery (Swift), registered as a native view in SampleApp.swift.
            .sheet(isPresented = showPreviews, onIsPresentedChange = { showPreviews = it }) {
                NativeView("previewGallery")
            },
    ) {
        Section(title = stringResource(Res.string.examples_title)) {
            for ((route, title, icon) in HomeScreenRows()) {
                Button(action = { onOpen(route) }) {
                    Label(title, systemImage = icon)
                }
            }
            Button(action = onShowText) {
                Label("Open the Styled text tab", systemImage = SFSymbol.arrowRightCircleFill)
            }
            Button(action = { showPreviews = true }) {
                Label("Browse the @Preview composables", systemImage = SFSymbol.eye)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CounterScreen() {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    VStack(spacing = 16.0, modifier = Modifier.navigationTitle(stringResource(Res.string.counter_title))) {
        Image(systemName = SFSymbol.swift, modifier = Modifier.font(Font.system(size = 64.0)).foregroundStyle(Color.red))
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
        append(", ")
        withStyle(SpanStyle(backgroundColor = Color.yellow)) { append("a highlight") }
        append(" and ")
        withStyle(SpanStyle(link = "https://developer.apple.com")) { append("a link") }
        append(".")
    }
    NavigationStack {
        ScrollView {
            Text(paragraph, modifier = Modifier.font(Font.body).padding().navigationTitle("Styled text"))
        }
    }
}

@androidx.compose.runtime.Composable
private fun RichEditorScreen() {
    var text by remember {
        mutableStateOf(buildStyledString {
            append("Select some text and press ")
            withStyle(SpanStyle(fontWeight = Font.Weight.bold)) { append("B") }
            append(", or use the Format menu (fonts, colors, alignment).")
        })
    }
    var selection by remember { mutableStateOf<RichTextSelection?>(null) }
    // SwiftUI's `text.transformAttributes(in: &selection) { … }`
    fun format(isOn: (SpanStyle) -> Boolean, set: SpanStyle.(Boolean) -> SpanStyle) {
        val current = selection ?: return
        val on = text.styles(current).all(isOn)
        text.transformStyle(current) { it.set(!on) }.let { (t, s) -> text = t; selection = s }
    }
    val styles = selection?.let { text.styles(it) }.orEmpty()
    VStack(alignment = HorizontalAlignment.leading, modifier = Modifier.padding().navigationTitle("Rich text editor")) {
        HStack(modifier = Modifier.toggleStyle(ToggleStyle.button)) {
            Toggle("B", isOn = styles.isNotEmpty() && styles.all { it.fontWeight == Font.Weight.bold }, onIsOnChange = {
                format({ it.fontWeight == Font.Weight.bold }) { copy(fontWeight = if (it) Font.Weight.bold else null) }
            })
            Toggle("I", isOn = styles.isNotEmpty() && styles.all { it.italic }, onIsOnChange = {
                format({ it.italic }) { copy(italic = it) }
            })
            Toggle("Highlight", isOn = styles.isNotEmpty() && styles.all { it.backgroundColor != null }, onIsOnChange = {
                format({ it.backgroundColor != null }) { copy(backgroundColor = if (it) Color.yellow else null) }
            })
        }
        TextEditor(text, onTextChange = { text = it }, selection = selection, onSelectionChange = { selection = it },
            modifier = Modifier.frame(minHeight = 160.0).border(Color.gray)
                // SwiftUI's attributedTextFormattingDefinition: no underline, text colors limited to a palette
                .attributedTextFormattingDefinition(AttributedTextFormattingDefinition(
                    AttributedTextValueConstraint.underlineStyle(setOf(null)),
                    AttributedTextValueConstraint.foregroundColor(setOf(null, Color.red, Color.blue)),
                )))
        Text("Kotlin state (selection ${selection?.ranges}):", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
        Text(text)
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

// Previews: shown in Xcode's canvas from the generated iosApp/ComposeSwiftUI/ComposeSwiftUIPreviews.swift, and
// listed by ComposeSwiftUIPreviewGallery (the "Browse the @Preview composables" row of the home screen).

@Preview(name = "Counter")
@androidx.compose.runtime.Composable
fun CounterScreenPreview() {
    WithComposeResources {
        NavigationStack { CounterScreen() }
    }
}

@androidx.compose.runtime.Composable
@Preview(name = "Styled text")
fun StyledTextScreenPreview() {
    StyledTextScreen()
}

@androidx.compose.runtime.Composable
private fun LongListScreen() {
    var starred by remember { mutableStateOf(setOf<Int>()) }
    val rows = remember { (1..5000).toList() }
    // LazyForEach composes only the rows SwiftUI shows
    List(modifier = Modifier.navigationTitle("Long list")) {
        LazyForEach(rows, key = { it }) { n ->
            Button(action = { starred = if (n in starred) starred - n else starred + n }) {
                HStack {
                    Text("Row $n")
                    Spacer()
                    Image(systemName = if (n in starred) SFSymbol.starFill else SFSymbol.star, modifier = Modifier.foregroundStyle(Color.orange))
                }
            }
        }
    }
}
