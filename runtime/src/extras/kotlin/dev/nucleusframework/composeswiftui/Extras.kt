@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

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
    const val STYLED_TEXT = 100_007
    const val TAB_VIEW = 100_008
    const val TAB = 100_009
    const val SCALED_METRIC = 100_066

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
public fun <T> ForEach(
    items: List<T>,
    key: (T) -> Any,
    onDelete: ((indices: Set<Int>) -> Unit)? = null,
    onMove: ((indices: Set<Int>, destination: Int) -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    if (onDelete == null && onMove == null) {
        for (item in items) key(key(item)) { content(item) }
        return
    }
    // SwiftUI `ForEach(data).onDelete(perform:).onMove(perform:)`: swipe to delete and drag to reorder in a List
    SwiftView(
        CompletionIds.EDITABLE_FOR_EACH,
        listOf(
            onDelete?.let { cb -> SValueCallback { cb((it as List<*>).map { i -> (i as Number).toInt() }.toSet()) } },
            onMove?.let { cb ->
                SValueCallback {
                    val l = it as List<*>
                    cb((l[0] as List<*>).map { i -> (i as Number).toInt() }.toSet(), (l[1] as Number).toInt())
                }
            },
        ),
        Modifier,
    ) {
        for (item in items) key(key(item)) { SwiftView(InteractiveIds.GROUP, emptyList(), Modifier) { content(item) } }
    }
}

/**
 * SwiftUI `NavigationStack(path:)` with `navigationDestination(for:)`, driven by Kotlin state:
 * push by appending to [path], SwiftUI back navigation calls [onPathChange] with the truncated path. A
 * `NavigationLink(value = v) { … }` inside the stack pushes `v` (it must be a [T]): [onPathChange] receives `path + v`.
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
        listOf(path.size, SValueCallback { v ->
            @Suppress("UNCHECKED_CAST")
            if (v is List<*>) onPathChange(path + (v[0] as T)) else onPathChange(path.take((v as Number).toInt()))
        }),
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
    val colorScheme: ColorScheme = ColorScheme.light,
    val dynamicTypeSize: DynamicTypeSize = DynamicTypeSize.large,
    val horizontalSizeClass: UserInterfaceSizeClass? = null,
    val verticalSizeClass: UserInterfaceSizeClass? = null,
    val locale: String = "",
    val layoutDirection: LayoutDirection = LayoutDirection.leftToRight,
    val isEnabled: Boolean = true,
    val isPresented: Boolean = false,
    val dismiss: () -> Unit = {},
    /** `scenePhase`: active, inactive or background. */
    val scenePhase: ScenePhase = ScenePhase.active,
    /** `accessibilityReduceMotion`. */
    val reduceMotion: Boolean = false,
    /** `accessibilityReduceTransparency`. */
    val reduceTransparency: Boolean = false,
    /** `accessibilityDifferentiateWithoutColor`. */
    val differentiateWithoutColor: Boolean = false,
    /** `accessibilityInvertColors`. */
    val invertColors: Boolean = false,
    /** `accessibilityVoiceOverEnabled`. */
    val voiceOverEnabled: Boolean = false,
    /** `legibilityWeight == .bold` (Bold Text). */
    val boldText: Boolean = false,
    /** `isSearching`: a `searchable` above this reader is being edited. */
    val isSearching: Boolean = false,
    /** `colorSchemeContrast == .increased` (Increase Contrast). */
    val increasedContrast: Boolean = false,
    /** `supportsMultipleWindows` (iPad, Mac). */
    val supportsMultipleWindows: Boolean = false,
    /** The environment's actions (`openURL`, `openWindow`…), equal when the same ones are available. */
    val actions: SwiftUIEnvironmentActions = SwiftUIEnvironmentActions(),
    /** `displayScale`: pixels per point. */
    val displayScale: Double = 2.0,
) {
    val isDark: Boolean get() = colorScheme == ColorScheme.dark
}

/**
 * The SwiftUI environment actions available where an [EnvironmentReader] is placed. Those an OS doesn't have do
 * nothing (`openWindow` on watchOS, `openSettings` outside macOS…).
 */
public class SwiftUIEnvironmentActions internal constructor(private val raw: List<Any?> = emptyList()) {
    private fun fn(i: Int) = raw.getOrNull(i) as? KotlinFunction
    private fun action(i: Int) = raw.getOrNull(i) as? SwiftAction

    /** `openURL(_:)`: opens [url] with the system (or a universal link's app). */
    public fun openURL(url: String) { fn(0)?.invoke(listOf(url)) }

    /** `openWindow(id:)` / `openWindow(value:)`: opens the `WindowGroup` with [id], or the one presenting a `String` [value]. */
    public fun openWindow(id: String? = null, value: String? = null) { fn(1)?.invoke(listOf(id, value)) }

    /** `dismissWindow()` / `dismissWindow(id:)`. */
    public fun dismissWindow(id: String? = null) { fn(2)?.invoke(listOf(id)) }

    /** `openSettings()` (macOS 14): shows the app's `Settings` scene. */
    public fun openSettings() { action(3)?.run() }

