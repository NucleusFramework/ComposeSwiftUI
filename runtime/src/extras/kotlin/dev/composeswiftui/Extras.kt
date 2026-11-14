@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// Hand-written APIs for SwiftUI features that have no direct generated equivalent.
// View kinds 100_002…, modifier ids 100_001…, value ids 5… (see Handwritten.swift).

internal object ExtraIds {
    const val NAV_PATH = 100_002
    const val ENV_READER = 100_003
    const val NAMESPACE_SCOPE = 100_004
    const val CUSTOM_LAYOUT = 100_005
    const val NATIVE_VIEW = 100_006

    const val FOCUSED = 100_001
    const val DRAG = 100_002
    const val MAGNIFY = 100_003
    const val ROTATE = 100_004
    const val SPATIAL_TAP = 100_005
    const val MATCHED_GEOMETRY = 100_006
    const val MATCHED_TRANSITION_SOURCE = 100_007
    const val ZOOM_TRANSITION = 100_008
}

// ---- data-driven containers ---------------------------------------------------------------------

/** SwiftUI `ForEach(items, id:)`: one keyed child group per item (identity = [key]). */
@HiddenFromObjC
@Composable
public fun <T> ForEach(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    for (item in items) key(key(item)) { content(item) }
}

/**
 * SwiftUI `NavigationStack(path:)` with `navigationDestination(for:)`, driven by Kotlin state:
 * push by appending to [path], SwiftUI back navigation calls [onPathChange] with the truncated path.
 */
@HiddenFromObjC
@Composable
public fun <T : Any> NavigationStack(
    path: List<T>,
    onPathChange: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    destination: @Composable (T) -> Unit,
    root: @Composable () -> Unit,
) {
    val slots: List<@Composable () -> Unit> = path.map { value -> { key(value) { destination(value) } } }
    SwiftView(
        ExtraIds.NAV_PATH,
        listOf(path.size, SValueCallback { count -> onPathChange(path.take((count as Number).toInt())) }),
        modifier,
        slots = slots,
        content = root,
    )
}

// ---- item-driven presentations -----------------------------------------------------------------

/** SwiftUI `sheet(item:)`: presented while [item] is non-null. */
@HiddenFromObjC
public fun <T : Any> Modifier.sheet(item: T?, onDismiss: () -> Unit, content: @Composable (T) -> Unit): Modifier =
    sheet(isPresented = item != null, onIsPresentedChange = { if (!it) onDismiss() }, content = { if (item != null) content(item) })

/** SwiftUI `fullScreenCover(item:)` (iOS / tvOS / watchOS). */
@HiddenFromObjC
public fun <T : Any> Modifier.fullScreenCover(item: T?, onDismiss: () -> Unit, content: @Composable (T) -> Unit): Modifier =
    fullScreenCover(isPresented = item != null, onIsPresentedChange = { if (!it) onDismiss() }, content = { if (item != null) content(item) })

/** SwiftUI `popover(item:)`. */
@HiddenFromObjC
public fun <T : Any> Modifier.popover(item: T?, onDismiss: () -> Unit, content: @Composable (T) -> Unit): Modifier =
    popover(isPresented = item != null, onIsPresentedChange = { if (!it) onDismiss() }, content = { if (item != null) content(item) })

/** SwiftUI `alert(_:isPresented:presenting:actions:message:)`. */
@HiddenFromObjC
public fun <T : Any> Modifier.alert(
    title: (T) -> String,
    item: T?,
    onDismiss: () -> Unit,
    actions: @Composable (T) -> Unit,
    message: @Composable (T) -> Unit,
): Modifier = alert(
    title = item?.let(title) ?: "",
    isPresented = item != null,
    onIsPresentedChange = { if (!it) onDismiss() },
    actions = { if (item != null) actions(item) },
    message = { if (item != null) message(item) },
)

/** SwiftUI `confirmationDialog(_:isPresented:titleVisibility:presenting:actions:)`. */
@HiddenFromObjC
public fun <T : Any> Modifier.confirmationDialog(
    title: (T) -> String,
    item: T?,
    onDismiss: () -> Unit,
    titleVisibility: Visibility = Visibility.automatic,
    actions: @Composable (T) -> Unit,
): Modifier = confirmationDialog(
    title = item?.let(title) ?: "",
    isPresented = item != null,
    onIsPresentedChange = { if (!it) onDismiss() },
    titleVisibility = titleVisibility,
    actions = { if (item != null) actions(item) },
)

// ---- environment ------------------------------------------------------------------------------

