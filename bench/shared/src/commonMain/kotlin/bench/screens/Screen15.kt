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

// Screen 15 — File browser: locations, nested DisclosureGroup folders, file rows with icons, sizes and dates.

data class Screen15File(val id: Int, val name: String, val icon: String, val tint: Rgb, val size: String, val date: String)

data class Screen15SubFolder(val id: Int, val name: String, val count: String, val expanded: Boolean, val files: List<Screen15File>)

data class Screen15Folder(
    val id: Int,
    val name: String,
    val icon: String,
    val tint: Rgb,
    val count: String,
    val expanded: Boolean,
    val subFolders: List<Screen15SubFolder>,
    val files: List<Screen15File>,
)

data class Screen15Location(val id: Int, val name: String, val icon: String, val tint: Rgb, val usage: String, val fraction: Double)

data class Screen15State(
    val locations: List<Screen15Location>,
    val folders: List<Screen15Folder>,
    val footer: String,
    val tick: Int,
)

private val pdfTint = Rgb(1.0, 0.23, 0.19)
private val imgTint = Rgb(0.2, 0.78, 0.35)
private val docTint = Rgb(0.0, 0.48, 1.0)
private val zipTint = Rgb(0.56, 0.56, 0.58)
private val codeTint = Rgb(0.69, 0.32, 0.87)

private fun screen15Folders(tick: Int): List<Screen15Folder> = listOf(
    Screen15Folder(
        101, "Projects", "folder.fill", Rgb(0.0, 0.48, 1.0), "3 items", true,
        subFolders = listOf(
            Screen15SubFolder(
                11, "compose-swiftui", "3 items", true,
                listOf(
                    Screen15File(111, "Composition.kt", "doc.text.fill", codeTint, "${12 + tick} KB", "Today, 09:41"),
                    Screen15File(112, "README.md", "doc.plaintext.fill", docTint, "4 KB", "Yesterday"),
                    Screen15File(113, "build.gradle.kts", "hammer.fill", codeTint, "2 KB", "12 Nov 2026"),
                ),
            ),
            Screen15SubFolder(
                12, "Design", "2 items", false,
                listOf(
                    Screen15File(121, "Mockups.fig", "paintbrush.fill", imgTint, "18.2 MB", "3 Nov 2026"),
                    Screen15File(122, "Icons.zip", "doc.zipper", zipTint, "6.4 MB", "1 Nov 2026"),
                ),
            ),
        ),
        files = listOf(Screen15File(13, "Roadmap.pdf", "doc.richtext.fill", pdfTint, "1.1 MB", "8 Nov 2026")),
    ),
    Screen15Folder(
        102, "Photos", "photo.on.rectangle.angled", Rgb(1.0, 0.58, 0.0), "2 items", true,
        subFolders = emptyList(),
        files = listOf(
            Screen15File(21, "IMG_${4021 + tick}.heic", "photo.fill", imgTint, "3.8 MB", "Today, 08:12"),
            Screen15File(22, "Beach panorama.jpg", "photo.fill", imgTint, "11.6 MB", "30 Oct 2026"),
        ),
    ),
    Screen15Folder(
        103, "Archive", "archivebox.fill", Rgb(0.56, 0.56, 0.58), "1 item", false,
        subFolders = emptyList(),
        files = listOf(Screen15File(31, "Taxes 2025.zip", "doc.zipper", zipTint, "48 MB", "2 Apr 2026")),
    ),
)

class Screen15ViewModel : BenchViewModel() {
    private val _state = MutableStateFlow(
        Screen15State(
            locations = listOf(
                Screen15Location(1, "iCloud Drive", "icloud.fill", Rgb(0.0, 0.48, 1.0), "128 GB of 200 GB", 0.64),
                Screen15Location(2, "On My iPhone", "iphone", Rgb(0.2, 0.2, 0.22), "54 GB of 256 GB", 0.21),
            ),
            folders = screen15Folders(0),
            footer = "6 folders · 9 files",
            tick = 0,
        )
    )
    val state: StateFlow<Screen15State> = _state

    fun setFolderExpanded(id: Int, value: Boolean) = _state.update {
        it.copy(folders = it.folders.map { f -> if (f.id == id) f.copy(expanded = value) else f })
    }

    fun setSubFolderExpanded(id: Int, value: Boolean) = _state.update {
        it.copy(folders = it.folders.map { f -> f.copy(subFolders = f.subFolders.map { s -> if (s.id == id) s.copy(expanded = value) else s }) })
    }

