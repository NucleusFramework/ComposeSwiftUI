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

// Screen 37 — Boarding pass: ticket card with airport codes, plane icon, details grid and a barcode made of Rectangles.

data class Screen37Field(val id: Int, val label: String, val value: String)

data class Screen37Bar(val id: Int, val width: Double, val tall: Boolean)

data class Screen37State(
    val airline: String,
    val flight: String,
    val fromCode: String,
    val fromCity: String,
    val fromTime: String,
    val toCode: String,
    val toCity: String,
    val toTime: String,
    val duration: String,
    val rows: List<List<Screen37Field>>,
    val status: String,
    val onTime: Boolean,
    val bars: List<Screen37Bar>,
    val code: String,
)

private fun screen37Rows(gate: String, boarding: String): List<List<Screen37Field>> = listOf(
    listOf(Screen37Field(1, "PASSENGER", "Grace Hopper"), Screen37Field(2, "SEAT", "14A")),
    listOf(Screen37Field(3, "GATE", gate), Screen37Field(4, "BOARDING", boarding)),
    listOf(Screen37Field(5, "CLASS", "Economy+"), Screen37Field(6, "GROUP", "B")),
)

class Screen37ViewModel : BenchViewModel() {
    private val gates = listOf("B22", "B24", "C03", "A11")
    private var tick = 0
    private val _state = MutableStateFlow(
        Screen37State(
            airline = "SKYLINE AIR",
            flight = "SK 2047 · Fri 15 Nov",
            fromCode = "CDG",
            fromCity = "Paris",
            fromTime = "08:45",
            toCode = "JFK",
            toCity = "New York",
            toTime = "11:20",
            duration = "8h 35m",
            rows = screen37Rows("B22", "08:05"),
            status = "On time",
            onTime = true,
            bars = (0 until 48).map { i -> Screen37Bar(i, listOf(1.0, 3.0, 2.0, 1.0, 4.0, 2.0, 1.0)[(i * 5 + i / 3) % 7], i % 9 != 4) },
            code = "SKY2047CDGJFK14A",
        )
    )
    val state: StateFlow<Screen37State> = _state

    override fun mutate() = _state.update { s ->
        tick++
        val onTime = tick % 2 == 0
        s.copy(
            rows = screen37Rows(gates[tick % gates.size], if (onTime) "08:05" else "08:25"),
            status = if (onTime) "On time" else "Delayed 20 min",
            onTime = onTime,
            bars = s.bars.map { it.copy(width = if (it.id == tick % s.bars.size) 5.0 - it.width else it.width) },
        )
    }
}

@Composable
fun Screen37(vm: Screen37ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.background(Color.indigo.opacity(0.08)).navigationTitle("Boarding Pass")) {
            VStack(modifier = Modifier.padding(), spacing = 16.0) {
                VStack(
                    modifier = Modifier.background(ShapeStyle.background).clipShape(RoundedRectangle(cornerRadius = 20.0)).shadow(color = Color.black.opacity(0.12), radius = 10.0, y = 4.0),
                    spacing = 0.0,
                ) {
                    VStack(
                        modifier = Modifier.foregroundStyle(Color.white).padding(20.0).background(
                            LinearGradient(colors = listOf(Color.indigo, Color.blue), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                        ),
                        spacing = 16.0,
                    ) {
                        HStack {
                            Label(state.airline, systemImage = SFSymbol.airplaneCircleFill, modifier = Modifier.font(Font.headline).kerning(1.0))
                            Spacer()
                            Text(verbatim = state.flight, modifier = Modifier.font(Font.caption).opacity(0.85))
                        }
                        HStack(alignment = VerticalAlignment.center) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(verbatim = state.fromCode, modifier = Modifier.font(Font.system(size = 40.0, weight = Font.Weight.heavy)).fontDesign(Font.Design.rounded))
                                Text(verbatim = state.fromCity, modifier = Modifier.font(Font.caption).opacity(0.85))
                                Text(verbatim = state.fromTime, modifier = Modifier.font(Font.title3).bold().monospacedDigit())
                            }
                            Spacer()
                            VStack(spacing = 6.0) {
                                HStack(spacing = 4.0) {
                                    for (i in 0 until 3) key(i) {
                                        ShapeView(Circle(), modifier = Modifier.frame(width = 4.0, height = 4.0), fill = Color.white.opacity(0.6))
                                    }
                                    Image(systemName = SFSymbol.airplane, modifier = Modifier.font(Font.title2))
                                    for (i in 3 until 6) key(i) {
                                        ShapeView(Circle(), modifier = Modifier.frame(width = 4.0, height = 4.0), fill = Color.white.opacity(0.6))
                                    }
                                }
                                Text(verbatim = state.duration, modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold).opacity(0.85))
                            }
                            Spacer()
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) {
                                Text(verbatim = state.toCode, modifier = Modifier.font(Font.system(size = 40.0, weight = Font.Weight.heavy)).fontDesign(Font.Design.rounded))
                                Text(verbatim = state.toCity, modifier = Modifier.font(Font.caption).opacity(0.85))
                                Text(verbatim = state.toTime, modifier = Modifier.font(Font.title3).bold().monospacedDigit())
                            }
                        }
                    }
                    HStack(modifier = Modifier.padding(Edge.Set.horizontal, 12.0).frame(height = 1.0), spacing = 5.0) {
                        for (i in 0 until 40) key(i) {
                            ShapeView(Rectangle(), modifier = Modifier.frame(width = 4.0, height = 1.0), fill = Color.gray.opacity(0.5))
                        }
                    }
                    VStack(modifier = Modifier.padding(20.0), spacing = 18.0) {
                        Grid(alignment = Alignment.leading, horizontalSpacing = 24.0, verticalSpacing = 14.0) {
                            for (row in state.rows) key(row.first().id) {
                                GridRow {
                                    for (field in row) key(field.id) {
                                        VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                                            Text(verbatim = field.label, modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold).foregroundStyle(ShapeStyle.secondary))
                                            Text(verbatim = field.value, modifier = Modifier.font(Font.headline))
                                        }
                                    }
                                }
                            }
                        }
                        HStack {
                            Label(state.status, systemImage = if (state.onTime) SFSymbol.checkmarkSealFill else SFSymbol.clockBadgeExclamationmarkFill, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).foregroundStyle(if (state.onTime) Color.green else Color.orange))
                            Spacer()
                        }
                        VStack(spacing = 6.0) {
                            HStack(alignment = VerticalAlignment.bottom, spacing = 2.0) {
                                for (bar in state.bars) key(bar.id) {
                                    ShapeView(Rectangle(), modifier = Modifier.frame(width = bar.width, height = if (bar.tall) 64.0 else 56.0), fill = Color.primary)
                                }
                            }
                            Text(verbatim = state.code, modifier = Modifier.font(Font.caption2).monospaced().kerning(2.0).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
                Label("Add to Apple Wallet", systemImage = SFSymbol.walletPassFill, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).foregroundStyle(Color.white).frame(maxWidth = Double.POSITIVE_INFINITY).padding(Edge.Set.vertical, 12.0).background(Color.black, RoundedRectangle(cornerRadius = 12.0)))
            }
        }
    }
}

val screen37 = BenchScreen("37 · Boarding pass", ::Screen37ViewModel) { Screen37(it as Screen37ViewModel) }
