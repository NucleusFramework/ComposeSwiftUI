package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.MouseEvent
import java.io.File
import java.net.ServerSocket
import javax.imageio.ImageIO

/**
 * The IDE side against the real preview host on the Mac. Needs a render of the macOS sample first
 * (`./gradlew :shared:renderComposeSwiftUIPreviews` in samples/macos-app) and
 * `-Dcsui.e2e.mac.module=<…/samples/macos-app/shared>`; skipped otherwise.
 */
class MacEndToEndTest : BasePlatformTestCase() {
    private val moduleDir: String? = System.getProperty("csui.e2e.mac.module")

    fun testRendersOnTheRunningMacHostWithoutGradle() {
        val dir = moduleDir ?: return
        val module = PreviewModule(dir, dir, ":shared")
        val client = PreviewHostClient(module)
        assertEquals("the Mac host of the last build", MAC_HOST, client.runningHost())
        val image = File(module.previewsDir, "sample.mac.CounterPreview.png").apply { delete() }
        assertTrue("render on the running host", client.render(listOf("sample.mac.CounterPreview"), emptyList(), 300))
        val png = ImageIO.read(image)
        assertNotNull("the image is written again", png)
        // 400 × 300 points, at the scale of this Mac's screen.
        assertEquals(4.0 / 3, png.width.toDouble() / png.height, 0.01)
    }

    fun testInteractiveClickChangesTheLivePreview() {
        val dir = moduleDir ?: return
        val module = PreviewModule(dir, dir, ":shared")
        val port = ServerSocket(0).use { it.localPort }
        assertTrue("start the live preview", PreviewHostClient(module).startInteractive("sample.mac.CounterPreview", port, emptyList()))
        val panel = InteractivePreview("Counter", SourceNavigator(project, module)) {}
        try {
            panel.setSize(500, 400)
            panel.doLayout()
            panel.connect(port)
            waitFor { panel.liveStatus.startsWith("Live ·") }
            val screen = panel.liveScreen
            waitFor { snapshot(screen).distinct().size > 2 }
            val before = snapshot(screen)

            // "Increment" sits at about (200, 195) points of the 400 × 300 point preview.
            val scale = minOf((screen.width - 24.0) / 400, (screen.height - 24.0) / 300, 2.0)
            val x = ((screen.width - 400 * scale) / 2 + 200 * scale).toInt()
            val y = (12 + 195 * scale).toInt()
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                screen.dispatchEvent(MouseEvent(screen, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            waitFor { !snapshot(screen).contentEquals(before) }
            assertFalse("the live preview changed after the click", snapshot(screen).contentEquals(before))
            System.getProperty("csui.e2e.dump")?.let { path ->
                val image = java.awt.image.BufferedImage(screen.width, screen.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
                screen.paint(image.graphics)
                ImageIO.write(image, "png", File(path))
            }
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
