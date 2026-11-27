package dev.nucleusframework.composeswiftui.idea.remote

import com.intellij.openapi.diagnostic.logger
import dev.nucleusframework.composeswiftui.idea.PreviewModule
import java.io.File

/**
 * Runs the preview tasks on a remote Mac: the build is synchronized there, Gradle runs in the Mac's daemon (kept warm
 * between renders), and the outputs the IDE reads come back with this machine's paths.
 */
object RemoteGradle {
    private val LOG = logger<RemoteGradle>()

    /** Runs [task] of [module] on the Mac of [workspace], like `GradleRunner.run` does locally. Blocks. */
    fun run(
        workspace: RemoteWorkspace,
        module: PreviewModule,
        task: String,
        arguments: List<String>,
        progress: (String) -> Unit,
        done: (Boolean, String) -> Unit,
    ) {
        val upload = workspace.upload()
        if (!upload.success) return done(false, "Couldn't copy the project to the Mac (${workspace.shell.destination}):\n${upload.output}")
        // What to render: the request the IDE wrote next to the module's outputs, which are not synchronized.
        val request = File(module.moduleDir, "build/composeSwiftUI/previewHost/ide-request.json")
        if (request.isFile) workspace.write(request, request.readBytes())

        val (success, output) = execute(workspace, module.rootProjectPath, listOf(module.taskPath(task)), arguments, progress)
        pullOutputs(workspace, module)
        done(success, output)
    }

    /** Runs [tasks] of the Gradle build at [rootProjectPath] on the Mac of [workspace]. Blocks. */
    fun runTasks(
        workspace: RemoteWorkspace,
        rootProjectPath: String,
        tasks: List<String>,
        arguments: List<String>,
        progress: (String) -> Unit,
        done: (Boolean, String) -> Unit,
    ) {
        val upload = workspace.upload()
        if (!upload.success) return done(false, "Couldn't copy the project to the Mac (${workspace.shell.destination}):\n${upload.output}")
        val (success, output) = execute(workspace, rootProjectPath, tasks, arguments, progress)
        done(success, output)
    }

    private fun execute(
        workspace: RemoteWorkspace,
        rootProjectPath: String,
        tasks: List<String>,
        arguments: List<String>,
        progress: (String) -> Unit,
    ): Pair<Boolean, String> {
        val output = StringBuilder()
        val command = buildString {
            append("cd ${quote(workspace.toRemote(rootProjectPath))} && ")
            // An Android target needs the SDK: the Mac's own, as local.properties is not copied.
            append("{ [[ -n \$ANDROID_HOME || ! -d \$HOME/Library/Android/sdk ]] || export ANDROID_HOME=\$HOME/Library/Android/sdk; } && ")
            // One build at a time on this copy, with the Xcode builds of the iOS App configuration.
            append("lockf -k ${quote(workspace.buildLock)} ./gradlew --console=plain ")
            append((tasks + arguments).joinToString(" ") { quote(it) })
        }
        val process = workspace.shell.start(command)
        process.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                output.append(line).append('\n')
                if (line.startsWith("> Task ")) progress(line.removePrefix("> Task ").substringBefore(' '))
            }
        }
        val success = process.waitFor() == 0
        return success to workspace.toLocalText(output.toString())
    }

    /** Brings back what the IDE reads after a render: the images and results, the manifest, the running hosts' state. */
    fun pullOutputs(workspace: RemoteWorkspace, module: PreviewModule) {
        val started = System.nanoTime()
        // Relative to the synchronized root, which may be above the Gradle build (its Git repository).
        val moduleDir = File(module.moduleDir).relativeTo(File(workspace.localRoot)).path.let { if (it.isEmpty()) "" else "$it/" }
        workspace.downloadDirectory("${moduleDir}build/composeSwiftUI/previews")
        workspace.downloadDirectory("${moduleDir}build/generated/composeSwiftUI/previews")
        workspace.downloadDirectory(
            "${moduleDir}build/composeSwiftUI/previewHost",
            HOST_STATE_FILTERS,
            delete = false,
        )
        mapPaths(workspace, File(module.previewsDir.path))
        mapPaths(workspace, module.manifestFile.parentFile)
        LOG.info("Pulled the outputs of ${module.gradlePath} in ${(System.nanoTime() - started) / 1_000_000} ms")
    }

    /** Only the small state files of the preview host: not the app, its objects, or the IDE's own files. */
    private val HOST_STATE_FILTERS = listOf(
        "--exclude", "ide-*",
        "--include", "mac/",
        "--include", "*.json",
        "--include", "*.build",
        "--include", "*.stamp",
        "--include", "bundle-id",
        "--exclude", "*/",
        "--exclude", "*",
    )

    /** Rewrites the Mac's paths in the text outputs of [dir] to this machine's. */
    private fun mapPaths(workspace: RemoteWorkspace, dir: File) {
        dir.listFiles()?.filter { it.isFile && it.extension in setOf("json", "txt", "md") }?.forEach { file ->
            val text = file.readText()
            if (workspace.mentionsRemote(text)) file.writeText(workspace.toLocalText(text))
        }
    }
}
