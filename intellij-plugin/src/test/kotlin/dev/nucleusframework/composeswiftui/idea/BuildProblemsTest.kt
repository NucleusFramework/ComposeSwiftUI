package dev.nucleusframework.composeswiftui.idea

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildProblemsTest {
    @Test
    fun kotlinErrorsWithTheirLocation() {
        val output = """
            > Task :shared:compileKotlinIosSimulatorArm64 FAILED
            w: file:///p/Other.kt:1:1 A warning
            e: file:///p/src/Screen.kt:12:5 Unresolved reference 'Txt'.
            e: file:///p/src/Screen.kt:12:5 Unresolved reference 'Txt'.
            e: /p/src/Other.kt:3:9: Type mismatch.
        """.trimIndent()
        assertEquals(
            listOf(
                BuildProblem("Unresolved reference 'Txt'.", "/p/src/Screen.kt", 12, 5),
                BuildProblem("Type mismatch.", "/p/src/Other.kt", 3, 9),
            ),
            PreviewRenderer.problems(output),
        )
    }

    @Test
    fun gradleExplanationOtherwise() {
        val output = """
            FAILURE: Build failed with an exception.

            * What went wrong:
            Execution failed for task ':shared:renderComposeSwiftUIPreviews'.
            > Compose SwiftUI: no iOS simulator found

            * Try:
            > Run with --stacktrace option to get the stack trace.
        """.trimIndent()
        assertEquals(
            "Execution failed for task ':shared:renderComposeSwiftUIPreviews'.\n> Compose SwiftUI: no iOS simulator found",
            PreviewRenderer.problems(output).single().message,
        )
    }
}
