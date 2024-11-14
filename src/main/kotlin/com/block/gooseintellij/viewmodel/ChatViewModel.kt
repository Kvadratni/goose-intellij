package com.block.gooseintellij.viewmodel

import com.intellij.openapi.editor.ex.EditorEx
import javax.swing.JTextArea

class ChatViewModel(private val editor: EditorEx? = null) {
    fun handleTextChange(inputField: JTextArea) {
        updateEditorLayout()
    }
    
    fun handleSendAction(text: String, onSend: (String) -> Unit) {
        val trimmedText = text.trim()
        if (trimmedText.isNotEmpty()) {
            onSend(trimmedText.replace("\n", " "))
        }
    }
    
    private fun updateEditorLayout() {
        editor?.contentComponent?.let { component ->
            component.revalidate()
            component.repaint()
        }
    }
    
    companion object {
        const val MAX_LINE_COUNT = 25
    }
}