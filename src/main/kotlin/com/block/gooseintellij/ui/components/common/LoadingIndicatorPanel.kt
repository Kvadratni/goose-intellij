package com.block.gooseintellij.ui.components.common

import com.intellij.ui.JBColor
import java.awt.FlowLayout
import javax.swing.JPanel

/**
 * Panel that wraps the LoadingIndicator with proper layout and background handling.
 * Used for consistent loading indicator display across the application.
 */
class LoadingIndicatorPanel : JPanel(FlowLayout(FlowLayout.CENTER)) {
    private val loadingIndicator = LoadingIndicator()

    init {
        isOpaque = false
        add(loadingIndicator)
        isVisible = false  // Initially hidden
    }

    fun startLoading() {
        loadingIndicator.startLoading()
        isVisible = true
        revalidate()
        repaint()
    }

    fun stopLoading() {
        loadingIndicator.stopLoading()
        isVisible = false
        revalidate()
        repaint()
    }
}