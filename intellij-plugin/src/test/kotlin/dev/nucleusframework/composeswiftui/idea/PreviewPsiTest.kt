package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class PreviewPsiTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        // Just enough of the libraries for references to resolve.
        myFixture.addFileToProject(
            "lib/Library.kt",
            """
            package dev.nucleusframework.composeswiftui

            annotation class Preview(val name: String = "")
            annotation class PreviewLightDark
            annotation class PreviewParameter(val provider: kotlin.reflect.KClass<*>)
            """.trimIndent(),
        )
        myFixture.addFileToProject("lib/Runtime.kt", "package androidx.compose.runtime\n\nannotation class Composable\n")
        myFixture.addFileToProject("lib/Android.kt", "annotation class Preview\n")
    }

    private fun file(text: String) = myFixture.configureByText("Screens.kt", text.trimIndent()) as KtFile

    fun testFindsPreviewsInSourceOrder() {
        val file = file(
            """
            package app

            import dev.nucleusframework.composeswiftui.*

            @Preview(name = "Second")
            @Composable
            fun B() {}

            @PreviewLightDark
            @Composable
            fun A(@PreviewParameter(Names::class) name: String) {}

            @Composable
            fun NotAPreview() {}

            @Preview
            @Composable
            private fun Hidden() {}
            """,
        )
        assertEquals(listOf("app.B", "app.A"), file.previewFunctions().map { it.fqName?.asString() })
        assertEquals("Second", file.findFunction("app.B")!!.previewTitle())
        assertEquals("A", file.findFunction("app.A")!!.previewTitle())
    }

    fun testExplainsWhyAPreviewIsSkipped() {
        val file = file(
            """
            import dev.nucleusframework.composeswiftui.*

            @Preview fun NoComposable() {}
            @Preview @Composable fun TwoParameters(a: Int, b: Int) {}
            @Preview @Composable fun PlainParameter(a: Int) {}
            @Preview @Composable suspend fun Suspended() {}
            object Holder {
                @Preview @Composable fun Nested() {}
            }
            """,
        )
        val functions = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).associateBy { it.name }
        assertEquals("Previews must be @Composable", functions.getValue("NoComposable").previewProblem())
        assertTrue(functions.getValue("TwoParameters").previewProblem()!!.startsWith("A preview takes at most one parameter"))
        assertTrue(functions.getValue("PlainParameter").previewProblem()!!.contains("@PreviewParameter"))
        assertTrue(functions.getValue("Suspended").previewProblem()!!.contains("suspend"))
        assertEquals("Previews must be top-level functions", functions.getValue("Nested").previewProblem())
    }

    fun testInspectionAndQuickFixes() {
        myFixture.enableInspections(PreviewInspection())
        myFixture.configureByText(
            "Screens.kt",
            """
            import dev.nucleusframework.composeswiftui.*
            import androidx.compose.runtime.Composable

            @Preview
            @Composable
            private fun <warning descr="Previews can't be private: this preview won't be rendered">Hidden</warning>() {}

            @Preview
            fun <warning descr="Previews must be @Composable: this preview won't be rendered">Plain</warning>() {}

            @Preview
            @Composable
            fun Fine() {}
            """.trimIndent(),
        )
        myFixture.checkHighlighting()

        myFixture.launchAction(myFixture.getAllQuickFixes().single { it.text == "Make the preview public" })
        myFixture.launchAction(myFixture.getAllQuickFixes().single { it.text == "Add @Composable" })
        myFixture.checkResult(
            """
            import dev.nucleusframework.composeswiftui.*
            import androidx.compose.runtime.Composable

            @Preview
            @Composable
            fun Hidden() {}

            @Composable
            @Preview
            fun Plain() {}

            @Preview
            @Composable
            fun Fine() {}
            """.trimIndent(),
        )
    }

    fun testInspectionIgnoresFilesWithoutTheLibrary() {
        myFixture.enableInspections(PreviewInspection())
        myFixture.configureByText("Android.kt", "@Preview\nprivate fun AndroidPreview() {}\n")
        myFixture.checkHighlighting()
    }

    fun testEntriesFromTheResultsOfTheLastRenders() {
        val moduleDir = Files.createTempDirectory("module").toFile()
        val previews = File(moduleDir, "build/composeSwiftUI/previews").apply { mkdirs() }
        ImageIO.write(BufferedImage(30, 60, BufferedImage.TYPE_INT_ARGB), "png", File(previews, "app.A~1.png"))
        ImageIO.write(BufferedImage(30, 60, BufferedImage.TYPE_INT_ARGB), "png", File(previews, "app.A~0.png"))
        File(previews, "results.json").writeText(
            """
            {"previews": [
              {"key": "app.A~1", "id": "app.A", "title": "A - Dark", "group": "Themes", "file": "app.A~1.png", "width": 10, "height": 20, "milliseconds": 300},
              {"key": "app.A~0", "id": "app.A", "title": "A - Light", "group": "Themes", "file": "app.A~0.png", "width": 10, "height": 20, "milliseconds": 250},
              {"key": "app.C", "id": "app.C", "title": "C", "error": "Uncaught Kotlin exception: boom"},
              {"key": "other.D", "id": "other.D", "title": "D", "file": "other.D.png"}
            ]}
            """.trimIndent(),
        )
        val module = PreviewModule(moduleDir.path, moduleDir.path, ":")
        val source = myFixture.configureByText("A.kt", "package app").virtualFile
        val entries = PreviewEntry.load(module, listOf("app.A", "app.B", "app.C"), mapOf("app.B" to "Bee"), source)

        assertEquals(listOf("app.A~0", "app.A~1", "app.B", "app.C"), entries.map { it.key })
        assertEquals("Themes · 10×20 pt · 250 ms", entries[0].subtitle)
        assertNotNull(entries[0].image)
        assertEquals("Bee", entries[2].title)
        assertFalse(entries[2].rendered)
        assertEquals("Uncaught Kotlin exception: boom", entries[3].error)
        moduleDir.deleteRecursively()
    }

    fun testPreviewEditorShowsTheFile() {
        val file = myFixture.configureByText(
            "Screen.kt",
            """
            import dev.nucleusframework.composeswiftui.*

            @Preview
            @Composable
            fun Screen() {}
            """.trimIndent(),
        ).virtualFile
        val editor = PreviewEditor(project, file)
        try {
            assertNotNull(editor.component)
            assertNull("no Gradle module in a light test", editor.module)
            assertTrue(PreviewEditorProvider().accept(project, file))
        } finally {
            Disposer.dispose(editor)
        }
    }

    fun testClickedViewGoesToTheInnermostCallOfTheProject() {
        myFixture.addFileToProject("Screen.kt", "package app\n\nfun Screen() {}\n")
        val layout = PreviewLayoutData(
            100.0, 100.0,
            listOf(
                LayoutNode(0.0, 0.0, 100.0, 100.0, listOf(LayoutCall("VStack", "Screen.kt", 3, 10, 50))),
                // The library's call (not in the project) is skipped for the app's own call.
                LayoutNode(10.0, 10.0, 20.0, 20.0, listOf(LayoutCall("SwiftView", "GenViews.kt", 900, 1, 1), LayoutCall("Text", "Screen.kt", 4, 20, 9))),
            ),
        )
        val navigator = SourceNavigator(project, PreviewModule("/nowhere", "/nowhere", ":"))
        navigator.prefetch(listOf(layout))
        val (node, call) = layout.nodeAt(15.0, 15.0, navigator)!!
        assertEquals(20.0, node.width)
        assertEquals("Text", call.call.function)
        assertEquals("Screen.kt", call.file.name)
        assertEquals("Text · Screen.kt:4", call.label)
        assertEquals("VStack", layout.nodeAt(80.0, 80.0, navigator)!!.second.call.function)
        assertNull(layout.nodeAt(150.0, 15.0, navigator))
    }
}
