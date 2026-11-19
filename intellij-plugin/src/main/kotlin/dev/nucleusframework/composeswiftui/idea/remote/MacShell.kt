package dev.nucleusframework.composeswiftui.idea.remote

import java.io.File
import java.util.concurrent.TimeUnit

/** Exit code and output (stdout and stderr together) of a command. */
data class ShellResult(val exitCode: Int, val output: String) {
    val success: Boolean get() = exitCode == 0
}

/**
 * Where the Apple tools run: this Mac ([LocalMacShell]), or a Mac reached over SSH from Linux or Windows
 * ([SshMacShell]). Commands are zsh command lines run in a login shell, so they find what the user's profile puts on
 * the `PATH` (Homebrew, a JDK).
 */
interface MacShell {
    val isRemote: Boolean

    /** Runs [command] and waits for it, feeding [input] to its standard input. */
    fun run(command: String, input: ByteArray? = null, timeoutSeconds: Long = 120): ShellResult

    /** Starts [command] without waiting: its output is the process' input stream. */
    fun start(command: String): Process

    /** Makes the Mac's `127.0.0.1:[port]` reachable at `127.0.0.1:[port]` here; closing undoes it. */
    fun forward(port: Int): AutoCloseable

    /** The user's home directory on the Mac. */
    val home: String
}

/** The Apple tools of this Mac. */
object LocalMacShell : MacShell {
    override val isRemote = false

    override fun run(command: String, input: ByteArray?, timeoutSeconds: Long): ShellResult =
        start(command).waitFor(input, timeoutSeconds)

    override fun start(command: String): Process =
        ProcessBuilder("/bin/zsh", "-lc", command).redirectErrorStream(true).start()

    override fun forward(port: Int) = AutoCloseable {}

    override val home: String = System.getProperty("user.home")
}

/**
 * The Apple tools of a Mac reached with `ssh [destination]` (an alias of `~/.ssh/config` or `user@host`, with key
 * authentication). All commands share one connection (OpenSSH connection sharing), so each costs a round trip, not a
 * handshake, and port forwards are added to that same connection.
 */
class SshMacShell(val destination: String) : MacShell {
    override val isRemote = true

    private val controlPath = File(System.getProperty("java.io.tmpdir"), "csui-ssh-%C").path

    /** Options of every ssh command: connection sharing, no prompt (a missing key fails instead of hanging). */
    val sshOptions: List<String> = listOf(
        "-o", "ControlMaster=auto",
        "-o", "ControlPath=$controlPath",
        "-o", "ControlPersist=600",
        "-o", "BatchMode=yes",
        "-o", "ConnectTimeout=10",
        "-o", "ServerAliveInterval=15",
    )

    /** The `ssh` command line rsync uses, with the same options. */
    val rsyncShell: String get() = (listOf("ssh") + sshOptions).joinToString(" ")

    override fun run(command: String, input: ByteArray?, timeoutSeconds: Long): ShellResult =
        start(command).waitFor(input, timeoutSeconds)

    override fun start(command: String): Process =
        ProcessBuilder(listOf("ssh") + sshOptions + listOf(destination, "/bin/zsh -lc ${quote(command)}"))
            .redirectErrorStream(true)
            .start()

    override fun forward(port: Int): AutoCloseable {
        val spec = "$port:127.0.0.1:$port"
        // The master connection must exist before it can carry a forward.
        run("true", timeoutSeconds = 30)
        control("forward", spec)
        return AutoCloseable { control("cancel", spec) }
    }

    private fun control(operation: String, spec: String) {
        ProcessBuilder(listOf("ssh") + sshOptions + listOf("-O", operation, "-L", spec, destination))
            .redirectErrorStream(true)
            .start()
            .waitFor(null, 30)
    }

    override val home: String by lazy { run("print -r -- \$HOME").output.trim() }

    /** Checks that the Mac answers, with its macOS and Xcode versions, or why it doesn't. */
    fun check(): ShellResult = run("sw_vers -productVersion && xcodebuild -version | head -1", timeoutSeconds = 30)
}

private fun Process.waitFor(input: ByteArray?, timeoutSeconds: Long): ShellResult {
    // Read while the process runs: a full pipe would block it.
    val output = StringBuilder()
    val reader = Thread { output.append(inputStream.bufferedReader().readText()) }.apply { start() }
    outputStream.use { stream -> input?.let(stream::write) }
    if (!waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        destroyForcibly()
        reader.join(1000)
        return ShellResult(-1, output.toString() + "\n(timed out after $timeoutSeconds s)")
    }
    reader.join()
    return ShellResult(exitValue(), output.toString())
}

/** [text] as one zsh word. */
fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"
