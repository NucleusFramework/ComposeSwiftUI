package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import platform.AppKit.NSView
import platform.AppKit.NSViewController

/** Creates the embedded AppKit controller once and updates it (called by the Swift renderer). */
public class NSViewControllerFactory internal constructor(
    private val factory: () -> NSViewController,
    private val updater: (NSViewController) -> Unit,
) {
    public fun create(): NSViewController = factory()
    public fun update(controller: NSViewController): Unit = updater(controller)
}

/** Creates the embedded AppKit view once and updates it (called by the Swift renderer). */
public class NSViewFactory internal constructor(
    private val factory: () -> NSView,
    private val updater: (NSView) -> Unit,
) {
    public fun create(): NSView = factory()
    public fun update(view: NSView): Unit = updater(view)
}

/**
 * Embeds an AppKit view controller (SwiftUI `NSViewControllerRepresentable`). [factory] runs once; [update] runs
 * after it and on every recomposition.
 */
@Composable
public fun NSViewControllerView(
    modifier: Modifier = Modifier,
    sizing: NativeViewSizing = NativeViewSizing.Fill,
    update: (NSViewController) -> Unit = {},
    factory: () -> NSViewController,
) {
    SwiftView(NativeViewKinds.NS_VIEW_CONTROLLER, listOf(NSViewControllerFactory(factory, update), sizing.ordinal), modifier)
}

/**
 * Embeds an AppKit view (SwiftUI `NSViewRepresentable`). [factory] runs once; [update] runs after it and on every
 * recomposition. Use [NativeViewSizing.FitHeight] for content of variable height, such as a wrapping `NSTextField`.
 */
@Composable
public fun AppKitView(
    modifier: Modifier = Modifier,
    sizing: NativeViewSizing = NativeViewSizing.Fill,
    update: (NSView) -> Unit = {},
    factory: () -> NSView,
) {
    SwiftView(NativeViewKinds.NS_VIEW, listOf(NSViewFactory(factory, update), sizing.ordinal), modifier)
}

/** SwiftUI `Image(nsImage:)`: a decoded or downloaded `NSImage`. Image modifiers (`resizable()`…) apply. */
@Composable
public fun Image(nsImage: platform.AppKit.NSImage, modifier: Modifier = Modifier) {
    SwiftView(NativeViewKinds.NS_IMAGE, listOf(nsImage), modifier)
}
