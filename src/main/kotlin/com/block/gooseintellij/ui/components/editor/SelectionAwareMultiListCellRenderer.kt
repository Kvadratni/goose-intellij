package com.block.gooseintellij.ui.components.editor

import javax.swing.*
import java.awt.Component

class SelectionAwareMultiListCellRenderer : ListCellRenderer<Any> {
    private var selectedItems = mutableSetOf<String>()
    override fun getListCellRendererComponent(
        list: JList<out Any>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return if (index == -1) {
            JLabel(selectedItems.joinToString(", ")).apply {
                isOpaque = false
                println("Selected items: $selectedItems")
            }
        } else {
            JCheckBox(value.toString(), selectedItems.contains(value.toString())).apply {
                isOpaque = false
            }
        }
    }

    fun setSelectedItems(selectedItems: Set<String>) {
        this.selectedItems = selectedItems as MutableSet<String>
    }
}
