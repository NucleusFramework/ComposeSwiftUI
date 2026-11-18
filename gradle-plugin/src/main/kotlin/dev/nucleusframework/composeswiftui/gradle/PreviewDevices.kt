package dev.nucleusframework.composeswiftui.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * The `device = "…"` arguments written in the preview sources, so `renderComposeSwiftUIPreviews` only links the
 * frameworks it renders with (the macOS one only for `device = "mac"`). A text scan, cheap enough for every build: a
 * device given through a constant is not seen, which the task reports with what to do.
 */
abstract class PreviewDevicesSource : ValueSource<Set<String>, PreviewDevicesSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val sources: ConfigurableFileCollection
    }

    override fun obtain(): Set<String> =
        parameters.sources.asFileTree.matching { it.include("**/*.kt") }.files
            .flatMapTo(sortedSetOf()) { file -> DEVICE.findAll(file.readText()).map { it.groupValues[1] }.asIterable() }

    private companion object {
        val DEVICE = Regex("""\bdevice\s*=\s*"([^"]*)"""")
    }
}
