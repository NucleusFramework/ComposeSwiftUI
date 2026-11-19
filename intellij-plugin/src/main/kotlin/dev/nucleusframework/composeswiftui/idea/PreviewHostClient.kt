package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.RemoteGradle
import dev.nucleusframework.composeswiftui.idea.remote.RemoteWorkspace
import dev.nucleusframework.composeswiftui.idea.remote.quote
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/** A literal edited in the code since the last build: [old] reads [new] at [offset] of [file] (a file name). */
data class LiteralEdit(val file: String, val offset: Int, val function: String?, val kind: String, val old: String, val new: String) {
    fun toJson() = JsonObject().apply {
        addProperty("file", file)
        addProperty("offset", offset)
        addProperty("function", function)
        addProperty("kind", kind)
        addProperty("old", old)
        addProperty("new", new)
    }
}

/**
 * Talks to the preview host app that `renderComposeSwiftUIPreviews` leaves running on the simulator or the Mac, without Gradle:
 * renders with literal edits (live literals) and starts live previews (interactive mode). Same request protocol as
 * the task (see `PreviewHost.swift`).
 */
class PreviewHostClient(private val module: PreviewModule) {
    private val work = File(module.moduleDir, "build/composeSwiftUI/previewHost")
    private val access: HostAccess = MacHosts.workspace(module.rootProjectPath)?.let(::RemoteHostAccess) ?: LocalHostAccess

    /**
     * UDID of the host for the `@Preview` [device] (`""`: the default one), or [MAC_HOST], if running and built from the
     * last build.
     */
    fun runningHost(device: String = ""): String? {
        val devices = json(access.read(File(work, "devices.json"))) ?: return null
        val udid = (devices.get(device) ?: devices.get("") ?: devices.entrySet().firstOrNull()?.value)?.asString ?: return null
        val state = File(work, "host-$udid.json")
        val pid = hostPid(udid) ?: return null
        val build = access.read(File(work, "host-$udid.build"))
        // The Mac host is built in its own directory; the simulator one at the root.
        val current = access.read(File(if (udid == MAC_HOST) File(work, "mac") else work, "app.stamp"))
        if (build == null || build != current || !access.isAlive(pid)) return null
        // Only the foreground app of a simulator renders: the host of another module may have taken its place.
        if (!isActive(state)) activate(udid, state)
        return udid
    }

    private fun isActive(state: File) = access.read(state)?.let { "\"active\": false" !in it } ?: true

