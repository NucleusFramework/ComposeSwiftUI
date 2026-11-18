package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The agent tools against a real Gradle build and simulator: the sample given by
 * `-Dcsui.e2e.module=<…/samples/kmp-app/shared>` (rendered once beforehand). Skipped otherwise. The throwaway code
 * goes to the build directory: the sources are left alone.
 */
class PreviewAgentTest : BasePlatformTestCase() {
    private val moduleDir: String? = System.getProperty("csui.e2e.module")

    fun testToolsRenderCheckAndCompare() {
        val dir = moduleDir ?: return
        val root = File(dir).parentFile
        com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess.allowRootAccess(testRootDisposable, root.path, root.canonicalPath)
        val module = PreviewModule(root.path, dir, ":shared")
        PreviewModule.resolver = { if (it.path.startsWith(root.path) || it.path.startsWith(root.canonicalPath)) module else null }
        val agent = PreviewAgent(project)
        val showcase = File(dir, "src/iosMain/kotlin/sample/PreviewShowcase.kt").path
        try {
            val list = agent.list(showcase)
            assertTrue(list, "variant 1: \"BadgePreview - Right to left\"" in list)

            val render = await { agent.render(showcase, "BadgePreview", 1) }
            assertTrue(render, "Text \"5 new messages\" · PreviewShowcase.kt:" in render)
            val image = Regex("""Image: `(.+?)`""").find(render)!!.groupValues[1]
            assertTrue(image, File(image).isFile && image.endsWith("sample.BadgePreview~1.png"))

            // The same image as expected: nothing differs.
            val expected = File(createTempDir(), "expected.png").apply { File(image).copyTo(this) }
            val same = await { agent.render(showcase, "BadgePreview", 1, expected.path) }
            assertTrue(same, "Mismatch: 0% of the pixels" in same)

            // Throwaway code: rendered from the scratch directory, which is gone afterwards.
            val scratch = await {
                agent.renderCode(
                    "@Preview(name = \"Tiny\", sizeThatFits = true)\n@Composable\nfun TinyButton() {\n    Button(\"Go\") {}\n}\n",
                    emptyList(),
                    showcase,
                )
            }
            assertTrue(scratch, "[Accessibility] Touch target" in scratch && "Button \"Go\"" in scratch)
            assertFalse(File(dir, "build/${PreviewAgent.SCRATCH_DIR}").listFiles().orEmpty().any { it.name.startsWith("s") })

            // A compile error in the code comes back with its message.
            val broken = await { agent.renderCode("@Preview\n@Composable\nfun Broken() {\n    Text(42.unknown)\n}\n", emptyList(), showcase) }
            assertTrue(broken, "The build failed" in broken && "unknown" in broken)

            val diff = File(createTempDir(), "diff.png")
            val json = agent.compareImages(image, expected.path, diff.path)
            assertTrue(json, json.startsWith("{") && "\"mismatchPercent\":0" in json)
        } finally {
            PreviewModule.resolver = null
        }
    }

    /** Runs [block] off the EDT (it waits for renders that report on the EDT) while pumping the EDT. */
    private fun await(block: suspend () -> String): String {
        val future: Future<String> = ApplicationManager.getApplication().executeOnPooledThread<String> { runBlocking { block() } }
        val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(6)
        while (!future.isDone && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(20)
        }
        return future.get(1, TimeUnit.SECONDS)
    }

    private fun createTempDir(): File = kotlin.io.path.createTempDirectory("agent").toFile()
}
