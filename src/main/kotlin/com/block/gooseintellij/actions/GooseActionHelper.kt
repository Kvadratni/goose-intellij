package com.block.gooseintellij.actions

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.block.gooseintellij.utils.GooseUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger

object GooseActionHelper {

    val logger = Logger.getInstance(GooseActionHelper::class.java)
    
    fun checkGooseAvailability(project: Project?): Boolean {
        if (GooseUtils.getGooseState() != true && GooseUtils.getSqGooseState() != true) {
            Messages.showMessageDialog(project, "Goose or SQ Goose is not available. Please install them.", "Error", Messages.getErrorIcon())
            return false
        }
        return true
    }
    
    fun getGooseTerminal(event: AnActionEvent): GooseTerminalWidget? {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")

        if (toolWindow == null) {
            logger.warn("ToolWindow 'Goose' not found")
            return null
        }

        if (!toolWindow.isVisible) {
            OpenGooseTerminalAction().actionPerformed(event)
        }
        
        val contentManager = toolWindow.contentManager
        val content = contentManager.getContent(0)
        val gooseTerminal = content?.component as? GooseTerminalWidget

        if (gooseTerminal == null) {
            logger.warn("GooseTerminalPanel is null")
            return null
        }

        return gooseTerminal
    }
    
    fun askGooseToGenerateTests(gooseTerminal: GooseTerminalWidget?, question: String) {
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(null, "Asking Goose To Generate Tests...") {
                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    logger.info("Question: $question")

                    if (gooseTerminal != null) {
                        GooseUtils.writeCommandToTerminal(gooseTerminal.connector!!, question)
                        logger.info("Question sent to GooseTerminalPanel")
                    } else {
                        logger.warn("GooseTerminalPanel is null")
                    }
                }
            })
    }

    fun checkAndSendToGoose(event: AnActionEvent, commandFormat: String, dataExtractor: (AnActionEvent) -> Triple<String?, String?, Boolean>) {
        val project = event.project

        if (!checkGooseAvailability(project)) return

        var (selectedText, filePath, isEditor) = dataExtractor(event)
        if (isEditor && filePath != null) {
            if(selectedText.isNullOrEmpty()) {
               selectedText = "The whole file"
            }
            val gooseTerminal = getGooseTerminal(event) ?: return
            val command = String.format(commandFormat, selectedText.replace("\n", "\\n"), filePath)
            askGooseToGenerateTests(gooseTerminal, command)
        } else if (!isEditor && filePath != null) {
            val gooseTerminal = getGooseTerminal(event) ?: return
            val command = String.format(commandFormat, filePath)
            askGooseToGenerateTests(gooseTerminal, command)
        } else {
            Messages.showMessageDialog("No file in context", "Warning", Messages.getWarningIcon())
        }
    }
}
