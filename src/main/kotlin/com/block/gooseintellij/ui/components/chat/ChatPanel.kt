package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.service.GooseChatService
import com.block.gooseintellij.utils.GooseIcons
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class ChatPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val chatService: GooseChatService = project.getService(GooseChatService::class.java)
    
    // Track the current conversation state
    private var currentConversation = StringBuilder()
    private var currentResponse = StringBuilder()
    
    private val outputArea: JTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = JBColor.background()
        margin = JBUI.insets(10)
        font = JBUI.Fonts.create("Monospaced", 12)
    }
    
    private val scrollPane = JBScrollPane(outputArea).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        border = JBUI.Borders.empty()
        viewport.background = JBColor.background()
    }

    private val inputPanel = JTextArea().apply{
        isEditable = true
        lineWrap = true
        wrapStyleWord = true
        background = JBColor.background()
        margin = JBUI.insets(10)
        val padding = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
        border = BorderFactory.createCompoundBorder(padding, JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0))
        toolTipText = "Type a message and press Enter to send"
        font = JBUI.Fonts.create("Monospaced", 12)
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && e.modifiers == 0) {
                    e.consume()
                    val message = text.trim()
                    if (message.isNotEmpty()) {
                        handleSendMessage(message)
                        text = ""
                    }
                }
            }
        })
    }

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
        // Add user message to conversation
        currentConversation.append("You: ").append(message).append("\n\n")
        currentResponse.setLength(0) // Clear the current response
        
        // Update display with user message and start assistant message
        updateDisplay()
        currentConversation.append("Goose: ")
        updateDisplay()
        
        chatService.sendMessage(
            message = message,
            streaming = true,
            streamHandler = object : GooseChatService.StreamHandler {
                override fun onText(text: String) {
                    SwingUtilities.invokeLater {
                        // Update current response with new text
                        currentResponse.setLength(0)
                        currentResponse.append(text)
                        updateDisplay()
                    }
                }
                
                override fun onData(data: List<Any>) {
                    // Handle structured data if needed
                }
                
                override fun onError(error: String) {
                    SwingUtilities.invokeLater {
                        currentResponse.setLength(0)
                        currentResponse.append("Error: ").append(error)
                        updateDisplay()
                    }
                }
                
                override fun onMessageAnnotation(annotation: Map<String, Any>) {
                    // Handle annotations if needed
                }
                
                override fun onFinish(finishReason: String, usage: Map<String, Int>) {
                    SwingUtilities.invokeLater {
                        // Add the final response to the conversation
                        currentConversation.append(currentResponse).append("\n\n")
                        currentResponse.setLength(0)
                        updateDisplay()
                    }
                }
            }
        ).exceptionally { e ->
            currentResponse.setLength(0)
            currentResponse.append("Error sending message: ").append(e.message)
            updateDisplay()
            null
        }
    }

    private fun appendSystemMessage(message: String) {
        currentConversation.append("System: ").append(message).append("\n\n")
        updateDisplay()
    }
    
    private fun updateDisplay() {
        // Combine current conversation with current response
        val fullText = StringBuilder(currentConversation)
        if (currentResponse.isNotEmpty()) {
            fullText.append(currentResponse)
        }
        
        // Update the output area
        outputArea.text = fullText.toString()
        outputArea.caretPosition = outputArea.document.length
    }

    fun dispose() {
        chatService.stopGoosedProcess()
    }
}
