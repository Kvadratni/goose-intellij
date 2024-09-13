package com.block.gooseintellij.actions

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.block.gooseintellij.utils.GooseUtils
import com.intellij.openapi.diagnostic.Logger

const val GOOSE_COMMAND_FORMAT = "Please analyze the following code %s\\n in file: %s\\n"

class SendToGooseAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project

        if (GooseUtils.getGooseState() != true && GooseUtils.getSqGooseState() != true) {
            Messages.showMessageDialog(project, "Goose or SQ Goose is not available. Please install them.", "Error", Messages.getErrorIcon())
            return
        }
        
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
            GooseUtils.writeCommandToTerminal(gooseTerminal?.connector!!, command)
        } else {
            Messages.showMessageDialog("No file or text selected.", "Warning", Messages.getWarningIcon())
        }
    }
}
