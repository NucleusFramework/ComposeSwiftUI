package dev.nucleusframework.composeswiftui.idea

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.externalSystem.util.task.TaskExecutionSpec
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.jetbrains.plugins.gradle.util.GradleConstants

/** A compiler error or a Gradle failure of a render, with its location when it has one. */
data class BuildProblem(val message: String, val path: String? = null, val line: Int = 0, val column: Int = 0)

/** How a render ended. */
data class RenderOutcome(
    val success: Boolean,
    val problems: List<BuildProblem>,
    val milliseconds: Long,
    /** Gradle compiled the code (false: the running host rendered the last build). */
    val built: Boolean = true,
)

interface PreviewRenderListener {
    /** [ids]: qualified names of the previews being rendered, empty for all of the module. */
    fun renderingStarted(module: PreviewModule, ids: Set<String>) {}
    fun renderingFinished(module: PreviewModule, ids: Set<String>, outcome: RenderOutcome) {}

    /** What the render is doing now: compiling, linking, rendering. */
    fun renderingProgress(module: PreviewModule, step: String) {}

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(PreviewRenderListener::class.java)
    }
}

/**
 * Runs `renderComposeSwiftUIPreviews`, at most once at a time per module. Requests made during a run are merged into
 * one more run, for the union of their previews.
 */
@Service(Service.Level.PROJECT)
class PreviewRenderer(private val project: Project) {
    private class State {
        var running: Set<String>? = null
        var startedAt = 0L
        var callbacks = ArrayList<(RenderOutcome) -> Unit>()

        /** Previews requested during the run; empty set: all of them. */
        var pending: MutableSet<String>? = null
        var pendingCallbacks = ArrayList<(RenderOutcome) -> Unit>()
    }

    private val states = HashMap<PreviewModule, State>()

    fun isRendering(module: PreviewModule) = synchronized(this) { states[module]?.running != null }

    /** Renders the previews [ids] (qualified names) of [module], or all of them when empty, then calls [then] (on the EDT). */
    fun render(module: PreviewModule, ids: Set<String>, then: ((RenderOutcome) -> Unit)? = null) {
        synchronized(this) {
            val state = states.getOrPut(module) { State() }
            if (state.running != null) {
                then?.let { state.pendingCallbacks += it }
                val pending = state.pending
                state.pending = when {
                    pending == null -> ids.toMutableSet()
                    pending.isEmpty() || ids.isEmpty() -> mutableSetOf()
                    else -> pending.apply { addAll(ids) }
                }
                return
            }
            state.running = ids
            state.startedAt = System.currentTimeMillis()
            then?.let { state.callbacks += it }
            codeChangedAt.putIfAbsent(module.rootProjectPath, 0L)
        }
        LOG.info("Rendering ${ids.ifEmpty { setOf("all") }} of ${module.gradlePath}")
        publisher().renderingStarted(module, ids)
        showProgress(module)
        val started = System.nanoTime()
        val code = codeChangedAt[module.rootProjectPath] ?: Long.MAX_VALUE
        val built = builtAt[module] ?: Long.MIN_VALUE
        val unsaved = FileDocumentManager.getInstance().unsavedDocuments.any { FileDocumentManager.getInstance().getFile(it)?.extension == "kt" }
        if (code < built && !unsaved) {
            // Nothing to build since the last build: the running host renders right away, without Gradle.
            LOG.info("Rendering ${module.gradlePath} on the running host, without Gradle")
            progressBars[module]?.step = "Rendering on the simulator…"
            publisher().renderingProgress(module, "Rendering on the simulator…")
            ApplicationManager.getApplication().executeOnPooledThread {
                val done = ids.isNotEmpty() && PreviewHostClient(module).render(ids, emptyList(), PreviewSettings.options.settleMs)
                if (done) {
                    finished(module, ids, RenderOutcome(true, emptyList(), (System.nanoTime() - started) / 1_000_000, built = false))
                } else {
                    build(module, ids, started)
                }
            }
            return
        }
        build(module, ids, started)
    }

