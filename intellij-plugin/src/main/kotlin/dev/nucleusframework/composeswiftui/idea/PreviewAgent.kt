package dev.nucleusframework.composeswiftui.idea

import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import dev.nucleusframework.composeswiftui.analysis.ImageComparer
import dev.nucleusframework.composeswiftui.analysis.PreviewAnalysis
import dev.nucleusframework.composeswiftui.analysis.PreviewReport
import dev.nucleusframework.composeswiftui.analysis.ViewTree
import dev.nucleusframework.composeswiftui.analysis.VisualParity
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

/** A request an agent got wrong (unknown file, no such preview): reported to it as is. */
class PreviewAgentException(message: String) : Exception(message)

/**
 * The previews as tools for AI agents, like Android Studio's `render_compose_preview`, `show_in_memory_compose_preview`,
 * `visual_lint_service_tool`, `compose_visual_comparison_judge`, `compare_images` and `verify_batch_visual_parity`.
 * Every answer is text an agent reads: image paths (to open), component trees, UI checks, errors with their lines.
 * Exposed over MCP by [ComposeSwiftUIMcpToolset].
 */
class PreviewAgent(private val project: Project) {
    /** One rendered variant, with its place among the variants of its preview. */
    class Variant(val index: Int, val preview: RenderedPreview, val module: PreviewModule) {
        val image: File? get() = preview.file?.let { File(module.previewsDir, it) }?.takeIf { it.isFile }
        val layout: File get() = File(module.previewsDir, "${preview.key}.layout.json")
    }

    // ---- list ---------------------------------------------------------------------------------------------

    /** The previews of [path] (a Kotlin file), or of the whole project when empty. */
    fun list(path: String): String {
        if (path.isBlank()) {
            val manifests = projectModules().map { it to it.manifestFile }.filter { it.second.isFile }
            if (manifests.isEmpty()) return "No preview found yet: render a file with previews first (render_swiftui_preview), or run the Gradle task generateComposeSwiftUIPreviews."
            return manifests.joinToString("\n") { (module, _) -> describe(module, null) }
        }
        val file = kotlinFile(path)
        val module = module(file)
        val declared = ReadAction.compute<List<String>, RuntimeException> {
            ktFile(file).previewFunctions().mapNotNull { it.fqName?.asString() }
        }
        return describe(module, file.path) + declared.filter { it !in manifestIds(module, file.path) }
            .joinToString("") { "$it: not built yet, render it to see its variants\n" }
    }

    private fun describe(module: PreviewModule, path: String?): String = buildString {
        @Suppress("UNCHECKED_CAST")
        val manifest = runCatching { Gson().fromJson(module.manifestFile.readText(), Map::class.java) as Map<String, Any?> }.getOrNull()
        @Suppress("UNCHECKED_CAST")
        val previews = (manifest?.get("previews") as? List<Map<String, Any?>>).orEmpty()
            .filter { path == null || File(it["source"] as String).canonicalPath == File(path).canonicalPath }
        appendLine("Module ${module.gradlePath} (${previews.size} preview function(s)):")
        for (preview in previews) {
            appendLine("${preview["functionName"]} (${preview["id"]}) in ${preview["source"]}" +
                if (preview["parameterized"] == true) ", @PreviewParameter: one image per value" else "")
            @Suppress("UNCHECKED_CAST")
            (preview["variants"] as List<Map<String, Any?>>).forEachIndexed { index, variant ->
                val settings = variant.filterKeys { it !in setOf("key", "title", "name") }
                    .filterValues { it != null && it != "" && it != 0.0 && it != false }
                    .entries.joinToString { "${it.key}=${it.value.toString().removeSuffix(".0")}" }
                appendLine("  variant $index: \"${variant["title"]}\"" + if (settings.isNotEmpty()) " [$settings]" else "")
            }
        }
    }

    // ---- render -------------------------------------------------------------------------------------------

