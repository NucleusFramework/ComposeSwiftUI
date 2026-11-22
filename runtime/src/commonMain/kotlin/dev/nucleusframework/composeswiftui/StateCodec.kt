package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState

/**
 * Encodes the saved state of a host (the `rememberSaveable` values and the `SavedStateRegistry`) to a string Swift can
 * keep in `@SceneStorage`, and back. Each value is a tagged JSON array, so numbers keep their Kotlin type:
 * `["i",1]`, `["l","9007199254740993"]`, `["s","text"]`, `["L",[…]]`, `["M",{…}]`, `["state","structural",…]`…
 */
internal object StateCodec {
    /** True when [value] survives [encode] / [decode]: the `canBeSaved` of the host's `SaveableStateRegistry`. */
    fun canBeSaved(value: Any?): Boolean = when (value) {
        null, is Boolean, is Int, is Long, is Float, is Double, is String, is Short, is Byte, is Char -> true
        is IntArray, is LongArray, is DoubleArray, is FloatArray, is BooleanArray -> true
        is MutableIntState, is MutableLongState, is MutableFloatState, is MutableDoubleState -> true
        is SnapshotMutableState<*> -> policyName(value) != null && canBeSaved(value.value)
        is List<*> -> value.all(::canBeSaved)
        is Map<*, *> -> value.keys.all { it is String } && value.values.all(::canBeSaved)
        is SavedState -> true
        else -> false
    }

