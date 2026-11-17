package dev.nucleusframework.composeswiftui.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
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

    /** The preview host app: name to source. */
    fun previewHost(): List<Pair<String, String>> = listOf("PreviewHost.swift", "PreviewInteractive.swift", "PreviewTouchSynthesis.swift").map { name ->
        name to loader.getResourceAsStream("dev/nucleusframework/composeswiftui/preview-host/$name")!!.bufferedReader().use { it.readText() }
    }
}

/**
 * Renders the `@Preview`s to `<outputDirectory>/<key>.png`, with real SwiftUI on the iOS simulator: builds a small
 * host app from the renderer and the debug simulator framework, runs it on the simulator of each preview (the
 * booted one by default, or the first available iPhone), and lets it snapshot the previews. Writes
 * `<outputDirectory>/results.json`, read by the Compose SwiftUI IntelliJ plugin to show the previews next to the code.
 *
 * Only what changed is redone: the host's Swift code is compiled once per renderer version, the app is relinked
 * and reinstalled only when the framework changed, and [previewFilter] renders the previews of one file.
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

    /** Previews and their variants, from `generateComposeSwiftUIPreviews`. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifestFile: RegularFileProperty

    /** Compose Multiplatform resources, copied to `compose-resources` in the app bundle. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResources: ConfigurableFileCollection

    /** Simulator name or UDID. Default: `Compose SwiftUI Previews`, created like the booted iPhone and run in the background. */
    @get:Input
    @get:Optional
    abstract val device: Property<String>

    /** Qualified names of the previews to render (`-PcomposeSwiftUI.previews=a.B,c.D`). Empty: all of them. */
    @get:Input
    abstract val previewFilter: ListProperty<String>

    /** Key of a preview to open live on the simulator instead of rendering (`-PcomposeSwiftUI.interactivePreview`). */
    @get:Input
    @get:Optional
    abstract val interactivePreview: Property<String>

    /** Longest wait for a preview that keeps changing (an endless animation) before it is snapshotted. */
    @get:Input
    abstract val settleMilliseconds: Property<Int>

    @get:Input
    val rendererVersion: String = BuildConfig.LIBRARY_VERSION

    @get:Internal
    abstract val workDirectory: DirectoryProperty

    /** Images and `results.json`. Not an `@OutputDirectory`: rendering one file keeps the images of the others. */
    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    init {
        // The simulator is outside Gradle's view: always render when asked.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun render() {
        val started = System.nanoTime()
        val work = workDirectory.get().asFile.apply { mkdirs() }
        val out = outputDirectory.get().asFile.apply { mkdirs() }
        val manifest = readManifest()

        // The IDE's request (written just before it runs the task): its previews and interactive key. A file keeps the
        // Gradle arguments the same from one render to the next, so the configuration cache is reused.
        val ideRequest = work.resolve(IDE_REQUEST_FILE_NAME).takeIf { it.isFile }?.let { file ->
            @Suppress("UNCHECKED_CAST")
            (runCatching { JsonSlurper().parse(file) as Map<String, Any?> }.getOrNull()).also { file.delete() }
        }
        val interactive = interactivePreview.orNull ?: ideRequest?.get("interactive") as? String
        if (interactive != null) {
            openInteractive(manifest, interactive, timed("host app") { buildHostApp(work) }, work)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val wanted = previewFilter.get().ifEmpty { ideRequest?.get("previews") as? List<String> ?: emptyList() }.toSet()
        val jobs = manifest.filter { wanted.isEmpty() || it.id in wanted }.flatMap { it.jobs() }
        val byDevice = jobs.groupBy { it.device.ifEmpty { device.orNull.orEmpty() } }
        // One simulator per device, found (and booted) while the app links, then rendering in parallel.
        val pool = Executors.newFixedThreadPool(byDevice.size.coerceAtLeast(1))
        val devices = work.resolve(DEVICES_FILE_NAME)
        @Suppress("UNCHECKED_CAST")
        val known = runCatching { JsonSlurper().parse(devices) as Map<String, String> }.getOrNull().orEmpty()
        val results = try {
            val simulators = byDevice.keys.associateWith { name ->
                // A host still running there: the simulator is booted, no need to ask simctl.
                val running = known[name]?.takeIf { udid -> hostPid(work.resolve("host-$udid.json"))?.let(::isAlive) == true }
                pool.submit(Callable { running ?: timed("simulator") { simulator(name.ifEmpty { null }) } })
            }
            val app = timed("host app") { buildHostApp(work) }
            // For the IDE, which talks to the running hosts directly (live preview, literal edits).
            // Merged: a render of some previews (or none) keeps the simulators of the others.
            devices.writeText(JsonOutput.toJson(known + simulators.mapValues { it.value.get() }))
            val rendered = byDevice.map { (deviceName, deviceJobs) ->
                pool.submit(Callable { renderOn(deviceName, simulators.getValue(deviceName).get(), deviceJobs, app, work, out) })
            }.flatMap { it.get() }
            // Hosts of other simulators still running an older build: updated, for the IDE's live literals and previews.
            val used = simulators.values.map { it.get() }.toSet()
            (known.values.toSet() - used).filter { udid ->
                val pid = hostPid(work.resolve("host-$udid.json"))
                pid != null && isAlive(pid) && work.resolve("host-$udid.build").let { !it.exists() || it.readText() != work.resolve("app.stamp").readText() }
            }.forEach { udid ->
                timed("update host") {
                    install(udid, app, work)
                    launch(udid, emptyList(), BATCH_SIZE, out, work)
                }
            }
            rendered
        } finally {
            pool.shutdown()
        }
        writeResults(out, manifest, jobs.map { it.id }.toSet(), results)

        val failed = results.count { it["error"] != null }
        logger.lifecycle(
            "Compose SwiftUI: ${results.size - failed} preview(s) rendered" +
                (if (failed > 0) ", $failed failed" else "") +
                " in ${(System.nanoTime() - started) / 1_000_000} ms to $out",
        )
    }

    /** A preview of the manifest. */
    private class ManifestPreview(val id: String, val source: String, val parameterized: Boolean, val variants: List<Map<String, Any?>>) {
        fun jobs() = variants.map { Job(this, it) }
    }

    /** One variant to render: the request entry of the host app. */
    private class Job(val preview: ManifestPreview, val variant: Map<String, Any?>) {
        val id get() = preview.id
        val key get() = variant["key"] as String
        val device get() = variant["device"] as? String ?: ""

        fun toRequest(index: Int = -1): Map<String, Any?> =
            variant + mapOf("id" to id, "parameterized" to preview.parameterized, "index" to index)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readManifest(): List<ManifestPreview> {
        val json = JsonSlurper().parse(manifestFile.get().asFile) as Map<String, Any?>
        return (json["previews"] as List<Map<String, Any?>>).map {
            ManifestPreview(
                id = it["id"] as String,
                source = it["source"] as? String ?: "",
                parameterized = it["parameterized"] as Boolean,
                variants = it["variants"] as List<Map<String, Any?>>,
            )
        }
    }

    /**
     * Renders [jobs] on the simulator [udid] (named [deviceName] by the previews) and returns one result per rendered
     * image or failed preview. A crash (a Kotlin exception in a preview) ends the app: the previews it was composing
     * are rendered again one at a time, so only the culprit fails, with the end of the app's output as its error.
     */
    private fun renderOn(deviceName: String, udid: String, jobs: List<Job>, app: File, work: File, out: File): List<Map<String, Any?>> {
        timed("install") { install(udid, app, work) }
        val results = ArrayList<Map<String, Any?>>()
        // Previews that crashed last time likely crash again: alone and first, so only they cost a relaunch.
        val crashed = previousResults(out).filter { it["crashed"] == true }.map { it["key"] as String }.toSet()
        var suspects = jobs.filter { job -> crashed.any { it.substringBefore('#') == job.key } }
        var pending = jobs - suspects.toSet()
        while (pending.isNotEmpty() || suspects.isNotEmpty()) {
            val isolating = suspects.isNotEmpty()
            val batch = if (isolating) suspects else pending
            val run = timed("render ${batch.size} preview(s)") { launch(udid, batch, if (isolating) 1 else BATCH_SIZE, out, work) }
            val begun = run.begun.map { it.substringBefore('#') }.toSet()
            val doneUnits = run.done.map { it["key"] as String }.toSet()
            // A job is unfinished when one of its units (parameter values) began and never ended.
            val unfinished = run.begun.filter { it !in doneUnits }.map { it.substringBefore('#') }.toSet()
            results += run.done.filter { (it["key"] as String).substringBefore('#') !in unfinished }
            results += run.errors
            if (run.completed) {
                if (isolating) suspects = emptyList() else pending = emptyList()
                continue
            }
            if (isolating) {
                val culprit = batch.firstOrNull { it.key in unfinished }
                    ?: throw GradleException("Compose SwiftUI: the preview host stopped unexpectedly:\n${run.output.takeLast(4000)}")
                val error = if (run.stalled) {
                    "The preview didn't finish within $STALL_SECONDS s: it blocks the main thread (an endless loop?)"
                } else {
                    crashMessage(run.output)
                }
                results += mapOf("key" to culprit.key, "id" to culprit.id, "error" to error, "crashed" to true)
                suspects = batch.filter { it.key !in begun || (it.key in unfinished && it !== culprit) }
            } else {
                suspects = batch.filter { it.key in unfinished }
                if (suspects.isEmpty()) {
                    throw GradleException("Compose SwiftUI: the preview host stopped unexpectedly:\n${run.output.takeLast(4000)}")
                }
                pending = batch.filter { it.key !in begun }
            }
        }
        // A preview failing before it begins (its provider) fails again in the isolation runs: once is enough.
        return results.associateBy { it["key"] }.values.map { it + mapOf("device" to deviceName) }
    }

    private class Run(
        val completed: Boolean,
        val begun: List<String>,
        val done: List<Map<String, Any?>>,
        val errors: List<Map<String, Any?>>,
        val output: String,
        /** Killed after [STALL_SECONDS] without progress. */
        val stalled: Boolean = false,
    )

    /**
     * Renders [jobs] with the host app on [udid]: sends the request to the running host when it is the current build,
     * otherwise (re)launches it first. Follows the response until `CSUI-END`, or until the app dies (a crash).
     */
    private fun launch(udid: String, jobs: List<Job>, batchSize: Int, out: File, work: File): Run =
        // One request at a time per host, with the IDE's (live literals): the host only reads the latest one.
        java.io.RandomAccessFile(work.resolve("request-$udid.lock"), "rw").channel.use { channel ->
            channel.lock().use { request(udid, jobs, batchSize, out, work) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun request(udid: String, jobs: List<Job>, batchSize: Int, out: File, work: File): Run {
        val seq = System.currentTimeMillis() * 1000 + (System.nanoTime() / 1000) % 1000
        val response = work.resolve("response-$udid.log").apply { delete() }
        val request = work.resolve("request-$udid.json")
        val state = work.resolve("host-$udid.json")
        val build = work.resolve("host-$udid.build")
        val bundleKey = work.resolve("app.stamp").readText()
        val started = System.nanoTime()
        var pid = hostPid(state)?.takeIf { build.exists() && build.readText() == bundleKey && isAlive(it) }
        // Another app came to the foreground of the simulator (the host of another module): only the foreground app renders.
        if (pid != null && !hostActive(state)) activate(udid, work, state)
        // Written in one move: the host polls the file and must never read half of it.
        val tmp = work.resolve("request-$udid.json.tmp")
        tmp.writeText(
            JsonOutput.toJson(
                mapOf(
                    "seq" to seq,
                    "response" to response.path,
                    "output" to out.path,
                    "jobs" to jobs.map { it.toRequest() },
                    "batchSize" to batchSize,
                    "settleMilliseconds" to settleMilliseconds.get(),
                    "interactive" to false,
                ),
            ),
        )
        java.nio.file.Files.move(tmp.toPath(), request.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)

        if (pid == null) {
            stopHost(udid, work)
            // simctl passes SIMCTL_CHILD_* variables to the app without the prefix.
            exec(
                "xcrun", "simctl", "launch", "--terminate-running-process", udid, bundleId(work),
                environment = mapOf(
                    "SIMCTL_CHILD_COMPOSE_SWIFTUI_PREVIEW_REQUEST" to request.path,
                    "SIMCTL_CHILD_COMPOSE_SWIFTUI_PREVIEW_STATE" to state.path,
                ),
            )
            build.writeText(bundleKey)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
            while (pid == null && System.nanoTime() < deadline) {
                Thread.sleep(20)
                pid = hostPid(state)
            }
            pid ?: throw GradleException("Compose SwiftUI: the preview host didn't start on the simulator")
            logger.info("Compose SwiftUI: host launched in ${(System.nanoTime() - started) / 1_000_000} ms")
        }

        val begun = ArrayList<String>()
        val done = ArrayList<Map<String, Any?>>()
        val errors = ArrayList<Map<String, Any?>>()
        var completed = false
        val output = StringBuilder()
        val slurper = JsonSlurper()
        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(LAUNCH_TIMEOUT_MINUTES)
        var read = 0L
        var partial = ""
        var progressAt = System.nanoTime()
        var stalled = false
        fun drain() {
            if (!response.exists()) return
            java.io.RandomAccessFile(response, "r").use { file ->
                if (file.length() <= read) return
                file.seek(read)
                val bytes = ByteArray((file.length() - read).toInt())
                file.readFully(bytes)
                read += bytes.size
                progressAt = System.nanoTime()
                val text = partial + String(bytes)
                val lines = text.split('\n')
                partial = lines.last()
                for (line in lines.dropLast(1)) {
                    output.appendLine(line)
                    logger.info(line)
                    when {
                        line.startsWith("CSUI-BEGIN ") -> begun += line.removePrefix("CSUI-BEGIN ")
                        line.startsWith("CSUI-DONE ") -> done += slurper.parseText(line.removePrefix("CSUI-DONE ")) as Map<String, Any?>
                        line.startsWith("CSUI-ERROR ") -> line.removePrefix("CSUI-ERROR ").split(' ', limit = 2).let {
                            errors += mapOf("key" to it[0], "id" to it[0].substringBefore('~').substringBefore('#'), "error" to it.getOrElse(1) { "" }.replace("\\n", "\n"))
                        }
                        line == "CSUI-END" -> completed = true
                    }
                }
            }
        }
        while (!completed) {
            drain()
            if (completed) break
            if (!isAlive(pid)) {
                Thread.sleep(100)
                drain()
                break
            }
            // A preview blocking the main thread: killed, then found like a crash.
            if (System.nanoTime() - progressAt > TimeUnit.SECONDS.toNanos(STALL_SECONDS)) {
                logger.warn("Compose SwiftUI: no progress for $STALL_SECONDS s: stopping the preview host")
                stopHost(udid, work)
                stalled = true
                drain()
                break
            }
            if (System.nanoTime() > deadline) {
                throw GradleException("Compose SwiftUI: rendering timed out after $LAUNCH_TIMEOUT_MINUTES minutes (a preview blocks the main thread?)")
            }
            Thread.sleep(15)
        }
        if (partial.isNotEmpty()) output.appendLine(partial)
        logger.info("Compose SwiftUI: host rendered in ${(System.nanoTime() - started) / 1_000_000} ms")
        return Run(completed, begun, done, errors, output.toString(), stalled)
    }

    /** Kills the rendering host of [udid], if running: much faster than letting simctl terminate it. */
    private fun stopHost(udid: String, work: File) {
        val state = work.resolve("host-$udid.json")
        hostPid(state)?.let { pid -> ProcessHandle.of(pid).ifPresent { it.destroyForcibly(); it.onExit().get(5, TimeUnit.SECONDS) } }
        state.delete()
    }

    private fun hostPid(state: File): Long? =
        runCatching { Regex("""\d+""").find(state.readText())?.value?.toLong() }.getOrNull()

    /** The host writes `"active": false` in its state when it leaves the foreground. */
    private fun hostActive(state: File) = runCatching { "\"active\": false" !in state.readText() }.getOrDefault(true)

    /** Brings the running host back to the foreground: `simctl launch` activates an app that runs. */
    private fun activate(udid: String, work: File, state: File) {
        exec("xcrun", "simctl", "launch", udid, bundleId(work))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!hostActive(state) && System.nanoTime() < deadline) Thread.sleep(20)
    }

    /** One app per module: hosts of several modules (or projects) can share a simulator. */
    private fun bundleId(work: File) = "$BUNDLE_ID.${sha256(listOf(work.canonicalPath.toByteArray()))}"

    // Simulator apps are processes of this Mac.
    private fun isAlive(pid: Long) = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    /** The Kotlin exception (or the end of the output) of a crashed preview. */
    private fun crashMessage(output: String): String {
        val lines = output.lines().filterNot { it.startsWith("CSUI-") }
        val start = lines.indexOfFirst { "Uncaught Kotlin exception" in it || "Fatal error" in it || "Terminating app" in it }
        return (if (start >= 0) lines.drop(start) else lines.takeLast(40)).joinToString("\n").trim()
            .ifEmpty { "The preview crashed the host app" }.take(8000)
    }

    /**
     * Merges [results] into `results.json`: previews rendered before and not this time keep their image, previews
     * gone from the sources are dropped with their images.
     */
    @Suppress("UNCHECKED_CAST")
    private fun writeResults(out: File, manifest: List<ManifestPreview>, rendered: Set<String>, results: List<Map<String, Any?>>) {
        val file = out.resolve(RESULTS_FILE_NAME)
        val previous = previousResults(out)
        val previews = manifest.associateBy { it.id }
        val kept = previous.filter { it["id"] in previews && it["id"] !in rendered }
        val variants = manifest.flatMap { p -> p.variants.map { (it["key"] as String) to Pair(p, it) } }.toMap()
        val fresh = results.map { result ->
            val (preview, variant) = variants[(result["key"] as String).substringBefore('#')] ?: (previews[result["id"]] to null)
            mapOf(
                "key" to result["key"],
                "id" to result["id"],
                "title" to (result["title"] ?: variant?.get("title") ?: result["id"]),
                "group" to (variant?.get("group") ?: ""),
                "source" to preview?.source.orEmpty(),
                "device" to result["device"],
                "file" to result["file"],
                "width" to result["width"],
                "height" to result["height"],
                "milliseconds" to result["milliseconds"],
                "error" to result["error"],
                "crashed" to (result["crashed"] == true),
            )
        }
        val all = kept + fresh
        val images = all.mapNotNull { it["file"] as? String }.toSet()
        out.listFiles { f ->
            (f.extension == "png" && f.name !in images) ||
                (f.name.endsWith(".layout.json") && f.name.removeSuffix(".layout.json") + ".png" !in images)
        }?.forEach { it.delete() }
        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("previews" to all))))
    }

    @Suppress("UNCHECKED_CAST")
    private fun previousResults(out: File): List<Map<String, Any?>> = out.resolve(RESULTS_FILE_NAME).takeIf { it.isFile }
        ?.let { runCatching { (JsonSlurper().parse(it) as Map<String, Any?>)["previews"] as List<Map<String, Any?>> }.getOrNull() }
        .orEmpty()

    /** Opens the preview [key] (`<variant key>` or `<variant key>#<parameter index>`) live on its simulator. */
    private fun openInteractive(manifest: List<ManifestPreview>, key: String, app: File, work: File) {
        val jobs = manifest.flatMap { it.jobs() }
        val base = key.substringBefore('#')
        // A bare qualified name opens the first variant of the preview.
        val job = jobs.firstOrNull { it.key == base } ?: jobs.firstOrNull { it.id == base }
            ?: throw GradleException("Compose SwiftUI: no preview $key")
        val index = key.substringAfter('#', "").toIntOrNull() ?: -1
        val udid = simulator(job.device.ifEmpty { device.orNull })
        install(udid, app, work)
        val request = work.resolve("interactive-$udid.json")
        request.writeText(
            JsonOutput.toJson(
                mapOf(
                    "seq" to 0, "response" to "/dev/null", "output" to work.path, "jobs" to listOf(job.toRequest(index)),
                    "batchSize" to 1, "settleMilliseconds" to 0, "interactive" to true,
                ),
            ),
        )
        // Replaces the rendering host, which the next render launches again.
        stopHost(udid, work)
        exec(
            "xcrun", "simctl", "launch", "--terminate-running-process", udid, bundleId(work),
            environment = mapOf("SIMCTL_CHILD_COMPOSE_SWIFTUI_PREVIEW_REQUEST" to request.path),
        )
        exec("open", "-a", "Simulator", "--args", "-CurrentDeviceUDID", udid)
        logger.lifecycle("Compose SwiftUI: ${job.variant["title"]} is running on the simulator")
    }

    private fun buildHostApp(work: File): File {
        val src = work.resolve("src")
        val framework = frameworkDirectory.get().asFile
        val app = work.resolve("$HOST_NAME.app")
        val sdk = exec("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path").trim()
        val arch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "x86_64"
        val common = arrayOf("-target", "$arch-apple-ios17.0-simulator", "-sdk", sdk, "-F", framework.parentFile.path)

        // The host's Swift code only uses the renderer, the runtime classes and `ComposeSwiftUIPreviews.host`, whose
        // ObjC names only change with the library version: compile it once, not on every change to the app's API.
        val sources = buildList {
            RendererResources.files().forEach { add(it to RendererResources.read(it)) }
            add("KotlinModule.swift" to "@_exported import ${frameworkName.get()}\n".toByteArray())
            RendererResources.previewHost().forEach { (name, text) -> add(name to text.toByteArray()) }
        }
        val key = sha256(sources.map { it.second } + listOf(sdk, frameworkName.get(), rendererVersion, arch).map { it.toByteArray() } + exportedNames(framework, sources))
        val objectFile = work.resolve("host-$key.o")
        if (!objectFile.exists()) {
            src.deleteRecursively()
            sources.forEach { (rel, bytes) -> src.resolve(rel).apply { parentFile.mkdirs() }.writeBytes(bytes) }
            work.listFiles { f -> f.name.startsWith("host-") }?.forEach { it.delete() }
            logger.lifecycle("Compose SwiftUI: compiling the preview host (once per library version)")
            exec(
                "xcrun", "swiftc", "-c", "-wmo", "-parse-as-library", "-swift-version", "5", "-Onone", *common,
                "-module-name", HOST_NAME, "-o", objectFile.path,
                *src.walkTopDown().filter { it.extension == "swift" }.sorted().map { it.path }.toList().toTypedArray(),
                timeoutMinutes = 15,
            )
        }

        // A static framework is part of the executable: relink when it changes. A dynamic one is loaded at run time:
        // the app is linked once, and only the copy of the framework in the bundle is replaced.
        val binary = framework.resolve(frameworkName.get())
        val static = staticFramework.get()
        val linkKey = sha256(
            listOf(key, if (static) binary.stamp() else "", linkerOpts.get().joinToString(" "), static.toString()).map { it.toByteArray() },
        )
        val resources = composeResources.files.filter { it.isDirectory }
        val bundleId = bundleId(work)
        // For the IDE, which activates the host itself.
        work.resolve(BUNDLE_ID_FILE_NAME).writeText(bundleId)
        val bundleKey = sha256(
            listOf(linkKey, binary.stamp(), bundleId).map { it.toByteArray() } +
                resources.flatMap { dir -> dir.walkTopDown().filter { it.isFile }.map { it.stamp().toByteArray() } },
        )
        val linkStamp = work.resolve("link.stamp")
        val bundleStamp = work.resolve("app.stamp")
        val executable = app.resolve(HOST_NAME)
        if (executable.exists() && bundleStamp.exists() && bundleStamp.readText() == bundleKey) return app

        if (!executable.exists() || !linkStamp.exists() || linkStamp.readText() != linkKey) {
            app.deleteRecursively()
            app.mkdirs()
            try {
                exec(
                    "xcrun", "swiftc", *common, objectFile.path, "-framework", frameworkName.get(), "-lc++",
                    // system libraries that common KMP libraries link through the app (SQLDelight: sqlite3)
                    "-lsqlite3", "-lz",
                    "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks", "-o", executable.path,
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
            linkStamp.writeText(linkKey)
        }
        app.resolve("compose-resources").deleteRecursively()
        resources.forEach { it.copyRecursively(app.resolve("compose-resources"), overwrite = true) }
        if (!static) {
            app.resolve("Frameworks").deleteRecursively()
            framework.copyRecursively(app.resolve("Frameworks/${framework.name}"))
        }
        app.resolve("Info.plist").writeText(infoPlist(bundleId))
        exec("codesign", "--force", "--deep", "--sign", "-", app.path)
        bundleStamp.writeText(bundleKey)
        return app
    }

    /**
     * ObjC names of the framework that the host's Swift code mentions: a change (e.g. an app class taking the name of
     * a runtime class) recompiles the host. Other API changes of the app don't.
     */
    private fun exportedNames(framework: File, sources: List<Pair<String, ByteArray>>): List<ByteArray> {
        val identifiers = sources.flatMapTo(HashSet()) { Regex("""\b[A-Z]\w+""").findAll(String(it.second)).map { m -> m.value } }
        val swiftName = Regex("""swift_name\("(\w+)"\)\)\)\s*\n?\s*@(?:interface|protocol)\s+(\w+)""")
        return framework.resolve("Headers").walkTopDown().filter { it.isFile }.sorted()
            .flatMap { header -> swiftName.findAll(header.readText()).map { it.groupValues[1] + "=" + it.groupValues[2] } }
            .filter { it.substringBefore('=') in identifiers }
            .sorted()
            .map { it.toByteArray() }
            .toList()
    }

    private fun File.stamp() = "$path:${length()}:${lastModified()}"

    /**
     * Installs the app unless the same build is already installed on [udid]. Once installed, a new build replaces the
     * files of the installed app in place: much faster than `simctl install`, which copies and registers the whole app.
     */
    private fun install(udid: String, app: File, work: File) {
        val stamp = work.resolve("installed-$udid.stamp")
        val build = work.resolve("app.stamp").readText()
        // Where the app is installed, remembered: asking simctl takes half a second. Gone after an erase or uninstall.
        val known = work.resolve("container-$udid.txt")
        val container = runCatching { File(known.readText()) }.getOrNull()?.takeIf { it.resolve(HOST_NAME).isFile }
            ?: runCatching { File(exec("xcrun", "simctl", "get_app_container", udid, bundleId(work)).trim()) }
                .getOrNull()?.takeIf { it.isDirectory }
        if (container != null && stamp.exists() && stamp.readText() == build) return
        stopHost(udid, work)
        if (container != null) {
            exec("rsync", "-a", "--delete", "${app.path}/", "${container.path}/")
            known.writeText(container.path)
        } else {
            exec("xcrun", "simctl", "install", udid, app.path)
            known.delete()
        }
        stamp.writeText(build)
    }

    /**
     * UDID of the requested simulator, booted if needed. By default, a simulator of its own (`Compose SwiftUI
     * Previews`, like the booted or newest iPhone), booted in the background: the host app must be in the foreground of
     * its device to render, and it would take over the simulator the app is run on.
     */
    @Suppress("UNCHECKED_CAST")
    private fun simulator(wanted: String?): String {
        val json = JsonSlurper().parseText(exec("xcrun", "simctl", "list", "devices", "available", "-j")) as Map<String, Any?>
        // runtime identifier, name, udid, state, device type
        val devices = (json["devices"] as Map<String, List<Map<String, Any?>>>).flatMap { (runtime, list) ->
            list.map { listOf(runtime, it["name"] as String, it["udid"] as String, it["state"] as String, it["deviceTypeIdentifier"] as? String ?: "") }
        }
        val device = if (wanted != null) {
            // Several runtimes can have a device of that name: prefer a booted one, else the newest runtime.
            devices.filter { it[1] == wanted || it[2] == wanted }.let { matches -> matches.firstOrNull { it[3] == "Booted" } ?: matches.lastOrNull() }
                ?: throw GradleException("Compose SwiftUI: no iOS simulator named $wanted")
        } else {
            devices.firstOrNull { it[1] == PREVIEW_DEVICE_NAME } ?: run {
                val model = devices.firstOrNull { it[3] == "Booted" && it[1].startsWith("iPhone") }
                    ?: devices.lastOrNull { it[1].startsWith("iPhone") }
                    ?: throw GradleException("Compose SwiftUI: no iOS simulator found: install an iOS runtime in Xcode › Settings › Components")
                logger.lifecycle("Compose SwiftUI: creating the simulator \"$PREVIEW_DEVICE_NAME\" (${model[1]})")
                val udid = exec("xcrun", "simctl", "create", PREVIEW_DEVICE_NAME, model[4], model[0]).trim()
                listOf(model[0], PREVIEW_DEVICE_NAME, udid, "Shutdown", model[4])
            }
        }
        val (_, name, udid, state) = device
        if (state != "Booted") {
            // Booted without a window: rendering happens in the background.
            logger.lifecycle("Compose SwiftUI: booting $name")
            exec("xcrun", "simctl", "bootstatus", udid, "-b", timeoutMinutes = 5)
        }
        return udid
    }

    private fun <T> timed(step: String, block: () -> T): T {
        val start = System.nanoTime()
        return block().also { logger.info("Compose SwiftUI: $step took ${(System.nanoTime() - start) / 1_000_000} ms") }
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

    internal companion object {
        /** Read by the IntelliJ plugin. */
        const val RESULTS_FILE_NAME = "results.json"

        /** Simulator of each device name of the last render (`""`: the default one), read by the IntelliJ plugin. */
        const val DEVICES_FILE_NAME = "devices.json"

        /** `{"previews": [qualified names], "interactive": key}`, written by the IntelliJ plugin before each render. */
        const val IDE_REQUEST_FILE_NAME = "ide-request.json"
        /** Bundle ID of the module's host app, read by the IntelliJ plugin. */
        const val BUNDLE_ID_FILE_NAME = "bundle-id"
        const val HOST_NAME = "ComposeSwiftUIPreviewHost"
        const val BUNDLE_ID = "dev.nucleusframework.composeswiftui.previewhost"

        /** The simulator the previews render on by default, created when missing. */
        const val PREVIEW_DEVICE_NAME = "Compose SwiftUI Previews"

        /** Previews composed at once, each in its own window. */
        const val BATCH_SIZE = 8
        const val LAUNCH_TIMEOUT_MINUTES = 5L

        /** Longest time without a rendered preview before the host is considered blocked. */
        const val STALL_SECONDS = 30L
        fun infoPlist(bundleId: String) = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>CFBundleIdentifier</key><string>$bundleId</string>
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
