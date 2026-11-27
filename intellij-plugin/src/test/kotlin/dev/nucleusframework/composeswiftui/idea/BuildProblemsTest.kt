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

    @Test
    fun xcodeSwiftAndKotlinErrors() {
        val output = """
            /p/iosApp/ContentView.swift:14:9: error: cannot find 'foo' in scope
            /p/iosApp/Bridge.m:22:5: error: use of undeclared identifier 'bar'
            e: file:///p/shared/App.kt:8:3 Unresolved reference 'Baz'.
            xcodebuild: error: Scheme 'Wrong' is not currently configured for the build action.
        """.trimIndent()
        assertEquals(
            listOf(
                BuildProblem("cannot find 'foo' in scope", "/p/iosApp/ContentView.swift", 14, 9),
                BuildProblem("use of undeclared identifier 'bar'", "/p/iosApp/Bridge.m", 22, 5),
                BuildProblem("Unresolved reference 'Baz'.", "/p/shared/App.kt", 8, 3),
                BuildProblem("Scheme 'Wrong' is not currently configured for the build action."),
            ),
            PreviewAgent.xcodeProblems(output),
        )
    }

    @Test
    fun discoverXcodeProjectsPrefersWorkspaceAndSkipsNested() {
        val root = kotlin.io.path.createTempDirectory("xcode-discover").toFile()
        try {
            val iosApp = java.io.File(root, "iosApp").apply { mkdirs() }
            java.io.File(iosApp, "App.xcodeproj/project.xcworkspace").mkdirs()
            java.io.File(iosApp, "App.xcworkspace").mkdirs()
            java.io.File(root, "Pods/Pods.xcodeproj").mkdirs()
            java.io.File(root, "build/Generated.xcodeproj").mkdirs()

            val discovered = PreviewAgent.discoverXcodeProjects(root)
            assertEquals(listOf(java.io.File(iosApp, "App.xcworkspace")), discovered)
        } finally {
            root.deleteRecursively()
        }
    }
}
