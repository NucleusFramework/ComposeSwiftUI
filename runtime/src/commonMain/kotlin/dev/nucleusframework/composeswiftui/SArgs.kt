package dev.nucleusframework.composeswiftui

/**
 * Typed access to encoded arguments, used by the Swift renderer: values come back as primitives or
 * typed objects, which avoids Swift dynamic casts from `Any` and NSArray bridging on the hot path.
 */
public interface SArgs {
    public fun argCount(): Int
    public fun argIsNull(index: Int): Boolean
    public fun argDouble(index: Int): Double
    public fun argInt(index: Int): Int

    /** A 64-bit integer (Swift `Int64` / `UInt64`), without the truncation of [argInt]. */
    public fun argLong(index: Int): Long
    public fun argBool(index: Int): Boolean
    public fun argString(index: Int): String?
    public fun argExpr(index: Int): SExpr?
    public fun argAction(index: Int): SAction?
    public fun argCallback(index: Int): SValueCallback?
    /** Slot index referenced at [index], or -1. */
    public fun argSlot(index: Int): Int
    public fun argExprList(index: Int): List<SExpr>
    public fun argList(index: Int): List<Any?>
    public fun argAt(index: Int): Any?
}

internal fun List<Any?>.argIsNullAt(i: Int) = getOrNull(i) == null
internal fun List<Any?>.argDoubleAt(i: Int) = (getOrNull(i) as? Number)?.toDouble() ?: 0.0
internal fun List<Any?>.argIntAt(i: Int) = (getOrNull(i) as? Number)?.toInt() ?: 0
internal fun List<Any?>.argLongAt(i: Int) = (getOrNull(i) as? Number)?.toLong() ?: 0L
internal fun List<Any?>.argBoolAt(i: Int) = getOrNull(i) as? Boolean ?: false
internal fun List<Any?>.argStringAt(i: Int) = getOrNull(i) as? String
internal fun List<Any?>.argExprAt(i: Int) = getOrNull(i) as? SExpr
internal fun List<Any?>.argActionAt(i: Int) = getOrNull(i) as? SAction
internal fun List<Any?>.argCallbackAt(i: Int) = getOrNull(i) as? SValueCallback
internal fun List<Any?>.argSlotAt(i: Int) = (getOrNull(i) as? SSlotRef)?.index ?: -1
internal fun List<Any?>.argExprListAt(i: Int): List<SExpr> = (getOrNull(i) as? List<*>)?.filterIsInstance<SExpr>() ?: emptyList()
internal fun List<Any?>.argListAt(i: Int): List<Any?> = getOrNull(i) as? List<Any?> ?: emptyList()

/** Implements [SArgs] over a list provider (shared by nodes, modifiers and expressions). */
internal abstract class ListArgs : SArgs {
    internal abstract val argList: List<Any?>
    override fun argCount() = argList.size
    override fun argIsNull(index: Int) = argList.argIsNullAt(index)
    override fun argDouble(index: Int) = argList.argDoubleAt(index)
    override fun argInt(index: Int) = argList.argIntAt(index)
    override fun argLong(index: Int) = argList.argLongAt(index)
    override fun argBool(index: Int) = argList.argBoolAt(index)
    override fun argString(index: Int) = argList.argStringAt(index)
    override fun argExpr(index: Int) = argList.argExprAt(index)
    override fun argAction(index: Int) = argList.argActionAt(index)
    override fun argCallback(index: Int) = argList.argCallbackAt(index)
    override fun argSlot(index: Int) = argList.argSlotAt(index)
    override fun argExprList(index: Int) = argList.argExprListAt(index)
    override fun argList(index: Int) = argList.argListAt(index)
    override fun argAt(index: Int) = argList.getOrNull(index)
}
