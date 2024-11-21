package com.block.gooseintellij.service

import com.block.gooseintellij.state.GooseConversationState
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection

class GooseChatServiceTest() {
    private lateinit var gooseChatService: GooseChatService
    private lateinit var mockProject: Project
    private lateinit var mockConversationState: GooseConversationState

    @BeforeEach
    fun setUp() {
        mockProject = mock(Project::class.java)
        mockConversationState = mock(GooseConversationState::class.java)
        gooseChatService = GooseChatService(mockProject)
    }

    @Test
    fun `test streaming response handling`() {
        // Mock stream content
        val mockContent = """
            0:"Hello"
            0:" world"
            d:{"finishReason":"stop","usage":{"prompt_tokens":10,"completion_tokens":20}}
        """.trimIndent()

        // Create mock connection
        val mockConnection = mock(HttpURLConnection::class.java)
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream(mockContent.toByteArray())

        `when`(mockConnection.outputStream).thenReturn(outputStream)
        `when`(mockConnection.inputStream).thenReturn(inputStream)

        // Mock stream handler
        val mockStreamHandler = object : StreamHandler {
            val receivedText = StringBuilder()
            var finishReason: String? = null
            var usage: Map<String, Int>? = null

            override fun onText(text: String) {
                receivedText.append(text)
            }

            override fun onData(data: List<Any>) {}

            override fun onError(error: String) {}

            override fun onMessageAnnotation(annotation: Map<String, Any>) {}

            override fun onFinish(finishReason: String, usage: Map<String, Int>) {
                this.finishReason = finishReason
                this.usage = usage
            }
        }

        // Send message and verify results
        val response = gooseChatService.sendMessage(
            message = "Test message",
            streaming = true,
            streamHandler = mockStreamHandler
        ).get()

        // Verify conversation state updates
        verify(mockConversationState).addMessage("user", "Test message")
        verify(mockConversationState, atLeastOnce()).addMessage("assistant", "Hello world")

        // Verify response content
        assertEquals("Hello world", response.message.content)
        assertEquals("stop", response.finishReason)
        assertNotNull(response.usage)
        assertEquals(10, response.usage?.get("prompt_tokens"))
        assertEquals(20, response.usage?.get("completion_tokens"))
    }

    @Test
    fun `test tool call and result handling`() {
        // Mock stream content with tool call and result
        val mockContent = """
            0:"Starting process"
            t:{"id":"call-123","name":"search_code","args":{"query":"test"}}
            r:{"id":"call-123","result":"Found 5 matches"}
            0:"Process complete"
        """.trimIndent()

        // Create mock connection
        val mockConnection = mock(HttpURLConnection::class.java)
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream(mockContent.toByteArray())

        `when`(mockConnection.outputStream).thenReturn(outputStream)
        `when`(mockConnection.inputStream).thenReturn(inputStream)

        // Mock stream handler to capture tool call and result messages
        val mockStreamHandler = object : StreamHandler {
            val receivedMessages = mutableListOf<String>()
            var toolCallId: String? = null
            var toolName: String? = null
            var toolArgs: Map<String, Any>? = null
            var toolResult: Any? = null

            override fun onText(text: String) {
                receivedMessages.add("TEXT: $text")
            }

            override fun onData(data: List<Any>) {}

            override fun onError(error: String) {}

            override fun onMessageAnnotation(annotation: Map<String, Any>) {}

            override fun onToolCallStart(toolCallId: String, toolName: String) {
                this.toolCallId = toolCallId
                this.toolName = toolName
                receivedMessages.add("TOOL_START: $toolName")
            }

            override fun onToolCall(toolCallId: String, toolName: String, args: Map<String, Any>) {
                this.toolArgs = args
                receivedMessages.add("TOOL_CALL: $toolName")
            }

            override fun onToolResult(toolCallId: String, result: Any) {
                this.toolResult = result
                receivedMessages.add("TOOL_RESULT: $result")
            }

            override fun onFinish(finishReason: String, usage: Map<String, Int>) {}
        }

        // Send message and verify results
        val response = gooseChatService.sendMessage(
            message = "Test message",
            streaming = true,
            streamHandler = mockStreamHandler
        ).get()

        // Verify tool call sequence
        assertEquals("call-123", mockStreamHandler.toolCallId)
        assertEquals("search_code", mockStreamHandler.toolName)
        assertEquals(mapOf("query" to "test"), mockStreamHandler.toolArgs)
        assertEquals("Found 5 matches", mockStreamHandler.toolResult)

        // Verify message sequence
        assertTrue(mockStreamHandler.receivedMessages.any { it.contains("Starting process") })
        assertTrue(mockStreamHandler.receivedMessages.any { it.contains("TOOL_START: search_code") })
        assertTrue(mockStreamHandler.receivedMessages.any { it.contains("TOOL_RESULT: Found 5 matches") })
        assertTrue(mockStreamHandler.receivedMessages.any { it.contains("Process complete") })
    }

