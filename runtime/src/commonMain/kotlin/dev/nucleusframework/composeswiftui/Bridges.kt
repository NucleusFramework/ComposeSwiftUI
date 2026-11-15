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
}

/** Size of a subview as measured by SwiftUI, passed to a [LayoutPolicy]. */
data class LayoutSize(val width: Double, val height: Double)

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
