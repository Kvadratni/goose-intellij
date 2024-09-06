package com.block.gooseintellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class AskGooseAboutFileOrDirectoryAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project
    val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)

    if (virtualFile != null) {
      val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose")

      ProgressManager.getInstance()
        .run(object : Task.Backgroundable(project, "Asking Goose About File/Directory...") {
          override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
            val question = "What can you tell me about the file/directory: ${virtualFile.path}?"
            val output = GooseCommandHelper.sendCommandToGoose(question)

            ApplicationManager.getApplication().invokeLater {
              val contentManager = toolWindow?.contentManager
              val content = contentManager?.getContent(0)
              val gooseTerminalPanel = content?.component

            }
          }
        })
    } else {
      Messages.showMessageDialog(
        "No file or directory selected.", "Warning", Messages.getWarningIcon()
      )
    }
  }

  override fun update(e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    e.presentation.isEnabledAndVisible = virtualFile != null
  }
}