    @Test
    fun `test tool call delta handling`() {
        // Mock stream content with tool call delta
        val mockContent = """
            0:"Starting process"
            ts:{"id":"call-123","name":"search_code"}
            td:{"id":"call-123","argsText":"que"}
            td:{"id":"call-123","argsText":"ry=test"}
            r:{"id":"call-123","result":"Found 5 matches"}
            0:"Process complete"
        """.trimIndent()

        // Create mock connection
        val mockConnection = mock(HttpURLConnection::class.java)
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream(mockContent.toByteArray())

        `when`(mockConnection.outputStream).thenReturn(outputStream)
        `when`(mockConnection.inputStream).thenReturn(inputStream)

        // Mock stream handler to capture tool call delta messages
        val mockStreamHandler = object : StreamHandler {
            val receivedDeltas = mutableListOf<String>()

            override fun onText(text: String) {}
            override fun onData(data: List<Any>) {}
            override fun onError(error: String) {}
            override fun onMessageAnnotation(annotation: Map<String, Any>) {}

            override fun onToolCallStart(toolCallId: String, toolName: String) {
                receivedDeltas.add("START:$toolName")
            }

            override fun onToolCallDelta(toolCallId: String, argsTextDelta: String) {
                receivedDeltas.add("DELTA:$argsTextDelta")
            }

            override fun onToolResult(toolCallId: String, result: Any) {
                receivedDeltas.add("RESULT:$result")
            }

            override fun onFinish(finishReason: String, usage: Map<String, Int>) {}
        }

        // Send message and verify results
        val response = gooseChatService.sendMessage(
            message = "Test message",
            streaming = true,
            streamHandler = mockStreamHandler
        ).get()

        // Verify delta sequence
        assertEquals(4, mockStreamHandler.receivedDeltas.size)
        assertEquals("START:search_code", mockStreamHandler.receivedDeltas[0])
        assertEquals("DELTA:que", mockStreamHandler.receivedDeltas[1])
        assertEquals("DELTA:ry=test", mockStreamHandler.receivedDeltas[2])
        assertEquals("RESULT:Found 5 matches", mockStreamHandler.receivedDeltas[3])
    }

    @Test
    fun `test error handling in streaming response`() {
        // Mock stream content with error
        val mockContent = """
            0:"Partial response"
            3:"Server error occurred"
        """.trimIndent()

        // Create mock connection
        val mockConnection = mock(HttpURLConnection::class.java)
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream(mockContent.toByteArray())

        `when`(mockConnection.outputStream).thenReturn(outputStream)
        `when`(mockConnection.inputStream).thenReturn(inputStream)

        // Mock stream handler
        val mockStreamHandler = object : StreamHandler {
            var receivedError: String? = null

            override fun onText(text: String) {}
            override fun onData(data: List<Any>) {}
            override fun onError(error: String) {
                receivedError = error
            }
            override fun onMessageAnnotation(annotation: Map<String, Any>) {}
            override fun onFinish(finishReason: String, usage: Map<String, Int>) {}
        }

        // Send message and verify results
        val response = gooseChatService.sendMessage(
            message = "Test message",
            streaming = true,
            streamHandler = mockStreamHandler
        ).get()

        // Verify error handling
        assertNotNull(response.error)
        assertEquals("Server error occurred", response.error)
        verify(mockConversationState).addMessage("assistant", "Error: Server error occurred")
    }
}
