package dev.nucleusframework.composeswiftui.analysis

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/** A rectangle of the images where they differ, in pixels of the expected image. */
data class DiffRegion(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * How two images differ, like Android Studio's `compare_images`: [mismatchPercent] of the pixels differ by more than
 * the tolerance on some channel; [mse], [rmse] and [psnr] over all channels (0–255).
 */
data class ImageComparison(
    val width: Int,
    val height: Int,
    /** The actual image had another size and was scaled to the expected one first. */
    val resized: Boolean,
    val actualWidth: Int,
    val actualHeight: Int,
    val differentPixels: Int,
    val mismatchPercent: Double,
    val mse: Double,
    val rmse: Double,
    val psnr: Double,
    val regions: List<DiffRegion>,
) {
    val identical get() = differentPixels == 0

    fun format(): String = buildString {
        appendLine("Compared ${width}×$height px" + if (resized) " (actual image ${actualWidth}×$actualHeight px, scaled to compare)" else "")
        if (resized && abs(width.toDouble() / height - actualWidth.toDouble() / actualHeight) > 0.02 * width / height) {
            appendLine("The aspect ratios differ: the actual image was stretched. Give the preview the size of the expected image (@Preview width and height) for a fair comparison.")
        }
        appendLine("Mismatch: ${fmt(mismatchPercent)}% of the pixels ($differentPixels)")
        appendLine("MSE: ${fmt(mse)}, RMSE: ${fmt(rmse)}, PSNR: ${if (psnr.isInfinite()) "∞" else fmt(psnr)} dB")
        if (regions.isNotEmpty()) {
            appendLine("Regions that differ (px): " + regions.joinToString { "x=${it.x} y=${it.y} w=${it.width} h=${it.height}" })
        }
    }

    /** The JSON of this comparison, by hand: the shared code has no JSON library. */
    fun toJson(extra: Map<String, String> = emptyMap()): String = buildString {
        append("{\"width\":").append(width).append(",\"height\":").append(height)
        append(",\"resized\":").append(resized)
        append(",\"differentPixels\":").append(differentPixels)
        append(",\"mismatchPercent\":").append(fmt(mismatchPercent))
        append(",\"mse\":").append(fmt(mse)).append(",\"rmse\":").append(fmt(rmse))
        append(",\"psnr\":").append(if (psnr.isInfinite()) "null" else fmt(psnr))
        append(",\"regions\":[").append(regions.joinToString(",") { "{\"x\":${it.x},\"y\":${it.y},\"width\":${it.width},\"height\":${it.height}}" }).append(']')
        for ((key, value) in extra) append(",\"").append(key).append("\":\"").append(value.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
        append('}')
    }
}

object ImageComparer {
    /** Channel difference (0–255) under which two pixels count as the same: absorbs antialiasing noise. */
    const val DEFAULT_TOLERANCE = 8

    /** Size of the cells that group differing pixels into regions. */
    private const val CELL = 8

    fun read(file: File): BufferedImage =
        ImageIO.read(file) ?: throw IllegalArgumentException("Not an image: $file")

    fun compare(expected: BufferedImage, actual: BufferedImage, tolerance: Int = DEFAULT_TOLERANCE): ImageComparison {
        val w = expected.width
        val h = expected.height
        val resized = actual.width != w || actual.height != h
        val other = if (resized) scaled(actual, w, h) else actual
        var different = 0
        var squares = 0.0
        val cols = (w + CELL - 1) / CELL
        val rows = (h + CELL - 1) / CELL
        val cells = BooleanArray(cols * rows)
        for (y in 0 until h) for (x in 0 until w) {
            val a = opaqueRgb(expected.getRGB(x, y))
            val b = opaqueRgb(other.getRGB(x, y))
            var differs = false
            for (shift in intArrayOf(16, 8, 0)) {
                val d = ((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)
                squares += (d * d).toDouble()
                if (abs(d) > tolerance) differs = true
            }
            if (differs) {
                different++
                cells[(y / CELL) * cols + x / CELL] = true
            }
        }
        val mse = squares / (w.toDouble() * h * 3)
        return ImageComparison(
            width = w,
            height = h,
            resized = resized,
            actualWidth = actual.width,
            actualHeight = actual.height,
            differentPixels = different,
            mismatchPercent = 100.0 * different / (w.toDouble() * h),
            mse = mse,
            rmse = sqrt(mse),
            psnr = if (mse == 0.0) Double.POSITIVE_INFINITY else 10 * log10(255.0 * 255.0 / mse),
            regions = regions(cells, cols, rows, w, h),
        )
    }

    /** The actual image in gray, with the pixels that differ from the expected one in red. */
    fun diffImage(expected: BufferedImage, actual: BufferedImage, tolerance: Int = DEFAULT_TOLERANCE): BufferedImage {
        val w = expected.width
        val h = expected.height
        val other = if (actual.width != w || actual.height != h) scaled(actual, w, h) else actual
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until h) for (x in 0 until w) {
            val a = opaqueRgb(expected.getRGB(x, y))
            val b = opaqueRgb(other.getRGB(x, y))
            val differs = intArrayOf(16, 8, 0).any { abs(((a shr it) and 0xFF) - ((b shr it) and 0xFF)) > tolerance }
            out.setRGB(x, y, if (differs) 0xFF0000 else {
                // Faded gray, so the red stands out.
                val gray = (((b shr 16) and 0xFF) * 3 + ((b shr 8) and 0xFF) * 6 + (b and 0xFF)) / 10
                val faded = 160 + gray * 95 / 255
                (faded shl 16) or (faded shl 8) or faded
            })
        }
        return out
    }

    /** Expected, actual and diff next to each other, with the regions that differ outlined on the actual image. */
    fun sideBySide(expected: BufferedImage, actual: BufferedImage, diff: BufferedImage, regions: List<DiffRegion>): BufferedImage {
        val w = expected.width
        val h = expected.height
        val gap = 16
        val out = BufferedImage(w * 3 + gap * 2, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.color = Color(0x80, 0x80, 0x80)
            g.fillRect(0, 0, out.width, out.height)
            g.drawImage(expected, 0, 0, null)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(actual, w + gap, 0, w, h, null)
            g.drawImage(diff, (w + gap) * 2, 0, null)
            g.color = Color.RED
            g.stroke = java.awt.BasicStroke(max(2f, w / 300f))
            for (r in regions) g.drawRect(w + gap + r.x, r.y, r.width, r.height)
        } finally {
            g.dispose()
        }
        return out
    }

    fun write(image: BufferedImage, file: File) {
        file.absoluteFile.parentFile?.mkdirs()
        ImageIO.write(image, "png", file)
    }

    private fun scaled(image: BufferedImage, w: Int, h: Int): BufferedImage {
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(image, 0, 0, w, h, null)
        } finally {
            g.dispose()
        }
        return out
    }

    /** Groups of touching cells that differ, largest first (at most 12). */
    private fun regions(cells: BooleanArray, cols: Int, rows: Int, w: Int, h: Int): List<DiffRegion> {
        val seen = BooleanArray(cells.size)
        val found = ArrayList<DiffRegion>()
        for (start in cells.indices) {
            if (!cells[start] || seen[start]) continue
            var minC = cols
            var minR = rows
            var maxC = -1
            var maxR = -1
            val queue = ArrayDeque<Int>().apply { add(start) }
            seen[start] = true
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                val c = cell % cols
                val r = cell / cols
                minC = minOf(minC, c); maxC = maxOf(maxC, c); minR = minOf(minR, r); maxR = maxOf(maxR, r)
                // 8-neighbours, and one cell further: nearby glyphs of one word make one region.
                for (dr in -2..2) for (dc in -2..2) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr !in 0 until rows || nc !in 0 until cols) continue
                    val n = nr * cols + nc
                    if (cells[n] && !seen[n]) {
                        seen[n] = true
                        queue += n
                    }
                }
            }
            val x = minC * CELL
            val y = minR * CELL
            found += DiffRegion(x, y, minOf(w, (maxC + 1) * CELL) - x, minOf(h, (maxR + 1) * CELL) - y)
        }
        return found.sortedByDescending { it.width * it.height }.take(12)
    }

    /**
     * The views of [tree] where [comparison] found differences: for each region, the smallest view that holds its center.
     * The expected image spans the whole preview (the actual image was scaled to it when their sizes differ).
     */
    fun viewsOfRegions(comparison: ImageComparison, tree: ViewTree): List<Pair<DiffRegion, ViewNode?>> =
        comparison.regions.map { r ->
            if (tree.width <= 0 || tree.height <= 0) return@map r to null
            val cx = (r.x + r.width / 2.0) * tree.width / comparison.width
            val cy = (r.y + r.height / 2.0) * tree.height / comparison.height
            r to tree.nodesAt(cx, cy).firstOrNull()
        }
}

