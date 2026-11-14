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

// Screen 16 — Kanban board: horizontal scroll of columns, cards with tag capsules, avatars, counters.

data class Screen16Tag(val label: String, val tint: Rgb)

data class Screen16Avatar(val initials: String, val tint: Rgb)

data class Screen16Card(
    val id: Int,
    val title: String,
    val tags: List<Screen16Tag>,
    val due: String,
    val comments: String,
    val checklist: String,
    val progress: Double,
    val avatars: List<Screen16Avatar>,
)

data class Screen16Column(val id: Int, val title: String, val tint: Rgb, val count: String, val cards: List<Screen16Card>)

data class Screen16State(val boardName: String, val columns: List<Screen16Column>, val tick: Int)

private val tagBug = Screen16Tag("Bug", Rgb(1.0, 0.23, 0.19))
private val tagUi = Screen16Tag("UI", Rgb(0.69, 0.32, 0.87))
private val tagApi = Screen16Tag("API", Rgb(0.0, 0.48, 1.0))
private val tagDocs = Screen16Tag("Docs", Rgb(0.2, 0.78, 0.35))
private val tagPerf = Screen16Tag("Perf", Rgb(1.0, 0.58, 0.0))

private val ada = Screen16Avatar("AL", Rgb(0.0, 0.48, 1.0))
private val grace = Screen16Avatar("GH", Rgb(1.0, 0.18, 0.33))
private val alan = Screen16Avatar("AT", Rgb(0.2, 0.68, 0.35))
private val linus = Screen16Avatar("LT", Rgb(1.0, 0.58, 0.0))

private fun screen16Columns(tick: Int): List<Screen16Column> {
    val done = 2 + tick % 4
    val columns = listOf(
        Screen16Column(
            1, "To Do", Rgb(0.56, 0.56, 0.58), "",
            listOf(
                Screen16Card(11, "Support dashed strokes on shape views", listOf(tagApi, tagUi), "Nov 18", "${3 + tick % 5}", "0/4", 0.0, listOf(ada, alan)),
                Screen16Card(12, "Write migration guide for 0.4", listOf(tagDocs), "Nov 21", "1", "1/6", 1.0 / 6.0, listOf(grace)),
                Screen16Card(13, "List rows flicker on fast scroll", listOf(tagBug, tagPerf), "Nov 16", "7", "0/2", 0.0, listOf(linus, ada, grace)),
            ),
        ),
        Screen16Column(
            2, "In Progress", Rgb(0.0, 0.48, 1.0), "",
            listOf(
                Screen16Card(21, "Diff-based modifier updates", listOf(tagPerf, tagApi), "Nov 15", "${12 + tick}", "$done/5", done / 5.0, listOf(alan, linus)),
                Screen16Card(22, "Redesign onboarding screens", listOf(tagUi), "Nov 20", "4", "3/8", 3.0 / 8.0, listOf(grace, ada)),
            ),
        ),
        Screen16Column(
            3, "Review", Rgb(1.0, 0.58, 0.0), "",
            listOf(
                Screen16Card(31, "Fix toolbar placement on macOS", listOf(tagBug), "Nov 14", "2", "4/4", 1.0, listOf(linus)),
            ),
        ),
        Screen16Column(
            4, "Done", Rgb(0.2, 0.78, 0.35), "",
            listOf(
                Screen16Card(41, "Generate bindings from symbol graphs", listOf(tagApi), "Nov 2", "18", "6/6", 1.0, listOf(ada, alan, grace)),
                Screen16Card(42, "Benchmark harness", listOf(tagPerf, tagDocs), "Nov 5", "9", "3/3", 1.0, listOf(linus)),
            ),
        ),
    )
    return columns.map { it.copy(count = "${it.cards.size}") }
}

class Screen16ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(Screen16State("Sprint 42 · Compose SwiftUI", screen16Columns(0), 0))
    val state: StateFlow<Screen16State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(tick = t, columns = screen16Columns(t), boardName = "Sprint ${42 + t} · Compose SwiftUI")
    }
}

