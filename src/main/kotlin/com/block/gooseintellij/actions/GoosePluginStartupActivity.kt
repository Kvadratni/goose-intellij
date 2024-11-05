package com.block.gooseintellij.actions

import com.block.gooseintellij.ui.terminal.GooseTerminalWidget
import com.block.gooseintellij.ui.terminal.GooseTerminalWidgetFactory
import com.block.gooseintellij.utils.GooseUtils
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GoosePluginStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    // Check for Goose and SQ Goose availability and paths
    GooseUtils.checkGooseAvailability()
    GooseUtils.getAvailableProviders()
    GooseUtils.getToolkitsWithDescriptions()

    // Check for .goosehints file and create it if it does not exist
    val gooseHintsFile = File(GooseUtils.getProjectPath(project), ".goosehints")
    if (!gooseHintsFile.exists()) {
      withContext(Dispatchers.IO) {
        gooseHintsFile.createNewFile()
      }
      val gitignoreFile = File(GooseUtils.getProjectPath(project), ".gitignore")
      gitignoreFile.appendText(System.lineSeparator() + ".goosehints")
      gitignoreFile.appendText(System.lineSeparator() + ".goose")
    }

    ApplicationManager.getApplication().invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
      val contentManager = toolWindow?.contentManager
      val content = contentManager?.getContent(0)
      val gooseTerminal = content?.component as? GooseTerminalWidget

      ProgressManager.getInstance()
        .run(object : Task.Backgroundable(project, "Initializing Goose plugin") {
          override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
            gooseTerminal?.connector?.let { connector ->
              GooseUtils.promptGooseInstallationIfNeeded(connector, project)
            }
          }
        })
    }
  }

  fun reInitializeGooseTerminal(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
    val contentManager = toolWindow?.contentManager
    val content = contentManager?.getContent(0)
    val gooseTerminal = content?.component as? GooseTerminalWidget

    gooseTerminal?.connector?.let { connector ->
      val propertiesComponent = PropertiesComponent.getInstance(project)
      propertiesComponent.setValue("goose.saved.session", project.name)
      GooseUtils.writeToTerminal(connector, "exit")
      GooseUtils.writeToTerminal(connector, project.name)
      content?.let { contentManager?.removeContent(it, true) }
    }

    toolWindow?.let {
      GooseTerminalWidgetFactory().createToolWindowContent(project, it)
      val newContent = contentManager?.getContent(0)
      val newTerminal = newContent?.component as? GooseTerminalWidget
      newTerminal?.connector?.let { connector ->
        GooseUtils.writeToTerminal(connector, "Restarting session...")
        GooseUtils.startGooseSession(connector, project)
      }

      if (!it.isVisible) {
        it.activate(null)
        it.show()
      }
    }
  }
}