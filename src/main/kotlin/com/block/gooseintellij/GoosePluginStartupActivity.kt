package com.block.gooseintellij

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class GoosePluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
            val contentManager = toolWindow?.contentManager
            val content = contentManager?.getContent(0)
            val gooseTerminal = content?.component as? GooseTerminalWidget


            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Initializing Goose plugin") {
                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    if (!canInitializeGoose(false)) {
                        if (!canInitializeGoose(true)) {
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
                        } else {
                            startGooseSession(true, gooseTerminal)
                        }
                    } else {
                        startGooseSession(false, gooseTerminal)
                    }
                }
            })
        }
    }

    private fun canInitializeGoose(useSQ: Boolean): Boolean {
        return try {
            val commandLine = if (useSQ) GeneralCommandLine("sq", "goose") else GeneralCommandLine("goose")
            val processHandler = OSProcessHandler(commandLine)
            processHandler.startNotify()
            processHandler.waitFor()
            true
        } catch (error: ExecutionException) {
            false
        }
    }

    private fun startGooseSession(usingSqGoose: Boolean, gooseTerminal: GooseTerminalWidget?) {
        val command = if (usingSqGoose) "sq goose session start" else "goose session start"
        gooseTerminal?.writeToTerminal("Starting Goose session...")
        GooseTerminalWidget.writeCommandToTerminal(gooseTerminal?.connector!!, command)
    }
}
