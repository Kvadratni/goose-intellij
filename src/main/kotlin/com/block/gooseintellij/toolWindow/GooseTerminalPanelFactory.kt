package com.block.gooseintellij.toolWindow

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JScrollPane
import javax.swing.JTextField
import com.intellij.openapi.util.Disposer
import java.io.IOException

class GooseTerminalPanelFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myTerminalPanel = GooseTerminalPanel(toolWindow)
        val content = ContentFactory.getInstance().createContent(myTerminalPanel.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        println("Tool window content created with Terminal Panel.")
    }
}

class GooseTerminalPanel(toolWindow: ToolWindow) : javax.swing.JPanel() {
    private val consoleView = ConsoleViewImpl(toolWindow.project, false)
    private var processHandler: OSProcessHandler? = null
    private val inputField = JTextField()

    init {
        layout = BorderLayout()
        val disposable = Disposer.newDisposable()
        Disposer.register(toolWindow.project, disposable)
        Disposer.register(disposable, consoleView)
        val scrollPane = JScrollPane(consoleView.component)

        add(scrollPane, BorderLayout.CENTER)
        add(inputField, BorderLayout.SOUTH)

        inputField.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val inputText = inputField.text
                inputField.text = ""
                processInput(inputText)
            }
        })
    }

    fun attachToProcess(processHandler: ProcessHandler) {
        this.processHandler = processHandler as? OSProcessHandler
        if (this.processHandler != null) {
            try {
                consoleView.attachToProcess(this.processHandler!!)
                println("Attached to process: ${this.processHandler!!.process}")
            } catch (e: Throwable) {
                println("Error attaching to process: ${e.message}")
            }
        } else {
            println("ProcessHandler is null or invalid")
        }
    }

    fun getContent() = this

    fun processInput(input: String) {
        try {
            // Print local echo of the input
            printOutput("> $input")
            if (this.processHandler?.processInput != null) {
                this.processHandler!!.processInput.apply {
                    write((input + "\n").toByteArray())
                    flush()
                }
            } else {
                printOutput("Process input stream is unavailable")
            }
        } catch (ioe: IOException) {
            printOutput("Failed to send input to Goose: ${ioe.message}")
        }
    }

    fun printOutput(text: String) {
        consoleView.print(text + "\n", com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT)
        println("Printed to terminal: $text")
    }
}
