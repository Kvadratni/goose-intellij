package com.block.gooseintellij.ui.chat

import com.block.gooseintellij.ui.components.chat.ChatPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager

class GooseChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            
        val chatPanel = ChatPanel(project)
        val content = ContentFactory.getInstance()
            .createContent(chatPanel, "Chat", false)
        content.isCloseable = false
        
        toolWindow.contentManager.addContent(content)
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.isAutoHide = false
        
        // Ensure the tool window is visible
        toolWindow.show()
    }
}
