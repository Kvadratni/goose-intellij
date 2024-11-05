package com.block.gooseintellij.viewmodel

import com.intellij.openapi.editor.ex.EditorEx
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import javax.swing.JTextArea

class ChatViewModelTest {
    private lateinit var viewModel: ChatViewModel
    private lateinit var mockEditor: EditorEx
    private lateinit var mockInputField: JTextArea
    
    @BeforeEach
    fun setup() {
        mockEditor = mock(EditorEx::class.java)
        mockInputField = mock(JTextArea::class.java)
        viewModel = ChatViewModel(mockEditor)
    }
    
    @Test
    fun `handleTextChange triggers editor revalidate`() {
        val mockComponent = mock(JTextArea::class.java)
        `when`(mockEditor.contentComponent).thenReturn(mockComponent)
        
        viewModel.handleTextChange(mockInputField)
        
        verify(mockComponent).revalidate()
        verify(mockComponent).repaint()
    }
    
    @Test
    fun `handleSendAction processes text and calls onSend`() {
        var capturedText = ""
        val onSend: (String) -> Unit = { capturedText = it }
        
        `when`(mockInputField.text).thenReturn("test\nmessage")
        viewModel.handleSendAction(mockInputField.text, onSend)
        
        assertEquals("test message", capturedText)
    }
    
    @Test
    fun `handleSendAction ignores empty text`() {
        var called = false
        val onSend: (String) -> Unit = { called = true }
        
        `when`(mockInputField.text).thenReturn("   ")
        viewModel.handleSendAction(mockInputField.text, onSend)
        
        assertFalse(called)
    }
}
