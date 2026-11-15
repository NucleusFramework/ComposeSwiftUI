package dev.nucleusframework.composeswiftui.idea

import com.intellij.AppTopics
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPanel

/** A top-level `@Preview @Composable fun Name()`, as found by the Gradle plugin. */
fun KtNamedFunction.isComposeSwiftUIPreview(): Boolean {
    val annotations = annotationEntries.mapNotNull { it.shortName?.asString() }
    return isTopLevel && "Preview" in annotations && "Composable" in annotations && valueParameters.isEmpty()
}

private fun KtNamedFunction.previewTitle(): String {
    val argument = annotationEntries.first { it.shortName?.asString() == "Preview" }.valueArguments.firstOrNull()
    return (argument?.getArgumentExpression() as? KtStringTemplateExpression)
        ?.entries?.joinToString("") { it.text }?.takeIf { it.isNotEmpty() }
        ?: name.orEmpty()
}

/** Right side of the split editor: the rendered previews of the file, re-rendered when the file is saved. */
class PreviewEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    // Resolved on use: the module is unknown until the Gradle sync has finished.
    val module: PreviewModule? get() = PreviewModule.of(project, file)
    private val status = JBLabel().apply { foreground = UIUtil.getContextHelpForeground() }
    private val cards = JPanel(VerticalLayout(JBUI.scale(20))).apply { border = JBUI.Borders.empty(12) }
    private val cardsByName = mutableMapOf<String, JComponent>()
    private val component = JPanel(BorderLayout())

    init {
        val refresh = object : AnAction("Render Previews", "Render the previews on the iOS simulator", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = render()
            override fun update(e: AnActionEvent) {
                val module = module
                e.presentation.isEnabled = module != null && !project.service<PreviewRenderer>().isRendering(module)
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }.apply { copyShortcutFrom(ActionManager.getInstance().getAction(RenderPreviewsAction.ID)) }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("ComposeSwiftUIPreview", DefaultActionGroup(refresh), true)
            .apply { targetComponent = component }
        val header = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.WEST)
            add(status, BorderLayout.CENTER)
            border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())
        }
        component.add(header, BorderLayout.NORTH)
        component.add(JBScrollPane(cards).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)

        val connection = project.messageBus.connect(this)
        connection.subscribe(PreviewRenderListener.TOPIC, object : PreviewRenderListener {
            override fun renderingStarted(module: PreviewModule) {
                if (module == this@PreviewEditor.module) status.text = "Rendering on the iOS simulator…"
            }

            override fun renderingFinished(module: PreviewModule, success: Boolean) {
                if (module != this@PreviewEditor.module) return
                status.text = if (success) "" else "Rendering failed: see the Run tool window"
                reload()
            }
        })
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    if (FileDocumentManager.getInstance().getFile(document) == file) {
                        ApplicationManager.getApplication().invokeLater { render() }
                    }
                }
            })

        connection.subscribe(ProjectDataImportListener.TOPIC, object : ProjectDataImportListener {
            override fun onImportFinished(projectPath: String?) {
                ApplicationManager.getApplication().invokeLater { if (!project.isDisposed) reloadOrRender() }
            }
        })
        reloadOrRender()
    }

    /** Shows the rendered previews, or renders them when there are none yet. */
    private fun reloadOrRender() {
        reload { previews ->
            val module = module
            status.text = if (module == null) "Waiting for the Gradle sync…" else ""
            if (module != null && previews.isNotEmpty() && previews.none { it.image != null }) render()
        }
    }

    fun render() {
        module?.let { project.service<PreviewRenderer>().render(it) }
    }

    /** Scrolls to the preview of [qualifiedName]. */
    fun show(qualifiedName: String) {
        cardsByName[qualifiedName]?.let { it.scrollRectToVisible(it.bounds.apply { setLocation(0, 0) }) }
    }

    private class Preview(val qualifiedName: String, val title: String, val image: BufferedImage?)

    /** Reads the file's previews and their images off the EDT (PSI and disk access), then shows them. */
    private fun reload(then: (List<Preview>) -> Unit = {}) {
        ReadAction.nonBlocking<List<Preview>> {
            val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return@nonBlocking emptyList()
            val module = module
            ktFile.declarations.filterIsInstance<KtNamedFunction>()
                .filter { it.isComposeSwiftUIPreview() }
                .mapNotNull { function ->
                    val qualifiedName = function.fqName?.asString() ?: return@mapNotNull null
                    val image = module?.image(qualifiedName)?.takeIf { it.exists() }?.let { ImageIO.read(it) }
                    Preview(qualifiedName, function.previewTitle(), image)
                }
        }
            .expireWith(this)
            .finishOnUiThread(ModalityState.any()) { previews ->
                showPreviews(previews)
                then(previews)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showPreviews(previews: List<Preview>) {
        cards.removeAll()
        cardsByName.clear()
        for (preview in previews) {
            val card = JPanel(VerticalLayout(JBUI.scale(6))).apply { isOpaque = false }
            card.add(JBLabel(preview.title).apply { font = JBUI.Fonts.label().asBold() })
            card.add(
                preview.image?.let { ScaledImage(it) }
                    ?: JBLabel("Not rendered yet").apply { foreground = UIUtil.getContextHelpForeground() },
            )
            cards.add(card)
            cardsByName[preview.qualifiedName] = card
        }
        if (previews.isEmpty()) cards.add(JBLabel("No @Preview composable in this file"))
        cards.revalidate()
        cards.repaint()
    }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent = cards
    override fun getName() = "Preview"
    override fun getFile() = file
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {}
}

/** The screenshot at phone width (~ points), keeping its aspect ratio. */
private class ScaledImage(private val image: BufferedImage) : JComponent() {
    private val displayWidth = JBUI.scale(300)
    private val displayHeight = displayWidth * image.height / image.width

    override fun getPreferredSize() = Dimension(displayWidth, displayHeight)

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2.drawImage(image, 0, 0, displayWidth, displayHeight, null)
        g2.color = JBUI.CurrentTheme.ToolWindow.borderColor()
        g2.drawRect(0, 0, displayWidth - 1, displayHeight - 1)
    }
}
