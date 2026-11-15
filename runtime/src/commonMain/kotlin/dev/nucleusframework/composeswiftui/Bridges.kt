package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** An action implemented in Swift and invoked from Kotlin (e.g. SwiftUI's `dismiss`). */
interface SwiftAction {
    fun run()
}

/**
 * Hooks installed by the Swift renderer at start-up (`ComposeGenerated.setUp()`), so that Kotlin can
 * run code inside SwiftUI constructs that only exist in Swift.
 */
object SwiftUIBridge {
    /** Runs `block` inside `withAnimation(animation)`; the animation is a generated `Animation` value. */
    var animationRunner: ((SExpr?, () -> Unit) -> Unit)? = null

    /** Like [animationRunner], then runs the completion when the animation is logically complete. */
    var animationCompletionRunner: ((SExpr?, () -> Unit, () -> Unit) -> Unit)? = null
}

/** Size of a subview as measured by SwiftUI, passed to a [LayoutPolicy]. */
data class LayoutSize(val width: Double, val height: Double) {
    /** Values attached to the subview with `Modifier.layoutValue(key, value)` (SwiftUI `LayoutValueKey`). */
    var values: Map<String, Any?> = emptyMap()
}

/** Top-leading position of a subview inside the layout bounds. */
data class LayoutPlacement(val x: Double, val y: Double)

/**
 * A custom SwiftUI `Layout` written in Kotlin. Subviews are measured with an unspecified proposal
 * (their ideal size); the policy returns the container size and where each subview goes.
 */
interface LayoutPolicy {
    /** [proposedWidth] / [proposedHeight] are null when unspecified. */
    fun sizeThatFits(proposedWidth: Double?, proposedHeight: Double?, subviews: List<LayoutSize>): LayoutSize

    /** Returns one placement per subview, relative to the layout's top-leading corner. */
    fun placeSubviews(width: Double, height: Double, subviews: List<LayoutSize>): List<LayoutPlacement>
}

/** Value given to [ComposeSwiftUIHost.updateInput] by the Swift `ComposeView(input:)`. */
val LocalHostInput = compositionLocalOf<Any?> { null }

/** The latest input passed from Swift (`ComposeView(input: …) { … }`), typed by the caller. */
@Composable
@Suppress("UNCHECKED_CAST")
fun <T> hostInput(): T = LocalHostInput.current as T

/**
 * A SwiftUI `LocalizedStringKey` / `LocalizedStringResource`: looked up in the app's string tables
 * and parsed as Markdown, unlike a plain `String` which is displayed verbatim.
 */
data class LocalizedKey(val key: String)

/** `"settings.title".localized` == `LocalizedKey("settings.title")`. */
val String.localized: LocalizedKey get() = LocalizedKey(this)

/**
 * An SF Symbol name, as taken by `Image(systemName:)` and the `systemImage:` parameters.
 * Use the generated constants (`SFSymbol.starFill`); the constructor covers custom or newer symbols.
 */
@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
value class SFSymbol(val name: String) {
    companion object
}

/** View kinds of the platform view embeddings (UIKit / AppKit), rendered by the Swift renderer. */
/** How an embedded UIKit or AppKit view is sized by SwiftUI. */
public enum class NativeViewSizing {
    /** Takes the size SwiftUI proposes (set it with `Modifier.frame`). */
    Fill,

    /** Takes the proposed width and the height its content needs at that width, e.g. a multi-line label. */
    FitHeight,

    /** Takes the size its content needs (Auto Layout fitting size or intrinsic content size). */
    FitContent,
}

internal object NativeViewKinds {
    const val UI_VIEW_CONTROLLER = 100_010
    const val UI_VIEW = 100_011
    const val NS_VIEW_CONTROLLER = 100_012
    const val NS_VIEW = 100_013
    const val UI_IMAGE = 100_015
    const val NS_IMAGE = 100_016
}

// ---- Kotlin code that Swift calls synchronously (Interactive.kt) -----------------------------------

/** A SwiftUI `ScrollViewProxy`, implemented in Swift: scrolls to the view marked `Modifier.id(id)`. */
interface ScrollProxyHandle {
    fun scrollTo(id: Any, anchor: Any?)
}

/** `onKeyPress` handler: returns true when the key press is handled. */
interface KeyPressHandler {
    fun onKeyPress(characters: String, key: String, modifiers: Int, phase: Int): Boolean
}

/** `Canvas` renderer: the drawing commands for a canvas of the given size. */
interface CanvasRenderer {
    fun draw(width: Double, height: Double): List<Any?>
}

/**
 * Visual effects computed in Kotlin for `scrollTransition` / `visualEffect`: `[opacity, scale, offsetX, offsetY,
 * rotationDegrees, blurRadius]`. The arguments are the transition phase value (or 0) and the view geometry.
 */
interface EffectsProvider {
    fun effects(phase: Double, isIdentity: Boolean, width: Double, height: Double, minX: Double, minY: Double): List<Double>
}

/** `alignmentGuide` value computed in Kotlin from the view size. */
interface AlignmentGuideProvider {
    fun compute(width: Double, height: Double): Double
}

/** `dropDestination` handler: returns true when the dropped text items are accepted. */
interface DropHandler {
    fun onDrop(items: List<String>, x: Double, y: Double): Boolean
}

/** `containerRelativeFrame` length computed in Kotlin from the container length along [axis] (0 horizontal, 1 vertical). */
interface LengthProvider {
    fun length(length: Double, axis: Int): Double
}

/** Progress (0…1, may overshoot) of a Kotlin animation curve at [t], the fraction of its duration elapsed. */
interface AnimationCurve {
    fun progress(t: Double): Double
}

/** A SwiftUI `DropDelegate` implemented in Kotlin; locations are in the view's coordinate space. */
interface DropDelegateHandler {
    fun validateDrop(x: Double, y: Double): Boolean
    fun dropEntered(x: Double, y: Double)
    /** A generated `DropOperation` value, or null for the default operation. */
    fun dropUpdated(x: Double, y: Double): Any?
    fun dropExited()
    /** The dropped text items and file URLs, loaded asynchronously; returns whether the drop is accepted. */
    fun performDrop(x: Double, y: Double, texts: List<String>, urls: List<String>): Boolean
}

/** Text items produced on demand (copy, cut, Services export). */
interface TextItemsProvider {
    fun items(): List<String>
}

/** Text payload of a dragged container item, by item id (`dragContainer`). */
interface TextForIdProvider {
    fun text(id: Any): String
}

/** `dropConfiguration`: a generated `DropOperation` for a drop at (x, y) of [itemsCount] items, or null. */
interface DropOperationProvider {
    fun operation(x: Double, y: Double, itemsCount: Int): Any?
}
