package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.CompositionData
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.Foundation.NSProcessInfo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.reflect.KClass
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner as AndroidxLocalViewModelStoreOwner

/**
 * Hosts one composition rendering into a tree of [SNode]s, displayed by `ComposeView` in Swift.
 *
 * The host is the composition's [LifecycleOwner] (driven by the view's appearance and the scene phase), its
 * [ViewModelStoreOwner] (view models live as long as the host) and its [SavedStateRegistryOwner]: `rememberSaveable`
 * values and `SavedStateHandle`s survive the process through [saveState] / [restoreState] (`@SceneStorage` in Swift).
 */
public class ComposeSwiftUIHost(private val content: @Composable () -> Unit) :
    ViewModelStoreOwner, LifecycleOwner, SavedStateRegistryOwner, HasDefaultViewModelProviderFactory {
    public val root: SNode = SNode(SNodeKinds.ROOT)

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
            throw IllegalArgumentException("No initializer for ${modelClass.simpleName}: use viewModel { … }")
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@ComposeSwiftUIHost)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@ComposeSwiftUIHost)
            set(DEFAULT_ARGS_KEY, savedState())
        }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + errorHandler(ComposeSwiftUIErrorSource.Composition) { fail(it) },
    )

    /** Where the `suspend` callbacks Swift awaits run (`task { }`, `refreshable { }`): cancelled with the host. */
    private val asyncScope = CoroutineScope(
        SupervisorJob(scope.coroutineContext[kotlinx.coroutines.Job]) + Dispatchers.Main +
            errorHandler(ComposeSwiftUIErrorSource.AsyncTask),
    )

    private val clock = BroadcastFrameClock { scheduleFrame() }
    private val recomposer = Recomposer(scope.coroutineContext + clock)
    private var composition: Composition? = null
    private var frameScheduled = false
    private val input = mutableStateOf<Any?>(null)
    private val environment = mutableStateOf<List<Any?>?>(null)
    private var restored: Map<String, Any?>? = null
    private var saveableRegistry: SaveableStateRegistry? = null
    private var visible = false
    private var scenePhase = "active"

    /** Frames this host has run, for tests and benchmarks. */
    public var frameCount: Int = 0
        private set

    /**
     * Set when the composition threw (see [ComposeSwiftUIErrorSource.Composition]): the host stopped composing and
     * `ComposeView` shows nothing, or this message in debug builds.
     */
    public var failure: String? = null
        private set

    /**
     * Records the source of every call, for the IDE previews: [inspect] maps nodes to code, and literal patches
     * ([setLiteralPatches]) apply. Set before [start]; costs memory and time, so off in apps.
     */
    public var inspectable: Boolean = false
    private var compositionData: CompositionData? = null

    init {
        savedStateController.performAttach()
        enableSavedStateHandles()
    }

    /** For the IDE: the call sites of the nodes, as JSON (see [CompositionInspector.toJson]). Needs [inspectable]. */
    public fun inspect(): String = compositionData?.let { CompositionInspector.toJson(CompositionInspector.nodeSources(it)) } ?: "[]"

    /** Installs the requested literal patches on this host's nodes. */
    internal fun applyLiteralPatches() {
        val data = compositionData ?: return
        val sources = CompositionInspector.nodeSources(data)
        LiteralPatches.install(LiteralPatches.requested, sources)
        sources.keys.forEach { it.reapplyLiteralPatches() }
    }

    /** Pushes a new value from Swift (`ComposeView(input:)`); read it with [hostInput]. */
    public fun updateInput(value: Any?) {
        input.value = value
        flush()
    }

    /**
     * The SwiftUI environment around the host (see `currentSwiftUIEnvironment`). Swift sets it before [start], so the
     * first composition already sees the right color scheme and size classes, and again whenever it changes.
     */
    public fun updateEnvironment(values: List<Any?>) {
        if (values == environment.value) return
        environment.value = values
        if (composition != null) flush()
    }

    /**
     * Restores the state [saveState] returned in an earlier run (from `@SceneStorage`): `rememberSaveable` values and
     * the `SavedStateRegistry` (`SavedStateHandle`). Call before [start]; an unreadable state is ignored.
     */
    public fun restoreState(state: String?) {
        if (isStarted) {
            ComposeSwiftUI.logger.log(ComposeSwiftUILogLevel.Error, "Compose SwiftUI: restoreState after start() is ignored")
            return
        }
        restored = state?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                StateCodec.decode(it) as? Map<String, Any?>
            } catch (e: Throwable) {
                ComposeSwiftUI.logger.log(ComposeSwiftUILogLevel.Error, "Compose SwiftUI: ignoring an unreadable saved state: $e")
                null
            }
        }
    }

    /** Saves the `rememberSaveable` values and the `SavedStateRegistry`, for [restoreState] in a later run. */
    public fun saveState(): String {
        val registry = savedState().also { savedStateController.performSave(it) }
        val values = saveableRegistry?.performSave().orEmpty()
        return StateCodec.encode(mapOf("saveable" to values, "registry" to registry))
    }

    /** Starts composing. Idempotent. [environment]: the SwiftUI environment (see [updateEnvironment]). */
    public fun start(environment: List<Any?>?) {
        if (isStarted) {
            environment?.let(::updateEnvironment)
            return
        }
        environment?.let { this.environment.value = it }
        start()
    }

    /** True once [start] ran (even if the composition then failed). */
    public val isStarted: Boolean get() = composition != null || failure != null

    /** Starts composing. Idempotent. */
    public fun start() {
        if (composition != null || failure != null) return
        liveHosts += this
        GlobalSnapshotManager.ensureStarted()
        savedStateController.performRestore(restored?.get("registry") as? SavedState)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        @Suppress("UNCHECKED_CAST")
        val registry = SaveableStateRegistry(restored?.get("saveable") as? Map<String, List<Any?>>, StateCodec::canBeSaved)
        saveableRegistry = registry
        restored = null
        scope.launch(clock, start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition = Composition(SNodeApplier(root), recomposer).apply {
                setContent {
                    if (inspectable) {
                        currentComposer.collectParameterInformation()
                        compositionData = currentComposer.compositionData
                    }
                    CompositionLocalProvider(
                        @Suppress("DEPRECATION") LocalViewModelStoreOwner provides this@ComposeSwiftUIHost,
                        AndroidxLocalViewModelStoreOwner provides this@ComposeSwiftUIHost,
                        LocalLifecycleOwner provides this@ComposeSwiftUIHost,
                        LocalSavedStateRegistryOwner provides this@ComposeSwiftUIHost,
                        LocalSaveableStateRegistry provides registry,
                        LocalHostInput provides input.value,
                        LocalHostEnvironment provides this@ComposeSwiftUIHost.environment.value,
                        LocalAsyncScope provides asyncScope,
                    ) {
                        Decorated(ComposeSwiftUI.decorators, 0, content)
                    }
                }
            }
        } catch (e: Throwable) {
            ComposeSwiftUI.reportError(ComposeSwiftUIErrorSource.Composition, e)
            fail(e)
            return
        }
        if (inspectable && LiteralPatches.requested.isNotEmpty()) applyLiteralPatches()
    }

    /** `onAppear` / `onDisappear` of the `ComposeView`: the lifecycle is RESUMED while visible in an active scene. */
    public fun setVisible(visible: Boolean) {
        this.visible = visible
        updateLifecycle()
    }

    /** The `scenePhase` of the `ComposeView`: `active`, `inactive` or `background`. */
    public fun setScenePhase(phase: String) {
        scenePhase = phase
        updateLifecycle()
    }

    private fun updateLifecycle() {
        if (composition == null) return
        lifecycleRegistry.currentState = when {
            !visible || scenePhase == "background" -> Lifecycle.State.CREATED
            scenePhase == "inactive" -> Lifecycle.State.STARTED
            else -> Lifecycle.State.RESUMED
        }
        flush()
    }

    /** True when a recomposition is pending. */
    internal val hasPendingFrame: Boolean get() = clock.hasAwaiters

    /**
     * Runs this host's pending recompositions right away. [flushCompose] is the cheaper call after a Kotlin callback:
     * it only runs the hosts that have work.
     */
    public fun flush() {
        // A few passes: effects of one pass (e.g. LazyForEach updating its rows) may schedule more work.
        repeat(4) {
            Snapshot.sendApplyNotifications()
            if (!clock.hasAwaiters) return
            sendFrame()
        }
    }

    internal fun sendFrame() {
        frameCount++
        clock.sendFrame(now())
    }

    public fun dispose() {
        liveHosts -= this
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        try {
            composition?.dispose()
        } catch (e: Throwable) {
            ComposeSwiftUI.reportError(ComposeSwiftUIErrorSource.Composition, e)
        }
        composition = null
        recomposer.cancel()
        scope.cancel()
        viewModelStore.clear()
        // The root's Swift model holds the root, which holds the model: a cycle neither ARC nor the GC can break
        root.detachObservers()
    }

    /** The composition threw: stop it, keep the app running and show the failure (debug) or nothing (release). */
    private fun fail(error: Throwable) {
        if (failure != null) return
        failure = error.toString()
        liveHosts -= this
        try {
            composition?.dispose()
        } catch (_: Throwable) {
        }
        composition = null
        recomposer.cancel()
        root._children.forEach { it.detachObservers() }
        root._children.clear()
        root.childrenChanged()
        root.notifyChanged()
    }

    private fun scheduleFrame() {
        if (frameScheduled) return
        frameScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            frameScheduled = false
            if (composition != null) sendFrame()
        }
    }

    private fun now(): Long = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000).toLong()
}

