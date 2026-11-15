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

// Screen 30 — Pull request review: PR header, file headers, monospaced diff lines with colored backgrounds, review summary.

data class Screen30Line(val id: Int, val oldNo: String, val newNo: String, val marker: String, val code: String, val fg: Rgb, val bg: Rgb)

data class Screen30File(val id: Int, val path: String, val stats: String, val icon: String, val lines: List<Screen30Line>)

data class Screen30Reviewer(val id: Int, val initials: String, val tint: Rgb)

data class Screen30State(
    val number: String,
    val title: String,
    val branchInfo: String,
    val additions: String,
    val deletions: String,
    val filesChanged: String,
    val files: List<Screen30File>,
    val reviewers: List<Screen30Reviewer>,
    val approvals: String,
    val changesRequested: String,
    val checks: String,
    val mergeable: Boolean,
)

private fun screen30Lines(raw: List<String>): List<Screen30Line> {
    var old = 40
    var new = 40
    return raw.mapIndexed { i, l ->
        val marker = l.take(1)
        val code = l.drop(1)
        when (marker) {
            "+" -> Screen30Line(i, "", "${new++}", "+", code, Rgb(0.07, 0.4, 0.15), Rgb(0.9, 0.97, 0.91))
            "-" -> Screen30Line(i, "${old++}", "", "−", code, Rgb(0.6, 0.1, 0.1), Rgb(1.0, 0.92, 0.92))
            "@" -> Screen30Line(i, "", "", "", "@@ -40,7 +40,9 @@ $code", Rgb(0.35, 0.4, 0.75), Rgb(0.93, 0.95, 1.0))
            else -> Screen30Line(i, "${old++}", "${new++}", "", code, Rgb(0.25, 0.25, 0.28), Rgb(1.0, 1.0, 1.0))
        }
    }
}

private fun screen30State(approvals: Int, extraLine: Boolean): Screen30State {
    val first = listOf(
        "@class SessionStore",
        " private val cache = mutableMapOf<String, Session>()",
        "-fun load(id: String): Session? = cache[id]",
        "+fun load(id: String): Session? {",
        "+    val hit = cache[id] ?: return disk.read(id)",
        "+    return hit.takeIf { !it.isExpired }",
        "+}",
        " fun clear() = cache.clear()",
    ) + if (extraLine) listOf("+// TODO: evict on memory warning") else emptyList()
    val second = listOf(
        "@func testLoad",
        " let store = SessionStore()",
        "-XCTAssertNil(store.load(\"a\"))",
        "+#expect(store.load(\"a\") == nil)",
        "+#expect(store.count == 0)",
    )
    val adds = first.count { it.startsWith("+") } + second.count { it.startsWith("+") } + 118
    return Screen30State(
        number = "#1284",
        title = "Cache sessions on disk and expire stale entries",
        branchInfo = "ada wants to merge 4 commits into main from feature/session-cache",
        additions = "+$adds",
        deletions = "−37",
        filesChanged = "6 files",
        files = listOf(
            Screen30File(1, "Sources/Store/SessionStore.kt", "+${first.count { it.startsWith("+") }} −1", "doc.text", screen30Lines(first)),
            Screen30File(2, "Tests/SessionStoreTests.swift", "+2 −1", "checkmark.diamond", screen30Lines(second)),
        ),
        reviewers = listOf(
            Screen30Reviewer(1, "GH", Rgb(0.0, 0.48, 1.0)),
            Screen30Reviewer(2, "LT", Rgb(1.0, 0.58, 0.0)),
            Screen30Reviewer(3, "MK", Rgb(0.69, 0.32, 0.87)),
        ),
        approvals = "$approvals approvals",
        changesRequested = if (approvals >= 2) "No changes requested" else "1 change requested",
        checks = "12 of 12 checks passed",
        mergeable = approvals >= 2,
    )
}

class Screen30ViewModel : BenchViewModel() {
    private var approvals = 2
    private var extra = false
    private val _state = MutableStateFlow(screen30State(approvals, extra))
    val state: StateFlow<Screen30State> = _state

    override fun mutate() {
        approvals = (approvals + 1) % 4
        extra = !extra
        _state.update { screen30State(approvals, extra) }
    }
}

