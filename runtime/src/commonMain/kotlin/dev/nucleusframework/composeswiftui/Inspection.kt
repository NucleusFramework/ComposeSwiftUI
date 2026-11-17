package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.LocationSourceInformation
import androidx.compose.runtime.tooling.SourceInformation
import androidx.compose.runtime.tooling.parseSourceInformation

/**
 * Where a composable was called: [offset] and [length] of the call in [file] (0-based [line]), and the called
 * [function], declared in [calleeFile].
 */
internal class CallSite(
    val function: String?,
    val file: String?,
    val line: Int,
    val offset: Int,
    val length: Int,
    val calleeFile: String?,
)

/**
 * Maps the nodes of a composition to the source of the calls that produced them, from the source information the
 * Compose compiler records (`sourceInformation`), the way Android Studio's inspector does. Used by the IDE previews
 * to go from a view to its code, and to patch literals without recompiling.
 */
@OptIn(ComposeToolingApi::class)
internal object CompositionInspector {
    /** Call sites of each node, innermost first. */
    fun nodeSources(data: CompositionData): Map<SNode, List<CallSite>> {
        val result = LinkedHashMap<SNode, List<CallSite>>()
        for (group in data.compositionGroups) walk(group, null, emptyList(), result)
        return result
    }

    /** The source information of a function body, handing out its call locations in call order. */
    private class Context(val info: SourceInformation, val parent: Context?) {
        private var next = 0
        val file: String? get() = info.sourceFile ?: parent?.file

        fun nextLocation(): LocationSourceInformation? {
            val locations = info.locations
            // Calls in a loop repeat from the first repeatable location.
            if (next >= locations.size) locations.indexOfFirst { it.isRepeatable }.takeIf { it >= 0 }?.let { next = it }
            return locations.getOrNull(next++)
        }
    }

    private fun walk(group: CompositionGroup, context: Context?, chain: List<CallSite>, out: MutableMap<SNode, List<CallSite>>) {
        val info = group.sourceInfo?.let { runCatching { parseSourceInformation(it) }.getOrNull() }
        var calls = chain
        var current = context
        if (info != null) {
            if (info.isCall && context != null) {
                context.nextLocation()?.let { location ->
                    calls = listOf(
                        CallSite(info.functionName, context.file, location.lineNumber, location.offset, location.length, info.sourceFile),
                    ) + chain
                }
            }
            current = Context(info, context)
        }
        (group.node as? SNode)?.let { out[it] = calls }
        for (child in group.compositionGroups) walk(child, current, calls, out)
    }

    /** JSON of [nodeSources] for the IDE: `[{"id": …, "calls": [{"function", "file", "line", "offset", "length"}]}]`. */
    fun toJson(sources: Map<SNode, List<CallSite>>): String = buildString {
        append('[')
        var first = true
        for ((node, calls) in sources) {
            if (calls.isEmpty()) continue
            if (!first) append(',')
            first = false
            append("{\"id\":").append(node.id).append(",\"calls\":[")
            calls.forEachIndexed { i, call ->
                if (i > 0) append(',')
                append("{\"function\":").append(jsonString(call.function))
                append(",\"file\":").append(jsonString(call.file))
                append(",\"calleeFile\":").append(jsonString(call.calleeFile))
                append(",\"line\":").append(call.line)
                append(",\"offset\":").append(call.offset)
                append(",\"length\":").append(call.length).append('}')
            }
            append("]}")
        }
        append(']')
    }
}

internal fun jsonString(value: String?): String {
    if (value == null) return "null"
    val sb = StringBuilder("\"")
    for (c in value) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
    }
    return sb.append('"').toString()
}

/**
 * A literal of the source changed in the IDE: [oldText] becomes [newText] at [offset] of [file], in the composable
 * [function]. [kind] is `string`, `number` or `boolean`; the texts are the values (unquoted, unescaped).
 */
class LiteralPatch(
    val file: String,
    val offset: Int,
    val function: String?,
    kind: String,
    oldText: String,
    newText: String,
) {
    internal val old: Any = value(kind, oldText)
    internal val new: Any = value(kind, newText)

    private fun value(kind: String, text: String): Any = when (kind) {
        "number" -> text.replace("_", "").removeSuffix("f").removeSuffix("F").removeSuffix("L").toDouble()
        "boolean" -> text == "true"
        else -> text
    }
}