@Composable
fun Screen16(vm: Screen16ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        VStack(modifier = Modifier.navigationTitle("Board"), alignment = HorizontalAlignment.leading, spacing = 0.0) {
            HStack(modifier = Modifier.padding(Edge.Set.horizontal).padding(Edge.Set.bottom, 8.0)) {
                Label(state.boardName, systemImage = "square.grid.3x2.fill", modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).foregroundStyle(ShapeStyle.secondary))
                Spacer()
                Image(systemName = "line.3.horizontal.decrease.circle", modifier = Modifier.font(Font.title3).foregroundStyle(Color.blue))
            }
            ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                HStack(modifier = Modifier.padding(), alignment = VerticalAlignment.top, spacing = 12.0) {
                    for (column in state.columns) key(column.id) {
                        VStack(
                            modifier = Modifier.padding(12.0)
                                .frame(width = 260.0)
                                .background(Color.gray.opacity(0.12), RoundedRectangle(cornerRadius = 16.0)),
                            alignment = HorizontalAlignment.leading,
                            spacing = 10.0,
                        ) {
                            HStack(spacing = 8.0) {
                                ShapeView(Circle(), modifier = Modifier.frame(width = 8.0, height = 8.0), fill = column.tint.color())
                                Text(column.title, modifier = Modifier.font(Font.headline))
                                Text(
                                    column.count,
                                    modifier = Modifier.font(Font.caption.weight(Font.Weight.bold))
                                        .monospacedDigit()
                                        .padding(Edge.Set.horizontal, 7.0)
                                        .padding(Edge.Set.vertical, 2.0)
                                        .background(ShapeStyle.background, Capsule()),
                                )
                                Spacer()
                                Image(systemName = "ellipsis", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                            }
                            for (card in column.cards) key(card.id) {
                                VStack(
                                    modifier = Modifier.padding(10.0)
                                        .background(ShapeStyle.background, RoundedRectangle(cornerRadius = 12.0))
                                        .shadow(color = Color.black.opacity(0.06), radius = 3.0, y = 1.0),
                                    alignment = HorizontalAlignment.leading,
                                    spacing = 8.0,
                                ) {
                                    HStack(spacing = 4.0) {
                                        for (tag in card.tags) key(tag.label) {
                                            Text(
                                                tag.label,
                                                modifier = Modifier.font(Font.caption2.weight(Font.Weight.semibold))
                                                    .foregroundStyle(tag.tint.color())
                                                    .padding(Edge.Set.horizontal, 8.0)
                                                    .padding(Edge.Set.vertical, 3.0)
                                                    .background(tag.tint.color().opacity(0.15), Capsule()),
                                            )
                                        }
                                    }
                                    Text(card.title, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).lineLimit(2).frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading))
                                    HStack(spacing = 8.0) {
                                        ProgressView(value = card.progress, modifier = Modifier.tint(column.tint.color()))
                                        Label(card.checklist, systemImage = "checklist", modifier = Modifier.font(Font.caption2).monospacedDigit().foregroundStyle(ShapeStyle.secondary).fixedSize())
                                    }
                                    HStack(modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary).labelStyle(LabelStyle.titleAndIcon).lineLimit(1), spacing = 10.0) {
                                        Label(card.due, systemImage = "calendar")
                                        Label(card.comments, systemImage = "bubble.left")
                                        Spacer()
                                        HStack(spacing = -8.0) {
                                            for (avatar in card.avatars) key(avatar.initials) {
                                                Text(
                                                    avatar.initials,
                                                    modifier = Modifier.font(Font.system(size = 9.0, weight = Font.Weight.bold))
                                                        .foregroundStyle(Color.white)
                                                        .frame(width = 24.0, height = 24.0)
                                                        .background(avatar.tint.color(), Circle())
                                                        .overlay { ShapeView(Circle(), stroke = ShapeStyle.background, lineWidth = 2.0) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Label("Add card", systemImage = "plus", modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
            }
            Spacer()
        }
    }
}

val screen16 = BenchScreen("16 · Kanban board", ::Screen16ViewModel) { Screen16(it as Screen16ViewModel) }
