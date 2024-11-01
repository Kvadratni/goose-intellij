package com.block.gooseintellij.service

interface SessionService {
    /**
     * Creates a new session
     * @return sessionId of the created session
     */
    fun createSession(): String
    
    /**
     * Gets the current active session ID
     * @return The current session ID or null if no active session
     */
    fun getCurrentSessionId(): String?
    
    /**
     * Ends the current session
     */
    fun endSession()
    
    /**
     * Checks if there is an active session
     * @return true if there is an active session, false otherwise
     */
    fun hasActiveSession(): Boolean
}