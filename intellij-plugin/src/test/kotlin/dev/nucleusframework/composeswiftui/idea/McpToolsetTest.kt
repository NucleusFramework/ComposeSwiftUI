package dev.nucleusframework.composeswiftui.idea

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.impl.util.asTools
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.nucleusframework.composeswiftui.idea.mcp.ComposeSwiftUIMcpToolset

/** The agent tools are registered on the IDE's MCP server, with the parameters agents see. */
class McpToolsetTest : BasePlatformTestCase() {
    fun testToolsAreRegisteredWithTheirSchemas() {
        val toolset = McpToolset.EP.extensionList.filterIsInstance<ComposeSwiftUIMcpToolset>().singleOrNull()
        assertNotNull("toolset registered", toolset)
        val tools = toolset!!.asTools().associateBy { it.descriptor.name }
        assertEquals(
            setOf(
                "list_swiftui_previews", "render_swiftui_preview", "render_swiftui_preview_code", "check_swiftui_preview",
                "compare_swiftui_preview_to_image", "compare_images", "verify_batch_visual_parity",
            ),
            tools.keys,
        )
        val render = tools.getValue("render_swiftui_preview").descriptor.inputSchema
        assertEquals(setOf("filePath"), render.requiredProperties)
        assertTrue(render.propertiesSchema.toString(), "previewIndex" in render.propertiesSchema.toString())
        assertTrue("component tree" in tools.getValue("render_swiftui_preview").descriptor.description)
    }
}
