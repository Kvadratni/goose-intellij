package com.block.gooseintellij.components

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.table.TableCellEditor

class MultiselectCellEditor(private val options: Array<String>) : DefaultCellEditor(
  MultiSelectComboBox(options)
), TableCellEditor, ActionListener {
  private lateinit var comboBox: MultiSelectComboBox

  init {
    initializeComboBox()
  }

  private fun initializeComboBox() {
    editorComponent = MultiSelectComboBox(options)
    comboBox = editorComponent as MultiSelectComboBox
    editorComponent.putClientProperty("JComboBox.isTableCellEditor", java.lang.Boolean.TRUE)
    delegate = object : EditorDelegate() {
      override fun setValue(value: Any?) {
        comboBox.selectedItem = value
      }

      override fun getCellEditorValue(): Any? {
        return comboBox.selectedItem
      }

      override fun shouldSelectCell(anEvent: EventObject?): Boolean {
        if (anEvent is MouseEvent) {
          return anEvent.id != MouseEvent.MOUSE_DRAGGED
        }
        return true
      }

      override fun stopCellEditing(): Boolean {
        if (comboBox.isEditable) {
          // Commit edited value.
          comboBox.actionPerformed(
            ActionEvent(
              this@MultiselectCellEditor, 0, ""
            )
          )
        }
        return super.stopCellEditing()
      }
    }
    comboBox.addActionListener(delegate)
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    initializeComboBox()
    comboBox.setSelectedItems((value as Collection<String>).toMutableSet())
    return comboBox
  }

  override fun getCellEditorValue(): Any {
    return comboBox.getSelectedItems()
  }

  override fun actionPerformed(e: ActionEvent?) {
    println("actionPerformed")
  }

  override fun isCellEditable(e: EventObject?): Boolean {
    return true
  }

  override fun shouldSelectCell(e: EventObject?): Boolean {
    if (e is MouseEvent) {
      return e.id != MouseEvent.MOUSE_DRAGGED
    }
    return true
  }
}
