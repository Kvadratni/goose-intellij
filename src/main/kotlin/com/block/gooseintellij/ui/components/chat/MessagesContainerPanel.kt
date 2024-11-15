package com.block.gooseintellij.ui.components.chat

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

/**
 * Container panel that manages the messages panel.
 * Uses BorderLayout to stack messages vertically.
 */
class MessagesContainerPanel : JPanel(BorderLayout()) {
    // Panel for stacking messages vertically
    private val messagesPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor.background()
    }
    
    // Scroll pane for messages
    val scrollPane = JBScrollPane(messagesPanel).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        border = null
        viewport.background = JBColor.background()
    }
    
    init {
        isOpaque = true
        background = JBColor.background()
        
        // Add scroll pane with messages
        add(scrollPane, BorderLayout.CENTER)
    }
    
    fun addMessageComponent(component: JPanel) {
        messagesPanel.add(component)
        messagesPanel.revalidate()
        messagesPanel.repaint()
        scrollToBottom()
    }
    
    fun removeMessageComponent(component: JPanel) {
        messagesPanel.remove(component)
        messagesPanel.revalidate()
        messagesPanel.repaint()
    }
    
    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val vertical = scrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }
    
    fun getMessagesPanel(): JPanel = messagesPanel
}