    /**
     * Renders the preview [composable] of [path] (every preview of the file when empty), all its variants or the
     * [index]th one, and describes each: image, component tree, UI checks, or the error.
     */
    suspend fun render(path: String, composable: String, index: Int, expected: String = "", includeTree: Boolean = true): String {
        val file = kotlinFile(path)
        val module = module(file)
        val function = composable.takeIf { it.isNotBlank() }?.let { name ->
            ReadAction.compute<KtNamedFunction?, RuntimeException> { ktFile(file).declarations.filterIsInstance<KtNamedFunction>().firstOrNull { it.name == name } }
                ?: throw PreviewAgentException("No top-level function $name in $path")
        }
        if (function != null && !ReadAction.compute<Boolean, RuntimeException> { function.isComposeSwiftUIPreview() }) {
            // A plain composable: rendered through a preview that calls it, like Android Studio renders any composable.
            val (packageName, problem) = ReadAction.compute<Pair<String, String?>, RuntimeException> {
                function.containingKtFile.packageFqName.asString() to function.takeIf { it.valueParameters.any { p -> !p.hasDefaultValue() } }
                    ?.let { "$composable takes parameters: render it with render_swiftui_preview_code, calling it with sample values" }
            }
            problem?.let { throw PreviewAgentException(it) }
            val call = if (packageName.isEmpty()) "$composable()" else "$packageName.$composable()"
            return renderCode("@Preview(name = \"$composable\")\n@Composable\nfun ${composable}AgentPreview() {\n    $call\n}\n", emptyList(), path, index, expected, includeTree)
        }
        val ids = if (function != null) {
            listOf(ReadAction.compute<String, RuntimeException> { function.fqName!!.asString() })
        } else {
            (ReadAction.compute<List<String>, RuntimeException> { ktFile(file).previewFunctions().mapNotNull { it.fqName?.asString() } } +
                manifestIds(module, file.path)).distinct()
        }
        if (ids.isEmpty()) throw PreviewAgentException("No @Preview in $path. Write one, or use render_swiftui_preview_code to try code without editing the file.")
        return renderIds(module, ids, index, expected, includeTree)
    }

