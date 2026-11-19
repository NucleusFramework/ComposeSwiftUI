package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonParser
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.SimulatorStream
import dev.nucleusframework.composeswiftui.idea.remote.Simulators
import dev.nucleusframework.composeswiftui.idea.remote.quote
import java.awt.event.MouseEvent
import java.io.DataInputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The IDE on Linux or Windows against a remote Mac: renders, live previews and the simulator stream go over SSH.
 * Needs `-Dcsui.e2e.remote=<ssh destination>` and `-Dcsui.e2e.remote.module=<…/samples/kmp-app/shared>`; skipped
 * otherwise.
 */
class RemoteMacEndToEndTest : BasePlatformTestCase() {
    private val destination: String? = System.getProperty("csui.e2e.remote")
    private val moduleDir: String? = System.getProperty("csui.e2e.remote.module")

    override fun setUp() {
        super.setUp()
        PreviewSettings.options.remoteMac = destination.orEmpty()
        PreviewSettings.options.remoteMacOnMac = true
    }

    private fun module(): PreviewModule? {
        if (destination == null) return null
        val dir = moduleDir ?: return null
        return PreviewModule(File(dir).parent, dir, ":shared")
    }

    fun testGradleRendersOnTheMacAndTheImagesComeBack() {
        val module = module() ?: return
        val (success, output) = gradleRender(module)
        assertTrue("the render succeeds:\n${output.takeLast(3000)}", success)
        val results = File(module.previewsDir, "results.json").readText()
        val workspace = MacHosts.workspace(module.rootProjectPath)!!
        assertFalse("the results have this machine's paths", workspace.mentionsRemote(results))
        val counter = JsonParser.parseString(results).asJsonObject.getAsJsonArray("previews")
            .map { it.asJsonObject }.first { it.get("id").asString == "sample.CounterScreenPreview" }
        assertTrue("the source is the local file", counter.get("source").asString.startsWith(module.rootProjectPath))
        assertTrue("the image came back", File(module.previewsDir, counter.get("file").asString).length() > 1000)
    }

    /** The other tests need a render (the manifest and a running host), whatever order they run in. */
    private fun ensureRendered(module: PreviewModule) {
        if (!module.manifestFile.isFile) assertTrue("the first render succeeds", gradleRender(module).first)
    }

    private fun gradleRender(module: PreviewModule): Pair<Boolean, String> {
        File(module.moduleDir, "build/composeSwiftUI/previewHost").mkdirs()
        File(module.moduleDir, "build/composeSwiftUI/previewHost/ide-request.json")
            .writeText("""{"previews":["sample.CounterScreenPreview","sample.SettingsPreview"]}""")
        val done = CountDownLatch(1)
        var result: Pair<Boolean, String>? = null
        project.service<GradleRunner>().run(module, "renderComposeSwiftUIPreviews", listOf("-Pkotlin.incremental.native=true"), {}) { success, output ->
            result = success to output
            done.countDown()
        }
        assertTrue("the render ends", done.await(15, TimeUnit.MINUTES))
        return result!!
    }

    fun testTheRunningHostRendersWithoutGradle() {
        val module = module() ?: return
        ensureRendered(module)
        val image = File(module.previewsDir, "sample.CounterScreenPreview.png")
        image.delete()
        assertTrue("the host renders", PreviewHostClient(module).render(listOf("sample.CounterScreenPreview"), emptyList(), 300))
        assertTrue("the image came back", image.length() > 1000)
    }

