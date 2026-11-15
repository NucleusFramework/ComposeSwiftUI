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

// Screen 44 — Job listings: filter chips, job cards with logo tiles, tag capsules, salary and bookmark.

data class Screen44Chip(val id: Int, val title: String, val icon: String, val selected: Boolean)

data class Screen44Tag(val id: Int, val text: String)

data class Screen44Job(
    val id: Int,
    val company: String,
    val initials: String,
    val logo: Rgb,
    val logoIcon: String,
    val title: String,
    val location: String,
    val posted: String,
    val tags: List<Screen44Tag>,
    val salary: String,
    val applicants: String,
    val bookmarked: Boolean,
    val featured: Boolean,
)

data class Screen44State(val chips: List<Screen44Chip>, val resultCount: String, val jobs: List<Screen44Job>, val saved: String)

private fun screen44Tags(vararg t: String) = t.mapIndexed { i, s -> Screen44Tag(i, s) }

private fun screen44Saved(jobs: List<Screen44Job>) = "${jobs.count { it.bookmarked }} saved"

class Screen44ViewModel : BenchViewModel() {
    private val initialJobs = listOf(
        Screen44Job(1, "Northwind", "NW", Rgb(0.0, 0.48, 1.0), "wind", "Senior iOS Engineer", "Remote · Europe", "2d ago", screen44Tags("Swift", "SwiftUI", "Full-time"), "€85k – €110k", "42 applicants", true, true),
        Screen44Job(2, "Lumen Labs", "LL", Rgb(1.0, 0.58, 0.0), "lightbulb.fill", "Kotlin Multiplatform Lead", "Berlin · Hybrid", "5h ago", screen44Tags("Kotlin", "KMP", "Lead"), "€95k – €125k", "18 applicants", false, true),
        Screen44Job(3, "Harbor", "HB", Rgb(0.19, 0.69, 0.78), "ferry.fill", "Product Designer", "Lisbon · On-site", "1w ago", screen44Tags("Figma", "Design Systems"), "€60k – €75k", "96 applicants", false, false),
        Screen44Job(4, "Quartz AI", "QA", Rgb(0.69, 0.32, 0.87), "cpu.fill", "ML Platform Engineer", "Remote · Worldwide", "3d ago", screen44Tags("Python", "Rust", "Contract"), "$140k – $180k", "57 applicants", true, false),
        Screen44Job(5, "Fieldnote", "FN", Rgb(0.2, 0.78, 0.35), "leaf.fill", "Android Developer", "Amsterdam · Hybrid", "Today", screen44Tags("Compose", "Kotlin"), "€70k – €90k", "9 applicants", false, false),
    )

    private val _state = MutableStateFlow(
        Screen44State(
            chips = listOf(
                Screen44Chip(1, "All", "square.grid.2x2", true),
                Screen44Chip(2, "Remote", "house.fill", false),
                Screen44Chip(3, "Full-time", "clock.fill", false),
                Screen44Chip(4, "Senior", "star.fill", false),
            ),
            resultCount = "128 open roles",
            jobs = initialJobs,
            saved = screen44Saved(initialJobs),
        )
    )
    val state: StateFlow<Screen44State> = _state

    private var tick = 0

    fun toggleBookmark(id: Int) = _state.update { s ->
        val jobs = s.jobs.map { if (it.id == id) it.copy(bookmarked = !it.bookmarked) else it }
        s.copy(jobs = jobs, saved = screen44Saved(jobs))
    }

    override fun mutate() {
        tick += 1
        toggleBookmark(tick % 5 + 1)
        _state.update { s ->
            val sel = tick % s.chips.size
            s.copy(chips = s.chips.mapIndexed { i, c -> c.copy(selected = i == sel) }, resultCount = "${128 - sel * 23} open roles")
        }
    }
}

@Composable
fun Screen44(vm: Screen44ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.listStyle(ListStyle.plain).navigationTitle("Jobs")) {
            Section {
                ScrollView(axes = Axis.Set.horizontal, showsIndicators = false) {
                    HStack(spacing = 8.0) {
                        for (chip in state.chips) key(chip.id) {
                            Label(
                                title = chip.title,
                                systemImage = SFSymbol(chip.icon),
                                modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.medium)
                                    .foregroundStyle(if (chip.selected) Color.white else Color.primary)
                                    .padding(Edge.Set.horizontal, 12.0)
                                    .padding(Edge.Set.vertical, 7.0)
                                    .background(if (chip.selected) Color.blue else Color.gray.opacity(0.15), Capsule()),
                            )
                        }
                    }
                }
                HStack {
                    Text(state.resultCount, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                    Spacer()
                    Label(title = state.saved, systemImage = SFSymbol.bookmarkFill, modifier = Modifier.font(Font.footnote).foregroundStyle(Color.blue))
                }
            }
            for (job in state.jobs) key(job.id) {
                VStack(modifier = Modifier.padding(Edge.Set.vertical, 6.0), alignment = HorizontalAlignment.leading, spacing = 10.0) {
                    HStack(alignment = VerticalAlignment.top, spacing = 12.0) {
                        ZStack(modifier = Modifier.frame(width = 48.0, height = 48.0)) {
                            ShapeView(RoundedRectangle(cornerRadius = 12.0), fill = job.logo.color().opacity(0.18))
                            Image(systemName = SFSymbol(job.logoIcon), modifier = Modifier.font(Font.title3).foregroundStyle(job.logo.color()))
                        }
                        VStack(alignment = HorizontalAlignment.leading, spacing = 3.0) {
                            HStack(spacing = 6.0) {
                                Text(job.company, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                                if (job.featured) {
                                    Text(
                                        "FEATURED",
                                        modifier = Modifier.font(Font.system(size = 9.0, weight = Font.Weight.bold))
                                            .foregroundStyle(Color.orange)
                                            .padding(Edge.Set.horizontal, 5.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(Color.orange.opacity(0.15), Capsule()),
                                    )
                                }
                            }
                            Text(job.title, modifier = Modifier.font(Font.headline))
                            Label(title = job.location, systemImage = SFSymbol.mappin, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                        Spacer()
                        Image(systemName = 
                            if (job.bookmarked) SFSymbol.bookmarkFill else SFSymbol.bookmark,
                            modifier = Modifier.font(Font.title3).foregroundStyle(if (job.bookmarked) Color.blue else Color.gray),
                        )
                    }
                    HStack(spacing = 6.0) {
                        for (tag in job.tags) key(tag.id) {
                            Text(
                                tag.text,
                                modifier = Modifier.font(Font.caption).foregroundStyle(job.logo.color())
                                    .padding(Edge.Set.horizontal, 8.0)
                                    .padding(Edge.Set.vertical, 3.0)
                                    .background(job.logo.color().opacity(0.12), Capsule()),
                            )
                        }
                    }
                    HStack {
                        Text(job.salary, modifier = Modifier.font(Font.subheadline).fontWeight(Font.Weight.semibold).monospacedDigit())
                        Spacer()
                        Text(job.applicants, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        Text("·", modifier = Modifier.foregroundStyle(ShapeStyle.tertiary))
                        Text(job.posted, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                    }
                }
            }
        }
    }
}

val screen44 = BenchScreen("44 · Job listings", ::Screen44ViewModel) { Screen44(it as Screen44ViewModel) }
