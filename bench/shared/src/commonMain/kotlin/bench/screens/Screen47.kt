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

// Screen 47 — Book library: currently-reading card with a progress Gauge, yearly goal, shelves of gradient covers with ratings.

data class Screen47Star(val id: Int, val icon: String)

data class Screen47Book(
    val id: Int,
    val title: String,
    val author: String,
    val coverTop: Rgb,
    val coverBottom: Rgb,
    val symbol: String,
    val stars: List<Screen47Star>,
    val rating: String,
)

data class Screen47Shelf(val id: Int, val title: String, val count: String, val books: List<Screen47Book>)

data class Screen47State(
    val current: Screen47Book,
    val progress: Double,
    val progressText: String,
    val pagesText: String,
    val goal: Double,
    val goalText: String,
    val goalCaption: String,
    val streak: String,
    val shelves: List<Screen47Shelf>,
)

private fun screen47Stars(rating: Double): List<Screen47Star> = (1..5).map { i ->
    val icon = when {
        rating >= i -> "star.fill"
        rating >= i - 0.5 -> "star.leadinghalf.filled"
        else -> "star"
    }
    Screen47Star(i, icon)
}

private fun screen47Book(id: Int, title: String, author: String, top: Rgb, bottom: Rgb, symbol: String, rating: Double) =
    Screen47Book(id, title, author, top, bottom, symbol, screen47Stars(rating), rating.toString())

private const val SCREEN47_PAGES = 412

private fun screen47Progress(page: Int) = Triple(page.toDouble() / SCREEN47_PAGES, "${page * 100 / SCREEN47_PAGES}%", "Page $page of $SCREEN47_PAGES")

class Screen47ViewModel : BenchViewModel() {
    private var page = 238

    private val _state = MutableStateFlow(
        screen47Progress(page).let { (p, pt, pages) ->
            Screen47State(
                current = screen47Book(100, "The Quiet Orbit", "Lena Marsh", Rgb(0.12, 0.2, 0.45), Rgb(0.4, 0.65, 0.95), "moon.stars.fill", 4.5),
                progress = p,
                progressText = pt,
                pagesText = pages,
                goal = 18.0,
                goalText = "18",
                goalCaption = "18 of 24 books this year",
                streak = "12-day streak",
                shelves = listOf(
                    Screen47Shelf(
                        1, "Want to Read", "4 books", listOf(
                            screen47Book(1, "Salt & Ember", "R. Okafor", Rgb(0.95, 0.45, 0.25), Rgb(0.6, 0.1, 0.2), "flame.fill", 4.0),
                            screen47Book(2, "Glass Harbor", "M. Ishikawa", Rgb(0.3, 0.8, 0.8), Rgb(0.1, 0.35, 0.55), "water.waves", 3.5),
                            screen47Book(3, "Paper Kings", "A. Duval", Rgb(0.95, 0.8, 0.3), Rgb(0.75, 0.45, 0.1), "crown.fill", 5.0),
                            screen47Book(4, "Northbound", "J. Lindqvist", Rgb(0.6, 0.75, 0.9), Rgb(0.3, 0.35, 0.6), "snowflake", 4.0),
                        )
                    ),
                    Screen47Shelf(
                        2, "Finished", "3 books", listOf(
                            screen47Book(5, "Verdant", "P. Alves", Rgb(0.45, 0.85, 0.45), Rgb(0.1, 0.45, 0.25), "leaf.fill", 4.5),
                            screen47Book(6, "Static Hearts", "K. Byrne", Rgb(0.85, 0.45, 0.85), Rgb(0.4, 0.15, 0.55), "bolt.heart.fill", 3.0),
                            screen47Book(7, "The Last Archive", "S. Haddad", Rgb(0.55, 0.5, 0.45), Rgb(0.25, 0.2, 0.18), "books.vertical.fill", 4.5),
                        )
                    ),
                ),
            )
        }
    )
    val state: StateFlow<Screen47State> = _state

    override fun mutate() {
        page = if (page >= SCREEN47_PAGES) 1 else page + 7
        val (p, pt, pages) = screen47Progress(page)
        _state.update { it.copy(progress = p, progressText = pt, pagesText = pages) }
    }
}

