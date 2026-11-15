package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 08 — Chat conversation: contact header, left/right bubbles, read receipts, typing indicator, input bar.

data class Screen08Message(val id: Int, val text: String, val time: String, val mine: Boolean, val read: Boolean)

data class Screen08State(
    val contact: String,
    val initials: String,
    val status: String,
    val dayLabel: String,
    val messages: List<Screen08Message>,
    val typing: Boolean,
    val draft: String,
)

class Screen08ViewModel : BenchViewModel() {
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen08State(
            contact = "Katherine Johnson",
            initials = "KJ",
            status = "online",
            dayLabel = "Today 09:12",
            messages = listOf(
                Screen08Message(1, "Morning! Did you get a chance to look at the trajectory numbers?", "09:12", false, true),
                Screen08Message(2, "Yes, they look right to me. I double-checked the re-entry window.", "09:14", true, true),
                Screen08Message(3, "Great. Can we sync before the launch review?", "09:15", false, true),
                Screen08Message(4, "Sure, 2 pm works.", "09:15", true, true),
                Screen08Message(5, "I'll bring the updated charts and the orbit plots.", "09:16", true, false),
                Screen08Message(6, "Perfect, see you then 🚀", "09:18", false, true),
            ),
            typing = true,
            draft = "",
        )
    )
    val state: StateFlow<Screen08State> = _state

    fun setDraft(value: String) = _state.update { it.copy(draft = value) }

    override fun mutate() = _state.update { s ->
        tick++
        s.copy(
            messages = s.messages.map { if (it.id == 5) it.copy(read = tick % 2 == 1) else it },
            typing = tick % 3 != 0,
            status = if (tick % 2 == 0) "online" else "last seen just now",
        )
    }
}

@Composable
fun Screen08(vm: Screen08ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(
            modifier = Modifier
                .safeAreaInset(edge = VerticalEdge.bottom) {
                    HStack(spacing = 10.0, modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 12.0)
                        .padding(edges = Edge.Set.vertical, length = 8.0)
                        .background(Color.gray.opacity(0.12))) {
                        Image(systemName = SFSymbol.plusCircleFill, modifier = Modifier.font(Font.title2).foregroundStyle(Color.gray))
                        TextField(title = "Message", text = state.draft, onTextChange = vm::setDraft, modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 12.0)
                            .padding(edges = Edge.Set.vertical, length = 8.0)
                            .background(Color.gray.opacity(0.15), Capsule()))
                        Image(systemName = SFSymbol.arrowUpCircleFill, modifier = Modifier.font(Font.title2).foregroundStyle(Color.blue))
                    }
                }
                .navigationTitle(state.contact)
                .toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline),
        ) {
            VStack(spacing = 8.0, modifier = Modifier.padding()) {
                VStack(spacing = 4.0, modifier = Modifier.padding(edges = Edge.Set.bottom, length = 6.0)) {
                    Text(
                        state.initials,
                        modifier = Modifier.font(Font.title3.bold()).foregroundStyle(Color.white)
                            .frame(width = 56.0, height = 56.0)
                            .background(LinearGradient(colors = listOf(Color.teal, Color.blue), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom), Circle()),
                    )
                    Text(state.contact, modifier = Modifier.font(Font.headline))
                    Text(state.status, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                }
                Text(state.dayLabel, modifier = Modifier.font(Font.caption2.weight(Font.Weight.medium)).foregroundStyle(ShapeStyle.secondary)
                    .padding(edges = Edge.Set.horizontal, length = 10.0)
                    .padding(edges = Edge.Set.vertical, length = 4.0)
                    .background(Color.gray.opacity(0.12), Capsule()))
                for (msg in state.messages) key(msg.id) {
                    HStack {
                        if (msg.mine) {
                            Spacer(minLength = 50.0)
                        }
                        VStack(alignment = if (msg.mine) HorizontalAlignment.trailing else HorizontalAlignment.leading, spacing = 3.0) {
                            Text(
                                msg.text,
                                modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 14.0)
                                    .padding(edges = Edge.Set.vertical, length = 9.0)
                                    .foregroundStyle(if (msg.mine) Color.white else Color.primary)
                                    .background(if (msg.mine) Color.blue else Color.gray.opacity(0.18), RoundedRectangle(cornerRadius = 18.0)),
                            )
                            HStack(spacing = 3.0, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary)) {
                                Text(msg.time)
                                if (msg.mine) {
                                    Image(systemName = 
                                        if (msg.read) SFSymbol.checkmarkCircleFill else SFSymbol.checkmarkCircle,
                                        modifier = Modifier.foregroundStyle(if (msg.read) Color.blue else Color.gray),
                                    )
                                }
                            }
                        }
                        if (!msg.mine) {
                            Spacer(minLength = 50.0)
                        }
                    }
                }
                if (state.typing) {
                    HStack(spacing = 4.0, modifier = Modifier.padding(edges = Edge.Set.horizontal, length = 14.0)
                        .padding(edges = Edge.Set.vertical, length = 12.0)
                        .background(Color.gray.opacity(0.18), Capsule())
                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)) {
                        for (i in 0 until 3) key(i) {
                            ShapeView(Circle(), fill = Color.gray.opacity(0.4 + 0.2 * i), modifier = Modifier.frame(width = 7.0, height = 7.0))
                        }
                    }
                }
            }
        }
    }
}

val screen08 = BenchScreen("08 · Chat conversation", ::Screen08ViewModel) { Screen08(it as Screen08ViewModel) }
