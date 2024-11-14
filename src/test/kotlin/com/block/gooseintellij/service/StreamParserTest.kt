package com.block.gooseintellij.service

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StreamParserTest {
    @Test
    fun `test basic text message parsing`() {
        val parser = GooseChatService.StreamParser()
        
        // Single line message
        val part1 = parser.parseLine("0:Hello")
        assertNotNull(part1)
        assertEquals("Hello", (part1 as GooseChatService.StreamPart.Text).content)
        
        // Multi-line message
        val part2 = parser.parseLine("0:First line")
        val part3 = parser.parseLine("Second line")
        assertNotNull(part2)
        assertNotNull(part3)
        assertEquals("First line", (part2 as GooseChatService.StreamPart.Text).content)
        assertEquals("First line\nSecond line", (part3 as GooseChatService.StreamPart.Text).content)
    }
    
    @Test
    fun `test counting sequence`() {
        val parser = GooseChatService.StreamParser()
        val lines = (1..15).map { "0:$it\n" }
        
        var lastContent = ""
        lines.forEach { line ->
            val part = parser.parseLine(line)
            assertNotNull(part)
            assertTrue(part is GooseChatService.StreamPart.Text)
            part as GooseChatService.StreamPart.Text
            assertEquals(it.toString(), part.content.trim())
            lastContent = part.content
        }
    }
    
    @Test
    fun `test json message parsing`() {
        val parser = GooseChatService.StreamParser()
        
        // Data message
        val dataPart = parser.parseLine("2:[1,2,3]")
        assertNotNull(dataPart)
        assertTrue(dataPart is GooseChatService.StreamPart.Data)
        assertEquals(listOf(1.0, 2.0, 3.0), (dataPart as GooseChatService.StreamPart.Data).content)
        
        // Finish message
        val finishPart = parser.parseLine("d:{\"finishReason\":\"stop\",\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20}}")
        assertNotNull(finishPart)
        assertTrue(finishPart is GooseChatService.StreamPart.FinishMessage)
        assertEquals("stop", (finishPart as GooseChatService.StreamPart.FinishMessage).finishReason)
    }
    
    @Test
    fun `test error message parsing`() {
        val parser = GooseChatService.StreamParser()
        val part = parser.parseLine("3:Error occurred")
        assertNotNull(part)
        assertTrue(part is GooseChatService.StreamPart.Error)
        assertEquals("Error occurred", (part as GooseChatService.StreamPart.Error).message)
    }
    
    @Test
    fun `test empty lines`() {
        val parser = GooseChatService.StreamParser()
        
        // Empty line in text content should become newline
        parser.parseLine("0:First line")
        val emptyPart = parser.parseLine("")
        assertNotNull(emptyPart)
        assertTrue(emptyPart is GooseChatService.StreamPart.Text)
        assertEquals("First line\n", (emptyPart as GooseChatService.StreamPart.Text).content)
        
        // Empty line outside text content should be ignored
        parser.reset()
        val nullPart = parser.parseLine("")
        assertNull(nullPart)
    }
}