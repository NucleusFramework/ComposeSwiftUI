package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.Foundation.NSProcessInfo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Hosts one composition rendering into a tree of [SNode]s, displayed by `ComposeView` in Swift.
 * Owns a [ViewModelStore]: view models created with [viewModel] live as long as the host.
 */
class ComposeSwiftUIHost(private val content: @Composable () -> Unit) : ViewModelStoreOwner {
    val root = SNode(SNodeKinds.ROOT)

    override val viewModelStore = ViewModelStore()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val clock = BroadcastFrameClock { scheduleFrame() }
    private val recomposer = Recomposer(scope.coroutineContext + clock)
    private var composition: Composition? = null
    private var frameScheduled = false
    private val input = mutableStateOf<Any?>(null)

    /** Pushes a new value from Swift (`ComposeView(input:)`); read it with [hostInput]. */
    fun updateInput(value: Any?) {
        input.value = value
        flush()
    }

    /** Starts composing. Idempotent. */
    fun start() {
        if (composition != null) return
        liveHosts += this
        GlobalSnapshotManager.ensureStarted()
        scope.launch(clock, start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        composition = Composition(SNodeApplier(root), recomposer).apply {
            setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides this@ComposeSwiftUIHost,
                    LocalHostInput provides input.value,
                    content = content,
                )
            }
        }
    }

    /**
     * Runs pending recompositions right away. Swift calls this after invoking a Kotlin callback so
     * that the state change is reflected in the same SwiftUI transaction when possible.
     */
    fun flush() {
        // A few passes: effects of one pass (e.g. LazyForEach updating its rows) may schedule more work.
        repeat(4) {
            Snapshot.sendApplyNotifications()
            if (!clock.hasAwaiters) return
            clock.sendFrame(now())
        }
    }

    fun dispose() {
        liveHosts -= this
        composition?.dispose()
        composition = null
        recomposer.cancel()
        scope.cancel()
        viewModelStore.clear()
    }

    private fun scheduleFrame() {
        if (frameScheduled) return
        frameScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            frameScheduled = false
            clock.sendFrame(now())
        }
    }

    private fun now(): Long = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000).toLong()
}

private val liveHosts = LinkedHashSet<ComposeSwiftUIHost>()

/**
 * Synchronously applies pending state changes in every live host. Swift calls this right after
 * invoking a Kotlin callback so the UI update lands in the same SwiftUI transaction.
 */
fun flushCompose() {
    liveHosts.toList().forEach { it.flush() }
}

/** Creates a host for [content]. Exported entry point for Swift (`ComposeView(host:)`). */
fun composeSwiftUI(content: @Composable () -> Unit): ComposeSwiftUIHost = ComposeSwiftUIHost(content)

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
