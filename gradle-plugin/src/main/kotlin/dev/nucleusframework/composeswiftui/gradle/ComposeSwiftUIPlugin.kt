package dev.nucleusframework.composeswiftui.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
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

    /**
     * Simulator (name or UDID) used by `renderComposeSwiftUIPreviews`. Default: the booted simulator, else the
     * first available iPhone.
     */
    abstract val previewDevice: Property<String>
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

            val previews = registerPreviews(project, kotlin)
            registerPreviewRendering(project, kotlin, extension, previews)
            val sync = project.tasks.register("syncComposeSwiftUIRenderer", SyncComposeSwiftUIRendererTask::class.java) { task ->
                task.group = "compose swiftui"
                task.description = "Writes the Compose SwiftUI renderer (Swift sources) into the Xcode project."
                task.outputDirectory.set(extension.swiftOutputDir.orElse(defaultOutputDir(project)))
                task.frameworkName.set(extension.frameworkName.orElse(project.provider { firstFrameworkName(kotlin) }))
                task.previewsSwiftFile.set(previews.flatMap { it.swiftOutputFile })
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

    /**
     * `@Preview` support: the generated Kotlin file is compiled into every Apple framework (one copy per target,
     * so no source directory is shared between source sets), and the Swift file is copied by the sync task.
     */
    private fun registerPreviews(project: Project, kotlin: KotlinMultiplatformExtension) =
        project.tasks.register("generateComposeSwiftUIPreviews", GenerateComposeSwiftUIPreviewsTask::class.java) { task ->
            task.group = "compose swiftui"
            task.description = "Generates the Kotlin and Swift code that shows the @Preview composables in Xcode."
            task.sources.from(project.provider { previewSourceDirs(project, kotlin) })
            task.targetNames.set(project.provider { appleMainCompilations(kotlin).map { it.target.name } })
            task.kotlinOutputDirectory.set(project.layout.buildDirectory.dir("generated/composeSwiftUI/previews/kotlin"))
            task.swiftOutputFile.set(
                project.layout.buildDirectory.file("generated/composeSwiftUI/previews/${PreviewCodegen.SWIFT_FILE_NAME}"),
            )
            task.hostSwiftOutputFile.set(
                project.layout.buildDirectory.file("generated/composeSwiftUI/previews/ComposeSwiftUIRenderedPreviews.swift"),
            )
        }.also { previews ->
            kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                if (target.konanTarget.family.isAppleFamily) {
                    target.compilations.named(KotlinCompilation.MAIN_COMPILATION_NAME) { compilation ->
                        compilation.defaultSourceSet.kotlin.srcDir(previews.flatMap { it.kotlinOutputDirectory.dir(target.name) })
                    }
                }
            }
        }

    /** `renderComposeSwiftUIPreviews`, run by the IntelliJ plugin. Uses the debug framework of the simulator target. */
    private fun registerPreviewRendering(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        extension: ComposeSwiftUIExtension,
        previews: TaskProvider<GenerateComposeSwiftUIPreviewsTask>,
    ) {
        val framework = project.provider { previewFramework(kotlin) }
        project.tasks.register("renderComposeSwiftUIPreviews", RenderComposeSwiftUIPreviewsTask::class.java) { task ->
            task.group = "compose swiftui"
            task.description = "Renders the @Preview composables to PNG on the iOS simulator."
            task.dependsOn(framework.map { it.linkTaskProvider })
            task.frameworkDirectory.set(project.layout.dir(framework.map { it.outputFile }))
            task.frameworkName.set(framework.map { it.baseName })
            task.staticFramework.set(framework.map { it.isStatic })
            task.previewsSwiftFile.set(previews.flatMap { it.swiftOutputFile })
            task.hostSwiftFile.set(previews.flatMap { it.hostSwiftOutputFile })
            task.device.set(extension.previewDevice)
            // Compose Multiplatform resources, when the module uses them (the app bundle must carry them).
            // Wire the task's outputs, not the task itself: a Task can't be stored in the configuration cache.
            task.composeResources.from(
                project.provider {
                    val name = "assemble${framework.get().target.name.replaceFirstChar { it.uppercase() }}MainResources"
                    if (name in project.tasks.names) project.tasks.named(name).get().outputs.files else project.files()
                },
            )
            task.workDirectory.set(project.layout.buildDirectory.dir("composeSwiftUI/previewHost"))
            task.outputDirectory.set(project.layout.buildDirectory.dir(PREVIEW_IMAGES_DIR))
        }
    }

    private fun previewFramework(kotlin: KotlinMultiplatformExtension): Framework {
        val targetName = if (System.getProperty("os.arch") == "aarch64") "iosSimulatorArm64" else "iosX64"
        val target = kotlin.targets.findByName(targetName) as? KotlinNativeTarget
            ?: throw GradleException("Compose SwiftUI previews need the `$targetName()` target with a framework binary.")
        return target.binaries.withType(Framework::class.java).firstOrNull { it.buildType == NativeBuildType.DEBUG }
            ?: throw GradleException("Compose SwiftUI previews need a debug framework on `$targetName`: `binaries.framework { }`.")
    }

    /**
     * Hand-written source directories compiled into every Apple framework (e.g. `commonMain`, `appleMain`,
     * `iosMain` for an iOS-only app), so a preview found there can be called from each target. Generated
     * directories (under the build directory) are skipped.
     */
    private fun previewSourceDirs(project: Project, kotlin: KotlinMultiplatformExtension): List<File> {
        val sourceSets = appleMainCompilations(kotlin)
            .map { it.allKotlinSourceSets.toSet() }
            .reduceOrNull { shared, sourceSet -> shared intersect sourceSet }
            .orEmpty()
        val buildDir = project.layout.buildDirectory.get().asFile
        return sourceSets.flatMap { it.kotlin.srcDirs }.filter { !it.startsWith(buildDir) }.sorted()
    }

    private fun appleMainCompilations(kotlin: KotlinMultiplatformExtension) =
        kotlin.targets.withType(KotlinNativeTarget::class.java)
            .filter { it.konanTarget.family.isAppleFamily }
            .map { it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) }

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

    internal companion object {
        /** Read by the IntelliJ plugin: keep in sync with `PreviewImages` there. */
        const val PREVIEW_IMAGES_DIR = "composeSwiftUI/previews"
    }

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

    /** `ComposeSwiftUIPreviews.swift`, written next to the renderer. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val previewsSwiftFile: RegularFileProperty

    @get:Input
    val rendererVersion: String = BuildConfig.LIBRARY_VERSION

    @TaskAction
    fun sync() {
        val out = outputDirectory.get().asFile
        val files = RendererResources.files()

        out.mkdirs()
        val wanted = files.toSet() + setOf("KotlinModule.swift", README, PreviewCodegen.SWIFT_FILE_NAME)
        // Remove files of an older renderer version (only inside our own folder).
        out.walkBottomUp().filter { it.isFile }.forEach { file ->
            if (file.relativeTo(out).invariantSeparatorsPath !in wanted) file.delete()
        }
        for (rel in files) {
            val bytes = RendererResources.read(rel)
            val target = out.resolve(rel)
            if (!target.exists() || !target.readBytes().contentEquals(bytes)) {
                target.parentFile.mkdirs()
                target.writeBytes(bytes)
            }
        }
        writeIfChanged(out.resolve("KotlinModule.swift"), "@_exported import ${frameworkName.get()}\n")
        previewsSwiftFile.orNull?.asFile?.let { writeIfChanged(out.resolve(PreviewCodegen.SWIFT_FILE_NAME), it.readText()) }
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

/**
 * Finds the `@Preview @Composable` functions and generates `ComposeSwiftUIPreviews` (Kotlin, one file per Apple
 * target) and `ComposeSwiftUIPreviews.swift` (the `#Preview`s and the gallery, copied into the Xcode project by
 * [SyncComposeSwiftUIRendererTask]). Files are still generated when there is no preview.
 */
