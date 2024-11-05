package com.block.gooseintellij.model

import java.time.Instant

/**
 * Represents a Goose AI session
 */
data class Session(
    val id: String,
    val startTime: Instant,
    var endTime: Instant? = null,
    var isActive: Boolean = true
)