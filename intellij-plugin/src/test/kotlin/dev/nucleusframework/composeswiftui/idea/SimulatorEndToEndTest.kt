package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.MouseEvent
import java.net.ServerSocket

/**
 * The IDE side against the real preview host on the simulator. Needs a render of the sample first
 * (`./gradlew :shared:renderComposeSwiftUIPreviews` in samples/kmp-app) and `-Dcsui.e2e.module=<…/samples/kmp-app/shared>`;
 * skipped otherwise.
 */
class SimulatorEndToEndTest : BasePlatformTestCase() {
    private val moduleDir: String? = System.getProperty("csui.e2e.module")

    fun testInteractiveTapChangesTheLivePreview() {
        val dir = moduleDir ?: return
        val module = PreviewModule(dir, dir, ":shared")
        val port = ServerSocket(0).use { it.localPort }
        assertTrue("start the live preview", PreviewHostClient(module).startInteractive("sample.CounterScreenPreview", port, emptyList()))
        val panel = InteractivePreview("Counter", SourceNavigator(project, module)) {}
        try {
            panel.setSize(500, 1000)
            panel.doLayout()
            panel.connect(port)
            waitFor { panel.liveStatus.startsWith("Live ·") }
            val screen = panel.liveScreen
            val before = (screen as javax.swing.JComponent).let { snapshot(it) }

            // "Increment" sits at about (201, 572) points of the 402 × 874 point screen.
            val scale = minOf((screen.width - 24.0) / 402, (screen.height - 24.0) / 874, 2.0)
            val x = ((screen.width - 402 * scale) / 2 + 201 * scale).toInt()
            val y = (12 + 572 * scale).toInt()
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                screen.dispatchEvent(MouseEvent(screen, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            waitFor { !snapshot(screen).contentEquals(before) }
            assertFalse("the live preview changed after the tap", snapshot(screen).contentEquals(before))
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun testLivePreviewKeepsWorkingWhileTheImagesRender() {
        val dir = moduleDir ?: return
        val module = PreviewModule(dir, dir, ":shared")
        val client = PreviewHostClient(module)
        val port = ServerSocket(0).use { it.localPort }
        assertTrue("start the live preview", client.startInteractive("sample.CounterScreenPreview", port, emptyList()))
        val panel = InteractivePreview("Counter", SourceNavigator(project, module)) {}
        try {
            panel.setSize(500, 1000)
            panel.doLayout()
            panel.connect(port)
            waitFor { panel.liveStatus.startsWith("Live ·") }
            // Renders on the same host, as a save does while the live preview runs.
            assertTrue("render next to the live preview", client.render(listOf("sample.CounterScreenPreview", "sample.SettingsPreview"), emptyList(), 300))
            val screen = panel.liveScreen as javax.swing.JComponent
            val before = snapshot(screen)
            val scale = minOf((screen.width - 24.0) / 402, (screen.height - 24.0) / 874, 2.0)
            val x = ((screen.width - 402 * scale) / 2 + 201 * scale).toInt()
            val y = (12 + 572 * scale).toInt()
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                screen.dispatchEvent(MouseEvent(screen, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            waitFor { !snapshot(screen).contentEquals(before) }
            assertFalse("the live preview still reacts after a render", snapshot(screen).contentEquals(before))
        } finally {
            Disposer.dispose(panel)
        }
    }

    private fun snapshot(component: javax.swing.JComponent): IntArray {
        val image = java.awt.image.BufferedImage(component.width, component.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        component.paint(image.graphics)
        return image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(20)
        }
    }
}
