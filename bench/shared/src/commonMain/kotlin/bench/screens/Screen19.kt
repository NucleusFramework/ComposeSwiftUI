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

// Screen 19 — Travel booking: segmented trip type, route card, flight cards with dashed route line and price.

data class Screen19Flight(
    val id: Int,
    val airline: String,
    val code: String,
    val tint: Rgb,
    val badge: String,
    val departTime: String,
    val arriveTime: String,
    val duration: String,
    val stops: String,
    val baggage: String,
    val price: String,
    val isBest: Boolean,
)

data class Screen19State(
    val tripTypes: List<String>,
    val tripType: String,
    val fromCode: String,
    val fromCity: String,
    val toCode: String,
    val toCity: String,
    val dates: String,
    val passengers: String,
    val resultsLabel: String,
    val flights: List<Screen19Flight>,
    val tick: Int,
)

private fun screen19Flights(tick: Int): List<Screen19Flight> = listOf(
    Screen19Flight(1, "SkyWays", "SW", Rgb(0.0, 0.48, 1.0), "Best value", "07:15", "15:40", "5h 25m", "Nonstop", "23 kg", "$${389 + tick}", true),
    Screen19Flight(2, "Pacific Air", "PA", Rgb(0.2, 0.68, 0.35), "Fastest", "09:05", "17:20", "5h 15m", "Nonstop", "23 kg", "$${452 + tick * 2}", false),
    Screen19Flight(3, "Northwind", "NW", Rgb(0.69, 0.32, 0.87), "Flexible", "12:40", "23:55", "8h 15m", "1 stop · DEN", "2 × 23 kg", "$${318 + tick % 7}", false),
    Screen19Flight(4, "Aurora", "AU", Rgb(1.0, 0.45, 0.2), "Eco", "18:30", "02:45", "5h 15m", "Nonstop", "Carry-on", "$${279 + tick % 5}", false),
)

class Screen19ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen19State(
            tripTypes = listOf("One way", "Round trip", "Multi-city"),
            tripType = "Round trip",
            fromCode = "SFO",
            fromCity = "San Francisco",
            toCode = "JFK",
            toCity = "New York",
            dates = "Fri 20 Nov – Sun 29 Nov",
            passengers = "2 adults · Economy",
            resultsLabel = "4 flights found",
            flights = screen19Flights(0),
            tick = 0,
        )
    )
    val state: StateFlow<Screen19State> = _state

    fun setTripType(value: String) = _state.update { it.copy(tripType = value) }

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(tick = t, tripType = it.tripTypes[t % it.tripTypes.size], flights = screen19Flights(t), passengers = "${2 + t % 3} adults · Economy")
    }
}

@Composable
fun Screen19(vm: Screen19ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.background(Color.gray.opacity(0.1)).navigationTitle("Flights")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                Picker(
                    title = "Trip type",
                    selection = state.tripType,
                    modifier = Modifier.pickerStyle(PickerStyle.segmented).labelsHidden(),
                    onSelectionChange = { vm.setTripType(it as String) },
                ) {
                    for (type in state.tripTypes) key(type) {
                        Text(type, modifier = Modifier.tag(type))
                    }
                }
                VStack(
                    modifier = Modifier.padding().background(ShapeStyle.background, RoundedRectangle(cornerRadius = 18.0)),
                    spacing = 14.0,
                ) {
                    HStack {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Text("From", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            Text(state.fromCode, modifier = Modifier.font(Font.system(size = 30.0, weight = Font.Weight.bold)))
                            Text(state.fromCity, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                        Spacer()
                        Image(systemName = 
                            "arrow.left.arrow.right",
                            modifier = Modifier.font(Font.headline)
                                .foregroundStyle(Color.white)
                                .frame(width = 40.0, height = 40.0)
                                .background(Color.blue, Circle()),
                        )
                        Spacer()
                        VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) {
                            Text("To", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            Text(state.toCode, modifier = Modifier.font(Font.system(size = 30.0, weight = Font.Weight.bold)))
                            Text(state.toCity, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                    Divider()
                    HStack(modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary)) {
                        Label(state.dates, systemImage = "calendar")
                        Spacer()
                        Label(state.passengers, systemImage = "person.2.fill")
                    }
                }
                Text(state.resultsLabel, modifier = Modifier.font(Font.headline))
                for (flight in state.flights) key(flight.id) {
                    VStack(
                        modifier = Modifier.padding()
                            .background(ShapeStyle.background, RoundedRectangle(cornerRadius = 18.0))
                            .overlay { ShapeView(RoundedRectangle(cornerRadius = 18.0), stroke = if (flight.isBest) Color.blue else Color.clear, lineWidth = 2.0) },
                        spacing = 14.0,
                    ) {
                        HStack(spacing = 10.0) {
                            Text(
                                flight.code,
                                modifier = Modifier.font(Font.caption.weight(Font.Weight.heavy))
                                    .foregroundStyle(Color.white)
                                    .frame(width = 32.0, height = 32.0)
                                    .background(flight.tint.color(), RoundedRectangle(cornerRadius = 8.0)),
                            )
                            Text(flight.airline, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)))
                            Spacer()
                            Text(
                                flight.badge,
                                modifier = Modifier.font(Font.caption2.weight(Font.Weight.bold))
                                    .foregroundStyle(flight.tint.color())
                                    .padding(Edge.Set.horizontal, 8.0)
                                    .padding(Edge.Set.vertical, 4.0)
                                    .background(flight.tint.color().opacity(0.12), Capsule()),
                            )
                        }
                        HStack(spacing = 10.0) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                                Text(flight.departTime, modifier = Modifier.font(Font.title2.bold()).monospacedDigit())
                                Text(state.fromCode, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                            VStack(spacing = 4.0) {
                                Text(flight.duration, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                                ZStack {
                                    ShapeView(
                                        Rectangle(),
                                        modifier = Modifier.frame(height = 1.0),
                                        stroke = Color.gray.opacity(0.6),
                                        strokeStyle = StrokeStyle(lineWidth = 1.0, dash = listOf(4.0, 3.0)),
                                    )
                                    Image(systemName = 
                                        "airplane",
                                        modifier = Modifier.font(Font.caption)
                                            .foregroundStyle(flight.tint.color())
                                            .padding(Edge.Set.horizontal, 4.0)
                                            .background(ShapeStyle.background),
                                    )
                                }
                                Text(flight.stops, modifier = Modifier.font(Font.caption2).foregroundStyle(if (flight.stops == "Nonstop") Color.green else Color.orange))
                            }
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) {
                                Text(flight.arriveTime, modifier = Modifier.font(Font.title2.bold()).monospacedDigit())
                                Text(state.toCode, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            }
                        }
                        Divider()
                        HStack {
                            Label(flight.baggage, systemImage = "suitcase.rolling.fill", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                            Spacer()
                            Text("per person", modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                            Text(flight.price, modifier = Modifier.font(Font.title3.bold()).monospacedDigit().foregroundStyle(Color.blue))
                        }
                    }
                }
            }
        }
    }
}

val screen19 = BenchScreen("19 · Travel booking", ::Screen19ViewModel) { Screen19(it as Screen19ViewModel) }
