package dev.nucleusframework.composeswiftui.testing

import androidx.compose.runtime.Composable
import dev.nucleusframework.composeswiftui.ComposeSwiftUIHost
import dev.nucleusframework.composeswiftui.ExperimentalComposeSwiftUIApi
import dev.nucleusframework.composeswiftui.Modifier
import dev.nucleusframework.composeswiftui.SAction
import dev.nucleusframework.composeswiftui.SExpr
import dev.nucleusframework.composeswiftui.SNode
import dev.nucleusframework.composeswiftui.SValueCallback
import dev.nucleusframework.composeswiftui.accessibilityIdentifier
import dev.nucleusframework.composeswiftui.flushCompose
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.time.TimeSource

/**
 * Composes [ComposeSwiftUITest.setContent] in a Kotlin test, without SwiftUI, and lets the test find nodes, act on
 * them (as SwiftUI would: invoking the button action, writing the binding) and check the result:
 *
 * ```
 * @Test fun increments() = runComposeSwiftUITest {
 *     setContent { Counter() }
 *     onNodeWithText("Increment").performClick()
 *     onNodeWithText("count=1").assertExists()
 * }
 * ```
 *
 * Runs on the main thread (Kotlin/Native tests do); effects and coroutines progress while the test waits
 * ([waitForIdle], [waitUntil]). The host is disposed at the end.
 */
@ExperimentalComposeSwiftUIApi
public fun runComposeSwiftUITest(block: ComposeSwiftUITest.() -> Unit) {
    val test = ComposeSwiftUITest()
    try {
        test.block()
    } finally {
        test.dispose()
    }
}

@ExperimentalComposeSwiftUIApi
public class ComposeSwiftUITest internal constructor() {
    private var current: ComposeSwiftUIHost? = null

    /** The host composing the content of [setContent]. */
    public val host: ComposeSwiftUIHost get() = checkNotNull(current) { "Call setContent first" }

    /** Composes [content]: the lifecycle is RESUMED, as in a visible `ComposeView` of an active scene. */
    public fun setContent(content: @Composable () -> Unit) {
        check(current == null) { "setContent can only be called once per test" }
        current = ComposeSwiftUIHost(content).apply {
            start()
            setVisible(true)
        }
        waitForIdle()
    }

    /** Lets coroutines, effects and pending recompositions run until nothing is left to do now. */
    public fun waitForIdle() {
        repeat(3) {
            pumpMainQueue()
            flushCompose()
        }
    }

    /** Waits until [condition] holds, letting the main queue (coroutines, effects) run; fails after [timeoutMillis]. */
    public fun waitUntil(timeoutMillis: Long = 2_000, condition: () -> Boolean) {
        val start = TimeSource.Monotonic.markNow()
        while (!condition()) {
            if (start.elapsedNow().inWholeMilliseconds > timeoutMillis) throw AssertionError("Condition not met in ${timeoutMillis}ms")
            pumpMainQueue(0.01)
            flushCompose()
        }
    }

    /** The node showing [text]: a string argument of the view (a `Text`, a button title, a label…). */
    public fun onNodeWithText(text: String, substring: Boolean = false): NodeInteraction =
        NodeInteraction(this, "text ${if (substring) "containing" else "="} \"$text\"") { node ->
            node.strings().any { if (substring) it.contains(text) else it == text }
        }

    /** The node with `Modifier.accessibilityIdentifier(identifier)`: the same identifier XCUITest finds. */
    public fun onNodeWithAccessibilityIdentifier(identifier: String): NodeInteraction =
        NodeInteraction(this, "accessibilityIdentifier = \"$identifier\"") { node ->
            node.modifiers.any { it.id in accessibilityIdentifierIds && it.args.firstOrNull() == identifier }
        }

    /** Every node matching [predicate], in tree order. */
    public fun onAllNodes(predicate: (SNode) -> Boolean): List<SNode> = paths().filter { predicate(it.last()) }.map { it.last() }