/**
 * Literal edits applied without recompiling (like Android Studio's live literals): the nodes produced by the code
 * around the literal show [LiteralPatch.new] wherever composition gives them [LiteralPatch.old]. Patches survive
 * recomposition and are replaced by the next build.
 */
internal object LiteralPatches {
    private val byNode = HashMap<Long, List<LiteralPatch>>()

    /** Patches of the IDE, also installed in hosts that start later. */
    var requested: List<LiteralPatch> = emptyList()

    val isEmpty: Boolean get() = byNode.isEmpty()

    fun clear() = byNode.clear()

    /** Patches the nodes of [sources] in the scope of each patch and returns the nodes to update. */
    fun install(patches: List<LiteralPatch>, sources: Map<SNode, List<CallSite>>): Set<SNode> {
        val touched = HashSet<SNode>()
        for (patch in patches) {
            for (node in scope(patch, sources)) {
                byNode[node.id] = byNode[node.id].orEmpty().filterNot { it.file == patch.file && it.offset == patch.offset } + patch
                touched += node
            }
        }
        return touched
    }

    /**
     * Nodes a literal can reach: those of the innermost call around it, else those of the function that contains it,
     * else (for a string, e.g. in a data class of the previews) every node.
     */
    private fun scope(patch: LiteralPatch, sources: Map<SNode, List<CallSite>>): Collection<SNode> {
        val innermost = sources.values.asSequence().flatten()
            .filter { sameFile(it.file, patch.file) && patch.offset >= it.offset && patch.offset < it.offset + it.length }
            .minByOrNull { it.length }
        if (innermost != null) {
            return sources.filterValues { calls -> calls.any { it.file == innermost.file && it.offset == innermost.offset } }.keys
        }
        if (patch.function != null) {
            val inFunction = sources.filterValues { calls -> calls.any { it.function == patch.function && sameFile(it.calleeFile, patch.file) } }.keys
            if (inFunction.isNotEmpty()) return inFunction
        }
        return if (patch.old is String) sources.keys else emptyList()
    }

    private fun sameFile(a: String?, b: String) = a != null && a.substringAfterLast('/') == b.substringAfterLast('/')

    fun args(node: SNode, args: List<Any?>): List<Any?> {
        val patches = byNode[node.id] ?: return args
        return args.map { replace(it, patches) }
    }

    fun modifiers(node: SNode, modifiers: List<SModifier>): List<SModifier> {
        val patches = byNode[node.id] ?: return modifiers
        return modifiers.map { m ->
            val args = m.args.map { replace(it, patches) }
            if (args == m.args) m else SModifier(m.id, args)
        }
    }

    private fun replace(value: Any?, patches: List<LiteralPatch>): Any? = when (value) {
        is String -> patches.fold(value) { text, p ->
            val old = p.old as? String
            if (old != null && old.isNotEmpty() && old in text) text.replaceFirst(old, p.new as String) else text
        }
        is Double -> patches.firstOrNull { (it.old as? Number)?.toDouble() == value }?.let { (it.new as Number).toDouble() } ?: value
        is Float -> patches.firstOrNull { (it.old as? Number)?.toDouble() == value.toDouble() }?.let { (it.new as Number).toFloat() } ?: value
        is Int -> patches.firstOrNull { (it.old as? Number)?.toDouble() == value.toDouble() }?.let { (it.new as Number).toInt() } ?: value
        is Long -> patches.firstOrNull { (it.old as? Number)?.toDouble() == value.toDouble() }?.let { (it.new as Number).toLong() } ?: value
        is Boolean -> patches.firstOrNull { it.old == value }?.new as? Boolean ?: value
        is List<*> -> value.map { replace(it, patches) }.let { if (it == value) value else it }
        is SModifier -> value
        is SExpr -> value.args.map { replace(it, patches) }.let { if (it == value.args) value else SExpr(value.id, it) }
        else -> value
    }
}
