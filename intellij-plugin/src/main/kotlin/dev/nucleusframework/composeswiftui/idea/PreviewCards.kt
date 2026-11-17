package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.TextTransferable
import com.intellij.util.ui.UIUtil
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/** One preview variant shown in the pane: rendered, failed, or not rendered yet. */
class PreviewEntry(
    val key: String,
    val id: String,
    val title: String,
    val group: String,
    val subtitle: String,
    val image: BufferedImage?,
    val imageFile: File?,
    val error: String?,
    /** Has a result of the last renders (an image or an error). */
    val rendered: Boolean,
    /** Size in points, 0 when unknown. */
    val width: Int,
    val height: Int,
    /** Its views and their code, for click-to-code. */
    val layout: PreviewLayoutData? = null,
) {
    companion object {
        /** Entries of the previews [ids] of a file, in source order, from the last renders of [module]. */
        fun load(module: PreviewModule?, ids: List<String>, titles: Map<String, String>, file: VirtualFile): List<PreviewEntry> {
            val results = module?.let { PreviewFiles.results(it) }.orEmpty().groupBy { it.id }
            val groups = module?.let { PreviewFiles.manifestGroups(it) }.orEmpty()
            return ids.flatMap { id ->
                val rendered = results[id]?.sortedWith(compareBy({ it.variantIndex() }, { it.parameterIndex() }))
                if (rendered.isNullOrEmpty()) {
                    val title = titles[id] ?: id.substringAfterLast('.')
                    val group = groups.entries.firstOrNull { it.key.substringBefore('~') == id }?.value.orEmpty()
                    listOf(PreviewEntry(id, id, title, group, "Not rendered yet", null, null, null, rendered = false, width = 0, height = 0))
                } else {
                    rendered.map { result ->
                        val imageFile = result.file?.let { File(module!!.previewsDir, it) }
                        val subtitle = listOf(
                            result.group,
                            result.device,
                            if (result.width > 0) "${result.width}×${result.height} pt" else "",
                            if (result.error == null && result.milliseconds > 0) "${result.milliseconds} ms" else "",
                        ).filter { it.isNotEmpty() }.joinToString(" · ")
                        PreviewEntry(
                            key = result.key,
                            id = id,
                            title = result.title.ifEmpty { titles[id] ?: id },
                            group = result.group,
                            subtitle = subtitle,
                            image = imageFile?.let(PreviewFiles::image),
                            imageFile = imageFile,
                            error = result.error,
                            rendered = true,
                            width = result.width,
                            height = result.height,
                            layout = PreviewLayoutData.read(File(module!!.previewsDir, "${result.key}.layout.json")),
                        )
                    }
                }
            }
        }

        /** The file changed since its previews were rendered (unsaved changes, or saved after the images). */
        fun isOutOfDate(entries: List<PreviewEntry>, file: VirtualFile): Boolean {
            val images = entries.mapNotNull { it.imageFile?.takeIf(File::isFile) }
            if (images.isEmpty()) return false
            return FileDocumentManager.getInstance().isFileModified(file) || images.any { it.lastModified() < file.timeStamp }
        }

        private fun RenderedPreview.variantIndex() = key.substringAfter('~', "0").substringBefore('#').toIntOrNull() ?: 0
        private fun RenderedPreview.parameterIndex() = key.substringAfter('#', "0").toIntOrNull() ?: 0
    }
}

/** A preview in the pane: title, details, then the image, the error or a placeholder. */
class PreviewCard(
    val entry: PreviewEntry,
    selected: Boolean,
    private val navigator: SourceNavigator?,
    private val actions: () -> ActionGroup,
    private val onSelect: (navigate: Boolean) -> Unit,
) : JPanel(VerticalLayout(JBUI.scale(4))) {
    var scale = 1.0
        private set

    private val content: JComponent = when {
        entry.image != null -> PreviewImageView(entry.image, entry.layout, navigator)
        entry.error != null -> JBTextArea(entry.error.lines().take(30).joinToString("\n")).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = false
            font = JBUI.Fonts.create(java.awt.Font.MONOSPACED, JBUI.Fonts.smallFont().size)
            foreground = JBColor.RED
            background = JBUI.CurrentTheme.Banner.ERROR_BACKGROUND
            border = JBUI.Borders.compound(JBUI.Borders.customLine(JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR), JBUI.Borders.empty(6))
        }
        else -> JBLabel("Not rendered yet", UIUtil.ComponentStyle.SMALL).apply {
            foreground = UIUtil.getContextHelpForeground()
            horizontalAlignment = JBLabel.CENTER
            border = JBUI.Borders.customLine(JBColor.border())
        }
    }

    init {
        isOpaque = false
        add(JBLabel(entry.title).apply { font = JBUI.Fonts.label().asBold() })
        if (entry.subtitle.isNotEmpty()) {
            add(JBLabel(entry.subtitle, UIUtil.ComponentStyle.SMALL).apply { foreground = UIUtil.getContextHelpForeground() })
        }
        add(content)
        setSelected(selected)
        val mouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybePopup(e)
            override fun mouseReleased(e: MouseEvent) = maybePopup(e)

            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                // A view of the preview: its own code (like Android Studio), else the preview function.
                val view = content as? PreviewImageView
                val target = if (e.component === view) view.callAt(e.x, e.y) else null
                if (target != null && navigator != null) {
                    onSelect(false)
                    navigator.navigate(target, entry.imageFile?.lastModified() ?: 0, focus = e.clickCount >= 2)
                } else {
                    onSelect(e.clickCount >= 2)
                }
            }

            private fun maybePopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                onSelect(false)
                ActionManager.getInstance().createActionPopupMenu("ComposeSwiftUIPreviewCard", actions())
                    .component.show(e.component, e.x, e.y)
            }
        }
        addMouseListener(mouse)
        content.addMouseListener(mouse)
        toolTipText = "Click a view: show its code · Double-click: go to the code · Right-click: more"
    }

    fun setScale(scale: Double) {
        this.scale = scale
        val width = (if (entry.width > 0) entry.width * scale else JBUI.scale(240).toDouble()).toInt()
        when (val view = content) {
            is PreviewImageView -> view.setDisplaySize(width, (entry.height * scale).toInt().coerceAtLeast(1))
            is JBTextArea -> {
                // The wrapped height at that width.
                val errorWidth = width.coerceAtLeast(JBUI.scale(240))
                view.preferredSize = null
                view.setSize(errorWidth, Short.MAX_VALUE.toInt())
                view.preferredSize = Dimension(errorWidth, view.preferredSize.height)
            }
            else -> view.preferredSize = Dimension(width, if (entry.height > 0) (entry.height * scale).toInt() else JBUI.scale(120))
        }
        revalidate()
    }

    fun setSelected(selected: Boolean) {
        border = if (selected) {
            JBUI.Borders.compound(JBUI.Borders.customLine(JBUI.CurrentTheme.Focus.focusColor(), 2), JBUI.Borders.empty(6))
        } else {
            JBUI.Borders.empty(8)
        }
        repaint()
    }
}

