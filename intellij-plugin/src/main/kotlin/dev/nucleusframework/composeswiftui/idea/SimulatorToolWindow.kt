package dev.nucleusframework.composeswiftui.idea

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.nucleusframework.composeswiftui.idea.remote.MacHosts
import dev.nucleusframework.composeswiftui.idea.remote.MacShell
import dev.nucleusframework.composeswiftui.idea.remote.Simulator
import dev.nucleusframework.composeswiftui.idea.remote.SimulatorStream
import dev.nucleusframework.composeswiftui.idea.remote.Simulators
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.Socket
import javax.imageio.ImageIO
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/** The *iOS Simulator* tool window: a simulator of the Mac (this one or the remote one), live in the IDE. */
class SimulatorToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimulatorPanel()
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        Disposer.register(content, panel)
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Picks a simulator, boots it, and shows its screen: the mouse touches, drags and scrolls, the keyboard types. The
 * device's controls sit next to the screen: Home, the app switcher, rotation, a screenshot, a shake, light or dark
 * appearance.
 * Like Android Studio's *Running Devices*.
 */
class SimulatorPanel : JPanel(BorderLayout()), Disposable {
    private val devices = ComboBox<Simulator>(DefaultComboBoxModel()).apply { prototypeDisplayValue = null }
    private val status = JBLabel().apply {
        foreground = UIUtil.getContextHelpForeground()
        border = JBUI.Borders.empty(0, 8)
    }
    private val screen = Screen()

    /** For tests: the screen component, the status text, the simulators listed. */
    internal val screenComponent: JComponent get() = screen
    internal val statusText: String get() = status.text
    internal val listed: List<Simulator> get() = (0 until devices.itemCount).map { devices.getItemAt(it) }
    internal fun select(device: Simulator) {
        devices.selectedItem = device
    }
    internal fun press(name: String) = button(name)

    @Volatile private var stream: SimulatorStream? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var busy = false
    @Volatile private var dark = false
    @Volatile private var device: Simulator? = null
    /** Screen ID of the shown screen, from the stream: the one to capture on a device with several. */
    @Volatile private var display: Int? = null

