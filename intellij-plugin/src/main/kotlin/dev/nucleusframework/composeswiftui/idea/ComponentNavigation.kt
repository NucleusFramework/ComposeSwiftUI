package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Rectangle
import java.io.File

/** A call that produced a view: [function] called at [offset] of [file] (a file name), on 1-based [line]. */
data class LayoutCall(val function: String?, val file: String?, val line: Int, val offset: Int, val length: Int)

/** A view of a preview: its frame in points, and the calls that produced it, innermost first. */
data class LayoutNode(val x: Double, val y: Double, val width: Double, val height: Double, val calls: List<LayoutCall>) {
    val area get() = width * height
    fun contains(px: Double, py: Double) = px >= x && py >= y && px < x + width && py < y + height
}

/** The views of a preview (`<key>.layout.json`, or the live preview's `inspect` answer), in points. */
class PreviewLayoutData(val width: Double, val height: Double, val nodes: List<LayoutNode>) {
    /** The smallest view at a point that [navigator] can show in the code. */
    fun nodeAt(x: Double, y: Double, navigator: SourceNavigator): Pair<LayoutNode, ResolvedCall>? =
        nodes.filter { it.contains(x, y) }.sortedBy { it.area }
            .firstNotNullOfOrNull { node -> navigator.resolve(node.calls)?.let { node to it } }

    companion object {
        fun read(file: File): PreviewLayoutData? =
            if (file.isFile) runCatching { parse(JsonParser.parseString(file.readText()).asJsonObject) }.getOrNull() else null

        fun parse(json: JsonObject) = PreviewLayoutData(
            json.get("width").asDouble,
            json.get("height").asDouble,
            json.getAsJsonArray("nodes").map { it.asJsonObject }.map { node ->
                LayoutNode(
                    node.get("x").asDouble, node.get("y").asDouble, node.get("width").asDouble, node.get("height").asDouble,
                    node.getAsJsonArray("calls").map { it.asJsonObject }.map { call ->
                        LayoutCall(
                            call.get("function")?.takeIf { !it.isJsonNull }?.asString,
                            call.get("file")?.takeIf { !it.isJsonNull }?.asString,
                            call.get("line").asInt,
                            call.get("offset").asInt,
                            call.get("length").asInt,
                        )
                    },
                )
            },
        )
    }
}

/** A call located in a project file. */
class ResolvedCall(val call: LayoutCall, val file: VirtualFile) {
    val label: String get() = "${call.function ?: "view"} · ${file.name}:${call.line}"
}

/**
 * Finds the code of a view: the innermost call in a file of the preview's module (the app's code, not the library's),
 * else in any file of the project. File names are resolved once.
 */
class SourceNavigator(private val project: Project, private val module: PreviewModule) {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, List<VirtualFile>>()

    /** Resolves the file names of [layouts] (index access: in the background, before the views are hovered). */
    fun prefetch(layouts: Collection<PreviewLayoutData>) {
        val names = layouts.flatMap { layout -> layout.nodes.flatMap { node -> node.calls.mapNotNull { it.file } } }.toSet() - cache.keys
        if (names.isEmpty()) return
        if (DumbService.isDumb(project)) {
            // Indexing (e.g. the IDE just opened): once it's done.
            ReadAction.nonBlocking<Unit> { resolveNames(names) }
                .inSmartMode(project)
                .submit(AppExecutorUtil.getAppExecutorService())
            return
        }
        ReadAction.run<RuntimeException> { resolveNames(names) }
    }

    private fun resolveNames(names: Collection<String>) {
        for (name in names) cache[name] = FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.projectScope(project)).toList()
    }

    fun resolve(calls: List<LayoutCall>): ResolvedCall? {
        val located = calls.mapNotNull { call -> call.file?.let { name -> call to files(name) } }
        located.firstNotNullOfOrNull { (call, files) ->
            files.firstOrNull { it.path.startsWith(module.moduleDir) }?.let { ResolvedCall(call, it) }
        }?.let { return it }
        return located.firstNotNullOfOrNull { (call, files) -> files.firstOrNull()?.let { ResolvedCall(call, it) } }
    }

    /** Only what [prefetch] resolved: the index can't be queried from the UI thread. */
    private fun files(name: String): List<VirtualFile> = cache[name].orEmpty()

    /**
     * Opens the call: at its offset when the file is as it was built (not edited since [builtAt]), else at its line.
     */
    fun navigate(resolved: ResolvedCall, builtAt: Long, focus: Boolean) {
        val file = resolved.file
        val unchanged = !FileDocumentManager.getInstance().isFileModified(file) && file.timeStamp <= builtAt
        val descriptor = if (unchanged) {
            OpenFileDescriptor(project, file, resolved.call.offset)
        } else {
            OpenFileDescriptor(project, file, (resolved.call.line - 1).coerceAtLeast(0), 0)
        }
        descriptor.navigate(focus)
    }
}

/** A view's frame scaled to the displayed preview. */
internal fun LayoutNode.scaled(scale: Double) =
    Rectangle((x * scale).toInt(), (y * scale).toInt(), (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
