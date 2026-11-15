package bench.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import bench.BenchScreen
import bench.BenchViewModel
import bench.Rgb
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Screen 43 — Event tickets: ticket cards with a date block, venue, seat info and a QR-like grid of squares.

data class Screen43Cell(val id: Int, val on: Boolean)

data class Screen43QrRow(val id: Int, val cells: List<Screen43Cell>)

data class Screen43Seat(val label: String, val value: String)

data class Screen43Ticket(
    val id: Int,
    val month: String,
    val day: String,
    val weekday: String,
    val title: String,
    val venue: String,
    val time: String,
    val tint: Rgb,
    val icon: String,
    val seats: List<Screen43Seat>,
    val status: String,
    val checkedIn: Boolean,
    val qr: List<Screen43QrRow>,
)

data class Screen43State(val subtitle: String, val tickets: List<Screen43Ticket>)

private fun screen43Qr(seed: Int): List<Screen43QrRow> = (0 until 9).map { r ->
    Screen43QrRow(r, (0 until 9).map { c ->
        val finder = (r < 3 && c < 3) || (r < 3 && c > 5) || (r > 5 && c < 3)
        val on = if (finder) !(r % 6 == 1 && c % 6 == 1) else (r * 7 + c * 3 + seed) % 5 < 2
        Screen43Cell(r * 9 + c, on)
    })
}

private fun screen43Ticket(id: Int, month: String, day: String, weekday: String, title: String, venue: String, time: String, tint: Rgb, icon: String, section: String, row: String, seat: String) =
    Screen43Ticket(
        id, month, day, weekday, title, venue, time, tint, icon,
        listOf(Screen43Seat("SECTION", section), Screen43Seat("ROW", row), Screen43Seat("SEAT", seat), Screen43Seat("GATE", "${id + 2}")),
        "Ready to scan", false, screen43Qr(id * 3),
    )

class Screen43ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen43State(
            subtitle = "3 upcoming events",
            tickets = listOf(
                screen43Ticket(1, "NOV", "21", "SAT", "Arctic Lights Tour", "Madison Square Garden", "Doors 7:00 PM", Rgb(0.35, 0.34, 0.84), "music.mic", "112", "F", "14"),
                screen43Ticket(2, "DEC", "04", "FRI", "City FC vs. Rovers", "Riverside Stadium", "Kick-off 8:45 PM", Rgb(0.2, 0.68, 0.35), "sportscourt.fill", "B4", "22", "7"),
                screen43Ticket(3, "JAN", "16", "SAT", "Swan Lake", "Royal Opera House", "Curtain 7:30 PM", Rgb(0.85, 0.25, 0.45), "theatermasks.fill", "Stalls", "K", "31"),
            ),
        )
    )
    val state: StateFlow<Screen43State> = _state

    private var tick = 0

    override fun mutate() {
        tick += 1
        _state.update { s ->
            s.copy(tickets = s.tickets.mapIndexed { i, t ->
                if (i == tick % s.tickets.size) {
                    val checked = !t.checkedIn
                    t.copy(checkedIn = checked, status = if (checked) "Checked in" else "Ready to scan", qr = screen43Qr(t.id * 3 + tick))
                } else t
            })
        }
    }
}

@Composable
fun Screen43(vm: Screen43ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Tickets")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                Text(state.subtitle, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                for (ticket in state.tickets) key(ticket.id) {
                    VStack(
                        modifier = Modifier.padding(14.0)
                            .background(ticket.tint.color().opacity(0.08), RoundedRectangle(cornerRadius = 18.0))
                            .overlay {
                                ShapeView(RoundedRectangle(cornerRadius = 18.0), stroke = ticket.tint.color().opacity(0.3), lineWidth = 1.0)
                            },
                        alignment = HorizontalAlignment.leading,
                        spacing = 12.0,
                    ) {
                        HStack(spacing = 14.0) {
                            VStack(
                                modifier = Modifier.foregroundStyle(Color.white).frame(width = 58.0, height = 70.0)
                                    .background(ticket.tint.color(), RoundedRectangle(cornerRadius = 12.0)),
                                spacing = 0.0,
                            ) {
                                Text(ticket.month, modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.bold))
                                Text(ticket.day, modifier = Modifier.font(Font.title).fontWeight(Font.Weight.heavy).monospacedDigit())
                                Text(ticket.weekday, modifier = Modifier.font(Font.caption2).opacity(0.85))
                            }
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                Text(ticket.title, modifier = Modifier.font(Font.headline).lineLimit(1))
                                Label(title = ticket.venue, systemImage = "mappin.and.ellipse", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                Label(title = ticket.time, systemImage = "clock", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                            Spacer()
                            Image(systemName = ticket.icon, modifier = Modifier.font(Font.title2).foregroundStyle(ticket.tint.color()))
                        }
                        Divider()
                        HStack(alignment = VerticalAlignment.bottom) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 10.0) {
                                HStack(spacing = 14.0) {
                                    for (seat in ticket.seats) key(seat.label) {
                                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                            Text(seat.label, modifier = Modifier.font(Font.system(size = 9.0, weight = Font.Weight.semibold)).foregroundStyle(ShapeStyle.secondary))
                                            Text(seat.value, modifier = Modifier.font(Font.subheadline).bold().monospacedDigit())
                                        }
                                    }
                                }
                                Label(
                                    title = ticket.status,
                                    systemImage = if (ticket.checkedIn) "checkmark.seal.fill" else "qrcode.viewfinder",
                                    modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold)
                                        .foregroundStyle(if (ticket.checkedIn) Color.green else ticket.tint.color())
                                        .padding(Edge.Set.horizontal, 8.0)
                                        .padding(Edge.Set.vertical, 4.0)
                                        .background((if (ticket.checkedIn) Color.green else ticket.tint.color()).opacity(0.15), Capsule()),
                                )
                            }
                            Spacer()
                            Grid(
                                modifier = Modifier.padding(6.0).background(Color.white, RoundedRectangle(cornerRadius = 6.0)),
                                horizontalSpacing = 1.0,
                                verticalSpacing = 1.0,
                            ) {
                                for (row in ticket.qr) key(row.id) {
                                    GridRow {
                                        for (cell in row.cells) key(cell.id) {
                                            ShapeView(Rectangle(), modifier = Modifier.frame(width = 7.0, height = 7.0), fill = if (cell.on) Color.black else Color.white)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen43 = BenchScreen("43 · Event tickets", ::Screen43ViewModel) { Screen43(it as Screen43ViewModel) }
