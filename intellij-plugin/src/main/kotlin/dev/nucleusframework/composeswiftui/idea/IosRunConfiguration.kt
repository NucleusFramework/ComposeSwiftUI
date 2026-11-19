package dev.nucleusframework.composeswiftui.idea

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.quote
import java.io.File
import javax.swing.JComponent
import javax.swing.JTextField

/** *iOS App*: builds the Xcode project of the app on the Mac (this one or the remote one) and runs it on a simulator. */
class IosRunConfigurationType : ConfigurationType {
    override fun getDisplayName() = "iOS App"
    override fun getConfigurationTypeDescription() = "Builds an iOS app with Xcode on the Mac and runs it on a simulator"
    override fun getIcon() = ComposeSwiftUIIcons.Simulator
    override fun getId() = "ComposeSwiftUIIosApp"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(IosRunConfigurationFactory(this))
}

class IosRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = "ComposeSwiftUIIosApp"
    override fun getOptionsClass() = IosRunConfigurationOptions::class.java
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        IosRunConfiguration(project, this, "iOS App").apply { guessDefaults() }
}

class IosRunConfigurationOptions : RunConfigurationOptions() {
    /** The `.xcodeproj` or `.xcworkspace`, relative to the project. */
    var xcodeProject by string("")
    var scheme by string("")
    var configuration by string("Debug")

    /** Simulator name or UDID; empty: the booted one, else the first iPhone. */
    var simulator by string("")
}

class IosRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<IosRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions() = super.getOptions() as IosRunConfigurationOptions

    /** The only Xcode project of the project, and its scheme named like it. */
    fun guessDefaults() {
        val base = project.guessProjectDir()?.path ?: return
        val found = File(base).walkTopDown()
            .onEnter { it.name !in setOf("build", ".gradle", ".git", ".idea", ".kotlin", "node_modules", "Pods", "DerivedData") }
            .filter { it.name.endsWith(".xcodeproj") }
            .take(2).toList()
        if (found.size == 1) {
            options.xcodeProject = found[0].relativeTo(File(base)).path
            options.scheme = found[0].nameWithoutExtension
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = IosRunSettingsEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = IosRunState(this, environment)
}

internal class IosRunState(private val configuration: IosRunConfiguration, environment: ExecutionEnvironment) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val project = configuration.project
        val options = configuration.options
        val shell = MacHosts.shell() ?: throw ExecutionException(MacHosts.NO_MAC)
        val base = project.guessProjectDir()?.path ?: throw ExecutionException("The project has no directory")
        val xcodeProject = options.xcodeProject.orEmpty().ifEmpty { throw ExecutionException("Set the Xcode project in the run configuration") }
        val local = File(xcodeProject).takeIf { it.isAbsolute }?.path ?: File(base, xcodeProject).path
        // The project is synchronized from its directory (or its Git repository), so the app's Gradle build is there too.
        val workspace = MacHosts.workspace(if (local.startsWith("$base/")) base else File(local).parent)
        val path = workspace?.toRemote(local) ?: local
        val kind = if (xcodeProject.endsWith(".xcworkspace")) "-workspace" else "-project"
        val scheme = options.scheme.orEmpty().ifEmpty { File(xcodeProject).nameWithoutExtension }
        val configurationName = options.configuration.orEmpty().ifEmpty { "Debug" }
        val derivedData = "${shell.home}/.compose-swiftui/DerivedData/${Integer.toHexString(local.hashCode())}"
        // The simulator picked in the toolbar, else the configuration's.
        // The same lock as the previews' renders of this copy; on this Mac, one out of the project.
        val buildLock = workspace?.buildLock ?: "${shell.home}/.compose-swiftui/locks/${Integer.toHexString(base.hashCode())}.lock"
        val simulator = (environment.executionTarget as? IosSimulatorTarget)?.simulator?.udid ?: options.simulator.orEmpty()

        // One script on the Mac: pick the simulator, build, install, launch with the app's output here. It stops (with
        // the app) when this side closes its input: the Stop button, even over SSH.
        val script = """
            run() {
              udid=${quote(simulator)}
              if [[ -n ${'$'}udid && ! ${'$'}udid =~ ^[0-9A-F-]{36}${'$'} ]]; then
                udid=${'$'}(xcrun simctl list devices available | grep -F " ${'$'}udid (" | head -1 | grep -oE '[0-9A-F-]{36}')
              fi
              # The booted one, but not the previews' own simulator (booted in the background, maybe an older iOS).
              [[ -z ${'$'}udid ]] && udid=${'$'}(xcrun simctl list devices booted | grep -v 'Compose SwiftUI Previews' | grep -oE '[0-9A-F-]{36}' | head -1)
              [[ -z ${'$'}udid ]] && udid=${'$'}(xcrun simctl list devices available | grep -E '^ +iPhone' | head -1 | grep -oE '[0-9A-F-]{36}')
              [[ -z ${'$'}udid ]] && { echo "No iOS simulator"; return 1; }
              echo "Simulator ${'$'}udid"
              xcrun simctl boot ${'$'}udid 2>/dev/null; xcrun simctl bootstatus ${'$'}udid -b >/dev/null
              echo "Building $scheme…"
              # One build at a time on the project, with the previews' Gradle renders.
              lockf -k ${quote(buildLock)} xcodebuild $kind ${quote(path)} -scheme ${quote(scheme)} -configuration ${quote(configurationName)} \
                -destination "id=${'$'}udid" -derivedDataPath ${quote(derivedData)} build 2>&1 \
                | grep --line-buffered -E '(error|warning):|^\*\* |^Ld |^CompileSwift |^PhaseScriptExecution |> Task :'
              # xcodebuild's own status, not the filter's.
              (( ${'$'}{pipestatus[1]} == 0 )) || { echo "The build failed"; return 1; }
              app=${'$'}(ls -d ${quote(derivedData)}/Build/Products/${quote(configurationName)}-iphonesimulator/*.app 2>/dev/null | head -1)
              [[ -z ${'$'}app ]] && { echo "The build produced no app"; return 1; }
              bundle=${'$'}(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "${'$'}app/Info.plist")
              echo "Installing ${'$'}bundle…"
              xcrun simctl install ${'$'}udid "${'$'}app" || return 1
              echo "${'$'}udid ${'$'}bundle" > ${quote("$derivedData/running")}
              xcrun simctl launch --console-pty --terminate-running-process ${'$'}udid ${'$'}bundle
            }
            rm -f ${quote("$derivedData/running")}
            # The watcher reads this side's input explicitly: a background job's own input is /dev/null.
            exec 3<&0
            ( cat <&3 >/dev/null 2>&1
              read -r udid bundle < ${quote("$derivedData/running")} && xcrun simctl terminate ${'$'}udid ${'$'}bundle
              pkill -f ${quote("xcodebuild .*-derivedDataPath $derivedData")}
              pkill -P ${'$'}${'$'} ) </dev/null >/dev/null 2>&1 &
            watcher=${'$'}!
            run; code=${'$'}?
            pkill -P ${'$'}watcher; kill ${'$'}watcher 2>/dev/null
            exit ${'$'}code
        """.trimIndent()

        if (workspace != null) {
            val upload = workspace.upload()
            if (!upload.success) throw ExecutionException("Couldn't copy the project to the Mac:\n${upload.output}")
        }
        val process = shell.start("mkdir -p ${quote(derivedData)} ${quote(File(buildLock).parent)} && $script")
        val handler = KillableColoredProcessHandler(process, GeneralCommandLine("xcodebuild").commandLineString)
        ProcessTerminatedListener.attach(handler)
        // The app is on the simulator: show it.
        ToolWindowManager.getInstance(project).invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow("iOS Simulator")?.show()
        }
        return handler
    }
}