    /** Builds and renders with Gradle. */
    private fun build(module: PreviewModule, ids: Set<String>, started: Long) {
        val buildStart = System.currentTimeMillis()
        writeRequest(module, ids, interactive = null)
        progressBars[module]?.step = "Starting Gradle…"
        ApplicationManager.getApplication().invokeLater { publisher().renderingProgress(module, "Starting Gradle…") }
        var stepStart = System.nanoTime()
        var lastStep = "Gradle startup and configuration"
        val progress = { taskPath: String ->
            // Where the time of a render goes, in the log.
            val now = System.nanoTime()
            LOG.info("$lastStep: ${(now - stepStart) / 1_000_000} ms")
            stepStart = now
            lastStep = taskPath
            val step = when {
                ":compileKotlin" in taskPath -> "Compiling Kotlin…"
                ":link" in taskPath -> "Linking the framework…"
                ":renderComposeSwiftUIPreviews" in taskPath -> "Rendering on the simulator…"
                else -> null
            }
            if (step != null) {
                progressBars[module]?.step = step
                ApplicationManager.getApplication().invokeLater { publisher().renderingProgress(module, step) }
            }
        }
        gradle(module, progress) { success, output ->
            LOG.info("$lastStep: ${(System.nanoTime() - stepStart) / 1_000_000} ms")
            if (success) builtAt[module] = buildStart
            val outcome = RenderOutcome(success, if (success) emptyList() else problems(output), (System.nanoTime() - started) / 1_000_000)
            finished(module, ids, outcome)
        }
    }

    /** Runs the render task in the background (Tooling API); again without the configuration cache if it doesn't suit the build. */
    private fun gradle(module: PreviewModule, progress: (String) -> Unit, done: (Boolean, String) -> Unit) {
        val arguments = properties(module)
        project.service<GradleRunner>().run(module, "renderComposeSwiftUIPreviews", arguments, progress) { success, output ->
            if (!success && "--configuration-cache" in arguments && "configuration cache" in output.lowercase()) {
                withoutConfigurationCache += module
                project.service<GradleRunner>().run(module, "renderComposeSwiftUIPreviews", properties(module), progress, done)
            } else {
                done(success, output)
            }
        }
    }

    /** Last change of a Kotlin file of each Gradle build (by root), and start of the last successful build of a module. */
    private val codeChangedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val builtAt = java.util.concurrent.ConcurrentHashMap<PreviewModule, Long>()

