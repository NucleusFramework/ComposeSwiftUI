@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import kotlin.native.HiddenFromObjC

// Symbolic scalar expressions evaluated natively on the Swift side — zero Kotlin callbacks per animation or scroll
// frame. Used by AnimatablePathShape (InsettableShape + Animatable), visualEffectExpr, and scrollTransitionExpr.
// Swift side: Expressions.swift. Value id 16, modifier ids 100_116..100_117.

internal object ExpressionIds {
    const val ANIMATABLE_PATH_SHAPE = 16
    const val VISUAL_EFFECT_EXPR = 100_116
    const val SCROLL_TRANSITION_EXPR = 100_117
}

/**
 * A symbolic scalar expression evaluated natively in Swift on every animation, layout, or scroll frame without
 * crossing the Kotlin/Swift bridge.
 *
 * Build expressions with arithmetic operators (`+`, `-`, `*`, `/`, `%`), math helpers ([abs], [sqrt], [sin], [cos],
 * [floor], [ceil], [round], [pow], [clamp]), and conditionals ([select], [lt], [le], [gt], [ge], [eq]).
 */
@HiddenFromObjC
public class ScalarExpr internal constructor(internal val encoded: Any) {

    public operator fun unaryMinus(): ScalarExpr = ScalarExpr(listOf(2, encoded))

    public operator fun plus(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(10, encoded, other.encoded))
    public operator fun plus(other: Double): ScalarExpr = ScalarExpr(listOf(10, encoded, other))

    public operator fun minus(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(11, encoded, other.encoded))
    public operator fun minus(other: Double): ScalarExpr = ScalarExpr(listOf(11, encoded, other))

    public operator fun times(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(12, encoded, other.encoded))
    public operator fun times(other: Double): ScalarExpr = ScalarExpr(listOf(12, encoded, other))

    public operator fun div(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(13, encoded, other.encoded))
    public operator fun div(other: Double): ScalarExpr = ScalarExpr(listOf(13, encoded, other))

    public operator fun rem(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(14, encoded, other.encoded))
    public operator fun rem(other: Double): ScalarExpr = ScalarExpr(listOf(14, encoded, other))

    public fun abs(): ScalarExpr = ScalarExpr(listOf(3, encoded))
    public fun sqrt(): ScalarExpr = ScalarExpr(listOf(4, encoded))
    public fun sin(): ScalarExpr = ScalarExpr(listOf(5, encoded))
    public fun cos(): ScalarExpr = ScalarExpr(listOf(6, encoded))
    public fun floor(): ScalarExpr = ScalarExpr(listOf(7, encoded))
    public fun ceil(): ScalarExpr = ScalarExpr(listOf(8, encoded))
    public fun round(): ScalarExpr = ScalarExpr(listOf(9, encoded))

    public fun pow(exponent: ScalarExpr): ScalarExpr = ScalarExpr(listOf(15, encoded, exponent.encoded))
    public fun pow(exponent: Double): ScalarExpr = ScalarExpr(listOf(15, encoded, exponent))

    public fun clamp(minValue: ScalarExpr, maxValue: ScalarExpr): ScalarExpr =
        max(minValue, min(this, maxValue))

    public fun clamp(minValue: Double, maxValue: Double): ScalarExpr =
        clamp(const(minValue), const(maxValue))

    public fun lt(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(18, encoded, other.encoded))
    public fun lt(other: Double): ScalarExpr = ScalarExpr(listOf(18, encoded, other))

    public fun le(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(19, encoded, other.encoded))
    public fun le(other: Double): ScalarExpr = ScalarExpr(listOf(19, encoded, other))

    public fun gt(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(20, encoded, other.encoded))
    public fun gt(other: Double): ScalarExpr = ScalarExpr(listOf(20, encoded, other))

    public fun ge(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(21, encoded, other.encoded))
    public fun ge(other: Double): ScalarExpr = ScalarExpr(listOf(21, encoded, other))

    public fun eq(other: ScalarExpr): ScalarExpr = ScalarExpr(listOf(22, encoded, other.encoded))
    public fun eq(other: Double): ScalarExpr = ScalarExpr(listOf(22, encoded, other))

    override fun equals(other: Any?): Boolean = other is ScalarExpr && encoded == other.encoded
    override fun hashCode(): Int = encoded.hashCode()
    override fun toString(): String = "ScalarExpr($encoded)"

