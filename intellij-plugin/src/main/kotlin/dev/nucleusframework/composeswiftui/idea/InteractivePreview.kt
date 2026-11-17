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
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.OutputStream
import java.net.Socket
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.Timer

/**
 * Interactive mode, like Android Studio's: the preview runs live on the simulator and streams here. The mouse taps,
 * drags and scrolls, the keyboard types. *Select Component* goes to the code of a view, *Slow Animations* slows them
 * down, and *Record* captures the frames of an animation to replay them frame by frame (the animation inspector).
 */
class InteractivePreview(
    private val title: String,
    private val navigator: SourceNavigator,
    private val onClose: () -> Unit,
) : JPanel(BorderLayout()), Disposable {
    private val status = JBLabel("Starting the live preview…").apply {
        foreground = UIUtil.getContextHelpForeground()
        border = JBUI.Borders.emptyRight(8)
    }
    private val screen = LiveScreen()

    /** The live screen, for tests. */
    internal val liveScreen: JComponent get() = screen
    internal val liveStatus: String get() = status.text
    private val timeline = Timeline()

    private var socket: Socket? = null
    private var output: OutputStream? = null

    /** Size of the preview in points, from the host's `info`. */
    private var pointsWidth = 0.0
    private var pointsHeight = 0.0

    private var selecting = false
    private var slowMotion = false
    private var recording = false
    private val recorded = ArrayList<Pair<Long, BufferedImage>>()
    private var viewLayout: PreviewLayoutData? = null
    private var hovered: Pair<LayoutNode, ResolvedCall>? = null
    private var startedAt = 0L

    init {
        val actions = DefaultActionGroup(
            action("Stop Interactive Mode", AllIcons.Actions.Suspend) { close() },
            Separator.getInstance(),
            toggle("Select Component (click a view to go to its code)", AllIcons.General.Locate, { selecting }) {
                selecting = it
                hovered = null
                if (it) send(JsonObject().apply { addProperty("t", "inspect") })
                screen.repaint()
            },
            toggle("Slow Animations", AllIcons.Actions.StepOut, { slowMotion }) {
                slowMotion = it
                send(JsonObject().apply {
                    addProperty("t", "speed")
                    addProperty("v", if (it) 0.1 else 1.0)
                })
            },
            toggle("Record Animation", AllIcons.Ide.Macro.Recording_1, { recording }) { start ->
                if (start) {
                    recorded.clear()
                    recording = true
                } else {
                    recording = false
                    timeline.show(recorded.toList())
                }
            },
        )
        val toolbar = ActionManager.getInstance().createActionToolbar("ComposeSwiftUIInteractive", actions, true)
        toolbar.targetComponent = this
        val header = JPanel(BorderLayout()).apply {
            add(JBLabel("Interactive: $title").apply {
                font = JBUI.Fonts.label().asBold()
                border = JBUI.Borders.empty(0, 8)
            }, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.CENTER)
            add(status, BorderLayout.EAST)
            border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())
        }
        add(header, BorderLayout.NORTH)
        add(screen, BorderLayout.CENTER)
        add(timeline, BorderLayout.SOUTH)
    }

    /** Connects to the live preview on [port] (retrying while the host starts it) and streams it until closed. */
    fun connect(port: Int) {
        startedAt = System.nanoTime()
        ApplicationManager.getApplication().executeOnPooledThread {
            val socket = (0 until 100).firstNotNullOfOrNull {
                runCatching { Socket("127.0.0.1", port) }.getOrElse { Thread.sleep(50); null }
            }
            if (socket == null) {
                onEdt { status.text = "The live preview didn't start" }
                return@executeOnPooledThread
            }
            this.socket = socket
            output = socket.getOutputStream()
            onEdt { status.text = "Live" }
            read(DataInputStream(socket.getInputStream().buffered()))
        }
    }

    private fun read(input: DataInputStream) {
        try {
            while (true) {
                val type = input.readUnsignedByte()
                val payload = ByteArray(input.readInt()).also(input::readFully)
                when (type) {
                    1 -> ImageIO.read(ByteArrayInputStream(payload))?.let { frame ->
                        onEdt {
                            screen.frame = frame
                            if (recording && recorded.size < MAX_RECORDED) recorded += System.nanoTime() to frame
                            if (screen.frames == 0) status.text = "Live · first frame in ${(System.nanoTime() - startedAt) / 1_000_000} ms"
                            screen.frames++
                        }
                    }
                    2 -> message(JsonParser.parseString(String(payload)).asJsonObject)
                }
            }
        } catch (_: Exception) {
            onEdt { if (socket != null) status.text = "Disconnected" }
        }
    }

    private fun message(json: JsonObject) {
        when (json.get("t")?.asString) {
            "info" -> onEdt {
                pointsWidth = json.get("width").asDouble
                pointsHeight = json.get("height").asDouble
                if (json.get("touches")?.asBoolean == false) status.text = "Live (touches unavailable on this simulator)"
            }
            "layout" -> {
                val parsed = PreviewLayoutData.parse(json.getAsJsonObject("layout"))
                navigator.prefetch(listOf(parsed))
                onEdt { viewLayout = parsed }
            }
        }
    }

    fun failed(message: String) {
        status.text = message
    }

    /** Sends the live literal edits of the code to the running preview. */
    fun patch(edits: List<LiteralEdit>) {
        send(JsonObject().apply {
            addProperty("t", "patches")
            add("patches", com.google.gson.JsonArray().apply { edits.forEach { add(it.toJson()) } })
        })
    }

    private fun send(command: JsonObject) {
        val output = output ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            runCatching { synchronized(output) { output.write((command.toString() + "\n").toByteArray()); output.flush() } }
        }
    }

    private fun close() {
        send(JsonObject().apply { addProperty("t", "stop") })
        Disposer.dispose(this)
        onClose()
    }

    override fun dispose() {
        val socket = socket
        this.socket = null
        runCatching { socket?.close() }
        timeline.stop()
    }

    private fun onEdt(block: () -> Unit) = ApplicationManager.getApplication().invokeLater(block)

    /** The live preview, scaled to fit; mouse and keyboard go to the simulator. */
    private inner class LiveScreen : JComponent() {
        var frame: BufferedImage? = null
            set(value) {
                field = value
                if (!timeline.isScrubbing) repaint()
            }
        var frames = 0
        private var dragging = false
        private var lastMove = 0L

        init {
            isFocusable = true
            val mouse = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    requestFocusInWindow()
                    if (selecting || timeline.isScrubbing) return
                    val (x, y) = points(e) ?: return
                    dragging = true
                    touch("down", x, y)
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (!dragging) return
                    val now = System.nanoTime()
                    if (now - lastMove < 8_000_000) return
                    lastMove = now
                    val (x, y) = points(e) ?: return
                    touch("move", x, y)
                }

                override fun mouseReleased(e: MouseEvent) {
                    if (!dragging) return
                    dragging = false
                    val (x, y) = points(e) ?: return
                    touch("up", x, y)
                }

                override fun mouseClicked(e: MouseEvent) {
                    if (!selecting) return
                    hovered?.let { navigator.navigate(it.second, System.currentTimeMillis(), focus = e.clickCount >= 2) }
                    // The views may have moved since: ask again.
                    send(JsonObject().apply { addProperty("t", "inspect") })
                }

                override fun mouseMoved(e: MouseEvent) {
                    if (!selecting) return
                    val (x, y) = points(e) ?: return
                    val found = viewLayout?.nodeAt(x, y, navigator)
                    if (found?.first != hovered?.first) {
                        hovered = found
                        toolTipText = found?.second?.label
                        repaint()
                    }
                }

                override fun mouseWheelMoved(e: MouseWheelEvent) {
                    val (x, y) = points(e) ?: return
                    val delta = e.preciseWheelRotation * 24
                    send(JsonObject().apply {
                        addProperty("t", "scroll")
                        addProperty("x", x)
                        addProperty("y", y)
                        addProperty("dx", if (e.isShiftDown) delta else 0.0)
                        addProperty("dy", if (e.isShiftDown) 0.0 else delta)
                    })
                }
            }
            addMouseListener(mouse)
            addMouseMotionListener(mouse)
            addMouseWheelListener(mouse)
            addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    val c = e.keyChar
                    if (c == KeyEvent.CHAR_UNDEFINED || c == '\b' || c.code == 127 || (e.isMetaDown || e.isControlDown)) return
                    send(JsonObject().apply {
                        addProperty("t", "text")
                        addProperty("s", if (c == '\r') "\n" else c.toString())
                    })
                }

                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_BACK_SPACE) send(JsonObject().apply { addProperty("t", "backspace") })
                }
            })
        }

        private fun touch(type: String, x: Double, y: Double) = send(JsonObject().apply {
            addProperty("t", type)
            addProperty("x", x)
            addProperty("y", y)
        })

        /** Where the preview is drawn: scale (pixels per point) and origin. */
        private fun placement(): Triple<Double, Int, Int>? {
            if (pointsWidth <= 0 || pointsHeight <= 0) return null
            val inset = JBUI.scale(12)
            val scale = minOf((width - 2 * inset) / pointsWidth, (height - 2 * inset) / pointsHeight, 2.0).coerceAtLeast(0.05)
            val x = ((width - pointsWidth * scale) / 2).toInt()
            return Triple(scale, x, inset)
        }

        private fun points(e: MouseEvent): Pair<Double, Double>? {
            val (scale, x0, y0) = placement() ?: return null
            val x = (e.x - x0) / scale
            val y = (e.y - y0) / scale
            return if (x in 0.0..pointsWidth && y in 0.0..pointsHeight) x to y else null
        }

        override fun paintComponent(g: Graphics) {
            val image = timeline.current ?: frame ?: return
            val (scale, x0, y0) = placement() ?: return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                val w = (pointsWidth * scale).toInt()
                val h = (pointsHeight * scale).toInt()
                g2.drawImage(image, x0, y0, w, h, null)
                g2.color = JBUI.CurrentTheme.ToolWindow.borderColor()
                g2.drawRect(x0, y0, w - 1, h - 1)
                if (selecting) {
                    hovered?.first?.scaled(scale)?.let { r ->
                        g2.color = JBUI.CurrentTheme.Focus.focusColor()
                        g2.stroke = BasicStroke(2f)
                        g2.drawRect(x0 + r.x, y0 + r.y, r.width, r.height)
                    }
                }
            } finally {
                g2.dispose()
            }
        }
    }

    /**
     * The recorded animation: a slider over its frames, play (at the recorded pace) and step buttons. While it shows,
     * the screen shows the chosen frame instead of the live one.
     */
    private inner class Timeline : JPanel(BorderLayout()) {
        private var frames: List<Pair<Long, BufferedImage>> = emptyList()
        private val slider = JSlider(0, 0, 0)
        private val label = JBLabel("", SwingConstants.CENTER).apply { foreground = UIUtil.getContextHelpForeground() }
        private var player: Timer? = null

        val isScrubbing get() = isVisible && frames.isNotEmpty()
        val current: BufferedImage? get() = if (isScrubbing) frames.getOrNull(slider.value)?.second else null

        init {
            isVisible = false
            border = JBUI.Borders.compound(JBUI.Borders.customLineTop(JBColor.border()), JBUI.Borders.empty(6, 8))
            slider.addChangeListener {
                updateLabel()
                screen.repaint()
            }
            val buttons = JPanel().apply {
                isOpaque = false
                add(button(AllIcons.Actions.Back, "Previous frame") { step(-1) })
                add(button(AllIcons.Actions.Execute, "Play") { play() })
                add(button(AllIcons.Actions.Forward, "Next frame") { step(1) })
                add(button(AllIcons.Actions.Close, "Back to the live preview") { hideTimeline() })
            }
            add(buttons, BorderLayout.WEST)
            add(slider, BorderLayout.CENTER)
            add(label, BorderLayout.EAST)
        }

        fun show(recorded: List<Pair<Long, BufferedImage>>) {
            frames = recorded
            if (frames.isEmpty()) {
                status.text = "Nothing changed while recording"
                return
            }
            slider.maximum = frames.size - 1
            slider.value = 0
            isVisible = true
            updateLabel()
            revalidate()
        }

        private fun updateLabel() {
            val start = frames.firstOrNull()?.first ?: return
            val at = frames.getOrNull(slider.value)?.first ?: return
            label.text = "Frame ${slider.value + 1} of ${frames.size} · %.0f ms".format((at - start) / 1e6)
        }

        private fun step(delta: Int) {
            stop()
            slider.value = (slider.value + delta).coerceIn(0, slider.maximum)
        }

        /** Replays from the current frame at the pace it was recorded. */
        private fun play() {
            stop()
            if (slider.value >= slider.maximum) slider.value = 0
            val timer = Timer(0, null)
            timer.addActionListener {
                if (slider.value >= slider.maximum) {
                    timer.stop()
                    return@addActionListener
                }
                val now = frames[slider.value].first
                slider.value += 1
                timer.initialDelay = ((frames[slider.value].first - now) / 1_000_000).toInt().coerceIn(1, 1000)
                timer.restart()
            }
            timer.isRepeats = false
            timer.start()
            player = timer
        }

        fun stop() {
            player?.stop()
            player = null
        }

        private fun hideTimeline() {
            stop()
            isVisible = false
            frames = emptyList()
            screen.repaint()
            revalidate()
        }

        private fun button(icon: javax.swing.Icon, tooltip: String, run: () -> Unit) = JButton(icon).apply {
            toolTipText = tooltip
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener { run() }
        }
    }

    private fun toggle(text: String, icon: javax.swing.Icon, selected: () -> Boolean, set: (Boolean) -> Unit) =
        object : DumbAwareToggleAction(text, null, icon) {
            override fun isSelected(e: AnActionEvent) = selected()
            override fun setSelected(e: AnActionEvent, state: Boolean) = set(state)
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }

    private companion object {
        /** About 20 s of animation at 60 frames per second. */
        const val MAX_RECORDED = 1200
    }
}