    init {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val now = System.currentTimeMillis()
                for (event in events) {
                    val path = event.path
                    // Sources and resources; not outputs (the render writes some) or IDE and VCS files.
                    if (ignoredDirs.any { it in path }) continue
                    codeChangedAt.keys.filter { path.startsWith(it) }.forEach { codeChangedAt[it] = now }
                    states.keys.map { it.rootProjectPath }.filter { path.startsWith(it) }.forEach { codeChangedAt[it] = now }
                }
            }
        })
    }

    /**
     * The same arguments for every render, so Gradle reuses its configuration cache (no configuration phase): what to
     * render goes in a request file the task reads.
     */
    private fun properties(module: PreviewModule) = buildList {
        if (PreviewSettings.options.incrementalNative) add("-Pkotlin.incremental.native=true")
        add("-PcomposeSwiftUI.previewSettleMs=${PreviewSettings.options.settleMs}")
        if (PreviewSettings.options.configurationCache && module !in withoutConfigurationCache) {
            add("--configuration-cache")
            // A build with configuration cache problems still renders.
            add("-Dorg.gradle.configuration-cache.problems=warn")
        }
    }

    private fun writeRequest(module: PreviewModule, ids: Set<String>, interactive: String?) {
        val request = com.google.gson.JsonObject().apply {
            add("previews", com.google.gson.JsonArray().apply { ids.sorted().forEach(::add) })
            interactive?.let { addProperty("interactive", it) }
        }
        java.io.File(module.moduleDir, "build/composeSwiftUI/previewHost").apply { mkdirs() }
            .resolve("ide-request.json").writeText(request.toString())
    }

    /** Projects whose build can't run with the configuration cache: rendered without it from then on. */
    private val withoutConfigurationCache = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<PreviewModule, Boolean>())

    /** Opens the preview [key] live on the simulator, where it can be used like the app. */
    fun runInteractive(module: PreviewModule, key: String) {
        writeRequest(module, setOf(key.substringBefore('~').substringBefore('#')), interactive = key)
        gradle(module, {}) { success, output ->
            if (!success) LOG.warn("Run on Simulator failed:\n${output.takeLast(4000)}")
        }
    }

    private fun run(
        module: PreviewModule,
        task: String,
        properties: List<String>,
        output: StringBuilder,
        activateOnFailure: Boolean = false,
        progress: (String) -> Unit,
        done: (Boolean, String) -> Unit,
    ) {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = module.rootProjectPath
            taskNames = listOf(module.taskPath(task))
            scriptParameters = properties.joinToString(" ")
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }
        val listener = object : ExternalSystemTaskNotificationListener {
            override fun onTaskOutput(id: ExternalSystemTaskId, text: String, outputType: ProcessOutputType) {
                synchronized(output) { output.append(text) }
                if (text.startsWith("> Task ")) progress(text)
            }
        }
        val callback = object : TaskCallback {
            override fun onSuccess() = done(true, output.toString())
            override fun onFailure() {
                val text = output.toString()
                // The configuration cache doesn't suit this build: again, without it, from now on.
                if ("--configuration-cache" in properties && "configuration cache" in text.lowercase() && "What went wrong" in text) {
                    withoutConfigurationCache += module
                    output.setLength(0)
                    run(module, task, properties(module), output, activateOnFailure, progress, done)
                } else {
                    done(false, text)
                }
            }
        }
        val spec = TaskExecutionSpec.create()
            .withProject(project)
            .withSystemId(GradleConstants.SYSTEM_ID)
            .withExecutorId(DefaultRunExecutor.EXECUTOR_ID)
            .withSettings(settings)
            .withProgressExecutionMode(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
            // Renders run in the background, often on save: they don't take the focus from the editor.
            .withActivateToolWindowBeforeRun(false)
            .withActivateToolWindowOnFailure(activateOnFailure)
            .dontNavigateToError()
            .withListener(listener)
            .withCallback(callback)
            .build()
        ExternalSystemUtil.runTask(spec)
    }

    /** The IDE's progress bar while [module] renders: shows the current step, ends with the render. */
    private val progressBars = ConcurrentHashMap<PreviewModule, ProgressBar>()

    private class ProgressBar {
        val done = java.util.concurrent.CountDownLatch(1)
        @Volatile var step = "Rendering previews…"
    }

    private fun showProgress(module: PreviewModule) {
        // Headless, a background task runs right away on this thread: it would wait for itself.
        if (ApplicationManager.getApplication().isHeadlessEnvironment) return
        val bar = ProgressBar()
        progressBars[module] = bar
        object : Task.Backgroundable(project, "Rendering Compose SwiftUI previews", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                while (!bar.done.await(100, TimeUnit.MILLISECONDS)) indicator.text = bar.step
            }
        }.queue()
    }

    private fun finished(module: PreviewModule, ids: Set<String>, outcome: RenderOutcome) {
        progressBars.remove(module)?.done?.countDown()
        LOG.info("Rendered ${module.gradlePath}: success=${outcome.success} in ${outcome.milliseconds} ms")
        val (again, callbacks, nextCallbacks) = synchronized(this) {
            val state = states.getValue(module)
            val ran = state.running.orEmpty()
            state.running = null
            val callbacks = state.callbacks.also { state.callbacks = ArrayList() }
            val next = state.pendingCallbacks.also { state.pendingCallbacks = ArrayList() }
            val pending = state.pending.also { state.pending = null }
            // Asked for during a run that covered it, with no change since it started (the pane opening, a click).
            val covered = pending != null && (ran.isEmpty() || pending.isNotEmpty() && ran.containsAll(pending))
            val unchanged = (codeChangedAt[module.rootProjectPath] ?: Long.MAX_VALUE) < state.startedAt
            if (outcome.success && covered && unchanged) Triple(null, callbacks + next, ArrayList()) else Triple(pending, callbacks, next)
        }
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            publisher().renderingFinished(module, ids, outcome)
            callbacks.forEach { it(outcome) }
            if (again != null) {
                render(module, again) { outcome -> nextCallbacks.forEach { it(outcome) } }
            }
        }
    }

    private fun publisher() = project.messageBus.syncPublisher(PreviewRenderListener.TOPIC)

    companion object {
        private val LOG = logger<PreviewRenderer>()
        private val ignoredDirs = listOf("/build/", "/.gradle/", "/.idea/", "/.git/", "/.kotlin/")
        private val kotlinError = Regex("""^e: (?:file://)?(.+?\.kts?):(\d+):(\d+):? (.*)$""")

        /** Kotlin compiler errors of the output, else Gradle's "What went wrong" explanation. */
        fun problems(output: String): List<BuildProblem> {
            val lines = output.lines()
            val errors = lines.mapNotNull { line ->
                kotlinError.find(line.trim())?.destructured?.let { (path, row, column, message) ->
                    BuildProblem(message, path, row.toInt(), column.toInt())
                }
            }.distinct()
            if (errors.isNotEmpty()) return errors
            val start = lines.indexOfFirst { it.startsWith("* What went wrong:") }
            if (start >= 0) {
                val explanation = lines.drop(start + 1).takeWhile { !it.startsWith("* Try:") }
                    .joinToString("\n").trim().removePrefix(">").trim()
                if (explanation.isNotEmpty()) return listOf(BuildProblem(explanation))
            }
            return listOf(BuildProblem("The render failed:\n" + lines.filter { it.isNotBlank() }.takeLast(15).joinToString("\n")))
        }
    }
}