@Composable
private fun Screen30Diff(file: Screen30File) {
    VStack(
        modifier = Modifier.clipShape(RoundedRectangle(cornerRadius = 10.0))
            .overlay { ShapeView(RoundedRectangle(cornerRadius = 10.0), stroke = Color.gray.opacity(0.3)) },
        alignment = HorizontalAlignment.leading,
        spacing = 0.0,
    ) {
        HStack(modifier = Modifier.padding(10.0).background(Color.gray.opacity(0.12)), spacing = 6.0) {
            Image(systemName = SFSymbol(file.icon), modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
            Text(verbatim = file.path, modifier = Modifier.font(Font.system(style = Font.TextStyle.footnote, design = Font.Design.monospaced)).bold().lineLimit(1).truncationMode(Text.TruncationMode.middle))
            Spacer()
            Text(verbatim = file.stats, modifier = Modifier.font(Font.caption.monospaced()).foregroundStyle(ShapeStyle.secondary))
        }
        for (line in file.lines) key(line.id) {
            HStack(
                modifier = Modifier.font(Font.system(size = 11.0, weight = Font.Weight.regular, design = Font.Design.monospaced))
                    .foregroundStyle(line.fg.color())
                    .padding(Edge.Set.vertical, 3.0)
                    .background(line.bg.color()),
                spacing = 0.0,
            ) {
                Text(verbatim = line.oldNo, modifier = Modifier.foregroundStyle(ShapeStyle.tertiary).frame(width = 26.0, alignment = Alignment.trailing))
                Text(verbatim = line.newNo, modifier = Modifier.foregroundStyle(ShapeStyle.tertiary).frame(width = 26.0, alignment = Alignment.trailing))
                Text(verbatim = line.marker, modifier = Modifier.bold().frame(width = 18.0))
                Text(verbatim = line.code, modifier = Modifier.lineLimit(1))
                Spacer(minLength = 0.0)
            }
        }
    }
}

@Composable
fun Screen30(vm: Screen30ViewModel) {
    val state by vm.state.collectAsState()
    NavigationStack {
        ScrollView(modifier = Modifier.navigationTitle("Review")) {
            VStack(modifier = Modifier.padding(), alignment = HorizontalAlignment.leading, spacing = 14.0) {
                HStack(spacing = 8.0) {
                    Label(
                        "Open",
                        systemImage = SFSymbol.arrowTrianglePull,
                        modifier = Modifier.font(Font.caption.bold())
                            .foregroundStyle(Color.white)
                            .padding(Edge.Set.horizontal, 10.0)
                            .padding(Edge.Set.vertical, 5.0)
                            .background(Color.green, Capsule()),
                    )
                    Text(verbatim = state.number, modifier = Modifier.font(Font.subheadline.monospacedDigit()).foregroundStyle(ShapeStyle.secondary))
                    Spacer()
                    Text(verbatim = state.additions, modifier = Modifier.font(Font.caption.monospaced()).foregroundStyle(Color.green))
                    Text(verbatim = state.deletions, modifier = Modifier.font(Font.caption.monospaced()).foregroundStyle(Color.red))
                    Text(verbatim = state.filesChanged, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
                }
                Text(state.title, modifier = Modifier.font(Font.title3.bold()))
                Text(verbatim = state.branchInfo, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary))
                for (file in state.files) key(file.id) {
                    Screen30Diff(file)
                }
                VStack(
                    modifier = Modifier.padding(14.0).background(Color.gray.opacity(0.08), RoundedRectangle(cornerRadius = 14.0)),
                    alignment = HorizontalAlignment.leading,
                    spacing = 10.0,
                ) {
                    HStack {
                        Label(state.approvals, systemImage = SFSymbol.checkmarkSealFill, modifier = Modifier.font(Font.subheadline.weight(Font.Weight.semibold)).foregroundStyle(Color.green))
                        Spacer()
                        HStack(spacing = -8.0) {
                            for (r in state.reviewers) key(r.id) {
                                Text(
                                    r.initials,
                                    modifier = Modifier.font(Font.caption2.bold())
                                        .foregroundStyle(Color.white)
                                        .frame(width = 28.0, height = 28.0)
                                        .background(r.tint.color(), Circle())
                                        .overlay { ShapeView(Circle(), stroke = Color.white, lineWidth = 2.0) },
                                )
                            }
                        }
                    }
                    Label(state.changesRequested, systemImage = SFSymbol.exclamationmarkBubbleFill, modifier = Modifier.font(Font.subheadline).foregroundStyle(if (state.mergeable) Color.secondary else Color.orange))
                    Label(state.checks, systemImage = SFSymbol.checkmarkCircleFill, modifier = Modifier.font(Font.subheadline).foregroundStyle(ShapeStyle.secondary))
                    Button(
                        action = {},
                        modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).tint(Color.green).controlSize(ControlSize.large).disabled(!state.mergeable),
                    ) {
                        Label("Merge pull request", systemImage = SFSymbol.arrowTriangleMerge, modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY))
                    }
                }
            }
        }
    }
}

val screen30 = BenchScreen("30 · Pull request review", ::Screen30ViewModel) { Screen30(it as Screen30ViewModel) }
