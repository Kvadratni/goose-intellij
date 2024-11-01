package com.block.gooseintellij.service

interface ConfigurationService {
    /**
     * Gets the current configuration value for a given key
     * @param key The configuration key
     * @return The configuration value or null if not found
     */
    fun getConfig(key: String): String?
    
    /**
     * Sets a configuration value
     * @param key The configuration key
     * @param value The value to set
     */
    fun setConfig(key: String, value: String)
    
    /**
     * Clears a configuration value
     * @param key The configuration key to clear
     */
    fun clearConfig(key: String)
    
    /**
     * Checks if a configuration exists
     * @param key The configuration key to check
     * @return true if the configuration exists, false otherwise
     */
    fun hasConfig(key: String): Boolean
}