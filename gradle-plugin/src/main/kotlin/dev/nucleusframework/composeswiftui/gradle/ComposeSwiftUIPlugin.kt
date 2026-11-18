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
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
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

    /** Library version added to `commonMain` (`appleMain` when the module has non-Apple targets). Default: the plugin version. */
    abstract val libraryVersion: Property<String>

    /**
     * Simulator (name or UDID) used by `renderComposeSwiftUIPreviews`, or `"mac"` to render on this Mac. Default: a
     * simulator of its own, `Compose SwiftUI Previews` (created like the booted iPhone), booted in the background so
     * the app's stays free; the Mac in a module without an iOS simulator target.
     */
    abstract val previewDevice: Property<String>

    /**
     * Opt-in framework renderers written to `ComposeSwiftUIIntegrations`, by file name without `.swift`
     * (`listOf("MapKit", "Charts", "HealthKitUI")`). Default: all of them. Listing only the ones the app uses keeps
     * the folder free of frameworks it doesn't link, so the whole folder can be added to the target.
     */
    abstract val integrations: ListProperty<String>
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

            // The library is published for Apple targets only: a module that also targets Android or the JVM gets it
            // in `appleMain` (or each Apple target's main source set without the default hierarchy).
            project.afterEvaluate {
                val apple = kotlin.targets.withType(KotlinNativeTarget::class.java).filter { it.konanTarget.family.isAppleFamily }
                val others = kotlin.targets.filter { it.platformType != KotlinPlatformType.common && it !in apple }
                val sourceSets = when {
                    others.isEmpty() -> listOf(kotlin.sourceSets.getByName("commonMain"))
                    else -> listOfNotNull(kotlin.sourceSets.findByName("appleMain"))
                        .ifEmpty { apple.map { it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).defaultSourceSet } }
                }
                sourceSets.forEach { sourceSet -> sourceSet.dependencies { api(coordinates.get()) } }
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
            registerPreviewList(project, previews)
            val sync = project.tasks.register("syncComposeSwiftUIRenderer", SyncComposeSwiftUIRendererTask::class.java) { task ->
                task.group = "compose swiftui"
                task.description = "Writes the Compose SwiftUI renderer (Swift sources) into the Xcode project."
                task.outputDirectory.set(extension.swiftOutputDir.orElse(defaultOutputDir(project)))
                task.frameworkName.set(extension.frameworkName.orElse(project.provider { firstFrameworkName(kotlin) }))
                task.previewsSwiftFile.set(previews.flatMap { it.swiftOutputFile })
                task.integrations.set(extension.integrations)
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
            task.sources.from(project.provider { previewSourceDirs(project, kotlin) + scratchDirs(project) })
            task.targetNames.set(project.provider { appleMainCompilations(kotlin).map { it.target.name } })
            task.kotlinOutputDirectory.set(project.layout.buildDirectory.dir("generated/composeSwiftUI/previews/kotlin"))
            task.swiftOutputFile.set(
                project.layout.buildDirectory.file("generated/composeSwiftUI/previews/${PreviewCodegen.SWIFT_FILE_NAME}"),
            )
            task.manifestFile.set(
                project.layout.buildDirectory.file("generated/composeSwiftUI/previews/${PreviewCodegen.MANIFEST_FILE_NAME}"),
            )
        }.also { previews ->
            kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                if (target.konanTarget.family.isAppleFamily) {
                    target.compilations.named(KotlinCompilation.MAIN_COMPILATION_NAME) { compilation ->
                        compilation.defaultSourceSet.kotlin.srcDir(previews.flatMap { it.kotlinOutputDirectory.dir(target.name) })
                        // Scratch previews: throwaway code an agent renders without touching the sources.
                        compilation.defaultSourceSet.kotlin.srcDir(project.provider { scratchDirs(project) })
                    }
                }
            }
        }

    /**
     * `renderComposeSwiftUIPreviews`, run by the IntelliJ plugin. Uses the debug framework of the simulator target, and
     * that of the macOS target for the previews rendered on the Mac: only linked when some preview goes there, since
     * linking a framework is the slow part of a render.
     */
    private fun registerPreviewRendering(
        project: Project,
        kotlin: KotlinMultiplatformExtension,
        extension: ComposeSwiftUIExtension,
        previews: TaskProvider<GenerateComposeSwiftUIPreviewsTask>,
    ) {
        // The `device` arguments written in the preview sources: re-read on each build, without breaking the configuration cache.
        val sourceDevices = project.providers.of(PreviewDevicesSource::class.java) { spec ->
            spec.parameters.sources.from(project.provider { previewSourceDirs(project, kotlin) + scratchDirs(project) })
        }
        val platforms = project.provider {
            val ios = previewFramework(kotlin, HostPlatform.IOS)
            val mac = previewFramework(kotlin, HostPlatform.MAC)
            if (ios == null && mac == null) {
                throw GradleException(
                    "Compose SwiftUI previews need the `${previewTargetName(HostPlatform.IOS)}()` or " +
                        "`${previewTargetName(HostPlatform.MAC)}()` target with a debug framework binary: `binaries.framework { }`.",
                )
            }
            // A preview without `device` renders on `previewDevice`, else on the simulator, else on the Mac.
            val default = extension.previewDevice.orNull.orEmpty().ifEmpty { if (ios == null) MAC_DEVICE else "" }
            val devices = sourceDevices.get() + default
            PreviewFrameworks(
                ios = ios?.takeIf { devices.any { !it.equals(MAC_DEVICE, ignoreCase = true) } },
                mac = mac?.takeIf { devices.any { it.equals(MAC_DEVICE, ignoreCase = true) } },
            )
        }
        // A null mapping is an absent provider: the task's framework properties stay unset.
        val framework = platforms.map { it.ios }
        val macFramework = platforms.map { it.mac }
        project.tasks.register("renderComposeSwiftUIPreviews", RenderComposeSwiftUIPreviewsTask::class.java) { task ->
            task.group = "compose swiftui"
            task.description = "Renders the @Preview composables to PNG on the iOS simulator or the Mac."
            task.dependsOn(platforms.map { listOfNotNull(it.ios?.linkTaskProvider, it.mac?.linkTaskProvider) })
            task.frameworkDirectory.set(project.layout.dir(framework.map { it.outputFile }))
            task.frameworkName.set(framework.map { it.baseName })
            task.staticFramework.set(framework.map { it.isStatic })
            task.linkerOpts.set(framework.map { if (it.isStatic) it.linkerOpts else emptyList() }.orElse(emptyList()))
            task.macFrameworkDirectory.set(project.layout.dir(macFramework.map { it.outputFile }))
            task.macFrameworkName.set(macFramework.map { it.baseName })
            task.macStaticFramework.set(macFramework.map { it.isStatic })
            task.macLinkerOpts.set(macFramework.map { if (it.isStatic) it.linkerOpts else emptyList() }.orElse(emptyList()))
            task.manifestFile.set(previews.flatMap { it.manifestFile })
            task.device.set(extension.previewDevice)
            // Set by the IDE plugin: the previews of the open file, or one preview to run interactively.
            task.previewFilter.set(
                project.providers.gradleProperty("composeSwiftUI.previews")
                    .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
                    .orElse(emptyList()),
            )
            task.interactivePreview.set(project.providers.gradleProperty("composeSwiftUI.interactivePreview"))
            // For agents on the command line: the previews of some files, and the image(s) to compare them with.
            task.previewFiles.set(
                project.providers.gradleProperty("composeSwiftUI.previewFile")
                    .map { it.split(',').map(String::trim).filter(String::isNotEmpty).map { path -> userFile(project, path).canonicalPath } }
                    .orElse(emptyList()),
            )
            task.golden.set(project.providers.gradleProperty("composeSwiftUI.golden").map { userFile(project, it).absolutePath })
            task.settleMilliseconds.set(
                project.providers.gradleProperty("composeSwiftUI.previewSettleMs").map(String::toInt).orElse(1500),
            )
            // Compose Multiplatform resources, when the module uses them (the app bundle must carry them).
            // Wire the task's outputs, not the task itself: a Task can't be stored in the configuration cache.
            fun resources(of: org.gradle.api.provider.Provider<Framework>) = project.provider {
                val target = of.orNull?.target ?: return@provider project.files()
                val name = "assemble${target.name.replaceFirstChar { it.uppercase() }}MainResources"
                if (name in project.tasks.names) project.tasks.named(name).get().outputs.files else project.files()
            }
            task.composeResources.from(resources(framework))
            task.macComposeResources.from(resources(macFramework))
            task.workDirectory.set(project.layout.buildDirectory.dir("composeSwiftUI/previewHost"))
            task.outputDirectory.set(project.layout.buildDirectory.dir(PREVIEW_IMAGES_DIR))
        }
    }

    /** A path given on the command line: absolute, or relative to the root project (where `gradlew` runs) or to the module. */
    private fun userFile(project: Project, path: String): File {
        val file = File(path)
        if (file.isAbsolute) return file
        return project.rootDir.resolve(path).takeIf { it.exists() } ?: project.file(path)
    }

    /** `listComposeSwiftUIPreviews`: every preview and its variants, for agents on the command line. */
    private fun registerPreviewList(project: Project, previews: TaskProvider<GenerateComposeSwiftUIPreviewsTask>) {
        project.tasks.register("listComposeSwiftUIPreviews", ListComposeSwiftUIPreviewsTask::class.java) { task ->
            task.group = "compose swiftui"
            task.description = "Lists the @Preview composables and their variants (-PcomposeSwiftUI.previewFile, -PcomposeSwiftUI.format=json)."
            task.manifestFile.set(previews.flatMap { it.manifestFile })
            task.previewFiles.set(
                project.providers.gradleProperty("composeSwiftUI.previewFile")
                    .map { it.split(',').map(String::trim).filter(String::isNotEmpty).map { path -> userFile(project, path).canonicalPath } }
                    .orElse(emptyList()),
            )
            task.format.set(project.providers.gradleProperty("composeSwiftUI.format").orElse("text"))
        }
    }

    private class PreviewFrameworks(val ios: Framework?, val mac: Framework?)

    private fun previewTargetName(platform: HostPlatform): String {
        val arm64 = System.getProperty("os.arch") == "aarch64"
        return when (platform) {
            HostPlatform.IOS -> if (arm64) "iosSimulatorArm64" else "iosX64"
            HostPlatform.MAC -> if (arm64) "macosArm64" else "macosX64"
        }
    }

    /** The debug framework the previews of [platform] render with, if the module has the target of this Mac. */
    private fun previewFramework(kotlin: KotlinMultiplatformExtension, platform: HostPlatform): Framework? {
        val targetName = previewTargetName(platform)
        val target = kotlin.targets.findByName(targetName) as? KotlinNativeTarget ?: return null
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

    /**
     * The scratch directory of agents' throwaway previews, except in Xcode Release builds: a scratch file left behind
     * never ships. An empty directory adds no input, so builds with and without it share their compilations.
     */
    private fun scratchDirs(project: Project): List<File> =
        if (project.providers.environmentVariable("CONFIGURATION").orNull == "Release") emptyList() else listOf(project.layout.buildDirectory.dir(SCRATCH_DIR).get().asFile)

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
        // Source information lets the IDE previews go from a view to its code and patch literals without a build.
        // Off in Xcode Release builds only, so Xcode Debug and the IDE share their compilations. A value set in
        // `composeCompiler { }` wins. (Reflection: the user's build may load the Compose plugin in another class loader.)
        val composeCompiler = project.extensions.findByName("composeCompiler") ?: return
        @Suppress("UNCHECKED_CAST")
        val includeSourceInformation = runCatching {
            composeCompiler.javaClass.getMethod("getIncludeSourceInformation").invoke(composeCompiler) as Property<Boolean>
        }.getOrNull() ?: return
        includeSourceInformation.convention(
            project.providers.environmentVariable("CONFIGURATION").map { it != "Release" }.orElse(true),
        )
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

        /** Scratch previews (under the build directory): compiled into the frameworks and scanned like the sources. */
        const val SCRATCH_DIR = "composeSwiftUI/scratch"

        /** `@Preview(device = "mac")`: rendered on this Mac. */
        const val MAC_DEVICE = RenderComposeSwiftUIPreviewsTask.MAC_HOST
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

    /** Integration files to write (names without `.swift`); empty: all. */
    @get:Input
    @get:Optional
    abstract val integrations: ListProperty<String>

    @TaskAction
    fun sync() {
        val out = outputDirectory.get().asFile
        val all = RendererResources.files()
        val files = all.filterNot { it.startsWith(INTEGRATIONS) }

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
        // Opt-in framework renderers, next to the renderer: add only the ones the app uses to its target.
        val integrationsOut = out.resolveSibling("ComposeSwiftUIIntegrations")
        val available = all.filter { it.startsWith(INTEGRATIONS) }
        val selected = integrations.orNull.orEmpty().toSet()
        val unknown = selected - available.map { it.removePrefix(INTEGRATIONS).removeSuffix(".swift") }.toSet()
        if (unknown.isNotEmpty()) {
            throw GradleException("Unknown Compose SwiftUI integrations $unknown; available: " +
                available.joinToString { it.removePrefix(INTEGRATIONS).removeSuffix(".swift") })
        }
        val written = available.filter { selected.isEmpty() || it.removePrefix(INTEGRATIONS).removeSuffix(".swift") in selected }
        // Integrations left out of the selection (or from older versions) go away; other files are the user's.
        val ours = available.map { it.removePrefix(INTEGRATIONS) }.toSet() + RETIRED_INTEGRATIONS
        integrationsOut.listFiles().orEmpty()
            .filter { it.name in ours && "$INTEGRATIONS${it.name}" !in written }
            .forEach { it.delete() }
        for (rel in written) {
            val bytes = RendererResources.read(rel)
            val target = integrationsOut.resolve(rel.removePrefix(INTEGRATIONS))
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
        const val INTEGRATIONS = "integrations/"

        /** Integration files of older versions, split into one file per framework since. */
        val RETIRED_INTEGRATIONS = setOf("AppServices.swift", "SystemServices.swift")
    }
}

/**
 * Finds the `@Preview @Composable` functions and generates `ComposeSwiftUIPreviews` (Kotlin, one file per Apple
 * target) and `ComposeSwiftUIPreviews.swift` (the `#Preview`s and the gallery, copied into the Xcode project by
 * [SyncComposeSwiftUIRendererTask]). Files are still generated when there is no preview.
 */
@CacheableTask
abstract class GenerateComposeSwiftUIPreviewsTask : DefaultTask() {
    // Absolute: the manifest records where each preview is declared, for the IDE.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val targetNames: ListProperty<String>

    @get:OutputDirectory
    abstract val kotlinOutputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val swiftOutputFile: RegularFileProperty

    /** Previews and their variants, read by the host app that renders them (see [RenderComposeSwiftUIPreviewsTask]). */
    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val previews = PreviewScanner.scanAll(
            sources.asFileTree.matching { it.include("**/*.kt") }.files.sorted().map { it.path to it.readText() },
        )
        val entries = PreviewScanner.entries(previews)

        val kotlinRoot = kotlinOutputDirectory.get().asFile
        kotlinRoot.deleteRecursively()
        val kotlinCode = PreviewCodegen.kotlin(entries)
        for (target in targetNames.get()) {
            kotlinRoot.resolve("$target/${PreviewCodegen.OBJECT_NAME}.kt").apply { parentFile.mkdirs() }.writeText(kotlinCode)
        }
        swiftOutputFile.get().asFile.apply { parentFile.mkdirs() }.writeText(PreviewCodegen.swift(entries))
        manifestFile.get().asFile.apply { parentFile.mkdirs() }.writeText(PreviewCodegen.manifest(entries))
        logger.info("Compose SwiftUI: ${entries.size} preview(s): ${entries.joinToString { it.function.qualifiedName }}")
    }
}
