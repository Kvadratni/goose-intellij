package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class OpenGooseTerminalAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project
    val toolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Goose Terminal")

    if (toolWindow != null && !toolWindow.isVisible) {
      toolWindow.activate(null)
      toolWindow.show()
    }
  }
}
