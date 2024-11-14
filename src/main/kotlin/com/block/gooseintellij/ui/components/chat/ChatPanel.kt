package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.service.GooseChatService
import com.block.gooseintellij.utils.GooseIcons
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.BoxLayout

class ChatPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val chatService: GooseChatService = project.getService(GooseChatService::class.java)
    
    // Message container with BoxLayout for vertical stacking
    private val messagesPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor.background()
    }
    
    private val scrollPane = JBScrollPane(messagesPanel).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        border = JBUI.Borders.empty()
        viewport.background = JBColor.background()
    }

    private val inputPanel = ChatInputPanel(
        icon = GooseIcons.SendToGoose,
        sendAction = { message -> handleSendMessage(message) }
    )

    init {
        isOpaque = true
        background = JBColor.background()
        
        val mainPanel = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = JBColor.background()
            add(scrollPane, BorderLayout.CENTER)
            border = JBUI.Borders.empty(10)
        }
        
        add(mainPanel, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        // Set preferred size for better initial display
        preferredSize = JBUI.size(400, 600)
        minimumSize = JBUI.size(200, 200)
        
        // Start the goosed process when panel is created
        chatService.startGoosedProcess().thenRun {
            appendSystemMessage("Goose Chat initialized and ready!")
            revalidate()
            repaint()
        }.exceptionally { e ->
            appendSystemMessage("Error initializing Goose Chat: ${e.message}")
            revalidate()
            repaint()
            null
        }
    }

    private fun handleSendMessage(message: String) {
        // Add user message bubble
        addMessageBubble(message, true)
        
        // Start streaming response
        chatService.sendMessage(
            message = message,
            streaming = true,
            streamHandler = object : GooseChatService.StreamHandler {
                private var responseBubble: ChatBubbleComponent? = null
                
                override fun onText(text: String) {
                    SwingUtilities.invokeLater {
                        if (responseBubble == null) {
                            responseBubble = addMessageBubble(text, false)
                            messagesPanel.revalidate()
                            messagesPanel.repaint()
                        } else {
                            // Update with minimal repaints
                            updateLastMessageBubble(text)
                        }
                        scrollToBottom()
                    }
                }
                
                override fun onData(data: List<Any>) {
                    // Handle structured data if needed
                }
                
                override fun onError(error: String) {
                    SwingUtilities.invokeLater {
                        addMessageBubble("Error: $error", false)
                    }
                }
                
                override fun onMessageAnnotation(annotation: Map<String, Any>) {
                    // Handle annotations if needed
                }
                
                override fun onFinish(finishReason: String, usage: Map<String, Int>) {
                    SwingUtilities.invokeLater {
                        responseBubble = null
                        scrollToBottom()
                    }
                }
            }
        ).exceptionally { e ->
            addMessageBubble("Error sending message: ${e.message}", false)
            null
        }
    }

    private fun addMessageBubble(message: String, isUserMessage: Boolean): ChatBubbleComponent {
        val wrapper = JPanel(FlowLayout(if (isUserMessage) FlowLayout.RIGHT else FlowLayout.LEFT, 0, 2)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        
        val bubble = ChatBubbleComponent(message, isUserMessage)
        wrapper.add(bubble)
        
        messagesPanel.add(wrapper)
        messagesPanel.revalidate()
        messagesPanel.repaint()
        scrollToBottom()
        
        return bubble
    }
    
    private fun updateLastMessageBubble(text: String) {
        val lastComponent = messagesPanel.components.lastOrNull()
        if (lastComponent is JPanel) {
            val bubble = lastComponent.components.firstOrNull { it is ChatBubbleComponent }
            if (bubble is ChatBubbleComponent) {
                bubble.setText(text)
                lastComponent.revalidate()
                lastComponent.repaint()
                messagesPanel.revalidate()
                messagesPanel.repaint()
                scrollToBottom()
            }
        }
    }

    private fun appendSystemMessage(message: String) {
        addMessageBubble(message, false)
    }
    
    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val vertical = scrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }

    fun dispose() {
        chatService.stopGoosedProcess()
    }
}
