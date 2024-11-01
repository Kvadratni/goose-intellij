package com.block.gooseintellij.ui.terminal

import com.block.gooseintellij.actions.GoosePluginStartupActivity
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.wm.ToolWindowFactory
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi

class GooseTerminalWidgetFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GooseTerminalWidget(toolWindow)
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

        // Compute preferred initial size (30% width of the editor window)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editorWindow = fileEditorManager.getSelectedEditor()?.component?.parent
        val editorWindowWidth = editorWindow?.width ?: 0
        val desiredWidth = (editorWindowWidth * 0.3).toInt()
        panel.preferredSize = Dimension(desiredWidth, editorWindow?.height ?: 600)

        // Add Action with Gear Icon
        val restartAction = object : com.intellij.openapi.actionSystem.AnAction(
            "Restart Shell", 
            "Restart the Shell", 
            AllIcons.Actions.Restart
        ) {
            override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                GoosePluginStartupActivity().reInitializeGooseTerminal(project)
            }
        }

        toolWindow.setTitleActions(listOf(restartAction))

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
        val content = contentFactory.createContent(panel, "Goose Chat", false)
        toolWindow.contentManager.addContent(content)
    }
}