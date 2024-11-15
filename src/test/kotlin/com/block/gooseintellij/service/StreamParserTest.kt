package com.block.gooseintellij.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.block.gooseintellij.service.GooseChatService.StreamPart

class StreamParserTest {
    @Test
    fun `test basic text message parsing`() {
        val parser = GooseChatService.StreamParser()
        
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
        val parser = GooseChatService.StreamParser()
        
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
        val parser = GooseChatService.StreamParser()
        val part = parser.parseLine("3:Error occurred")
        assertNotNull(part)
        assertTrue(part is StreamPart.Error)
        assertEquals("Error occurred", (part as StreamPart.Error).message)
    }
    
    @Test
    fun `test empty lines`() {
        val parser = GooseChatService.StreamParser()
        
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