@CacheableTask
abstract class GenerateComposeSwiftUIPreviewsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val targetNames: ListProperty<String>

    @get:OutputDirectory
    abstract val kotlinOutputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val swiftOutputFile: RegularFileProperty

    /** Preview list of the host app that renders the previews to PNG (see [RenderComposeSwiftUIPreviewsTask]). */
    @get:OutputFile
    abstract val hostSwiftOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val previews = sources.asFileTree.matching { it.include("**/*.kt") }.files
            .sorted()
            .flatMap { PreviewScanner.scan(it.readText()) }
        val entries = PreviewScanner.entries(previews)

        val kotlinRoot = kotlinOutputDirectory.get().asFile
        kotlinRoot.deleteRecursively()
        val kotlinCode = PreviewCodegen.kotlin(entries)
        for (target in targetNames.get()) {
            kotlinRoot.resolve("$target/${PreviewCodegen.OBJECT_NAME}.kt").apply { parentFile.mkdirs() }.writeText(kotlinCode)
        }
        swiftOutputFile.get().asFile.apply { parentFile.mkdirs() }.writeText(PreviewCodegen.swift(entries))
        hostSwiftOutputFile.get().asFile.apply { parentFile.mkdirs() }.writeText(PreviewCodegen.hostSwift(entries))
        logger.info("Compose SwiftUI: ${entries.size} preview(s): ${entries.joinToString { it.function.qualifiedName }}")
    }
}