    /** The tree as indented text (kinds, string arguments, modifier ids), for failure messages and debugging. */
    public fun printTree(): String = buildString {
        fun walk(node: SNode, depth: Int) {
            append("  ".repeat(depth)).append("kind=").append(node.kind)
            node.strings().takeIf { it.isNotEmpty() }?.let { append(' ').append(it) }
            if (node.modifierCount() > 0) append(" modifiers=").append(node.modifiers.map { it.id })
            append('\n')
            node.allChildren().forEach { walk(it, depth + 1) }
        }
        walk(host.root, 0)
    }

    internal fun paths(): List<List<SNode>> {
        val out = ArrayList<List<SNode>>()
        fun walk(path: List<SNode>) {
            out += path
            path.last().allChildren().forEach { walk(path + it) }
        }
        walk(listOf(host.root))
        return out
    }

    internal fun dispose() {
        current?.dispose()
        current = null
    }
}

/** A node found by a finder; resolved again on every call, so it follows recompositions. */
@ExperimentalComposeSwiftUIApi
public class NodeInteraction internal constructor(
    private val test: ComposeSwiftUITest,
    private val description: String,
    private val matcher: (SNode) -> Boolean,
) {
    private fun path(): List<SNode>? = test.paths().firstOrNull { matcher(it.last()) }

    /** The node, or null when nothing matches. */
    public val nodeOrNull: SNode? get() = path()?.last()

    /** The node; fails when nothing matches. */
    public val node: SNode get() = nodeOrNull ?: fail("No node with $description")

    public fun assertExists(): NodeInteraction = apply { node }

    public fun assertDoesNotExist(): NodeInteraction = apply {
        nodeOrNull?.let { fail("Unexpected node with $description: $it") }
    }

    /** The node's string arguments, then those of its descendants: what it shows. */
    public val texts: List<String> get() = buildList {
        fun walk(n: SNode) {
            addAll(n.strings())
            n.allChildren().forEach(::walk)
        }
        walk(node)
    }

    /** Taps the node as SwiftUI would: runs the action of the node, or of the closest ancestor that has one (a button
     * around a label). */
    public fun performClick(): NodeInteraction = apply {
        val path = path() ?: fail("No node with $description to click")
        val action = path.asReversed().firstNotNullOfOrNull { n -> n.actions().firstOrNull() }
            ?: fail("No action on the node with $description or its ancestors")
        action.invoke()
        test.waitForIdle()
    }

    /** Writes [value] into the node's binding (a `TextField` text, a `Toggle` state, a `Slider` value…), as SwiftUI does. */
    public fun performValueChange(value: Any?): NodeInteraction = apply {
        val path = path() ?: fail("No node with $description")
        val callback = path.asReversed().firstNotNullOfOrNull { n -> n.args.firstOrNull { it is SValueCallback } as SValueCallback? }
            ?: fail("No binding on the node with $description or its ancestors")
        callback.invoke(value)
        test.waitForIdle()
    }

    /** Types [text] into a text field (replaces its text). */
    public fun performTextInput(text: String): NodeInteraction = performValueChange(text)

    private fun fail(message: String): Nothing = throw AssertionError("$message\n${test.printTree()}")
}

private val accessibilityIdentifierIds: Set<Int> by lazy {
    setOf(Modifier.accessibilityIdentifier("").elements.single().id, Modifier.accessibilityIdentifier("", true).elements.single().id)
}

/** The string arguments of a node (expression arguments included): its text, title, label… */
internal fun SNode.strings(): List<String> = buildList {
    fun collect(value: Any?) {
        when (value) {
            is String -> if (value.isNotEmpty()) add(value)
            is SExpr -> value.args.forEach(::collect)
            is List<*> -> value.forEach(::collect)
        }
    }
    args.forEach(::collect)
}

private fun SNode.actions(): List<SAction> = args.filterIsInstance<SAction>()

/** Children, and the content of slots (labels, overlays, sheet contents…). */
internal fun SNode.allChildren(): List<SNode> = buildList {
    addAll(children)
    var index = 0
    var misses = 0
    while (misses < 8) {
        val slot = slot(index++)
        if (slot == null) misses++ else {
            misses = 0
            addAll(slot.children)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun pumpMainQueue(seconds: Double = 0.0) {
    NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(seconds))
}

