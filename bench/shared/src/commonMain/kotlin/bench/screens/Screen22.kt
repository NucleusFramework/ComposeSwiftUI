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

// Screen 22 — Map-like overview: shape canvas (water, land, roads, park), offset pins with labels, bottom info card.

data class Screen22Land(val id: Int, val width: Double, val height: Double, val x: Double, val y: Double, val tint: Rgb, val oval: Boolean)

data class Screen22Road(val id: Int, val length: Double, val x: Double, val y: Double, val angle: Double)

data class Screen22Pin(
    val id: Int,
    val label: String,
    val icon: String,
    val tint: Rgb,
    val x: Double,
    val y: Double,
    val selected: Boolean,
)

data class Screen22Place(
    val title: String,
    val category: String,
    val address: String,
    val rating: String,
    val distance: String,
    val eta: String,
    val open: String,
)

data class Screen22State(
    val lands: List<Screen22Land>,
    val roads: List<Screen22Road>,
    val pins: List<Screen22Pin>,
    val place: Screen22Place,
    val scaleLabel: String,
)

private val screen22Places = listOf(
    Screen22Place("Harbor Café", "Coffee shop", "12 Quay Street", "4.7", "350 m", "4 min", "Open until 18:00"),
    Screen22Place("Lighthouse Museum", "Museum", "1 Cape Road", "4.5", "1.2 km", "15 min", "Open until 17:30"),
    Screen22Place("Old Town Market", "Market", "Place du Marché", "4.8", "800 m", "10 min", "Open until 20:00"),
    Screen22Place("Pine Park", "Park", "Avenue des Pins", "4.6", "1.9 km", "24 min", "Open 24 hours"),
)

private fun screen22Pins(selected: Int) = listOf(
    Screen22Pin(0, "Café", "cup.and.saucer.fill", Rgb(0.6, 0.4, 0.2), 70.0, 210.0, false),
    Screen22Pin(1, "Museum", "building.columns.fill", Rgb(0.35, 0.34, 0.84), 250.0, 90.0, false),
    Screen22Pin(2, "Market", "cart.fill", Rgb(1.0, 0.58, 0.0), 160.0, 330.0, false),
    Screen22Pin(3, "Park", "leaf.fill", Rgb(0.2, 0.7, 0.35), 280.0, 250.0, false),
).map { it.copy(selected = it.id == selected) }

class Screen22ViewModel : BenchViewModel() {
    private var selected = 0
    private val _state = MutableStateFlow(
        Screen22State(
            lands = listOf(
                Screen22Land(1, 260.0, 220.0, -40.0, 150.0, Rgb(0.89, 0.92, 0.84), true),
                Screen22Land(2, 220.0, 180.0, 200.0, 20.0, Rgb(0.91, 0.9, 0.85), true),
                Screen22Land(3, 180.0, 140.0, 220.0, 240.0, Rgb(0.78, 0.9, 0.74), false),
                Screen22Land(4, 160.0, 110.0, 90.0, 300.0, Rgb(0.93, 0.91, 0.86), false),
            ),
            roads = listOf(
                Screen22Road(1, 320.0, 30.0, 230.0, -20.0),
                Screen22Road(2, 260.0, 160.0, 160.0, 65.0),
                Screen22Road(3, 200.0, 40.0, 330.0, 10.0),
            ),
            pins = screen22Pins(0),
            place = screen22Places[0],
            scaleLabel = "200 m",
        )
    )
    val state: StateFlow<Screen22State> = _state

    override fun mutate() {
        selected = (selected + 1) % screen22Places.size
        _state.update { it.copy(pins = screen22Pins(selected), place = screen22Places[selected]) }
    }
}

