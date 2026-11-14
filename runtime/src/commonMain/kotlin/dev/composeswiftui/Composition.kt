package dev.composeswiftui

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.key

/** Applies composition changes to the [SNode] tree and notifies the affected parents. */
internal class SNodeApplier(root: SNode) : AbstractApplier<SNode>(root) {
    private val dirty = LinkedHashSet<SNode>()

    override fun insertTopDown(index: Int, instance: SNode) = Unit

    override fun insertBottomUp(index: Int, instance: SNode) {
        current._children.add(index, instance)
        dirty += current
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) { current._children.removeAt(index).detachObservers() }
        dirty += current
    }

    override fun move(from: Int, to: Int, count: Int) {
        current._children.move(from, to, count)
        dirty += current
    }

    override fun onClear() {
        root._children.forEach { it.detachObservers() }
        root._children.clear()
        dirty += root
    }

    override fun onEndChanges() {
        dirty.forEach { it.notifyChanged() }
        dirty.clear()
    }

    private fun <T> MutableList<T>.move(from: Int, to: Int, count: Int) {
        val dest = if (from > to) to else to - count
        if (count == 1) {
            if (from == to + 1 || from == to - 1) {
                val fromEl = get(from)
                set(from, set(to, fromEl))
            } else {
                add(dest, removeAt(from))
            }
        } else {
            val subView = subList(from, from + count)
            val subCopy = subView.toMutableList()
            subView.clear()
            addAll(dest, subCopy)
        }
    }
}

/** Breaks Kotlin <-> Swift reference cycles for a removed subtree. */
private fun SNode.detachObservers() {
    observer = null
    _children.forEach { it.detachObservers() }
}

/** Marker for composable slot content captured inside a modifier argument. */
internal class SlotContent(val content: @Composable () -> Unit)

/**
 * Emits one SwiftUI view node. Used by all generated view composables.
 *
 * @param kind generated view kind id
 * @param args encoded init arguments (`SSlotRef` for secondary builders)
 * @param slots secondary builder contents, referenced by index from [args]
 */
@Composable
fun SwiftView(
    kind: Int,
    args: List<Any?>,
    modifier: Modifier = Modifier,
    slots: List<(@Composable () -> Unit)?> = emptyList(),
    content: @Composable () -> Unit = {},
) {
    // Modifier slot contents are numbered after the view's own slots.
    val modifierSlots = ArrayList<@Composable () -> Unit>()
    val encodedModifiers = modifier.elements.map { m ->
        if (m.args.none { it is SlotContent }) m else SModifier(m.id, m.args.map { arg ->
            if (arg is SlotContent) {
                modifierSlots += arg.content
                SSlotRef(slots.size + modifierSlots.size - 1)
            } else arg
        })
    }
    ComposeNode<SNode, SNodeApplier>(
        factory = { SNode(kind) },
        update = {
            reconcile {
                updateArgs(args)
                updateModifiers(encodedModifiers)
            }
        },
        content = {
            slots.forEachIndexed { index, slot -> if (slot != null) key(index) { Slot(index, slot) } }
            modifierSlots.forEachIndexed { i, slot -> key(slots.size + i) { Slot(slots.size + i, slot) } }
            content()
        },
    )
}

@Composable
private fun Slot(index: Int, content: @Composable () -> Unit) {
    ComposeNode<SNode, SNodeApplier>(
        factory = { SNode(SNodeKinds.SLOT).also { it.slotIndex = index } },
        update = {},
        content = content,
    )
}
