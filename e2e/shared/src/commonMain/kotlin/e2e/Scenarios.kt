@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, dev.nucleusframework.composeswiftui.InternalComposeSwiftUIApi::class)

package e2e

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import compose_swiftui_e2e.shared.generated.resources.Res
import compose_swiftui_e2e.shared.generated.resources.greeting
import dev.nucleusframework.composeswiftui.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Shared state of the end-to-end run, read by the Swift driver (e2e/app). */
object E2E {
    /** What the scenarios saw, in order: `"error:Callback:boom"`, `"lifecycle:ON_RESUME"`, `"env:dark"`… */
    val events = ArrayList<String>()

    fun log(event: String) {
        events += event
    }

    fun eventsJoined(): String = events.joinToString("\n")

    fun reset() = events.clear()

    /** Installs the error hook, as an app would to plug its crash reporter. */
    fun installErrorHook() {
        ComposeSwiftUI.onError = { source, error -> log("error:${source.name}:${error.message}") }
    }

    private val hosts = ArrayList<kotlin.native.ref.WeakReference<ComposeSwiftUIHost>>()

    /** A host for scenario [name]. */
    fun host(name: String): ComposeSwiftUIHost = composeSwiftUI { Scenario(name) }.also { hosts += kotlin.native.ref.WeakReference(it) }

    private fun liveRoots(): List<SNode> = hosts.mapNotNull { it.value }.filter { it.isStarted && it.failure == null }.map { it.root }

    private fun SNode.walk(): Sequence<SNode> = sequence {
        yield(this@walk)
        val slots = generateSequence(0) { it + 1 }.take(16).mapNotNull { slot(it) }
        (children.asSequence() + slots.flatMap { it.children.asSequence() }).forEach { yieldAll(it.walk()) }
    }

    /** Without accessibility permission (CI): the strings of the live hosts' nodes, instead of the accessibility tree. */
    fun nodeTexts(): List<String> = liveRoots().flatMap { root -> root.walk().flatMap { n -> n.args.filterIsInstance<String>() }.toList() }

    /** Without accessibility permission (CI): runs the action (or turns on the binding) of the node showing [text]. */
    fun pressNode(text: String): Boolean {
        val node = liveRoots().flatMap { it.walk().toList() }.firstOrNull { n -> text in n.args && n.args.any { it is SAction || it is SValueCallback } }
            ?: return false
        (node.args.firstOrNull { it is SAction } as SAction?)?.invoke() ?: (node.args.first { it is SValueCallback } as SValueCallback).invoke(true)
        flushCompose()
        return true
    }

    /** Starts the first `task { }` action of [host] as Swift would, but where SwiftUI can't cancel it. */
    fun startTaskOutsideSwiftUI(host: ComposeSwiftUIHost) {
        fun find(node: SNode): SAsyncAction? =
            (0 until node.modifierCount()).firstNotNullOfOrNull { i -> node.modifierAt(i).args.firstNotNullOfOrNull { it as? SAsyncAction } }
                ?: node.children.firstNotNullOfOrNull(::find)
        find(host.root)!!.start(object : SwiftAction {
            override fun run() = log("task:done")
        })
    }

    private val trackedHosts = ArrayList<kotlin.native.ref.WeakReference<ComposeSwiftUIHost>>()

    /** Watches [host] for [trackedHostsAlive]. */
    fun trackHost(host: ComposeSwiftUIHost) {
        trackedHosts += kotlin.native.ref.WeakReference(host)
    }

    fun trackedHostsAlive(): Int = trackedHosts.count { it.value != null }

    @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
    fun collectGarbage() = kotlin.native.runtime.GC.collect()

    /** The view kind of `Chart`, rendered by the Charts integration. */
    fun chartKind(): Int = composeSwiftUI { Chart { } }.run {
        start()
        root.children.single().kind.also { dispose() }
    }

    /** An expression carrying 64-bit values, as the bindings encode a Swift `Int64` / `UInt64` argument. */
    fun longArgs(): SExpr = SExpr(0, listOf(9_007_199_254_740_993L, Long.MIN_VALUE, -1L))

    /** Lets scenarios that collect a flow observe the lifecycle (`collectAsStateWithLifecycle`). */
    val ticker = MutableStateFlow(0)

    fun setTicker(value: Int) {
        ticker.value = value
    }
}

