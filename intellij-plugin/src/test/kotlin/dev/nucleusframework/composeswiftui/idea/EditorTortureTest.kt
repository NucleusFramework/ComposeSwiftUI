package dev.nucleusframework.composeswiftui.idea

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * The preview pane through a series of everyday and unusual edits, against a real Gradle build and simulator: a copy
 * of the sample given by `-Dcsui.torture.module=<…/shared>` (rendered once beforehand). Skipped otherwise.
 */
class EditorTortureTest : BasePlatformTestCase() {
    private val moduleDir: String? = System.getProperty("csui.torture.module")

    fun testEditsKeepThePreviewsInSync() {
        val dir = moduleDir ?: return
        com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess.allowRootAccess(testRootDisposable, File(dir).parent, File(dir).canonicalFile.parent)
        val module = PreviewModule(File(dir).parent, dir, ":shared")
        val source = File(dir, "src/iosMain/kotlin/sample/EditorTorture.kt")
        source.writeText(code(label = "Hello", extra = ""))
        val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(source)!!
        // Left over by an earlier run: the document must be the file on disk.
        file.refresh(false, false)
        FileDocumentManager.getInstance().reloadFromDisk(FileDocumentManager.getInstance().getDocument(file)!!)
        PreviewModule.resolver = { if (it.path.startsWith(File(dir).parent) || it.path.startsWith(File(dir).canonicalFile.parent)) module else null }
        val options = PreviewSettings.options
        val previousMode = options.autoRender
        options.autoRender = AutoRender.ON_SAVE
        val editor = PreviewEditor(project, file)
        current = editor
        try {
            step("open") { editor.shownEntries.map { it.id }.containsAll(listOf("sample.TortureA", "sample.TortureB")) }

            step("first render") {
                editor.render()
                true
            }
            waitIdle(editor)
            assertTrue(editor.statusText, editor.statusText.startsWith("Rendered in"))
            val first = pixels(editor, "sample.TortureA")
            assertNotNull("TortureA rendered", first)

            // A literal: shown by the running host without a build, before and after the save.
            edit(file) { it.replace("\"Hello\"", "\"Hello, world\"") }
            step("literal edit shown live") { pixels(editor, "sample.TortureA")?.contentEquals(first) == false && "literal" in editor.statusText }
            save(file)
            step("literal save: no build") { !project.service().isRendering(module) && "literal" in editor.statusText }

            // A structural edit: built on save.
            val beforeStructural = pixels(editor, "sample.TortureA")
            edit(file) { it.replace("/*extra*/", "/*extra*/ Text(\"Second line\")") }
            step("structural edit out of date") { editor.statusText.startsWith("Out of date") }
            save(file)
            waitBuild(editor)
            assertTrue(editor.statusText, editor.statusText.startsWith("Rendered in"))
            assertFalse("structural edit shown", pixels(editor, "sample.TortureA")!!.contentEquals(beforeStructural))

            // A compile error: the build fails, the problem points at the file.
            edit(file) { it.replace("Text(\"Second line\")", "Text(\"Second line\"") }
            save(file)
            waitBuild(editor)
            assertEquals("Render failed", editor.statusText)
            assertTrue(editor.shownProblems.toString(), editor.shownProblems.any { it.path?.endsWith("EditorTorture.kt") == true })

            // Fixed back to the built code plus a literal: the failure is gone, the literal shows live.
            edit(file) { it.replace("Text(\"Second line\"", "Text(\"Second line!\")") }
            save(file)
            step("error fixed with a literal edit") { editor.shownProblems.isEmpty() && "Render failed" != editor.statusText }
            waitIdle(editor)

            // Edits in a row while building: the last one is what shows.
            for (i in 1..4) {
                edit(file) { it.replace(Regex("""/\*extra\*/.*"""), "/*extra*/ Text(\"Burst $i\"); Spacer()") }
                save(file)
                Thread.sleep(700)
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            }
            waitIdle(editor)
            assertTrue(editor.statusText, editor.statusText.startsWith("Rendered in"))

            // A renamed preview replaces the old one.
            edit(file) { it.replace("fun TortureB(", "fun TortureRenamed(") }
            save(file)
            waitBuild(editor)
            step("renamed preview") { editor.shownEntries.map { it.id }.let { "sample.TortureRenamed" in it && "sample.TortureB" !in it } }
            assertTrue(editor.shownEntries.first { it.id == "sample.TortureRenamed" }.rendered)

            // The host was killed (simulator reset, crash): a literal edit builds and relaunches it.
            PreviewHostClient(module).runningHost()?.let { udid ->
                val pid = Regex("""\d+""").find(File(dir, "build/composeSwiftUI/previewHost/host-$udid.json").readText())!!.value.toLong()
                ProcessHandle.of(pid).ifPresent { it.destroyForcibly() }
                Thread.sleep(500)
            }
            val beforeKill = pixels(editor, "sample.TortureA")
            edit(file) { it.replace("\"Hello, world\"", "\"After the kill\"") }
            step("literal after host killed") { pixels(editor, "sample.TortureA")?.contentEquals(beforeKill) == false }
            waitIdle(editor)
            // Built meanwhile: saving the literal needs no other build.
            save(file)
            waitIdle(editor)
            assertTrue(editor.statusText, "literal" in editor.statusText)

            // While typing: a structural edit builds (saved) once the analysis ends without errors; a literal doesn't.
            options.autoRender = AutoRender.WHILE_TYPING
            edit(file) { it.replace(Regex("""/\*extra\*/.*"""), "/*extra*/ Text(\"Typed\")") }
            project.messageBus.syncPublisher(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC).daemonFinished(listOf(editor))
            waitBuild(editor)
            assertFalse("saved by the plugin", FileDocumentManager.getInstance().isFileModified(file))
            assertTrue(editor.statusText, editor.statusText.startsWith("Rendered in"))
            edit(file) { it.replace("\"Typed\"", "\"Typed live\"") }
            project.messageBus.syncPublisher(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC).daemonFinished(listOf(editor))
            step("typed literal shown live") { "literal" in editor.statusText && !editor.statusText.endsWith("…") }
            assertTrue("a literal isn't saved", FileDocumentManager.getInstance().isFileModified(file))
            options.autoRender = AutoRender.ON_SAVE

            // Every preview removed.
            edit(file) { "package sample\n" }
            save(file)
            waitBuild(editor)
            step("no previews") { editor.shownEntries.isEmpty() }
        } finally {
            options.autoRender = previousMode
            Disposer.dispose(editor)
            PreviewModule.resolver = null
            source.delete()
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(source.parentFile)
        }
    }