    /** `dismissSearch()`: ends the search of the enclosing `searchable`. */
    public fun dismissSearch() { action(4)?.run() }

    /** `refresh()`: runs the enclosing `refreshable` action (no-op without one). */
    public fun refresh() { action(5)?.run() }

    /** `rename()`: starts renaming the enclosing `renameAction` target. */
    public fun rename() { action(6)?.run() }

    /** True when the enclosing view has a `refreshable` action. */
    public val canRefresh: Boolean get() = raw.getOrNull(5) != null

    // Equal when the same actions are available: new closures alone don't recompose the reader's content.
    private val available get() = raw.map { it != null }
    override fun equals(other: Any?): Boolean = other is SwiftUIEnvironmentActions && other.available == available
    override fun hashCode(): Int = available.hashCode()
}

/**
 * Reads SwiftUI `@Environment` values (color scheme, dynamic type, size classes, locale, layout
 * direction, enabled/presented state, `dismiss`, scene phase, accessibility preferences, search state) and its
 * actions ([SwiftUIEnvironment.actions]: `openURL`, `openWindow`, `dismissWindow`, `openSettings`, `dismissSearch`,
 * `refresh`, `rename`) where it is placed. The first composition sees [currentSwiftUIEnvironment] (the environment
 * around the `ComposeView`); the values of this position arrive after the first layout pass.
 */
@HiddenFromObjC
@Composable
public fun EnvironmentReader(modifier: Modifier = Modifier, content: @Composable (SwiftUIEnvironment) -> Unit) {
    // The host's environment until this position reports its own: no flash of the defaults (light mode…)
    val initial = currentSwiftUIEnvironment
    var env by remember { mutableStateOf(initial) }
    SwiftView(
        ExtraIds.ENV_READER,
        listOf(SValueCallback { value ->
            val new = swiftUIEnvironmentOf(value as List<*>)
            if (new.copy(dismiss = env.dismiss) != env) env = new
        }),
        modifier,
    ) { content(env) }
}

/**
 * The SwiftUI environment around the `ComposeView` of this composition, known from the first composition (Swift
 * passes it before composing), so a dark-mode screen never starts light. [EnvironmentReader] reads it at a position.
 */
public val currentSwiftUIEnvironment: SwiftUIEnvironment
    @Composable get() {
        val raw = LocalHostEnvironment.current
        return remember(raw) { raw?.let(::swiftUIEnvironmentOf) ?: SwiftUIEnvironment() }
    }

/** Decodes the values Swift reports (`EnvironmentReporter` in Handwritten.swift). */
internal fun swiftUIEnvironmentOf(v: List<*>): SwiftUIEnvironment {
    val dismiss = v[8] as? SwiftAction
    return SwiftUIEnvironment(
        colorScheme = if (v[0] == "dark") ColorScheme.dark else ColorScheme.light,
        dynamicTypeSize = dynamicTypeSizeOf(v[1] as String),
        horizontalSizeClass = sizeClassOf(v[2] as? String),
        verticalSizeClass = sizeClassOf(v[3] as? String),
        locale = v[4] as String,
        layoutDirection = if (v[5] == "rightToLeft") LayoutDirection.rightToLeft else LayoutDirection.leftToRight,
        isEnabled = v[6] as Boolean,
        isPresented = v[7] as Boolean,
        dismiss = { dismiss?.run() },
        scenePhase = when (v[9]) { "background" -> ScenePhase.background; "inactive" -> ScenePhase.inactive; else -> ScenePhase.active },
        reduceMotion = v[10] == true,
        reduceTransparency = v[11] == true,
        differentiateWithoutColor = v[12] == true,
        invertColors = v[13] == true,
        voiceOverEnabled = v[14] == true,
        boldText = v[15] == true,
        isSearching = v.getOrNull(16) == true,
        increasedContrast = v.getOrNull(17) == true,
        supportsMultipleWindows = v.getOrNull(18) == true,
        // [19] and [20] (refresh / rename available) only make Swift report when they change
        actions = SwiftUIEnvironmentActions((v.getOrNull(21) as? List<*>).orEmpty()),
        displayScale = (v.getOrNull(22) as? Number)?.toDouble() ?: 2.0,
    )
}

// Swift sends `String(describing:)` of the case; unknown future cases fall back to the default.
private fun dynamicTypeSizeOf(name: String): DynamicTypeSize = when (name) {
    "xSmall" -> DynamicTypeSize.xSmall
    "small" -> DynamicTypeSize.small
    "medium" -> DynamicTypeSize.medium
    "xLarge" -> DynamicTypeSize.xLarge
    "xxLarge" -> DynamicTypeSize.xxLarge
    "xxxLarge" -> DynamicTypeSize.xxxLarge
    "accessibility1" -> DynamicTypeSize.accessibility1
    "accessibility2" -> DynamicTypeSize.accessibility2
    "accessibility3" -> DynamicTypeSize.accessibility3
    "accessibility4" -> DynamicTypeSize.accessibility4
    "accessibility5" -> DynamicTypeSize.accessibility5
    else -> DynamicTypeSize.large
}

