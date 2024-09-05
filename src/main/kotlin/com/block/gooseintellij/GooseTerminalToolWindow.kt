package com.block.gooseintellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import javax.swing.JTextArea

class GooseTerminalToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val gooseTerminalPanel = GooseTerminalPanel()
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(gooseTerminalPanel, "Goose Terminal", false)
        toolWindow.contentManager.addContent(content)
    }
}

class GooseTerminalPanel : JPanel() {
    private val terminalOutput: JTextArea = JTextArea()

    init {
        terminalOutput.isEditable = false
        add(terminalOutput)
    }

    fun printOutput(text: String) {
        terminalOutput.append("$text\n")
    }

    fun clearOutput() {
        terminalOutput.text = ""
    }
}
