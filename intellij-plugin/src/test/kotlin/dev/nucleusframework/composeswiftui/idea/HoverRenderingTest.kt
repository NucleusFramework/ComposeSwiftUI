package dev.nucleusframework.composeswiftui.idea

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Paints a real preview with a hovered view to /tmp, to look at the outline (needs -Dcsui.e2e.module). */
class HoverRenderingTest : BasePlatformTestCase() {
    fun testPaintHover() {
        val dir = System.getProperty("csui.e2e.module") ?: return
        val previews = File(dir, "build/composeSwiftUI/previews")
        val image = ImageIO.read(File(previews, "sample.SettingsPreview~0.png"))
        val layout = PreviewLayoutData.read(File(previews, "sample.SettingsPreview~0.layout.json"))!!
        myFixture.addFileToProject("PreviewShowcase.kt", "package sample\n" + "\n".repeat(60))
        val navigator = SourceNavigator(project, PreviewModule(dir, dir, ":shared"))
        navigator.prefetch(listOf(layout))
        val view = PreviewImageView(image, layout, navigator)
        val width = (402 * 0.7).toInt()
        val height = (874 * 0.7).toInt()
        view.setDisplaySize(width, height)
        view.setSize(width, height)
        // The toggle row, at (200, 194) points.
        val x = (200 * 0.7).toInt()
        val y = (194 * 0.7).toInt()
        view.dispatchEvent(MouseEvent(view, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false))
        println("tooltip: ${view.toolTipText}")
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        view.paint(out.createGraphics())
        ImageIO.write(out, "png", File("/tmp/hover.png"))
    }
}
