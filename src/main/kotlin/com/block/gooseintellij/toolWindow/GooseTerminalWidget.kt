package com.block.gooseintellij.toolWindow

import com.block.gooseintellij.utils.GooseUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.pty.PtyProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import com.pty4j.PtyProcessBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets
import com.intellij.openapi.diagnostic.Logger
import java.awt.BorderLayout
import java.awt.Dimension
import com.intellij.openapi.wm.ToolWindow

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

  private fun createTerminal(project: Project, parent: Disposable): JBTerminalWidget {
    val settingsProvider = JBTerminalSystemSettingsProviderBase()
    val terminalWidget = JBTerminalWidget(project, settingsProvider, parent)
    connector = createLocalShellTtyConnector()
    terminalWidget.start(connector)
    writeCommandToTerminal(connector!!, "cd ${GooseUtils.getProjectPath(project)}")
    writeCommandToTerminal(connector!!, "export goose=\$(which goose)")
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
      val command = GooseUtils.getShell()
      Logger.getInstance(GooseTerminalWidget::class.java)
        .info("Starting the terminal process with command: ${command.joinToString(" ")}")
      val builder = PtyProcessBuilder().setCommand(command)
        .setEnvironment(mapOf("TERM" to "xterm-256color"))
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
