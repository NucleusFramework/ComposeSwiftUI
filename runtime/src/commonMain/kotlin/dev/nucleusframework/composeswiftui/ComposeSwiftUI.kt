package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import platform.Foundation.NSLog

/** Where the runtime caught a Kotlin exception (see [ComposeSwiftUI.onError]). */
public enum class ComposeSwiftUIErrorSource {
    /** A callback Swift invoked: a button action, a binding setter, a gesture… The UI keeps running. */
    Callback,

    /** Composition or recomposition, or an effect (`LaunchedEffect`, `DisposableEffect`) of the composition. The
     * failing host stops composing and shows nothing (a red message in debug builds). */
    Composition,

    /** A `suspend` action Swift awaits (`task { }`, `refreshable { }`, an async closure). The UI keeps running. */
    AsyncTask,
}

/** Severity of a runtime log line, mapped to the `os_log` types by the Swift renderer. */
public enum class ComposeSwiftUILogLevel { Debug, Info, Error, Fault }

/** Writes the runtime's log lines. The Swift renderer installs one that writes to the unified log (`os_log`). */
public fun interface ComposeSwiftUILogger {
    public fun log(level: ComposeSwiftUILogLevel, message: String)
}

/** Global configuration of the Compose SwiftUI runtime. */
public object ComposeSwiftUI {
    /**
     * Called on the main thread for every Kotlin exception the runtime catches instead of letting it terminate the
     * app: plug Crashlytics, Sentry or your own reporting here. The error is logged before the call either way.
     */
    public var onError: ((source: ComposeSwiftUIErrorSource, error: Throwable) -> Unit)? = null

    /** Receives the runtime's log lines. Default: `NSLog`; the Swift renderer replaces it with an `os_log` logger. */
    public var logger: ComposeSwiftUILogger = ComposeSwiftUILogger { level, message ->
        // No format arguments: Kotlin strings can't go through C varargs
        NSLog("[${level.name}] $message".replace("%", "%%"))
    }

    internal val decorators = ArrayList<HostDecorator>()

    /**
     * Wraps the content of every host created from now on, outermost first: provides composition locals other
     * libraries expect (Compose UI's `LocalDensity` for `stringResource`, a DI scope…).
     */
    public fun addHostDecorator(decorator: HostDecorator) {
        decorators += decorator
    }

    /** Reports [error] like the runtime does: logs it, then calls [onError]. For Kotlin code that catches its own errors. */
    public fun reportError(source: ComposeSwiftUIErrorSource, error: Throwable) {
        logger.log(ComposeSwiftUILogLevel.Error, "Compose SwiftUI: uncaught exception in ${source.name}: ${error.stackTraceToString()}")
        try {
            onError?.invoke(source, error)
        } catch (e: Throwable) {
            logger.log(ComposeSwiftUILogLevel.Fault, "Compose SwiftUI: ComposeSwiftUI.onError threw: $e")
        }
    }
}

/** Wraps the content of every host (see [ComposeSwiftUI.addHostDecorator]). */
public interface HostDecorator {
    @Composable
    public fun Decorate(content: @Composable () -> Unit)
}

/** Runs [block], reporting any exception other than a cancellation instead of throwing it (into Swift, where it
 * would terminate the process). */
internal inline fun <T> guarded(source: ComposeSwiftUIErrorSource, fallback: T, block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        fallback
    } catch (e: Throwable) {
        ComposeSwiftUI.reportError(source, e)
        fallback
    }

internal fun errorHandler(source: ComposeSwiftUIErrorSource, then: (Throwable) -> Unit = {}) =
    CoroutineExceptionHandler { _, e ->
        ComposeSwiftUI.reportError(source, e)
        then(e)
    }
