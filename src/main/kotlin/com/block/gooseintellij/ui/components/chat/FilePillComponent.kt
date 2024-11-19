package com.block.gooseintellij.ui.components.chat

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.BorderFactory

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.IconUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.icons.AllIcons

class FilePillComponent(
    private val project: Project,
    val file: VirtualFile,
    private val showCloseButton: Boolean = false,
    val onRemove: (String) -> Unit
) : JPanel() {
    private val closeButton = JButton(AllIcons.Actions.Close).apply {
        preferredSize = Dimension(16, 16)
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        border = BorderFactory.createEmptyBorder(6, 0, 6, 10)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        addActionListener {
            onRemove(file.name)
        }
    }
    
    companion object {
        private val BACKGROUND_COLOR = JBColor(
            Color(240, 240, 240),  // Light theme
            Color(60, 63, 65)      // Dark theme
        )
        private val HOVER_COLOR = JBColor(
            Color(230, 230, 230),  // Light theme
            Color(70, 73, 75)      // Dark theme
        )
        private const val ARC_SIZE = 12

        /**
         * Checks if a file path already exists in a collection of FilePillComponents
         */
        fun hasFilePathInPills(filePath: String, pills: Map<FilePillComponent, String>): Boolean {
            return pills.values.any { it == filePath }
        }

        /**
         * Filters out any duplicate file paths from a collection of FilePillComponents
         */
        fun getUniquePills(pills: Map<FilePillComponent, String>): Map<FilePillComponent, String> {
            val uniquePaths = mutableSetOf<String>()
            return pills.filterTo(mutableMapOf()) { (_, path) ->
                uniquePaths.add(path)
            }
        }
    }

    private var isHovered = false

    init {
        layout = BorderLayout(5, 0)  // Add some spacing between components
        isOpaque = false
        
        val label = JBLabel(file.name, IconUtil.getIcon(file, 1, project), JBLabel.LEFT).apply {
            border = JBUI.Borders.empty(6, 10)  // Increased padding
            foreground = JBColor.foreground()
        }
        
        // Create content panel for the label
        val contentPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(label, BorderLayout.CENTER)
            
            // Add click listener to focus the file
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    isHovered = true
                    repaint()
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                }
                
                override fun mouseExited(e: MouseEvent) {
                    isHovered = false
                    repaint()
                    cursor = Cursor.getDefaultCursor()
                }
                
                override fun mouseClicked(e: MouseEvent) {
                    if (showCloseButton) {
                        // When showCloseButton is true, clicking the pill focuses the file
                        FileEditorManager.getInstance(project).openFile(file, true)
                    } else {
                        // When showCloseButton is false, clicking the pill triggers removal
                        onRemove(file.name)
                    }
                }
            })
        }
        
        // Add an empty border around the entire pill for better spacing
        border = JBUI.Borders.empty(2, 2)
        
        add(contentPanel, BorderLayout.CENTER)
        
        // Add close button if enabled
        if (showCloseButton) {
            add(closeButton, BorderLayout.EAST)
        }
    }

    fun removeButton() {
        remove(closeButton)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = if (isHovered) HOVER_COLOR else BACKGROUND_COLOR
            g2.fillRoundRect(0, 0, width, height, ARC_SIZE, ARC_SIZE)
        } finally {
            g2.dispose()
        }
    }
}
