package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.impl.util.asTools
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.nucleusframework.composeswiftui.idea.mcp.ComposeSwiftUIMcpToolset
import dev.nucleusframework.composeswiftui.idea.mcp.StandaloneMcpServer
import java.awt.image.BufferedImage
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * The plugin's own MCP server, over real HTTP like Android Studio's agent or Claude Code would. With
 * `-Dcsui.e2e.mac.module=<…/samples/macos-app/shared>` (rendered once beforehand), a preview renders through it.
 */
class StandaloneMcpServerTest : BasePlatformTestCase() {
    private val http = HttpClient.newHttpClient()
    private lateinit var url: String

    override fun setUp() {
        super.setUp()
        val options = PreviewSettings.options
        options.mcpServerMode = McpServerMode.ON
        options.mcpPort = ServerSocket(0).use { it.localPort }
        StandaloneMcpServer.instance.applySettings()
        url = StandaloneMcpServer.instance.url!!
    }

    override fun tearDown() {
        try {
            PreviewSettings.options.mcpServerMode = McpServerMode.OFF
            StandaloneMcpServer.instance.applySettings()
            PreviewSettings.options.mcpServerMode = McpServerMode.AUTO
            PreviewSettings.options.mcpPort = StandaloneMcpServer.DEFAULT_PORT
        } finally {
            super.tearDown()
        }
    }

    fun testSpeaksMcpOverHttp() {
        val init = rpc("initialize", """{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}""")
        val initResult = init.getAsJsonObject("result")
        assertEquals("2025-06-18", initResult.get("protocolVersion").asString)
        assertEquals("compose-swiftui", initResult.getAsJsonObject("serverInfo").get("name").asString)
        val instructions = initResult.get("instructions").asString
        assertTrue(instructions, "iOS and macOS" in instructions && "run_apple_gradle" in instructions && "build_xcode_project" in instructions)
        assertEquals(202, post("""{"jsonrpc":"2.0","method":"notifications/initialized"}""").statusCode())
        assertEquals(405, http.send(HttpRequest.newBuilder(URI(url)).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode())
        assertEquals(403, post("""{"jsonrpc":"2.0","id":9,"method":"ping"}""", origin = "https://evil.example").statusCode())
        assertEquals(200, post("""{"jsonrpc":"2.0","id":9,"method":"ping"}""", origin = "http://localhost:3000").statusCode())
        assertEquals(-32601, rpc("nope", "{}").getAsJsonObject("error").get("code").asInt)

        // The same tools, with the same descriptions, as on the IDE's MCP server.
        val tools = rpc("tools/list", "{}").getAsJsonObject("result").getAsJsonArray("tools").map { it.asJsonObject }
        val ide = McpToolset.EP.extensionList.filterIsInstance<ComposeSwiftUIMcpToolset>().single().asTools().associateBy { it.descriptor.name }
        assertEquals(ide.keys, tools.map { it.get("name").asString }.toSet())
        val render = tools.single { it.get("name").asString == "render_swiftui_preview" }
        assertEquals(listOf("filePath"), render.getAsJsonObject("inputSchema").getAsJsonArray("required").map { it.asString })
        assertTrue(render.toString(), "component tree" in render.get("description").asString && "|" !in render.get("description").asString)

        val appleGradle = tools.single { it.get("name").asString == "run_apple_gradle" }
        assertEquals(listOf("tasks"), appleGradle.getAsJsonObject("inputSchema").getAsJsonArray("required").map { it.asString })
        assertTrue(appleGradle.toString(), "iOS and macOS" in appleGradle.get("description").asString && "|" !in appleGradle.get("description").asString)

        val xcodeBuild = tools.single { it.get("name").asString == "build_xcode_project" }
        assertEquals(emptyList<String>(), xcodeBuild.getAsJsonObject("inputSchema").getAsJsonArray("required").map { it.asString })
        assertTrue(xcodeBuild.toString(), "xcodebuild" in xcodeBuild.get("description").asString && "|" !in xcodeBuild.get("description").asString)

        val missing = call("render_swiftui_preview", "{}")
        assertTrue(missing.toString(), missing.get("isError").asBoolean && "filePath" in text(missing))

        val missingTasks = call("run_apple_gradle", """{"tasks":[]}""")
        assertTrue(missingTasks.toString(), missingTasks.get("isError").asBoolean && "tasks" in text(missingTasks))

        val badAction = call("build_xcode_project", """{"action":"archive"}""")
        assertTrue(badAction.toString(), badAction.get("isError").asBoolean && "action" in text(badAction))

        val badPlatform = call("build_xcode_project", """{"platform":"android"}""")
        assertTrue(badPlatform.toString(), badPlatform.get("isError").asBoolean && "platform" in text(badPlatform))
    }

    fun testToolRunsOnTheOpenProject() {
        val dir = kotlin.io.path.createTempDirectory("mcp").toFile()
        val image = File(dir, "a.png").apply { ImageIO.write(BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", this) }
        val result = call("compare_images", """{"image1Path":"${image.path}","image2Path":"${image.path}"}""")
        assertFalse(text(result), result.get("isError").asBoolean)
        assertTrue(text(result), "\"mismatchPercent\":0" in text(result))
    }

    /** A real render on the Mac host, through the server, like an agent in Android Studio. */
    fun testRendersAMacPreview() {
        val dir = System.getProperty("csui.e2e.mac.module") ?: return
        val root = File(dir).parentFile
        com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess.allowRootAccess(testRootDisposable, root.path, root.canonicalPath)
        val module = PreviewModule(root.path, dir, ":shared")
        PreviewModule.resolver = { if (it.path.startsWith(root.path) || it.path.startsWith(root.canonicalPath)) module else null }
        try {
            val file = File(dir, "src/commonMain/kotlin/sample/mac/MacPreviews.kt").path
            val result = call("render_swiftui_preview", """{"filePath":"$file","composableName":"CounterPreview","previewIndex":0}""")
            assertFalse(text(result), result.get("isError").asBoolean)
            assertTrue(text(result), "Button \"Increment\"" in text(result) && "sample.mac.CounterPreview.png" in text(result))
        } finally {
            PreviewModule.resolver = null
        }
    }

    private fun text(result: JsonObject) = result.getAsJsonArray("content")[0].asJsonObject.get("text").asString

    private fun call(tool: String, arguments: String): JsonObject =
        rpc("tools/call", """{"name":"$tool","arguments":$arguments}""").getAsJsonObject("result")

    private fun rpc(method: String, params: String): JsonObject =
        JsonParser.parseString(post("""{"jsonrpc":"2.0","id":1,"method":"$method","params":$params}""").body()).asJsonObject

    /** Off the EDT, which the tools need (renders report there), while pumping it. */
    private fun post(body: String, origin: String? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .apply { origin?.let { header("Origin", it) } }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val future = ApplicationManager.getApplication().executeOnPooledThread<HttpResponse<String>> {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(6)
        while (!future.isDone && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(10)
        }
        return future.get(1, TimeUnit.SECONDS)
    }
}
