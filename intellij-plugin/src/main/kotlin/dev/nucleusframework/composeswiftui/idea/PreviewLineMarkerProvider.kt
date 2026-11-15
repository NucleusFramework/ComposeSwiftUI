package dev.nucleusframework.composeswiftui.idea

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction

/** Gutter icon on each preview: renders it and shows it in the preview pane. */
class PreviewLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val function = element.parent as? KtNamedFunction ?: return null
        if (function.nameIdentifier != element || !function.isComposeSwiftUIPreview()) return null
        val qualifiedName = function.fqName?.asString() ?: return null
        val handler = GutterIconNavigationHandler<PsiElement> { _, psi ->
            val file = psi.containingFile.virtualFile ?: return@GutterIconNavigationHandler
            val split = FileEditorManager.getInstance(psi.project).getEditors(file)
                .filterIsInstance<TextEditorWithPreview>()
                .firstOrNull { it.previewEditor is PreviewEditor }
                ?: return@GutterIconNavigationHandler
            split.setLayout(TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW)
            val preview = split.previewEditor as PreviewEditor
            preview.show(qualifiedName)
            preview.render()
        }
        return LineMarkerInfo(
            element, element.textRange, AllIcons.Actions.Preview, { "Render Compose SwiftUI preview" },
            handler, GutterIconRenderer.Alignment.LEFT, { "Compose SwiftUI preview" },
        )
    }
}
