package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder
import org.jetbrains.plugins.gradle.util.gradleIdentityPathOrNull
import org.jetbrains.plugins.gradle.util.gradlePath
import java.awt.image.BufferedImage
import java.io.File
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/** A Gradle module that applies the Compose SwiftUI plugin. */
data class PreviewModule(val rootProjectPath: String, val moduleDir: String, val gradlePath: String) {
    /** Images and `results.json`, written by the `renderComposeSwiftUIPreviews` task. */
    val previewsDir: File get() = File(moduleDir, "build/composeSwiftUI/previews")

    /** Every preview of the module and where it is declared, written by `generateComposeSwiftUIPreviews`. */
    val manifestFile: File get() = File(moduleDir, "build/generated/composeSwiftUI/previews/previews.json")

    fun taskPath(task: String) = if (gradlePath == ":") ":$task" else "$gradlePath:$task"

    companion object {
        /** Tests: the module of files outside a Gradle-imported project. */
        @org.jetbrains.annotations.TestOnly
        internal var resolver: ((VirtualFile) -> PreviewModule?)? = null

        fun of(project: Project, file: VirtualFile): PreviewModule? = resolver?.invoke(file)
            ?: com.intellij.openapi.application.ReadAction.compute<PreviewModule?, RuntimeException> { find(project, file) }

        private fun find(project: Project, file: VirtualFile): PreviewModule? {
            val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
            val root = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return null
            val data = CachedModuleDataFinder.findMainModuleData(module)?.data ?: return null
            // The identity path also addresses projects of included builds from the root build.
            val gradlePath = data.gradleIdentityPathOrNull ?: data.gradlePath
            return PreviewModule(root, data.linkedExternalProjectPath, gradlePath)
        }
    }
}

/** One rendered (or failed) preview variant, from `results.json`. */
data class RenderedPreview(
    val key: String,
    val id: String,
    val title: String,
    val group: String,
    val source: String,
    val device: String,
    val file: String?,
    val width: Int,
    val height: Int,
    val milliseconds: Int,
    val error: String?,
)

object PreviewFiles {
    fun results(module: PreviewModule): List<RenderedPreview> = readJson(File(module.previewsDir, "results.json"))
        ?.getAsJsonArray("previews")
        ?.map { it.asJsonObject }
        ?.map {
            RenderedPreview(
                key = it.string("key"),
                id = it.string("id"),
                title = it.string("title"),
                group = it.string("group"),
                source = it.string("source"),
                device = it.string("device"),
                file = it.string("file").ifEmpty { null },
                width = it.int("width"),
                height = it.int("height"),
                milliseconds = it.int("milliseconds"),
                error = it.string("error").ifEmpty { null },
            )
        }
        .orEmpty()

    /** Qualified names of the previews declared in [path], by the last Gradle run (finds custom multipreviews too). */
    fun manifestIds(module: PreviewModule, path: String): List<String> {
        // Canonical: Gradle and the IDE may see the project through different symlinks (/tmp, /private/tmp).
        val target = File(path).canonicalPath
        return readJson(module.manifestFile)
            ?.getAsJsonArray("previews")
            ?.map { it.asJsonObject }
            ?.filter { File(it.string("source")).canonicalPath == target }
            ?.map { it.string("id") }
            .orEmpty()
    }

    /** Groups of every preview of the module, from the manifest. */
    fun manifestGroups(module: PreviewModule): Map<String, String> = readJson(module.manifestFile)
        ?.getAsJsonArray("previews")
        ?.flatMap { preview ->
            preview.asJsonObject.getAsJsonArray("variants").map { it.asJsonObject.let { v -> v.string("key") to v.string("group") } }
        }
        ?.toMap()
        .orEmpty()

    private fun readJson(file: File): JsonObject? =
        if (file.isFile) runCatching { JsonParser.parseString(file.readText()).asJsonObject }.getOrNull() else null

