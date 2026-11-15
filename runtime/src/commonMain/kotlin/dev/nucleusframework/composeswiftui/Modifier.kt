package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Ordered chain of SwiftUI view modifiers, applied in declaration order exactly like chaining
 * modifiers in Swift: `Modifier.padding().background(Color.red)` == `.padding().background(.red)`.
 */
@Immutable
interface Modifier {
    val elements: List<SModifier>

    infix fun then(other: Modifier): Modifier =
        if (other.elements.isEmpty()) this else ModifierChain(elements + other.elements)

    companion object : Modifier {
        override val elements: List<SModifier> = emptyList()
        override fun toString() = "Modifier"
    }
}

private class ModifierChain(override val elements: List<SModifier>) : Modifier {
    override fun equals(other: Any?) = other is ModifierChain && other.elements == elements
    override fun hashCode() = elements.hashCode()
}

/** Appends a raw modifier element. Used by generated modifier extensions. */
fun Modifier.element(id: Int, vararg args: Any?): Modifier = ModifierChain(elements + SModifier(id, args.toList()))

/** Wraps composable content passed to a modifier (overlay, background, sheet, toolbar...). */
fun slotContent(content: @Composable () -> Unit): Any = SlotContent(content)

fun action(fn: () -> Unit): SAction = SAction(fn)

@Suppress("UNCHECKED_CAST")
fun <T> valueCallback(fn: (T) -> Unit): SValueCallback = SValueCallback { fn(it as T) }