@Composable
private fun Screen22Canvas(state: Screen22State) {
    ZStack(
        modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY, alignment = Alignment.topLeading)
            .background(Color(red = 0.67, green = 0.82, blue = 0.95))
            .clipShape(Rectangle()),
        alignment = Alignment.topLeading,
    ) {
        for (land in state.lands) key(land.id) {
            if (land.oval) {
                ShapeView(Ellipse(), modifier = Modifier.frame(width = land.width, height = land.height).offset(x = land.x, y = land.y), fill = land.tint.color())
            } else {
                ShapeView(
                    RoundedRectangle(cornerRadius = 28.0),
                    modifier = Modifier.frame(width = land.width, height = land.height).offset(x = land.x, y = land.y),
                    fill = land.tint.color(),
                    stroke = Color.white.opacity(0.6),
                    lineWidth = 2.0,
                )
            }
        }
        for (road in state.roads) key(road.id) {
            ShapeView(
                Capsule(),
                modifier = Modifier.frame(width = road.length, height = 8.0)
                    .rotationEffect(Angle(degrees = road.angle))
                    .offset(x = road.x, y = road.y),
                fill = Color.white,
            )
        }
        for (pin in state.pins) key(pin.id) {
            VStack(modifier = Modifier.offset(x = pin.x, y = pin.y), spacing = 4.0) {
                Image(systemName = 
                    pin.icon,
                    modifier = Modifier.font(Font.system(size = if (pin.selected) 18.0 else 13.0, weight = Font.Weight.semibold))
                        .foregroundStyle(Color.white)
                        .frame(width = if (pin.selected) 44.0 else 30.0, height = if (pin.selected) 44.0 else 30.0)
                        .background(pin.tint.color(), Circle())
                        .overlay { ShapeView(Circle(), stroke = Color.white, lineWidth = 3.0) }
                        .shadow(color = Color.black.opacity(0.25), radius = 3.0, y = 2.0),
                )
                Text(
                    pin.label,
                    modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.semibold)
                        .padding(Edge.Set.horizontal, 6.0)
                        .padding(Edge.Set.vertical, 2.0)
                        .background(Color.white, Capsule()),
                )
            }
        }
        HStack(modifier = Modifier.padding(8.0).background(Color.white.opacity(0.85), RoundedRectangle(cornerRadius = 6.0)).offset(x = 12.0, y = 12.0), spacing = 6.0) {
            ShapeView(Rectangle(), modifier = Modifier.frame(width = 40.0, height = 3.0), fill = Color.black)
            Text(state.scaleLabel, modifier = Modifier.font(Font.caption2).monospacedDigit())
        }
    }
}

@Composable
private fun Screen22Card(place: Screen22Place) {
    VStack(
        modifier = Modifier.padding(16.0)
            .background(Color.white, RoundedRectangle(cornerRadius = 20.0))
            .shadow(color = Color.black.opacity(0.15), radius = 10.0, y = 4.0)
            .padding(12.0),
        alignment = HorizontalAlignment.leading,
        spacing = 10.0,
    ) {
        HStack(alignment = VerticalAlignment.top) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                Text(place.title, modifier = Modifier.font(Font.title3).bold())
                Text(place.category, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
            }
            Spacer()
            Image(systemName = "xmark.circle.fill", modifier = Modifier.font(Font.title2).symbolRenderingMode(SymbolRenderingMode.hierarchical).foregroundStyle(Color.gray))
        }
        HStack(spacing = 14.0) {
            Label(place.rating, systemImage = "star.fill", modifier = Modifier.foregroundStyle(Color.orange))
            Label(place.distance, systemImage = "location.fill")
            Label(place.eta, systemImage = "figure.walk")
        }
        Text(place.open, modifier = Modifier.font(Font.footnote).foregroundStyle(Color.green))
        Text(place.address, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
        HStack(spacing = 10.0) {
            Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent)) {
                Label("Directions", systemImage = "arrow.triangle.turn.up.right.diamond.fill", modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY))
            }
            Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered)) {
                Image(systemName = "phone.fill")
            }
            Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered)) {
                Image(systemName = "square.and.arrow.up")
            }
        }
    }
}

@Composable
fun Screen22(vm: Screen22ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ZStack(modifier = Modifier.navigationTitle("Explore"), alignment = Alignment.bottom) {
            Screen22Canvas(state)
            Screen22Card(state.place)
        }
    }
}

val screen22 = BenchScreen("22 · Map overview", ::Screen22ViewModel) { Screen22(it as Screen22ViewModel) }
