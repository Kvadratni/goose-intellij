package com.block.gooseintellij.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.AnimatedIcon
import javax.swing.Icon

/**
 * Animated icon that cycles through different goose flight positions.
 * Uses IntelliJ's AnimatedIcon for smooth animation handling.
 */
class AnimatedGooseIcon : AnimatedIcon(
    "fly",
    generateFrames(),
    IconLoader.getIcon("/icons/fly_frames/0.svg", AnimatedGooseIcon::class.java),
    DEFAULT_CYCLE_LENGTH
) {
    companion object {
        private const val DEFAULT_CYCLE_LENGTH = 800 // Total animation duration in ms
        private const val FRAME_COUNT = 7 // Number of frames (0-6)
        
        private fun generateFrames(): Array<Icon> {
            return (0 until FRAME_COUNT).map { frameIndex ->
                val resourcePath = "/icons/fly_frames/$frameIndex.svg"
                IconLoader.getIcon(resourcePath, AnimatedGooseIcon::class.java)
            }.toTypedArray()
        }
    }
}