    override fun mutate() = _state.update {
        val t = it.tick + 1
        val expandedFolders = it.folders.associate { f -> f.id to f.expanded }
        val expandedSubs = it.folders.flatMap { f -> f.subFolders }.associate { s -> s.id to s.expanded }
        it.copy(
            tick = t,
            folders = screen15Folders(t).map { f ->
                f.copy(expanded = expandedFolders.getValue(f.id), subFolders = f.subFolders.map { s -> s.copy(expanded = expandedSubs.getValue(s.id)) })
            },
            locations = it.locations.map { l -> if (l.id == 1) l.copy(usage = "${128 + t} GB of 200 GB", fraction = (128 + t) / 200.0) else l },
        )
    }
}

@Composable
private fun Screen15FileRow(file: Screen15File) {
    HStack(spacing = 10.0) {
        Image(systemName = file.icon, modifier = Modifier.font(Font.title3).foregroundStyle(file.tint.color()).frame(width = 28.0))
        VStack(alignment = HorizontalAlignment.leading, spacing = 1.0) {
            Text(file.name, modifier = Modifier.font(Font.subheadline).lineLimit(1).truncationMode(Text.TruncationMode.middle))
            Text(file.date, modifier = Modifier.font(Font.caption2).foregroundStyle(ShapeStyle.secondary))
        }
        Spacer()
        Text(file.size, modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
    }
}

@Composable
fun Screen15(vm: Screen15ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        List(modifier = Modifier.navigationTitle("Browse")) {
            Section(title = "Locations") {
                for (location in state.locations) key(location.id) {
                    HStack(spacing = 12.0) {
                        Image(systemName = 
                            location.icon,
                            modifier = Modifier.font(Font.system(size = 16.0, weight = Font.Weight.semibold))
                                .foregroundStyle(location.tint.color())
                                .frame(width = 32.0, height = 32.0)
                                .background(location.tint.color().opacity(0.12), RoundedRectangle(cornerRadius = 8.0)),
                        )
                        VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
                            HStack {
                                Text(location.name, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)))
                                Spacer()
                                Text(location.usage, modifier = Modifier.font(Font.caption).monospacedDigit().foregroundStyle(ShapeStyle.secondary))
                            }
                            ProgressView(value = location.fraction, modifier = Modifier.tint(location.tint.color()))
                        }
                    }
                }
            }
            Section(header = { Text("Folders") }, footer = { Text(state.footer) }, content = {
                for (folder in state.folders) key(folder.id) {
                    DisclosureGroup(
                        isExpanded = folder.expanded,
                        onIsExpandedChange = { vm.setFolderExpanded(folder.id, it) },
                        content = {
                            for (sub in folder.subFolders) key(sub.id) {
                                DisclosureGroup(
                                    isExpanded = sub.expanded,
                                    onIsExpandedChange = { vm.setSubFolderExpanded(sub.id, it) },
                                    content = {
                                        for (file in sub.files) key(file.id) {
                                            Screen15FileRow(file)
                                        }
                                    },
                                    label = {
                                        HStack {
                                            Label(sub.name, systemImage = "folder", modifier = Modifier.font(Font.subheadline))
                                            Spacer()
                                            Text(sub.count, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.tertiary))
                                        }
                                    },
                                )
                            }
                            for (file in folder.files) key(file.id) {
                                Screen15FileRow(file)
                            }
                        },
                        label = {
                            HStack {
                                Label(
                                    modifier = Modifier.font(Font.headline),
                                    title = { Text(folder.name) },
                                    icon = { Image(systemName = folder.icon, modifier = Modifier.foregroundStyle(folder.tint.color())) },
                                )
                                Spacer()
                                Text(
                                    folder.count,
                                    modifier = Modifier.font(Font.caption.weight(Font.Weight.medium))
                                        .foregroundStyle(ShapeStyle.secondary)
                                        .padding(Edge.Set.horizontal, 8.0)
                                        .padding(Edge.Set.vertical, 2.0)
                                        .background(Color.gray.opacity(0.15), Capsule()),
                                )
                            }
                        },
                    )
                }
            })
        }
    }
}

val screen15 = BenchScreen("15 · File browser", ::Screen15ViewModel) { Screen15(it as Screen15ViewModel) }
