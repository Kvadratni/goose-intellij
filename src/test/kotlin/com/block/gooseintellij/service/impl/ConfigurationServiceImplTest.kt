package com.block.gooseintellij.service.impl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ConfigurationServiceImplTest {
    private lateinit var configService: ConfigurationServiceImpl

    @BeforeEach
    fun setup() {
        configService = ConfigurationServiceImpl()
    }

    @Test
    fun `test set and get config`() {
        val key = "testKey"
        val value = "testValue"
        
        configService.setConfig(key, value)
        assertEquals(value, configService.getConfig(key))
    }

    @Test
    fun `test clear config`() {
        val key = "testKey"
        val value = "testValue"
        
        configService.setConfig(key, value)
        assertTrue(configService.hasConfig(key))
        
        configService.clearConfig(key)
        assertFalse(configService.hasConfig(key))
        assertNull(configService.getConfig(key))
    }

    @Test
    fun `test has config`() {
        val key = "testKey"
        assertFalse(configService.hasConfig(key))
        
        configService.setConfig(key, "testValue")
        assertTrue(configService.hasConfig(key))
    }

    @Test
    fun `test get non-existent config returns null`() {
        assertNull(configService.getConfig("nonExistentKey"))
    }
}