/**
 * The screenshot at the scale of the pane. Downscaled in steps (sharp text without aliasing) once per size, so
 * scrolling only blits.
 */
class PreviewImageView(
    private val image: BufferedImage,
    private val layout: PreviewLayoutData? = null,
    private val navigator: SourceNavigator? = null,
) : JComponent() {
    private var displayWidth = image.width
    private var displayHeight = image.height
    private var scaled: Image? = null

    /** The view under the mouse, outlined. */
    private var hovered: LayoutNode? = null

    init {
        if (layout != null && navigator != null) {
            addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val found = nodeAt(e.x, e.y)
                    if (found?.first != hovered) {
                        hovered = found?.first
                        toolTipText = found?.second?.label
                        repaint()
                    }
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mouseExited(e: MouseEvent) {
                    hovered = null
                    repaint()
                }
            })
        }
    }

    private fun pointsPerPixel() = (layout?.width ?: 1.0) / displayWidth.coerceAtLeast(1)

    private fun nodeAt(x: Int, y: Int): Pair<LayoutNode, ResolvedCall>? {
        val layout = layout ?: return null
        val navigator = navigator ?: return null
        return layout.nodeAt(x * pointsPerPixel(), y * pointsPerPixel(), navigator)
    }

    /** The code of the view at a point of this component. */
    fun callAt(x: Int, y: Int): ResolvedCall? = nodeAt(x, y)?.second

    fun setDisplaySize(width: Int, height: Int) {
        if (width == displayWidth && height == displayHeight) return
        displayWidth = width
        displayHeight = height
        scaled = null
        revalidate()
        repaint()
    }

    override fun getPreferredSize() = Dimension(displayWidth, displayHeight)

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            // Device pixels on HiDPI screens: the image keeps all the detail the screen can show.
            val deviceScale = g2.transform.scaleX
            val targetWidth = (displayWidth * deviceScale).toInt().coerceAtLeast(1)
            val targetHeight = (displayHeight * deviceScale).toInt().coerceAtLeast(1)
            val bitmap = scaled?.takeIf { it.getWidth(null) == targetWidth && it.getHeight(null) == targetHeight }
                ?: downscale(image, targetWidth, targetHeight).also { scaled = it }
            g2.scale(1 / deviceScale, 1 / deviceScale)
            g2.drawImage(bitmap, 0, 0, null)
            g2.scale(deviceScale, deviceScale)
            g2.color = JBUI.CurrentTheme.ToolWindow.borderColor()
            g2.stroke = BasicStroke(1f)
            g2.drawRect(0, 0, displayWidth - 1, displayHeight - 1)
            hovered?.let { node ->
                val bounds = node.scaled(1 / pointsPerPixel())
                g2.color = JBUI.CurrentTheme.Focus.focusColor()
                g2.stroke = BasicStroke(2f)
                g2.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
            }
        } finally {
            g2.dispose()
        }
    }

    private fun downscale(source: BufferedImage, width: Int, height: Int): BufferedImage {
        var current = source
        while (current.width / 2 >= width && current.height / 2 >= height) {
            current = resize(current, current.width / 2, current.height / 2)
        }
        return resize(current, width, height)
    }

    private fun resize(source: BufferedImage, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(source, 0, 0, width, height, null)
        g.dispose()
        return result
    }
}

/** Copy and save actions of the cards. */
object PreviewImages {
    fun copy(entry: PreviewEntry) {
        val image = entry.image ?: return
        CopyPasteManager.getInstance().setContents(object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any = image
        })
    }

    fun copyText(text: String) {
        CopyPasteManager.getInstance().setContents(TextTransferable(text as CharSequence))
    }

    fun save(project: Project, entry: PreviewEntry) {
        val source = entry.imageFile ?: return
        val descriptor = FileSaverDescriptor("Save Preview Image", "Save the rendered preview as PNG", "png")
        val name = entry.title.replace(Regex("""[^\w\- ]"""), "_") + ".png"
        val target = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project).save(name) ?: return
        source.copyTo(target.file, overwrite = true)
    }
}
