package com.block.gooseintellij.viewmodel

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.pty.PtyProcessTtyConnector
import com.block.gooseintellij.utils.GooseUtils
import com.jediterm.terminal.TtyConnector
import com.pty4j.PtyProcessBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets

class TerminalViewModel(private val project: Project) {
    private val logger = Logger.getInstance(TerminalViewModel::class.java)
    
    fun createLocalShellTtyConnector(): TtyConnector {
        return try {
            val command = GooseUtils.getShell()
            logger.info("Starting the terminal process with command: ${command.joinToString(" ")}")
            val builder = PtyProcessBuilder().setCommand(command)
                .setEnvironment(mapOf("TERM" to "xterm-256color"))
            val ptyProcess = builder.start()
            PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            logger.error("Failed to start the terminal process", e)
            throw RuntimeException("Failed to start the terminal process", e)
        }
    }

    fun createTerminalSettings(): JBTerminalSystemSettingsProviderBase {
        return JBTerminalSystemSettingsProviderBase()
    }

    fun getInitialCommands(): List<String> {
        return listOf(
            "cd ${GooseUtils.getProjectPath(project)}",
            "export goose=\$(which goose)",
            if (SystemInfo.isWindows) "cls" else "clear"
        )
    }

    companion object {
        fun writeCommandToTerminal(connector: TtyConnector, command: String) {
            connector.write(command + "\r\n")
        }
    }
}
