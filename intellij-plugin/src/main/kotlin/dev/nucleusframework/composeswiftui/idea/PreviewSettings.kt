package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import dev.nucleusframework.composeswiftui.idea.mcp.StandaloneMcpServer
import dev.nucleusframework.composeswiftui.idea.remote.SshMacShell
import java.awt.datatransfer.StringSelection

/** When the previews render without a click. */
enum class AutoRender(val label: String) {
    MANUAL("Only when asked"),
    ON_SAVE("When a Kotlin file is saved"),
    WHILE_TYPING("While typing (literals at once, other edits once the code has no error)"),
}

enum class PreviewLayout { LIST, GRID, GALLERY }

/** When the plugin serves the agent tools on its own MCP server ([StandaloneMcpServer]). */
enum class McpServerMode(val label: String) {
    AUTO("When the IDE has no MCP server of its own (Android Studio)"),
    ON("Always"),
    OFF("Never"),
}

@Service(Service.Level.APP)
@State(name = "ComposeSwiftUIPreview", storages = [Storage("composeSwiftUIPreview.xml")])
class PreviewSettings : SimplePersistentStateComponent<PreviewSettings.Options>(Options()) {
    class Options : BaseState() {
        var autoRender by enum(AutoRender.ON_SAVE)
        var typingDelayMs by property(1000)
        var incrementalNative by property(true)
        // The background Gradle runs pass the same arguments every time: the cache is reused.
        var configurationCache by property(true)
        var settleMs by property(1500)
        var layout by enum(PreviewLayout.LIST)
        var mcpServerMode by enum(McpServerMode.AUTO)
        var mcpPort by property(StandaloneMcpServer.DEFAULT_PORT)
        // `ssh` destination of the Mac that runs the Apple tools when the IDE is not on a Mac.
        var remoteMac by string("")
        // Use the remote Mac even on a Mac (to try it, or to offload a small Mac).
        var remoteMacOnMac by property(false)
    }

    companion object {
        val options: Options get() = service<PreviewSettings>().state
    }
}

/** Settings › Tools › Compose SwiftUI Previews. */
class PreviewConfigurable : BoundConfigurable("Compose SwiftUI Previews") {
    override fun createPanel(): DialogPanel {
        val options = PreviewSettings.options
        return panel {
            group("Rendering") {
                row("Render previews:") {
                    comboBox(AutoRender.entries, textListCellRenderer { it?.label })
                        .bindItem(options::autoRender.toNullableProperty())
                }
                row("Pause before rendering while typing (ms):") {
                    intTextField(200..10_000).bindIntText(options::typingDelayMs)
                }
                row("Longest wait for a preview to settle (ms):") {
                    intTextField(100..10_000).bindIntText(options::settleMs)
                        .comment("A preview is captured as soon as it stops changing; endless animations wait this long.")
                }
                row {
                    checkBox("Kotlin/Native incremental compilation (faster framework links)")
                        .bindSelected(options::incrementalNative)
                }
                row {
                    checkBox("Gradle configuration cache (skips the configuration of the build on every render)")
                        .bindSelected(options::configurationCache)
                        .comment("Turned off automatically for a build that doesn't support it.")
                }
            }
            group("Remote Mac") {
                lateinit var destinationField: JBTextField
                row("SSH destination:") {
                    destinationField = textField().bindText({ options.remoteMac.orEmpty() }, { options.remoteMac = it.trim() })
                        .comment(
                            "On Linux or Windows, the Mac that builds for Apple platforms, renders the previews and runs the " +
                                "simulator: an alias of ~/.ssh/config or user@host, with key authentication. The project is " +
                                "synchronized to it with rsync.",
                        ).component
                }
                row {
                    checkBox("Use it on a Mac too").bindSelected(options::remoteMacOnMac)
                }
                row {
                    button("Test Connection") {
                        val destination = destinationField.text.trim()
                        val result = if (destination.isEmpty()) null else SshMacShell(destination).check()
                        Messages.showInfoMessage(
                            when {
                                result == null -> "Set the SSH destination first."
                                result.success -> "Connected: macOS ${result.output.trim().replace("\n", ", ")}"
                                else -> "Failed:\n${result.output.trim()}"
                            },
                            "Remote Mac",
                        )
                    }
                }
            }
            group("AI Agents") {
                row("Serve the preview tools over MCP:") {
                    comboBox(McpServerMode.entries, textListCellRenderer { it?.label })
                        .bindItem(options::mcpServerMode.toNullableProperty())
                }
                row("Port:") {
                    intTextField(1024..65_535).bindIntText(options::mcpPort)
                        .comment("The next free port is used when it is taken (another IDE window).")
                }
                row {
                    comment(serverStatus())
                }
                row {
                    button("Copy Android Studio mcp.json Entry") { copy(McpClientConfig.androidStudio()) }
                    button("Copy Claude Code Command") { copy(McpClientConfig.claudeCode()) }
                }.comment(
                    "Android Studio: Settings › Tools › AI › MCP Servers, add it to mcp.json. In IntelliJ IDEA with the MCP Server " +
                        "plugin on, its own server already has these tools.",
                )
            }
        }
    }

    override fun apply() {
        super.apply()
        StandaloneMcpServer.instance.applySettings()
    }

    private fun serverStatus(): String = StandaloneMcpServer.instance.url?.let { "Running at $it" }
        ?: if (StandaloneMcpServer.ideMcpServerEnabled()) "Not running: the IDE's MCP Server serves the tools." else "Not running."

    private fun copy(text: String) = CopyPasteManager.getInstance().setContents(StringSelection(text))
}

/** What an MCP client needs to reach [StandaloneMcpServer]: its running URL, else the configured port. */
object McpClientConfig {
    private fun url(): String = StandaloneMcpServer.instance.url
        ?: "http://127.0.0.1:${PreviewSettings.options.mcpPort}${StandaloneMcpServer.PATH}"

    fun androidStudio() =
        "\"compose-swiftui\": {\n  \"httpUrl\": \"${url()}\"\n}"

    fun claudeCode() = "claude mcp add --transport http compose-swiftui ${url()}"
}