    /**
     * Renders throwaway [code] (a whole Kotlin file, without `package`) in the module of [contextPath], like Android
     * Studio's in-memory previews: it is compiled from the module's scratch directory, then deleted. Its `@Preview`
     * functions are rendered, or previews calling [composables] when it has none.
     */
    suspend fun renderCode(
        code: String,
        composables: List<String>,
        contextPath: String,
        index: Int = -1,
        expected: String = "",
        includeTree: Boolean = true,
    ): String {
        val context = kotlinFile(contextPath)
        val module = module(context)
        val (contextPackage, contextImports) = ReadAction.compute<Pair<String, List<String>>, RuntimeException> {
            val kt = ktFile(context)
            kt.packageFqName.asString() to kt.importDirectives.mapNotNull { it.text }
        }
        // A package of its own each time: the ids are new, so the running host can't answer from an older build.
        val packageName = listOf(contextPackage, "agentscratch", "s${System.currentTimeMillis()}").filter { it.isNotEmpty() }.joinToString(".")
        val body = code.lines().filterNot { it.trimStart().startsWith("package ") }.joinToString("\n")
        val ownImports = body.lines().filter { it.trimStart().startsWith("import ") }.map { it.trim() }.toSet()
        val imports = (listOf("androidx.compose.runtime.*", "dev.nucleusframework.composeswiftui.*").map { "import $it" } +
            contextImports + listOfNotNull(contextPackage.takeIf { it.isNotEmpty() }?.let { "import $it.*" }))
            .filter { it !in ownImports }.distinct()
        val wrappers = if (Regex("""@Preview\b""").containsMatchIn(body)) "" else {
            if (composables.isEmpty()) throw PreviewAgentException("The code has no @Preview function: add one, or name the composables to render.")
            composables.joinToString("") { "\n@Preview(name = \"$it\")\n@Composable\nfun ${it}AgentPreview() {\n    $it()\n}\n" }
        }
        val source = "package $packageName\n\n" + (ownImports + imports).joinToString("\n") + "\n\n" +
            body.lines().filterNot { it.trimStart().startsWith("import ") }.joinToString("\n").trim() + "\n" + wrappers
        val ids = ReadAction.compute<List<String>, RuntimeException> {
            KtPsiFactory(project).createFile("Scratch.kt", source).previewFunctions().mapNotNull { it.fqName?.asString() }
        }
        if (ids.isEmpty()) throw PreviewAgentException("No renderable @Preview in the code: previews are top-level, start at the beginning of a line, are @Composable and not private.")
        val dir = File(module.moduleDir, "build/$SCRATCH_DIR/${packageName.substringAfterLast('.')}")
        val scratch = File(dir, "AgentScratch.kt")
        dir.mkdirs()
        scratch.writeText(source)
        try {
            return "Scratch file (deleted after rendering): ${scratch.path}\n" + renderIds(module, ids, index, expected, includeTree, scratch)
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun renderIds(
        module: PreviewModule,
        ids: List<String>,
        index: Int,
        expected: String,
        includeTree: Boolean,
        scratch: File? = null,
    ): String {
        val outcome = renderNow(module, ids.toSet())
        if (!outcome.success) {
            return buildString {
                appendLine("The build failed (${outcome.milliseconds} ms):")
                for (problem in outcome.problems) {
                    // A line of the scratch file: shown as its line in the code given (after the header lines added).
                    appendLine("- " + (problem.path?.let { "$it:${problem.line}:${problem.column}: " } ?: "") + problem.message)
                }
                if (scratch != null) appendLine("\nThe scratch file was:\n```kotlin\n${scratch.readText()}```")
            }
        }
        val results = PreviewFiles.results(module).filter { it.id in ids }.sortedWith(compareBy({ ids.indexOf(it.id) }, { variantOrder(it.key) }))
        if (results.isEmpty()) return "Nothing was rendered for ${ids.joinToString()} (the preview may not be in a source set compiled into every Apple framework)."
        var variants = ids.flatMap { id -> results.filter { it.id == id }.mapIndexed { i, r -> Variant(i, r, module) } }
        if (index >= 0) {
            variants = ids.mapNotNull { id -> variants.filter { it.preview.id == id }.getOrNull(index) }
            if (variants.isEmpty()) throw PreviewAgentException("No variant $index: ${results.size} variant(s) rendered (indices from 0)")
        }
        val reference = expected.takeIf { it.isNotBlank() }?.let { resolve(it) }?.also {
            if (!it.isFile) throw PreviewAgentException("No image at $it")
        }
        return buildString {
            appendLine("Rendered in ${outcome.milliseconds} ms${if (outcome.built) "" else " (the running host, no build needed)"}.")
            appendLine()
            variants.forEachIndexed { i, variant -> append(describe(variant, reference.takeIf { i == 0 }, includeTree)) }
            if (reference != null && variants.size > 1) appendLine("Only variant ${variants.first().index} was compared with the expected image: pass previewIndex to pick another.")
        }
    }

    /** One variant as Markdown, analyzed (and compared with [expected]). */
    fun describe(variant: Variant, expected: File? = null, includeTree: Boolean = true): String {
        val preview = variant.preview
        val image = variant.image
        val analysis = image?.let { analyze(variant, it, expected) }
        val header = "variant ${variant.index} of ${preview.id}, ${preview.width}×${preview.height} pt" +
            (preview.device.takeIf { it.isNotEmpty() }?.let { " on $it" } ?: "")
        return PreviewReport.markdown("${preview.title} ($header)", preview.key, image, preview.error, analysis, includeTree)
    }

    fun analyze(variant: Variant, image: File, expected: File?): PreviewAnalysis {
        val tree = layoutTree(variant.layout)
        return PreviewReport.analyze(image, tree, expected, File(variant.module.previewsDir, "${variant.preview.key}.compare.png"))
    }

    /** Renders through the IDE's renderer (the running host when the code didn't change, else Gradle), and waits. */
    private suspend fun renderNow(module: PreviewModule, ids: Set<String>): RenderOutcome {
        val done = CompletableDeferred<RenderOutcome>()
        ApplicationManager.getApplication().invokeAndWait {
            // The agent edits files through the IDE: what it wrote must be on disk for Gradle.
            FileDocumentManager.getInstance().saveAllDocuments()
            project.service<PreviewRenderer>().render(module, ids) { done.complete(it) }
        }
        return done.await()
    }

    // ---- comparisons --------------------------------------------------------------------------------------

    /** Android Studio's `compare_images`: metrics as JSON, and the diff image when [diffPath] is given. */
    fun compareImages(path1: String, path2: String, diffPath: String): String {
        val expected = ImageComparer.read(resolve(path1).also { if (!it.isFile) throw PreviewAgentException("No image at $it") })
        val actual = ImageComparer.read(resolve(path2).also { if (!it.isFile) throw PreviewAgentException("No image at $it") })
        val comparison = ImageComparer.compare(expected, actual)
        val diff = diffPath.takeIf { it.isNotBlank() }?.let(::resolve)?.also {
            ImageComparer.write(ImageComparer.sideBySide(expected, actual, ImageComparer.diffImage(expected, actual), comparison.regions), it)
        }
        return comparison.toJson(listOfNotNull(diff?.let { "diffImage" to it.path }).toMap()) + "\n\n" + comparison.format()
    }

    /** Android Studio's `verify_batch_visual_parity`. */
    fun verifyParity(before: String, after: String, report: String, threshold: Double): String {
        val beforeDir = resolve(before).also { if (!it.isDirectory) throw PreviewAgentException("No folder at $it") }
        val afterDir = resolve(after).also { if (!it.isDirectory) throw PreviewAgentException("No folder at $it") }
        val reportFile = resolve(report.ifBlank { "build/composeSwiftUI/parity/report.md" })
        val pairs = VisualParity.verify(beforeDir, afterDir, reportFile, threshold)
        val compared = pairs.filter { it.comparison != null }
        return "{\"pairs\":${compared.size},\"matches\":${compared.count { it.matches(threshold) }},\"mismatches\":${compared.count { !it.matches(threshold) }}," +
            "\"unpaired\":${pairs.size - compared.size},\"report\":\"${reportFile.path.replace("\\", "\\\\").replace("\"", "\\\"")}\"}\n\n" + reportFile.readText()
    }

