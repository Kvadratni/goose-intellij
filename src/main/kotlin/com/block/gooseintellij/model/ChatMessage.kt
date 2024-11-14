package com.block.gooseintellij.model

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val messages: List<ChatMessage>
)

data class ChatResponse(
    val message: ChatMessage,
    val error: String? = null,
    val finishReason: String? = null,
    val usage: Map<String, Int>? = null
)