package dev.nucleusframework.composeswiftui.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Family
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import org.gradle.process.ExecOperations
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * `assembleComposeSwiftUIPackage`: a Swift package (and a podspec) with the module's frameworks as an XCFramework and
 * the Swift renderer of the same version, in `build/composeSwiftUI/package`. An app adds it like any package (local,
 * or published from CI) instead of running Gradle from an Xcode build phase: no script, no `ENABLE_USER_SCRIPT_SANDBOXING
 * = NO`, and a renderer that only changes when the package version does.
 */
internal fun registerSwiftPackage(project: Project, kotlin: KotlinMultiplatformExtension, extension: ComposeSwiftUIExtension) {
    val buildType = extension.packageBuildType.map { if (it.equals("debug", ignoreCase = true)) NativeBuildType.DEBUG else NativeBuildType.RELEASE }
    val xcframeworkDir = project.layout.buildDirectory.dir("composeSwiftUI/xcframework")
    val frameworks = project.provider {
        kotlin.targets.withType(KotlinNativeTarget::class.java)
            .filter { it.konanTarget.family.isAppleFamily }
            .flatMap { target -> target.binaries.withType(Framework::class.java).filter { it.buildType == buildType.get() } }
    }
    val name = extension.frameworkName.orElse(project.provider { frameworks.get().firstOrNull()?.baseName ?: project.name })
    val xcframework = project.tasks.register("assembleComposeSwiftUIXCFramework", AssembleComposeSwiftUIXCFrameworkTask::class.java) { task ->
        task.group = "compose swiftui"
        task.description = "Assembles the module's Apple frameworks into the XCFramework of the Compose SwiftUI package."
        task.dependsOn(project.provider { frameworks.get().map { it.linkTaskProvider } })
        task.frameworkName.set(name)
        task.slices.set(
            project.provider {
                frameworks.get().map { framework ->
                    val target = framework.target.konanTarget
                    "${target.family.name}|${target.name.contains("simulator") || target.name.endsWith("x64")}|${framework.outputFile.absolutePath}"
                }
            },
        )
        task.frameworkFiles.from(project.provider { frameworks.get().map { it.outputFile } })
        task.outputDirectory.set(xcframeworkDir)
    }
    project.tasks.register("assembleComposeSwiftUIPackage", AssembleComposeSwiftUIPackageTask::class.java) { task ->
        task.group = "compose swiftui"
        task.description = "Writes a Swift package (and a podspec): the XCFramework and the Swift renderer of the same version."
        task.dependsOn(xcframework)
        task.xcframeworkDirectory.set(xcframeworkDir)
        task.frameworkName.set(name)
        task.platforms.set(project.provider { frameworks.get().map { it.target.konanTarget.family.name }.distinct() })
        task.integrations.set(extension.integrations)
        task.version.set(BuildConfig.LIBRARY_VERSION)
        task.outputDirectory.set(project.layout.buildDirectory.dir("composeSwiftUI/package"))
    }
}

/**
 * `xcodebuild -create-xcframework` over the linked frameworks. Frameworks of the same platform (watchOS arm64_32 and
 * arm64 devices) are merged with `lipo` first. Kotlin's own XCFramework task isn't used: it flattens the symbolic links
 * of macOS frameworks, which then fail to sign.
 */
