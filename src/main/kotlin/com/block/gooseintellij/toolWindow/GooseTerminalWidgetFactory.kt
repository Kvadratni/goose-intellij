package com.block.gooseintellij.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.ToolWindow
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.pty.PtyProcessTtyConnector
import com.intellij.ui.content.ContentFactory
import com.jediterm.terminal.TtyConnector
import com.pty4j.PtyProcessBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindowFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import com.intellij.openapi.fileEditor.FileEditorManager

class GooseTerminalWidgetFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = GooseTerminalWidget(toolWindow)
    
    // Compute preferred initial size (30% width of the editor window)
    val fileEditorManager = FileEditorManager.getInstance(project)
    val editorWindow = fileEditorManager.getSelectedEditor()?.component?.parent
    val editorWindowWidth = editorWindow?.width ?: 0
    val desiredWidth = (editorWindowWidth * 0.3).toInt()
    panel.preferredSize = Dimension(desiredWidth, editorWindow?.height ?: 600)

    // Listen for resize events on the terminal panel
    panel.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        // Adjust the size dynamically if necessary
      }

      override fun componentShown(e: ComponentEvent) {
        panel.size = Dimension(desiredWidth, editorWindow?.height ?: 600)
      }
    })

    val contentFactory = ApplicationManager.getApplication().getService(ContentFactory::class.java)
    val content = contentFactory.createContent(panel, "goose-shell", false)
    toolWindow.contentManager.addContent(content)
  }
}

class GooseTerminalWidget(toolWindow: ToolWindow) : javax.swing.JPanel() {
  var connector: TtyConnector? = null
  init {
    val project = toolWindow.project
    val terminalWidget = createTerminal(project, Disposer.newDisposable())

    this.layout = BorderLayout()
    this.add(terminalWidget.component, BorderLayout.CENTER)
    
    // Optional: Ensure preferred size gets set on init
    this.preferredSize = Dimension(300, 600)
  }

  //writes output to terminal
  fun writeToTerminal(output: String) {
    writeCommandToTerminal(connector!!, "echo '$output'")
  }

  private fun createTerminal(project: Project, parent: Disposable): JBTerminalWidget {
    val settingsProvider = JBTerminalSystemSettingsProviderBase()
    val terminalWidget = JBTerminalWidget(project, settingsProvider, parent)
    connector = createLocalShellTtyConnector()
    terminalWidget.start(connector)
    writeCommandToTerminal(connector!!, "cd ${project.basePath}")
    clearTerminal()
    return terminalWidget
  }

  private fun clearTerminal() {
    if (SystemInfo.isWindows) {
      writeCommandToTerminal(connector!!, "cls")
    } else
      writeCommandToTerminal(connector!!, "clear")
  }

  private fun createLocalShellTtyConnector(): TtyConnector {
    return try {
      val command: Array<String>
      val isMacOs = SystemInfo.isMac

      if (SystemInfo.isWindows) {
        command = arrayOf("cmd.exe")
      } else if (isMacOs && SystemInfo.OS_VERSION >= "10.15") {
        // Default to zsh for macOS >= 10.15
        command = arrayOf("/bin/zsh")
      } else {
        // Linux or older macOS should use bash
        command = arrayOf("/bin/bash")
      }

      val builder = PtyProcessBuilder().setCommand(command)
      val ptyProcess = builder.start()
      PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8)
    } catch (e: IOException) {
      Logger.getInstance(GooseTerminalWidget::class.java)
        .error("Failed to start the terminal process", e)
      throw RuntimeException("Failed to start the terminal process", e)
    }
  }

  companion object {
    fun writeCommandToTerminal(connector: TtyConnector, command: String) {
      connector.write(command + "\r\n")
    }
  }
}