    init {
        val actions = DefaultActionGroup(
            action("Refresh Simulators", AllIcons.Actions.Refresh) { refresh() },
            action("Show the Simulator (boots it when needed)", AllIcons.Actions.Execute, { !busy && devices.selectedItem != null }) { connect() },
            action("Stop Showing", AllIcons.Actions.Suspend, { stream != null }) { disconnect("Stopped") },
            Separator.getInstance(),
            action("Shut Down the Simulator", AllIcons.Actions.Exit, { !busy && (devices.selectedItem as? Simulator)?.booted == true }) { shutdown() },
        )
        val toolbar = ActionManager.getInstance().createActionToolbar("ComposeSwiftUISimulator", actions, true)
        toolbar.targetComponent = this
        val header = JPanel(BorderLayout()).apply {
            add(devices, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.CENTER)
            add(status, BorderLayout.EAST)
            border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())
        }
        // The device's own controls, beside its screen.
        val live = { socket != null }
        val controls = DefaultActionGroup(
            action("Home", AllIcons.Nodes.HomeFolder, live) { button("home") },
            action("App Switcher", AllIcons.Actions.ListFiles, live) { button("appSwitcher") },
            Separator.getInstance(),
            action("Rotate Left", AllIcons.Actions.Undo, live) { onDevice("Rotated left") { simulators, udid -> simulators.rotate(udid, left = true) } },
            action("Rotate Right", AllIcons.Actions.Redo, live) { onDevice("Rotated right") { simulators, udid -> simulators.rotate(udid, left = false) } },
            Separator.getInstance(),
            action("Screenshot (copied, and saved where you choose)", AllIcons.Actions.Download, live) { screenshot() },
            action("Shake", AllIcons.Actions.ShortcutFilter, live) { onDevice("Shaking…") { simulators, udid -> simulators.shake(udid) } },
            object : DumbAwareToggleAction("Dark Appearance", null, AllIcons.Actions.ToggleVisibility) {
                override fun isSelected(e: AnActionEvent) = dark
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    dark = state
                    onDevice(if (state) "Dark appearance" else "Light appearance") { simulators, udid -> simulators.setDark(udid, state) }
                }
                override fun update(e: AnActionEvent) {
                    super.update(e)
                    e.presentation.isEnabled = live()
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            },
        )
        val side = ActionManager.getInstance().createActionToolbar("ComposeSwiftUISimulatorControls", controls, false)
        side.targetComponent = this
        side.component.border = JBUI.Borders.customLineLeft(JBUI.CurrentTheme.ToolWindow.borderColor())
        add(side.component, BorderLayout.EAST)
        add(header, BorderLayout.NORTH)
        add(screen, BorderLayout.CENTER)
        // Like Running Devices: a simulator already running shows at once.
        refresh(connectToBooted = true)
    }

    private fun shell(): MacShell? = MacHosts.shell().also { if (it == null) setStatus(MacHosts.NO_MAC) }

    private fun background(label: String, work: (MacShell) -> Unit) {
        val shell = shell() ?: return
        busy = true
        setStatus(label)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                work(shell)
            } catch (e: Exception) {
                setStatus(e.message ?: e.toString())
            } finally {
                busy = false
            }
        }
    }

    private fun refresh(select: String? = null, connectToBooted: Boolean = false) = background("Listing the simulators…") { shell ->
        val list = Simulators(shell).list()
        SwingUtilities.invokeLater {
            val current = select ?: (devices.selectedItem as? Simulator)?.udid
            devices.model = DefaultComboBoxModel(list.toTypedArray())
            (list.firstOrNull { it.udid == current } ?: list.firstOrNull())?.let { devices.selectedItem = it }
            if (stream == null) status.text = if (list.isEmpty()) "No iOS simulator" else ""
            // The list is sorted booted first.
            if (connectToBooted && stream == null && list.firstOrNull()?.booted == true) connect()
        }
    }

    internal fun connect() {
        val device = devices.selectedItem as? Simulator ?: return
        disconnect(null)
        background("Starting ${device.name}…") { shell ->
            if (!device.booted) {
                val boot = Simulators(shell).boot(device.udid)
                if (!boot.success) return@background setStatus("Couldn't boot ${device.name}: ${boot.output.trim().lines().lastOrNull()}")
                refresh(device.udid)
            }
            setStatus("Connecting to ${device.name}…")
            val stream = SimulatorStream.start(shell, device.udid)
            this.stream = stream
            val socket = (0 until 100).firstNotNullOfOrNull {
                if (!stream.isAlive) return@firstNotNullOfOrNull null
                runCatching { Socket("127.0.0.1", stream.port) }.getOrElse { Thread.sleep(100); null }
            }
            if (socket == null) {
                stream.close()
                this.stream = null
                return@background setStatus("Couldn't connect to the simulator stream")
            }
            socket.tcpNoDelay = true
            this.socket = socket
            output = socket.getOutputStream()
            this.device = device
            dark = Simulators(shell).isDark(device.udid)
            setStatus(device.name)
            ApplicationManager.getApplication().executeOnPooledThread { read(socket, DataInputStream(socket.getInputStream().buffered())) }
            SwingUtilities.invokeLater { screen.requestFocusInWindow() }
        }
    }

    private fun read(socket: Socket, input: DataInputStream) {
        try {
            while (true) {
                val type = input.readUnsignedByte()
                val payload = ByteArray(input.readInt()).also(input::readFully)
                when (type) {
                    1 -> ImageIO.read(ByteArrayInputStream(payload))?.let { frame -> SwingUtilities.invokeLater { screen.frame = frame } }
                    2 -> {
                        val json = JsonParser.parseString(String(payload)).asJsonObject
                        if (json.get("t")?.asString == "info") SwingUtilities.invokeLater {
                            display = json.get("screen")?.asInt
                            screen.pointsWidth = json.get("width").asDouble
                            screen.pointsHeight = json.get("height").asDouble
                            // A foldable (iPhone Duo) takes touches through CoreDevice, which the stream can't reach yet.
                            if (json.get("touches")?.asBoolean == false) {
                                status.text = "${device?.name.orEmpty()}: view only (touches aren't supported on this device yet)"
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            if (this.socket === socket) disconnect("Disconnected")
        }
    }

    private fun disconnect(message: String?) {
        val socket = socket
        val stream = stream
        this.socket = null
        this.stream = null
        output = null
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching { socket?.close() }
            stream?.close()
        }
        SwingUtilities.invokeLater {
            screen.frame = null
            if (message != null) status.text = message
        }
    }

    private fun shutdown() {
        val device = devices.selectedItem as? Simulator ?: return
        disconnect(null)
        background("Shutting down ${device.name}…") { shell ->
            Simulators(shell).shutdown(device.udid)
            refresh(device.udid)
        }
    }

    /** Runs [work] on the shown simulator, off the UI thread. */
    private fun onDevice(label: String, work: (Simulators, String) -> Unit) {
        val shell = shell() ?: return
        val udid = device?.udid ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching { work(Simulators(shell), udid) }.onFailure { setStatus(it.message ?: it.toString()) }
            setStatus(label)
        }
    }

    /** Copies the screen at full resolution, and offers to save it. */
    private fun screenshot() {
        val shell = shell() ?: return
        val device = device ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val png = runCatching { Simulators(shell).screenshot(device.udid, display) }.getOrElse { return@executeOnPooledThread setStatus(it.message ?: "") }
            val image = ImageIO.read(ByteArrayInputStream(png)) ?: return@executeOnPooledThread setStatus("The screenshot is empty")
            SwingUtilities.invokeLater {
                PreviewImages.copyImage(image)
                status.text = "Screenshot copied"
                val name = "${device.name} ${java.time.LocalDateTime.now().withNano(0)}.png".replace(':', '-')
                val target = FileChooserFactory.getInstance()
                    .createSaveFileDialog(FileSaverDescriptor("Save Screenshot", "", "png"), this)
                    .save(name) ?: return@invokeLater
                target.file.writeBytes(png)
                status.text = "Saved ${target.file.name}"
            }
        }
    }

    private fun button(name: String) = send(JsonObject().apply {
        addProperty("t", "button")
        addProperty("name", name)
    })

    private fun send(command: JsonObject) {
        val output = output ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching { synchronized(output) { output.write((command.toString() + "\n").toByteArray()); output.flush() } }
        }
    }

    private fun setStatus(text: String) = SwingUtilities.invokeLater { status.text = text }

    override fun dispose() = disconnect(null)

    private fun action(text: String, icon: javax.swing.Icon, enabled: () -> Boolean = { true }, run: () -> Unit) =
        object : DumbAwareAction(text, null, icon) {
            override fun actionPerformed(e: AnActionEvent) = run()
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = enabled()
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }

    /** The simulator's screen, scaled to fit, with its rounded corners. */
    private inner class Screen : JComponent() {
        var frame: BufferedImage? = null
            set(value) {
                field = value
                repaint()
            }
        var pointsWidth = 0.0
        var pointsHeight = 0.0

        /** Wheel deltas gathered between two scroll gestures sent to the simulator. */
        private var wheelX = 0.0
        private var wheelY = 0.0
        private var wheelAt: Pair<Double, Double>? = null
        private val wheelTimer = Timer(60) { flushWheel() }.apply { isRepeats = false }

        init {
            isFocusable = true
            val mouse = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    requestFocusInWindow()
                    points(e)?.let { (x, y) -> touch("down", x, y) }
                }

                override fun mouseDragged(e: MouseEvent) {
                    points(e, clamp = true)?.let { (x, y) -> touch("move", x, y) }
                }

                override fun mouseReleased(e: MouseEvent) {
                    points(e, clamp = true)?.let { (x, y) -> touch("up", x, y) }
                }

                override fun mouseWheelMoved(e: MouseWheelEvent) {
                    val point = points(e) ?: return
                    val delta = e.preciseWheelRotation * 40
                    if (e.isShiftDown) wheelX += delta else wheelY += delta
                    if (wheelAt == null) wheelAt = point
                    wheelTimer.restart()
                }
            }
            addMouseListener(mouse)
            addMouseMotionListener(mouse)
            addMouseWheelListener(mouse)
            addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    val c = e.keyChar
                    if (c == KeyEvent.CHAR_UNDEFINED || c == '\b' || c.code == 127 || e.isMetaDown || e.isControlDown) return
                    send(JsonObject().apply {
                        addProperty("t", "text")
                        addProperty("s", if (c == '\r') "\n" else c.toString())
                    })
                }

                override fun keyPressed(e: KeyEvent) {
                    val usage = when (e.keyCode) {
                        KeyEvent.VK_BACK_SPACE -> 0x2A
                        KeyEvent.VK_ESCAPE -> 0x29
                        KeyEvent.VK_RIGHT -> 0x4F
                        KeyEvent.VK_LEFT -> 0x50
                        KeyEvent.VK_DOWN -> 0x51
                        KeyEvent.VK_UP -> 0x52
                        KeyEvent.VK_DELETE -> 0x4C
                        else -> return
                    }
                    send(JsonObject().apply {
                        addProperty("t", "key")
                        addProperty("usage", usage)
                    })
                }
            })
        }

        private fun touch(type: String, x: Double, y: Double) = send(JsonObject().apply {
            addProperty("t", type)
            addProperty("x", x)
            addProperty("y", y)
        })

        private fun flushWheel() {
            val (x, y) = wheelAt ?: return
            // The finger can't leave the screen: a long wheel run becomes a shorter swipe.
            val dx = wheelX.coerceIn(-pointsWidth / 2, pointsWidth / 2)
            val dy = wheelY.coerceIn(-pointsHeight / 2, pointsHeight / 2)
            wheelX = 0.0
            wheelY = 0.0
            wheelAt = null
            send(JsonObject().apply {
                addProperty("t", "scroll")
                addProperty("x", x.coerceIn(dx.coerceAtLeast(0.0) + 1, pointsWidth + dx.coerceAtMost(0.0) - 1))
                addProperty("y", y.coerceIn(dy.coerceAtLeast(0.0) + 1, pointsHeight + dy.coerceAtMost(0.0) - 1))
                addProperty("dx", dx)
                addProperty("dy", dy)
            })
        }

        /** Scale (pixels of this component per point) and origin of the drawn screen. */
        private fun placement(): Triple<Double, Double, Double>? {
            if (pointsWidth <= 0 || pointsHeight <= 0) return null
            val margin = JBUI.scale(12).toDouble()
            val scale = minOf((width - 2 * margin) / pointsWidth, (height - 2 * margin) / pointsHeight)
            if (scale <= 0) return null
            return Triple(scale, (width - pointsWidth * scale) / 2, (height - pointsHeight * scale) / 2)
        }

        private fun points(e: MouseEvent, clamp: Boolean = false): Pair<Double, Double>? {
            val (scale, x0, y0) = placement() ?: return null
            val x = (e.x - x0) / scale
            val y = (e.y - y0) / scale
            if (clamp) return x.coerceIn(0.0, pointsWidth) to y.coerceIn(0.0, pointsHeight)
            if (x < 0 || y < 0 || x > pointsWidth || y > pointsHeight) return null
            return x to y
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val frame = frame
            val placement = placement()
            if (frame == null || placement == null) {
                g.color = UIUtil.getContextHelpForeground()
                val text = if (socket == null) "Pick a simulator and press ▶" else "Waiting for the screen…"
                val metrics = g.fontMetrics
                g.drawString(text, (width - metrics.stringWidth(text)) / 2, height / 2)
                return
            }
            val (scale, x0, y0) = placement
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = pointsWidth * scale
            val h = pointsHeight * scale
            // Rounded like the device's display.
            val radius = 55 * scale
            g2.clip(RoundRectangle2D.Double(x0, y0, w, h, radius, radius))
            g2.drawImage(frame, x0.toInt(), y0.toInt(), w.toInt(), h.toInt(), null)
            g2.dispose()
        }
    }
}
