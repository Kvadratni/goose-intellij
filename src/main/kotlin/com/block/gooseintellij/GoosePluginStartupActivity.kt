package com.block.gooseintellij

import com.block.gooseintellij.toolWindow.GooseTerminalPanel
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
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

            toolWindow?.show(null)

            val contentManager = toolWindow?.contentManager
            val content = contentManager?.getContent(0)
            val gooseTerminalPanel = content?.component as? GooseTerminalPanel

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
                                    gooseTerminalPanel?.printOutput("Goose installation was canceled by the user.")
                                }
                            }
                        } else {
                            startGooseSession(true, gooseTerminalPanel)
                        }
                    } else {
                        startGooseSession(false, gooseTerminalPanel)
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

    private fun startGooseSession(usingSqGoose: Boolean, gooseTerminalPanel: GooseTerminalPanel?) {
        gooseTerminalPanel?.printOutput("Starting Goose session...")

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Running Goose session", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val commandLine = if (usingSqGoose) GeneralCommandLine("sq", "goose", "session", "start")
                                  else GeneralCommandLine("goose", "session", "start")
                try {
                    val processHandler = OSProcessHandler(commandLine)
                    processHandler.startNotify()
                    gooseTerminalPanel?.attachToProcess(processHandler)
                    processHandler.waitFor()
                    ApplicationManager.getApplication().invokeLater {
                        gooseTerminalPanel?.printOutput("Goose session started.")
                    }
                } catch (e: ExecutionException) {
                    ApplicationManager.getApplication().invokeLater {
                        gooseTerminalPanel?.printOutput("Failed to start Goose session.")
                    }
                }
            }
        })
    }
}