@DisableCachingByDefault(because = "Runs xcodebuild")
abstract class AssembleComposeSwiftUIXCFrameworkTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Input
    abstract val frameworkName: Property<String>

    /** `family|simulator|path` of each framework. */
    @get:Input
    abstract val slices: ListProperty<String>

    /** The frameworks themselves, so a relinked framework reassembles the XCFramework. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val frameworkFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun assemble() {
        val name = frameworkName.get()
        val out = outputDirectory.get().asFile.apply { deleteRecursively(); mkdirs() }
        val work = temporaryDir.apply { deleteRecursively(); mkdirs() }
        val groups = slices.get().map { it.split('|') }.groupBy { it[0] to it[1] }.values
        val frameworks = groups.mapIndexed { i, group ->
            val paths = group.map { File(it[2]) }
            if (paths.size == 1) return@mapIndexed paths.single()
            // Same platform, several architectures: one fat framework
            val merged = work.resolve("$i/$name.framework")
            merged.parentFile.mkdirs()
            exec.exec { it.commandLine("ditto", paths.first().absolutePath, merged.absolutePath) }
            val binary = merged.resolve(name).canonicalFile
            exec.exec { it.commandLine(listOf("lipo", "-create") + paths.map { p -> p.resolve(name).canonicalPath } + listOf("-output", binary.absolutePath)) }
            merged
        }
        exec.exec {
            it.commandLine(
                listOf("xcodebuild", "-create-xcframework") + frameworks.flatMap { f -> listOf("-framework", f.absolutePath) } +
                    listOf("-output", out.resolve("$name.xcframework").absolutePath),
            )
        }
    }
}

@DisableCachingByDefault(because = "Copies files")
abstract class AssembleComposeSwiftUIPackageTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcframeworkDirectory: DirectoryProperty

    @get:Input
    abstract val frameworkName: Property<String>

    /** Kotlin/Native families of the frameworks (`IOS`, `OSX`, `TVOS`, `WATCHOS`). */
    @get:Input
    abstract val platforms: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val integrations: ListProperty<String>

    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun assemble() {
        val name = frameworkName.get()
        val out = outputDirectory.get().asFile.apply { deleteRecursively(); mkdirs() }
        val xcframework = xcframeworkDirectory.get().asFile.walkTopDown().firstOrNull { it.name == "$name.xcframework" }
            ?: throw GradleException("No $name.xcframework in ${xcframeworkDirectory.get().asFile}")
        copyPreservingLinks(xcframework.toPath(), out.resolve(xcframework.name).toPath())

        // The renderer and the listed integrations form one module: the integrations use its internal helpers.
        val sources = out.resolve("Sources/ComposeSwiftUI").apply { mkdirs() }
        val all = RendererResources.files()
        all.filterNot { it.startsWith("integrations/") }.forEach { rel ->
            sources.resolve(rel).apply { parentFile.mkdirs() }.writeBytes(RendererResources.read(rel))
        }
        val selected = integrations.orNull.orEmpty()
        val available = all.filter { it.startsWith("integrations/") }.associateBy { it.removePrefix("integrations/").removeSuffix(".swift") }
        val unknown = selected - available.keys
        if (unknown.isNotEmpty()) throw GradleException("Unknown Compose SwiftUI integrations $unknown; available: ${available.keys.sorted()}")
        val integrationSources = selected.map { it to RendererResources.read(available.getValue(it)).decodeToString() }
        integrationSources.forEach { (integration, source) -> sources.resolve("Integrations/$integration.swift").apply { parentFile.mkdirs() }.writeText(source) }
        sources.resolve("KotlinModule.swift").writeText("@_exported import $name\n")
        sources.resolve(IntegrationsRegistration.FILE).writeText(IntegrationsRegistration.swift(integrationSources))

        val families = platforms.get().toSet()
        out.resolve("Package.swift").writeText(packageSwift(name, families))
        out.resolve("ComposeSwiftUI$name.podspec").writeText(podspec(name, families, version.get()))
        out.resolve("README.txt").writeText(
            "Generated by the dev.nucleusframework.compose-swiftui Gradle plugin (${version.get()}): the Kotlin framework $name " +
                "and the Compose SwiftUI renderer.\nSwift Package Manager: add this folder as a local package (or publish it), then " +
                "`import ComposeSwiftUI`.\nCocoaPods: pod 'ComposeSwiftUI$name', :path => '<this folder>'\n",
        )
        logger.lifecycle("Compose SwiftUI package written to $out")
    }

    /** macOS frameworks are made of symbolic links (`Versions/Current`): copy them as links. */
    private fun copyPreservingLinks(from: Path, to: Path) {
        Files.walk(from).use { paths ->
            paths.forEach { source ->
                val target = to.resolve(from.relativize(source).toString())
                when {
                    Files.isSymbolicLink(source) -> Files.createSymbolicLink(target, Files.readSymbolicLink(source))
                    Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) -> Files.createDirectories(target)
                    else -> Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)
                }
            }
        }
    }

    private fun swiftPlatforms(families: Set<String>) = listOfNotNull(
        ".iOS(.v17)".takeIf { Family.IOS.name in families },
        ".macOS(.v14)".takeIf { Family.OSX.name in families },
        ".tvOS(.v17)".takeIf { Family.TVOS.name in families },
        ".watchOS(.v10)".takeIf { Family.WATCHOS.name in families },
    )

    private fun packageSwift(name: String, families: Set<String>) = """
        |// swift-tools-version:5.9
        |// Generated by the Compose SwiftUI Gradle plugin. Do not edit.
        |import PackageDescription
        |
        |let package = Package(
        |    name: "ComposeSwiftUI$name",
        |    platforms: [${swiftPlatforms(families).joinToString(", ")}],
        |    products: [
        |        .library(name: "ComposeSwiftUI", targets: ["ComposeSwiftUI"]),
        |    ],
        |    targets: [
        |        .binaryTarget(name: "$name", path: "$name.xcframework"),
        |        .target(
        |            name: "ComposeSwiftUI",
        |            dependencies: ["$name"],
        |            linkerSettings: [.linkedLibrary("c++")]
        |        ),
        |    ],
        |    swiftLanguageVersions: [.v5]
        |)
        |""".trimMargin()

    private fun podspec(name: String, families: Set<String>, version: String): String {
        val platforms = listOfNotNull(
            "  s.ios.deployment_target = '17.0'".takeIf { Family.IOS.name in families },
            "  s.osx.deployment_target = '14.0'".takeIf { Family.OSX.name in families },
            "  s.tvos.deployment_target = '17.0'".takeIf { Family.TVOS.name in families },
            "  s.watchos.deployment_target = '10.0'".takeIf { Family.WATCHOS.name in families },
        )
        return """
            |# Generated by the Compose SwiftUI Gradle plugin. Do not edit.
            |Pod::Spec.new do |s|
            |  s.name = 'ComposeSwiftUI$name'
            |  s.module_name = 'ComposeSwiftUI'
            |  s.version = '$version'
            |  s.summary = 'The Kotlin framework $name and the Compose SwiftUI renderer.'
            |  s.homepage = 'https://github.com/NucleusFramework/ComposeSwiftUI'
            |  s.license = { :type => 'MIT' }
            |  s.authors = 'NucleusFramework'
            |  s.source = { :http => 'https://github.com/NucleusFramework/ComposeSwiftUI' }
            |${platforms.joinToString("\n")}
            |  s.swift_version = '5.0'
            |  s.vendored_frameworks = '$name.xcframework'
            |  s.source_files = 'Sources/ComposeSwiftUI/**/*.swift'
            |  s.libraries = 'c++'
            |end
            |""".trimMargin()
    }
}
