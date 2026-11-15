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

// Screen 45 — Real-estate listing: gradient photo placeholder, price, spec icons, description, features and agent card.

data class Screen45Spec(val id: Int, val icon: String, val value: String, val label: String)

data class Screen45Feature(val id: Int, val icon: String, val title: String)

data class Screen45State(
    val gradient: List<Rgb>,
    val photoCount: String,
    val status: String,
    val favorite: Boolean,
    val price: String,
    val priceNote: String,
    val priceReduced: Boolean,
    val address: String,
    val city: String,
    val specs: List<Screen45Spec>,
    val about: String,
    val features: List<Screen45Feature>,
    val agentInitials: String,
    val agentName: String,
    val agency: String,
    val rating: String,
    val views: String,
)

class Screen45ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen45State(
            gradient = listOf(Rgb(0.98, 0.72, 0.45), Rgb(0.93, 0.42, 0.45), Rgb(0.45, 0.32, 0.72)),
            photoCount = "1 / 24",
            status = "FOR SALE",
            favorite = false,
            price = "$1,249,000",
            priceNote = "Est. $6,120/mo",
            priceReduced = false,
            address = "2417 Willow Creek Lane",
            city = "Palo Alto, CA 94301",
            specs = listOf(
                Screen45Spec(1, "bed.double.fill", "4", "Beds"),
                Screen45Spec(2, "shower.fill", "3", "Baths"),
                Screen45Spec(3, "square.dashed", "2,380", "Sq ft"),
                Screen45Spec(4, "car.fill", "2", "Garage"),
            ),
            about = "Sun-filled craftsman home on a quiet tree-lined street. Open kitchen with quartz counters, vaulted living room, and a landscaped backyard with a cedar deck. Walking distance to parks and top-rated schools.",
            features = listOf(
                Screen45Feature(1, "leaf.fill", "Garden"),
                Screen45Feature(2, "sun.max.fill", "Solar panels"),
                Screen45Feature(3, "flame.fill", "Fireplace"),
                Screen45Feature(4, "bolt.car.fill", "EV charger"),
                Screen45Feature(5, "snowflake", "Central A/C"),
                Screen45Feature(6, "wifi", "Fiber internet"),
            ),
            agentInitials = "MR",
            agentName = "Maya Rodriguez",
            agency = "Bayview Realty Group",
            rating = "4.9 · 128 reviews",
            views = "312 views",
        )
    )
    val state: StateFlow<Screen45State> = _state

    private var tick = 0

    override fun mutate() {
        tick += 1
        _state.update {
            val reduced = tick % 2 == 1
            it.copy(
                favorite = !it.favorite,
                priceReduced = reduced,
                price = if (reduced) "$1,199,000" else "$1,249,000",
                photoCount = "${tick % 24 + 1} / 24",
                views = "${312 + tick} views",
            )
        }
    }
}

@Composable
fun Screen45(vm: Screen45ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Listing")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 18.0) {
                ZStack(modifier = Modifier.frame(height = 210.0).clipShape(RoundedRectangle(cornerRadius = 20.0)), alignment = Alignment.bottomLeading) {
                    LinearGradientView(LinearGradient(colors = state.gradient.map { it.color() }, startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing))
                    Image(systemName = 
                        "house.lodge.fill",
                        modifier = Modifier.font(Font.system(size = 72.0)).foregroundStyle(Color.white.opacity(0.85))
                            .frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY),
                    )
                    HStack(modifier = Modifier.padding(12.0)) {
                        Text(
                            state.status,
                            modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.bold).foregroundStyle(Color.white)
                                .padding(Edge.Set.horizontal, 10.0).padding(Edge.Set.vertical, 5.0)
                                .background(Color.green, Capsule()),
                        )
                        Spacer()
                        Label(
                            title = state.photoCount,
                            systemImage = "photo.on.rectangle",
                            modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(Color.white)
                                .padding(Edge.Set.horizontal, 10.0).padding(Edge.Set.vertical, 5.0)
                                .background(Color.black.opacity(0.35), Capsule()),
                        )
                    }
                }
                HStack(alignment = VerticalAlignment.top) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        HStack(spacing = 8.0) {
                            Text(state.price, modifier = Modifier.font(Font.title).fontWeight(Font.Weight.bold).monospacedDigit())
                            if (state.priceReduced) {
                                Label(title = "Reduced", systemImage = "arrow.down", modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).foregroundStyle(Color.red))
                            }
                        }
                        Text(state.address, modifier = Modifier.font(Font.headline))
                        Text(state.city, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        Text(state.priceNote, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.tertiary))
                    }
                    Spacer()
                    Image(systemName = 
                        if (state.favorite) "heart.fill" else "heart",
                        modifier = Modifier.font(Font.title2).foregroundStyle(if (state.favorite) Color.pink else Color.gray)
                            .frame(width = 44.0, height = 44.0)
                            .background(Color.gray.opacity(0.12), Circle()),
                    )
                }
                HStack(
                    modifier = Modifier.padding(Edge.Set.vertical, 12.0)
                        .background(Color.indigo.opacity(0.07), RoundedRectangle(cornerRadius = 14.0)),
                    spacing = 0.0,
                ) {
                    for (spec in state.specs) key(spec.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 4.0) {
                            Image(systemName = spec.icon, modifier = Modifier.font(Font.title3).foregroundStyle(Color.indigo))
                            Text(spec.value, modifier = Modifier.font(Font.headline).monospacedDigit())
                            Text(spec.label, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary).textCase(Text.Case.uppercase))
                        }
                    }
                }
                VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) {
                    Text("About this home", modifier = Modifier.font(Font.title3).bold())
                    Text(state.about, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).lineSpacing(3.0))
                }
                Grid(alignment = Alignment.leading, horizontalSpacing = 16.0, verticalSpacing = 10.0) {
                    for (rowIndex in 0 until (state.features.size + 1) / 2) key(rowIndex) {
                        GridRow {
                            for (feature in state.features.drop(rowIndex * 2).take(2)) key(feature.id) {
                                Label(title = feature.title, systemImage = feature.icon, modifier = Modifier.font(Font.subheadline).foregroundStyle(Color.primary))
                            }
                        }
                    }
                }
                HStack(
                    modifier = Modifier.padding(14.0).background(Color.gray.opacity(0.1), RoundedRectangle(cornerRadius = 16.0)),
                    spacing = 12.0,
                ) {
                    Text(
                        state.agentInitials,
                        modifier = Modifier.font(Font.headline).foregroundStyle(Color.white)
                            .frame(width = 48.0, height = 48.0)
                            .background(LinearGradient(colors = listOf(Color.teal, Color.blue), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom), Circle()),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                        Text(state.agentName, modifier = Modifier.font(Font.headline))
                        Text(state.agency, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        Label(title = state.rating, systemImage = "star.fill", modifier = Modifier.font(Font.caption2).foregroundStyle(Color.orange))
                    }
                    Spacer()
                    Image(systemName = "phone.fill", modifier = Modifier.foregroundStyle(Color.white).frame(width = 36.0, height = 36.0).background(Color.green, Circle()))
                    Image(systemName = "message.fill", modifier = Modifier.foregroundStyle(Color.white).frame(width = 36.0, height = 36.0).background(Color.blue, Circle()))
                }
                Label(title = state.views, systemImage = "eye", modifier = Modifier.font(Font.footnote).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
            }
        }
    }
}

val screen45 = BenchScreen("45 · Real-estate listing", ::Screen45ViewModel) { Screen45(it as Screen45ViewModel) }
