package com.block.gooseintellij.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.FileAlreadyExistsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.createDirectories

class GooseChatStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(GooseChatStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        try {
            setupGoosedBinary(project)
        } catch (e: Exception) {
            logger.error("Failed to initialize Goose Chat: ${e.message}", e)
            throw e // Propagate the error to show initialization failure
        }
    }

    private suspend fun setupGoosedBinary(project: Project) = withContext(Dispatchers.IO) {
        try {
            // Get the resource first to fail fast if not found
            val resourceStream = GooseChatStartupActivity::class.java.classLoader.getResourceAsStream("goosed")
            requireNotNull(resourceStream) { "Could not find goosed binary in resources" }

            val tempDir = Path.of(System.getProperty("java.io.tmpdir"), "goose-intellij")
            val binaryPath = tempDir.resolve("goosed")
            val tempFilePath = tempDir.resolve("goosed.tmp")
            
            // Create directory if it doesn't exist
            try {
                tempDir.createDirectories()
            } catch (e: Exception) {
                logger.error("Failed to create temporary directory: ${e.message}")
                throw e
            }

            // Check if we already have a valid binary
            if (binaryPath.exists()) {
                logger.info("Found existing valid goosed binary at: $binaryPath")
                project.putUserData(GOOSED_BINARY_PATH_KEY, binaryPath.toString())
                return@withContext
            }

            // If we get here, we need to create or replace the binary
            try {
                // Write to temporary file first
                resourceStream.use { input ->
                    Files.copy(input, tempFilePath, StandardCopyOption.REPLACE_EXISTING)
                }
                
                // Set executable permissions
                tempFilePath.toFile().setExecutable(true, false)
                
                // Attempt atomic move
                Files.move(tempFilePath, binaryPath, 
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE)
                
                
                project.putUserData(GOOSED_BINARY_PATH_KEY, binaryPath.toString())
                logger.info("Successfully initialized goosed binary at: $binaryPath")
            } catch (e: Exception) {
                // Clean up temp file if it exists
                try {
                    Files.deleteIfExists(tempFilePath)
                } catch (cleanupError: Exception) {
                    logger.warn("Failed to clean up temporary file: ${cleanupError.message}")
                }
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to setup goosed binary", e)
            throw e
        }
    }

    companion object {
        val GOOSED_BINARY_PATH_KEY = com.intellij.openapi.util.Key<String>("goosed.binary.path")
    }
}