private class IosRunSettingsEditor : SettingsEditor<IosRunConfiguration>() {
    private val xcodeProject = JTextField()
    private val scheme = JTextField()
    private val configuration = JTextField()
    private val simulator = JTextField()

    override fun resetEditorFrom(s: IosRunConfiguration) {
        xcodeProject.text = s.options.xcodeProject.orEmpty()
        scheme.text = s.options.scheme.orEmpty()
        configuration.text = s.options.configuration.orEmpty()
        simulator.text = s.options.simulator.orEmpty()
    }

    override fun applyEditorTo(s: IosRunConfiguration) {
        s.options.xcodeProject = xcodeProject.text.trim()
        s.options.scheme = scheme.text.trim()
        s.options.configuration = configuration.text.trim()
        s.options.simulator = simulator.text.trim()
    }

    override fun createEditor(): JComponent = panel {
        row("Xcode project:") { cell(xcodeProject).align(AlignX.FILL).comment("The .xcodeproj or .xcworkspace, relative to the project") }
        row("Scheme:") { cell(scheme).align(AlignX.FILL) }
        row("Configuration:") { cell(configuration).align(AlignX.FILL) }
        row("Simulator:") { cell(simulator).align(AlignX.FILL).comment("Name or UDID; empty: the booted one, else the first iPhone") }
        row { comment("Runs on the remote Mac set in Settings › Tools › Compose SwiftUI Previews when the IDE is not on a Mac.") }
    }
}
