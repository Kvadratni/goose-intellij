package com.block.gooseintellij.service

import com.block.gooseintellij.ui.components.chat.ChatPanel
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class ChatPanelService(private val project: Project) {
    private var chatPanel: ChatPanel? = null
    private var isLoading: Boolean = false

    fun getChatPanel(): ChatPanel {
        if (chatPanel == null) {
            chatPanel = ChatPanel(project)
        }
        return chatPanel!!
    }

    fun appendMessage(text: String, isUserMessage: Boolean) {
        SwingUtilities.invokeLater {
            val panel = getChatPanel()
            panel.appendMessage(text, isUserMessage)
            
            // Show the tool window if not visible
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Chat")
            toolWindow?.show()
        }
    }

    fun showLoadingIndicator() {
        if (isLoading) return
        isLoading = true
        SwingUtilities.invokeLater {
            val panel = getChatPanel()
            panel.showLoadingIndicator()
        }
    }

    fun hideLoadingIndicator() {
        if (!isLoading) return
        isLoading = false
        SwingUtilities.invokeLater {
            val panel = getChatPanel()
            panel.hideLoadingIndicator()
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ChatPanelService {
            return project.getService(ChatPanelService::class.java)
        }
    }
}