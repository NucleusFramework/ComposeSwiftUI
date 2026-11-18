package dev.nucleusframework.composeswiftui.idea.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import dev.nucleusframework.composeswiftui.idea.PreviewAgent
import dev.nucleusframework.composeswiftui.idea.PreviewAgentException
import kotlinx.coroutines.currentCoroutineContext

/**
 * The Compose SwiftUI previews as tools of the IDE's MCP server (Settings › Tools › MCP Server), for Claude Code,
 * Junie, Codex or any agent connected to the IDE: Android Studio's preview tools, rendered by real SwiftUI.
 * Loaded only when the MCP Server plugin is (see `compose-swiftui-mcp.xml`).
 */
class ComposeSwiftUIMcpToolset : McpToolset {
    @McpTool
    @McpDescription(
        """
        |Lists the Compose SwiftUI @Preview functions of a Kotlin file (or of the whole project) with their variants:
        |the index of each variant (multipreview annotations, repeated @Preview, @PreviewParameter values) is the
        |previewIndex of the other tools. Compose SwiftUI previews render real SwiftUI on the iOS simulator.
        """,
    )
    suspend fun list_swiftui_previews(
        @McpDescription("Kotlin file, absolute or relative to the project root. Empty: every module that rendered previews once")
        filePath: String = "",
    ): String = call { list(filePath) }

    @McpTool
    @McpDescription(
        """
        |Renders a Compose SwiftUI preview with real SwiftUI on the iOS simulator and describes it: the PNG path (open it
        |to see the preview), the component tree (every view with its source line, frame in points and text) and UI
        |checks (touch targets, text contrast, small text, overlaps, clipping). On failure: compiler errors with their
        |lines, or the Kotlin exception of the preview. Saves open files and builds what changed first.
        |Render once before a visual change to know the "before" state, then after each edit to verify it.
        |composableName may also be a @Composable without parameters that is not a preview.
        """,
    )
    suspend fun render_swiftui_preview(
        @McpDescription("Kotlin file declaring the preview, absolute or relative to the project root")
        filePath: String,
        @McpDescription("Simple name of the preview function. Empty: every preview of the file")
        composableName: String = "",
        @McpDescription("0-based variant to render (see list_swiftui_previews). -1: all of them")
        previewIndex: Int = -1,
    ): String = call { render(filePath, composableName, previewIndex) }

    @McpTool
    @McpDescription(
        """
        |Renders throwaway Compose SwiftUI code without editing the project, like an in-memory preview: prototype a UI,
        |try several variants (light and dark with @Preview(colorScheme = PreviewColorScheme.Dark), Dynamic Type sizes,
        |iPhone and iPad with width/height, right-to-left) or call a composable of the project with sample values.
        |The code is a whole Kotlin file without a package line; it sees the declarations of the context file's package
        |and imports, plus androidx.compose.runtime and dev.nucleusframework.composeswiftui. Its @Preview functions are
        |rendered (top-level, not private), or previews calling composableNames when it has none.
        """,
    )
    suspend fun render_swiftui_preview_code(
        @McpDescription("Kotlin code with @Preview @Composable functions (no package line)")
        code: String,
        @McpDescription("A Kotlin file of the module the code belongs to: gives the dependencies, package and imports")
        contextFilePath: String,
        @McpDescription("Composables without parameters to preview when the code has no @Preview")
        composableNames: List<String> = emptyList(),
    ): String = call { renderCode(code, composableNames, contextFilePath) }

    @McpTool
    @McpDescription(
        """
        |UI checks of a rendered Compose SwiftUI preview, like Android Studio's UI Check: accessibility (touch targets
        |under 44 pt), color contrast of texts (WCAG AA, estimated from the image), typography (text under 11 pt) and
        |layout (texts overlapping, views clipped by the preview's width). Renders the preview first. Use variants
        |(Dynamic Type sizes, screen sizes, right-to-left) to check adaptive layouts.
        """,
    )
    suspend fun check_swiftui_preview(
        @McpDescription("Kotlin file declaring the preview")
        filePath: String,
        @McpDescription("Simple name of the preview function. Empty: every preview of the file")
        composableName: String = "",
        @McpDescription("0-based variant. -1: all of them")
        previewIndex: Int = -1,
    ): String = call { render(filePath, composableName, previewIndex, includeTree = false) }

    @McpTool
    @McpDescription(
        """
        |Renders a Compose SwiftUI preview and compares it with an expected image (a design mock, a screenshot of the
        |app, an earlier render): mismatch %, PSNR, the regions that differ with the view and source line under each,
        |and a side-by-side image (expected, actual, differences in red) to open. Iterate on the code until it matches.
        |Give the preview the size of the expected image (@Preview width and height, in points) for a fair comparison.
        """,
    )
    suspend fun compare_swiftui_preview_to_image(
        @McpDescription("Kotlin file declaring the preview")
        filePath: String,
        @McpDescription("Simple name of the preview function")
        composableName: String,
        @McpDescription("PNG or JPEG of the expected UI, absolute or relative to the project root")
        expectedImagePath: String,
        @McpDescription("0-based variant to compare")
        previewIndex: Int = 0,
    ): String = call { render(filePath, composableName, previewIndex, expectedImagePath) }

    @McpTool
    @McpDescription(
        """
        |Compares two images pixel by pixel: JSON with the size, mismatch %, MSE, RMSE, PSNR and the regions that
        |differ. With diffOutputPath, writes expected, actual and differences side by side there.
        """,
    )
    suspend fun compare_images(
        @McpDescription("Expected image")
        image1Path: String,
        @McpDescription("Actual image (scaled to the expected one's size when they differ)")
        image2Path: String,
        @McpDescription("PNG to write the comparison to. Empty: none")
        diffOutputPath: String = "",
    ): String = call { compareImages(image1Path, image2Path, diffOutputPath) }

    @McpTool
    @McpDescription(
        """
        |Compares two folders of screenshots, paired by file name (e.g. build/composeSwiftUI/previews copied before a
        |change, and the same folder after it), and writes a Markdown report with a diff image per pair that differs.
        |Returns the counts and the report.
        """,
    )
    suspend fun verify_batch_visual_parity(
        @McpDescription("Folder of the expected PNGs")
        beforeDirectoryPath: String,
        @McpDescription("Folder of the new PNGs")
        afterDirectoryPath: String,
        @McpDescription("Markdown report to write. Empty: build/composeSwiftUI/parity/report.md")
        reportOutputPath: String = "",
        @McpDescription("Largest share of differing pixels, in percent, for a pair to match")
        thresholdPercent: Double = 0.5,
    ): String = call { verifyParity(beforeDirectoryPath, afterDirectoryPath, reportOutputPath, thresholdPercent) }

    private suspend fun call(block: suspend PreviewAgent.() -> String): String {
        val agent = PreviewAgent(currentCoroutineContext().project)
        return try {
            agent.block()
        } catch (e: PreviewAgentException) {
            mcpFail(e.message ?: "Failed")
        }
    }
}
