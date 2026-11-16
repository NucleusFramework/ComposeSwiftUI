package dev.nucleusframework.composeswiftui.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** The Swift renderer sources embedded in the plugin jar by the `embedSwiftRenderer` build task. */
internal object RendererResources {
    private val loader = RendererResources::class.java.classLoader

    fun files(): List<String> =
        loader.getResourceAsStream("${BuildConfig.RENDERER_RESOURCES}/index.txt")?.bufferedReader()?.readText()
            ?.lines()?.filter { it.isNotBlank() }
            ?: throw GradleException("Compose SwiftUI: renderer resources missing from the plugin jar")

    fun read(path: String): ByteArray =
        loader.getResourceAsStream("${BuildConfig.RENDERER_RESOURCES}/$path")!!.use { it.readBytes() }

    fun previewHost(): String =
        loader.getResourceAsStream("dev/nucleusframework/composeswiftui/preview-host/PreviewHost.swift")!!
            .bufferedReader().use { it.readText() }
}

/**
 * Renders every `@Preview` to `<outputDirectory>/<qualified name>.png`, with real SwiftUI on the iOS simulator:
 * builds a small host app from the renderer, the generated preview list and the debug simulator framework, runs it
 * on the booted simulator (or boots the first available iPhone), and lets it snapshot each preview.
 *
 * Used by the Compose SwiftUI IntelliJ plugin to show the previews next to the code.
 */
@DisableCachingByDefault(because = "Runs the iOS simulator")
abstract class RenderComposeSwiftUIPreviewsTask : DefaultTask() {
    /** Debug `.framework` for the simulator of this Mac. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val frameworkDirectory: DirectoryProperty

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    abstract val staticFramework: Property<Boolean>

    /** The framework's `linkerOpts`, e.g. SQLDelight's `-lsqlite3`: a static framework leaves them to the app link. */
    @get:Input
    abstract val linkerOpts: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val previewsSwiftFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val hostSwiftFile: RegularFileProperty

    /** Compose Multiplatform resources, copied to `compose-resources` in the app bundle. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResources: ConfigurableFileCollection

    /** Simulator name or UDID. Default: the booted simulator, else the first available iPhone. */
    @get:Input
    @get:Optional
    abstract val device: Property<String>

    @get:Input
    val rendererVersion: String = BuildConfig.LIBRARY_VERSION

    @get:Internal
    abstract val workDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun render() {
        val work = workDirectory.get().asFile
        val app = buildHostApp(work)
        val udid = simulator()
        exec("xcrun", "simctl", "install", udid, app.path)

        val out = outputDirectory.get().asFile.apply { deleteRecursively(); mkdirs() }
        // simctl passes SIMCTL_CHILD_* variables to the app without the prefix.
        exec(
            "xcrun", "simctl", "launch", "--console", "--terminate-running-process", udid, BUNDLE_ID,
            environment = mapOf("SIMCTL_CHILD_COMPOSE_SWIFTUI_PREVIEW_OUTPUT" to out.path),
            timeoutMinutes = 5,
        )
        val count = out.listFiles { f -> f.extension == "png" }?.size ?: 0
        logger.lifecycle("Compose SwiftUI: $count preview(s) rendered to $out")
    }

