package com.block.gooseintellij

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.block.gooseintellij.toolWindow.GooseTerminalPanel

class OpenGooseTerminalAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")
        
        if (toolWindow != null && !toolWindow.isVisible) {
            toolWindow.activate(null)
            
            val contentManager = toolWindow.contentManager
            val content = contentManager.getContent(0)
            val gooseTerminalPanel = content?.component as? GooseTerminalPanel

            gooseTerminalPanel?.processInput("goose session start\n")
        }
    }
}

const val GOOSE_COMMAND_PREFIX = "Please analyze the following code: "

class SendToGooseAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")

        if (toolWindow == null || !toolWindow.isVisible) {
            OpenGooseTerminalAction().actionPerformed(event)
        }

        val contentManager = toolWindow?.contentManager
        val content = contentManager?.getContent(0)
        val gooseTerminalPanel = content?.component as? GooseTerminalPanel

        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText

        if (selectedText != null && selectedText.isNotEmpty()) {
            val command = GOOSE_COMMAND_PREFIX + selectedText + "\n"
            gooseTerminalPanel?.processInput(command)
        } else {
            Messages.showMessageDialog("No text selected.", "Warning", Messages.getWarningIcon())
        }
    }
}
