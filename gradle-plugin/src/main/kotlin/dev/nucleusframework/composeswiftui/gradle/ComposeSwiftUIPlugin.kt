package dev.nucleusframework.composeswiftui.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.io.File

/** `composeSwiftUI { }` configuration (everything is optional). */
abstract class ComposeSwiftUIExtension {
    /**
     * Where the Swift renderer is written. Default: `$SRCROOT/ComposeSwiftUI` when Gradle runs from an
     * Xcode build phase, otherwise `<root>/iosApp/ComposeSwiftUI`.
     */
    abstract val swiftOutputDir: DirectoryProperty

    /** Kotlin framework imported by the renderer. Default: `baseName` of the first Apple framework. */
    abstract val frameworkName: Property<String>

    /** Library version added to `commonMain`. Default: the plugin version. */
    abstract val libraryVersion: Property<String>
}

/**
 * Compose SwiftUI in one line: adds `dev.nucleusframework:compose-swiftui`, applies the Compose
 * compiler, exports the library from every Apple framework, and writes the Swift renderer into the
 * Xcode project on every `embedAndSign…AppleFrameworkForXcode` run (the build phase of KMP apps).
 */
class ComposeSwiftUIPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("composeSwiftUI", ComposeSwiftUIExtension::class.java)
        extension.libraryVersion.convention(BuildConfig.LIBRARY_VERSION)

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            applyComposeCompiler(project)
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            val coordinates = extension.libraryVersion.map { "dev.nucleusframework:compose-swiftui:$it" }

            kotlin.sourceSets.named("commonMain") { sourceSet ->
                sourceSet.dependencies { api(coordinates.get()) }
            }
            kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                if (target.konanTarget.family.isAppleFamily) {
                    target.binaries.withType(Framework::class.java).configureEach { framework ->
                        // the Swift renderer uses the runtime classes through the app's framework
                        framework.export(coordinates.get())
                    }
                }
            }

            val sync = project.tasks.register("syncComposeSwiftUIRenderer", SyncComposeSwiftUIRendererTask::class.java) { task ->
                task.group = "compose swiftui"
                task.description = "Writes the Compose SwiftUI renderer (Swift sources) into the Xcode project."
                task.outputDirectory.set(extension.swiftOutputDir.orElse(defaultOutputDir(project)))
                task.frameworkName.set(extension.frameworkName.orElse(project.provider { firstFrameworkName(kotlin) }))
            }
            // Runs with the KMP Xcode build phase, so the renderer always matches the library version.
            project.tasks.configureEach { task ->
                if ((task.name.startsWith("embedAndSign") && task.name.endsWith("AppleFrameworkForXcode")) ||
                    task.name == "embedSwiftExportForXcode"
                ) {
                    task.dependsOn(sync)
                }
            }
        }
    }

    private fun applyComposeCompiler(project: Project) {
        try {
            project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        } catch (e: Exception) {
            throw GradleException(
                "Compose SwiftUI needs the Compose compiler plugin: add `kotlin(\"plugin.compose\")` " +
                    "(same version as Kotlin) to the plugins block of ${project.path}.",
                e,
            )
        }
    }

    private fun defaultOutputDir(project: Project) = project.layout.dir(
        project.provider {
            val srcRoot = System.getenv("SRCROOT")
            if (srcRoot != null) File(srcRoot, "ComposeSwiftUI") else project.rootDir.resolve("iosApp/ComposeSwiftUI")
        },
    )

    private fun firstFrameworkName(kotlin: KotlinMultiplatformExtension): String =
        kotlin.targets.withType(KotlinNativeTarget::class.java)
            .filter { it.konanTarget.family.isAppleFamily }
            .flatMap { it.binaries.withType(Framework::class.java) }
            .firstOrNull()?.baseName
            ?: throw GradleException(
                "Compose SwiftUI: no Apple framework binary found. Declare one, e.g. " +
                    "`iosArm64 { binaries.framework { baseName = \"Shared\" } }`, or set composeSwiftUI.frameworkName.",
            )

    private val Family.isAppleFamily: Boolean
        get() = this == Family.IOS || this == Family.OSX || this == Family.TVOS || this == Family.WATCHOS
}

/** Copies the renderer sources embedded in the plugin and writes `KotlinModule.swift`. */
@DisableCachingByDefault(because = "Writes into the Xcode project, outside the build directory")
abstract class SyncComposeSwiftUIRendererTask : DefaultTask() {
    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    val rendererVersion: String = BuildConfig.LIBRARY_VERSION

    @TaskAction
    fun sync() {
        val out = outputDirectory.get().asFile
        val base = BuildConfig.RENDERER_RESOURCES
        val loader = SyncComposeSwiftUIRendererTask::class.java.classLoader
        val index = loader.getResourceAsStream("$base/index.txt")?.bufferedReader()?.readText()
            ?: throw GradleException("Compose SwiftUI: renderer resources missing from the plugin jar")
        val files = index.lines().filter { it.isNotBlank() }

        out.mkdirs()
        val wanted = files.toSet() + setOf("KotlinModule.swift", README)
        // Remove files of an older renderer version (only inside our own folder).
        out.walkBottomUp().filter { it.isFile }.forEach { file ->
            if (file.relativeTo(out).invariantSeparatorsPath !in wanted) file.delete()
        }
        for (rel in files) {
            val bytes = loader.getResourceAsStream("$base/$rel")!!.use { it.readBytes() }
            val target = out.resolve(rel)
            if (!target.exists() || !target.readBytes().contentEquals(bytes)) {
                target.parentFile.mkdirs()
                target.writeBytes(bytes)
            }
        }
        writeIfChanged(out.resolve("KotlinModule.swift"), "@_exported import ${frameworkName.get()}\n")
        writeIfChanged(
            out.resolve(README),
            "Generated by the dev.nucleusframework.compose-swiftui Gradle plugin ($rendererVersion).\n" +
                "Do not edit: this folder is rewritten on every build. Add it once to your app target in Xcode.\n",
        )
        logger.lifecycle("Compose SwiftUI renderer $rendererVersion synced to $out (framework ${frameworkName.get()})")
    }

    private fun writeIfChanged(file: File, text: String) {
        if (!file.exists() || file.readText() != text) file.writeText(text)
    }

    private companion object {
        const val README = "README.txt"
    }
}
