package com.block.gooseintellij.ui.components.common

import com.intellij.util.ui.JBInsets
import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel

open class RoundedPanel constructor(layout: LayoutManager?, private val arcRadius: Int = 8) : JPanel(layout) {
    init {
        isOpaque = false
        cursor = Cursor.getDefaultCursor()
    }

    override fun setOpaque(isOpaque: Boolean) {
        // Always keep panel non-opaque to handle transparency properly
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            // Enable anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

            // Get component bounds accounting for insets
            val bounds = Rectangle(size)
            JBInsets.removeFrom(bounds, insets)

            // Create rounded rectangle for the panel
            val arc = arcRadius * 2
            val roundRect = RoundRectangle2D.Double(
                bounds.x.toDouble(),
                bounds.y.toDouble(),
                bounds.width.toDouble(),
                bounds.height.toDouble(),
                arc.toDouble(),
                arc.toDouble()
            )

            // Paint background if set
            if (isBackgroundSet) {
                g2.color = background
                g2.fill(roundRect)
            }

            // Set clip for content
            g2.clip = roundRect
        } finally {
            g2.dispose()
        }
    }

    override fun paintChildren(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            // Enable anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

            // Get component bounds accounting for insets
            val bounds = Rectangle(size)
            JBInsets.removeFrom(bounds, insets)

            // Create rounded rectangle for clipping
            val arc = arcRadius * 2
            val roundRect = RoundRectangle2D.Double(
                bounds.x.toDouble(),
                bounds.y.toDouble(),
                bounds.width.toDouble(),
                bounds.height.toDouble(),
                arc.toDouble(),
                arc.toDouble()
            )

            // Set clip and paint children
            g2.clip = roundRect
            super.paintChildren(g2)
        } finally {
            g2.dispose()
        }
    }

    override fun paintBorder(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            // Enable anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

            // Get component bounds accounting for insets
            val bounds = Rectangle(size)
            JBInsets.removeFrom(bounds, insets)

            // Create rounded rectangle for the border
            val arc = arcRadius * 2
            val roundRect = RoundRectangle2D.Double(
                bounds.x.toDouble(),
                bounds.y.toDouble(),
                bounds.width.toDouble(),
                bounds.height.toDouble(),
                arc.toDouble(),
                arc.toDouble()
            )

            // Paint border if set
            if (border != null) {
                border.paintBorder(this, g2, bounds.x, bounds.y, bounds.width, bounds.height)
            }
        } finally {
            g2.dispose()
        }
    }

    // Ensure repaints properly handle the rounded corners
    override fun isPaintingOrigin(): Boolean = true
}