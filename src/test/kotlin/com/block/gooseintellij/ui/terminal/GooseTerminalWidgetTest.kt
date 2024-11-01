package com.block.gooseintellij.ui.terminal

import com.block.gooseintellij.viewmodel.TerminalViewModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class GooseTerminalWidgetTest {
    private lateinit var toolWindow: ToolWindow
    private lateinit var mockProject: Project
    private lateinit var terminalWidget: GooseTerminalWidget
    
    @BeforeEach
    fun setup() {
        toolWindow = mock(ToolWindow::class.java)
        mockProject = mock(Project::class.java)
        `when`(toolWindow.project).thenReturn(mockProject)
        
        terminalWidget = GooseTerminalWidget(toolWindow)
    }
    
    @Test
    fun `constructor initializes with correct layout`() {
        assertEquals(java.awt.BorderLayout::class.java, terminalWidget.layout.javaClass)
    }
    
    @Test
    fun `terminal widget has expected size`() {
        assertEquals(300, terminalWidget.preferredSize.width)
        assertEquals(600, terminalWidget.preferredSize.height)
    }
    
    @Test
    fun `connector is initialized`() {
        assertNotNull(terminalWidget.connector)
    }
}