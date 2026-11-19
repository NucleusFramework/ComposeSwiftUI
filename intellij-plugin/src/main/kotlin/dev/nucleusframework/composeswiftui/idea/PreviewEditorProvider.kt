package dev.nucleusframework.composeswiftui.idea

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** Code | preview split editor for Kotlin files that declare Compose SwiftUI previews, like Compose's own preview. */
class PreviewEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.extension != "kt" || !file.isInLocalFileSystem) return false
        // Cheap text check (no PSI here): a preview (or a library multipreview) in a file that uses the library.
        val text = String(file.contentsToByteArray(), file.charset)
        return "@Preview" in text && "dev.nucleusframework.composeswiftui" in text
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val preview = PreviewEditor(project, file).apply { attach(textEditor) }
        return TextEditorWithPreview(textEditor, preview, "Compose SwiftUI Preview")
    }

    override fun getEditorTypeId() = "compose-swiftui-preview"

    // Hides every other editor of the file, not only the default one: Android Studio opens Kotlin files in an editor of
    // its own, which would otherwise be selected, leaving the previews in a tab at the bottom.
    override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}
