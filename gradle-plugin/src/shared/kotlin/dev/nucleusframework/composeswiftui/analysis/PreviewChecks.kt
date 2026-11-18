package dev.nucleusframework.composeswiftui.analysis

import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** What a UI check is about, like Android Studio's UI Check categories. */
enum class CheckCategory { Accessibility, Contrast, Typography, Layout }

/** A problem found in a rendered preview, on [node] (null: the whole preview). */
data class CheckIssue(val category: CheckCategory, val message: String, val node: ViewNode?) {
    fun format(): String = buildString {
        append('[').append(category).append("] ").append(message)
        node?.let { n ->
            append(" — ").append(n.name)
            n.text?.let { append(" \"").append(it.take(40)).append('"') }
            n.location?.let { append(" at ").append(it) }
        }
    }
}

/**
 * UI checks on a rendered preview, from its views and its image: what Android Studio's UI Check (visual lint) reports,
 * with Apple's Human Interface Guidelines as the reference. Heuristic: an issue is worth a look, not a proof.
 */
object PreviewChecks {
    /** Apple's minimum hit target, in points. */
    const val MIN_TOUCH_TARGET = 44.0

    /** WCAG AA contrast for body text, and for large text (from about 18 pt, i.e. a 24 pt tall line). */
    const val MIN_CONTRAST = 4.5
    const val MIN_CONTRAST_LARGE = 3.0

    private val interactive = setOf(
        "Button", "Toggle", "Link", "Stepper", "Slider", "Picker", "Menu", "TextField", "SecureField", "NavigationLink",
        "DatePicker", "ColorPicker", "ShareLink", "PasteButton", "EditButton",
    )

    /** Containers that give their rows (or items) a full-size hit area of their own. */
    private val rowContainers = setOf("List", "Form", "Menu", "ToolbarItem", "toolbar", "Picker", "TabView", "ContextMenu", "contextMenu", "swipeActions")

    /** Containers whose content may extend past the visible area by design. */
    private val scrolling = setOf("ScrollView", "List", "Form", "TabView", "LazyHStack", "LazyHGrid", "NavigationSplitView", "Carousel")

    private val textViews = setOf("Text", "Label", "TextField", "SecureField", "TextEditor")

    fun check(tree: ViewTree, image: BufferedImage?): List<CheckIssue> {
        val issues = ArrayList<CheckIssue>()
        val scale = if (image != null && tree.width > 0) image.width / tree.width else 1.0
        tree.nodes.forEachIndexed { index, node ->
            if (node.name in interactive && !node.isInside(rowContainers) && (node.width < MIN_TOUCH_TARGET - 0.5 || node.height < MIN_TOUCH_TARGET - 0.5)) {
                issues += CheckIssue(
                    CheckCategory.Accessibility,
                    "Touch target ${number(node.width)}×${number(node.height)} pt, under ${MIN_TOUCH_TARGET.toInt()}×${MIN_TOUCH_TARGET.toInt()} pt: " +
                        "pad it or use .frame(minWidth: 44, minHeight: 44)",
                    node,
                )
            }
            if (node.name in textViews && node.text != null && tree.children(index).isEmpty()) {
                if (node.height in 1.0..11.0) {
                    issues += CheckIssue(
                        CheckCategory.Typography,
                        "Text ${number(node.height)} pt tall: its font is likely under 11 pt, the smallest legible size",
                        node,
                    )
                }
                if (image != null) contrast(image, node, scale)?.let { ratio ->
                    val minimum = if (node.height >= 24) MIN_CONTRAST_LARGE else MIN_CONTRAST
                    if (ratio < minimum) {
                        issues += CheckIssue(
                            CheckCategory.Contrast,
                            "Text contrast about ${String.format(java.util.Locale.ROOT, "%.1f", ratio)}:1, under $minimum:1 (estimated from the image)",
                            node,
                        )
                    }
                }
            }
            if (!node.isInside(scrolling) && tree.width > 0 && (node.x < -0.5 || node.right > tree.width + 0.5)) {
                issues += CheckIssue(
                    CheckCategory.Layout,
                    "Extends past the ${if (node.x < -0.5) "leading" else "trailing"} edge of the ${number(tree.width)} pt wide preview: clipped on this size",
                    node,
                )
            }
        }
        // Texts drawn over each other: a layout that doesn't fit (or a ZStack worth a second look).
        val texts = tree.nodes.withIndex().filter { (i, n) -> n.text != null && n.name in textViews && tree.children(i).isEmpty() }.map { it.value }
        for (i in texts.indices) for (j in i + 1 until texts.size) {
            val a = texts[i]
            val b = texts[j]
            val overlap = a.intersection(b)
            if (overlap > 0.25 * min(a.area(), b.area()) && !a.contains(b) && !b.contains(a)) {
                issues += CheckIssue(CheckCategory.Layout, "Overlaps ${b.name} \"${b.text?.take(30)}\"${b.location?.let { " at $it" } ?: ""}", a)
            }
        }
        return issues
    }

    /**
     * The contrast ratio of a text, estimated from the pixels of its frame: the most frequent color is the background,
     * the color of at least 2% of the pixels that contrasts most with it is the text. Null when the frame holds a single color.
     */
    fun contrast(image: BufferedImage, node: ViewNode, scale: Double): Double? {
        val x0 = max(0, (node.x * scale).toInt())
        val y0 = max(0, (node.y * scale).toInt())
        val x1 = min(image.width, ((node.x + node.width) * scale).toInt())
        val y1 = min(image.height, ((node.y + node.height) * scale).toInt())
        if (x1 - x0 < 2 || y1 - y0 < 2) return null
        // Colors quantized to 5 bits per channel, so antialiasing doesn't spread the counts too thin.
        val counts = HashMap<Int, Int>()
        for (y in y0 until y1) for (x in x0 until x1) {
            val argb = opaqueRgb(image.getRGB(x, y))
            val key = ((argb shr 19) and 0x1F shl 10) or ((argb shr 11) and 0x1F shl 5) or ((argb shr 3) and 0x1F)
            counts.merge(key, 1, Int::plus)
        }
        val total = (x1 - x0) * (y1 - y0)
        val background = counts.maxByOrNull { it.value }!!.key
        val bgLuminance = luminance(background)
        return counts.filter { it.key != background && it.value >= total * 0.02 }
            .map { ratio(luminance(it.key), bgLuminance) }
            .maxOrNull()
    }

    private fun luminance(key: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 31.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(key shr 10 and 0x1F) + 0.7152 * channel(key shr 5 and 0x1F) + 0.0722 * channel(key and 0x1F)
    }

    private fun ratio(a: Double, b: Double) = (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
