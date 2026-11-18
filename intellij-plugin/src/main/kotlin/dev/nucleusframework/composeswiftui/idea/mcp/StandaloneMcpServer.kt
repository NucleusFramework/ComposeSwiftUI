package dev.nucleusframework.composeswiftui.idea.mcp

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.nucleusframework.composeswiftui.idea.McpServerMode
import dev.nucleusframework.composeswiftui.idea.PreviewAgent
import dev.nucleusframework.composeswiftui.idea.PreviewAgentException
import dev.nucleusframework.composeswiftui.idea.PreviewSettings
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors

/**
 * The agent tools ([PreviewTools]) on an MCP server of the plugin's own, for IDEs without JetBrains' MCP Server
 * plugin, like Android Studio, whose agent only relays its own tools: Gemini (`mcp.json`), Claude Code, Codex or any
 * MCP client connects to [url] directly. Streamable HTTP, answering each request with a JSON body; bound to the
 * loopback interface, refusing other origins (DNS rebinding).
 */
@Service(Service.Level.APP)
class StandaloneMcpServer : Disposable {
    private var server: HttpServer? = null
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /** The endpoint, while the server runs. */
    val url: String? get() = server?.let { "http://127.0.0.1:${it.address.port}$PATH" }

    /** Starts, stops or moves the server to follow the settings. */
    @Synchronized
    fun applySettings() {
        val options = PreviewSettings.options
        val wanted = when (options.mcpServerMode) {
            McpServerMode.ON -> true
            McpServerMode.OFF -> false
            McpServerMode.AUTO -> !ideMcpServerEnabled()
        }
        val current = server
        if (current != null && (!wanted || current.address.port !in options.mcpPort until options.mcpPort + PORT_ATTEMPTS)) stop()
        if (wanted && server == null) start(options.mcpPort)
    }