    fun encode(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    fun decode(text: String): Any? = Reader(text).run { readValue().also { skipSpaces() } }

    private fun policyName(state: SnapshotMutableState<*>): String? = when (state.policy) {
        structuralEqualityPolicy<Any?>() -> "structural"
        referentialEqualityPolicy<Any?>() -> "referential"
        neverEqualPolicy<Any?>() -> "never"
        else -> null
    }

    private fun write(out: StringBuilder, value: Any?) {
        fun tagged(tag: String, body: () -> Unit) {
            out.append("[\"").append(tag).append("\",")
            body()
            out.append(']')
        }
        when (value) {
            null -> out.append("[\"n\"]")
            is Boolean -> tagged("b") { out.append(value) }
            is Int -> tagged("i") { out.append(value) }
            is Short -> tagged("sh") { out.append(value) }
            is Byte -> tagged("by") { out.append(value) }
            is Char -> tagged("c") { writeString(out, value.toString()) }
            // As strings: a JSON number can't hold every Long, nor NaN or the infinities
            is Long -> tagged("l") { writeString(out, value.toString()) }
            is Double -> tagged("d") { writeString(out, value.toString()) }
            is Float -> tagged("f") { writeString(out, value.toString()) }
            is String -> tagged("s") { writeString(out, value) }
            is IntArray -> { write(out, value.toList()); retag(out, "ia") }
            is LongArray -> { write(out, value.toList()); retag(out, "la") }
            is DoubleArray -> { write(out, value.toList()); retag(out, "da") }
            is FloatArray -> { write(out, value.toList()); retag(out, "fa") }
            is BooleanArray -> { write(out, value.toList()); retag(out, "ba") }
            // Primitive states first: they are SnapshotMutableStates too, but restore as their own type
            is MutableIntState -> tagged("istate") { out.append(value.intValue) }
            is MutableLongState -> tagged("lstate") { writeString(out, value.longValue.toString()) }
            is MutableFloatState -> tagged("fstate") { writeString(out, value.floatValue.toString()) }
            is MutableDoubleState -> tagged("dstate") { writeString(out, value.doubleValue.toString()) }
            is SnapshotMutableState<*> -> tagged("state") {
                writeString(out, policyName(value) ?: "structural")
                out.append(',')
                write(out, value.value)
            }
            is List<*> -> tagged("L") { writeList(out, value) }
            is Map<*, *> -> tagged("M") { writeMap(out, value) }
            is SavedState -> tagged("S") { writeMap(out, value.read { toMap() }) }
            else -> throw IllegalArgumentException("Can't save a ${value::class.simpleName}")
        }
    }

    // Arrays are written as a list, then their tag replaces the list's "L" (the last one written).
    private fun retag(out: StringBuilder, tag: String) {
        val at = out.lastIndexOf("[\"L\",")
        out.setRange(at, at + 4, "[\"$tag\"")
    }

    private fun writeList(out: StringBuilder, list: List<*>) {
        out.append('[')
        list.forEachIndexed { i, item ->
            if (i > 0) out.append(',')
            write(out, item)
        }
        out.append(']')
    }

    private fun writeMap(out: StringBuilder, map: Map<*, *>) {
        out.append('{')
        var first = true
        for ((key, item) in map) {
            if (!canBeSaved(item)) continue
            if (!first) out.append(',')
            first = false
            writeString(out, key as String)
            out.append(':')
            write(out, item)
        }
        out.append('}')
    }

    private fun writeString(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Reader(val s: String) {
        var i = 0

        fun skipSpaces() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun expect(c: Char) {
            skipSpaces()
            require(i < s.length && s[i] == c) { "Expected '$c' at $i" }
            i++
        }

        fun peek(): Char {
            skipSpaces()
            return s[i]
        }

        fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (s[i] != '"') {
                val c = s[i++]
                if (c != '\\') {
                    out.append(c)
                    continue
                }
                when (val e = s[i++]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> out.append(s.substring(i, i + 4).toInt(16).toChar()).also { i += 4 }
                    else -> out.append(e)
                }
            }
            i++
            return out.toString()
        }

        fun readRaw(): String {
            skipSpaces()
            val start = i
            while (i < s.length && s[i] !in ",]}") i++
            return s.substring(start, i).trim()
        }

        fun readList(): List<Any?> {
            expect('[')
            val out = ArrayList<Any?>()
            if (peek() == ']') return out.also { i++ }
            while (true) {
                out += readValue()
                if (peek() == ',') i++ else break
            }
            expect(']')
            return out
        }

        fun readMap(): Map<String, Any?> {
            expect('{')
            val out = LinkedHashMap<String, Any?>()
            if (peek() == '}') return out.also { i++ }
            while (true) {
                val key = readString()
                expect(':')
                out[key] = readValue()
                if (peek() == ',') i++ else break
            }
            expect('}')
            return out
        }

        fun readValue(): Any? {
            expect('[')
            val tag = readString()
            if (tag == "n") return null.also { expect(']') }
            expect(',')
            val value: Any? = when (tag) {
                "b" -> readRaw().toBooleanStrict()
                "i" -> readRaw().toInt()
                "sh" -> readRaw().toShort()
                "by" -> readRaw().toByte()
                "c" -> readString().single()
                "l" -> readString().toLong()
                "d" -> readString().toDouble()
                "f" -> readString().toFloat()
                "s" -> readString()
                "L" -> readList()
                "ia" -> readList().map { it as Int }.toIntArray()
                "la" -> readList().map { it as Long }.toLongArray()
                "da" -> readList().map { it as Double }.toDoubleArray()
                "fa" -> readList().map { it as Float }.toFloatArray()
                "ba" -> readList().map { it as Boolean }.toBooleanArray()
                "M" -> readMap()
                "S" -> savedState(readMap())
                "istate" -> mutableIntStateOf(readRaw().toInt())
                "lstate" -> mutableLongStateOf(readString().toLong())
                "fstate" -> mutableFloatStateOf(readString().toFloat())
                "dstate" -> mutableDoubleStateOf(readString().toDouble())
                "state" -> {
                    val policy = readString()
                    expect(',')
                    mutableStateOf(
                        readValue(),
                        when (policy) {
                            "referential" -> referentialEqualityPolicy()
                            "never" -> neverEqualPolicy()
                            else -> structuralEqualityPolicy()
                        },
                    ) as MutableState<Any?>
                }
                else -> throw IllegalArgumentException("Unknown saved value tag '$tag'")
            }
            expect(']')
            return value
        }
    }
}
