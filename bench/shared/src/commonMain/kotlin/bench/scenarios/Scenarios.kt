package bench.scenarios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nucleusframework.composeswiftui.*

// Scenario benchmarks: workloads the 50-screen benchmark can't cover (long scrolling lists, a single-row change in a
// large screen, animations). Each scenario has a Compose SwiftUI side (here) and a plain SwiftUI side with its own
// native @Observable model (bench/app/Scenarios.swift); both generate the same data, so they render identically.

/** One row of the mail list. Strings are built the same way on the Swift side. */
@Immutable
data class MailRow(val id: Int, val title: String, val subtitle: String, val time: String, val unread: Boolean, val icon: String)

fun mailRow(i: Int, revision: Int = 0): MailRow = MailRow(
    id = i,
    title = if (revision == 0) "Sender $i" else "Sender $i · r$revision",
    subtitle = "Subject line number $i, with a few more words to fill the row",
    time = "${i % 12 + 1}:${(i % 60).toString().padStart(2, '0')}",
    unread = i % 3 == 0,
    icon = ICONS[i % ICONS.size],
)

private val ICONS = listOf("envelope.fill", "star.fill", "paperclip", "flag.fill", "bell.fill")

/** A long mail list: N rows in a `List`. */
class LongListScenario(val count: Int, val lazy: Boolean) {
    // Immutable list in one state, as a ViewModel's StateFlow<List<…>> would hold it
    var rows by mutableStateOf((0 until count).map { mailRow(it) })
    private var revision = 0

    /** Toggles "unread" on one visible row: the single-element change. */
    fun toggleOne(index: Int) {
        rows = rows.toMutableList().also { it[index] = it[index].copy(unread = !it[index].unread) }
    }

    /** Renames every row: the worst case, every row changes. */
    fun renameAll() {
        revision++
        rows = rows.mapIndexed { i, row -> row.copy(title = "Sender $i · r$revision") }
    }

    fun host(): ComposeSwiftUIHost = composeSwiftUI {
        NavigationStack {
            List(modifier = Modifier.listStyle(ListStyle.plain).navigationTitle("Inbox")) {
                if (lazy) {
                    LazyForEach(rows, key = { it.id }) { MailRowView(it) }
                } else {
                    for (row in rows) key(row.id) { MailRowView(row) }
                }
            }
        }
    }
}

@Composable
private fun MailRowView(row: MailRow) {
    HStack(spacing = 12.0) {
        ShapeView(Circle(), fill = if (row.unread) Color.blue else Color.clear, modifier = Modifier.frame(width = 10.0, height = 10.0))
        Image(systemName = row.icon, modifier = Modifier.foregroundStyle(Color.orange).frame(width = 24.0))
        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
            Text(row.title, modifier = Modifier.font(Font.headline))
            Text(row.subtitle, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
        }
        Spacer()
        Text(row.time, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
    }
}

/** Animated bars: one state change animates every bar's height and color. */
class BarsScenario(val count: Int) {
    var phase by mutableStateOf(false)

    fun toggle() {
        withAnimation(Animation.easeInOut(duration = 0.35)) { phase = !phase }
    }

    fun host(): ComposeSwiftUIHost = composeSwiftUI {
        NavigationStack {
            HStack(alignment = VerticalAlignment.bottom, spacing = 2.0, modifier = Modifier.padding().navigationTitle("Bars")) {
                for (i in 0 until count) key(i) {
                    val height = barHeight(i, phase)
                    ShapeView(
                        RoundedRectangle(cornerRadius = 2.0),
                        fill = if (phase) Color.blue else Color.orange,
                        modifier = Modifier.frame(height = height),
                    )
                }
            }
        }
    }
}

fun barHeight(i: Int, phase: Boolean): Double = 20.0 + ((i * 37 + if (phase) 101 else 0) % 200)
