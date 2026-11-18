package dev.nucleusframework.composeswiftui.gradle

import dev.nucleusframework.composeswiftui.analysis.CheckCategory
import dev.nucleusframework.composeswiftui.analysis.ImageComparer
import dev.nucleusframework.composeswiftui.analysis.PreviewChecks
import dev.nucleusframework.composeswiftui.analysis.ViewTree
import dev.nucleusframework.composeswiftui.analysis.VisualParity
import groovy.json.JsonSlurper
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewAnalysisTest {
    // A Button holding a Text, inside an HStack of the preview, as the host writes it (innermost call first).
    private val stack = """{"function":"SwiftView","file":"GenViews.kt","line":935,"offset":100},
        {"function":"HStack","file":"Screen.kt","line":9,"offset":50},
        {"function":"ScreenPreview","file":"ComposeSwiftUIPreviews.kt","line":3,"offset":10}"""
    private val button = """{"function":"SwiftView","file":"GenViews.kt","line":120,"offset":200},
        {"function":"Button","file":"Screen.kt","line":10,"offset":80},$stack"""
    private val layout = """
        {"width": 100, "height": 60, "nodes": [
          {"x": 0, "y": 0, "width": 100, "height": 60, "calls": [$stack]},
          {"x": 10, "y": 10, "width": 30, "height": 20, "calls": [$button]},
          {"x": 12, "y": 12, "width": 26, "height": 16, "text": "Go", "calls": [
            {"function":"SwiftView","file":"GenViews.kt","line":336,"offset":300},
            {"function":"Text","file":"Screen.kt","line":11,"offset":95}, $button]},
          {"x": 50, "y": 10, "width": 70, "height": 10, "text": "Tiny", "calls": [
            {"function":"SwiftView","file":"GenViews.kt","line":336,"offset":300},
            {"function":"Text","file":"Screen.kt","line":13,"offset":140}, $stack]}
        ]}
    """

    @Suppress("UNCHECKED_CAST")
    private fun tree() = ViewTree.parse(JsonSlurper().parseText(layout) as Map<String, Any?>)

    @Test
    fun `nests views by their calls and names them by the user's call`() {
        val tree = tree()
        assertEquals(listOf(-1, 0, 1, 0), tree.parents.toList())
        assertEquals(listOf("HStack", "Button", "Text", "Text"), tree.nodes.map { it.name })
        assertEquals("Screen.kt:12", tree.nodes[2].location)
        val text = tree.format()
        assertTrue("      Text \"Go\" · Screen.kt:12 · x=12 y=12 w=26 h=16" in text, text)
    }

    @Test
    fun `finds small touch targets, small text and views past the edge`() {
        val issues = PreviewChecks.check(tree(), null)
        assertEquals(
            setOf(CheckCategory.Accessibility to "Button", CheckCategory.Typography to "Text", CheckCategory.Layout to "Text"),
            issues.map { it.category to it.node!!.name }.toSet(),
        )
    }

    @Test
    fun `estimates text contrast from the image`() {
        val tree = tree()
        fun image(text: Color) = BufferedImage(100, 60, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, 100, 60)
                color = text
                fillRect(14, 14, 20, 10)
                dispose()
            }
        }
        val node = tree.nodes[2]
        assertEquals(21.0, PreviewChecks.contrast(image(Color.BLACK), node, 1.0)!!, 0.01)
        val pale = PreviewChecks.check(tree, image(Color(0xDD, 0xDD, 0xDD)))
        assertTrue(pale.any { it.category == CheckCategory.Contrast && it.node == node }, pale.toString())
    }

    @Test
    fun `compares images and writes a parity report`() {
        fun image(mark: Boolean) = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, 64, 64)
                if (mark) {
                    color = Color.BLUE
                    fillRect(40, 40, 8, 8)
                }
                dispose()
            }
        }
        val same = ImageComparer.compare(image(false), image(false))
        assertTrue(same.identical)
        assertTrue(same.psnr.isInfinite())
        val different = ImageComparer.compare(image(false), image(true))
        assertEquals(64, different.differentPixels)
        assertEquals(64 * 100.0 / (64 * 64), different.mismatchPercent, 1e-9)
        assertEquals(1, different.regions.size)
        assertEquals(40, different.regions.single().x)

        val dir = Files.createTempDirectory("parity").toFile()
        val before = dir.resolve("before").apply { mkdirs() }
        val after = dir.resolve("after").apply { mkdirs() }
        ImageComparer.write(image(false), before.resolve("a.png"))
        ImageComparer.write(image(false), after.resolve("a.png"))
        ImageComparer.write(image(false), before.resolve("b.png"))
        ImageComparer.write(image(true), after.resolve("b.png"))
        ImageComparer.write(image(true), after.resolve("c.png"))
        val pairs = VisualParity.verify(before, after, dir.resolve("report.md"), threshold = 0.5)
        assertEquals(listOf(true, false, false), pairs.map { it.matches(0.5) })
        val report = dir.resolve("report.md").readText()
        assertTrue("2 pair(s) compared: 1 match" in report, report)
        assertTrue(dir.resolve("report-diffs/b.png").isFile)
        dir.deleteRecursively()
    }
}