@Composable
private fun Decorated(decorators: List<HostDecorator>, index: Int, content: @Composable () -> Unit) {
    if (index == decorators.size) content() else decorators[index].Decorate { Decorated(decorators, index + 1, content) }
}

private val liveHosts = LinkedHashSet<ComposeSwiftUIHost>()

/**
 * Synchronously applies pending state changes. Swift calls this right after invoking a Kotlin callback so the UI
 * update lands in the same SwiftUI transaction. Only the hosts the change affects (those with a pending frame) run.
 */
public fun flushCompose() {
    // A few passes: effects of one pass (e.g. LazyForEach updating its rows) may schedule more work.
    repeat(4) {
        Snapshot.sendApplyNotifications()
        val pending = liveHosts.filter { it.hasPendingFrame }
        if (pending.isEmpty()) return
        pending.forEach { it.sendFrame() }
    }
}

/**
 * For the IDE previews: shows [patches] (literal edits) in every inspectable host, live ones and those created later,
 * without recompiling. An empty list removes the patches (after a build that includes the edits).
 */
public fun setLiteralPatches(patches: List<LiteralPatch>) {
    LiteralPatches.requested = patches
    LiteralPatches.clear()
    liveHosts.filter { it.inspectable }.forEach { it.applyLiteralPatches() }
    flushCompose()
}

/** Creates a host for [content]. Exported entry point for Swift (`ComposeView(host:)`). */
public fun composeSwiftUI(content: @Composable () -> Unit): ComposeSwiftUIHost = ComposeSwiftUIHost(content)

/** Forwards global snapshot writes to apply notifications on the main queue (as Compose UI does). */
internal object GlobalSnapshotManager {
    private var handle: ObserverHandle? = null
    private var scheduled = false

    fun ensureStarted() {
        if (handle != null) return
        handle = Snapshot.registerGlobalWriteObserver {
            if (!scheduled) {
                scheduled = true
                dispatch_async(dispatch_get_main_queue()) {
                    scheduled = false
                    Snapshot.sendApplyNotifications()
                }
            }
        }
    }
}
