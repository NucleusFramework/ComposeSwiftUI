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

// Screen 40 — Restaurant menu: restaurant header, menu sections, dishes with prices and dietary tag capsules, toolbar.

data class Screen40Tag(val id: Int, val label: String, val tint: Rgb)

data class Screen40Dish(
    val id: Int,
    val name: String,
    val blurb: String,
    val price: String,
    val icon: String,
    val tags: List<Screen40Tag>,
    val quantity: Int,
    val quantityLabel: String,
    val popular: Boolean,
)

data class Screen40Course(val id: Int, val title: String, val dishes: List<Screen40Dish>)

data class Screen40State(
    val restaurant: String,
    val cuisine: String,
    val rating: String,
    val eta: String,
    val courses: List<Screen40Course>,
    val cartLabel: String,
)

private val screen40Veg = Screen40Tag(1, "Vegan", Rgb(0.2, 0.7, 0.35))
private val screen40Gf = Screen40Tag(2, "Gluten-free", Rgb(0.85, 0.6, 0.1))
private val screen40Spicy = Screen40Tag(3, "Spicy", Rgb(0.9, 0.25, 0.2))
private val screen40Veggie = Screen40Tag(4, "Vegetarian", Rgb(0.3, 0.6, 0.3))

private fun screen40Dish(id: Int, name: String, blurb: String, cents: Int, icon: String, tags: List<Screen40Tag>, popular: Boolean = false) =
    Screen40Dish(id, name, blurb, "$" + (cents / 100) + "." + (cents % 100).toString().padStart(2, '0'), icon, tags, 0, "", popular)

class Screen40ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen40State(
            restaurant = "Trattoria Lucia",
            cuisine = "Italian · Pasta · Wine bar",
            rating = "4.7",
            eta = "25–35 min",
            courses = listOf(
                Screen40Course(
                    1, "Starters",
                    listOf(
                        screen40Dish(11, "Bruschetta", "Grilled bread, tomato, basil, garlic", 850, "leaf", listOf(screen40Veg)),
                        screen40Dish(12, "Burrata", "Creamy burrata, heirloom tomatoes, olive oil", 1400, "circle.hexagongrid", listOf(screen40Veggie, screen40Gf), popular = true),
                        screen40Dish(13, "Calamari", "Crispy squid, lemon aioli", 1250, "fish", emptyList()),
                    ),
                ),
                Screen40Course(
                    2, "Pasta",
                    listOf(
                        screen40Dish(21, "Cacio e Pepe", "Tonnarelli, pecorino, black pepper", 1600, "fork.knife", listOf(screen40Veggie), popular = true),
                        screen40Dish(22, "Arrabbiata", "Penne, tomato, chili, parsley", 1450, "flame", listOf(screen40Veg, screen40Spicy)),
                        screen40Dish(23, "Lasagna", "Slow-cooked beef ragù, béchamel", 1850, "square.stack.3d.up", emptyList()),
                    ),
                ),
                Screen40Course(
                    3, "Desserts",
                    listOf(
                        screen40Dish(31, "Tiramisu", "Mascarpone, espresso, cocoa", 900, "cup.and.saucer", listOf(screen40Veggie)),
                        screen40Dish(32, "Sorbetto", "Lemon & basil sorbet", 700, "snowflake", listOf(screen40Veg, screen40Gf)),
                    ),
                ),
            ),
            cartLabel = "0",
        )
    )
    val state: StateFlow<Screen40State> = _state
    private val order = listOf(12, 21, 22, 31, 11)
    private var tick = 0

    override fun mutate() = _state.update { s ->
        val target = order[tick++ % order.size]
        val courses = s.courses.map { c ->
            c.copy(dishes = c.dishes.map { d ->
                if (d.id != target) d else {
                    val q = (d.quantity + 1) % 3
                    d.copy(quantity = q, quantityLabel = if (q == 0) "" else "${q}×")
                }
            })
        }
        s.copy(courses = courses, cartLabel = courses.sumOf { c -> c.dishes.sumOf { it.quantity } }.toString())
    }
}