    private fun activate(udid: String, state: File) {
        // The Mac host renders in the background.
        if (udid == MAC_HOST) return
        val bundleId = access.read(File(work, "bundle-id"))?.trim() ?: return
        // `simctl launch` brings an app that runs to the foreground.
        access.launch(udid, bundleId)
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!isActive(state) && System.nanoTime() < deadline) Thread.sleep(20)
    }

    private fun hostPid(udid: String) =
        access.read(File(work, "host-$udid.json"))?.let { Regex("""\d+""").find(it)?.value?.toLongOrNull() }

    /** Request entries of the previews [ids], from the manifest of the last Gradle run, by device. */
    fun jobs(ids: Collection<String>): Map<String, List<JsonObject>> {
        val manifest = json(module.manifestFile)?.getAsJsonArray("previews") ?: return emptyMap()
        return manifest.map { it.asJsonObject }
            .filter { it.get("id").asString in ids }
            .flatMap { preview ->
                preview.getAsJsonArray("variants").map { variant ->
                    variant.asJsonObject.deepCopy().apply {
                        addProperty("id", preview.get("id").asString)
                        addProperty("parameterized", preview.get("parameterized").asBoolean)
                        addProperty("index", -1)
                    }
                }
            }
            .groupBy { it.get("device")?.asString.orEmpty() }
    }

    /**
     * Renders the previews [ids] with [edits] into the previews folder and waits until done. False when no host of the
     * last build runs (a Gradle render is needed).
     */
    fun render(ids: Collection<String>, edits: List<LiteralEdit>, settleMilliseconds: Int): Boolean {
        val byDevice = jobs(ids)
        if (byDevice.isEmpty()) return false
        val hosts = byDevice.keys.associateWith { runningHost(it) ?: return false }
        val results = ArrayList<JsonObject>()
        for ((device, jobs) in byDevice) {
            val output = request(hosts.getValue(device), jobs, edits, settleMilliseconds, interactive = false, port = null) ?: return false
            results += parseResults(output, jobs, device)
        }
        access.pullPreviews(module)
        mergeResults(ids.toSet(), results)
        return true
    }

    /** The rendered previews (`CSUI-DONE`) and failures (`CSUI-ERROR`) of a host response, as `results.json` entries. */
    private fun parseResults(output: String, jobs: List<JsonObject>, device: String): List<JsonObject> {
        val variants = jobs.associateBy { it.get("key").asString }
        return output.lines().mapNotNull { line ->
            when {
                line.startsWith("CSUI-DONE ") -> runCatching { JsonParser.parseString(line.removePrefix("CSUI-DONE ")).asJsonObject }.getOrNull()
                line.startsWith("CSUI-ERROR ") -> line.removePrefix("CSUI-ERROR ").split(' ', limit = 2).let {
                    JsonObject().apply {
                        addProperty("key", it[0])
                        addProperty("id", it[0].substringBefore('~').substringBefore('#'))
                        addProperty("error", it.getOrElse(1) { "" }.replace("\\n", "\n"))
                    }
                }
                else -> null
            }
        }.map { result ->
            val variant = variants[result.get("key").asString.substringBefore('#')]
            result.apply {
                if (!has("title")) addProperty("title", variant?.get("title")?.asString ?: get("id").asString)
                addProperty("group", variant?.get("group")?.asString.orEmpty())
                addProperty("device", device)
            }
        }
    }

    /** Replaces the results of the previews [ids] in `results.json`, like the Gradle task does. */
    private fun mergeResults(ids: Set<String>, fresh: List<JsonObject>) {
        val file = File(module.previewsDir, "results.json")
        val previous = json(file)?.getAsJsonArray("previews")?.map { it.asJsonObject }.orEmpty()
        val all = JsonArray()
        previous.filter { it.get("id")?.asString !in ids }.forEach(all::add)
        fresh.forEach(all::add)
        file.writeText(JsonObject().apply { add("previews", all) }.toString())
        access.pushed(file)
    }

    /** Starts the live preview [key] (`<variant key>[#<parameter index>]`) streaming on [port]. */
    fun startInteractive(key: String, port: Int, edits: List<LiteralEdit>): Boolean {
        val base = key.substringBefore('#')
        val id = base.substringBefore('~')
        val (device, job) = jobs(listOf(id)).entries
            .firstNotNullOfOrNull { (device, jobs) -> jobs.firstOrNull { it.get("key").asString == base }?.let { device to it } }
            // A bare qualified name (from the gutter): the first variant.
            ?: jobs(listOf(id)).entries.firstOrNull()?.let { (device, jobs) -> device to jobs.first() }
            ?: return false
        key.substringAfter('#', "").toIntOrNull()?.let { job.addProperty("index", it) }
        val udid = runningHost(device) ?: return false
        return request(udid, listOf(job), edits, 0, interactive = true, port = port) != null
    }

    private fun request(
        udid: String,
        jobs: List<JsonObject>,
        edits: List<LiteralEdit>,
        settleMilliseconds: Int,
        interactive: Boolean,
        port: Int?,
    ): String? {
        return HostLock.withLock(File(work, "request-$udid.lock")) { send(udid, jobs, edits, settleMilliseconds, interactive, port) }
    }

    private fun send(
        udid: String,
        jobs: List<JsonObject>,
        edits: List<LiteralEdit>,
        settleMilliseconds: Int,
        interactive: Boolean,
        port: Int?,
    ): String? {
        val response = File(work, "response-ide-$udid.log")
        val body = JsonObject().apply {
            addProperty("seq", System.currentTimeMillis() * 1000 + System.nanoTime() / 1000 % 1000)
            addProperty("response", access.path(response))
            addProperty("output", access.path(module.previewsDir))
            add("jobs", JsonArray().apply { jobs.forEach(::add) })
            addProperty("batchSize", 8)
            addProperty("settleMilliseconds", settleMilliseconds)
            addProperty("interactive", interactive)
            port?.let { addProperty("port", it) }
            add("patches", JsonArray().apply { edits.forEach { add(it.toJson()) } })
        }
        val pid = hostPid(udid) ?: return null
        return access.exchange(work, udid, body.toString(), response, pid)
    }

    private fun json(file: File): JsonObject? =
        if (file.isFile) json(file.readText()) else null

    private fun json(text: String?): JsonObject? =
        text?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
}

/** How the IDE reaches the preview host's files and process: on this Mac, or on the remote one. */
private interface HostAccess {
    /** Text of the host's [file] (a local path), null when missing. */
    fun read(file: File): String?
    fun isAlive(pid: Long): Boolean

    /** Brings the host app of [bundleId] to the foreground of the simulator [udid]. */
    fun launch(udid: String, bundleId: String)

    /** The path of [file] where the host runs. */
    fun path(file: File): String

    /**
     * Sends the request [body] to the host of [udid] in [work] (written in one move: the host polls the file and must
     * never read half of it), and waits for the end of its [response] while the host [pid] lives.
     */
    fun exchange(work: File, udid: String, body: String, response: File, pid: Long): String?

    /** After a render: makes the images the host wrote readable here. */
    fun pullPreviews(module: PreviewModule) {}

    /** After the IDE changed [file]: makes the host's copy the same. */
    fun pushed(file: File) {}
}

