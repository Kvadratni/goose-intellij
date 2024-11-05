package com.block.gooseintellij.service.impl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SessionServiceImplTest {
    private lateinit var sessionService: SessionServiceImpl

    @BeforeEach
    fun setup() {
        sessionService = SessionServiceImpl()
    }

    @Test
    fun `test create session creates new session and returns id`() {
        val sessionId = sessionService.createSession()
        assertNotNull(sessionId)
        assertTrue(sessionService.hasActiveSession())
        assertEquals(sessionId, sessionService.getCurrentSessionId())
    }

    @Test
    fun `test end session terminates active session`() {
        val sessionId = sessionService.createSession()
        assertTrue(sessionService.hasActiveSession())
        
        sessionService.endSession()
        assertFalse(sessionService.hasActiveSession())
        assertNull(sessionService.getCurrentSessionId())
    }

    @Test
    fun `test create session ends previous session`() {
        val firstSessionId = sessionService.createSession()
        val secondSessionId = sessionService.createSession()
        
        assertNotEquals(firstSessionId, secondSessionId)
        assertTrue(sessionService.hasActiveSession())
        assertEquals(secondSessionId, sessionService.getCurrentSessionId())
    }

    @Test
    fun `test getCurrentSessionId returns null when no active session`() {
        assertNull(sessionService.getCurrentSessionId())
    }
}