package dev.nucleusframework.composeswiftui.idea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiteralDiffTest {
    private val compiled = """
        @Composable
        fun Badge() {
            Text("3 new messages", modifier = Modifier.padding(8.0))
            Toggle("On", isOn = true, onIsOnChange = {})
            Text("Count: ${'$'}count")
        }
    """.trimIndent()

    private fun edits(current: String) = LiteralDiff.edits("Badge.kt", compiled, current) { "Badge" }

    @Test
    fun stringsNumbersAndBooleans() {
        val current = compiled
            .replace("3 new messages", "4 unread")
            .replace("8.0", "0x10")
            .replace("isOn = true", "isOn = false")
            .replace("Count: ", "Total: ")
        assertEquals(
            listOf(
                LiteralEdit("Badge.kt", compiled.indexOf("3 new"), "Badge", "string", "3 new messages", "4 unread"),
                LiteralEdit("Badge.kt", compiled.indexOf("8.0"), "Badge", "number", "8.0", "16"),
                LiteralEdit("Badge.kt", compiled.indexOf("true"), "Badge", "boolean", "true", "false"),
                LiteralEdit("Badge.kt", compiled.indexOf("Count: "), "Badge", "string", "Count: ", "Total: "),
            ),
            edits(current),
        )
    }

    @Test
    fun formattingAndCommentsAreNotChanges() {
        val current = compiled.replace("Text(\"3", "Text(  \"3").replace("@Composable", "// a badge\n@Composable")
        assertEquals(emptyList<LiteralEdit>(), edits(current))
    }

    @Test
    fun anythingElseNeedsABuild() {
        assertNull(edits(compiled.replace("padding(8.0)", "padding(8.0).bold()")))
        assertNull(edits(compiled.replace("Text(\"3 new messages\"", "Label(\"3 new messages\"")))
        // An empty string has no string part: adding text changes the tokens.
        assertNull(LiteralDiff.edits("A.kt", "val a = \"\"", "val a = \"x\"") { null })
    }
}
