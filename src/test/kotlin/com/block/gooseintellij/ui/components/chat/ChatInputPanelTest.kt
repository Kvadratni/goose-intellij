package com.block.gooseintellij.ui.components.chat

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ChatInputPanelTest {
    private lateinit var toolWindow: ToolWindow
    private lateinit var chatInputPanel: ChatInputPanel
    
    @BeforeEach
    fun setup() {
        toolWindow = mock(ToolWindow::class.java)
        val project = mock(Project::class.java)
        // Mock necessary ToolWindow behavior
        org.mockito.Mockito.`when`(toolWindow.project).thenReturn(project)
        chatInputPanel = ChatInputPanel(mock(javax.swing.Icon::class.java), mock(com.intellij.openapi.editor.ex.EditorEx::class.java)) { _ -> }
    }
    
    @Test
    fun `test insertNewLine action adds newline character`() {
        // Get the input field from ChatInputPanel
        val inputField = chatInputPanel.javaClass.getDeclaredField("inputField").apply {
            isAccessible = true
        }.get(chatInputPanel) as javax.swing.JTextArea
        
        // Get the action map
        val actionMap = inputField.actionMap
        
        // Get the insertNewLine action
        val insertNewLineAction = actionMap.get("insertNewLine")
        assertNotNull(insertNewLineAction, "insertNewLine action should exist")
        
        // Set initial text
        inputField.text = "Test"
        
        // Trigger the action
        insertNewLineAction.actionPerformed(ActionEvent(inputField, ActionEvent.ACTION_PERFORMED, "insertNewLine"))
        
        // Verify newline was added
        assertEquals("Test\n", inputField.text)
    }
    
    @Test
    fun `test shift-enter key binding triggers insertNewLine action`() {
        // Get the input field from ChatInputPanel
        val inputField = chatInputPanel.javaClass.getDeclaredField("inputField").apply {
            isAccessible = true
        }.get(chatInputPanel) as javax.swing.JTextArea
        
        // Set initial text
        inputField.text = "Test"
        
        // Create and dispatch a shift+enter key event
        val shiftEnterEvent = KeyEvent(
            inputField,
            KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            KeyEvent.SHIFT_DOWN_MASK,
            KeyEvent.VK_ENTER,
            '\n'
        )
        
        // Get the key binding
        val binding = inputField.getInputMap().get(KeyStroke.getKeyStroke("shift ENTER"))
        assertNotNull(binding, "Shift+Enter binding should exist")
        assertEquals("insertNewLine", binding.toString(), "Binding should map to insertNewLine action")
        
        // Simulate the key press
        inputField.dispatchEvent(shiftEnterEvent)
        
        // Note: In a real test environment, we'd need to wait for event processing
        // For this test, we're just verifying the binding exists and maps correctly
    }
}
