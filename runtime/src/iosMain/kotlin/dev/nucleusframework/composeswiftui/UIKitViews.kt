package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/** Creates the embedded UIKit controller once (called by the Swift renderer). */
class UIViewControllerFactory internal constructor(private val factory: () -> UIViewController) {
    fun create(): UIViewController = factory()
}

/** Creates the embedded UIKit view once (called by the Swift renderer). */
class UIViewFactory internal constructor(private val factory: () -> UIView) {
    fun create(): UIView = factory()
}

/**
 * Embeds a UIKit view controller (SwiftUI `UIViewControllerRepresentable`), e.g. Compose Multiplatform content:
 * `UIViewControllerView { ComposeUIViewController { MyChart() } }`. [factory] runs once.
 */
@Composable
fun UIViewControllerView(modifier: Modifier = Modifier, factory: () -> UIViewController) {
    SwiftView(NativeViewKinds.UI_VIEW_CONTROLLER, listOf(UIViewControllerFactory(factory)), modifier)
}

/** Embeds a UIKit view (SwiftUI `UIViewRepresentable`). [factory] runs once. */
@Composable
fun UIKitView(modifier: Modifier = Modifier, factory: () -> UIView) {
    SwiftView(NativeViewKinds.UI_VIEW, listOf(UIViewFactory(factory)), modifier)
}
