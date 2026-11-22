package dev.nucleusframework.composeswiftui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Handle returned by [watchFlow]; cancel it to stop observing. */
public class FlowWatcher internal constructor(private val job: Job) {
    public fun cancel(): Unit = job.cancel()
}

/**
 * Lets plain Swift / SwiftUI code observe a Kotlin [StateFlow] (e.g. a shared ViewModel's state).
 * [onEach] runs on the main thread, synchronously with the emission when emitted from main.
 */
public fun <T> watchFlow(flow: StateFlow<T>, onEach: (T) -> Unit): FlowWatcher {
    val job = CoroutineScope(Dispatchers.Main.immediate).launch(start = CoroutineStart.UNDISPATCHED) {
        flow.collect { onEach(it) }
    }
    return FlowWatcher(job)
}