    private fun buildHostApp(work: File): File {
        val src = work.resolve("src").apply { deleteRecursively(); mkdirs() }
        for (rel in RendererResources.files()) {
            src.resolve(rel).apply { parentFile.mkdirs() }.writeBytes(RendererResources.read(rel))
        }
        src.resolve("KotlinModule.swift").writeText("@_exported import ${frameworkName.get()}\n")
        src.resolve("PreviewHost.swift").writeText(RendererResources.previewHost())
        previewsSwiftFile.get().asFile.copyTo(src.resolve(PreviewCodegen.SWIFT_FILE_NAME))
        hostSwiftFile.get().asFile.copyTo(src.resolve("ComposeSwiftUIRenderedPreviews.swift"))

        val framework = frameworkDirectory.get().asFile
        val app = work.resolve("$HOST_NAME.app").apply { deleteRecursively(); mkdirs() }
        val sdk = exec("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path").trim()
        val arch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "x86_64"
        val common = arrayOf("-target", "$arch-apple-ios17.0-simulator", "-sdk", sdk, "-F", framework.parentFile.path)

        // The Swift side only changes with the renderer, the previews list or the framework API (headers): reuse
        // its object file otherwise, so a Kotlin edit only relinks.
        val sources = src.walkTopDown().filter { it.extension == "swift" }.sorted().toList()
        val api = framework.walkTopDown().filter { it.isFile && (it.parentFile.name == "Headers" || it.parentFile.name == "Modules") }
        val key = sha256((sources + api.sorted()).map { it.readBytes() } + sdk.toByteArray())
        val objectFile = work.resolve("host-$key.o")
        if (!objectFile.exists()) {
            work.listFiles { f -> f.name.startsWith("host-") }?.forEach { it.delete() }
            logger.lifecycle("Compose SwiftUI: compiling the preview host")
            exec(
                "xcrun", "swiftc", "-c", "-wmo", "-parse-as-library", "-swift-version", "5", "-Onone", *common,
                "-module-name", HOST_NAME, "-o", objectFile.path, *sources.map { it.path }.toTypedArray(),
                timeoutMinutes = 15,
            )
        }
        try {
            exec(
                "xcrun", "swiftc", *common, objectFile.path, "-framework", frameworkName.get(), "-lc++",
                // system libraries that common KMP libraries link through the app (SQLDelight: sqlite3)
                "-lsqlite3", "-lz",
                "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks", "-o", app.resolve(HOST_NAME).path,
                // Like Xcode's default DEAD_CODE_STRIPPING: unused C interop wrappers must not need their symbols.
                "-Xlinker", "-dead_strip",
                *linkerOpts.get().flatMap { listOf("-Xlinker", it) }.toTypedArray(),
            )
        } catch (e: GradleException) {
            if ("Undefined symbols" !in e.message.orEmpty()) throw e
            throw GradleException(
                "Compose SwiftUI: the preview host doesn't get the app's Xcode linker flags (OTHER_LDFLAGS). Declare " +
                    "the libraries the framework needs on it, e.g. `binaries.framework { linkerOpts(\"-lsqlite3\") }` " +
                    "for SQLDelight.\n${e.message}",
            )
        }
        composeResources.files.filter { it.isDirectory }.forEach { it.copyRecursively(app.resolve("compose-resources"), overwrite = true) }
        if (!staticFramework.get()) framework.copyRecursively(app.resolve("Frameworks/${framework.name}"))
        app.resolve("Info.plist").writeText(INFO_PLIST)
        exec("codesign", "--force", "--deep", "--sign", "-", app.path)
        return app
    }

    /** UDID of the requested, booted or first available iPhone simulator, booted if needed. */
    private fun simulator(): String {
        val lines = exec("xcrun", "simctl", "list", "devices", "available").lines()
        val deviceRegex = Regex("""^\s+(.+?) \(([0-9A-F-]{36})\) \((\w+)\)""")
        val devices = lines.mapNotNull { deviceRegex.find(it)?.destructured }
            .map { (name, udid, state) -> Triple(name, udid, state) }
        val wanted = device.orNull
        val (name, udid, state) = when {
            wanted != null -> devices.firstOrNull { it.first == wanted || it.second == wanted }
            else -> devices.firstOrNull { it.third == "Booted" } ?: devices.lastOrNull { it.first.startsWith("iPhone") }
        } ?: throw GradleException("Compose SwiftUI: no iOS simulator found${wanted?.let { " named $it" }.orEmpty()}")
        if (state != "Booted") {
            logger.lifecycle("Compose SwiftUI: booting $name")
            exec("xcrun", "simctl", "bootstatus", udid, "-b", timeoutMinutes = 5)
        }
        return udid
    }

    private fun sha256(parts: List<ByteArray>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEach(digest::update)
        return digest.digest().joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun exec(
        vararg command: String,
        environment: Map<String, String> = emptyMap(),
        timeoutMinutes: Long = 2,
    ): String {
        val process = ProcessBuilder(*command).redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
        // Read before waiting, so a full pipe never blocks the process.
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw GradleException("Compose SwiftUI: timed out: ${command.joinToString(" ")}")
        }
        if (process.exitValue() != 0) {
            throw GradleException("Compose SwiftUI: `${command.take(3).joinToString(" ")}` failed:\n$output")
        }
        logger.info(output)
        return output
    }

    private companion object {
        const val HOST_NAME = "ComposeSwiftUIPreviewHost"
        const val BUNDLE_ID = "dev.nucleusframework.composeswiftui.previewhost"
        val INFO_PLIST = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>CFBundleIdentifier</key><string>$BUNDLE_ID</string>
            |    <key>CFBundleExecutable</key><string>$HOST_NAME</string>
            |    <key>CFBundleName</key><string>Previews</string>
            |    <key>CFBundlePackageType</key><string>APPL</string>
            |    <key>CFBundleVersion</key><string>1</string>
            |    <key>CFBundleShortVersionString</key><string>1.0</string>
            |    <key>CFBundleSupportedPlatforms</key><array><string>iPhoneSimulator</string></array>
            |    <key>MinimumOSVersion</key><string>17.0</string>
            |    <key>UIDeviceFamily</key><array><integer>1</integer><integer>2</integer></array>
            |    <key>UILaunchScreen</key><dict/>
            |    <key>CADisableMinimumFrameDurationOnPhone</key><true/>
            |</dict>
            |</plist>
            |""".trimMargin()
    }
}
