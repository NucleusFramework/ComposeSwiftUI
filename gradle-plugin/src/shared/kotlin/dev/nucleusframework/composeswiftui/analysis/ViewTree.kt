package dev.nucleusframework.composeswiftui.analysis

// Shared by the Gradle plugin and the IDE plugin (compiled into both): JDK only, no JSON library. Callers parse
// `<key>.layout.json` with their own JSON library and hand the result over as maps and lists.

/** A composable call that led to a view: [line] is 0-based, as recorded by the Compose compiler. */
data class ViewCall(val function: String?, val file: String?, val line: Int, val offset: Int, val calleeFile: String?)

/** One view of a rendered preview: its frame in points and the calls that produced it, innermost first. */
data class ViewNode(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val calls: List<ViewCall>,
    /** The strings the view was given (its text, a label, an SF Symbol name), joined. */
    val text: String?,
) {
    /** The call written by the user: the first one outside the file of the view's own declaration. */
    val call: ViewCall? get() = calls.firstOrNull { it.function != null && it.file != calls.first().file } ?: calls.firstOrNull()
    val name: String get() = call?.function ?: "View"
    val location: String? get() = call?.let { c -> c.file?.let { "$it:${c.line + 1}" } }
    val right get() = x + width
    val bottom get() = y + height

    fun contains(other: ViewNode) = other.x >= x - 0.5 && other.y >= y - 0.5 && other.right <= right + 0.5 && other.bottom <= bottom + 0.5
    fun area() = width * height

    fun intersection(other: ViewNode): Double {
        val w = minOf(right, other.right) - maxOf(x, other.x)
        val h = minOf(bottom, other.bottom) - maxOf(y, other.y)
        return if (w > 0 && h > 0) w * h else 0.0
    }

    /** Some call of the chain is one of [functions]: the view is inside such a container. */
    fun isInside(functions: Set<String>) = calls.drop(1).any { it.function in functions }

    internal fun sameCalls(other: List<ViewCall>, from: Int) =
        calls.size + from == other.size && calls.indices.all { calls[it].sameSite(other[it + from]) }
}

private fun ViewCall.sameSite(other: ViewCall) = function == other.function && file == other.file && offset == other.offset

/** The views of a rendered preview (`<key>.layout.json`), in composition order, with their nesting. */
class ViewTree(val width: Double, val height: Double, val nodes: List<ViewNode>) {
    /** Index of the parent of each node, -1 for a root: the closest earlier node whose calls end this node's calls. */
    val parents: IntArray = IntArray(nodes.size).also { parents ->
        val stack = ArrayList<Int>()
        nodes.forEachIndexed { index, node ->
            while (stack.isNotEmpty()) {
                val top = nodes[stack.last()]
                val from = node.calls.size - top.calls.size
                if (from > 0 && top.sameCalls(node.calls, from)) break
                stack.removeAt(stack.lastIndex)
            }
            parents[index] = stack.lastOrNull() ?: -1
            stack += index
        }
    }

    fun children(index: Int) = parents.indices.filter { parents[it] == index }

    /** Depth of each node in the tree. */
    fun depth(index: Int): Int {
        var depth = 0
        var current = parents[index]
        while (current >= 0) {
            depth++
            current = parents[current]
        }
        return depth
    }

    /** The smallest views that contain the point, innermost first. */
    fun nodesAt(px: Double, py: Double): List<ViewNode> =
        nodes.filter { px >= it.x && py >= it.y && px < it.right && py < it.bottom }.sortedBy { it.area() }

    /**
     * The component tree as indented text, one view per line:
     * `Text "Ada Lovelace" · Contacts.kt:22 · x=16 y=12 w=120 h=20`.
     */
    fun format(): String = buildString {
        appendLine("Preview ${number(width)}×${number(height)} pt")
        nodes.forEachIndexed { index, node ->
            append("  ".repeat(depth(index) + 1))
            append(node.name)
            node.text?.let { append(" \"").append(it.take(80)).append('"') }
            node.location?.let { append(" · ").append(it) }
            append(" · x=").append(number(node.x)).append(" y=").append(number(node.y))
            append(" w=").append(number(node.width)).append(" h=").append(number(node.height))
            appendLine()
        }
    }

    companion object {
        /** From the parsed `<key>.layout.json`: `{"width", "height", "nodes": [{"x", "y", "width", "height", "text", "calls": […]}]}`. */
        @Suppress("UNCHECKED_CAST")
        fun parse(json: Map<String, Any?>): ViewTree {
            val nodes = (json["nodes"] as? List<Map<String, Any?>>).orEmpty().map { node ->
                ViewNode(
                    x = node.number("x"),
                    y = node.number("y"),
                    width = node.number("width"),
                    height = node.number("height"),
                    calls = (node["calls"] as? List<Map<String, Any?>>).orEmpty().map { call ->
                        ViewCall(
                            function = call["function"] as? String,
                            file = call["file"] as? String,
                            line = call.number("line").toInt(),
                            offset = call.number("offset").toInt(),
                            calleeFile = call["calleeFile"] as? String,
                        )
                    },
                    text = (node["text"] as? String)?.takeIf { it.isNotBlank() },
                )
            }
            return ViewTree(json.number("width"), json.number("height"), nodes)
        }

        private fun Map<String, Any?>.number(key: String) = (this[key] as? Number)?.toDouble() ?: 0.0
    }
}

internal fun number(value: Double): String =
    if (kotlin.math.abs(value - Math.round(value)) < 0.05) Math.round(value).toString() else String.format(java.util.Locale.ROOT, "%.1f", value)
