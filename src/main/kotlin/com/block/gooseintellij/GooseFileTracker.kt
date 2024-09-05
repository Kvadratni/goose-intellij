package com.block.gooseintellij

import com.intellij.openapi.application.ApplicationAdapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GooseFileTracker(private val project: Project) {
    init {
        val messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                updateOpenFiles()
            }

            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                updateOpenFiles()
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                updateOpenFiles()
            }
        })

        ApplicationManager.getApplication().addApplicationListener(object : ApplicationAdapter() {
            override fun beforeWriteActionStart(action: Any) {
                updateUnsavedFiles()
            }
        })
    }

    private fun updateOpenFiles() {
        val openFiles = FileEditorManager.getInstance(project).openFiles.map { it.path }.joinToString("\n")
        // Write open file paths to a temp file or use it as needed
    }

    private fun updateUnsavedFiles() {
        val unsavedFiles = FileDocumentManager.getInstance().unsavedDocuments.map { it.text }.joinToString("\n")
        // Write unsaved changes to a temp file or use it as needed
    }
}