private fun sizeClassOf(name: String?): UserInterfaceSizeClass? = when (name) {
    "compact" -> UserInterfaceSizeClass.compact
    "regular" -> UserInterfaceSizeClass.regular
    else -> null
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

internal fun dragValue(value: Any?): DragGestureValue {
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
public fun withAnimation(animation: Animation? = Animation.default, completion: () -> Unit, block: () -> Unit) {
    // SwiftUI `withAnimation(_:completionCriteria:_:completion:)`
    val runner = SwiftUIBridge.animationCompletionRunner ?: run { block(); completion(); return }
    runner(animation, block, completion)
}

/** SwiftUI `withAnimation(_:_:)`. */
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
    SwiftView(ExtraIds.CUSTOM_LAYOUT, listOf(GuardedLayoutPolicy(policy)), modifier, content = content)
}

/** Reports an exception of the app's layout instead of letting it reach Swift (see [ComposeSwiftUI.onError]). */
private data class GuardedLayoutPolicy(val policy: LayoutPolicy) : LayoutPolicy {
    override fun sizeThatFits(proposedWidth: Double?, proposedHeight: Double?, subviews: List<LayoutSize>): LayoutSize =
        guarded(ComposeSwiftUIErrorSource.Callback, LayoutSize(0.0, 0.0)) { policy.sizeThatFits(proposedWidth, proposedHeight, subviews) }

    override fun placeSubviews(width: Double, height: Double, subviews: List<LayoutSize>): List<LayoutPlacement> =
        guarded(ComposeSwiftUIErrorSource.Callback, emptyList()) { policy.placeSubviews(width, height, subviews) }
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

    /**
     * SwiftUI `Path.addArc(center:radius:startAngle:endAngle:clockwise:)`. Angles are in degrees; [radius] is a
     * fraction of the shorter side of the frame.
     */
    public fun arc(centerX: Double, centerY: Double, radius: Double, startDegrees: Double, endDegrees: Double, clockwise: Boolean) {
        commands += listOf(7.0, centerX, centerY, radius, startDegrees, endDegrees, if (clockwise) 1.0 else 0.0)
    }

    /** SwiftUI `Path.addRelativeArc(center:radius:startAngle:delta:)` (degrees, radius as in [arc]). */
    public fun relativeArc(centerX: Double, centerY: Double, radius: Double, startDegrees: Double, deltaDegrees: Double) {
        commands += listOf(8.0, centerX, centerY, radius, startDegrees, deltaDegrees)
    }

    /** SwiftUI `Path.addArc(tangent1End:tangent2End:radius:)` (radius as in [arc]). */
    public fun tangentArc(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double) {
        commands += listOf(9.0, x1, y1, x2, y2, radius)
    }

    /**
     * SwiftUI `Path.addRoundedRect(in:cornerSize:style:)` with equal corners ([cornerRadius] as a fraction of the
     * shorter side of the frame).
     */
    public fun roundedRect(x: Double, y: Double, width: Double, height: Double, cornerRadius: Double, style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        commands += listOf(10.0, x, y, width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius, styleCode(style))
    }

    /** SwiftUI `Path.addRoundedRect(in:cornerRadii:style:)` (radii as in [roundedRect]). */
    public fun roundedRect(
        x: Double, y: Double, width: Double, height: Double,
        topLeading: Double, bottomLeading: Double, bottomTrailing: Double, topTrailing: Double,
        style: RoundedCornerStyle = RoundedCornerStyle.continuous,
    ) {
        commands += listOf(10.0, x, y, width, height, topLeading, bottomLeading, bottomTrailing, topTrailing, styleCode(style))
    }

    /** SwiftUI `Path.addLines(_:)`: a polyline through [points] (x, y pairs). */
    public fun lines(vararg points: Pair<Double, Double>) {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    }

    /** SwiftUI `Path.addRects(_:)`. */
    public fun rects(vararg rects: List<Double>) {
        rects.forEach { (x, y, w, h) -> rect(x, y, w, h) }
    }

    /** SwiftUI `Path.addPath(_:)`: appends the commands of another path. */
    public fun path(build: PathBuilder.() -> Unit) {
        commands += PathBuilder().apply(build).commands
    }

    private fun styleCode(style: RoundedCornerStyle) = if (style == RoundedCornerStyle.circular) 0.0 else 1.0
}

/**
 * A custom SwiftUI `Shape` drawn from Kotlin path commands in unit coordinates; usable everywhere a
 * `Shape` is (ShapeView, clipShape, background(in:), contentShape…).
 */
@HiddenFromObjC
public class PathShape internal constructor(commands: List<Double>) : SExpr(5, listOf(commands)), Shape {
    public constructor(build: PathBuilder.() -> Unit) : this(PathBuilder().apply(build).commands.toList())
}

/**
 * A Kotlin action to pass in a [NativeView]'s `args`: the Swift renderer gets it with `Args.action(i)`, which runs it
 * and applies the resulting Compose changes. It is updated in place on recomposition, like the built-in callbacks.
 */
@HiddenFromObjC
public fun nativeAction(action: () -> Unit): Any = SAction(action)

/**
 * A Kotlin callback to pass in a [NativeView]'s `args`: the Swift renderer gets it with `Args.callback(i)` and calls
 * it with a value (Swift `String`, `Bool` / numbers boxed as `KotlinBoolean` / `KotlinInt`…, arrays as lists; `NSNull`
 * arrives as null).
 */
@HiddenFromObjC
public fun nativeCallback(callback: (Any?) -> Unit): Any = SValueCallback(callback)

/**
 * A Kotlin `suspend` function to pass in a [NativeView]'s `args`: the Swift renderer awaits it with
 * `await kotlinAwait(a.raw(i), [arguments])`, which returns its result (null if it fails or the Swift task is
 * cancelled, which cancels the coroutine).
 */
@HiddenFromObjC
public fun nativeSuspendFunction(function: suspend (args: List<Any?>) -> Any?): Any = SAsyncFunction(function)

/**
 * Renders a Swift view registered with `ComposeRegistry.nativeViews[key]` (MapKit, UIKit wrappers…). Pass actions
 * and callbacks in [args] with [nativeAction], [nativeCallback] and [nativeSuspendFunction].
 */
@HiddenFromObjC
@Composable
public fun NativeView(key: String, modifier: Modifier = Modifier, vararg args: Any?) {
    SwiftView(ExtraIds.NATIVE_VIEW, listOf(key) + args.toList(), modifier)
}

/**
 * Displays a back stack with SwiftUI navigation (push animation, back button, swipe back): the first key is
 * the root, the others are pushed. Works with any observable list, including a Navigation 3 `NavBackStack`
 * or `mutableStateListOf(...)`. Push with `backStack.add(key)`; SwiftUI back navigation removes keys.
 */
@HiddenFromObjC
@Composable
public fun <T : Any> NavDisplay(
    backStack: MutableList<T>,
    modifier: Modifier = Modifier,
    entryProvider: @Composable (T) -> Unit,
) {
    NavigationStack(
        path = backStack.drop(1),
        onPathChange = { path -> while (backStack.size > path.size + 1) backStack.removeAt(backStack.lastIndex) },
        modifier = modifier,
        destination = entryProvider,
    ) {
        backStack.firstOrNull()?.let { entryProvider(it) }
    }
}

// ---- tabs with selection -------------------------------------------------------------------------

/**
 * SwiftUI `TabView(selection:)`: the selected tab is Kotlin state. Each child is a [Tab] whose `value` matches
 * a possible [selection]; tapping a tab calls [onSelectionChange] with that value.
 *
 * With [onCustomizationChange], `tabViewCustomization(_:)` (iOS 18 / macOS 15, iPad sidebar and Mac): users reorder
 * and hide the tabs and sections that have a `customizationId`; the customization travels as its JSON encoding
 * ([customization], null at first), which you persist like any string.
 */
@HiddenFromObjC
@Composable
public fun <T : Any> TabView(
    selection: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    customization: String? = null,
    onCustomizationChange: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    SwiftView(ExtraIds.TAB_VIEW, listOf(selection, SValueCallback { onSelectionChange(it as T) }, customization,
        onCustomizationChange?.let { f -> SValueCallback { f(it as String) } }), modifier, content = content)
}

/**
 * SwiftUI `TabSection(_:content:)` (iOS 18, macOS 15): groups [Tab]s in the sidebar of an adaptable [TabView]
 * (`Modifier.tabViewStyle(TabViewStyle.sidebarAdaptable)`). Every tab needs a [Tab] `value` then.
 */
@HiddenFromObjC
@Composable
public fun TabSection(title: String, modifier: Modifier = Modifier, customizationId: String? = null, content: @Composable () -> Unit) {
    SwiftView(CompletionIds.TAB_SECTION, listOf(title, customizationId), modifier, content = content)
}

/**
 * SwiftUI `Tab(_:systemImage:value:role:)`, for use in a [TabView] or a [TabSection]. [value] is the tag compared
 * with the `TabView` selection; leave it null when the `TabView` has no selection. [role] (`TabRole.search`) needs
 * iOS 18 / macOS 15 / tvOS 18 / watchOS 11; [badge] shows a count or a short text on the tab item; [customizationId]
 * (`customizationID(_:)`, the title by default) identifies the tab in the [TabView]'s customization.
 */
@HiddenFromObjC
@Composable
public fun Tab(
    title: String,
    systemImage: SFSymbol? = null,
    value: Any? = null,
    modifier: Modifier = Modifier,
    role: TabRole? = null,
    badge: String? = null,
    customizationId: String? = null,
    content: @Composable () -> Unit,
) {
    SwiftView(ExtraIds.TAB, listOf(title, systemImage?.name, value, role, badge, customizationId), modifier, content = content)
}

// ---- mixed-style text ----------------------------------------------------------------------------

/** Style of a run of a [StyledString]. Null or default properties inherit from the enclosing style and the `Text`. */
public data class SpanStyle(
    val color: Color? = null,
    val font: Font? = null,
    /** System font size in points, used when [font] is null. */
    val fontSize: Double? = null,
    val fontWeight: Font.Weight? = null,
    val fontDesign: Font.Design? = null,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val monospaced: Boolean = false,
    val baselineOffset: Double = 0.0,
    val kerning: Double = 0.0,
    /** Highlight behind the text (`AttributedString.backgroundColor`). */
    val backgroundColor: Color? = null,
    val tracking: Double = 0.0,
    /** URL the run links to (`AttributedString.link`). */
    val link: String? = null,
    /** Paragraph alignment (`AttributedString.alignment`, iOS 26), used by the rich [TextEditor]. */
    val alignment: AttributedTextAlignment? = null,
    /**
     * Attributes a rich [TextEditor] set that have no property here (a font picked by name, a line height, a writing
     * direction, a Genmoji…): kept as is when the run goes back to the editor. Null for styles built in Kotlin.
     */
    val nativeAttributes: NativeAttributes? = null,
) {
    /** Returns this style with the properties set in [other] taking precedence. */
    public fun merge(other: SpanStyle): SpanStyle = SpanStyle(
        color = other.color ?: color,
        font = other.font ?: font,
        fontSize = other.fontSize ?: fontSize,
        fontWeight = other.fontWeight ?: fontWeight,
        fontDesign = other.fontDesign ?: fontDesign,
        italic = italic || other.italic,
        underline = underline || other.underline,
        strikethrough = strikethrough || other.strikethrough,
        monospaced = monospaced || other.monospaced,
        baselineOffset = if (other.baselineOffset != 0.0) other.baselineOffset else baselineOffset,
        kerning = if (other.kerning != 0.0) other.kerning else kerning,
        backgroundColor = other.backgroundColor ?: backgroundColor,
        tracking = if (other.tracking != 0.0) other.tracking else tracking,
        link = other.link ?: link,
        alignment = other.alignment ?: alignment,
    )
}

/** SwiftUI `AttributedString.TextAlignment`: the alignment of a paragraph in a rich [TextEditor]. */
public enum class AttributedTextAlignment { left, center, right }

/** Opaque handle on the SwiftUI attributes of a run edited in a rich [TextEditor] (see [SpanStyle.nativeAttributes]). */
public class NativeAttributes internal constructor(internal val token: Int) {
    override fun equals(other: Any?): Boolean = other is NativeAttributes && other.token == token
    override fun hashCode(): Int = token
    override fun toString(): String = "NativeAttributes($token)"
}

/** A string made of differently styled runs, shown by `Text(StyledString)`. Build it with [buildStyledString]. */
public class StyledString internal constructor(public val runs: List<Pair<String, SpanStyle>>) {
    public val text: String get() = runs.joinToString("") { it.first }

    public operator fun plus(other: StyledString): StyledString = StyledString(runs + other.runs)

    override fun equals(other: Any?): Boolean = other is StyledString && other.runs == runs
    override fun hashCode(): Int = runs.hashCode()
    override fun toString(): String = text
}

private const val SYMBOL_RUN = "\u0000sf:"

public class StyledStringBuilder internal constructor() {
    private val runs = ArrayList<Pair<String, SpanStyle>>()
    private var current = SpanStyle()

    public fun append(text: String) {
        if (text.isNotEmpty()) runs += text to current
    }

    public fun append(text: String, style: SpanStyle): Unit = withStyle(style) { append(text) }

    /** An SF Symbol inline with the text (SwiftUI `Text(Image(systemName:))`), styled like the text around it. */
    public fun appendSymbol(symbol: SFSymbol) {
        runs += (SYMBOL_RUN + symbol.name) to current
    }

    public fun append(text: StyledString) {
        text.runs.forEach { (s, style) -> runs += s to current.merge(style) }
    }

    public fun <R> withStyle(style: SpanStyle, block: StyledStringBuilder.() -> R): R {
        val previous = current
        current = previous.merge(style)
        try {
            return block()
        } finally {
            current = previous
        }
    }

    internal fun build() = StyledString(runs.toList())
}

/** Builds a [StyledString], like Compose's `buildAnnotatedString`. */
public fun buildStyledString(block: StyledStringBuilder.() -> Unit): StyledString =
    StyledStringBuilder().apply(block).build()

/** A [StyledString] of one run. */
public fun StyledString(text: String, style: SpanStyle = SpanStyle()): StyledString =
    StyledString(if (text.isEmpty()) emptyList() else listOf(text to style))

/**
 * SwiftUI `Text` made of styled runs (the equivalent of concatenating `Text` values). Modifiers such as
 * `font` or `foregroundStyle` apply to the runs that don't set that property themselves.
 */
@HiddenFromObjC
@Composable
public fun Text(text: StyledString, modifier: Modifier = Modifier) {
    SwiftView(ExtraIds.STYLED_TEXT, listOf(text.runs.size) + text.encodeRuns(), modifier)
}

/** Returns a copy where the runs inside [range] (UTF-16 offsets) are restyled by [transform]. */
@HiddenFromObjC
public fun StyledString.transformStyle(range: TextRange, transform: (SpanStyle) -> SpanStyle): StyledString {
    if (range.start >= range.end) return this
    val out = ArrayList<Pair<String, SpanStyle>>(runs.size + 2)
    var pos = 0
    for ((s, style) in runs) {
        val a = (range.start - pos).coerceIn(0, s.length)
        val b = (range.end - pos).coerceIn(0, s.length)
        if (a > 0) out += s.substring(0, a) to style
        if (b > a) out += s.substring(a, b) to transform(style)
        if (b < s.length) out += s.substring(b) to style
        pos += s.length
    }
    return StyledString(out)
}

/** The style of the character at [offset] (UTF-16), or of the last character when [offset] is the end. */
public fun StyledString.styleAt(offset: Int): SpanStyle? {
    var pos = 0
    for ((s, style) in runs) {
        pos += s.length
        if (offset < pos) return style
    }
    return runs.lastOrNull()?.second
}

/** The runs of [range] (UTF-16 offsets), cut at its bounds. */
@HiddenFromObjC
public fun StyledString.subSequence(range: TextRange): StyledString {
    val out = ArrayList<Pair<String, SpanStyle>>()
    var pos = 0
    for ((s, style) in runs) {
        val a = (range.start - pos).coerceIn(0, s.length)
        val b = (range.end - pos).coerceIn(0, s.length)
        if (b > a) out += s.substring(a, b) to style
        pos += s.length
    }
    return StyledString(out)
}

/** Returns a copy where [range] (UTF-16 offsets) is replaced by [replacement]. */
@HiddenFromObjC
public fun StyledString.replace(range: TextRange, replacement: StyledString): StyledString {
    val end = text.length
    return subSequence(TextRange(0, range.start.coerceIn(0, end))) + replacement + subSequence(TextRange(range.end.coerceIn(0, end), end))
}

/**
 * Flat encoding of the runs, [STYLED_RUN_STRIDE] values per run (decoded by the Swift renderers). [changed] gives,
 * for a run carrying [SpanStyle.nativeAttributes], the properties Kotlin changed since the editor reported it.
 */
internal fun StyledString.encodeRuns(changed: (SpanStyle) -> Int = { ALL_STYLE_FIELDS }): List<Any?> {
    val args = ArrayList<Any?>(runs.size * STYLED_RUN_STRIDE)
    for ((s, style) in runs) {
        // ponytail: symbol runs are marked by a prefix no user text contains; a run type if more kinds appear
        val symbol = s.startsWith(SYMBOL_RUN)
        args += if (symbol) s.removePrefix(SYMBOL_RUN) else s
        args.encodeStyle(style, symbol, changed(style))
    }
    return args
}

internal fun MutableList<Any?>.encodeStyle(style: SpanStyle, symbol: Boolean = false, changed: Int = ALL_STYLE_FIELDS) {
    var flags = 0
    if (style.italic) flags = flags or 1
    if (style.underline) flags = flags or 2
    if (style.strikethrough) flags = flags or 4
    if (style.monospaced) flags = flags or 8
    if (symbol) flags = flags or 16
    this += style.color
    this += style.font
    this += style.fontSize
    this += style.fontWeight
    this += style.fontDesign
    this += flags
    this += style.baselineOffset
    this += style.kerning
    this += style.backgroundColor
    this += style.tracking
    this += style.link
    this += style.alignment?.name
    this += if (changed == ALL_STYLE_FIELDS) 0 else style.nativeAttributes?.token ?: 0
    this += changed
}

// Groups of SpanStyle properties, flagged when Kotlin changes them on a run the editor reported.
internal const val FIELD_COLOR = 1
internal const val FIELD_FONT = 2
internal const val FIELD_UNDERLINE = 4
internal const val FIELD_STRIKETHROUGH = 8
internal const val FIELD_BASELINE = 16
internal const val FIELD_KERNING = 32
internal const val FIELD_BACKGROUND = 64
internal const val FIELD_TRACKING = 128
internal const val FIELD_LINK = 256
internal const val FIELD_ALIGNMENT = 512
internal const val ALL_STYLE_FIELDS = 1023

/** The property groups that differ between [a] and [b]. */
internal fun changedFields(a: SpanStyle, b: SpanStyle): Int {
    var m = 0
    if (a.color != b.color) m = m or FIELD_COLOR
    if (a.font != b.font || a.fontSize != b.fontSize || a.fontWeight != b.fontWeight || a.fontDesign != b.fontDesign ||
        a.italic != b.italic || a.monospaced != b.monospaced) m = m or FIELD_FONT
    if (a.underline != b.underline) m = m or FIELD_UNDERLINE
    if (a.strikethrough != b.strikethrough) m = m or FIELD_STRIKETHROUGH
    if (a.baselineOffset != b.baselineOffset) m = m or FIELD_BASELINE
    if (a.kerning != b.kerning) m = m or FIELD_KERNING
    if (a.backgroundColor != b.backgroundColor) m = m or FIELD_BACKGROUND
    if (a.tracking != b.tracking) m = m or FIELD_TRACKING
    if (a.link != b.link) m = m or FIELD_LINK
    if (a.alignment != b.alignment) m = m or FIELD_ALIGNMENT
    return m
}

/**
 * Decodes runs sent back by Swift in the [encodeRuns] layout. Values Swift created itself (formatting applied in a
 * rich editor) arrive as a `[r, g, b, a]` list for colors, a PostScript name for a font picked by name and a weight
 * name for the font weight.
 */
internal fun decodeStyledRuns(values: List<*>): StyledString {
    val runs = ArrayList<Pair<String, SpanStyle>>(values.size / STYLED_RUN_STRIDE)
    for (b in 0 until values.size / STYLED_RUN_STRIDE * STYLED_RUN_STRIDE step STYLED_RUN_STRIDE) {
        runs += values[b] as String to decodeStyle(values, b + 1)
    }
    return StyledString(runs)
}

internal fun decodeStyle(values: List<*>, b: Int): SpanStyle {
    val flags = (values[b + 5] as Number).toInt()
    val size = (values[b + 2] as? Number)?.toDouble()
    return SpanStyle(
        color = decodeColor(values[b]),
        font = when (val f = values[b + 1]) {
            is Font -> f
            is String -> Font.custom(f, size ?: 17.0)
            else -> null
        },
        fontSize = if (values[b + 1] is String) null else size,
        fontWeight = when (val w = values[b + 3]) {
            is Font.Weight -> w
            is String -> fontWeightNamed(w)
            else -> null
        },
        fontDesign = values[b + 4] as? Font.Design,
        italic = flags and 1 != 0,
        underline = flags and 2 != 0,
        strikethrough = flags and 4 != 0,
        monospaced = flags and 8 != 0,
        baselineOffset = (values[b + 6] as Number).toDouble(),
        kerning = (values[b + 7] as Number).toDouble(),
        backgroundColor = decodeColor(values[b + 8]),
        tracking = (values[b + 9] as Number).toDouble(),
        link = values[b + 10] as? String,
        alignment = (values[b + 11] as? String)?.let { AttributedTextAlignment.valueOf(it) },
        nativeAttributes = (values[b + 12] as? Number)?.toInt()?.takeIf { it != 0 }?.let(::NativeAttributes),
    )
}

private fun decodeColor(value: Any?): Color? = when (value) {
    is Color -> value
    is List<*> -> value.map { (it as Number).toDouble() }.let { Color(red = it[0], green = it[1], blue = it[2], opacity = it[3]) }
    else -> null
}

private fun fontWeightNamed(name: String): Font.Weight? = when (name) {
    "ultraLight" -> Font.Weight.ultraLight
    "thin" -> Font.Weight.thin
    "light" -> Font.Weight.light
    "regular" -> Font.Weight.regular
    "medium" -> Font.Weight.medium
    "semibold" -> Font.Weight.semibold
    "bold" -> Font.Weight.bold
    "heavy" -> Font.Weight.heavy
    "black" -> Font.Weight.black
    else -> null
}

/** Values per run: the text, then [STYLE_STRIDE] style values. */
internal const val STYLE_STRIDE = 14
internal const val STYLED_RUN_STRIDE = 1 + STYLE_STRIDE

/** The pointer location of SwiftUI `HoverPhase.active(_:)` (in the modifier's coordinate space); null for `.ended`. */
@HiddenFromObjC
public val HoverPhase.location: CGPoint?
    get() = (args.firstOrNull() as? SExpr)?.let { CGPoint((it.args[0] as Number).toDouble(), (it.args[1] as Number).toDouble()) }

// ---- option sets ---------------------------------------------------------------------------------------

// SwiftUI combines option set members with array literals (`[.top, .leading]`); Kotlin uses `+` or `union`.

private const val OPTION_SET_UNION = 15

/** SwiftUI `Edge.Set.union(_:)`: `Edge.Set.top + Edge.Set.leading` is Swift's `[.top, .leading]`. */
@HiddenFromObjC
public infix fun Edge.Set.union(other: Edge.Set): Edge.Set = Edge.Set(OPTION_SET_UNION, listOf("Edge.Set", this, other))

/** SwiftUI `Edge.Set.union(_:)`. */
@HiddenFromObjC
public operator fun Edge.Set.plus(other: Edge.Set): Edge.Set = union(other)

/** SwiftUI `Axis.Set.union(_:)`. */
@HiddenFromObjC
public infix fun Axis.Set.union(other: Axis.Set): Axis.Set = Axis.Set(OPTION_SET_UNION, listOf("Axis.Set", this, other))

/** SwiftUI `Axis.Set.union(_:)`. */
@HiddenFromObjC
public operator fun Axis.Set.plus(other: Axis.Set): Axis.Set = union(other)

/** SwiftUI `EventModifiers.union(_:)`. */
@HiddenFromObjC
public infix fun EventModifiers.union(other: EventModifiers): EventModifiers = EventModifiers(OPTION_SET_UNION, listOf("EventModifiers", this, other))

/** SwiftUI `EventModifiers.union(_:)`. */
@HiddenFromObjC
public operator fun EventModifiers.plus(other: EventModifiers): EventModifiers = union(other)

/** SwiftUI `HorizontalEdge.Set.union(_:)`. */
@HiddenFromObjC
public infix fun HorizontalEdge.Set.union(other: HorizontalEdge.Set): HorizontalEdge.Set = HorizontalEdge.Set(OPTION_SET_UNION, listOf("HorizontalEdge.Set", this, other))

/** SwiftUI `HorizontalEdge.Set.union(_:)`. */
@HiddenFromObjC
public operator fun HorizontalEdge.Set.plus(other: HorizontalEdge.Set): HorizontalEdge.Set = union(other)

/** SwiftUI `VerticalEdge.Set.union(_:)`. */
@HiddenFromObjC
public infix fun VerticalEdge.Set.union(other: VerticalEdge.Set): VerticalEdge.Set = VerticalEdge.Set(OPTION_SET_UNION, listOf("VerticalEdge.Set", this, other))

/** SwiftUI `VerticalEdge.Set.union(_:)`. */
@HiddenFromObjC
public operator fun VerticalEdge.Set.plus(other: VerticalEdge.Set): VerticalEdge.Set = union(other)

/** SwiftUI `SafeAreaRegions.union(_:)`. */
@HiddenFromObjC
public infix fun SafeAreaRegions.union(other: SafeAreaRegions): SafeAreaRegions = SafeAreaRegions(OPTION_SET_UNION, listOf("SafeAreaRegions", this, other))

/** SwiftUI `SafeAreaRegions.union(_:)`. */
@HiddenFromObjC
public operator fun SafeAreaRegions.plus(other: SafeAreaRegions): SafeAreaRegions = union(other)

/** SwiftUI `ContentShapeKinds.union(_:)`. */
@HiddenFromObjC
public infix fun ContentShapeKinds.union(other: ContentShapeKinds): ContentShapeKinds = ContentShapeKinds(OPTION_SET_UNION, listOf("ContentShapeKinds", this, other))

/** SwiftUI `ContentShapeKinds.union(_:)`. */
@HiddenFromObjC
public operator fun ContentShapeKinds.plus(other: ContentShapeKinds): ContentShapeKinds = union(other)

/** SwiftUI `AccessibilityTraits.union(_:)`. */
@HiddenFromObjC
public infix fun AccessibilityTraits.union(other: AccessibilityTraits): AccessibilityTraits = AccessibilityTraits(OPTION_SET_UNION, listOf("AccessibilityTraits", this, other))

/** SwiftUI `AccessibilityTraits.union(_:)`. */
@HiddenFromObjC
public operator fun AccessibilityTraits.plus(other: AccessibilityTraits): AccessibilityTraits = union(other)

/** SwiftUI `AccessibilityTechnologies.union(_:)`. */
@HiddenFromObjC
public infix fun AccessibilityTechnologies.union(other: AccessibilityTechnologies): AccessibilityTechnologies = AccessibilityTechnologies(OPTION_SET_UNION, listOf("AccessibilityTechnologies", this, other))

/** SwiftUI `AccessibilityTechnologies.union(_:)`. */
@HiddenFromObjC
public operator fun AccessibilityTechnologies.plus(other: AccessibilityTechnologies): AccessibilityTechnologies = union(other)

/** SwiftUI `MatchedGeometryProperties.union(_:)`. */
@HiddenFromObjC
public infix fun MatchedGeometryProperties.union(other: MatchedGeometryProperties): MatchedGeometryProperties = MatchedGeometryProperties(OPTION_SET_UNION, listOf("MatchedGeometryProperties", this, other))

/** SwiftUI `MatchedGeometryProperties.union(_:)`. */
@HiddenFromObjC
public operator fun MatchedGeometryProperties.plus(other: MatchedGeometryProperties): MatchedGeometryProperties = union(other)

/** SwiftUI `GestureMask.union(_:)`. */
@HiddenFromObjC
public infix fun GestureMask.union(other: GestureMask): GestureMask = GestureMask(OPTION_SET_UNION, listOf("GestureMask", this, other))

/** SwiftUI `GestureMask.union(_:)`. */
@HiddenFromObjC
public operator fun GestureMask.plus(other: GestureMask): GestureMask = union(other)

/** SwiftUI `SearchSuggestionsPlacement.Set.union(_:)`. */
@HiddenFromObjC
public infix fun SearchSuggestionsPlacement.Set.union(other: SearchSuggestionsPlacement.Set): SearchSuggestionsPlacement.Set = SearchSuggestionsPlacement.Set(OPTION_SET_UNION, listOf("SearchSuggestionsPlacement.Set", this, other))

/** SwiftUI `SearchSuggestionsPlacement.Set.union(_:)`. */
@HiddenFromObjC
public operator fun SearchSuggestionsPlacement.Set.plus(other: SearchSuggestionsPlacement.Set): SearchSuggestionsPlacement.Set = union(other)

/**
 * SwiftUI `@ScaledMetric`: [value] scaled with the user's Dynamic Type size, relative to the [relativeTo] text style
 * (body by default). Use it for icon sizes, spacing or frames that should grow with the text.
 */
@HiddenFromObjC
@Composable
public fun ScaledMetric(value: Double, relativeTo: Font.TextStyle? = null, content: @Composable (scaled: Double) -> Unit) {
    var scaled by remember(value) { mutableStateOf(value) }
    SwiftView(ExtraIds.SCALED_METRIC, listOf(value, relativeTo, SValueCallback { scaled = (it as Number).toDouble() }), Modifier) { content(scaled) }
}
