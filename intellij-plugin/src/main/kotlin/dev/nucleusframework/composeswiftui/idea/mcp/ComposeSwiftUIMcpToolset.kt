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
 * Loaded only when the MCP Server plugin is (see `compose-swiftui-mcp.xml`); [StandaloneMcpServer] serves the same
 * tools ([PreviewTools]) elsewhere.
 */
class ComposeSwiftUIMcpToolset : McpToolset {
    @McpTool
    @McpDescription(PreviewTools.LIST)
    suspend fun list_swiftui_previews(
        @McpDescription(PreviewTools.LIST_FILE)
        filePath: String = "",
    ): String = call { list(filePath) }

    @McpTool
    @McpDescription(PreviewTools.RENDER)
    suspend fun render_swiftui_preview(
        @McpDescription(PreviewTools.RENDER_FILE)
        filePath: String,
        @McpDescription(PreviewTools.RENDER_NAME)
        composableName: String = "",
        @McpDescription(PreviewTools.RENDER_INDEX)
        previewIndex: Int = -1,
    ): String = call { render(filePath, composableName, previewIndex) }

    @McpTool
    @McpDescription(PreviewTools.RENDER_CODE)
    suspend fun render_swiftui_preview_code(
        @McpDescription(PreviewTools.RENDER_CODE_CODE)
        code: String,
        @McpDescription(PreviewTools.RENDER_CODE_CONTEXT)
        contextFilePath: String,
        @McpDescription(PreviewTools.RENDER_CODE_NAMES)
        composableNames: List<String> = emptyList(),
    ): String = call { renderCode(code, composableNames, contextFilePath) }

    @McpTool
    @McpDescription(PreviewTools.CHECK)
    suspend fun check_swiftui_preview(
        @McpDescription(PreviewTools.CHECK_FILE)
        filePath: String,
        @McpDescription(PreviewTools.RENDER_NAME)
        composableName: String = "",
        @McpDescription(PreviewTools.CHECK_INDEX)
        previewIndex: Int = -1,
    ): String = call { render(filePath, composableName, previewIndex, includeTree = false) }

    @McpTool
    @McpDescription(PreviewTools.COMPARE)
    suspend fun compare_swiftui_preview_to_image(
        @McpDescription(PreviewTools.CHECK_FILE)
        filePath: String,
        @McpDescription(PreviewTools.COMPARE_NAME)
        composableName: String,
        @McpDescription(PreviewTools.COMPARE_IMAGE)
        expectedImagePath: String,
        @McpDescription(PreviewTools.COMPARE_INDEX)
        previewIndex: Int = 0,
    ): String = call { render(filePath, composableName, previewIndex, expectedImagePath) }

    @McpTool
    @McpDescription(PreviewTools.COMPARE_IMAGES)
    suspend fun compare_images(
        @McpDescription(PreviewTools.COMPARE_IMAGES_1)
        image1Path: String,
        @McpDescription(PreviewTools.COMPARE_IMAGES_2)
        image2Path: String,
        @McpDescription(PreviewTools.COMPARE_IMAGES_DIFF)
        diffOutputPath: String = "",
    ): String = call { compareImages(image1Path, image2Path, diffOutputPath) }

    @McpTool
    @McpDescription(PreviewTools.PARITY)
    suspend fun verify_batch_visual_parity(
        @McpDescription(PreviewTools.PARITY_BEFORE)
        beforeDirectoryPath: String,
        @McpDescription(PreviewTools.PARITY_AFTER)
        afterDirectoryPath: String,
        @McpDescription(PreviewTools.PARITY_REPORT)
        reportOutputPath: String = "",
        @McpDescription(PreviewTools.PARITY_THRESHOLD)
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
