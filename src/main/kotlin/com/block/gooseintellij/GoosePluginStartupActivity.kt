package com.block.gooseintellij

import com.block.gooseintellij.toolWindow.GooseTerminalWidget
import com.block.gooseintellij.toolWindow.GooseTerminalWidgetFactory
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
import com.intellij.ide.util.PropertiesComponent

class GoosePluginStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    ApplicationManager.getApplication().invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
      val contentManager = toolWindow?.contentManager
      val content = contentManager?.getContent(0)
      val gooseTerminal = content?.component as? GooseTerminalWidget


      ProgressManager.getInstance()
        .run(object : Task.Backgroundable(project, "Initializing Goose plugin") {
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
                startGooseSession(true, gooseTerminal, project)
              }
            } else {
              startGooseSession(false, gooseTerminal, project)
            }
          }
        })
    }
  }

  private fun canInitializeGoose(useSQ: Boolean): Boolean {
    return try {
      val commandLine =
        if (useSQ) GeneralCommandLine("sq", "goose") else GeneralCommandLine("goose")
      val processHandler = OSProcessHandler(commandLine)
      processHandler.startNotify()
      processHandler.waitFor()
      true
    } catch (error: ExecutionException) {
      false
    }
  }

  private fun startGooseSession(
    usingSqGoose: Boolean,
    gooseTerminal: GooseTerminalWidget?,
    project: Project,
  ) {
    val propertiesComponent = PropertiesComponent.getInstance(project)

    // Load saved profile from settings
    val savedProfile = propertiesComponent.getValue("goose.selected.profile")
    val profileArgument = savedProfile?.let { "--profile $it" } ?: ""
    val gooseInstance = if (usingSqGoose) "sq goose" else "goose"
    val savedSessionName = propertiesComponent.getValue("goose.saved.session")
    val sessionCommand = savedSessionName?.let { "resume $it" } ?: "start"
    val command = "$gooseInstance session $sessionCommand $profileArgument"
    gooseTerminal?.writeToTerminal("Starting Goose session...")
    GooseTerminalWidget.writeCommandToTerminal(gooseTerminal?.connector!!, command)
  }

  fun reInitializeGooseTerminal(project: Project) {
    var toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
    var contentManager = toolWindow?.contentManager
    var content = contentManager?.getContent(0)
    var gooseTerminal = content?.component as? GooseTerminalWidget

    if (gooseTerminal != null) {
      val propertiesComponent = PropertiesComponent.getInstance(project)
      propertiesComponent.setValue("goose.saved.session", project.name)
      gooseTerminal.writeToTerminal("exit")
      gooseTerminal.writeToTerminal(project.name)
      contentManager?.removeContent(content!!, true)
    }
    GooseTerminalWidgetFactory().createToolWindowContent(project, toolWindow!!)
    contentManager = toolWindow.contentManager
    content = contentManager.getContent(0)
    gooseTerminal = content?.component as? GooseTerminalWidget
    gooseTerminal!!.writeToTerminal("Restarting session...")
    startGooseSession(true, gooseTerminal, project);
  }
}
