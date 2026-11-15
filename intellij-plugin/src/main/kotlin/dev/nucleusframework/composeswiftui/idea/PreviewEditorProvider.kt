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
        // Cheap text check (no PSI here): a preview in a file that uses the library.
        val text = String(file.contentsToByteArray(), file.charset)
        return "@Preview" in text && "dev.nucleusframework.composeswiftui" in text
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        return TextEditorWithPreview(textEditor, PreviewEditor(project, file), "Compose SwiftUI Preview")
    }

    override fun getEditorTypeId() = "compose-swiftui-preview"

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
