package com.block.gooseintellij.ui.chat

import com.block.gooseintellij.service.ChatPanelService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class GooseChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            
        val chatPanel = ChatPanelService.getInstance(project).getChatPanel()
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
