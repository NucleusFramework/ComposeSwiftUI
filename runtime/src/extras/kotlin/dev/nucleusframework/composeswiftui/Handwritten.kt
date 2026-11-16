@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// Hand-written bindings complementing the generated ones. Ids >= 100_000 are reserved for them.

/** CoreGraphics `CGPoint`. */
@HiddenFromObjC
public class CGPoint(public val x: Double, public val y: Double) : SExpr(1, listOf(x, y))

/** CoreGraphics `CGSize`. */
@HiddenFromObjC
public class CGSize(public val width: Double, public val height: Double) : SExpr(2, listOf(width, height))

/** CoreGraphics `CGRect`. */
@HiddenFromObjC
public class CGRect(public val x: Double, public val y: Double, public val width: Double, public val height: Double) :
    SExpr(3, listOf(x, y, width, height))

/** CoreGraphics `CGVector`. */
@HiddenFromObjC
public class CGVector(public val dx: Double, public val dy: Double) : SExpr(4, listOf(dx, dy))

/** CoreGraphics `CGAffineTransform`: `x' = a*x + c*y + tx`, `y' = b*x + d*y + ty`. */
@HiddenFromObjC
public class CGAffineTransform(
    public val a: Double = 1.0,
    public val b: Double = 0.0,
    public val c: Double = 0.0,
    public val d: Double = 1.0,
    public val tx: Double = 0.0,
    public val ty: Double = 0.0,
) : SExpr(6, listOf(a, b, c, d, tx, ty))

/** Rebuilds a CoreGraphics value sent by Swift as a plain [SExpr] (bindings and callbacks of value types). */
internal fun cgValue(e: SExpr): Any? {
    val d = e.args.map { (it as? Number)?.toDouble() ?: 0.0 }
    return when (e.id) {
        1 -> CGPoint(d[0], d[1])
        2 -> CGSize(d[0], d[1])
        3 -> CGRect(d[0], d[1], d[2], d[3])
        4 -> CGVector(d[0], d[1])
        6 -> CGAffineTransform(d[0], d[1], d[2], d[3], d[4], d[5])
        else -> null
    }
}

internal object HandwrittenIds {
    const val SHAPE_VIEW = 100_000
    const val GEOMETRY_READER = 100_001
    const val TOOLBAR_ITEM = 100_000
}

/**
 * Renders any SwiftUI [Shape] (`Circle()`, `RoundedRectangle(cornerRadius = 12.0)`...), optionally
 * filled and/or stroked: `Circle().fill(.red)` == `ShapeView(Circle(), fill = Color.red)`.
 */
@HiddenFromObjC
@Composable
public fun ShapeView(
    shape: Shape,
    modifier: Modifier = Modifier,
    fill: ShapeStyle? = null,
    stroke: ShapeStyle? = null,
    lineWidth: Double = 1.0,
    strokeStyle: StrokeStyle? = null,
    trimFrom: Double = 0.0,
    trimTo: Double = 1.0,
) {
    SwiftView(HandwrittenIds.SHAPE_VIEW, listOf(shape, fill, stroke, lineWidth, strokeStyle, trimFrom, trimTo), modifier)
}

/**
 * SwiftUI `GeometryReader`: [content] receives the proposed size. The size is reported by SwiftUI after layout,
 * so the first composition sees `CGSize(0.0, 0.0)` (like a two-pass layout).
 */
@HiddenFromObjC
@Composable
public fun GeometryReader(
    modifier: Modifier = Modifier,
    content: @Composable (CGSize) -> Unit,
) {
    var size by remember { mutableStateOf(CGSize(0.0, 0.0)) }
    SwiftView(
        HandwrittenIds.GEOMETRY_READER,
        listOf(SValueCallback { value ->
            val wh = value as List<*>
            val new = CGSize((wh[0] as Number).toDouble(), (wh[1] as Number).toDouble())
            if (new.width != size.width || new.height != size.height) size = new
        }),
        modifier,
    ) { content(size) }
}

/**
 * SwiftUI `.toolbar { ToolbarItem(placement:) { content }.sharedBackgroundVisibility(_:) }`. Can be chained several times.
 * [sharedBackgroundVisibility] (iOS/macOS 26+, ignored elsewhere) hides the Liquid Glass capsule behind the item.
 */
@HiddenFromObjC
public fun Modifier.toolbarItem(
    placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic,
    sharedBackgroundVisibility: Visibility = Visibility.automatic,
    content: @Composable () -> Unit,
): Modifier = element(HandwrittenIds.TOOLBAR_ITEM, placement, slotContent(content), sharedBackgroundVisibility)