    // ---- helpers ------------------------------------------------------------------------------------------

    private fun resolve(path: String): File {
        val file = File(path)
        return if (file.isAbsolute) file else File(project.basePath ?: "", path)
    }

    private fun kotlinFile(path: String): VirtualFile {
        val file = resolve(path)
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.takeIf { it.extension == "kt" }
            ?: throw PreviewAgentException("No Kotlin file at $file")
    }

    private fun ktFile(file: VirtualFile) = PsiManager.getInstance(project).findFile(file) as? KtFile
        ?: throw PreviewAgentException("Not a Kotlin file: ${file.path}")

    private fun module(file: VirtualFile) = PreviewModule.of(project, file)
        ?: throw PreviewAgentException("${file.path} is not in a Gradle module that applies the Compose SwiftUI plugin (or the Gradle project isn't imported yet)")

    private fun manifestIds(module: PreviewModule, path: String) = PreviewFiles.manifestIds(module, path)

    /** The modules of the project that rendered or generated previews once. */
    private fun projectModules(): List<PreviewModule> {
        val base = project.basePath?.let(::File) ?: return emptyList()
        return base.walkTopDown()
            .onEnter { it.name !in setOf(".git", ".gradle", ".idea", "node_modules", ".kotlin") && (it.name != "build" || it.parentFile == null) }
            .maxDepth(5)
            .filter { File(it, "build/generated/composeSwiftUI/previews/previews.json").isFile }
            .map { PreviewModule(base.path, it.path, ":" + it.relativeTo(base).invariantSeparatorsPath.replace('/', ':').removePrefix(":")) }
            .toList()
    }

    companion object {
        /** Same as the Gradle plugin's `SCRATCH_DIR`, under the module's build directory. */
        const val SCRATCH_DIR = "composeSwiftUI/scratch"

        /** `id~2#1` → (2, 1): variants in declaration order, then parameter values. */
        fun variantOrder(key: String): Int {
            val variant = key.substringAfter('~', "0").substringBefore('#').toIntOrNull() ?: 0
            val value = key.substringAfter('#', "0").toIntOrNull() ?: 0
            return variant * 10_000 + value
        }

        @Suppress("UNCHECKED_CAST")
        fun layoutTree(file: File): ViewTree? = file.takeIf { it.isFile }?.let {
            runCatching { ViewTree.parse(Gson().fromJson(it.readText(), Map::class.java) as Map<String, Any?>) }.getOrNull()
        }
    }
}