/** Transparent pixels over white, like the images look in a viewer: `0xRRGGBB`. */
internal fun opaqueRgb(argb: Int): Int {
    val alpha = (argb ushr 24) and 0xFF
    if (alpha == 0xFF) return argb and 0xFFFFFF
    fun blend(shift: Int) = (((argb shr shift) and 0xFF) * alpha + 255 * (255 - alpha)) / 255
    return (blend(16) shl 16) or (blend(8) shl 8) or blend(0)
}

internal fun fmt(value: Double) = String.format(java.util.Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')

/** One pair of a parity check: images with the same name in both folders. */
data class ParityPair(val name: String, val before: File?, val after: File?, val comparison: ImageComparison?, val diff: File?) {
    fun matches(threshold: Double) = comparison != null && comparison.mismatchPercent <= threshold
}

/**
 * Android Studio's `verify_batch_visual_parity`: pairs the PNGs of two folders by name, compares each pair and writes a
 * Markdown report, with the diff images next to it.
 */
object VisualParity {
    fun verify(before: File, after: File, report: File, threshold: Double = 0.5, tolerance: Int = ImageComparer.DEFAULT_TOLERANCE): List<ParityPair> {
        fun pngs(dir: File) = dir.listFiles { f -> f.isFile && f.extension.equals("png", ignoreCase = true) }.orEmpty().associateBy { it.name }
        val beforeImages = pngs(before)
        val afterImages = pngs(after)
        val diffs = File(report.absoluteFile.parentFile, report.nameWithoutExtension + "-diffs")
        val pairs = (beforeImages.keys + afterImages.keys).sorted().map { name ->
            val b = beforeImages[name]
            val a = afterImages[name]
            if (b == null || a == null) return@map ParityPair(name, b, a, null, null)
            val expected = ImageComparer.read(b)
            val actual = ImageComparer.read(a)
            val comparison = ImageComparer.compare(expected, actual, tolerance)
            val diff = if (comparison.identical) null else File(diffs, name).also {
                ImageComparer.write(ImageComparer.sideBySide(expected, actual, ImageComparer.diffImage(expected, actual, tolerance), comparison.regions), it)
            }
            ParityPair(name, b, a, comparison, diff)
        }
        report.absoluteFile.parentFile?.mkdirs()
        report.writeText(markdown(before, after, pairs, threshold))
        return pairs
    }

    fun markdown(before: File, after: File, pairs: List<ParityPair>, threshold: Double): String = buildString {
        val matched = pairs.count { it.matches(threshold) }
        val compared = pairs.count { it.comparison != null }
        appendLine("# Visual parity")
        appendLine()
        appendLine("Before: `${before.path}`  ")
        appendLine("After: `${after.path}`")
        appendLine()
        appendLine("$compared pair(s) compared: $matched match (at most $threshold% of the pixels differ), ${compared - matched} differ; " +
            "${pairs.size - compared} image(s) without a pair.")
        appendLine()
        appendLine("| Image | Mismatch | PSNR | Result | Diff (expected, actual, differences) |")
        appendLine("|---|---|---|---|---|")
        for (p in pairs) {
            val c = p.comparison
            when {
                p.before == null -> appendLine("| ${p.name} | | | only after | |")
                p.after == null -> appendLine("| ${p.name} | | | only before | |")
                c != null -> appendLine(
                    "| ${p.name} | ${fmt(c.mismatchPercent)}% | ${if (c.psnr.isInfinite()) "∞" else fmt(c.psnr) + " dB"} | " +
                        "${if (p.matches(threshold)) "match" else "differs"}${if (c.resized) " (sizes differ)" else ""} | ${p.diff?.let { "`${it.path}`" }.orEmpty()} |",
                )
            }
        }
    }
}
