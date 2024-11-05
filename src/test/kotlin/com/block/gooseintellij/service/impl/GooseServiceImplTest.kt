package com.block.gooseintellij.service.impl

import com.block.gooseintellij.service.SessionService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class GooseServiceImplTest {
    private lateinit var gooseService: GooseServiceImpl
    private lateinit var mockProject: Project
    private lateinit var mockSessionService: SessionService
    
    @BeforeEach
    fun setup() {
        mockProject = mock(Project::class.java)
        mockSessionService = mock(SessionService::class.java)
        gooseService = GooseServiceImpl(mockProject, mockSessionService)
    }
    
    @Test
    fun `initialize creates new session`() {
        gooseService.initialize()
        verify(mockSessionService).createSession()
    }
    
    @Test
    fun `isSessionActive returns session service state`() {
        `when`(mockSessionService.hasActiveSession()).thenReturn(true)
        assertTrue(gooseService.isSessionActive())
        
        `when`(mockSessionService.hasActiveSession()).thenReturn(false)
        assertFalse(gooseService.isSessionActive())
    }
    
    @Test
    fun `terminateSession ends session`() {
        gooseService.terminateSession()
        verify(mockSessionService).endSession()
    }
    
    @Test
    fun `executeCommand throws exception when no active session`() {
        `when`(mockSessionService.hasActiveSession()).thenReturn(false)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { gooseService.executeCommand("test") }
        }
    }
}
