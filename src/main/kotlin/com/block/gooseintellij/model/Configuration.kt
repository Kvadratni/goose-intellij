package com.block.gooseintellij.model

/**
 * Represents plugin configuration settings
 */
data class Configuration(
    val key: String,
    val value: String,
    val description: String? = null
)