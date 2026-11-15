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

// Screen 25 — Onboarding: hero SF Symbol (hierarchical), title/body, feature highlights, page dots, primary/secondary buttons.

data class Screen25Highlight(val id: Int, val icon: String, val text: String)

data class Screen25Dot(val id: Int, val width: Double, val tint: Rgb)

data class Screen25Page(
    val symbol: String,
    val tint: Rgb,
    val accent: Rgb,
    val title: String,
    val text: String,
    val highlights: List<Screen25Highlight>,
)

data class Screen25State(val page: Screen25Page, val dots: List<Screen25Dot>, val step: String, val primary: String)

private val screen25Pages = listOf(
    Screen25Page(
        "sparkles.rectangle.stack.fill", Rgb(0.35, 0.34, 0.84), Rgb(0.69, 0.32, 0.87), "Organize everything",
        "Collect notes, links and photos in smart stacks that sort themselves while you work.",
        listOf(Screen25Highlight(1, "square.stack.3d.up.fill", "Smart stacks"), Screen25Highlight(2, "tag.fill", "Auto tags"), Screen25Highlight(3, "magnifyingglass", "Instant search")),
    ),
    Screen25Page(
        "person.2.wave.2.fill", Rgb(0.0, 0.48, 1.0), Rgb(0.19, 0.69, 0.78), "Share with your team",
        "Invite collaborators, leave comments and see changes from everyone in real time.",
        listOf(Screen25Highlight(1, "bubble.left.and.bubble.right.fill", "Comments"), Screen25Highlight(2, "person.crop.circle.badge.plus", "Invites"), Screen25Highlight(3, "clock.arrow.circlepath", "History")),
    ),
    Screen25Page(
        "lock.shield.fill", Rgb(0.2, 0.7, 0.35), Rgb(0.0, 0.6, 0.55), "Private by design",
        "Your data is end-to-end encrypted and never leaves your devices without your consent.",
        listOf(Screen25Highlight(1, "key.fill", "Encryption"), Screen25Highlight(2, "faceid", "Face ID"), Screen25Highlight(3, "icloud.fill", "Private sync")),
    ),
    Screen25Page(
        "bell.badge.fill", Rgb(1.0, 0.58, 0.0), Rgb(1.0, 0.23, 0.19), "Stay in the loop",
        "Get gentle reminders for what matters, and silence everything else with Focus filters.",
        listOf(Screen25Highlight(1, "moon.fill", "Focus"), Screen25Highlight(2, "calendar", "Schedules"), Screen25Highlight(3, "app.badge.fill", "Badges")),
    ),
)

private fun screen25State(index: Int): Screen25State {
    val page = screen25Pages[index]
    return Screen25State(
        page = page,
        dots = screen25Pages.indices.map { i -> Screen25Dot(i, if (i == index) 24.0 else 8.0, if (i == index) page.tint else Rgb(0.82, 0.82, 0.85)) },
        step = "Step ${index + 1} of ${screen25Pages.size}",
        primary = if (index == screen25Pages.size - 1) "Get Started" else "Continue",
    )
}

class Screen25ViewModel : BenchViewModel() {
    private var index = 0
    private val _state = MutableStateFlow(screen25State(0))
    val state: StateFlow<Screen25State> = _state

    override fun mutate() {
        index = (index + 1) % screen25Pages.size
        _state.update { screen25State(index) }
    }
}

@Composable
fun Screen25(vm: Screen25ViewModel) {
    val state by vm.state.collectAsState()
    val page = state.page
    NavigationStack {
        VStack(
            modifier = Modifier.padding(Edge.Set.bottom, 24.0)
                .navigationTitle("Welcome")
                .navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode.inline),
            spacing = 22.0,
        ) {
            Text(state.step, modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.semibold).textCase(Text.Case.uppercase).foregroundStyle(ShapeStyle.secondary))
            ZStack {
                ShapeView(Circle(), modifier = Modifier.frame(width = 250.0, height = 250.0), stroke = page.tint.color().opacity(0.15), lineWidth = 10.0)
                ShapeView(
                    Circle(),
                    modifier = Modifier.frame(width = 200.0, height = 200.0),
                    fill = LinearGradient(
                        colors = listOf(page.tint.color().opacity(0.25), page.accent.color().opacity(0.1)),
                        startPoint = UnitPoint.topLeading,
                        endPoint = UnitPoint.bottomTrailing,
                    ),
                )
                Image(systemName = 
                    page.symbol,
                    modifier = Modifier.font(Font.system(size = 92.0, weight = Font.Weight.medium))
                        .symbolRenderingMode(SymbolRenderingMode.hierarchical)
                        .foregroundStyle(page.tint.color()),
                )
                Image(systemName = 
                    "sparkle",
                    modifier = Modifier.font(Font.system(size = 22.0)).foregroundStyle(page.accent.color()).offset(x = 96.0, y = -84.0),
                )
            }
            VStack(modifier = Modifier.padding(Edge.Set.horizontal, 32.0), spacing = 12.0) {
                Text(page.title, modifier = Modifier.font(Font.largeTitle.bold()).multilineTextAlignment(TextAlignment.center))
                Text(page.text, modifier = Modifier.font(Font.body).foregroundStyle(ShapeStyle.secondary).multilineTextAlignment(TextAlignment.center))
            }
            HStack(spacing = 12.0) {
                for (h in page.highlights) key(h.id) {
                    VStack(
                        modifier = Modifier.frame(width = 96.0, height = 72.0).background(Color.gray.opacity(0.1), RoundedRectangle(cornerRadius = 14.0)),
                        spacing = 6.0,
                    ) {
                        Image(systemName = 
                            h.icon,
                            modifier = Modifier.font(Font.title3)
                                .symbolRenderingMode(SymbolRenderingMode.palette)
                                .foregroundStyle(page.tint.color(), page.accent.color()),
                        )
                        Text(h.text, modifier = Modifier.font(Font.caption).fontWeight(Font.Weight.medium))
                    }
                }
            }
            HStack(spacing = 8.0) {
                for (dot in state.dots) key(dot.id) {
                    ShapeView(Capsule(), modifier = Modifier.frame(width = dot.width, height = 8.0), fill = dot.tint.color())
                }
            }
            Spacer()
            VStack(modifier = Modifier.padding(Edge.Set.horizontal, 24.0), spacing = 12.0) {
                Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.plain)) {
                    Text(
                        state.primary,
                        modifier = Modifier.font(Font.headline)
                            .foregroundStyle(Color.white)
                            .frame(maxWidth = Double.POSITIVE_INFINITY)
                            .padding(Edge.Set.vertical, 14.0)
                            .background(page.tint.color(), RoundedRectangle(cornerRadius = 14.0)),
                    )
                }
                Button(title = "Skip for now", action = {}, modifier = Modifier.font(Font.subheadline).tint(page.tint.color()))
            }
        }
    }
}

val screen25 = BenchScreen("25 · Onboarding", ::Screen25ViewModel) { Screen25(it as Screen25ViewModel) }
