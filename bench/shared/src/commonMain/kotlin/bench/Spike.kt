package bench

import androidx.compose.runtime.*
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow

class CounterViewModel : androidx.lifecycle.ViewModel() {
    val count = MutableStateFlow(0)
    fun increment() { count.value++ }
}

/** Two-column flow layout written in Kotlin. */
private object FlowPolicy : LayoutPolicy {
    override fun sizeThatFits(proposedWidth: Double?, proposedHeight: Double?, subviews: List<LayoutSize>): LayoutSize {
        val width = proposedWidth ?: 300.0
        return LayoutSize(width, place(width, subviews).let { p -> p.zip(subviews).maxOfOrNull { it.first.y + it.second.height } ?: 0.0 })
    }

    override fun placeSubviews(width: Double, height: Double, subviews: List<LayoutSize>) = place(width, subviews)

    private fun place(width: Double, subviews: List<LayoutSize>): List<LayoutPlacement> {
        var x = 0.0; var y = 0.0; var row = 0.0
        return subviews.map { s ->
            if (x + s.width > width && x > 0) { x = 0.0; y += row + 8; row = 0.0 }
            LayoutPlacement(x, y).also { x += s.width + 8; row = maxOf(row, s.height) }
        }
    }
}

private val Star = PathShape {
    moveTo(0.5, 0.0); lineTo(0.62, 0.38); lineTo(1.0, 0.38); lineTo(0.69, 0.62); lineTo(0.81, 1.0)
    lineTo(0.5, 0.76); lineTo(0.19, 1.0); lineTo(0.31, 0.62); lineTo(0.0, 0.38); lineTo(0.38, 0.38); close()
}

data class Detail(val id: Int, val title: String)

fun spikeScreen() = composeSwiftUI {
    val vm = viewModel { CounterViewModel() }
    val count by vm.count.collectAsState()
    var path by remember { mutableStateOf(listOf<Int>()) }
    var sheet by remember { mutableStateOf<Detail?>(null) }
    var big by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0.0) }
    val sheetModifier = Modifier.sheet(item = sheet, onDismiss = { sheet = null }) { d ->
        VStack(spacing = 16.0) {
            Text(d.title, modifier = Modifier.font(Font.title))
            EnvironmentReader { env -> Button("Close via dismiss") { env.dismiss() } }
        }
    }
    NavigationStack(path = path, onPathChange = { path = it }, modifier = sheetModifier, destination = { n ->
        VStack(spacing = 12.0, modifier = Modifier.navigationTitle("Page $n")) {
            Text("Destination #$n", modifier = Modifier.font(Font.largeTitle))
            Button("Push ${n + 1}") { path = path + (n + 1) }
        }
    }) {
        ScrollView {
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0, modifier = Modifier.padding().navigationTitle("Extras")) {
                EnvironmentReader { env -> Text("Dark: ${env.isDark} · RTL: ${env.layoutDirection == LayoutDirection.rightToLeft}") }
                HStack {
                    Button("Push page 1") { path = path + 1 }
                    Button("Open sheet") { sheet = Detail(count, "Item $count") }
                    Button("Count $count", action = vm::increment)
                }
                ShapeView(Star, fill = Color.orange, modifier = Modifier.frame(width = if (big) 120.0 else 60.0, height = if (big) 120.0 else 60.0)
                    .onTapGesture { withAnimation(Animation.spring) { big = !big } })
                CustomLayout(FlowPolicy) {
                    for (tag in listOf("Kotlin", "Compose", "SwiftUI", "iOS", "macOS", "tvOS", "watchOS", "Layout")) key(tag) {
                        Text(tag, modifier = Modifier.padding(Edge.Set.horizontal, 10.0).padding(Edge.Set.vertical, 6.0)
                            .background(Color.blue.opacity(0.15), Capsule()))
                    }
                }
                Text("Drag me: ${dragX.toInt()}", modifier = Modifier.padding().background(Color.green.opacity(0.2), RoundedRectangle(cornerRadius = 10.0))
                    .offset(x = dragX).dragGesture(onChanged = { dragX = it.translationX }, onEnded = { withAnimation { dragX = 0.0 } }))
            }
        }
    }
}
