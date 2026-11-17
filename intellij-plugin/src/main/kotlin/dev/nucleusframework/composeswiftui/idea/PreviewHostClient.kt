package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
 * Talks to the preview host app that `renderComposeSwiftUIPreviews` leaves running on the simulator, without Gradle:
 * renders with literal edits (live literals) and starts live previews (interactive mode). Same request protocol as
 * the task (see `PreviewHost.swift`).
 */
class PreviewHostClient(private val module: PreviewModule) {
    private val work = File(module.moduleDir, "build/composeSwiftUI/previewHost")

    /** UDID of the host for the `@Preview` [device] (`""`: the default one), if running and built from the last build. */
    fun runningHost(device: String = ""): String? {
        val devices = json(File(work, "devices.json")) ?: return null
        val udid = (devices.get(device) ?: devices.get("") ?: devices.entrySet().firstOrNull()?.value)?.asString ?: return null
        val state = File(work, "host-$udid.json")
        val pid = hostPid(udid) ?: return null
        val build = runCatching { File(work, "host-$udid.build").readText() }.getOrNull()
        val current = runCatching { File(work, "app.stamp").readText() }.getOrNull()
        if (build == null || build != current || !isAlive(pid)) return null
        // Only the foreground app of a simulator renders: the host of another module may have taken its place.
        if (!isActive(state)) activate(udid, state)
        return udid
    }

    private fun isActive(state: File) = runCatching { "\"active\": false" !in state.readText() }.getOrDefault(true)

    private fun activate(udid: String, state: File) {
        val bundleId = runCatching { File(work, "bundle-id").readText().trim() }.getOrNull() ?: return
        // `simctl launch` brings an app that runs to the foreground.
        runCatching { ProcessBuilder("xcrun", "simctl", "launch", udid, bundleId).redirectErrorStream(true).start().waitFor(10, TimeUnit.SECONDS) }
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!isActive(state) && System.nanoTime() < deadline) Thread.sleep(20)
    }

    private fun isAlive(pid: Long) = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    private fun hostPid(udid: String) =
        runCatching { Regex("""\d+""").find(File(work, "host-$udid.json").readText())?.value?.toLong() }.getOrNull()

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
        val response = File(work, "response-ide-$udid.log").apply { delete() }
        val body = JsonObject().apply {
            addProperty("seq", System.currentTimeMillis() * 1000 + System.nanoTime() / 1000 % 1000)
            addProperty("response", response.path)
            addProperty("output", module.previewsDir.path)
            add("jobs", JsonArray().apply { jobs.forEach(::add) })
            addProperty("batchSize", 8)
            addProperty("settleMilliseconds", settleMilliseconds)
            addProperty("interactive", interactive)
            port?.let { addProperty("port", it) }
            add("patches", JsonArray().apply { edits.forEach { add(it.toJson()) } })
        }
        // Written in one move: the host polls the file and must never read half of it.
        val tmp = File(work, "request-$udid.json.ide")
        tmp.writeText(body.toString())
        Files.move(tmp.toPath(), File(work, "request-$udid.json").toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        val pid = hostPid(udid) ?: return null
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

    private fun json(file: File): JsonObject? =
        if (file.isFile) runCatching { JsonParser.parseString(file.readText()).asJsonObject }.getOrNull() else null
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