@Composable
private fun Scenario(name: String) {
    when (name) {
        "callbacks" -> CallbackErrors()
        "composition" -> CompositionError()
        "effect" -> EffectError()
        "task" -> TaskError()
        "taskCancel" -> TaskCancellation()
        "unsupported" -> SwiftView(kind = 999_999, args = emptyList())
        "bridges" -> Bridges()
        "chart" -> Chart {
            BarMark(x = PlottableValue.value("Day", "Mon"), y = PlottableValue.value("Count", 3))
            BarMark(x = PlottableValue.value("Day", "Tue"), y = PlottableValue.value("Count", 5))
        }
        "lifecycle" -> LifecycleScreen()
        "viewModels" -> ViewModels()
        "saveable" -> Saveable()
        "environment" -> Environment()
        "resources" -> Text(stringResource(Res.string.greeting))
        else -> Text("Unknown scenario $name")
    }
}

@Composable
private fun CallbackErrors() {
    var count by remember { mutableIntStateOf(0) }
    var on by remember { mutableStateOf(false) }
    VStack {
        Button("Throw in action", action = { error("action boom") })
        Toggle("Throw in binding", isOn = on, onIsOnChange = { error("binding boom") })
        Button("Increment", action = { count++ })
        Text("count=$count")
    }
}

/** Kotlin code Swift calls during layout and drawing: an exception there must not terminate the app. */
@Composable
private fun Bridges() {
    VStack {
        Canvas(modifier = Modifier.frame(width = 50.0, height = 50.0)) { error("canvas boom") }
        CustomLayout(object : LayoutPolicy {
            override fun sizeThatFits(proposedWidth: Double?, proposedHeight: Double?, subviews: List<LayoutSize>): LayoutSize =
                error("layout boom")

            override fun placeSubviews(width: Double, height: Double, subviews: List<LayoutSize>): List<LayoutPlacement> =
                error("layout boom")
        }) { Text("in layout") }
        Text("bridges alive")
    }
}

@Composable
private fun CompositionError() {
    var broken by remember { mutableStateOf(false) }
    VStack {
        Button("Break composition", action = { broken = true })
        if (broken) error("composition boom")
        Text("composition ok")
    }
}

@Composable
private fun EffectError() {
    LaunchedEffect(Unit) {
        delay(50)
        error("effect boom")
    }
    Text("effect pending")
}

@Composable
private fun TaskError() {
    var count by remember { mutableIntStateOf(0) }
    VStack(modifier = Modifier.task { error("task boom") }) {
        Button("Increment", action = { count++ })
        Text("count=$count")
    }
}

@Composable
private fun TaskCancellation() {
    Text("task running", modifier = Modifier.task {
        E2E.log("task:started")
        try {
            awaitCancellation()
        } finally {
            E2E.log("task:cancelled")
        }
    })
}

@Composable
private fun LifecycleScreen() {
    val owner = LocalLifecycleOwner.current
    val ticks by E2E.ticker.collectAsStateWithLifecycle()
    for (event in listOf(Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP)) {
        LifecycleEventEffect(event) { E2E.log("lifecycle:${event.name}") }
    }
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) E2E.log("lifecycle:ON_DESTROY")
        }
        owner.lifecycle.addObserver(observer)
        onDispose { }
    }
    val state by owner.lifecycle.currentStateFlow.collectAsState()
    Text("ticks=$ticks state=$state")
}

class HandleViewModel(private val handle: SavedStateHandle) : ViewModel() {
    var note: String
        get() = handle["note"] ?: ""
        set(value) {
            handle["note"] = value
        }
}

class GreetingViewModel : ViewModel() {
    val greeting = "koin works"
}

private val koinModule = module { viewModelOf(::GreetingViewModel) }

@Composable
private fun ViewModels() {
    // The official viewModel { }, with a SavedStateHandle from the host's CreationExtras
    val handleVm = viewModel { HandleViewModel(createSavedStateHandle()) }
    KoinApplication(application = { modules(koinModule) }) {
        val koinVm = koinViewModel<GreetingViewModel>()
        E2E.log("koin:${koinVm.greeting}")
    }
    var note by remember { mutableStateOf(handleVm.note) }
    E2E.log("handle:note=$note")
    VStack {
        Text("note=$note")
        Button("Set note", action = {
            handleVm.note = "saved in handle"
            note = handleVm.note
        })
    }
}

@Composable
private fun Saveable() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    var big by rememberSaveable { mutableStateOf(0L) }
    E2E.log("saveable:count=$count big=$big")
    VStack {
        Text("saved=$count")
        Button("Increment saved", action = {
            count++
            big = 9_007_199_254_740_993L
        })
    }
}

@Composable
private fun Environment() {
    // What the very first composition sees: must already be the window's appearance
    val initial = currentSwiftUIEnvironment
    val first = remember { initial.colorScheme }
    E2E.log("env:first=${first.name()}")
    EnvironmentReader { env ->
        val readerFirst = remember { env.colorScheme }
        E2E.log("env:reader=${readerFirst.name()}")
        Text("scheme=${env.colorScheme.name()}")
    }
}

private fun ColorScheme.name() = if (this == ColorScheme.dark) "dark" else "light"