@Composable
private fun Screen47Cover(book: Screen47Book, width: Double, height: Double) {
    ZStack(
        modifier = Modifier.frame(width = width, height = height)
            .clipShape(RoundedRectangle(cornerRadius = 8.0))
            .shadow(color = Color.black.opacity(0.2), radius = 4.0, x = 0.0, y = 2.0),
    ) {
        LinearGradientView(LinearGradient(colors = listOf(book.coverTop.color(), book.coverBottom.color()), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom))
        VStack(spacing = 6.0) {
            Image(systemName = book.symbol, modifier = Modifier.font(Font.title2).foregroundStyle(Color.white.opacity(0.9)))
            Text(
                book.title,
                modifier = Modifier.font(Font.system(size = 10.0, weight = Font.Weight.bold, design = Font.Design.serif))
                    .foregroundStyle(Color.white).multilineTextAlignment(TextAlignment.center).padding(Edge.Set.horizontal, 6.0),
            )
        }
    }
}

@Composable
private fun Screen47Stars(stars: List<Screen47Star>) {
    HStack(spacing = 1.0) {
        for (star in stars) key(star.id) {
            Image(systemName = star.icon, modifier = Modifier.font(Font.system(size = 9.0)).foregroundStyle(Color.orange))
        }
    }
}

@Composable
fun Screen47(vm: Screen47ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Library")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 22.0) {
                HStack(
                    modifier = Modifier.padding(14.0).background(Color.indigo.opacity(0.08), RoundedRectangle(cornerRadius = 18.0)),
                    alignment = VerticalAlignment.top,
                    spacing = 14.0,
                ) {
                    Screen47Cover(state.current, 78.0, 116.0)
                    VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) {
                        Text("CURRENTLY READING", modifier = Modifier.font(Font.caption2).fontWeight(Font.Weight.bold).foregroundStyle(Color.indigo))
                        Text(state.current.title, modifier = Modifier.font(Font.headline))
                        Text(state.current.author, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        Screen47Stars(state.current.stars)
                        Gauge(
                            value = state.progress,
                            bounds = 0.0..1.0,
                            modifier = Modifier.gaugeStyle(GaugeStyle.linearCapacity).tint(Color.indigo),
                            label = { Text(state.pagesText, modifier = Modifier.font(Font.caption).monospacedDigit()) },
                            currentValueLabel = { Text(state.progressText, modifier = Modifier.font(Font.caption).monospacedDigit()) },
                        )
                    }
                }
                HStack(spacing = 16.0) {
                    Gauge(
                        value = state.goal,
                        bounds = 0.0..24.0,
                        modifier = Modifier.gaugeStyle(GaugeStyle.accessoryCircularCapacity).tint(Color.orange),
                        content = { Text(state.goalText, modifier = Modifier.font(Font.headline)) },
                    )
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                        Text("2026 Reading Goal", modifier = Modifier.font(Font.headline))
                        Text(state.goalCaption, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        Label(title = state.streak, systemImage = "flame.fill", modifier = Modifier.font(Font.caption).foregroundStyle(Color.orange))
                    }
                    Spacer()
                }
                for (shelf in state.shelves) key(shelf.id) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 10.0) {
                        HStack(alignment = VerticalAlignment.firstTextBaseline) {
                            Text(shelf.title, modifier = Modifier.font(Font.title3).bold())
                            Spacer()
                            Text(shelf.count, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                        }
                        ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                            HStack(alignment = VerticalAlignment.top, spacing = 14.0) {
                                for (book in shelf.books) key(book.id) {
                                    VStack(modifier = Modifier.frame(width = 92.0), alignment = HorizontalAlignment.leading, spacing = 4.0) {
                                        Screen47Cover(book, 92.0, 136.0)
                                        Text(book.title, modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).lineLimit(1))
                                        Text(book.author, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
                                        HStack(spacing = 4.0) {
                                            Screen47Stars(book.stars)
                                            Text(book.rating, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
                                        }
                                    }
                                }
                            }
                        }
                        ShapeView(Rectangle(), modifier = Modifier.frame(height = 6.0).opacity(0.25), fill = Color.brown)
                    }
                }
            }
        }
    }
}

val screen47 = BenchScreen("47 · Book library", ::Screen47ViewModel) { Screen47(it as Screen47ViewModel) }
