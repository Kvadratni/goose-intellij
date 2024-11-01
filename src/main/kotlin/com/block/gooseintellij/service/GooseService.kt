package com.block.gooseintellij.service

interface GooseService {
    /**
     * Initializes the Goose service with the current project context
     */
    fun initialize()

    /**
     * Executes a Goose command and returns the response
     * @param command The command to execute
     * @return The response from Goose
     */
    suspend fun executeCommand(command: String): String

    /**
     * Gets the current session status
     * @return true if there's an active session, false otherwise
     */
    fun isSessionActive(): Boolean

    /**
     * Terminates the current session if any
     */
    fun terminateSession()
}