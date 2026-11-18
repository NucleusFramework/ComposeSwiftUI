package dev.nucleusframework.composeswiftui.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Prints the previews and their variants, like `android preview list`: what an agent renders next with
 * `renderComposeSwiftUIPreviews -PcomposeSwiftUI.previews=<id>`.
 */
@DisableCachingByDefault(because = "Prints to the console")
abstract class ListComposeSwiftUIPreviewsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifestFile: RegularFileProperty

    /** Canonical paths of the files whose previews to list; empty: all of them. */
    @get:Input
    abstract val previewFiles: ListProperty<String>

    /** `text` or `json`. */
    @get:Input
    abstract val format: Property<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    @Suppress("UNCHECKED_CAST")
    fun list() {
        val files = previewFiles.get().toSet()
        val previews = ((JsonSlurper().parse(manifestFile.get().asFile) as Map<String, Any?>)["previews"] as List<Map<String, Any?>>)
            .filter { files.isEmpty() || runCatching { File(it["source"] as String).canonicalPath }.getOrNull() in files }
        if (format.get() == "json") {
            println(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("previews" to previews))))
            return
        }
        if (previews.isEmpty()) {
            println("No preview" + if (files.isNotEmpty()) " in ${files.joinToString()}" else "")
            return
        }
        for (preview in previews) {
            val variants = preview["variants"] as List<Map<String, Any?>>
            println("${preview["id"]}  (${preview["source"]})" + if (preview["parameterized"] == true) "  @PreviewParameter: one image per value" else "")
            for (variant in variants) {
                val settings = variant.filterKeys { it !in setOf("key", "title", "name") }
                    .filterValues { it != null && it != "" && it != 0 && it != false }
                    .entries.joinToString { "${it.key}=${it.value}" }
                println("  ${variant["key"]}  \"${variant["title"]}\"" + if (settings.isNotEmpty()) "  [$settings]" else "")
            }
        }
    }
}
