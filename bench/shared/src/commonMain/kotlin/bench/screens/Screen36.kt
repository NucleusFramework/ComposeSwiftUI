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

// Screen 36 — Hotel detail: gradient hero, rating stars, amenities grid of Labels, description, price/book bar.

data class Screen36Star(val id: Int, val symbol: String)

data class Screen36Amenity(val id: Int, val title: String, val icon: String)

data class Screen36Review(val id: Int, val label: String, val score: String, val value: Double)

data class Screen36State(
    val name: String,
    val location: String,
    val stars: List<Screen36Star>,
    val rating: String,
    val reviewsCount: String,
    val favorite: Boolean,
    val amenities: List<Screen36Amenity>,
    val about: String,
    val reviews: List<Screen36Review>,
    val price: String,
    val priceDetail: String,
    val heroFrom: Rgb,
    val heroTo: Rgb,
)

private fun screen36Stars(rating: Double): List<Screen36Star> = (0 until 5).map { i ->
    val symbol = when {
        rating >= i + 1 -> "star.fill"
        rating >= i + 0.5 -> "star.leadinghalf.filled"
        else -> "star"
    }
    Screen36Star(i, symbol)
}

class Screen36ViewModel : BenchViewModel() {
    private var nights = 3
    private val _state = MutableStateFlow(
        Screen36State(
            name = "Hotel Miramar",
            location = "Positano, Amalfi Coast",
            stars = screen36Stars(4.5),
            rating = "4.6",
            reviewsCount = "1,284 reviews",
            favorite = false,
            amenities = listOf(
                Screen36Amenity(1, "Free Wi-Fi", "wifi"),
                Screen36Amenity(2, "Pool", "figure.pool.swim"),
                Screen36Amenity(3, "Breakfast", "cup.and.saucer.fill"),
                Screen36Amenity(4, "Parking", "car.fill"),
                Screen36Amenity(5, "Spa", "sparkles"),
                Screen36Amenity(6, "Sea view", "water.waves"),
            ),
            about = "A family-run cliffside hotel with terraced rooms overlooking the bay, a heated infinity pool and a rooftop restaurant serving local seafood.",
            reviews = listOf(
                Screen36Review(1, "Cleanliness", "4.8", 0.96),
                Screen36Review(2, "Location", "4.9", 0.98),
                Screen36Review(3, "Service", "4.5", 0.9),
                Screen36Review(4, "Value", "4.1", 0.82),
            ),
            price = screen36Price(3),
            priceDetail = screen36Detail(3),
            heroFrom = Rgb(0.0, 0.62, 0.86),
            heroTo = Rgb(0.98, 0.55, 0.35),
        )
    )
    val state: StateFlow<Screen36State> = _state

    override fun mutate() {
        nights = nights % 7 + 1
        _state.update { it.copy(favorite = !it.favorite, price = screen36Price(nights), priceDetail = screen36Detail(nights)) }
    }
}

private fun screen36Price(nights: Int) = "€" + (245 * nights).toString()
private fun screen36Detail(nights: Int) = if (nights == 1) "1 night · taxes incl." else "$nights nights · taxes incl."

@Composable
fun Screen36(vm: Screen36ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(
            modifier = Modifier.navigationTitle("Hotel")
                .safeAreaInset(edge = VerticalEdge.bottom) {
                    HStack(modifier = Modifier.padding(Edge.Set.horizontal, 20.0).padding(Edge.Set.vertical, 12.0).background(Material.bar)) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                            Text(verbatim = state.price, modifier = Modifier.font(Font.title2).bold().monospacedDigit())
                            Text(verbatim = state.priceDetail, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                        Spacer()
                        Button(title = "Book now", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).controlSize(ControlSize.large).tint(Color.orange), action = {})
                    }
                },
        ) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 18.0) {
                ZStack(modifier = Modifier.frame(height = 200.0), alignment = Alignment.topTrailing) {
                    ShapeView(
                        Rectangle(),
                        fill = LinearGradient(colors = listOf(state.heroFrom.color(), state.heroTo.color()), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom),
                    )
                    VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY), spacing = 8.0) {
                        Image(systemName = SFSymbol.building2Fill, modifier = Modifier.font(Font.system(size = 56.0)).foregroundStyle(Color.white.opacity(0.9)))
                        Image(systemName = SFSymbol.waterWaves, modifier = Modifier.font(Font.title).foregroundStyle(Color.white.opacity(0.6)))
                    }
                    Image(systemName = 
                        if (state.favorite) SFSymbol.heartFill else SFSymbol.heart,
                        modifier = Modifier.font(Font.title3).foregroundStyle(if (state.favorite) Color.red else Color.white)
                            .padding(10.0)
                            .background(Color.black.opacity(0.25), Circle())
                            .padding(14.0),
                    )
                }
                VStack(modifier = Modifier.padding(Edge.Set.horizontal), alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    Text(verbatim = state.name, modifier = Modifier.font(Font.title).bold())
                    Label(state.location, systemImage = SFSymbol.mappinAndEllipse, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    HStack(spacing = 2.0) {
                        for (star in state.stars) key(star.id) {
                            Image(systemName = SFSymbol(star.symbol), modifier = Modifier.foregroundStyle(Color.yellow))
                        }
                        Text(verbatim = state.rating, modifier = Modifier.fontWeight(Font.Weight.semibold).padding(Edge.Set.leading, 6.0))
                        Text(verbatim = "(${state.reviewsCount})", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                    }
                }
                VStack(modifier = Modifier.padding(Edge.Set.horizontal), alignment = HorizontalAlignment.leading, spacing = 12.0) {
                    Text("Amenities", modifier = Modifier.font(Font.headline))
                    LazyVGrid(columns = listOf(GridItem(), GridItem()), alignment = HorizontalAlignment.leading, spacing = 12.0) {
                        for (amenity in state.amenities) key(amenity.id) {
                            Label(
                                amenity.title,
                                systemImage = SFSymbol(amenity.icon),
                                modifier = Modifier.font(Font.subheadline)
                                    .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                                    .padding(10.0)
                                    .background(Color.blue.opacity(0.08), RoundedRectangle(cornerRadius = 10.0)),
                            )
                        }
                    }
                }
                VStack(modifier = Modifier.padding(Edge.Set.horizontal), alignment = HorizontalAlignment.leading, spacing = 8.0) {
                    Text("About", modifier = Modifier.font(Font.headline))
                    Text(verbatim = state.about, modifier = Modifier.font(Font.body).foregroundStyle(ShapeStyle.secondary).lineSpacing(3.0))
                }
                VStack(modifier = Modifier.padding(Edge.Set.horizontal).padding(Edge.Set.bottom), alignment = HorizontalAlignment.leading, spacing = 10.0) {
                    Text("Guest ratings", modifier = Modifier.font(Font.headline))
                    for (review in state.reviews) key(review.id) {
                        HStack(spacing = 12.0) {
                            Text(verbatim = review.label, modifier = Modifier.font(Font.subheadline).frame(width = 100.0, alignment = Alignment.leading))
                            ProgressView(value = review.value, modifier = Modifier.tint(Color.orange))
                            Text(verbatim = review.score, modifier = Modifier.font(Font.subheadline).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
            }
        }
    }
}

val screen36 = BenchScreen("36 · Hotel detail", ::Screen36ViewModel) { Screen36(it as Screen36ViewModel) }
