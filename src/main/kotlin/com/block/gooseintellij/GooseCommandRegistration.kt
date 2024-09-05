package com.block.gooseintellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class OpenGooseTerminalAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")
        toolWindow?.show(null)
        // Initialise and send initial command to Goose
        Messages.showMessageDialog(project, "Goose Terminal Opened", "Info", Messages.getInformationIcon())
    }
}

class SendToGooseAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val document = editor?.document
        val selectedText = editor?.selectionModel?.selectedText

        if (selectedText != null && selectedText.isNotEmpty()) {
            // Process selected text with Goose
            val gooseTerminalPanel = GooseTerminalPanel() // You would generally retrieve the singleton instance
            gooseTerminalPanel.printOutput("Processing selected text: $selectedText")
        } else {
            Messages.showMessageDialog("No text selected.", "Warning", Messages.getWarningIcon())
        }
    }
}