/** SwiftUI environment values read at the position of an [EnvironmentReader]. */
public data class SwiftUIEnvironment(
    val colorScheme: String = "light",
    val dynamicTypeSize: String = "large",
    val horizontalSizeClass: String? = null,
    val verticalSizeClass: String? = null,
    val locale: String = "",
    val layoutDirection: String = "leftToRight",
    val isEnabled: Boolean = true,
    val isPresented: Boolean = false,
    val dismiss: () -> Unit = {},
) {
    val isDark: Boolean get() = colorScheme == "dark"
}

/**
 * Reads SwiftUI `@Environment` values (color scheme, dynamic type, size classes, locale, layout
 * direction, enabled/presented state, `dismiss`) where it is placed. Values arrive after the first
 * layout pass (the first composition sees defaults).
 */
@HiddenFromObjC
@Composable
public fun EnvironmentReader(modifier: Modifier = Modifier, content: @Composable (SwiftUIEnvironment) -> Unit) {
    var env by remember { mutableStateOf(SwiftUIEnvironment()) }
    SwiftView(
        ExtraIds.ENV_READER,
        listOf(SValueCallback { value ->
            val v = value as List<*>
            val dismiss = v[8] as? SwiftAction
            val new = SwiftUIEnvironment(
                colorScheme = v[0] as String,
                dynamicTypeSize = v[1] as String,
                horizontalSizeClass = v[2] as? String,
                verticalSizeClass = v[3] as? String,
                locale = v[4] as String,
                layoutDirection = v[5] as String,
                isEnabled = v[6] as Boolean,
                isPresented = v[7] as Boolean,
                dismiss = { dismiss?.run() },
            )
            if (new.copy(dismiss = env.dismiss) != env) env = new
        }),
        modifier,
    ) { content(env) }
}

// ---- focus & gestures -------------------------------------------------------------------------

/** SwiftUI `focused(_:)` with a Kotlin-owned boolean (bridged to a `@FocusState` in Swift). */
@HiddenFromObjC
public fun Modifier.focused(isFocused: Boolean, onFocusChange: (Boolean) -> Unit): Modifier =
    element(ExtraIds.FOCUSED, isFocused, SValueCallback { onFocusChange(it as Boolean) })

/** Values of a SwiftUI `DragGesture`. */
public data class DragGestureValue(
    val translationX: Double,
    val translationY: Double,
    val locationX: Double,
    val locationY: Double,
    val startLocationX: Double,
    val startLocationY: Double,
    val predictedEndTranslationX: Double,
    val predictedEndTranslationY: Double,
    val velocityX: Double,
    val velocityY: Double,
)

private fun dragValue(value: Any?): DragGestureValue {
    val v = (value as List<*>).map { (it as Number).toDouble() }
    return DragGestureValue(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9])
}

/** SwiftUI `.gesture(DragGesture(minimumDistance:).onChanged { }.onEnded { })`. */
@HiddenFromObjC
public fun Modifier.dragGesture(
    minimumDistance: Double = 10.0,
    simultaneous: Boolean = false,
    onChanged: (DragGestureValue) -> Unit = {},
    onEnded: (DragGestureValue) -> Unit = {},
): Modifier = element(
    ExtraIds.DRAG, minimumDistance, simultaneous,
    SValueCallback { onChanged(dragValue(it)) }, SValueCallback { onEnded(dragValue(it)) },
)

/** SwiftUI `MagnifyGesture`: [onChanged] / [onEnded] receive the magnification factor. */
@HiddenFromObjC
public fun Modifier.magnifyGesture(
    simultaneous: Boolean = false,
    onChanged: (Double) -> Unit = {},
    onEnded: (Double) -> Unit = {},
): Modifier = element(
    ExtraIds.MAGNIFY, simultaneous,
    SValueCallback { onChanged((it as Number).toDouble()) }, SValueCallback { onEnded((it as Number).toDouble()) },
)

/** SwiftUI `RotateGesture`: [onChanged] / [onEnded] receive the rotation in degrees. */
@HiddenFromObjC
public fun Modifier.rotateGesture(
    simultaneous: Boolean = false,
    onChanged: (Double) -> Unit = {},
    onEnded: (Double) -> Unit = {},
): Modifier = element(
    ExtraIds.ROTATE, simultaneous,
    SValueCallback { onChanged((it as Number).toDouble()) }, SValueCallback { onEnded((it as Number).toDouble()) },
)

/** SwiftUI `SpatialTapGesture`: [action] receives the tap location (x, y) in the view. */
@HiddenFromObjC
public fun Modifier.onTapGesture(count: Int = 1, action: (x: Double, y: Double) -> Unit): Modifier =
    element(ExtraIds.SPATIAL_TAP, count, SValueCallback { value ->
        val v = (value as List<*>).map { (it as Number).toDouble() }
        action(v[0], v[1])
    })

