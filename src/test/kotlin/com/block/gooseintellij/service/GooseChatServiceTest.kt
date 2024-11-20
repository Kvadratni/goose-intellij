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
        val mockStreamHandler = object : GooseChatService.StreamHandler {
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
        val mockStreamHandler = object : GooseChatService.StreamHandler {
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
