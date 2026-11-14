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

// Screen 14 — News feed: category chips, cards with gradient image placeholders, category capsule, byline.

data class Screen14Article(
    val id: Int,
    val category: String,
    val categoryTint: Rgb,
    val headline: String,
    val summary: String,
    val author: String,
    val initials: String,
    val time: String,
    val readTime: String,
    val symbol: String,
    val gradient: List<Rgb>,
    val bookmarked: Boolean,
)

data class Screen14State(
    val categories: List<String>,
    val selectedCategory: String,
    val articles: List<Screen14Article>,
    val tick: Int,
)

class Screen14ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen14State(
            categories = listOf("Top Stories", "Tech", "Science", "Business", "Sports", "Culture"),
            selectedCategory = "Top Stories",
            articles = listOf(
                Screen14Article(
                    1, "Science", Rgb(0.2, 0.78, 0.35),
                    "Deep-sea survey maps a hidden reef twice the size of a city",
                    "Researchers aboard a survey vessel used autonomous submersibles to chart a vast cold-water reef system, revealing dozens of species never documented before in the region.",
                    "Maya Okafor", "MO", "2h ago", "6 min read", "water.waves",
                    listOf(Rgb(0.0, 0.45, 0.7), Rgb(0.1, 0.8, 0.75)), false,
                ),
                Screen14Article(
                    2, "Tech", Rgb(0.0, 0.48, 1.0),
                    "New compiler release halves build times for large codebases",
                    "The update introduces incremental caching across modules and a rewritten scheduler. Early adopters report dramatic gains on continuous integration machines.",
                    "Leo Brandt", "LB", "4h ago", "4 min read", "cpu.fill",
                    listOf(Rgb(0.35, 0.34, 0.84), Rgb(0.0, 0.48, 1.0)), true,
                ),
                Screen14Article(
                    3, "Business", Rgb(1.0, 0.58, 0.0),
                    "Coffee prices climb as harvest forecasts are revised downward",
                    "Traders cite irregular rainfall in key growing regions. Roasters warn that retail prices could follow within the next two quarters.",
                    "Sofia Marin", "SM", "5h ago", "3 min read", "cup.and.saucer.fill",
                    listOf(Rgb(0.55, 0.35, 0.2), Rgb(1.0, 0.58, 0.0)), false,
                ),
                Screen14Article(
                    4, "Sports", Rgb(1.0, 0.23, 0.19),
                    "Underdogs clinch the title in a dramatic final-minute comeback",
                    "Two goals in stoppage time sealed the championship, ending a forty-year wait for the club and its supporters.",
                    "Tom Achebe", "TA", "8h ago", "5 min read", "soccerball",
                    listOf(Rgb(0.9, 0.1, 0.3), Rgb(1.0, 0.6, 0.3)), false,
                ),
            ),
            tick = 0,
        )
    )
    val state: StateFlow<Screen14State> = _state

    override fun mutate() = _state.update {
        val t = it.tick + 1
        it.copy(
            tick = t,
            selectedCategory = it.categories[t % it.categories.size],
            articles = it.articles.mapIndexed { i, a ->
                if (i == t % it.articles.size) a.copy(bookmarked = !a.bookmarked, time = "${2 + i * 2 + t % 3}h ago") else a
            },
        )
    }
}

@Composable
fun Screen14(vm: Screen14ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.background(Color.gray.opacity(0.1)).navigationTitle("News")) {
            VStack(modifier = Modifier.padding(Edge.Set.bottom, 16.0), alignment = HorizontalAlignment.leading, spacing = 16.0) {
                ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                    HStack(modifier = Modifier.padding(Edge.Set.horizontal), spacing = 8.0) {
                        for (category in state.categories) key(category) {
                            val selected = category == state.selectedCategory
                            Text(
                                category,
                                modifier = Modifier.font(Font.subheadline.weight(Font.Weight.medium))
                                    .padding(Edge.Set.horizontal, 14.0)
                                    .padding(Edge.Set.vertical, 7.0)
                                    .foregroundStyle(if (selected) Color.white else Color.primary)
                                    .background(if (selected) Color.blue else ShapeStyle.background, Capsule()),
                            )
                        }
                    }
                }
                for (article in state.articles) key(article.id) {
                    VStack(
                        modifier = Modifier.background(ShapeStyle.background)
                            .clipShape(RoundedRectangle(cornerRadius = 16.0))
                            .shadow(color = Color.black.opacity(0.08), radius = 6.0, y = 2.0)
                            .padding(Edge.Set.horizontal),
                        alignment = HorizontalAlignment.leading,
                        spacing = 0.0,
                    ) {
                        ZStack(modifier = Modifier.frame(height = 150.0), alignment = Alignment.bottomLeading) {
                            LinearGradientView(
                                LinearGradient(colors = article.gradient.map { it.color() }, startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                            )
                            ImageSystemName(
                                article.symbol,
                                modifier = Modifier.font(Font.system(size = 64.0, weight = Font.Weight.light))
                                    .foregroundStyle(Color.white.opacity(0.35))
                                    .frame(maxWidth = Double.POSITIVE_INFINITY, maxHeight = Double.POSITIVE_INFINITY),
                            )
                            Text(
                                article.category,
                                modifier = Modifier.font(Font.caption.weight(Font.Weight.bold))
                                    .textCase(Text.Case.uppercase)
                                    .foregroundStyle(article.categoryTint.color())
                                    .padding(Edge.Set.horizontal, 10.0)
                                    .padding(Edge.Set.vertical, 4.0)
                                    .background(ShapeStyle.background, Capsule())
                                    .padding(12.0),
                            )
                        }
                        VStack(modifier = Modifier.padding(14.0), alignment = HorizontalAlignment.leading, spacing = 8.0) {
                            Text(article.headline, modifier = Modifier.font(Font.headline).lineLimit(2))
                            Text(article.summary, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary).lineLimit(3))
                            HStack(modifier = Modifier.padding(Edge.Set.top, 4.0), spacing = 8.0) {
                                Text(
                                    article.initials,
                                    modifier = Modifier.font(Font.caption2.weight(Font.Weight.bold))
                                        .foregroundStyle(Color.white)
                                        .frame(width = 26.0, height = 26.0)
                                        .background(article.categoryTint.color(), Circle()),
                                )
                                Text(article.author, modifier = Modifier.font(Font.caption.weight(Font.Weight.semibold)))
                                Text("·", modifier = Modifier.foregroundStyle(ShapeStyle.tertiary))
                                Text(article.time, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                                Spacer()
                                Label(article.readTime, systemImage = "clock", modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                                ImageSystemName(
                                    if (article.bookmarked) "bookmark.fill" else "bookmark",
                                    modifier = Modifier.foregroundStyle(if (article.bookmarked) Color.orange else Color.gray),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val screen14 = BenchScreen("14 · News feed", ::Screen14ViewModel) { Screen14(it as Screen14ViewModel) }
