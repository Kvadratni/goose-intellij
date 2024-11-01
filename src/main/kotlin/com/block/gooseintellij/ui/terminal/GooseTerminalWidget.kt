package com.block.gooseintellij.ui.terminal

import com.block.gooseintellij.viewmodel.TerminalViewModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.openapi.wm.ToolWindow
import com.jediterm.terminal.TtyConnector
import java.awt.BorderLayout
import java.awt.Dimension

class GooseTerminalWidget(toolWindow: ToolWindow) : javax.swing.JPanel() {
    private val viewModel: TerminalViewModel
    var connector: TtyConnector? = null
    
    init {
        val project = toolWindow.project
        viewModel = TerminalViewModel(project)
        val terminalWidget = createTerminal(project, Disposer.newDisposable())

        this.layout = BorderLayout()
        this.add(terminalWidget.component, BorderLayout.CENTER)
        this.preferredSize = Dimension(300, 600)
    }

    private fun createTerminal(project: Project, parent: Disposable): JBTerminalWidget {
        val settingsProvider = viewModel.createTerminalSettings()
        val terminalWidget = JBTerminalWidget(project, settingsProvider, parent)
        connector = viewModel.createLocalShellTtyConnector()
        terminalWidget.start(connector)
        
        // Execute initial commands
        viewModel.getInitialCommands().forEach { command ->
            TerminalViewModel.writeCommandToTerminal(connector!!, command)
        }
        
        return terminalWidget
    }
}
