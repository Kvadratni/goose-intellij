package com.block.gooseintellij.service

import com.block.gooseintellij.actions.GooseChatStartupActivity
import com.block.gooseintellij.config.GooseChatEnvironment
import com.block.gooseintellij.model.ChatMessage
import com.block.gooseintellij.model.ChatRequest
import com.block.gooseintellij.model.ChatResponse
import com.block.gooseintellij.state.GooseProviderSettings
import com.block.gooseintellij.ui.dialog.GooseProviderDialog
import com.block.gooseintellij.utils.PortManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.google.gson.Gson
import com.block.gooseintellij.utils.DialogUtils
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val NEWLINE = "\\n"

@Service(Service.Level.PROJECT)
class GooseChatService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(GooseChatService::class.java)
    private var process: Process? = null
    private var selectedPort: Int = -1
    private val gson = Gson()
    private var isInitialized = false

    @Synchronized
    fun startGoosedProcess(): CompletableFuture<Unit> {
        if (isInitialized) {
            logger.info("Goosed process already initialized")
            return CompletableFuture.completedFuture(Unit)
        }
        
        return CompletableFuture.supplyAsync {
            try {
                // Wait for up to 30 seconds for the binary path to be set
                var attempts = 0
                var binaryPath: String? = null
                while (attempts < 30) {
                    binaryPath = project.getUserData(GooseChatStartupActivity.GOOSED_BINARY_PATH_KEY)
                    if (binaryPath != null) break
                    logger.info("Waiting for binary path, attempt ${attempts + 1}/30")
                    Thread.sleep(1000)
                    attempts++
                }
                
                if (binaryPath == null) {
                    val errorMsg = "Goose binary not found. Please ensure Goose is installed and try again."
                    logger.error(errorMsg)
                    invokeLater(ModalityState.any()) {
                        DialogUtils.showError(project, errorMsg, "Initialization Error")
                    }
                    return@supplyAsync Unit
                }
                
                // Verify binary exists and is executable
                val binaryFile = java.io.File(binaryPath)
                if (!binaryFile.exists() || !binaryFile.canExecute()) {
                    val errorMsg = "Goose binary at $binaryPath is not accessible or executable."
                    logger.error(errorMsg)
                    invokeLater(ModalityState.any()) {
                        DialogUtils.showError(project, errorMsg, "Binary Access Error")
                    }
                    return@supplyAsync Unit
                }
                logger.info("Found goosed binary at: $binaryPath")

                selectedPort = PortManager.findAvailablePort()
                logger.info("Starting goosed on port $selectedPort")

                val processBuilder = ProcessBuilder(binaryPath)
                    .redirectErrorStream(true)

                // Set environment variables for the goosed process
                // Get provider type from settings or prompt user
                val settings = GooseProviderSettings.getInstance(project)
                var providerType = settings.providerType
                
                if (providerType == null) {
                    val future = CompletableFuture<String?>()
                    
                    // Ensure dialog creation and showing happens on EDT
                    ApplicationManager.getApplication().invokeAndWait({
                        val dialog = GooseProviderDialog(project)
                        if (dialog.showAndGet()) {
                            val selectedProvider = dialog.getSelectedProvider()
                            settings.providerType = selectedProvider
                            future.complete(selectedProvider)
                        } else {
                            future.complete(null)
                        }
                    }, ModalityState.any())
                    
                    providerType = future.get(30, TimeUnit.SECONDS)
                    if (providerType == null) {
                        val errorMsg = "No provider type was selected. Please configure a provider to continue."
                        logger.error(errorMsg)
                        invokeLater {
                            DialogUtils.showError(project, errorMsg, "Configuration Required")
                        }
                        return@supplyAsync Unit
                    }
                }

                // Get API key from keychain or prompt user
                var apiKey = KeychainService.getInstance(project).getApiKey(providerType)
                
                // If no API key is found, show dialog to get it
                if (apiKey == null) {
                    logger.info("No API key found for provider: $providerType. Prompting user for configuration.")
                    val apiKeyFuture = CompletableFuture<String?>()
                    
                    // Ensure dialog creation and showing happens on EDT
                    ApplicationManager.getApplication().invokeAndWait({
                        val dialog = GooseProviderDialog(project).apply {
                            setSelectedProvider(providerType)
                        }

                        if (dialog.showAndGet()) {
                            // Dialog stores the API key in KeychainService when OK is clicked
                            apiKey = KeychainService.getInstance(project).getApiKey(providerType)
                            apiKeyFuture.complete(apiKey)
                        } else {
                            apiKeyFuture.complete(null)
                        }
                    }, ModalityState.any())
                    
                    apiKey = apiKeyFuture.get(30, TimeUnit.SECONDS)
                    if (apiKey == null) {
                        val errorMsg = "API key configuration was cancelled or failed. Goose functionality will be limited."
                        logger.warn(errorMsg)
                        invokeLater {
                            DialogUtils.showInfo(project, errorMsg, "Configuration Cancelled")
                        }
                        return@supplyAsync Unit
                    }
                }

                // Configure environment variables
                processBuilder.environment().apply {
                    put("GOOSE_SERVER__PORT", selectedPort.toString())
                    put("GOOSE_CHAT_HOST", GooseChatEnvironment.host)
                    put("GOOSE_PROVIDER__TYPE", providerType)
                    put("GOOSE_PROVIDER__API_KEY", apiKey)
                }
                logger.info("Environment configured with port=$selectedPort, host=${GooseChatEnvironment.host}, provider=$providerType, apiKey=$apiKey ")

                process = processBuilder.start()
                
                if (process == null || !process!!.isAlive) {
                    val errorMsg = "Failed to start Goose process. Please check your installation and try again."
                    logger.error(errorMsg)
                    invokeLater {
                        DialogUtils.showError(project, errorMsg, "Process Error")
                    }
                    return@supplyAsync Unit
                }
                logger.info("Goosed process started successfully with PID: ${process?.pid()}")

                // Start a thread to log process output
                Thread {
                    try {
                        process?.inputStream?.bufferedReader()?.use { reader ->
                            reader.lineSequence().forEach { line ->
                                when {
                                    line.contains("error", ignoreCase = true) -> logger.error("Goosed output: $line")
                                    line.contains("warn", ignoreCase = true) -> logger.warn("Goosed output: $line")
                                    else -> logger.info("Goosed output: $line")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error reading goosed process output", e)
                    }
                }.apply {
                    name = "GoosedOutputLogger"
                    isDaemon = true
                    start()
                }

                // Wait for the server to be ready
                try {
                    logger.info("Goosed server is ready and responding")
                } catch (e: TimeoutException) {
                    logger.error("Goosed server failed to respond in time", e)
                    stopGoosedProcess()
                    invokeLater {
                        DialogUtils.showError(project, "Goose server failed to start within the timeout period. Please try again.", "Server Timeout")
                    }
                    return@supplyAsync Unit
                }

                isInitialized = true
                logger.info("Goosed initialization completed successfully")
                Unit
            } catch (e: Exception) {
                logger.error("Failed to start goosed process", e)
                stopGoosedProcess()
                throw e
            }
        }
    }

    // Data classes for stream protocol
sealed class StreamPart {
    data class Text(val content: String) : StreamPart()
    data class Data(val content: List<Any>) : StreamPart()
    data class Error(val message: String) : StreamPart()
    data class MessageAnnotation(val annotation: Map<String, Any>) : StreamPart()
    data class FinishMessage(
        val finishReason: String,
        val usage: Map<String, Int>
    ) : StreamPart()
}

interface StreamHandler {
    fun onText(text: String)
    fun onData(data: List<Any>)
    fun onError(error: String)
    fun onMessageAnnotation(annotation: Map<String, Any>)
    fun onFinish(finishReason: String, usage: Map<String, Int>)
}

// Class to maintain stream parsing state using a state machine approach
internal class StreamParser {
    private enum class State {
        EXPECT_TYPE,      // Expecting a new type identifier or continuation of current type
        IN_TEXT_CONTENT,  // Processing text content (with or without type identifier)
        IN_JSON_CONTENT   // Processing JSON content for data/finish messages
    }

    private var state = State.EXPECT_TYPE
    private var currentType: String? = null
    private var textBuffer = StringBuilder()
    private var jsonBuffer = StringBuilder()
    private val gson = Gson()

    companion object {
        private val TYPE_PATTERN = Regex("^([0-9d]):")
        private val ESCAPE_PATTERN = Regex("""\\[nrt"]""")
    }

    fun parseLine(line: String): StreamPart? {
        if (line.isEmpty()) {
            return when (state) {
                State.IN_TEXT_CONTENT -> {
                    textBuffer.append('\n')
                    StreamPart.Text(textBuffer.toString())
                }
                State.IN_JSON_CONTENT -> {
                    jsonBuffer.append('\n')
                    null
                }
                else -> null
            }
        }

        // Check for new message type
        val typeMatch = TYPE_PATTERN.find(line)
        if (typeMatch != null) {
            val newType = typeMatch.groupValues[1]
            val content = line.substring(typeMatch.value.length)
            
            // Reset buffers on type change
            if (newType != currentType) {
                textBuffer.clear()
                jsonBuffer.clear()
            }
            
            currentType = newType
            return when (newType) {
                "0" -> {
                    state = State.IN_TEXT_CONTENT
                    handleTextContent(content, true)
                }
                "2" -> {
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "3" -> {
                    state = State.EXPECT_TYPE
                    StreamPart.Error(content.trim('"'))
                }
                "8" -> {
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "d" -> {
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                else -> null
            }
        }
        
        // Handle content continuation
        return when (state) {
            State.IN_TEXT_CONTENT -> handleTextContent(line)
            State.IN_JSON_CONTENT -> {
                jsonBuffer.append(line)
                tryParseJson()
            }
            else -> null
        }
    }

    private fun handleTextContent(text: String, isNewType: Boolean = false): StreamPart {
        // First handle escaped characters
        val unescaped = if (text.contains(ESCAPE_PATTERN)) {
            text.replace(NEWLINE, "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
        } else {
            text
        }

        // Handle wrapping quotes
        var unwrapped = when {
            // Handle empty message with quotes ("""" -> "")
            unescaped.matches(Regex("^\"*$")) -> {
                val quoteCount = unescaped.count { it == '"' }
                "\"".repeat(quoteCount / 2)
            }
            // Handle wrapped quotes while preserving internal quotes
            unescaped.startsWith("\"") && unescaped.endsWith("\"") -> {
                var result = unescaped
                while (result.length >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
                    // Only unwrap if we have matching outer quotes
                    val innerContent = result.substring(1, result.length - 1)
                    // Stop if removing more quotes would affect internal quoted content
                    if (innerContent.count { it == '"' } % 2 != 0) break
                    result = innerContent
                }
                result
            }
            else -> unescaped
        }
        if (unwrapped == "\"") {
            unwrapped = "\n"
        }

        if (isNewType) {
            // For new type messages, clear buffer and set new content
            textBuffer.setLength(0)
            textBuffer.append(unwrapped)
            return StreamPart.Text(unwrapped)
        } else {
            // For continuations, append with newline and return full buffer
            textBuffer.append('\n').append(unwrapped)
            return StreamPart.Text(textBuffer.toString())
        }
    }

    private fun tryParseJson(): StreamPart? {
        try {
            when (currentType) {
                "2" -> {
                    state = State.EXPECT_TYPE
                    return StreamPart.Data(gson.fromJson(jsonBuffer.toString(), Array<Any>::class.java).toList())
                }
                "8" -> {
                    state = State.EXPECT_TYPE
                    return StreamPart.MessageAnnotation(gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>)
                }
                "d" -> {
                    state = State.EXPECT_TYPE
                    val finishData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    return StreamPart.FinishMessage(
                        finishReason = finishData["finishReason"] as String,
                        usage = finishData["usage"] as Map<String, Int>
                    )
                }
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // If JSON is incomplete, continue accumulating
            return null
        }
        return null
    }

    fun reset() {
        state = State.EXPECT_TYPE
        currentType = null
        textBuffer.clear()
        jsonBuffer.clear()
    }
}

fun sendMessage(
    message: String,
    streaming: Boolean = false,
    streamHandler: StreamHandler? = null
): CompletableFuture<ChatResponse> {
    return CompletableFuture.supplyAsync {
        if (selectedPort == -1) {
            throw IllegalStateException("Goosed process not started - no port selected")
        }

        val connection = URI("http://${GooseChatEnvironment.host}:$selectedPort/reply").toURL()
            .openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = GooseChatEnvironment.timeoutMs.toInt()
        connection.readTimeout = GooseChatEnvironment.timeoutMs.toInt()
        
        // Add stream protocol headers if streaming
        if (streaming) {
            connection.setRequestProperty("x-vercel-ai-data-stream", "v1")
            connection.setRequestProperty("Accept", "text/event-stream")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Connection", "keep-alive")
        }

        val chatRequest = ChatRequest(
            messages = listOf(ChatMessage("user", message))
        )
        val jsonInput = gson.toJson(chatRequest)

        try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonInput)
                writer.flush()
            }

            if (!streaming) {
                // Non-streaming mode - return full response
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = reader.readText()
                    return@supplyAsync gson.fromJson(response, ChatResponse::class.java)
                }
            }
            
            // Streaming mode
            var responseBuilder = StringBuilder()
            var finishReason: String? = null
            var usage: Map<String, Int>? = null
            var error: String? = null

            val parser = StreamParser()
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    if(line.isEmpty()) streamHandler?.onText(NEWLINE)
                    val part = parser.parseLine(line)
                    when (part) {
                        is StreamPart.Text -> {
                            if(part.content.isEmpty()) {
                                streamHandler?.onText(NEWLINE)
                                // Only accumulate for final response
                                responseBuilder.append(NEWLINE)
                            }
                            // For streaming, send directly to handler without accumulating
                            streamHandler?.onText(part.content)
                            // Only accumulate for final response
                            responseBuilder.append(part.content)
                        }
                        is StreamPart.Data -> {
                            streamHandler?.onData(part.content)
                        }
                        is StreamPart.Error -> {
                            error = part.message
                            streamHandler?.onError(part.message)
                        }
                        is StreamPart.MessageAnnotation -> {
                            streamHandler?.onMessageAnnotation(part.annotation)
                        }
                        is StreamPart.FinishMessage -> {
                            finishReason = part.finishReason
                            usage = part.usage
                            streamHandler?.onFinish(part.finishReason, part.usage)
                            parser.reset() // Reset parser state after finish
                        }
                        null -> {} // Ignore empty lines
                    }
                }
            }

            // Return final response with accumulated text
            ChatResponse(
                message = ChatMessage("assistant", responseBuilder.toString()),
                finishReason = finishReason,
                usage = usage,
                error = error
            )

        } catch (e: Exception) {
            logger.error("Failed to send message", e)
            ChatResponse(
                message = ChatMessage("assistant", "Error: Failed to communicate with server"),
                error = e.message
            )
        }
    }
    }

    fun stopGoosedProcess() {
        process?.destroy()
        process = null
        if (selectedPort != -1) {
            PortManager.releasePort(selectedPort)
            selectedPort = -1
        }
    }

    override fun dispose() {
        logger.info("Disposing GooseChatService")
        stopGoosedProcess()
    }

    init {
        // Register this service to be disposed when the project is closed
        Disposer.register(project, this)
    }
}
