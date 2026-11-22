package dev.nucleusframework.composeswiftui.gradle

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationsTest {
    @Test
    fun registrationCallsEachListedIntegrationUnderItsGuard() {
        val swift = IntegrationsRegistration.swift(
            listOf(
                "MapKit" to "import SwiftUI\nimport MapKit\n\npublic extension ComposeIntegrations {\n    @MainActor static func registerMapKit() {}\n}\n",
                "HealthKitUI" to "// header\nimport SwiftUI\n\n#if os(iOS) || os(watchOS)\nextension ComposeIntegrations {\n    @MainActor static func registerHealthKitUI() {}\n}\n#endif\n",
            ),
        )
        assertTrue("        registerMapKit()  // MapKit" in swift)
        assertTrue("        #if os(iOS) || os(watchOS)\n        registerHealthKitUI()  // HealthKitUI\n        #endif" in swift)
    }

    @Test
    fun noListMeansNothingRegistered() {
        assertTrue("registerConfigured() {\n    }" in IntegrationsRegistration.swift(emptyList()))
    }

    @Test
    fun namesTheRendererFilesAProjectListingFilesMisses() = withApp { app, renderer ->
        File(app, "App.xcodeproj").mkdirs()
        File(app, "App.xcodeproj/project.pbxproj").writeText("/* Core.swift */ /* Args.swift */")
        assertEquals(listOf("New.swift"), XcodeProjects.missingRendererFiles(renderer))
    }

    @Test
    fun aSynchronizedFolderMissesNothing() = withApp { app, renderer ->
        File(app, "App.xcodeproj").mkdirs()
        File(app, "App.xcodeproj/project.pbxproj").writeText("PBXFileSystemSynchronizedRootGroup path = ComposeSwiftUI;")
        assertEquals(emptyList(), XcodeProjects.missingRendererFiles(renderer))
    }

    private fun withApp(block: (File, File) -> Unit) {
        val app = Files.createTempDirectory("app").toFile()
        try {
            val renderer = File(app, "ComposeSwiftUI").apply { mkdirs() }
            listOf("Core.swift", "Args.swift", "New.swift").forEach { File(renderer, it).writeText("") }
            block(app, renderer)
        } finally {
            app.deleteRecursively()
        }
    }
}