@Composable
fun Screen40(vm: Screen40ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(
            modifier = Modifier.listStyle(ListStyle.plain).navigationTitle("Menu")
                .toolbarItem(placement = ToolbarItemPlacement.navigation) { Button(title = "Info", systemImage = "info.circle", action = {}) }
                .toolbarItem(placement = ToolbarItemPlacement.primaryAction) {
                    Button(action = {}) {
                        Label(modifier = Modifier.labelStyle(LabelStyle.titleAndIcon), title = { Text(verbatim = state.cartLabel, modifier = Modifier.monospacedDigit()) }, icon = { ImageSystemName("bag.fill") })
                    }
                },
        ) {
            Section {
                HStack(modifier = Modifier.padding(Edge.Set.vertical, 6.0), spacing = 14.0) {
                    ImageSystemName(
                        "fork.knife.circle.fill",
                        modifier = Modifier.font(Font.system(size = 44.0)).foregroundStyle(Color.white, Color.red).symbolRenderingMode(SymbolRenderingMode.palette),
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text(verbatim = state.restaurant, modifier = Modifier.font(Font.title2).bold())
                        Text(verbatim = state.cuisine, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        HStack(modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary), spacing = 10.0) {
                            Label(state.rating, systemImage = "star.fill")
                            Label(state.eta, systemImage = "bicycle")
                            Label("Open", systemImage = "clock")
                        }
                    }
                }
            }
            for (course in state.courses) key(course.id) {
                Section(header = { Text(verbatim = course.title, modifier = Modifier.font(Font.title3).bold().foregroundStyle(Color.primary).textCase(null)) }, content = {
                    for (dish in course.dishes) key(dish.id) {
                        HStack(modifier = Modifier.padding(Edge.Set.vertical, 4.0), alignment = VerticalAlignment.top, spacing = 12.0) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 5.0) {
                                HStack(spacing = 6.0) {
                                    Text(verbatim = dish.name, modifier = Modifier.font(Font.headline))
                                    if (dish.popular) {
                                        ImageSystemName("flame.fill", modifier = Modifier.font(Font.caption).foregroundStyle(Color.orange))
                                    }
                                }
                                Text(verbatim = dish.blurb, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).lineLimit(2))
                                HStack(spacing = 6.0) {
                                    Text(verbatim = dish.price, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).monospacedDigit())
                                    for (tag in dish.tags) key(tag.id) {
                                        Text(
                                            verbatim = tag.label,
                                            modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.medium).foregroundStyle(tag.tint.color())
                                                .padding(Edge.Set.horizontal, 7.0).padding(Edge.Set.vertical, 2.0)
                                                .background(tag.tint.color().opacity(0.14), Capsule()),
                                        )
                                    }
                                }
                            }
                            Spacer()
                            ZStack(modifier = Modifier.frame(width = 64.0, height = 64.0), alignment = Alignment.topTrailing) {
                                ImageSystemName(
                                    dish.icon,
                                    modifier = Modifier.font(Font.title2).foregroundStyle(Color.red.opacity(0.7))
                                        .frame(width = 64.0, height = 64.0)
                                        .background(Color.orange.opacity(0.12), RoundedRectangle(cornerRadius = 12.0)),
                                )
                                if (dish.quantity > 0) {
                                    Text(verbatim = dish.quantityLabel, modifier = Modifier.font(Font.caption2).bold().foregroundStyle(Color.white).padding(5.0).background(Color.red, Circle()).offset(x = 6.0, y = -6.0))
                                }
                            }
                        }
                    }
                })
            }
        }
    }
}

val screen40 = BenchScreen("40 · Restaurant menu", ::Screen40ViewModel) { Screen40(it as Screen40ViewModel) }
