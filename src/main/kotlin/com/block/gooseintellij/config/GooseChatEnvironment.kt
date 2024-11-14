package com.block.gooseintellij.config

import com.intellij.openapi.diagnostic.Logger

object GooseChatEnvironment {
    private val logger = Logger.getInstance(GooseChatEnvironment::class.java)

    private const val DEFAULT_HOST = "localhost"
    private const val DEFAULT_MIN_PORT = 3000
    private const val DEFAULT_MAX_PORT = 3100
    private const val DEFAULT_TIMEOUT_MS = 30000L

    val host: String
        get() = System.getenv("GOOSE_CHAT_HOST") ?: DEFAULT_HOST

    val minPort: Int
        get() = System.getenv("GOOSE_CHAT_MIN_PORT")?.toIntOrNull() ?: DEFAULT_MIN_PORT

    val maxPort: Int
        get() = System.getenv("GOOSE_CHAT_MAX_PORT")?.toIntOrNull() ?: DEFAULT_MAX_PORT

    val timeoutMs: Long
        get() = System.getenv("GOOSE_CHAT_TIMEOUT_MS")?.toLongOrNull() ?: DEFAULT_TIMEOUT_MS

    fun validate() {
        require(minPort in 1024..65535) { "Min port must be between 1024 and 65535" }
        require(maxPort in 1024..65535) { "Max port must be between 1024 and 65535" }
        require(minPort < maxPort) { "Min port must be less than max port" }
        require(timeoutMs > 0) { "Timeout must be positive" }
    }
}