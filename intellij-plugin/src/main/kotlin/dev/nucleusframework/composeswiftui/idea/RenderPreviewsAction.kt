package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.DumbAwareAction

/** Renders the previews of the current file again (Code › Render Compose SwiftUI Previews, with a shortcut). */
class RenderPreviewsAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        previewEditor(e)?.render()
    }

    override fun update(e: AnActionEvent) {
        val editor = previewEditor(e)
        val module = editor?.module
        e.presentation.isEnabledAndVisible = editor != null
        e.presentation.isEnabled = module != null && !e.project!!.service<PreviewRenderer>().isRendering(module)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    private fun previewEditor(e: AnActionEvent): PreviewEditor? {
        val project = e.project ?: return null
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        return FileEditorManager.getInstance(project).getEditors(file)
            .filterIsInstance<TextEditorWithPreview>()
            .firstNotNullOfOrNull { it.previewEditor as? PreviewEditor }
    }

    companion object {
        const val ID = "ComposeSwiftUI.RenderPreviews"
    }
}
