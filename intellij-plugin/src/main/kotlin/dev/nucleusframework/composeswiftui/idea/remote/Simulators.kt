package dev.nucleusframework.composeswiftui.idea.remote

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.logger
import java.net.ServerSocket
import java.security.MessageDigest

/** An iOS simulator of the Mac. */
data class Simulator(val udid: String, val name: String, val runtime: String, val state: String) {
    val booted: Boolean get() = state == "Booted"

    override fun toString() = "$name ($runtime)" + if (booted) " — booted" else ""
}

/** The simulators of the Mac behind [shell], with `simctl`. */
class Simulators(private val shell: MacShell) {
    fun list(): List<Simulator> {
        val result = shell.run("xcrun simctl list devices available -j", timeoutSeconds = 60)
        if (!result.success) error(result.output.trim())
        val devices = JsonParser.parseString(result.output.substring(result.output.indexOf('{'))).asJsonObject.getAsJsonObject("devices")
        return devices.entrySet().flatMap { (runtime, list) ->
            // com.apple.CoreSimulator.SimRuntime.iOS-27-2 (or iOS-27-2-iPhone19-4, a device's own) → iOS 27.2
            val label = Regex("""(\w+?)-(\d+)-(\d+)""").find(runtime.substringAfterLast('.'))
                ?.destructured?.let { (os, major, minor) -> "$os $major.$minor" } ?: runtime.substringAfterLast('.')
            list.asJsonArray.map { it.asJsonObject }.map {
                Simulator(it.get("udid").asString, it.get("name").asString, label, it.get("state").asString)
            }
        }.filter { it.runtime.startsWith("iOS") || it.runtime.startsWith("iPadOS") }
            // Booted first, the previews' own simulator after the user's.
            .sortedWith(
                compareByDescending<Simulator> { it.booted }
                    .thenBy { it.name == PREVIEWS_SIMULATOR }
                    .thenByDescending { it.runtime }
                    .thenBy { it.name },
            )
    }

    fun boot(udid: String): ShellResult =
        shell.run("xcrun simctl boot ${quote(udid)} 2>/dev/null; xcrun simctl bootstatus ${quote(udid)} -b", timeoutSeconds = 300)

    fun shutdown(udid: String): ShellResult = shell.run("xcrun simctl shutdown ${quote(udid)}", timeoutSeconds = 120)

    /** Turns the device a quarter turn ([left] or right); the app turns if it supports the orientation. */
    fun rotate(udid: String, left: Boolean): ShellResult =
        shell.run("xcrun devicectl device orientation rotate --device ${quote(udid)} ${if (left) "left" else "right"}", timeoutSeconds = 30)

    /** Shakes the device, as the Simulator app does (`UIEventSubtypeMotionShake` in the foreground app). */
    fun shake(udid: String): ShellResult =
        shell.run("xcrun simctl spawn ${quote(udid)} notifyutil -p com.apple.UIKit.SimulatorShake", timeoutSeconds = 30)

    /** Whether the device is in dark mode. */
    fun isDark(udid: String): Boolean = shell.run("xcrun simctl ui ${quote(udid)} appearance", timeoutSeconds = 30).output.trim() == "dark"

    fun setDark(udid: String, dark: Boolean): ShellResult =
        shell.run("xcrun simctl ui ${quote(udid)} appearance ${if (dark) "dark" else "light"}", timeoutSeconds = 30)

    /** A PNG of the screen at full resolution: of the [display] (screen ID) on a device with several (iPhone Duo). */
    fun screenshot(udid: String, display: Int? = null): ByteArray {
        val option = display?.let { "--display=$it " }.orEmpty()
        val result = shell.run(
            "d=\$(mktemp -d) && xcrun simctl io ${quote(udid)} screenshot $option--type=png \"\$d/s.png\" >/dev/null 2>&1 && base64 < \"\$d/s.png\"; rm -rf \"\$d\"",
            timeoutSeconds = 60,
        )
        val png = java.util.Base64.getMimeDecoder().decode(result.output.trim())
        if (png.size < 8) error("Couldn't take the screenshot")
        return png
    }

    private companion object {
        /** The simulator the previews render on by default (see the Gradle plugin). */
        const val PREVIEWS_SIMULATOR = "Compose SwiftUI Previews"
    }
}

/**
 * The screen of a simulator streamed by `simstream` (see `simstream.m`), compiled on the Mac on first use. The
 * stream listens on the Mac's loopback; on a remote Mac its port is forwarded here.
 */
class SimulatorStream private constructor(val port: Int, private val process: Process, private val forward: AutoCloseable) : AutoCloseable {
    val isAlive: Boolean get() = process.isAlive


    override fun close() {
        forward.close()
        // Closing its input stops the streamer (see [start]).
        runCatching { process.outputStream.close() }
        process.destroy()
    }

    companion object {
        private val LOG = logger<SimulatorStream>()
        private val source: String by lazy {
            SimulatorStream::class.java.getResource("/dev/nucleusframework/composeswiftui/remote/simstream.m")!!.readText()
        }

        /** Compiles the streamer on the Mac when this version isn't there yet; returns its path. */
        fun install(shell: MacShell): String {
            val hash = MessageDigest.getInstance("SHA-256").digest(source.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
            val dir = "${shell.home}/.compose-swiftui/bin"
            val binary = "$dir/simstream-$hash"
            val command = """
                [[ -x ${quote(binary)} ]] && exit 0
                mkdir -p ${quote(dir)} && cat > ${quote("$binary.m")} &&
                clang -fobjc-arc -O2 -Wno-deprecated-declarations -framework Foundation -framework AppKit -framework IOSurface \
                  -framework CoreImage -framework ImageIO ${quote("$binary.m")} -o ${quote(binary)}
            """.trimIndent()
            val result = shell.run(command, source.toByteArray(), timeoutSeconds = 180)
            if (!result.success) error("Couldn't build the simulator streamer on the Mac:\n${result.output}")
            return binary
        }

        /** Starts streaming the booted simulator [udid]. */
        fun start(shell: MacShell, udid: String): SimulatorStream {
            val binary = install(shell)
            val port = ServerSocket(0).use { it.localPort }
            // Runs while this side keeps its input open: an SSH session that ends takes the streamer with it.
            val process = shell.start("${quote(binary)} ${quote(udid)} $port 0.75 & pid=\$!; cat >/dev/null; kill \$pid 2>/dev/null")
            // Wait until it listens: through an SSH forward, connecting earlier succeeds here and is closed at once.
            val reader = process.inputStream.bufferedReader()
            val printed = StringBuilder()
            val listening = java.util.concurrent.CompletableFuture<Boolean>()
            Thread {
                runCatching {
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (!listening.isDone) printed.appendLine(line)
                        if ("listening on" in line) listening.complete(true)
                    }
                }
                listening.complete(false)
            }.apply { isDaemon = true }.start()
            if (!runCatching { listening.get(60, java.util.concurrent.TimeUnit.SECONDS) }.getOrDefault(false)) {
                runCatching { process.outputStream.close() }
                process.destroy()
                error("The simulator stream didn't start: ${printed.toString().trim()}")
            }
            val forward = shell.forward(port)
            LOG.info("Streaming $udid on port $port")
            return SimulatorStream(port, process, forward)
        }
    }
}
