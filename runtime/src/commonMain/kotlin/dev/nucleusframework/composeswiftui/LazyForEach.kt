package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * SwiftUI `ForEach(items, id:)` whose rows are composed only while SwiftUI shows them. Inside a `List`, SwiftUI
 * creates rows as they scroll into view: each row then gets its own small composition, disposed when the row leaves
 * the screen. Use it for long lists; a plain Kotlin loop composes every row up front. Each row should be one view.
 *
 * A row recomposes when its item changes (by `equals`), so prefer immutable items such as data classes.
 */
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
@Composable
public fun <T> LazyForEach(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    val parent = rememberCompositionContext()
    val lazyItems = remember { LazyItems() }
    lazyItems.parent = parent
    @Suppress("UNCHECKED_CAST")
    lazyItems.content = content as @Composable (Any?) -> Unit
    // The identity version only changes when keys are added, removed or moved: SwiftUI then re-reads the ids.
    val version = lazyItems.prepare(items, key)
    SideEffect { lazyItems.update(items) }
    DisposableEffect(lazyItems) { onDispose { lazyItems.disposeAll() } }
    SwiftView(SNodeKinds.LAZY_FOR_EACH, listOf(lazyItems, version))
}

/** The rows of a [LazyForEach], composed on demand by the Swift renderer. */
public class LazyItems internal constructor() {
    internal var parent: CompositionContext? = null
    internal var content: @Composable (Any?) -> Unit = {}
    private var keys: List<Any> = emptyList()
    private var items: List<Any?> = emptyList()
    private var indexByKey: Map<Any, Int> = emptyMap()
    private var idByKey: Map<Any, Long> = emptyMap()
    private var keyById: Map<Long, Any> = emptyMap()
    private var nextId = 0L
    private var version = 0
    private val rows = HashMap<SNode, Row>()

    /** One stable id per item, in order: SwiftUI's `ForEach` identity, read by the Swift renderer. */
    public var ids: LongArray = LongArray(0)
        private set

    private class Row(val id: Long, val item: MutableState<Any?>, val composition: Composition)

    /** Composes the row with [id] and returns the root of its node tree (called when SwiftUI shows the row). */
    public fun compose(id: Long): SNode {
        val root = SNode(SNodeKinds.ROOT)
        val parent = parent ?: return root
        val item = mutableStateOf(itemFor(id))
        val composition = Composition(SNodeApplier(root), parent)
        composition.setContent { content(item.value) }
        rows[root] = Row(id, item, composition)
        return root
    }

    /** Disposes a row composed by [compose] (called when SwiftUI hides the row). */
    public fun dispose(root: SNode) {
        rows.remove(root)?.composition?.dispose()
    }

    /** Recomputes the ids if the keys changed; returns the identity version. */
    internal fun <T> prepare(newItems: List<T>, key: (T) -> Any): Int {
        // An item that is the same instance as before keeps its key: only the others are keyed again.
        val old = items
        val same = newItems.size == keys.size &&
            newItems.indices.all { newItems[it] === old.getOrNull(it) || key(newItems[it]) == keys[it] }
        if (same) return version
        val newKeys = newItems.map(key)
        val newIdByKey = HashMap<Any, Long>(newKeys.size * 2)
        val newIds = LongArray(newKeys.size)
        newKeys.forEachIndexed { i, k ->
            val id = idByKey[k] ?: nextId++
            newIdByKey[k] = id
            newIds[i] = id
        }
        keys = newKeys
        idByKey = newIdByKey
        keyById = newIdByKey.entries.associate { (k, id) -> id to k }
        indexByKey = HashMap<Any, Int>(newKeys.size * 2).apply { newKeys.forEachIndexed { i, k -> put(k, i) } }
        ids = newIds
        return ++version
    }

    internal fun update(newItems: List<Any?>) {
        items = newItems
        // Only rows whose item changed recompose (structural equality).
        for (row in rows.values) row.item.value = itemFor(row.id)
    }

    internal fun disposeAll() {
        rows.values.forEach { it.composition.dispose() }
        rows.clear()
    }

    private fun itemFor(id: Long): Any? = keyById[id]?.let { indexByKey[it] }?.let { items.getOrNull(it) }
}