    public companion object {
        /** A constant number. */
        public fun const(value: Double): ScalarExpr = ScalarExpr(value)

        /** Contextual input argument at [index] (e.g. geometry, rect component, or scroll phase). */
        public fun arg(index: Int): ScalarExpr = ScalarExpr(listOf(0, index))

        /** Animatable parameter at [index] (interpolated by SwiftUI via `Animatable.animatableData`). */
        public fun param(index: Int): ScalarExpr = ScalarExpr(listOf(1, index))

        public fun min(a: ScalarExpr, b: ScalarExpr): ScalarExpr = ScalarExpr(listOf(16, a.encoded, b.encoded))
        public fun min(a: ScalarExpr, b: Double): ScalarExpr = ScalarExpr(listOf(16, a.encoded, b))
        public fun min(a: Double, b: ScalarExpr): ScalarExpr = ScalarExpr(listOf(16, a, b.encoded))

        public fun max(a: ScalarExpr, b: ScalarExpr): ScalarExpr = ScalarExpr(listOf(17, a.encoded, b.encoded))
        public fun max(a: ScalarExpr, b: Double): ScalarExpr = ScalarExpr(listOf(17, a.encoded, b))
        public fun max(a: Double, b: ScalarExpr): ScalarExpr = ScalarExpr(listOf(17, a, b.encoded))

        /** Evaluates to [ifTrue] when [condition] is non-zero, otherwise [ifFalse]. */
        public fun select(condition: ScalarExpr, ifTrue: ScalarExpr, ifFalse: ScalarExpr): ScalarExpr =
            ScalarExpr(listOf(23, condition.encoded, ifTrue.encoded, ifFalse.encoded))

        public fun select(condition: ScalarExpr, ifTrue: Double, ifFalse: Double): ScalarExpr =
            ScalarExpr(listOf(23, condition.encoded, ifTrue, ifFalse))
    }
}

@HiddenFromObjC public operator fun Double.plus(other: ScalarExpr): ScalarExpr = ScalarExpr.const(this) + other
@HiddenFromObjC public operator fun Double.minus(other: ScalarExpr): ScalarExpr = ScalarExpr.const(this) - other
@HiddenFromObjC public operator fun Double.times(other: ScalarExpr): ScalarExpr = ScalarExpr.const(this) * other
@HiddenFromObjC public operator fun Double.div(other: ScalarExpr): ScalarExpr = ScalarExpr.const(this) / other

// ---- Animatable & insettable custom shapes -----------------------------------------------------

/** Symbolic accessors for the `CGRect` passed to `Shape.path(in:)` (already inset by `inset(by:)`). */
@HiddenFromObjC
public class RectExpr internal constructor() {
    public val minX: ScalarExpr = ScalarExpr.arg(0)
    public val minY: ScalarExpr = ScalarExpr.arg(1)
    public val width: ScalarExpr = ScalarExpr.arg(2)
    public val height: ScalarExpr = ScalarExpr.arg(3)
    public val midX: ScalarExpr = minX + width * 0.5
    public val midY: ScalarExpr = minY + height * 0.5
    public val maxX: ScalarExpr = minX + width
    public val maxY: ScalarExpr = minY + height
    public val minSide: ScalarExpr = ScalarExpr.min(width, height)
}

/**
 * Builds a symbolic path for [AnimatablePathShape] in point coordinates derived from [RectExpr] and the shape's
 * animatable parameters.
 */
@HiddenFromObjC
public class ExprPathBuilder internal constructor() {
    internal val commands = ArrayList<List<Any>>()

    public fun moveTo(x: ScalarExpr, y: ScalarExpr) {
        commands += listOf(0, x.encoded, y.encoded)
    }

    public fun lineTo(x: ScalarExpr, y: ScalarExpr) {
        commands += listOf(1, x.encoded, y.encoded)
    }

    public fun quadTo(cx: ScalarExpr, cy: ScalarExpr, x: ScalarExpr, y: ScalarExpr) {
        commands += listOf(2, cx.encoded, cy.encoded, x.encoded, y.encoded)
    }

    public fun curveTo(
        c1x: ScalarExpr, c1y: ScalarExpr,
        c2x: ScalarExpr, c2y: ScalarExpr,
        x: ScalarExpr, y: ScalarExpr,
    ) {
        commands += listOf(3, c1x.encoded, c1y.encoded, c2x.encoded, c2y.encoded, x.encoded, y.encoded)
    }

    public fun close() {
        commands += listOf(4)
    }

    public fun ellipse(x: ScalarExpr, y: ScalarExpr, width: ScalarExpr, height: ScalarExpr) {
        commands += listOf(5, x.encoded, y.encoded, width.encoded, height.encoded)
    }

    public fun rect(x: ScalarExpr, y: ScalarExpr, width: ScalarExpr, height: ScalarExpr) {
        commands += listOf(6, x.encoded, y.encoded, width.encoded, height.encoded)
    }

    public fun arc(
        centerX: ScalarExpr,
        centerY: ScalarExpr,
        radius: ScalarExpr,
        startDegrees: ScalarExpr,
        endDegrees: ScalarExpr,
        clockwise: Boolean = false,
    ) {
        commands += listOf(
            7, centerX.encoded, centerY.encoded, radius.encoded,
            startDegrees.encoded, endDegrees.encoded, if (clockwise) 1 else 0,
        )
    }

