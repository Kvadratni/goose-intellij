package com.block.gooseintellij.components

import javax.swing.*

class MultiSelectComboBox(options: Array<String>) : JComboBox<String>(options) {
    private val selectedItems = mutableSetOf<String>()

    init {
        renderer = SelectionAwareMultiListCellRenderer()
        addActionListener {
            val selectedItem = selectedItem as? String
            if (selectedItem != null) {
                if (selectedItems.contains(selectedItem)) {
                    selectedItems.remove(selectedItem)
                } else {
                    selectedItems.add(selectedItem)
                }
            }
            (renderer as SelectionAwareMultiListCellRenderer).setSelectedItems(selectedItems)
        }
        setEditable(false)
    }

    fun setSelectedItems(selectedItems: Set<String>) {
        this.selectedItems.clear()
        this.selectedItems.addAll(selectedItems)
        (renderer as SelectionAwareMultiListCellRenderer).setSelectedItems(selectedItems)
    }

    fun getSelectedItems(): Set<String> {
        return selectedItems
    }

}
