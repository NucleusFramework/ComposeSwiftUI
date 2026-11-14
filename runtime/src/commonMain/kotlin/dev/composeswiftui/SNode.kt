package dev.composeswiftui

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
open class SExpr(val id: Int, val args: List<Any?> = emptyList()) {
    override fun equals(other: Any?) = other is SExpr && other.id == id && other.args == args
    override fun hashCode() = id * 31 + args.hashCode()
    override fun toString() = "SExpr($id, $args)"
}

/** One applied view modifier: generated [id] plus its encoded arguments. */
class SModifier(val id: Int, val args: List<Any?>) {
    override fun equals(other: Any?) = other is SModifier && other.id == id && other.args == args
    override fun hashCode() = id * 31 + args.hashCode()
}

/**
 * Node of the tree produced by composition. Swift renders each node as one SwiftUI view.
 * [kind] identifies the SwiftUI view type (generated id, or one of [SNodeKinds]).
 */
class SNode internal constructor(val kind: Int) {
    val id: Long = nextId++

    var args: List<Any?> = emptyList()
        private set
    var modifiers: List<SModifier> = emptyList()
        private set

    /** Regular children (the `@ViewBuilder content` of the view). */
    val children: List<SNode> get() = _children.filter { it.kind != SNodeKinds.SLOT }

    /** Slot nodes by slot index (secondary builders such as labels, overlays, sheets). */
    fun slot(index: Int): SNode? = _children.firstOrNull { it.kind == SNodeKinds.SLOT && it.slotIndex == index }

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
}

/** Structural equality where callbacks only compare by kind (their target is swapped in place). */
private fun sameShape(a: Any?, b: Any?): Boolean = when {
    a is SAction -> b is SAction
    a is SValueCallback -> b is SValueCallback
    a is List<*> && b is List<*> -> a.size == b.size && a.indices.all { sameShape(a[it], b[it]) }
    a is SExpr && b is SExpr -> a.id == b.id && sameShape(a.args, b.args)
    else -> a == b
}

private fun transferCallbacks(old: Any?, new: Any?) {
    when {
        old is SAction && new is SAction -> old.fn = new.fn
        old is SValueCallback && new is SValueCallback -> old.fn = new.fn
        old is List<*> && new is List<*> -> old.indices.forEach { transferCallbacks(old[it], new[it]) }
        old is SExpr && new is SExpr -> transferCallbacks(old.args, new.args)
    }
}