    public fun relativeArc(
        centerX: ScalarExpr,
        centerY: ScalarExpr,
        radius: ScalarExpr,
        startDegrees: ScalarExpr,
        deltaDegrees: ScalarExpr,
    ) {
        commands += listOf(8, centerX.encoded, centerY.encoded, radius.encoded, startDegrees.encoded, deltaDegrees.encoded)
    }

    public fun roundedRect(
        x: ScalarExpr,
        y: ScalarExpr,
        width: ScalarExpr,
        height: ScalarExpr,
        cornerRadius: ScalarExpr,
        style: RoundedCornerStyle = RoundedCornerStyle.continuous,
    ) {
        val code = if (style == RoundedCornerStyle.circular) 0 else 1
        commands += listOf(9, x.encoded, y.encoded, width.encoded, height.encoded, cornerRadius.encoded, code)
    }
}

/**
 * A custom SwiftUI `InsettableShape` & `Animatable` whose path is defined by symbolic [ScalarExpr] expressions over
 * the shape's [RectExpr] and numeric [parameters].
 *
 * SwiftUI interpolates [parameters] directly via `Animatable.animatableData` on every display refresh without
 * calling back into Kotlin, and supports `inset(by:)`, `background(in:)`, `containerShape`, and `strokeBorder`.
 */
@HiddenFromObjC
public class AnimatablePathShape internal constructor(
    parameters: List<Double>,
    commands: List<List<Any>>,
) : SExpr(ExpressionIds.ANIMATABLE_PATH_SHAPE, listOf(parameters, commands)), InsettableShape {

    public constructor(
        parameters: List<Double>,
        build: ExprPathBuilder.(rect: RectExpr, params: List<ScalarExpr>) -> Unit,
    ) : this(
        parameters = parameters.toList(),
        commands = ExprPathBuilder().apply {
            val rect = RectExpr()
            val paramExprs = List(parameters.size) { ScalarExpr.param(it) }
            build(rect, paramExprs)
        }.commands.toList(),
    )

    public constructor(
        vararg parameters: Double,
        build: ExprPathBuilder.(rect: RectExpr, params: List<ScalarExpr>) -> Unit,
    ) : this(parameters.toList(), build)
}

// ---- Swift-evaluated visualEffect and scrollTransition -----------------------------------------

/** Symbolic visual effects evaluated natively in Swift by [visualEffectExpr] and [scrollTransitionExpr]. */
@HiddenFromObjC
public data class VisualEffectsExpr(
    val opacity: ScalarExpr = ScalarExpr.const(1.0),
    val scale: ScalarExpr = ScalarExpr.const(1.0),
    val offsetX: ScalarExpr = ScalarExpr.const(0.0),
    val offsetY: ScalarExpr = ScalarExpr.const(0.0),
    val rotationDegrees: ScalarExpr = ScalarExpr.const(0.0),
    val blurRadius: ScalarExpr = ScalarExpr.const(0.0),
) {
    internal fun encoded(): List<Any> = listOf(
        opacity.encoded,
        scale.encoded,
        offsetX.encoded,
        offsetY.encoded,
        rotationDegrees.encoded,
        blurRadius.encoded,
    )
}

/** Symbolic view geometry in global coordinates for [visualEffectExpr]. */
@HiddenFromObjC
public class EffectGeometryExpr internal constructor() {
    public val width: ScalarExpr = ScalarExpr.arg(0)
    public val height: ScalarExpr = ScalarExpr.arg(1)
    public val minX: ScalarExpr = ScalarExpr.arg(2)
    public val minY: ScalarExpr = ScalarExpr.arg(3)
}

/**
 * SwiftUI `visualEffect { content, geometry in }` evaluated entirely on the Swift side from symbolic [ScalarExpr]
 * expressions, avoiding Kotlin callbacks during scroll and layout updates.
 */
@HiddenFromObjC
public fun Modifier.visualEffectExpr(
    effect: (EffectGeometryExpr) -> VisualEffectsExpr,
): Modifier = element(ExpressionIds.VISUAL_EFFECT_EXPR, effect(EffectGeometryExpr()).encoded())

/**
 * SwiftUI `scrollTransition(_:axis:transition:)` evaluated entirely on the Swift side from symbolic [ScalarExpr]
 * expressions over `phase` (`-1.0` top/leading, `0.0` identity, `1.0` bottom/trailing), avoiding per-frame bridge
 * crossings while scrolling.
 */
@HiddenFromObjC
public fun Modifier.scrollTransitionExpr(
    configuration: ScrollTransitionConfiguration = ScrollTransitionConfiguration.interactive,
    axis: Axis? = null,
    transition: (phase: ScalarExpr) -> VisualEffectsExpr,
): Modifier = element(
    ExpressionIds.SCROLL_TRANSITION_EXPR,
    configuration,
    axis,
    transition(ScalarExpr.arg(0)).encoded(),
)
