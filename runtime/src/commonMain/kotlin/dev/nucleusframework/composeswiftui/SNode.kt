package dev.nucleusframework.composeswiftui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Receives change notifications for a [SNode]. Implemented on the Swift side by the
 * observable model that backs each rendered SwiftUI view.
 */
public interface SNodeObserver {
    /** Args, modifiers or children of the node changed. Always called on the main thread. */
    public fun nodeDidChange()
}

/** Stable callback handle passed to Swift. The wrapped function is swapped on recomposition. */
public class SAction internal constructor(internal var fn: () -> Unit) {
    /** Runs the action; an exception is reported ([ComposeSwiftUI.onError]) instead of reaching Swift. */
    public fun invoke(): Unit = guarded(ComposeSwiftUIErrorSource.Callback, Unit) { fn() }
}

/**
 * A Kotlin `suspend () -> Unit` passed to Swift for an `async` SwiftUI closure (`refreshable`, `task`): Swift awaits
 * it ([start] runs it on the main thread and calls `done` when it ends) and cancels the coroutine through the returned
 * action when the SwiftUI task is cancelled. It runs in the scope of the host that composed it, so disposing the host
 * cancels it.
 */
public class SAsyncAction internal constructor(internal var fn: suspend () -> Unit) {
    internal var scope: CoroutineScope? = null

    public fun start(done: SwiftAction): SwiftAction {
        val job = (scope ?: detachedAsyncScope).launch {
            try {
                fn()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ComposeSwiftUI.reportError(ComposeSwiftUIErrorSource.AsyncTask, e)
            }
        }
        // Swift awaits `done` whatever ends the coroutine: completion, failure or cancellation (the host's included).
        job.invokeOnCompletion { onMain { done.run() } }
        return object : SwiftAction {
            override fun run() = job.cancel()
        }
    }
}

/**
 * A Kotlin `suspend` function with arguments and a result, passed to Swift where SwiftUI expects an `async` closure
 * that returns a value: Swift awaits it ([start] calls `done` with `[result]`, `[null]` on failure or cancellation).
 */
public class SAsyncFunction internal constructor(internal var fn: suspend (List<Any?>) -> Any?) {
    internal var scope: CoroutineScope? = null

    public fun start(args: List<Any?>, done: KotlinFunction): SwiftAction {
        var result: Any? = null
        val job = (scope ?: detachedAsyncScope).launch {
            try {
                result = fn(args)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ComposeSwiftUI.reportError(ComposeSwiftUIErrorSource.AsyncTask, e)
            }
        }
        job.invokeOnCompletion { onMain { guarded(ComposeSwiftUIErrorSource.Callback, Unit) { done.invoke(listOf(result)) } } }
        return object : SwiftAction {
            override fun run() = job.cancel()
        }
    }
}

/** For async callbacks created outside a host (previews of a single node, tests): never cancelled. */
private val detachedAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + errorHandler(ComposeSwiftUIErrorSource.AsyncTask))

/** Runs [block] on the main thread: right away when already there (completion handlers can run on any thread). */
private inline fun onMain(crossinline block: () -> Unit) {
    if (NSThread.isMainThread) block() else dispatch_async(dispatch_get_main_queue()) { block() }
}

/** Stable value callback handle passed to Swift (bindings, value-taking closures). */
public class SValueCallback internal constructor(internal var fn: (Any?) -> Unit) {
    /** Calls the callback; an exception is reported ([ComposeSwiftUI.onError]) instead of reaching Swift. */
    public fun invoke(value: Any?): Unit = guarded(ComposeSwiftUIErrorSource.Callback, Unit) { fn(value) }
}

/** Reference to slot content (a nested view hierarchy), by slot index on the owning node. */
public class SSlotRef internal constructor(public val index: Int) {
    override fun equals(other: Any?): Boolean = other is SSlotRef && other.index == index
    override fun hashCode(): Int = index
    override fun toString(): String = "SSlotRef($index)"
}

/**
 * A SwiftUI expression value: a static member, enum case, initializer or static factory of a
 * SwiftUI type (e.g. `Font.title`, `Color(red:green:blue:)`, `.borderedProminent`).
 * [id] is a generated identifier decoded by the generated Swift code.
 */