    private fun JsonObject.string(name: String) = get(name)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    private fun JsonObject.int(name: String) = get(name)?.takeIf { !it.isJsonNull }?.asInt ?: 0

    private val images = ConcurrentHashMap<String, SoftReference<BufferedImage>>()

    /** The image, decoded once per version of the file. */
    fun image(file: File): BufferedImage? {
        if (!file.isFile) return null
        val key = "${file.path}:${file.lastModified()}:${file.length()}"
        images[key]?.get()?.let { return it }
        val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
        images.keys.removeIf { it.startsWith("${file.path}:") }
        images[key] = SoftReference(image)
        return image
    }
}

/** The multipreview annotations of the library. */
private val builtInMultipreviews = setOf("PreviewLightDark", "PreviewDynamicTypeSizes", "PreviewScreenSizes", "PreviewLayoutDirections")

/** Has `@Preview` (any package) or a multipreview annotation of the library. */
fun KtNamedFunction.hasPreviewAnnotation(): Boolean =
    annotationEntries.any { it.shortName?.asString().let { name -> name == "Preview" || name in builtInMultipreviews } }

/**
 * A preview the Gradle plugin renders: a top-level, non-private `@Composable` function with a preview annotation
 * and no parameter except one `@PreviewParameter`.
 */
fun KtNamedFunction.isComposeSwiftUIPreview(): Boolean =
    isTopLevel && hasPreviewAnnotation() && previewProblem() == null

/** Why the Gradle plugin won't render this preview, or null. */
fun KtNamedFunction.previewProblem(): String? {
    val annotations = annotationEntries.mapNotNull { it.shortName?.asString() }
    return when {
        !isTopLevel -> "Previews must be top-level functions"
        hasModifier(KtTokens.PRIVATE_KEYWORD) -> "Previews can't be private"
        listOf(KtTokens.SUSPEND_KEYWORD, KtTokens.EXPECT_KEYWORD, KtTokens.ABSTRACT_KEYWORD, KtTokens.OVERRIDE_KEYWORD)
            .any { hasModifier(it) } -> "Previews can't be suspend, expect, abstract or override"
        "Composable" !in annotations -> "Previews must be @Composable"
        valueParameters.size > 1 -> "A preview takes at most one parameter, annotated with @PreviewParameter"
        valueParameters.size == 1 && valueParameters.single().annotationEntries.none { it.shortName?.asString() == "PreviewParameter" } ->
            "The parameter of a preview needs @PreviewParameter(Provider::class)"
        // The Gradle plugin finds previews with a source scan that expects declarations at the start of a line.
        !startsAtLineStart() -> "Previews must start at the beginning of a line"
        else -> null
    }
}

private fun KtNamedFunction.startsAtLineStart(): Boolean {
    val text = containingFile.text
    val start = textRange.startOffset
    return start == 0 || text[start - 1] == '\n'
}

/** The `name` of the first `@Preview`, or the function name. */
fun KtNamedFunction.previewTitle(): String {
    val argument = annotationEntries.firstOrNull { it.shortName?.asString() == "Preview" }?.valueArguments
        ?.firstOrNull { it.getArgumentName() == null || it.getArgumentName()?.asName?.asString() == "name" }
    return (argument?.getArgumentExpression() as? KtStringTemplateExpression)
        ?.entries?.joinToString("") { it.text }?.takeIf { it.isNotEmpty() }
        ?: name.orEmpty()
}

/** Preview functions of the file, in source order. */
fun KtFile.previewFunctions(): List<KtNamedFunction> =
    declarations.filterIsInstance<KtNamedFunction>().filter { it.isComposeSwiftUIPreview() }

/** The top-level function [qualifiedName] of the file. */
fun KtFile.findFunction(qualifiedName: String): KtNamedFunction? =
    declarations.filterIsInstance<KtNamedFunction>().firstOrNull { it.fqName?.asString() == qualifiedName }
