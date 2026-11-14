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

// Screen 02 — Mail inbox: mailbox rows with badge counts, message rows with unread dots, previews and flags.

data class Screen02Mailbox(val id: Int, val title: String, val icon: String, val tint: Rgb, val count: Int)

data class Screen02Message(
    val id: Int,
    val sender: String,
    val subject: String,
    val preview: String,
    val time: String,
    val unread: Boolean,
    val flagged: Boolean,
    val attachment: Boolean,
)

data class Screen02State(
    val mailboxes: List<Screen02Mailbox>,
    val messages: List<Screen02Message>,
    val updated: String,
    val unreadSummary: String,
)

class Screen02ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen02State(
            mailboxes = listOf(
                Screen02Mailbox(101, "Inbox", "tray.fill", Rgb(0.0, 0.48, 1.0), 12),
                Screen02Mailbox(102, "VIP", "star.fill", Rgb(1.0, 0.8, 0.0), 3),
                Screen02Mailbox(103, "Flagged", "flag.fill", Rgb(1.0, 0.58, 0.0), 5),
                Screen02Mailbox(104, "Drafts", "doc.fill", Rgb(0.56, 0.56, 0.58), 1),
            ),
            messages = listOf(
                Screen02Message(1, "Grace Hopper", "Compiler review notes", "I went through the A-0 changes and left a few comments on the linker part, mostly naming.", "9:41", true, false, true),
                Screen02Message(2, "Alan Turing", "Lunch on Thursday?", "There is a new place near the lab that does excellent soups. Let me know if noon works for you.", "8:15", true, true, false),
                Screen02Message(3, "Linus Torvalds", "Re: Patch series v3", "Applied the first four patches, the fifth one needs a rebase on top of the scheduler work.", "Yesterday", false, false, false),
                Screen02Message(4, "Margaret Hamilton", "Apollo guidance checklist", "Attached is the updated checklist for the priority display tests, please review before Friday.", "Yesterday", false, true, true),
                Screen02Message(5, "Dennis Ritchie", "Typo in chapter 5", "Small thing: the example on page 112 is missing a semicolon after the struct declaration.", "Monday", false, false, false),
                Screen02Message(6, "Barbara Liskov", "Substitution talk slides", "Here are the slides from yesterday, feel free to reuse the diagrams in your lecture.", "Sunday", true, false, true),
            ),
            updated = "Updated Just Now",
            unreadSummary = "3 Unread",
        )
    )
    val state: StateFlow<Screen02State> = _state

    fun toggleFlag(id: Int) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.id == id) it.copy(flagged = !it.flagged) else it })
    }

    override fun mutate() = _state.update { s ->
        tick++
        val target = tick % s.messages.size
        val messages = s.messages.mapIndexed { i, m -> if (i == target) m.copy(unread = !m.unread) else m }
        val unread = messages.count { it.unread }
        s.copy(
            messages = messages,
            mailboxes = s.mailboxes.map { if (it.id == 101) it.copy(count = 9 + unread) else it },
            unreadSummary = "$unread Unread",
            updated = "Updated $tick min ago",
        )
    }
}

@Composable
fun Screen02(vm: Screen02ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(
            modifier = Modifier.listStyle(ListStyle.plain)
                .navigationTitle("Mail")
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) {
                    Button(title = "Compose", systemImage = "square.and.pencil", action = {})
                },
        ) {
            Section(title = "Mailboxes") {
                for (box in state.mailboxes) key(box.id) {
                    HStack(spacing = 12.0, modifier = Modifier.badge(box.count)) {
                        Image(systemName = box.icon, modifier = Modifier.foregroundStyle(box.tint.color()).frame(width = 26.0))
                        Text(box.title)
                    }
                }
            }
            Section(
                header = {
                    HStack {
                        Text("Today")
                        Spacer()
                        Text(state.unreadSummary, modifier = Modifier.foregroundStyle(Color.blue))
                    }
                },
                footer = {
                    Text(state.updated, modifier = Modifier.font(Font.caption).frame(maxWidth = Double.POSITIVE_INFINITY))
                },
                content = {
                    for (msg in state.messages) key(msg.id) {
                        HStack(alignment = VerticalAlignment.top, spacing = 10.0, modifier = Modifier.padding(edges = Edge.Set.vertical, length = 4.0)) {
                            ShapeView(
                                Circle(),
                                fill = if (msg.unread) Color.blue else Color.clear,
                                modifier = Modifier.frame(width = 10.0, height = 10.0).padding(edges = Edge.Set.top, length = 6.0),
                            )
                            VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                                HStack(spacing = 6.0) {
                                    Text(msg.sender, modifier = Modifier.font(Font.headline).lineLimit(1))
                                    if (msg.flagged) {
                                        Image(systemName = "flag.fill", modifier = Modifier.font(Font.caption).foregroundStyle(Color.orange))
                                    }
                                    Spacer()
                                    if (msg.attachment) {
                                        Image(systemName = "paperclip", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                    }
                                    Text(msg.time, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                                    Image(systemName = "chevron.right", modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)).foregroundStyle(ShapeStyle.tertiary))
                                }
                                Text(msg.subject, modifier = Modifier.font(Font.subheadline).fontWeight(if (msg.unread) Font.Weight.semibold else Font.Weight.regular).lineLimit(1))
                                Text(msg.preview, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).lineLimit(2))
                            }
                        }
                    }
                },
            )
        }
    }
}

val screen02 = BenchScreen("02 · Mail inbox", ::Screen02ViewModel) { Screen02(it as Screen02ViewModel) }
