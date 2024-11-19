package com.block.gooseintellij.ui.components.chat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class ChatInputPanelTest {
    private lateinit var toolWindow: ToolWindow
    private lateinit var chatInputPanel: ChatInputPanel
    private lateinit var project: Project
    private lateinit var virtualFile: VirtualFile
    
    @BeforeEach
    fun setup() {
        toolWindow = mock(ToolWindow::class.java)
        project = mock(Project::class.java)
        virtualFile = mock(VirtualFile::class.java)
        
        // Mock necessary behavior
        `when`(toolWindow.project).thenReturn(project)
        `when`(virtualFile.name).thenReturn("test.kt")
        `when`(virtualFile.path).thenReturn("/test/path/test.kt")
        
        chatInputPanel = ChatInputPanel(project, mock(javax.swing.Icon::class.java), mock(com.intellij.openapi.editor.ex.EditorEx::class.java)) { _, _ -> }
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
    
    @Test
    fun `test file pill panel visibility`() {
        // Initially, file pill panel should not be visible
        assertNull(chatInputPanel.javaClass.getDeclaredField("filePillPanel").apply {
            isAccessible = true
        }.get(chatInputPanel))
        
        // Add a file pill
        chatInputPanel.appendFileTag("test.kt")
        
        // File pill panel should now be visible
        assertNotNull(chatInputPanel.javaClass.getDeclaredField("filePillPanel").apply {
            isAccessible = true
        }.get(chatInputPanel))
        
        // Get the pills map
        val filePills = chatInputPanel.getFilePills()
        assertEquals(1, filePills.size)
        assertEquals("/test/path/test.kt", filePills.values.first())
    }
    
    @Test
    fun `test file pill map integration`() {
        // Add multiple files
        chatInputPanel.appendFileTag("test.kt")
        
        // Create second virtual file mock
        val virtualFile2 = mock(VirtualFile::class.java)
        `when`(virtualFile2.name).thenReturn("other.kt")
        `when`(virtualFile2.path).thenReturn("/test/path/other.kt")
        
        // Get the pills map
        var filePills = chatInputPanel.getFilePills()
        
        // Verify initial state
        assertEquals(1, filePills.size)
        assertTrue(filePills.values.contains("/test/path/test.kt"))
        
        // Try to add the same file again (should not duplicate)
        chatInputPanel.appendFileTag("test.kt")
        filePills = chatInputPanel.getFilePills()
        assertEquals(1, filePills.size)
        
        // Verify file paths are unique
        assertEquals(1, FilePillComponent.getUniquePills(filePills).size)
    }
    
    @Test
    fun `test pill removal and cleanup`() {
        // Add a file pill
        chatInputPanel.appendFileTag("test.kt")
        
        // Get the pill component and trigger removal
        val filePills = chatInputPanel.getFilePills()
        assertEquals(1, filePills.size)
        
        val pill = filePills.keys.first()
        pill.onRemove(pill.file.name)
        
        // Verify pill was removed
        val updatedPills = chatInputPanel.getFilePills()
        assertTrue(updatedPills.isEmpty())
        
        // Verify panel was cleaned up
        assertNull(chatInputPanel.javaClass.getDeclaredField("filePillPanel").apply {
            isAccessible = true
        }.get(chatInputPanel))
    }
}
