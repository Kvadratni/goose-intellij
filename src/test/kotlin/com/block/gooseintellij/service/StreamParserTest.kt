package com.block.gooseintellij.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.block.gooseintellij.service.parser.StreamParser
import com.block.gooseintellij.service.parser.StreamParser.StreamPart

class StreamParserTest {
    @Test
    fun `test tool call message parsing`() {
        val parser = StreamParser()
        
        // Test tool call start
        val toolCallStart = parser.parseLine("""b:{"toolCallId":"call-456","toolName":"streaming-tool"}""")
        assertNotNull(toolCallStart)
        assertTrue(toolCallStart is StreamPart.ToolCallStreamStart)
        assertEquals("call-456", (toolCallStart as StreamPart.ToolCallStreamStart).toolCallId)
        assertEquals("streaming-tool", toolCallStart.toolName)
        
        // Test tool call delta
        val toolCallDelta = parser.parseLine("""c:{"toolCallId":"call-456","argsTextDelta":"partial arg"}""")
        assertNotNull(toolCallDelta)
        assertTrue(toolCallDelta is StreamPart.ToolCallDelta)
        assertEquals("call-456", (toolCallDelta as StreamPart.ToolCallDelta).toolCallId)
        assertEquals("partial arg", toolCallDelta.argsTextDelta)
        
        // Test tool call
        val toolCall = parser.parseLine("""9:{"toolCallId":"call-123","toolName":"my-tool","args":{"some":"argument"}}""")
        assertNotNull(toolCall)
        assertTrue(toolCall is StreamPart.ToolCall)
        assertEquals("call-123", (toolCall as StreamPart.ToolCall).toolCallId)
        assertEquals("my-tool", toolCall.toolName)
        assertEquals("argument", toolCall.args["some"])
        
        // Test tool result
        val toolResult = parser.parseLine("""a:{"toolCallId":"call-123","result":"tool output"}""")
        assertNotNull(toolResult)
        assertTrue(toolResult is StreamPart.ToolResult)
        assertEquals("call-123", (toolResult as StreamPart.ToolResult).toolCallId)
        assertEquals("tool output", toolResult.result)
    }

    @Test
    fun `test basic text message parsing`() {
        val parser = StreamParser()
        
        // Single line message
        val part1 = parser.parseLine("0:Hello")
        assertNotNull(part1)
        assertTrue(part1 is StreamPart.Text)
        assertEquals("Hello", (part1 as StreamPart.Text).content)
        
        // Multi-line message
        val part2 = parser.parseLine("0:First line")
        val part3 = parser.parseLine("Second line")
        assertNotNull(part2)
        assertNotNull(part3)
        assertTrue(part2 is StreamPart.Text)
        assertTrue(part3 is StreamPart.Text)
        assertEquals("First line", (part2 as StreamPart.Text).content)
        assertEquals("First line\nSecond line", (part3 as StreamPart.Text).content)
    }
    
    @Test
    fun `test json message parsing`() {
        val parser = StreamParser()
        
        // Data message
        val dataPart = parser.parseLine("2:[1,2,3]")
        assertNotNull(dataPart)
        assertTrue(dataPart is StreamPart.Data)
        assertEquals(listOf(1.0, 2.0, 3.0), (dataPart as StreamPart.Data).content)
        
        // Finish message
        val finishPart = parser.parseLine("d:{\"finishReason\":\"stop\",\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20}}")
        assertNotNull(finishPart)
        assertTrue(finishPart is StreamPart.FinishMessage)
        assertEquals("stop", (finishPart as StreamPart.FinishMessage).finishReason)
        assertEquals(mapOf("prompt_tokens" to 10, "completion_tokens" to 20), finishPart.usage)
    }
    
    @Test
    fun `test error message parsing`() {
        val parser = StreamParser()
        val part = parser.parseLine("3:Error occurred")
        assertNotNull(part)
        assertTrue(part is StreamPart.Error)
        assertEquals("Error occurred", (part as StreamPart.Error).message)
    }
    
    @Test
    fun `test empty lines`() {
        val parser = StreamParser()
        
        // Empty line in text content should become newline
        parser.parseLine("0:First line")
        val emptyPart = parser.parseLine("")
        assertNotNull(emptyPart)
        assertTrue(emptyPart is StreamPart.Text)
        assertEquals("First line\n", (emptyPart as StreamPart.Text).content)
        
        // Empty line outside text content should be ignored
        parser.reset()
        val nullPart = parser.parseLine("")
        assertNull(nullPart)
    }
}