// ---- namespaces & animations -------------------------------------------------------------------

/** A SwiftUI `@Namespace`, created by [NamespaceScope]. */
public class NamespaceId internal constructor(internal val id: Long)

private var nextNamespace = 1L

/** Declares a SwiftUI `@Namespace` for [content] (matched geometry, zoom transitions). */
@HiddenFromObjC
@Composable
public fun NamespaceScope(modifier: Modifier = Modifier, content: @Composable (NamespaceId) -> Unit) {
    val ns = remember { NamespaceId(nextNamespace++) }
    SwiftView(ExtraIds.NAMESPACE_SCOPE, listOf(ns.id), modifier) { content(ns) }
}

/** SwiftUI `matchedGeometryEffect(id:in:properties:anchor:isSource:)`. */
@HiddenFromObjC
public fun Modifier.matchedGeometryEffect(
    id: Any,
    namespace: NamespaceId,
    properties: MatchedGeometryProperties = MatchedGeometryProperties.frame,
    anchor: UnitPoint = UnitPoint.center,
    isSource: Boolean = true,
): Modifier = element(ExtraIds.MATCHED_GEOMETRY, id, namespace.id, properties, anchor, isSource)

/** SwiftUI `matchedTransitionSource(id:in:)` (iOS 18+ / macOS 15+). */
@HiddenFromObjC
public fun Modifier.matchedTransitionSource(id: Any, namespace: NamespaceId): Modifier =
    element(ExtraIds.MATCHED_TRANSITION_SOURCE, id, namespace.id)

/** SwiftUI `navigationTransition(.zoom(sourceID:in:))` (iOS 18+). */
@HiddenFromObjC
public fun Modifier.zoomTransition(sourceId: Any, namespace: NamespaceId): Modifier =
    element(ExtraIds.ZOOM_TRANSITION, sourceId, namespace.id)

/**
 * SwiftUI `withAnimation(animation) { }`: state changes made in [block] (and the resulting
 * recomposition) are applied inside one animated SwiftUI transaction.
 */
@HiddenFromObjC
public fun <R> withAnimation(animation: Animation? = Animation.default, block: () -> R): R {
    val runner = SwiftUIBridge.animationRunner ?: return block()
    var result: Any? = null
    runner(animation) { result = block() }
    @Suppress("UNCHECKED_CAST")
    return result as R
}

// ---- custom layout, shapes and native views ----------------------------------------------------

/** A SwiftUI `Layout` implemented in Kotlin by [policy]. */
@HiddenFromObjC
@Composable
public fun CustomLayout(policy: LayoutPolicy, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SwiftView(ExtraIds.CUSTOM_LAYOUT, listOf(policy), modifier, content = content)
}

/** Builds a [PathShape] in unit coordinates (0…1 on each axis, scaled to the shape's frame). */
public class PathBuilder internal constructor() {
    internal val commands = ArrayList<Double>()

    public fun moveTo(x: Double, y: Double) { commands += listOf(0.0, x, y) }
    public fun lineTo(x: Double, y: Double) { commands += listOf(1.0, x, y) }
    public fun quadTo(cx: Double, cy: Double, x: Double, y: Double) { commands += listOf(2.0, cx, cy, x, y) }
    public fun curveTo(c1x: Double, c1y: Double, c2x: Double, c2y: Double, x: Double, y: Double) {
        commands += listOf(3.0, c1x, c1y, c2x, c2y, x, y)
    }
    public fun close() { commands += 4.0 }
    public fun ellipse(x: Double, y: Double, width: Double, height: Double) { commands += listOf(5.0, x, y, width, height) }
    public fun rect(x: Double, y: Double, width: Double, height: Double) { commands += listOf(6.0, x, y, width, height) }
}

/**
 * A custom SwiftUI `Shape` drawn from Kotlin path commands in unit coordinates; usable everywhere a
 * `Shape` is (ShapeView, clipShape, background(in:), contentShape…).
 */
@HiddenFromObjC
public class PathShape internal constructor(commands: List<Double>) : SExpr(5, listOf(commands)), Shape {
    public constructor(build: PathBuilder.() -> Unit) : this(PathBuilder().apply(build).commands.toList())
}

/** Renders a Swift view registered with `ComposeRegistry.nativeViews[key]` (MapKit, UIKit wrappers…). */
@HiddenFromObjC
@Composable
public fun NativeView(key: String, modifier: Modifier = Modifier, vararg args: Any?) {
    SwiftView(ExtraIds.NATIVE_VIEW, listOf(key) + args.toList(), modifier)
}
