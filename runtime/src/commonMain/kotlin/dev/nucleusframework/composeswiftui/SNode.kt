package dev.nucleusframework.composeswiftui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Receives change notifications for a [SNode]. Implemented on the Swift side by the
 * observable model that backs each rendered SwiftUI view.
 */
interface SNodeObserver {
    /** Args, modifiers or children of the node changed. Always called on the main thread. */
    fun nodeDidChange()
}

/** Stable callback handle passed to Swift. The wrapped function is swapped on recomposition. */
class SAction internal constructor(internal var fn: () -> Unit) {
    fun invoke() = fn()
}

/**
 * A Kotlin `suspend () -> Unit` passed to Swift for an `async` SwiftUI closure (`refreshable`, `task`): Swift awaits
 * it ([start] runs it on the main thread and calls `done` when it ends) and cancels the coroutine through the returned
 * action when the SwiftUI task is cancelled.
 */
class SAsyncAction internal constructor(internal var fn: suspend () -> Unit) {
    fun start(done: SwiftAction): SwiftAction {
        val job = asyncActionScope.launch {
            try {
                fn()
            } finally {
                done.run()
            }
        }
        return object : SwiftAction {
            override fun run() = job.cancel()
        }
    }
}

/**
 * A Kotlin `suspend` function with arguments and a result, passed to Swift where SwiftUI expects an `async` closure
 * that returns a value: Swift awaits it ([start] calls `done` with `[result]`, `[null]` on failure or cancellation).
 */
class SAsyncFunction internal constructor(internal var fn: suspend (List<Any?>) -> Any?) {
    fun start(args: List<Any?>, done: KotlinFunction): SwiftAction {
        val job = asyncActionScope.launch {
            var result: Any? = null
            try {
                result = fn(args)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                println("Compose SwiftUI: async callback failed: $e")
            } finally {
                done.invoke(listOf(result))
            }
        }
        return object : SwiftAction {
            override fun run() = job.cancel()
        }
    }
}

private val asyncActionScope = MainScope()

/** Stable value callback handle passed to Swift (bindings, value-taking closures). */
class SValueCallback internal constructor(internal var fn: (Any?) -> Unit) {
    fun invoke(value: Any?) = fn(value)
}

/** Reference to slot content (a nested view hierarchy), by slot index on the owning node. */
class SSlotRef internal constructor(val index: Int) {
    override fun equals(other: Any?) = other is SSlotRef && other.index == index
    override fun hashCode() = index
    override fun toString() = "SSlotRef($index)"
}

/**
 * A SwiftUI expression value: a static member, enum case, initializer or static factory of a
 * SwiftUI type (e.g. `Font.title`, `Color(red:green:blue:)`, `.borderedProminent`).
 * [id] is a generated identifier decoded by the generated Swift code.
 */
open class SExpr(val id: Int, val args: List<Any?> = emptyList()) : SArgs by ExprArgs(args) {
    override fun equals(other: Any?) = other is SExpr && other.id == id && other.args == args
    override fun hashCode() = id * 31 + args.hashCode()
    override fun toString() = "SExpr($id, $args)"
}

/** One applied view modifier: generated [id] plus its encoded arguments. */
class SModifier(val id: Int, val args: List<Any?>) : SArgs by ExprArgs(args) {
    override fun equals(other: Any?) = other is SModifier && other.id == id && other.args == args
    override fun hashCode() = id * 31 + args.hashCode()
}

/**
 * Node of the tree produced by composition. Swift renders each node as one SwiftUI view.
 * [kind] identifies the SwiftUI view type (generated id, or one of [SNodeKinds]).
 */
class SNode internal constructor(val kind: Int) : SArgs {
    val id: Long = nextId++

    var args: List<Any?> = emptyList()
        private set

    private val argsView = object : ListArgs() {
        override val argList get() = args
    }

    override fun argCount() = argsView.argCount()
    override fun argIsNull(index: Int) = argsView.argIsNull(index)
    override fun argDouble(index: Int) = argsView.argDouble(index)
    override fun argInt(index: Int) = argsView.argInt(index)
    override fun argBool(index: Int) = argsView.argBool(index)
    override fun argString(index: Int) = argsView.argString(index)
    override fun argExpr(index: Int) = argsView.argExpr(index)
    override fun argAction(index: Int) = argsView.argAction(index)
    override fun argCallback(index: Int) = argsView.argCallback(index)
    override fun argSlot(index: Int) = argsView.argSlot(index)
    override fun argExprList(index: Int) = argsView.argExprList(index)
    override fun argList(index: Int) = argsView.argList(index)
    override fun argAt(index: Int) = argsView.argAt(index)

    /** Modifier [index] of the chain (avoids bridging the whole list from Swift). */
    fun modifierAt(index: Int): SModifier = modifiers[index]
    fun modifierCount(): Int = modifiers.size
    var modifiers: List<SModifier> = emptyList()
        private set

    /** Regular children (the `@ViewBuilder content` of the view). */
    val children: List<SNode> get() = _children.filter { it.kind != SNodeKinds.SLOT }

    /** Slot nodes by slot index (secondary builders such as labels, overlays, sheets). */
    fun slot(index: Int): SNode? = _children.firstOrNull { it.kind == SNodeKinds.SLOT && it.slotIndex == index }

    /** True when the node has neither children nor slots (rendered inline by its parent on the Swift side). */
    val isLeaf: Boolean get() = _children.isEmpty()

    var slotIndex: Int = -1
        internal set

    /** Swift-side model attached to this node (opaque to Kotlin). */
    var observer: SNodeObserver? = null

    /** Monotonic change counter, handy for Swift-side diffing and debugging. */
    var version: Int = 0
        private set

    internal val _children = ArrayList<SNode>()

    internal fun notifyChanged() {
        version++
        observer?.nodeDidChange()
    }

    // Last values produced by composition. `args` / `modifiers` may differ temporarily when Swift
    // writes a binding value optimistically (see setArgSilently).
    private var composedArgs: List<Any?> = emptyList()
    private var composedModifiers: List<SModifier> = emptyList()

    internal fun updateArgs(newArgs: List<Any?>) {
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

    internal fun updateModifiers(newModifiers: List<SModifier>) {
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
    fun setArgSilently(index: Int, value: Any?) {
        if (index !in args.indices) return
        args = args.toMutableList().also { it[index] = value }
    }

    fun setModifierArgSilently(modifierIndex: Int, argIndex: Int, value: Any?) {
        val m = modifiers.getOrNull(modifierIndex) ?: return
        if (argIndex !in m.args.indices) return
        modifiers = modifiers.toMutableList().also {
            it[modifierIndex] = SModifier(m.id, m.args.toMutableList().apply { this[argIndex] = value })
        }
    }

    override fun toString() = "SNode(kind=$kind, id=$id)"

    private companion object {
        var nextId = 1L
    }
}

object SNodeKinds {
    const val ROOT = -1
    const val SLOT = -2
    const val LAZY_FOR_EACH = 100_014
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
