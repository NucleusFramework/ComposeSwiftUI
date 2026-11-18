package dev.nucleusframework.composeswiftui.analysis

import java.io.File

/** What an agent needs to know about one rendered preview, beyond its image. */
class PreviewAnalysis(
    val tree: ViewTree?,
    val issues: List<CheckIssue>,
    val comparison: ImageComparison? = null,
    /** Expected, actual and differences side by side. */
    val comparisonImage: File? = null,
    /** The view under each region that differs from the expected image. */
    val regionViews: List<Pair<DiffRegion, ViewNode?>> = emptyList(),
)

object PreviewReport {
    /**
     * Checks the preview rendered to [image] (with its views [tree]) and, given an [expected] image, compares them,
     * writing the side-by-side image to [comparisonImage].
     */
    fun analyze(image: File, tree: ViewTree?, expected: File? = null, comparisonImage: File? = null): PreviewAnalysis {
        val actual = ImageComparer.read(image)
        val issues = tree?.let { PreviewChecks.check(it, actual) }.orEmpty()
        if (expected == null) return PreviewAnalysis(tree, issues)
        val reference = ImageComparer.read(expected)
        val comparison = ImageComparer.compare(reference, actual)
        if (comparisonImage != null && !comparison.identical) {
            ImageComparer.write(ImageComparer.sideBySide(reference, actual, ImageComparer.diffImage(reference, actual), comparison.regions), comparisonImage)
        }
        val views = tree?.let { ImageComparer.viewsOfRegions(comparison, it) }.orEmpty()
        return PreviewAnalysis(tree, issues, comparison, comparisonImage?.takeIf { !comparison.identical }, views)
    }

    /** One preview as Markdown: image, error, comparison, UI checks and (with [includeTree]) component tree. */
    fun markdown(title: String, key: String, image: File?, error: String?, analysis: PreviewAnalysis?, includeTree: Boolean = true): String = buildString {
        appendLine("## $title")
        appendLine()
        appendLine("Key: `$key`  ")
        if (image != null) appendLine("Image: `${image.path}`  ")
        if (error != null) {
            appendLine()
            appendLine("Failed:")
            appendLine()
            appendLine("```")
            appendLine(error.trim())
            appendLine("```")
        }
        if (analysis == null) return@buildString
        analysis.comparison?.let { c ->
            appendLine()
            appendLine("### Compared with the expected image")
            appendLine()
            append(c.format())
            analysis.comparisonImage?.let { appendLine("Expected, actual and differences: `${it.path}`") }
            for ((region, view) in analysis.regionViews) {
                if (view == null) continue
                appendLine("- region x=${region.x} y=${region.y} w=${region.width} h=${region.height} px: ${view.name}" +
                    (view.text?.let { " \"${it.take(40)}\"" } ?: "") + (view.location?.let { " at $it" } ?: ""))
            }
        }
        appendLine()
        appendLine("### UI checks")
        appendLine()
        if (analysis.issues.isEmpty()) appendLine("No issue found.") else analysis.issues.forEach { appendLine("- ${it.format()}") }
        analysis.tree?.takeIf { includeTree }?.let { tree ->
            appendLine()
            appendLine("### Component tree")
            appendLine()
            appendLine("```")
            append(tree.format())
            appendLine("```")
        }
        appendLine()
    }
}
