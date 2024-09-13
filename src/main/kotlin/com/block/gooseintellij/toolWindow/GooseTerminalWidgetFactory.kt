package com.block.gooseintellij.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.wm.ToolWindowFactory
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