    fun testInteractiveTapChangesTheLivePreview() {
        val module = module() ?: return
        ensureRendered(module)
        val port = ServerSocket(0).use { it.localPort }
        // As the IDE does: when no host runs, a render starts one.
        val started = PreviewHostClient(module).startInteractive("sample.CounterScreenPreview", port, emptyList()) ||
            gradleRender(module).first && PreviewHostClient(module).startInteractive("sample.CounterScreenPreview", port, emptyList())
        assertTrue("start the live preview", started)
        val forward = MacHosts.workspace(module.rootProjectPath)!!.shell.forward(port)
        val panel = InteractivePreview("Counter", SourceNavigator(project, module)) {}
        try {
            panel.setSize(500, 1000)
            panel.doLayout()
            panel.connect(port)
            waitFor { panel.liveStatus.startsWith("Live ·") }
            assertTrue("the live preview streams: ${panel.liveStatus}", panel.liveStatus.startsWith("Live ·"))
            val screen = panel.liveScreen
            val before = snapshot(screen)
            val scale = minOf((screen.width - 24.0) / 402, (screen.height - 24.0) / 874, 2.0)
            val x = ((screen.width - 402 * scale) / 2 + 201 * scale).toInt()
            val y = (12 + 572 * scale).toInt()
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                screen.dispatchEvent(MouseEvent(screen, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            waitFor { !snapshot(screen).contentEquals(before) }
            assertFalse("the live preview changed after the tap", snapshot(screen).contentEquals(before))
        } finally {
            Disposer.dispose(panel)
            forward.close()
        }
    }

    fun testTheSimulatorStreamsItsScreen() {
        if (destination == null) return
        val shell = MacHosts.shell()!!
        val simulators = Simulators(shell).list()
        assertTrue("the Mac has simulators", simulators.isNotEmpty())
        val device = simulators.first { it.booted }
        SimulatorStream.start(shell, device.udid).use { stream ->
            val socket = (0 until 100).firstNotNullOfOrNull {
                runCatching { Socket("127.0.0.1", stream.port) }.getOrElse { Thread.sleep(100); null }
            }
            assertNotNull("the stream accepts a viewer", socket)
            socket!!.use {
                it.soTimeout = 15_000
                val input = DataInputStream(it.getInputStream())
                val types = (0 until 2).map {
                    val type = input.readUnsignedByte()
                    input.readFully(ByteArray(input.readInt()))
                    type
                }
                assertEquals("info, then a frame", listOf(2, 1), types)
            }
        }
        val controls = Simulators(shell)
        assertTrue("a screenshot is a PNG", controls.screenshot(device.udid).let { it.size > 1000 && it[1] == 'P'.code.toByte() })
        assertTrue("shake", controls.shake(device.udid).success)
        val dark = controls.isDark(device.udid)
        controls.setDark(device.udid, !dark)
        assertEquals("the appearance changed", !dark, controls.isDark(device.udid))
        controls.setDark(device.udid, dark)
    }

    private fun snapshot(component: javax.swing.JComponent): IntArray {
        val image = java.awt.image.BufferedImage(component.width, component.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        component.paint(image.graphics)
        return image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 20_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(20)
        }
    }
}

/** The *iOS App* run configuration against a remote Mac: builds the sample with Xcode there and runs it. */
class RemoteIosRunTest : BasePlatformTestCase() {
    private val destination: String? = System.getProperty("csui.e2e.remote")
    private val moduleDir: String? = System.getProperty("csui.e2e.remote.module")

    fun testTheAppBuildsInstallsAndRunsUntilStopped() {
        if (destination == null) return
        val dir = moduleDir ?: return
        PreviewSettings.options.remoteMac = destination
        PreviewSettings.options.remoteMacOnMac = true
        val type = IosRunConfigurationType()
        // Another app with -Dcsui.e2e.remote.xcode=<.xcodeproj>, .scheme and .bundle; the sample's by default.
        val bundle = System.getProperty("csui.e2e.remote.bundle") ?: "dev.nucleusframework.sample.iosApp"
        val configuration = IosRunConfiguration(project, type.configurationFactories[0], "iOS").apply {
            options.xcodeProject = System.getProperty("csui.e2e.remote.xcode") ?: File(File(dir).parent, "iosApp/iosApp.xcodeproj").path
            options.scheme = System.getProperty("csui.e2e.remote.scheme") ?: "iosApp"
        }
        val executor = com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance()
        val environment = com.intellij.execution.runners.ExecutionEnvironmentBuilder.create(project, executor, configuration).build()
        val state = configuration.getState(executor, environment) as IosRunState
        val runner = com.intellij.execution.runners.ProgramRunner.getRunner(executor.id, configuration)!!
        val result = state.execute(executor, runner)
        val handler = result.processHandler
        val output = StringBuffer()
        handler.addProcessListener(object : com.intellij.execution.process.ProcessListener {
            override fun onTextAvailable(event: com.intellij.execution.process.ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                output.append(event.text)
            }
        })
        handler.startNotify()
        try {
            val deadline = System.currentTimeMillis() + 20 * 60_000
            while ("Installing" !in output && !handler.isProcessTerminated && System.currentTimeMillis() < deadline) {
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
                Thread.sleep(200)
            }
            assertTrue("the app is built and installed:\n${output.takeLast(4000)}", "Installing" in output)
            // Launched: the console reads the app's output until Stop.
            Thread.sleep(5000)
            assertFalse("the app runs:\n${output.takeLast(2000)}", handler.isProcessTerminated)
        } finally {
            handler.destroyProcess()
            handler.waitFor(30_000)
        }
        assertTrue("Stop ends the run", handler.isProcessTerminated)
        val shell = MacHosts.shell()!!
        val running = shell.run("xcrun simctl spawn booted launchctl list | grep -c ${quote(bundle)} || true").output.trim()
        assertEquals("Stop terminates the app on the simulator", "0", running.lines().last())
    }
}

/** The *iOS Simulator* tool window's panel against a remote Mac: streams the screen, and a click is a tap. */
class RemoteSimulatorPanelTest : BasePlatformTestCase() {
    private val destination: String? = System.getProperty("csui.e2e.remote")

    fun testThePanelShowsTheSimulatorAndAClickTaps() {
        if (destination == null) return
        PreviewSettings.options.remoteMac = destination
        PreviewSettings.options.remoteMacOnMac = true
        val panel = SimulatorPanel()
        try {
            panel.setSize(520, 1000)
            panel.doLayout()
            waitFor(60_000) { panel.listed.any { it.booted } }
            panel.select(panel.listed.first { it.booted && it.name == "iPhone 18 Pro" })
            panel.connect()
            val screen = panel.screenComponent
            waitFor(60_000) { panel.statusText == "iPhone 18 Pro" }
            Thread.sleep(1500)
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            screen.setSize(520, 950)
            // From the home screen, whatever app the simulator shows.
            panel.press("home")
            Thread.sleep(2000)
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            val dump = System.getProperty("java.io.tmpdir")
            val before = snapshot(screen, File(dump, "simulator-panel-before.png"))
            // Settings ("Réglages") sits at about (339, 421) points of the 402 × 874 point screen.
            val scale = minOf((screen.width - 24.0) / 402, (screen.height - 24.0) / 874)
            val x = ((screen.width - 402 * scale) / 2 + 339 * scale).toInt()
            val y = ((screen.height - 874 * scale) / 2 + 421 * scale).toInt()
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                screen.dispatchEvent(MouseEvent(screen, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1))
            }
            Thread.sleep(2500)
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            val after = snapshot(screen, File(dump, "simulator-panel-after.png"))
            assertFalse("the tap changed the screen", before.contentEquals(after))
            panel.press("home")
            Thread.sleep(2500)
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            val home = snapshot(screen, File(dump, "simulator-panel-home.png"))
            assertFalse("Home left the app", home.contentEquals(after))
        } finally {
            Disposer.dispose(panel)
        }
    }

    private fun snapshot(component: javax.swing.JComponent, file: File): IntArray {
        val image = java.awt.image.BufferedImage(component.width, component.height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        component.paint(image.graphics)
        javax.imageio.ImageIO.write(image, "png", file)
        return image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    }

    private fun waitFor(milliseconds: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + milliseconds
        while (!condition() && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(50)
        }
    }
}
