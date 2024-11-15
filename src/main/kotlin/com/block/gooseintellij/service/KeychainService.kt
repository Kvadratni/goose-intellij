package com.block.gooseintellij.service

import com.intellij.credentialStore.generateServiceName
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class KeychainService(private val project: Project) {
    private val logger = Logger.getInstance(KeychainService::class.java)

    companion object {
        private const val SUBSYSTEM = "com.block.gooseintellij"
        private const val API_KEY_PREFIX = "INTELLIGOOSE_PROVIDER__API_KEY_"

        fun getInstance(project: Project): KeychainService =
            project.getService(KeychainService::class.java)
    }

    private fun getCredentialAttributes(providerType: String): CredentialAttributes {
        val serviceName = generateServiceName(SUBSYSTEM, "$API_KEY_PREFIX${providerType.uppercase()}")
        logger.debug("Generated service name for provider $providerType: $serviceName")
        return CredentialAttributes(serviceName)
    }

    /**
     * Gets the API key for the specified provider type.
     * First checks environment variables, then falls back to PasswordSafe.
     */
    fun getApiKey(providerType: String): String? {
        try {
            // First check environment variable
            val envKey = "$API_KEY_PREFIX${providerType.uppercase()}"
            System.getenv(envKey)?.let { envValue ->
                logger.debug("Found API key in environment variable for provider $providerType")
                return envValue
            }

            // Fall back to PasswordSafe
            val key = getCredentialAttributes(providerType)
            val apiKey = PasswordSafe.instance.getPassword(key)
            logger.debug("Retrieved API key from PasswordSafe for provider $providerType: ${if (apiKey != null) "success" else "not found"}")
            return apiKey
        } catch (e: Exception) {
            logger.error("Failed to retrieve API key for provider $providerType", e)
            throw RuntimeException("Failed to retrieve API key: ${e.message}", e)
        }
    }

    /**
     * Sets the API key for the specified provider type.
     */
    fun setApiKey(providerType: String, apiKey: String) {
        try {
            require(providerType.isNotBlank()) { "Provider type cannot be blank" }
            require(apiKey.isNotBlank()) { "API key cannot be blank" }
            
            val key = getCredentialAttributes(providerType)
            PasswordSafe.instance.setPassword(key, apiKey)
            logger.info("API key stored successfully for provider: $providerType")
        } catch (e: Exception) {
            logger.error("Failed to store API key for provider $providerType", e)
            throw RuntimeException("Failed to store API key: ${e.message}", e)
        }
    }

    /**
     * Removes the API key for the specified provider type.
     */
    fun removeApiKey(providerType: String) {
        try {
            val key = getCredentialAttributes(providerType)
            PasswordSafe.instance.setPassword(key, null)
            logger.info("API key removed for provider: $providerType")
        } catch (e: Exception) {
            logger.error("Failed to remove API key for provider $providerType", e)
            throw RuntimeException("Failed to remove API key: ${e.message}", e)
        }
    }
}
