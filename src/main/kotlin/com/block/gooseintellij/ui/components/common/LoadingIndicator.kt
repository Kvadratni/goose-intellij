package com.block.gooseintellij.ui.components.common

import com.block.gooseintellij.icons.AnimatedGooseIcon
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.SwingUtilities

class LoadingIndicator : JPanel(BorderLayout()) {
    private val animatedIcon = AnimatedGooseIcon()
    
    private var isLoading = false
    
    init {
        isOpaque = false
        border = JBUI.Borders.empty(4)
        add(animatedIcon, BorderLayout.CENTER)
        // Start visible but with animation suspended
        isVisible = true
        animatedIcon.suspend()
    }
    
    fun startLoading() {
        if (isLoading) return
        
        isLoading = true
        ensureEDT {
            isVisible = true
            revalidate()
            repaint()
            animatedIcon.resume()
        }
    }
    
    fun stopLoading() {
        if (!isLoading) return
        
        isLoading = false
        ensureEDT {
            animatedIcon.suspend()
            // Keep the component visible but suspended to maintain layout
            revalidate()
            repaint()
        }
    }
    
    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (!visible) {
            stopLoading()
        }
    }
    
    private fun ensureEDT(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            block()
        } else {
            ApplicationManager.getApplication().invokeLater(block)
        }
    }
    
    override fun getPreferredSize(): Dimension {
        return if (isLoading) {
            super.getPreferredSize()
        } else {
            Dimension(0, 0) // Collapse when not loading
        }
    }
}
