package dev.nucleusframework.composeswiftui.idea

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.ExecutionTargetProvider
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.Simulator
import dev.nucleusframework.composeswiftui.idea.remote.Simulators
import java.util.concurrent.atomic.AtomicBoolean

/** A simulator to run an *iOS App* configuration on, picked next to the configuration in the toolbar. */
class IosSimulatorTarget(val simulator: Simulator) : ExecutionTarget() {
    override fun getId() = "ios-simulator:${simulator.udid}"
    override fun getDisplayName() = "${simulator.name} (${simulator.runtime})"
    override fun getIcon() = AllIcons.Nodes.Deploy
    override fun canRun(configuration: RunConfiguration) = configuration is IosRunConfiguration
}

/** The Mac's simulators as execution targets of *iOS App* configurations. */
class IosSimulatorTargetProvider : ExecutionTargetProvider() {
    override fun getTargets(project: Project, configuration: RunConfiguration): List<ExecutionTarget> {
        if (configuration !is IosRunConfiguration) return emptyList()
        val cache = service<SimulatorList>()
        cache.refreshIfNeeded(project)
        return cache.simulators.map(::IosSimulatorTarget)
    }
}

/** The simulators of the Mac, listed in the background (it takes a round trip to the Mac) and kept. */
@Service(Service.Level.APP)
class SimulatorList {
    @Volatile var simulators: List<Simulator> = emptyList()
        private set
    private val listing = AtomicBoolean(false)
    @Volatile private var listedAt = 0L

    /** Lists them again when the list is older than a minute; then updates the toolbar's targets of [project]. */
    fun refreshIfNeeded(project: Project) {
        if (System.currentTimeMillis() - listedAt < 60_000 || !listing.compareAndSet(false, true)) return
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val shell = MacHosts.shell() ?: return@executeOnPooledThread
                simulators = Simulators(shell).list()
                listedAt = System.currentTimeMillis()
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) ExecutionTargetManager.update(project)
                }
            } catch (_: Exception) {
                // The Mac is unreachable: try again on the next request.
            } finally {
                listing.set(false)
            }
        }
    }
}

/**
 * On Linux or Windows (or a Mac using a remote Mac), adds an *iOS App* configuration for the project's Xcode project,
 * as the Kotlin Multiplatform plugin does on a Mac: the app runs from the toolbar on the simulator picked there.
 */
class IosRunConfigurationCreator : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (SystemInfo.isMac && MacHosts.remote() == null) return
        val runManager = RunManager.getInstance(project)
        if (runManager.allConfigurationsList.any { it is IosRunConfiguration }) return
        val factory = IosRunConfigurationType().configurationFactories.single()
        val template = IosRunConfiguration(project, factory, "iOS App").apply { guessDefaults() }
        val xcodeProject = template.options.xcodeProject.orEmpty().ifEmpty { return }
        val settings = runManager.createConfiguration(template.options.scheme.orEmpty().ifEmpty { "iosApp" }, factory)
        (settings.configuration as IosRunConfiguration).options.apply {
            this.xcodeProject = xcodeProject
            scheme = template.options.scheme
        }
        runManager.addConfiguration(settings)
        if (runManager.selectedConfiguration == null) runManager.selectedConfiguration = settings
        service<SimulatorList>().refreshIfNeeded(project)
    }
}
