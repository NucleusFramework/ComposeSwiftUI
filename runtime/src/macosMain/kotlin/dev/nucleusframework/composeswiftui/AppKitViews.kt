package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import platform.AppKit.NSView
import platform.AppKit.NSViewController

/** Creates the embedded AppKit controller once (called by the Swift renderer). */
class NSViewControllerFactory internal constructor(private val factory: () -> NSViewController) {
    fun create(): NSViewController = factory()
}

/** Creates the embedded AppKit view once (called by the Swift renderer). */
class NSViewFactory internal constructor(private val factory: () -> NSView) {
    fun create(): NSView = factory()
}

/** Embeds an AppKit view controller (SwiftUI `NSViewControllerRepresentable`). [factory] runs once. */
@Composable
fun NSViewControllerView(modifier: Modifier = Modifier, factory: () -> NSViewController) {
    SwiftView(NativeViewKinds.NS_VIEW_CONTROLLER, listOf(NSViewControllerFactory(factory)), modifier)
}

/** Embeds an AppKit view (SwiftUI `NSViewRepresentable`). [factory] runs once. */
@Composable
fun AppKitView(modifier: Modifier = Modifier, factory: () -> NSView) {
    SwiftView(NativeViewKinds.NS_VIEW, listOf(NSViewFactory(factory)), modifier)
}