    private var current: PreviewEditor? = null

    private fun code(label: String, extra: String) = """
        |package sample
        |
        |import androidx.compose.runtime.Composable
        |import dev.nucleusframework.composeswiftui.*
        |
        |@Preview(name = "Torture A", sizeThatFits = true)
        |@Composable
        |fun TortureA() {
        |    VStack {
        |        Text("$label")
        |        /*extra*/ $extra
        |    }
        |}
        |
        |@Preview(name = "Torture B", sizeThatFits = true)
        |@Composable
        |fun TortureB() {
        |    Toggle("Enabled", isOn = true, onIsOnChange = {})
        |}
        |""".trimMargin()

    private fun edit(file: VirtualFile, change: (String) -> String) {
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteAction.runAndWait<RuntimeException> {
            com.intellij.openapi.command.CommandProcessor.getInstance().executeCommand(project, {
                document.setText(change(document.text))
            }, null, null)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun save(file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteAction.runAndWait<RuntimeException> { FileDocumentManager.getInstance().saveDocument(document) }
    }

    private fun PreviewRenderer?.isRendering(module: PreviewModule) = this!!.isRendering(module)
    private fun com.intellij.openapi.project.Project.service() = getService(PreviewRenderer::class.java)

    private fun rendering(editor: PreviewEditor) = editor.module?.let { project.service().isRendering(it) } == true

    /** Waits for the render a save starts, then for its end. */
    private fun waitBuild(editor: PreviewEditor) {
        step("build starts") { rendering(editor) }
        waitIdle(editor)
    }

    /** Waits until no render runs and the status settled. */
    private fun waitIdle(editor: PreviewEditor) {
        step("idle (${editor.statusText})") {
            !rendering(editor) && !editor.statusText.endsWith("…")
        }
        // The images reload off the UI thread.
        val until = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(20)
        }
    }

    private fun pixels(editor: PreviewEditor, id: String): IntArray? =
        editor.shownEntries.firstOrNull { it.id == id }?.image?.let { it.getRGB(0, 0, it.width, it.height, null, 0, it.width) }

    private fun step(name: String, timeoutMs: Long = 180_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (true) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            if (condition()) break
            if (System.currentTimeMillis() - start > timeoutMs) fail("timed out: $name (status: ${current?.statusText})")
            Thread.sleep(20)
        }
        println("TORTURE ${"%-40s".format(name)} ${System.currentTimeMillis() - start} ms")
    }
}
