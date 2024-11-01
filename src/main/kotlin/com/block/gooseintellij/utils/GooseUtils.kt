package com.block.gooseintellij.utils

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.jediterm.terminal.TtyConnector
import java.io.File
import java.io.IOException

object GooseUtils {
    private val logger = Logger.getInstance(GooseUtils::class.java)

    private var isSqGoosePresent: Boolean? = null
    private var isGoosePresent: Boolean? = null
    private var sqPath: String? = null
    private var goosePath: String? = null
    private var toolkitToDescriptionMap: MutableMap<String, String> = mutableMapOf()
    private var providerList: MutableList<String> = mutableListOf()


    fun writeCommandToTerminal(connector: TtyConnector, command: String) {
        connector.write(command + "\r\n")
    }
    
    fun writeToTerminal(connector: TtyConnector, output: String) {
        writeCommandToTerminal(connector, "echo '$output'")
    }

    fun startGooseSession(connector: TtyConnector, project: Project) {
        val propertiesComponent = PropertiesComponent.getInstance(project)

        // Load saved profile from settings
        val savedProfile = propertiesComponent.getValue("goose.selected.profile")
        val profileArgument = savedProfile?.let { "--profile $it" } ?: ""
        val gooseInstance = if(getSqGooseState()) "sq goose" else "goose"
        val savedSessionName = propertiesComponent.getValue("goose.saved.session")
        val sessionCommand = savedSessionName?.let { "resume $it" } ?: "start"
        val command = "$gooseInstance session $sessionCommand $profileArgument"
        writeToTerminal(connector, "Starting Goose session...")
        writeCommandToTerminal(connector, command)
    }

    fun checkGooseAvailability() {
        try {
            if (isSqGoosePresent == null) {
                sqPath = getCommandOutput(arrayOf("which", "sq")).ifEmpty { "/opt/homebrew/bin/sq" }
                isSqGoosePresent = getCommandOutput(arrayOf(sqPath!!, "goose", "--version")).isNotEmpty()
                logger.info("SQ Goose present: $isSqGoosePresent, Path: $sqPath")
            }
        } catch (e: Exception) {
            isSqGoosePresent = false
            logger.error("Error checking SQ Goose availability", e)
        }

        try {
            if (isGoosePresent == null) {
                goosePath = getCommandOutput(arrayOf("which", "goose"))
                isGoosePresent = getCommandOutput(arrayOf(goosePath!!, "--version")).isNotEmpty()
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
            logger.warn("Command check failed for: ${command.joinToString(" ")}", e)
            throw e
        }
    }

    fun promptGooseInstallationIfNeeded(connector: TtyConnector, project: Project): Boolean {
        if (getSqGooseState() || getGooseState()) {
            startGooseSession(connector, project)
        } else {
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
                    writeToTerminal(connector,"Goose installation was canceled by the user.")
                }
            }
            return false
        }
        return true
    }

    fun getGooseState(): Boolean {
        return isGoosePresent ?: false
    }

    fun getSqGooseState(): Boolean {
        return isSqGoosePresent ?: false
    }

    fun getSqPath(): String {
        return sqPath ?: ""
    }

    fun getGoosePath(): String {
        return goosePath ?: ""
    }

    fun getShell(): Array<String> {
        val command: Array<String>
        val isMacOs = SystemInfo.isMac

        if (SystemInfo.isWindows) {
            command = arrayOf("cmd.exe")
        } else if (isMacOs && SystemInfo.OS_VERSION >= "10.15") {
            // Default to zsh for macOS >= 10.15
            command = arrayOf("/bin/zsh", "-l")
        } else {
            // Linux or older macOS should use bash
            command = arrayOf("/bin/bash")
        }
        return command
    }

    fun getToolkitsWithDescriptions(): Map<String, String> {
        if (toolkitToDescriptionMap.isNotEmpty()) {
            return toolkitToDescriptionMap
        }
        val commands = prependGoosePath(mutableListOf("toolkit", "list"))
        val toolkitList = ProcessBuilder(commands).start()
        toolkitToDescriptionMap = mutableMapOf<String, String>()
        val toolkitLines = toolkitList.inputStream.bufferedReader().readLines().drop(1)
        toolkitLines.forEach {
            val (name, description) = it.split(": ", limit = 2)
            toolkitToDescriptionMap[name.trim().removePrefix("- ")] = description.trim()
        }
        return toolkitToDescriptionMap
    }


    fun getAvailableProviders(): List<String> {
        if (providerList.isNotEmpty()) {
            return providerList
        }
        val commands = prependGoosePath(mutableListOf("providers", "list"))
        val providersOutput = ProcessBuilder(commands).start()
        providerList = mutableListOf<String>()
        val providerLines = providersOutput.inputStream.bufferedReader().readLines()
        providerLines.filter { it.trim().startsWith("- ") }.forEach {
            val (name) = it.split(": ", limit = 2)
            providerList.add(name.trim().removePrefix("- "))
        }
        return providerList
    }

    fun prependGoosePath(commands: MutableList<String>): MutableList<String> {
        if (getSqGooseState()) {
            commands.add(0, getSqPath())
            commands.add(1, "goose")
        } else {
            commands.add(0, getGoosePath())
        }
        return commands
    }

    fun getProjectPath(project: Project): String {
        val basePath = project.basePath!!
        if (basePath.endsWith(".ijwb")) {
            return File(basePath).parent
        }
        return basePath
    }
}
