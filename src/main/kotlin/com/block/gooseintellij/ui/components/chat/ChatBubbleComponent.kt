package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.ui.components.common.RoundedPanel
import com.block.gooseintellij.ui.components.common.VolatileImageBufferingPainter
import com.block.gooseintellij.utils.MarkdownRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.DefaultEditorKit

class ChatBubbleComponent(
    message: String,
    val isUserMessage: Boolean,
    private val timestamp: LocalDateTime = LocalDateTime.now()
) : RoundedPanel(BorderLayout(), 12) {

    companion object {
        private const val MAX_WIDTH = 4000  // Maximum width of chat bubble
        // Use theme-aware colors for chat bubbles
        private val USER_BUBBLE_COLOR = JBColor(
            Color(227, 242, 253, 230),  // Light theme: Soft blue with alpha
            Color(43, 43, 43, 230)      // Dark theme: Soft dark gray with alpha
        )
        private val GOOSE_BUBBLE_COLOR = JBColor(
            Color(245, 245, 245, 200),  // Light theme: Light gray with alpha
            Color(60, 63, 65, 200)      // Dark theme: Dark gray with alpha
        )
        private val HOVER_TINT = JBColor(
            Color(0, 0, 0, 10),         // Light theme: Slight darkening
            Color(255, 255, 255, 10)    // Dark theme: Slight lightening
        )
        private const val WIDTH_PERCENTAGE = 0.9  // 90% of parent width
        private const val MIN_WIDTH = 200
        private const val MAX_LINE_COUNT = 15  // Maximum number of visible lines before scrolling
        private const val HORIZONTAL_PADDING = 12
        private const val VERTICAL_PADDING = 8
        private const val CONTENT_PADDING = 5
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    private var isHovered = false
    
    private var messageArea: JEditorPane
    private var parentWidth: Int = 0
    
    init {
        background = if (isUserMessage) USER_BUBBLE_COLOR else GOOSE_BUBBLE_COLOR
        
        // Outer padding for bubble spacing
        border = EmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING)
        
        // Add component listener to handle parent resizing
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                val newParentWidth = parent?.width ?: 0
                if (newParentWidth != parentWidth && newParentWidth > 0) {
                    parentWidth = newParentWidth
                    revalidate()
                    repaint()
                }
            }
        })
        
        val contentPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = EmptyBorder(CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING)
        }
        
        // Create a Markdown-enabled editor pane for the message
        messageArea = MarkdownRenderer.createMarkdownPane().apply {
            background = background
            border = null
        }
        
        // Set initial content
        MarkdownRenderer.setMarkdownContent(messageArea, message)
        
        // Create a scrollable panel for the message
        val scrollPane = JBScrollPane(messageArea).apply {
            border = null
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            
            // Calculate dimensions based on line height like ChatInputPanel
            val lineHeight = messageArea.getFontMetrics(messageArea.font).height
            val preferredWidth = messageArea.preferredSize.width.coerceIn(MIN_WIDTH, MAX_WIDTH)
            val maxHeight = lineHeight * MAX_LINE_COUNT
            
            // Set size constraints
            preferredSize = null
            maximumSize = Dimension(MAX_WIDTH, maxHeight)
            
            // Only show scrollbar when content exceeds MAX_LINE_COUNT lines
            if (messageArea.preferredSize.height > maxHeight) {
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            } else {
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_NEVER
            }
        }
        
        val timestampLabel = JLabel(timestamp.format(TIME_FORMATTER)).apply {
            font = JBUI.Fonts.create("Monospaced", 10)
            foreground = JBColor.GRAY
        }
        
        val bottomPanel = JPanel(FlowLayout(
            if (isUserMessage) FlowLayout.RIGHT else FlowLayout.LEFT,
            2, 0
        )).apply {
            isOpaque = false
            add(timestampLabel)
        }
        
        contentPanel.add(scrollPane, BorderLayout.CENTER)
        contentPanel.add(bottomPanel, BorderLayout.SOUTH)
        
        add(contentPanel, BorderLayout.CENTER)
        
        // Add hover effect
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                isHovered = true
                background = if (isUserMessage)
                    blend(USER_BUBBLE_COLOR, HOVER_TINT)
                else
                    blend(GOOSE_BUBBLE_COLOR, HOVER_TINT)
                repaint()
            }
            
            override fun mouseExited(e: MouseEvent) {
                isHovered = false
                background = if (isUserMessage) USER_BUBBLE_COLOR else GOOSE_BUBBLE_COLOR
                repaint()
            }
        })
    }
    
    private fun blend(c1: Color, c2: Color): Color {
        val r = (c1.red * (255 - c2.alpha) + c2.red * c2.alpha) / 255
        val g = (c1.green * (255 - c2.alpha) + c2.green * c2.alpha) / 255
        val b = (c1.blue * (255 - c2.alpha) + c2.blue * c2.alpha) / 255
        val a = c1.alpha
        return Color(r, g, b, a)
    }
    
    fun setText(text: String) {
        messageArea?.let { area ->
            MarkdownRenderer.setMarkdownContent(area, text)
            area.caretPosition = 0  // Reset scroll position to top
        }
    }
    
    fun getText(): String {
        return messageArea?.text ?: ""
    }
    
    override fun getPreferredSize(): Dimension {
        val parentWidth = parent?.width ?: MIN_WIDTH
        val bubbleWidth = (parentWidth * WIDTH_PERCENTAGE).toInt().coerceAtLeast(MIN_WIDTH)
        
        // Get the preferred height based on the content
        val contentPreferredSize = super.getPreferredSize()
        
        return Dimension(bubbleWidth, contentPreferredSize.height)
    }
}
