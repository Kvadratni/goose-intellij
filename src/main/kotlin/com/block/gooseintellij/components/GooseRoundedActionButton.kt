package com.block.gooseintellij.components

import com.intellij.ide.ui.RoundedActionButton
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Paint
import javax.swing.Icon

class GooseRoundedActionButton(icon: Icon, borderSize: Int) : RoundedActionButton(borderSize, borderSize) {

    init {
        this.icon = icon
    }

    override fun getBackgroundBorderPaint(): Paint {
        return JBColor.background()
    }

    override fun getBackgroundPaint(): Paint {
        return JBColor.background()
    }

    override fun getButtonForeground(): Color {
        return JBColor.foreground()
    }
}
