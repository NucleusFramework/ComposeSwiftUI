package dev.nucleusframework.composeswiftui.idea.remote

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import dev.nucleusframework.composeswiftui.idea.PreviewSettings
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** The Mac of the Apple tools, from the settings: null when there is none (not a Mac, and no remote Mac set). */
object MacHosts {
    private val shells = ConcurrentHashMap<String, SshMacShell>()

    /** The configured remote Mac, when the IDE should use it. */
    fun remote(): SshMacShell? {
        val destination = PreviewSettings.options.remoteMac.orEmpty().trim()
        if (destination.isEmpty()) return null
        if (SystemInfo.isMac && !PreviewSettings.options.remoteMacOnMac) return null
        return shells.getOrPut(destination) { SshMacShell(destination) }
    }

    fun shell(): MacShell? = remote() ?: LocalMacShell.takeIf { SystemInfo.isMac }

    /** Why there is no Mac, for the UI. */
    const val NO_MAC = "The Apple tools need a Mac: set one in Settings › Tools › Compose SwiftUI Previews › Remote Mac."

    private val workspaces = ConcurrentHashMap<Pair<String, String>, RemoteWorkspace>()

    /**
     * The copy on the remote Mac of the Gradle build at [rootProjectPath], when a remote Mac is used: of its whole Git
     * repository, so builds it includes from elsewhere in the repository are there too.
     */
    fun workspace(rootProjectPath: String): RemoteWorkspace? {
        val shell = remote() ?: return null
        val root = generateSequence(File(rootProjectPath)) { it.parentFile }.firstOrNull { File(it, ".git").exists() }?.path ?: rootProjectPath
        return workspaces.getOrPut(shell.destination to root) { RemoteWorkspace(shell, root) }
    }
}

/**
 * A Gradle build mirrored on a remote Mac: the sources go there with rsync before each build, and the outputs the IDE
 * reads come back. Paths in the text that comes back (compiler errors, the previews' results) are mapped to this
 * machine's.
 */
class RemoteWorkspace(val shell: SshMacShell, val localRoot: String) {
    val remoteRoot: String by lazy {
        val name = File(localRoot).name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        "${shell.home}/.compose-swiftui/projects/$name-${Integer.toHexString(localRoot.hashCode())}"
    }

    fun toRemote(local: String): String =
        if (local == localRoot || local.startsWith("$localRoot/")) remoteRoot + local.removePrefix(localRoot) else local

    fun toRemote(local: File): String = toRemote(local.path)

    /** [remoteRoot] with its symbolic links resolved (`~/.compose-swiftui/projects` may link to another disk). */
    private val remoteRealRoot: String by lazy {
        shell.run("mkdir -p ${quote(remoteRoot)} && cd ${quote(remoteRoot)} && pwd -P", timeoutSeconds = 30)
            .takeIf { it.success }?.output?.trim()?.takeIf { it.startsWith("/") } ?: remoteRoot
    }

    /** [text] with the Mac's paths of the build (as given or as resolved, which the tools write) mapped to this machine's. */
    fun toLocalText(text: String): String {
        val mapped = text.replace(remoteRoot, localRoot)
        return if (remoteRealRoot == remoteRoot) mapped else mapped.replace(remoteRealRoot, localRoot)
    }

    /** Whether [text] mentions the build's paths on the Mac. */
    fun mentionsRemote(text: String) = remoteRoot in text || remoteRealRoot in text

    /** Sends the sources: only what changed, and deletes what was deleted here (except build outputs, kept there). */
    @Synchronized
    fun upload(): ShellResult {
        val started = System.nanoTime()
        shell.run("mkdir -p ${quote(remoteRoot)}", timeoutSeconds = 30).takeIf { !it.success }?.let { return it }
        val result = rsync(
            listOf("--delete") + EXCLUDES.flatMap { listOf("--exclude", it) } +
                gitIgnoredProtections(File(localRoot)).flatMap { listOf("--filter", "P $it") },
            "$localRoot/",
            "${shell.destination}:${remoteRoot}/",
        )
        LOG.info("Uploaded $localRoot in ${(System.nanoTime() - started) / 1_000_000} ms")
        return result
    }

    /** Copies the remote [relative] directory (of the build) here, replacing its content. */
    fun downloadDirectory(relative: String, filters: List<String> = emptyList(), delete: Boolean = true): ShellResult {
        val local = File(localRoot, relative).apply { mkdirs() }
        return rsync(
            (if (delete) listOf("--delete") else emptyList()) + filters,
            "${shell.destination}:${remoteRoot}/$relative/",
            "${local.path}/",
        )
    }

    /** Writes [content] to the remote copy of the local [file]. */
    fun write(file: File, content: ByteArray): ShellResult {
        val remote = toRemote(file)
        return shell.run("mkdir -p ${quote(File(remote).parent)} && cat > ${quote(remote)}", content, timeoutSeconds = 30)
    }

    /** The remote copy of the local [file], or null when it doesn't exist there. */
    fun read(file: File): String? {
        val result = shell.run("cat ${quote(toRemote(file))} 2>/dev/null", timeoutSeconds = 30)
        return if (result.success) result.output else null
    }

    private fun rsync(options: List<String>, source: String, target: String): ShellResult {
        val command = listOf("rsync", "-a", "-e", shell.rsyncShell) + options + listOf(source, target)
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(10, TimeUnit.MINUTES)) process.destroyForcibly()
        val result = ShellResult(process.exitValue(), output)
        if (!result.success) LOG.warn("rsync $source -> $target failed: $output")
        return result
    }

    companion object {
        private val LOG = logger<RemoteWorkspace>()

        /**
         * What the `.gitignore` files of [root] ignore, as rsync patterns: kept on the Mac though missing here. Tools
         * generate such files there (the Swift renderer Compose SwiftUI writes into the Xcode project, Pods), and the
         * build needs them; deleting them each time would break it. Only their deletion is prevented: what exists here
         * is still sent.
         */
        fun gitIgnoredProtections(root: File): List<String> =
            root.walkTopDown()
                .onEnter { it == root || it.name !in setOf("build", ".gradle", ".git", ".kotlin", ".idea", "node_modules", "DerivedData") }
                .filter { it.isFile && it.name == ".gitignore" }
                .flatMap { file ->
                    val dir = file.parentFile.relativeTo(root).path.let { if (it.isEmpty()) "" else "/$it" }
                    file.readLines().map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("!") }
                        .flatMap { pattern ->
                            val anchored = pattern.trimEnd('/').contains('/')
                            val path = pattern.removePrefix("/")
                            // Unanchored patterns match at any depth below their .gitignore, as in Git.
                            if (anchored) listOf("$dir/$path") else listOf("$dir/$path", "$dir/**/$path")
                        }
                }
                .distinct()
                .toList()

        /**
         * Never sent, and never deleted there: outputs, caches, local state of this machine's tools, and
         * `local.properties`, whose SDK paths are this machine's.
         */
        val EXCLUDES = listOf(
            "build/", ".gradle/", ".kotlin/", ".idea/", ".git/", "*.iml", ".DS_Store", "local.properties",
            "xcuserdata/", "DerivedData/", ".swiftpm/", "Pods/",
        )
    }
}
