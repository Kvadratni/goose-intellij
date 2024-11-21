package com.block.gooseintellij.service

interface StreamHandler {
    fun onText(text: String)
    fun onData(data: List<Any>)
    fun onError(error: String)
    fun onMessageAnnotation(annotation: Map<String, Any>)
    fun onFinish(finishReason: String, usage: Map<String, Int>)
    
    // Optional tool-related callbacks with default implementations
    fun onToolCallStart(toolCallId: String, toolName: String) {
        onText("\nStarting tool call: $toolName (ID: $toolCallId)")
    }
    
    fun onToolCallDelta(toolCallId: String, argsTextDelta: String) {
        onText(argsTextDelta)
    }
    
    fun onToolCall(toolCallId: String, toolName: String, args: Map<String, Any>) {
        onText("\nCalling tool: $toolName (ID: $toolCallId) with args: $args")
    }
    
    fun onToolResult(toolCallId: String, result: Any) {
        onText("\nTool result (ID: $toolCallId): $result")
    }
}