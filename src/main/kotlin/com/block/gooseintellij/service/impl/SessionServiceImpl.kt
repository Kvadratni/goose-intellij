package com.block.gooseintellij.service.impl

import com.block.gooseintellij.model.Session
import com.block.gooseintellij.service.SessionService
import java.time.Instant
import java.util.UUID

class SessionServiceImpl : SessionService {
    private var currentSession: Session? = null

    override fun createSession(): String {
        // End any existing session
        endSession()
        
        // Create new session
        val sessionId = UUID.randomUUID().toString()
        currentSession = Session(
            id = sessionId,
            startTime = Instant.now(),
            isActive = true
        )
        return sessionId
    }

    override fun getCurrentSessionId(): String? {
        return currentSession?.takeIf { it.isActive }?.id
    }

    override fun endSession() {
        currentSession?.let { session ->
            if (session.isActive) {
                session.isActive = false
                session.endTime = Instant.now()
            }
        }
        currentSession = null
    }

    override fun hasActiveSession(): Boolean {
        return currentSession?.isActive == true
    }
}