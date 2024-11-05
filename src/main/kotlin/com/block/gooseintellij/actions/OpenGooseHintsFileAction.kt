package com.block.gooseintellij.actions

import com.block.gooseintellij.utils.GooseUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class OpenGooseHintsFileAction : AnAction("Open Goose Hints File") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val filePath = GooseUtils.getProjectPath(project) + "/.goosehints"
        val file = File(filePath)

        val virtualFile: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)

        // Open in editor
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
