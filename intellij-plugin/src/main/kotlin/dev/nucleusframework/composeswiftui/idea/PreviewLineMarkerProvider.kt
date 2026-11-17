package dev.nucleusframework.composeswiftui.idea

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.kotlin.psi.KtNamedFunction

/** Gutter icon on each preview: render it, show it in the preview pane, or run it on the simulator. */
class PreviewLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val function = element.parent as? KtNamedFunction ?: return null
        if (function.nameIdentifier != element || !function.isComposeSwiftUIPreview()) return null
        val qualifiedName = function.fqName?.asString() ?: return null
        // Read now (a read action): the click runs on the UI thread, without read access to the PSI.
        val file = function.containingFile.virtualFile ?: return null
        val key = if (function.valueParameters.isNotEmpty()) "$qualifiedName#0" else qualifiedName
        val handler = GutterIconNavigationHandler<PsiElement> { event, psi ->
            val project = psi.project
            val group = DefaultActionGroup(
                action("Render Preview", AllIcons.Actions.Refresh) {
                    previewEditor(project, file, show = true)?.apply { show(qualifiedName) }?.render(qualifiedName)
                },
                action("Show in Preview Pane", AllIcons.Actions.Preview) { previewEditor(project, file, show = true)?.show(qualifiedName) },
                action("Interactive Mode", AllIcons.Actions.Execute) {
                    previewEditor(project, file, show = true)?.startInteractive(key)
                },
                action("Run on Simulator", AllIcons.Nodes.Deploy) {
                    PreviewModule.of(project, file)?.let { project.service<PreviewRenderer>().runInteractive(it, key) }
                },
            )
            JBPopupFactory.getInstance()
                .createActionGroupPopup(null, group, SimpleDataContext.getProjectContext(project), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
                .show(RelativePoint(event))
        }
        return LineMarkerInfo(
            element, element.textRange, AllIcons.Actions.Preview, { "Compose SwiftUI preview: render, show or run" },
            handler, GutterIconRenderer.Alignment.LEFT, { "Compose SwiftUI preview" },
        )
    }

    private fun previewEditor(project: Project, file: VirtualFile, show: Boolean): PreviewEditor? {
        val split = FileEditorManager.getInstance(project).getEditors(file)
            .filterIsInstance<TextEditorWithPreview>()
            .firstOrNull { it.previewEditor is PreviewEditor }
            ?: return null
        // The gutter is only visible with the code, so this shows the preview next to it.
        if (show) split.setLayout(TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW)
        return split.previewEditor as PreviewEditor
    }
}
