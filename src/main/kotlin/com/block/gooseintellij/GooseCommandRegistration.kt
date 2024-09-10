package com.block.gooseintellij

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger

class OpenGooseTerminalAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")

        if (toolWindow != null && !toolWindow.isVisible) {
            toolWindow.activate(null)
            toolWindow.show()
        }
    }
}

const val GOOSE_COMMAND_FORMAT = "Please analyze the following code %s\\n in file: %s\\n"

class SendToGooseAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")

        if (toolWindow == null || !toolWindow.isVisible) {
            OpenGooseTerminalAction().actionPerformed(event)
        }

        val contentManager = toolWindow?.contentManager
        val content = contentManager?.getContent(0)
        val gooseTerminal = content?.component as? GooseTerminalWidget

        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)

        if (selectedText != null && selectedText.isNotEmpty() && psiFile != null) {
            val filePath = psiFile.virtualFile.path
            val command = String.format(GOOSE_COMMAND_FORMAT, selectedText.replace("\n", "\\n"), filePath)
            GooseTerminalWidget.writeCommandToTerminal(gooseTerminal?.connector!!, command)
        } else {
            Messages.showMessageDialog("No file or text selected.", "Warning", Messages.getWarningIcon())
        }
    }
}

class AskGooseAboutFileOrDirectoryAction : AnAction() {

    private val logger = Logger.getInstance(AskGooseAboutFileOrDirectoryAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        logger.info("ActionPerformed triggered")
        
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        logger.info("Project: $project, VirtualFile: $virtualFile")

        if (virtualFile != null) {
            val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")
            logger.info("ToolWindow: $toolWindow")

            if (toolWindow == null) {
                logger.warn("ToolWindow 'Goose' not found")
            } else {
                if (!toolWindow.isVisible) {
                    OpenGooseTerminalAction().actionPerformed(event)
                }
                ProgressManager.getInstance()
                    .run(object : Task.Backgroundable(project, "Asking Goose About File/Directory...") {
                        override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                            val question = "What can you tell me about the file/directory: ${virtualFile.path}?"
                            logger.info("Question: $question")
                            
                            val contentManager = toolWindow.contentManager
                            val content = contentManager.getContent(0)
                            val gooseTerminal = content?.component as? GooseTerminalWidget

                            if (gooseTerminal != null) {
                                GooseTerminalWidget.writeCommandToTerminal(gooseTerminal.connector!!, question)
                                logger.info("Question sent to GooseTerminalPanel")
                            } else {
                                logger.warn("GooseTerminalPanel is null")
                            }
                        }
                    })
            }
        } else {
            Messages.showMessageDialog(
                "No file or directory selected.", "Warning", Messages.getWarningIcon()
            )
            logger.warn("No file or directory selected.")
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
