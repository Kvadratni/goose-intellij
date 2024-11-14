package com.block.gooseintellij.service

import com.block.gooseintellij.actions.GooseChatStartupActivity
import com.block.gooseintellij.config.GooseChatEnvironment
import com.block.gooseintellij.model.ChatMessage
import com.block.gooseintellij.model.ChatRequest
import com.block.gooseintellij.model.ChatResponse
import com.block.gooseintellij.utils.PortManager
import com.google.gson.Gson
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
                    Thread.sleep(1000)
                    attempts++
                }
                
                binaryPath ?: throw IllegalStateException("Goosed binary path not found after waiting 30 seconds - GooseChatStartupActivity may not have completed")

                selectedPort = PortManager.findAvailablePort()
                logger.info("Starting goosed on port $selectedPort")

                val processBuilder = ProcessBuilder(binaryPath)
                    .redirectErrorStream(true)

                // Set environment variables for the goosed process
                processBuilder.environment().apply {
                    put("GOOSE_SERVER__PORT", selectedPort.toString())
                    put("GOOSE_CHAT_HOST", GooseChatEnvironment.host)
                }

                process = processBuilder.start()

                // Start a thread to log process output
                Thread {
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.lineSequence().forEach { line ->
                            logger.info("Goosed output: $line")
                        }
                    }
                }.start()

                // Wait for the server to be ready
               // waitForServer()
                isInitialized = true
                Unit
            } catch (e: Exception) {
                stopGoosedProcess()
                throw e
            }
        }
    }

    private fun waitForServer() {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < GooseChatEnvironment.timeoutMs) {
            try {
                val connection = URI("http://${GooseChatEnvironment.host}:$selectedPort/health").toURL()
                    .openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                if (connection.responseCode == 200) {
                    return
                }
            } catch (e: Exception) {
                Thread.sleep(100)
            }
        }
        throw TimeoutException("Server did not start within ${GooseChatEnvironment.timeoutMs}ms")
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

private fun parseStreamPart(line: String): StreamPart? {
    if (line.isEmpty()) return null
    
    val typeId = line.substringBefore(':')
    val content = line.substringAfter(':')
    
    return when (typeId) {
        "0" -> StreamPart.Text(content.trim('"').replace("\\n", "\n")) // Text part with newline handling
        "2" -> StreamPart.Data(gson.fromJson(content, Array<Any>::class.java).toList()) // Data part
        "3" -> StreamPart.Error(content.trim('"')) // Error part
        "8" -> StreamPart.MessageAnnotation(gson.fromJson(content, Map::class.java) as Map<String, Any>) // Message annotation
        "d" -> { // Finish message
            val finishData = gson.fromJson(content, Map::class.java) as Map<String, Any>
            StreamPart.FinishMessage(
                finishReason = finishData["finishReason"] as String,
                usage = finishData["usage"] as Map<String, Int>
            )
        }
        else -> null
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

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    val part = parseStreamPart(line)
                    when (part) {
                        is StreamPart.Text -> {
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
