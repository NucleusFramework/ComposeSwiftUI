package dev.nucleusframework.composeswiftui.idea

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/** A Gradle module that applies the Compose SwiftUI plugin. */
data class PreviewModule(val rootProjectPath: String, val moduleDir: String) {
    /** Gradle path, from the directory layout (`<root>/feature/ui` → `:feature:ui`). */
    // ponytail: assumes the default project-dir layout; read GradleModuleData if a project remaps projectDir
    val gradlePath: String
        get() = File(moduleDir).relativeTo(File(rootProjectPath)).invariantSeparatorsPath
            .split('/').filter { it.isNotEmpty() }.joinToString("") { ":$it" }

    /** Written by the `renderComposeSwiftUIPreviews` task: `<qualified function name>.png`. */
    fun image(qualifiedName: String) = File(moduleDir, "build/composeSwiftUI/previews/$qualifiedName.png")

    companion object {
        fun of(project: Project, file: VirtualFile): PreviewModule? {
            val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
            val root = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return null
            val dir = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
            return PreviewModule(root, dir)
        }
    }
}

interface PreviewRenderListener {
    fun renderingStarted(module: PreviewModule) {}
    fun renderingFinished(module: PreviewModule, success: Boolean) {}

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(PreviewRenderListener::class.java)
    }
}

/** Runs `renderComposeSwiftUIPreviews`, at most once at a time per module (a request during a run queues one more). */
@Service(Service.Level.PROJECT)
class PreviewRenderer(private val project: Project) {
    private val running = mutableSetOf<PreviewModule>()
    private val pending = mutableSetOf<PreviewModule>()

    fun isRendering(module: PreviewModule) = synchronized(this) { module in running }

    fun render(module: PreviewModule) {
        synchronized(this) {
            if (module in running) {
                pending += module
                return
            }
            running += module
        }
        publisher().renderingStarted(module)
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = module.rootProjectPath
            taskNames = listOf("${module.gradlePath}:renderComposeSwiftUIPreviews")
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }
        val callback = object : TaskCallback {
            override fun onSuccess() = finished(module, true)
            override fun onFailure() = finished(module, false)
        }
        ExternalSystemUtil.runTask(
            settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID, callback,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC, false,
        )
    }

    private fun finished(module: PreviewModule, success: Boolean) {
        val again = synchronized(this) {
            running -= module
            pending.remove(module)
        }
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            publisher().renderingFinished(module, success)
            if (again) render(module)
        }
    }

    private fun publisher() = project.messageBus.syncPublisher(PreviewRenderListener.TOPIC)
}
