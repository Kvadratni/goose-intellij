package com.block.gooseintellij.utils

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.jediterm.terminal.TtyConnector
import java.io.IOException

object GooseUtils {
    private val logger = Logger.getInstance(GooseUtils::class.java)

    private var isSqGoosePresent: Boolean? = null
    private var isGoosePresent: Boolean? = null
    private var sqPath: String? = null
    private var goosePath: String? = null

    fun writeCommandToTerminal(connector: TtyConnector, command: String) {
        connector.write(command + "\r\n")
    }

    fun startGooseSession(usingSqGoose: Boolean, gooseTerminal: GooseTerminalWidget?, project: Project) {
        val propertiesComponent = PropertiesComponent.getInstance(project)

        // Load saved profile from settings
        val savedProfile = propertiesComponent.getValue("goose.selected.profile")
        val profileArgument = savedProfile?.let { "--profile $it" } ?: ""
        val gooseInstance = if (usingSqGoose) "sq goose" else "goose"
        val savedSessionName = propertiesComponent.getValue("goose.saved.session")
        val sessionCommand = savedSessionName?.let { "resume $it" } ?: "start"
        val command = "$gooseInstance session $sessionCommand $profileArgument"
        gooseTerminal?.writeToTerminal("Starting Goose session...")
        writeCommandToTerminal(gooseTerminal?.connector!!, command)
    }

    fun checkGooseAvailability() {
        try {
            if (isSqGoosePresent == null) {
                sqPath = getCommandOutput(arrayOf("which", "sq"))
                isSqGoosePresent = getCommandOutput(arrayOf(sqPath!!, "goose", "--version")).isNotEmpty()
                logger.info("SQ Goose present: $isSqGoosePresent, Path: $sqPath")
            }
        } catch (e: Exception) {
            isSqGoosePresent = false
            logger.error("Error checking SQ Goose availability", e)
        }

        try {
            if (isGoosePresent == null) {
                goosePath = getCommandOutput(arrayOf("which", "goose", "--version"))
                isGoosePresent = getCommandOutput(arrayOf(goosePath!!)).isNotEmpty()
                logger.info("Goose present: $isGoosePresent, Path: $goosePath")
            }
        } catch (e: Exception) {
            isGoosePresent = false
            logger.error("Error checking Goose availability", e)
        }
    }

    private fun getCommandOutput(command: Array<String>): String {
        return try {
            val process = ProcessBuilder(command.asList()).start()
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: IOException) {
            logger.warn("Command check failed for: $command", e)
            ""
        }
    }

    fun promptGooseInstallationIfNeeded(gooseTerminal: GooseTerminalWidget?, project: Project): Boolean {
        if (isSqGoosePresent!!) {
            if (isGoosePresent!!) {
                ApplicationManager.getApplication().invokeLater {
                    val installUrl = "https://github.com/square/goose"
                    val result = Messages.showYesNoDialog(
                        project,
                        "Goose is required to be installed. Do you want to install it?",
                        "Install Goose",
                        "Install",
                        "Cancel",
                        Messages.getQuestionIcon()
                    )

                    if (result == Messages.YES) {
                        BrowserUtil.browse(installUrl)
                    } else {
                        gooseTerminal?.writeToTerminal("Goose installation was canceled by the user.")
                    }
                }
                return false
            }
            startGooseSession(true, gooseTerminal, project)
        } else {
            startGooseSession(false, gooseTerminal, project)
        }
        return true
    }

    fun getGooseState(): Boolean? {
        return isGoosePresent
    }

    fun getSqGooseState(): Boolean? {
        return isSqGoosePresent
    }

    fun getShell(): Array<String> {
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
        return command
    }
}