public open class SExpr(public val id: Int, public val args: List<Any?> = emptyList()) : SArgs by ExprArgs(args) {
    override fun equals(other: Any?): Boolean = other is SExpr && other.id == id && other.args == args
    override fun hashCode(): Int = id * 31 + args.hashCode()
    override fun toString(): String = "SExpr($id, $args)"
}

/** One applied view modifier: generated [id] plus its encoded arguments. */
public class SModifier(public val id: Int, public val args: List<Any?>) : SArgs by ExprArgs(args) {
    override fun equals(other: Any?): Boolean = other is SModifier && other.id == id && other.args == args
    override fun hashCode(): Int = id * 31 + args.hashCode()
}

/**
 * Node of the tree produced by composition. Swift renders each node as one SwiftUI view.
 * [kind] identifies the SwiftUI view type (generated id, or one of [SNodeKinds]).
 */
public class SNode internal constructor(public val kind: Int) : SArgs {
    public val id: Long = nextId++

    public var args: List<Any?> = emptyList()
        private set

    private val argsView = object : ListArgs() {
        override val argList get() = args
    }

    override fun argCount(): Int = argsView.argCount()
    override fun argIsNull(index: Int): Boolean = argsView.argIsNull(index)
    override fun argDouble(index: Int): Double = argsView.argDouble(index)
    override fun argInt(index: Int): Int = argsView.argInt(index)
    override fun argLong(index: Int): Long = argsView.argLong(index)
    override fun argBool(index: Int): Boolean = argsView.argBool(index)
    override fun argString(index: Int): String? = argsView.argString(index)
    override fun argExpr(index: Int): SExpr? = argsView.argExpr(index)
    override fun argAction(index: Int): SAction? = argsView.argAction(index)
    override fun argCallback(index: Int): SValueCallback? = argsView.argCallback(index)
    override fun argSlot(index: Int): Int = argsView.argSlot(index)
    override fun argExprList(index: Int): List<SExpr> = argsView.argExprList(index)
    override fun argList(index: Int): List<Any?> = argsView.argList(index)
    override fun argAt(index: Int): Any? = argsView.argAt(index)

    /** Modifier [index] of the chain (avoids bridging the whole list from Swift). */
    public fun modifierAt(index: Int): SModifier = modifiers[index]
    public fun modifierCount(): Int = modifiers.size
    public var modifiers: List<SModifier> = emptyList()
        private set

    /** Regular children (the `@ViewBuilder content` of the view). */
    public val children: List<SNode>
        get() = childrenCache ?: _children.filter { it.kind != SNodeKinds.SLOT }.also { childrenCache = it }

    // Swift reads `children` on every render of the node: filtered once per structural change, not per read
    private var childrenCache: List<SNode>? = null

    /** Called by the applier after inserting, removing or moving children. */
    internal fun childrenChanged() {
        childrenCache = null
    }

    /** Slot nodes by slot index (secondary builders such as labels, overlays, sheets). */
    public fun slot(index: Int): SNode? = _children.firstOrNull { it.kind == SNodeKinds.SLOT && it.slotIndex == index }

    /** True when the node has neither children nor slots (rendered inline by its parent on the Swift side). */
    public val isLeaf: Boolean get() = _children.isEmpty()

    public var slotIndex: Int = -1
        internal set

    /** Swift-side model attached to this node (opaque to Kotlin). */
    public var observer: SNodeObserver? = null

    /** Monotonic change counter, handy for Swift-side diffing and debugging. */
    public var version: Int = 0
        private set

    internal val _children = ArrayList<SNode>()

    /** Scope of the host that composes this node: async callbacks of its args run (and get cancelled) in it. */
    internal var asyncScope: CoroutineScope? = null

    internal fun notifyChanged() {
        version++
        observer?.nodeDidChange()
    }

    // Last values produced by composition. `args` / `modifiers` may differ temporarily when Swift
    // writes a binding value optimistically (see setArgSilently).
    private var composedArgs: List<Any?> = emptyList()
    private var composedModifiers: List<SModifier> = emptyList()

    // What composition gave, before literal patches (see LiteralPatches).
    private var rawArgs: List<Any?> = emptyList()
    private var rawModifiers: List<SModifier> = emptyList()

