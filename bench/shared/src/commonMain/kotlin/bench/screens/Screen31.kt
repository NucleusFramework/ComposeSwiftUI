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

// Screen 31 — Social profile: cover + avatar header, stats, bio, segmented Picker tabs, 3-column post grid.

data class Screen31Stat(val id: Int, val value: String, val label: String)

data class Screen31Post(val id: Int, val symbol: String, val top: Rgb, val bottom: Rgb, val likes: String, val pinned: Boolean)

data class Screen31State(
    val name: String,
    val handle: String,
    val initials: String,
    val bio: String,
    val location: String,
    val link: String,
    val following: Boolean,
    val stats: List<Screen31Stat>,
    val tab: Int,
    val posts: List<Screen31Post>,
)

private val screen31Symbols = listOf(
    "mountain.2.fill", "camera.fill", "leaf.fill", "sun.max.fill", "cup.and.saucer.fill", "bicycle",
    "airplane", "music.note", "pawprint.fill", "fork.knife", "book.fill", "moon.stars.fill",
)
private val screen31Palette = listOf(
    Rgb(1.0, 0.45, 0.2) to Rgb(0.9, 0.2, 0.4),
    Rgb(0.2, 0.6, 1.0) to Rgb(0.35, 0.34, 0.84),
    Rgb(0.2, 0.75, 0.55) to Rgb(0.1, 0.5, 0.6),
    Rgb(0.95, 0.75, 0.1) to Rgb(0.9, 0.5, 0.0),
    Rgb(0.69, 0.32, 0.87) to Rgb(0.4, 0.2, 0.7),
)

private fun screen31Posts(tab: Int, bump: Int): List<Screen31Post> {
    val count = listOf(12, 9, 6)[tab]
    return (0 until count).map { i ->
        val k = (i + tab * 5) % screen31Symbols.size
        val colors = screen31Palette[(i + tab) % screen31Palette.size]
        val likes = 120 + ((i * 37 + tab * 11) % 90) * 13 + if (i == 0) bump else 0
        Screen31Post(i, screen31Symbols[k], colors.first, colors.second, if (likes >= 1000) "${likes / 1000}.${likes % 1000 / 100}k" else "$likes", i == 0 && tab == 0)
    }
}

class Screen31ViewModel : BenchViewModel() {
    private var bump = 0
    private val _state = MutableStateFlow(
        Screen31State(
            name = "Camille Laurent",
            handle = "@camille.shoots",
            initials = "CL",
            bio = "Landscape photographer. Chasing light across the Alps, one sunrise at a time.",
            location = "Annecy, France",
            link = "camille.photo",
            following = false,
            stats = listOf(Screen31Stat(1, "248", "Posts"), Screen31Stat(2, "18.4k", "Followers"), Screen31Stat(3, "391", "Following")),
            tab = 0,
            posts = screen31Posts(0, 0),
        )
    )
    val state: StateFlow<Screen31State> = _state

    fun setTab(value: Int) = _state.update { it.copy(tab = value, posts = screen31Posts(value, bump)) }

    override fun mutate() {
        bump += 1
        _state.update {
            val tab = (it.tab + 1) % 3
            val posts = it.stats[0].value.toInt() + 1
            it.copy(
                tab = tab,
                posts = screen31Posts(tab, bump),
                following = !it.following,
                stats = listOf(it.stats[0].copy(value = "$posts")) + it.stats.drop(1),
            )
        }
    }
}