    private fun start(port: Int) {
        // The next ports when another IDE window (or IDE) already serves on this one.
        for (candidate in port until port + PORT_ATTEMPTS) {
            val created = try {
                HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), candidate), 0)
            } catch (_: BindException) {
                continue
            }
            created.createContext(PATH) { exchange ->
                exchange.use {
                    try {
                        handle(it)
                    } catch (e: Throwable) {
                        // A reply rather than a dropped connection, which clients report as an empty response.
                        LOG.warn("Compose SwiftUI MCP: request failed", e)
                        runCatching { it.reply(500, gson.toJson(error(JsonNull.INSTANCE, -32603, "${e::class.simpleName}: ${e.message}"))) }
                    }
                }
            }
            // Renders take seconds: one thread per call, so a long render doesn't hold the others.
            created.executor = Executors.newCachedThreadPool { Thread(it, "Compose SwiftUI MCP").apply { isDaemon = true } }
            created.start()
            server = created
            LOG.info("Compose SwiftUI MCP server on $url")
            return
        }
        LOG.warn("Compose SwiftUI MCP server: ports $port to ${port + PORT_ATTEMPTS - 1} are taken")
    }

    @Synchronized
    private fun stop() {
        server?.stop(0)
        server = null
    }

    override fun dispose() = stop()

    private fun handle(exchange: HttpExchange) {
        if (!localOrigin(exchange.requestHeaders.getFirst("Origin"))) return exchange.reply(403, "")
        when (exchange.requestMethod) {
            // No server-initiated messages: no stream to open (the spec's answer is 405).
            "GET", "DELETE" -> {
                exchange.responseHeaders.add("Allow", "POST")
                exchange.reply(405, "")
            }
            "POST" -> {
                val body = runCatching { JsonParser.parseString(exchange.requestBody.bufferedReader().readText()) }.getOrNull()
                    ?: return exchange.reply(400, gson.toJson(error(JsonNull.INSTANCE, -32700, "Parse error")))
                val messages = if (body.isJsonArray) body.asJsonArray.toList() else listOf(body)
                val responses = messages.mapNotNull { message -> message.takeIf { it.isJsonObject }?.asJsonObject?.let(::respond) }
                when {
                    // Notifications and responses only.
                    responses.isEmpty() -> exchange.reply(202, "")
                    body.isJsonArray -> exchange.reply(200, gson.toJson(JsonArray().apply { responses.forEach(::add) }))
                    else -> exchange.reply(200, gson.toJson(responses.single()))
                }
            }
            else -> exchange.reply(405, "")
        }
    }

    /** The response to a JSON-RPC request, or null for a notification. */
    private fun respond(message: JsonObject): JsonObject? {
        val id = message.get("id") ?: return null
        val params = message.getAsJsonObject("params") ?: JsonObject()
        return when (message.get("method")?.asString) {
            "initialize" -> result(id, initialize(params))
            "ping" -> result(id, JsonObject())
            "tools/list" -> result(id, JsonObject().apply { add("tools", JsonArray().apply { PreviewTools.all.forEach { add(schema(it)) } }) })
            "tools/call" -> result(id, call(params))
            else -> error(id, -32601, "Method not found")
        }
    }

    private fun initialize(params: JsonObject) = JsonObject().apply {
        val requested = params.get("protocolVersion")?.asString
        addProperty("protocolVersion", requested?.takeIf { it in PROTOCOL_VERSIONS } ?: PROTOCOL_VERSIONS.first())
        add("capabilities", JsonObject().apply { add("tools", JsonObject().apply { addProperty("listChanged", false) }) })
        add(
            "serverInfo",
            JsonObject().apply {
                addProperty("name", "compose-swiftui")
                addProperty("title", "Compose SwiftUI previews")
                addProperty("version", pluginVersion())
            },
        )
        addProperty(
            "instructions",
            "Render Compose SwiftUI @Preview composables with real SwiftUI (iOS simulator or Mac), look at the PNG, the " +
                "component tree and the UI checks, edit, render again. File paths are absolute, or relative to the root of " +
                "the project open in the IDE; with several projects open, pass projectPath.",
        )
    }

    private fun schema(tool: PreviewTools.Tool) = JsonObject().apply {
        addProperty("name", tool.name)
        addProperty("description", tool.description)
        add(
            "inputSchema",
            JsonObject().apply {
                addProperty("type", "object")
                val properties = JsonObject()
                for (param in tool.params + PROJECT_PARAM) {
                    properties.add(
                        param.name,
                        JsonObject().apply {
                            addProperty("type", param.type)
                            addProperty("description", param.description)
                            if (param.type == "array") add("items", JsonObject().apply { addProperty("type", "string") })
                            param.default?.let { add("default", gson.toJsonTree(it)) }
                        },
                    )
                }
                add("properties", properties)
                add("required", JsonArray().apply { tool.params.filter { it.default == null }.forEach { add(it.name) } })
            },
        )
    }

    /** Runs a tool: failures are tool results with `isError`, for the agent to read. */
    private fun call(params: JsonObject): JsonObject {
        val tool = PreviewTools.all.firstOrNull { it.name == params.get("name")?.asString }
            ?: return toolResult("Unknown tool ${params.get("name")}", isError = true)
        @Suppress("UNCHECKED_CAST")
        val given = params.getAsJsonObject("arguments")?.let { gson.fromJson(it, Map::class.java) as Map<String, Any?> }.orEmpty()
        val missing = tool.params.filter { it.default == null && given[it.name]?.toString().isNullOrEmpty() }
        if (missing.isNotEmpty()) return toolResult("Missing ${missing.joinToString { it.name }}", isError = true)
        val args = tool.params.associate { it.name to (given[it.name] ?: it.default) }
        return try {
            val project = project(given["projectPath"]?.toString().orEmpty(), tool.params.filter { it.isPath }.map { args[it.name].toString() })
            toolResult(runBlocking { PreviewAgent(project).(tool.run)(PreviewTools.Args(args)) })
        } catch (e: PreviewAgentException) {
            toolResult(e.message ?: "Failed", isError = true)
        } catch (e: Throwable) {
            LOG.warn("Compose SwiftUI MCP: ${tool.name} failed", e)
            toolResult("${e::class.simpleName}: ${e.message}", isError = true)
        }
    }

    /** The open project of the call: [projectPath], else the one containing an absolute path argument, else the only one. */
    private fun project(projectPath: String, paths: List<String>): Project {
        val projects = ProjectManager.getInstance().openProjects.filter { !it.isDefault && !it.isDisposed && it.basePath != null }
        if (projects.isEmpty()) throw PreviewAgentException("No project is open in the IDE")
        fun contains(project: Project, path: String) = File(path).canonicalPath.startsWith(File(project.basePath!!).canonicalPath + File.separator)
        if (projectPath.isNotEmpty()) {
            return projects.firstOrNull { File(it.basePath!!).canonicalPath == File(projectPath).canonicalPath || contains(it, projectPath) }
                ?: throw PreviewAgentException("No open project at $projectPath. Open projects: ${projects.joinToString { it.basePath!! }}")
        }
        // The innermost project, for projects nested in others.
        paths.filter { File(it).isAbsolute }.firstNotNullOfOrNull { path ->
            projects.filter { contains(it, path) }.maxByOrNull { it.basePath!!.length }
        }?.let { return it }
        return projects.singleOrNull()
            ?: throw PreviewAgentException("Several projects are open: pass projectPath, or absolute paths. Open projects: ${projects.joinToString { it.basePath!! }}")
    }

    private fun toolResult(text: String, isError: Boolean = false) = JsonObject().apply {
        add("content", JsonArray().apply { add(JsonObject().apply { addProperty("type", "text"); addProperty("text", text) }) })
        addProperty("isError", isError)
    }

    private fun result(id: JsonElement, result: JsonObject) = JsonObject().apply {
        addProperty("jsonrpc", "2.0")
        add("id", id)
        add("result", result)
    }

    private fun error(id: JsonElement, code: Int, message: String) = JsonObject().apply {
        addProperty("jsonrpc", "2.0")
        add("id", id)
        add("error", JsonObject().apply { addProperty("code", code); addProperty("message", message) })
    }

    private fun HttpExchange.reply(status: Int, body: String) {
        val bytes = body.toByteArray()
        if (bytes.isNotEmpty()) responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) }
    }

    companion object {
        private val LOG = logger<StandaloneMcpServer>()
        const val PATH = "/mcp"
        const val DEFAULT_PORT = 64360
        private const val PORT_ATTEMPTS = 10
        private val PROTOCOL_VERSIONS = listOf("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05")
        private val PROJECT_PARAM = PreviewTools.Param(
            "projectPath", "string", "Root of the open project to use, when several are open. Empty: found from the paths", "",
        )

        val instance: StandaloneMcpServer get() = service()

        /** JetBrains' MCP Server plugin, which serves the tools itself (see `compose-swiftui-mcp.xml`). */
        fun ideMcpServerEnabled(): Boolean {
            val id = PluginId.getId("com.intellij.mcpServer")
            return PluginManagerCore.getPlugin(id) != null && !PluginManagerCore.isDisabled(id)
        }

        private fun pluginVersion() =
            PluginManagerCore.getPlugin(PluginId.getId("dev.nucleusframework.compose-swiftui"))?.version ?: "dev"

        /** A browser page may only reach the server from a local page; MCP clients send no Origin. */
        internal fun localOrigin(origin: String?): Boolean {
            if (origin == null || origin == "null") return origin == null
            val host = runCatching { URI(origin).host }.getOrNull() ?: return false
            return host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host == "::1"
        }
    }
}

/** Starts the server with the first project (an application service is created on demand). */
class StandaloneMcpServerStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        StandaloneMcpServer.instance.applySettings()
    }
}