    /** Applies the current literal patches to the last composed values. */
    internal fun reapplyLiteralPatches() {
        updateArgs(rawArgs)
        updateModifiers(rawModifiers)
    }

    internal fun updateArgs(composed: List<Any?>) {
        asyncScope?.let { bindScope(composed, it) }
        rawArgs = composed
        val newArgs = if (LiteralPatches.isEmpty) composed else LiteralPatches.args(this, composed)
        if (sameShape(composedArgs, newArgs)) {
            transferCallbacks(composedArgs, newArgs)
            if (args !== composedArgs) {
                // a binding was written from Swift: re-sync with what composition decided
                args = composedArgs
                notifyChanged()
            }
        } else {
            composedArgs = newArgs
            args = newArgs
            notifyChanged()
        }
    }

    internal fun updateModifiers(composed: List<SModifier>) {
        asyncScope?.let { scope -> composed.forEach { bindScope(it.args, scope) } }
        rawModifiers = composed
        val newModifiers = if (LiteralPatches.isEmpty) composed else LiteralPatches.modifiers(this, composed)
        val old = composedModifiers
        val same = old.size == newModifiers.size &&
            old.indices.all { old[it].id == newModifiers[it].id && sameShape(old[it].args, newModifiers[it].args) }
        if (same) {
            old.indices.forEach { transferCallbacks(old[it].args, newModifiers[it].args) }
            if (modifiers !== composedModifiers) {
                modifiers = composedModifiers
                notifyChanged()
            }
        } else {
            composedModifiers = newModifiers
            modifiers = newModifiers
            notifyChanged()
        }
    }

    /**
     * Called by Swift when a binding is written: stores the value immediately so SwiftUI reads
     * it back synchronously (e.g. while typing), before Kotlin recomposes.
     */
    public fun setArgSilently(index: Int, value: Any?) {
        if (index !in args.indices) return
        args = args.toMutableList().also { it[index] = value }
    }

    public fun setModifierArgSilently(modifierIndex: Int, argIndex: Int, value: Any?) {
        val m = modifiers.getOrNull(modifierIndex) ?: return
        if (argIndex !in m.args.indices) return
        modifiers = modifiers.toMutableList().also {
            it[modifierIndex] = SModifier(m.id, m.args.toMutableList().apply { this[argIndex] = value })
        }
    }

    override fun toString(): String = "SNode(kind=$kind, id=$id)"

    private companion object {
        var nextId = 1L
    }
}

public object SNodeKinds {
    public const val ROOT: Int = -1
    public const val SLOT: Int = -2
    public const val LAZY_FOR_EACH: Int = 100_014
}

/** Structural equality where callbacks only compare by kind (their target is swapped in place). */
private fun sameShape(a: Any?, b: Any?): Boolean = when {
    a === b -> true
    a is SAction -> b is SAction
    a is SAsyncAction -> b is SAsyncAction
    a is SAsyncFunction -> b is SAsyncFunction
    a is SValueCallback -> b is SValueCallback
    a is List<*> && b is List<*> -> a.size == b.size && a.indices.all { sameShape(a[it], b[it]) }
    a is SExpr && b is SExpr -> a.id == b.id && sameShape(a.args, b.args)
    else -> a == b
}

/** Gives the async callbacks found in [value] the host's scope. */
private fun bindScope(value: Any?, scope: CoroutineScope) {
    when (value) {
        is SAsyncAction -> value.scope = scope
        is SAsyncFunction -> value.scope = scope
        is List<*> -> value.forEach { bindScope(it, scope) }
        is SExpr -> bindScope(value.args, scope)
    }
}

private fun transferCallbacks(old: Any?, new: Any?) {
    when {
        old is SAction && new is SAction -> old.fn = new.fn
        old is SAsyncAction && new is SAsyncAction -> old.fn = new.fn
        old is SAsyncFunction && new is SAsyncFunction -> old.fn = new.fn
        old is SValueCallback && new is SValueCallback -> old.fn = new.fn
        old is List<*> && new is List<*> -> old.indices.forEach { transferCallbacks(old[it], new[it]) }
        old is SExpr && new is SExpr -> transferCallbacks(old.args, new.args)
    }
}

internal class ExprArgs(override val argList: List<Any?>) : ListArgs()
