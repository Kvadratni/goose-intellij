package com.block.gooseintellij.service.impl

import com.block.gooseintellij.service.GooseService
import com.block.gooseintellij.service.SessionService
import com.intellij.openapi.project.Project

class GooseServiceImpl(
    private val project: Project,
    private val sessionService: SessionService
) : GooseService {
    
    override fun initialize() {
        // Initialize project context and create a new session
        sessionService.createSession()
    }

    override suspend fun executeCommand(command: String): String {
        if (!isSessionActive()) {
            throw IllegalStateException("No active session")
        }
        // TODO: Implement command execution logic
        return ""
    }

    override fun isSessionActive(): Boolean {
        return sessionService.hasActiveSession()
    }

    override fun terminateSession() {
        sessionService.endSession()
    }
}