@Composable
private fun Screen31Header(state: Screen31State) {
    ZStack(modifier = Modifier.padding(Edge.Set.bottom, 40.0), alignment = Alignment.bottomLeading) {
        ShapeView(
            Rectangle(),
            modifier = Modifier.frame(height = 130.0),
            fill = LinearGradient(colors = listOf(Color.orange, Color.pink, Color.purple), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
        )
        ImageSystemName(
            "mountain.2.fill",
            modifier = Modifier.font(Font.system(size = 70.0)).foregroundStyle(Color.white.opacity(0.25)).offset(x = 250.0, y = -20.0),
        )
        Text(
            state.initials,
            modifier = Modifier.font(Font.system(size = 30.0, weight = Font.Weight.bold, design = Font.Design.rounded))
                .foregroundStyle(Color.white)
                .frame(width = 88.0, height = 88.0)
                .background(LinearGradient(colors = listOf(Color.indigo, Color.blue), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom), Circle())
                .overlay { ShapeView(Circle(), stroke = Color.white, lineWidth = 4.0) }
                .offset(x = 16.0, y = 44.0),
        )
    }
}

@Composable
fun Screen31(vm: Screen31ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle(state.handle)) {
            VStack(alignment = HorizontalAlignment.leading, spacing = 14.0) {
                Screen31Header(state)
                HStack(modifier = Modifier.padding(Edge.Set.horizontal, 16.0), alignment = VerticalAlignment.top) {
                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
                        HStack(spacing = 4.0) {
                            Text(state.name, modifier = Modifier.font(Font.title2.bold()))
                            ImageSystemName("checkmark.seal.fill", modifier = Modifier.foregroundStyle(Color.blue))
                        }
                        Text(verbatim = state.handle, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    }
                    Spacer()
                    if (state.following) {
                        Button(title = "Following", action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered).buttonBorderShape(ButtonBorderShape.capsule))
                    } else {
                        Button(title = "Follow", action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).buttonBorderShape(ButtonBorderShape.capsule))
                    }
                }
                VStack(modifier = Modifier.padding(Edge.Set.horizontal, 16.0), alignment = HorizontalAlignment.leading, spacing = 6.0) {
                    Text(state.bio, modifier = Modifier.font(Font.subheadline))
                    HStack(spacing = 12.0) {
                        Label(state.location, systemImage = "mappin.and.ellipse")
                        Label(state.link, systemImage = "link", modifier = Modifier.foregroundStyle(Color.blue))
                    }
                }
                HStack(modifier = Modifier.padding(Edge.Set.vertical, 10.0).background(Color.gray.opacity(0.08), RoundedRectangle(cornerRadius = 12.0)).padding(Edge.Set.horizontal, 16.0)) {
                    for (stat in state.stats) key(stat.id) {
                        VStack(modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY), spacing = 2.0) {
                            Text(stat.value, modifier = Modifier.font(Font.headline).monospacedDigit())
                            Text(stat.label, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                        }
                    }
                }
                Picker(
                    title = "Content",
                    selection = state.tab,
                    modifier = Modifier.pickerStyle(PickerStyle.segmented).padding(Edge.Set.horizontal, 16.0),
                    onSelectionChange = { vm.setTab(it as Int) },
                ) {
                    Label("Posts", systemImage = "square.grid.3x3", modifier = Modifier.tag(0))
                    Label("Reels", systemImage = "play.rectangle", modifier = Modifier.tag(1))
                    Label("Tagged", systemImage = "person.crop.square", modifier = Modifier.tag(2))
                }
                LazyVGrid(columns = listOf(GridItem(spacing = 2.0), GridItem(spacing = 2.0), GridItem(spacing = 2.0)), spacing = 2.0) {
                    for (post in state.posts) key(post.id) {
                        ShapeView(
                            Rectangle(),
                            modifier = Modifier.aspectRatio(1.0, contentMode = ContentMode.fit)
                                .overlay {
                                    ImageSystemName(post.symbol, modifier = Modifier.font(Font.title).foregroundStyle(Color.white.opacity(0.9)))
                                }
                                .overlay(alignment = Alignment.bottomLeading) {
                                    HStack(modifier = Modifier.font(Font.caption2.bold()).foregroundStyle(Color.white).padding(6.0), spacing = 3.0) {
                                        ImageSystemName("heart.fill")
                                        Text(verbatim = post.likes)
                                    }
                                }
                                .overlay(alignment = Alignment.topTrailing) {
                                    if (post.pinned) {
                                        ImageSystemName("pin.fill", modifier = Modifier.font(Font.caption).foregroundStyle(Color.white).rotationEffect(Angle(degrees = 45.0)).padding(6.0))
                                    }
                                },
                            fill = LinearGradient(colors = listOf(post.top.color(), post.bottom.color()), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing),
                        )
                    }
                }
            }
        }
    }
}

val screen31 = BenchScreen("31 · Social profile", ::Screen31ViewModel) { Screen31(it as Screen31ViewModel) }
