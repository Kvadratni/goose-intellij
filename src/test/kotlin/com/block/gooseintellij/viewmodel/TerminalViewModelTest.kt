package com.block.gooseintellij.viewmodel

import com.intellij.openapi.project.Project
import com.jediterm.terminal.TtyConnector
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class TerminalViewModelTest {
    private lateinit var viewModel: TerminalViewModel
    private lateinit var mockProject: Project
    
    @BeforeEach
    fun setup() {
        mockProject = mock(Project::class.java)
        viewModel = TerminalViewModel(mockProject)
    }
    
    @Test
    fun `createTerminalSettings returns valid settings`() {
        val settings = viewModel.createTerminalSettings()
        assertNotNull(settings)
    }
    
    @Test
    fun `getInitialCommands returns expected commands`() {
        val commands = viewModel.getInitialCommands()
        assertTrue(commands.isNotEmpty())
        assertTrue(commands.any { it.startsWith("cd ") })
        assertTrue(commands.any { it.contains("goose") })
        assertTrue(commands.any { it.contains("clear") || it.contains("cls") })
    }
    
    @Test
    fun `writeCommandToTerminal writes command with newline`() {
        val mockConnector = mock(TtyConnector::class.java)
        val command = "test command"
        
        TerminalViewModel.writeCommandToTerminal(mockConnector, command)
        verify(mockConnector).write(command + "\r\n")
    }
}