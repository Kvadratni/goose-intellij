package com.block.gooseintellij.service

import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class KeychainServiceTest {

    @Mock
    private lateinit var project: Project

    private lateinit var keychainService: KeychainService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        keychainService = KeychainService(project)
    }

    @Test
    fun `test setApiKey with valid input`() {
        val providerType = "openai"
        val apiKey = "test-api-key"
        
        keychainService.setApiKey(providerType, apiKey)
        val retrievedKey = keychainService.getApiKey(providerType)
        
        assert(retrievedKey == apiKey) { "Retrieved key does not match stored key" }
    }

    @Test
    fun `test setApiKey with blank provider type`() {
        assertThrows<IllegalArgumentException> {
            keychainService.setApiKey("", "test-api-key")
        }
    }

    @Test
    fun `test setApiKey with blank API key`() {
        assertThrows<IllegalArgumentException> {
            keychainService.setApiKey("openai", "")
        }
    }

    @Test
    fun `test removeApiKey removes existing key`() {
        val providerType = "openai"
        val apiKey = "test-api-key"
        
        keychainService.setApiKey(providerType, apiKey)
        keychainService.removeApiKey(providerType)
        
        val retrievedKey = keychainService.getApiKey(providerType)
        assert(retrievedKey == null) { "Key should have been removed" }
    }

    @Test
    fun `test getApiKey returns environment variable value if set`() {
        val providerType = "openai"
        val envKey = "GOOSE_PROVIDER__API_KEY_OPENAI"
        val envValue = "env-api-key"
        
        // Set environment variable (this is system dependent and may not work in all test environments)
        System.setProperty(envKey, envValue)
        
        try {
            val retrievedKey = keychainService.getApiKey(providerType)
            assert(retrievedKey == envValue) { "Retrieved key should match environment variable" }
        } finally {
            System.clearProperty(envKey)
        }
    }
}