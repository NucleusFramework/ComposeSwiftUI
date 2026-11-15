package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/** Creates the embedded UIKit controller once and updates it (called by the Swift renderer). */
class UIViewControllerFactory internal constructor(
    private val factory: () -> UIViewController,
    private val updater: (UIViewController) -> Unit,
) {
    fun create(): UIViewController = factory()
    fun update(controller: UIViewController): Unit = updater(controller)
}

/** Creates the embedded UIKit view once and updates it (called by the Swift renderer). */
class UIViewFactory internal constructor(
    private val factory: () -> UIView,
    private val updater: (UIView) -> Unit,
) {
    fun create(): UIView = factory()
    fun update(view: UIView): Unit = updater(view)
}

/**
 * Embeds a UIKit view controller (SwiftUI `UIViewControllerRepresentable`), e.g. Compose Multiplatform content:
 * `UIViewControllerView { ComposeUIViewController { MyChart() } }`. [factory] runs once; [update] runs after it and
 * on every recomposition. [sizing] decides whether the controller's view is sized by SwiftUI or by its content.
 */
@Composable
fun UIViewControllerView(
    modifier: Modifier = Modifier,
    sizing: NativeViewSizing = NativeViewSizing.Fill,
    update: (UIViewController) -> Unit = {},
    factory: () -> UIViewController,
) {
    SwiftView(NativeViewKinds.UI_VIEW_CONTROLLER, listOf(UIViewControllerFactory(factory, update), sizing.ordinal), modifier)
}

/**
 * Embeds a UIKit view (SwiftUI `UIViewRepresentable`). [factory] runs once; [update] runs after it and on every
 * recomposition. Use [NativeViewSizing.FitHeight] for content of variable height, such as a multi-line `UILabel`.
 */
@Composable
fun UIKitView(
    modifier: Modifier = Modifier,
    sizing: NativeViewSizing = NativeViewSizing.Fill,
    update: (UIView) -> Unit = {},
    factory: () -> UIView,
) {
    SwiftView(NativeViewKinds.UI_VIEW, listOf(UIViewFactory(factory, update), sizing.ordinal), modifier)
}

/** SwiftUI `Image(uiImage:)`: a decoded or downloaded `UIImage`. Image modifiers (`resizable()`…) apply. */
@Composable
fun Image(uiImage: platform.UIKit.UIImage, modifier: Modifier = Modifier) {
    SwiftView(NativeViewKinds.UI_IMAGE, listOf(uiImage), modifier)
}
