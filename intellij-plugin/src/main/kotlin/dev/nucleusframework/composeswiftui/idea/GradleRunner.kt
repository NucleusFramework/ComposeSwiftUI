package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.RemoteGradle
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.task.TaskStartEvent
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import java.io.File
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * Runs the preview tasks in the background through the Gradle Tooling API, as Android Studio does for its previews:
 * no IntelliJ init scripts, Run tool window or build view (whose processing cost more than the build itself), and
 * the same arguments every time, so Gradle reuses its configuration cache. One connection per Gradle build, kept
 * open so its daemon stays warm.
 */
@Service(Service.Level.PROJECT)
class GradleRunner(private val project: Project) : Disposable {
    private val connections = ConcurrentHashMap<String, ProjectConnection>()
    private val javaHomes = ConcurrentHashMap<String, String>()

    /** One build at a time per Gradle build: two at once would start a second daemon. */
    private val builds = ConcurrentHashMap<String, Semaphore>()

    /**
     * Runs [task] of [module] with [arguments]; [progress] gets each task path as it starts, [done] the success and
     * the output. Returns immediately.
     */
    fun run(module: PreviewModule, task: String, arguments: List<String>, progress: (String) -> Unit, done: (Boolean, String) -> Unit) {
        // Resolving the Gradle JVM and connecting block: never on the UI thread.
        ApplicationManager.getApplication().executeOnPooledThread {
            start(module.rootProjectPath, listOf(module.taskPath(task)), arguments, module, task, progress, done)
        }
    }

    /**
     * Runs [tasks] of the Gradle build at [rootProjectPath] with [arguments] (locally or on the remote Mac);
     * [progress] gets each task path as it starts, [done] the success and the output. Returns immediately.
     */
    fun runTasks(
        rootProjectPath: String,
        tasks: List<String>,
        arguments: List<String>,
        progress: (String) -> Unit,
        done: (Boolean, String) -> Unit,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            start(rootProjectPath, tasks, arguments, null, null, progress, done)
        }
    }

    private fun start(
        rootProjectPath: String,
        tasks: List<String>,
        arguments: List<String>,
        module: PreviewModule?,
        previewTask: String?,
        progress: (String) -> Unit,
        finished: (Boolean, String) -> Unit,
    ) {
        val lock = builds.getOrPut(rootProjectPath) { Semaphore(1) }
        lock.acquire()
        val done = { success: Boolean, output: String ->
            lock.release()
            finished(success, output)
        }
        // On Linux or Windows, Gradle runs on the remote Mac.
        MacHosts.workspace(rootProjectPath)?.let { workspace ->
            try {
                if (module != null && previewTask != null) {
                    RemoteGradle.run(workspace, module, previewTask, arguments, progress, done)
                } else {
                    RemoteGradle.runTasks(workspace, rootProjectPath, tasks, arguments, progress, done)
                }
            } catch (e: Exception) {
                LOG.warn("Remote Gradle failed", e)
                done(false, e.message.orEmpty())
            }
            return
        }
        val output = StringBuffer()
        val sink = object : OutputStream() {
            override fun write(b: Int) {
                output.append(b.toChar())
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                output.append(String(b, off, len))
            }
        }
        try {
            val connection = connections.getOrPut(rootProjectPath) {
                GradleConnector.newConnector().forProjectDirectory(File(rootProjectPath)).connect()
            }
            val build = connection.newBuild()
                .forTasks(*tasks.toTypedArray())
                .withArguments(arguments)
                .setStandardOutput(sink)
                .setStandardError(sink)
                .addProgressListener({ event ->
                    if (event is TaskStartEvent) progress(event.descriptor.taskPath)
                }, OperationType.TASK)
            // The project's Gradle JVM, as for the IDE's own Gradle runs.
            javaHomes.getOrPut(rootProjectPath) {
                // Needs a progress context off the UI thread.
                runCatching {
                    ProgressManager.getInstance().runProcess<String?>(
                        { GradleInstallationManager.getInstance().getGradleJvmPath(project, rootProjectPath) },
                        EmptyProgressIndicator(),
                    )
                }.getOrNull().orEmpty()
            }.takeIf { it.isNotEmpty() }?.let { build.setJavaHome(File(it)) }
            build.run(object : ResultHandler<Void?> {
                override fun onComplete(result: Void?) = done(true, output.toString())
                override fun onFailure(failure: GradleConnectionException) {
                    LOG.info("Gradle failed for ${module?.gradlePath ?: rootProjectPath}: ${failure.message}")
                    done(false, output.toString() + "\n" + (failure.cause?.message ?: failure.message.orEmpty()))
                }
            })
        } catch (e: Exception) {
            LOG.warn("Gradle Tooling API unavailable", e)
            done(false, e.message.orEmpty())
        }
    }

    override fun dispose() {
        connections.values.forEach { runCatching { it.close() } }
        connections.clear()
    }

    private companion object {
        val LOG = logger<GradleRunner>()
    }
}
