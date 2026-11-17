package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer

/** When the previews render without a click. */
enum class AutoRender(val label: String) {
    MANUAL("Only when asked"),
    ON_SAVE("When a Kotlin file is saved"),
    WHILE_TYPING("While typing (literals at once, other edits once the code has no error)"),
}

enum class PreviewLayout { LIST, GRID, GALLERY }

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
        }
    }
}