private object LocalHostAccess : HostAccess {
    override fun read(file: File) = runCatching { file.readText() }.getOrNull()

    override fun isAlive(pid: Long) = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    override fun launch(udid: String, bundleId: String) {
        runCatching { ProcessBuilder("xcrun", "simctl", "launch", udid, bundleId).redirectErrorStream(true).start().waitFor(10, TimeUnit.SECONDS) }
    }

    override fun path(file: File): String = file.path

    override fun exchange(work: File, udid: String, body: String, response: File, pid: Long): String? {
        response.delete()
        val tmp = File(work, "request-$udid.json.ide")
        tmp.writeText(body)
        Files.move(tmp.toPath(), File(work, "request-$udid.json").toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        val deadline = System.nanoTime() + 30_000_000_000L
        while (System.nanoTime() < deadline) {
            val text = if (response.isFile) response.readText() else ""
            if ("CSUI-END" in text) return text
            // A literal edit crashed the preview: a build renders it again, isolating the culprit.
            if (!isAlive(pid)) return null
            Thread.sleep(15)
        }
        return null
    }
}

/** The host on a remote Mac: each operation is one command over the shared SSH connection. */
private class RemoteHostAccess(private val workspace: RemoteWorkspace) : HostAccess {
    override fun read(file: File) = workspace.read(file)

    override fun isAlive(pid: Long) = workspace.shell.run("kill -0 $pid", timeoutSeconds = 30).success

    override fun launch(udid: String, bundleId: String) {
        workspace.shell.run("xcrun simctl launch ${quote(udid)} ${quote(bundleId)}", timeoutSeconds = 30)
    }

    override fun path(file: File) = workspace.toRemote(file)

    override fun exchange(work: File, udid: String, body: String, response: File, pid: Long): String? {
        val dir = quote(workspace.toRemote(work))
        val responsePath = quote(workspace.toRemote(response))
        // The same as the local exchange, run on the Mac: one round trip instead of one per poll.
        val script = """
            rm -f $responsePath
            cat > $dir/request-$udid.json.ide && mv -f $dir/request-$udid.json.ide $dir/request-$udid.json
            for i in {1..2000}; do
              if [[ -f $responsePath ]] && grep -q CSUI-END $responsePath; then cat $responsePath; exit 0; fi
              kill -0 $pid 2>/dev/null || exit 3
              sleep 0.015
            done
            exit 4
        """.trimIndent()
        val result = workspace.shell.run(script, body.toByteArray(), timeoutSeconds = 40)
        return if (result.success) workspace.toLocalText(result.output) else null
    }

    override fun pullPreviews(module: PreviewModule) {
        RemoteGradle.pullOutputs(workspace, module)
    }

    override fun pushed(file: File) {
        workspace.write(file, file.readBytes())
    }
}

/**
 * One request at a time per host, across the IDE and the Gradle task (same lock file): the host only reads the latest
 * request, an earlier one sent meanwhile would never be answered.
 */
internal object HostLock {
    private val local = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock>()

    fun <T> withLock(file: File, block: () -> T): T {
        // File locks are per process: threads of the IDE wait on a lock of their own first.
        val lock = local.getOrPut(file.path) { java.util.concurrent.locks.ReentrantLock() }
        lock.lock()
        try {
            file.parentFile.mkdirs()
            java.io.RandomAccessFile(file, "rw").channel.use { channel ->
                channel.lock().use { return block() }
            }
        } finally {
            lock.unlock()
        }
    }
}

/**
 * The text of each file as last built, kept next to the host so live literals still work after the IDE restarts: it
 * is only valid while the host app is that build (same `app.stamp`).
 */
object BuiltSources {
    private fun dir(module: PreviewModule) = File(module.moduleDir, "build/composeSwiftUI/previewHost/ide-built")
    private fun stamp(module: PreviewModule) =
        runCatching { File(module.moduleDir, "build/composeSwiftUI/previewHost/app.stamp").readText() }.getOrNull()

    private fun entry(module: PreviewModule, path: String) =
        File(dir(module), Integer.toHexString(path.hashCode()) + ".json")

    fun save(module: PreviewModule, path: String, text: String) {
        val stamp = stamp(module) ?: return
        dir(module).mkdirs()
        entry(module, path).writeText(JsonObject().apply {
            addProperty("stamp", stamp)
            addProperty("path", path)
            addProperty("text", text)
        }.toString())
    }

    fun load(module: PreviewModule, path: String): String? {
        val file = entry(module, path).takeIf { it.isFile } ?: return null
        val json = runCatching { JsonParser.parseString(file.readText()).asJsonObject }.getOrNull() ?: return null
        if (json.get("path")?.asString != path || json.get("stamp")?.asString != stamp(module)) return null
        return json.get("text")?.asString
    }
}

/** The Mac's host, in place of a simulator UDID in `devices.json` and the host files (see the Gradle task). */
const val MAC_HOST = "mac"
