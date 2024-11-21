package com.block.gooseintellij.service.parser

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger

/**
 * Parser for Vercel AI SDK stream protocol.
 * Handles parsing of different message types in the stream according to the protocol specification.
 */
class StreamParser {
    private val logger = Logger.getInstance(StreamParser::class.java)

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
        private const val NEWLINE = "\\n"
    }

    /**
     * Represents different types of stream parts according to the Vercel AI SDK protocol.
     */
    sealed class StreamPart {
        data class Text(val content: String) : StreamPart()
        data class Data(val content: List<Any>) : StreamPart()
        data class Error(val message: String) : StreamPart()
        data class MessageAnnotation(val annotation: Map<String, Any>) : StreamPart()
        data class ToolCallStreamStart(
            val toolCallId: String,
            val toolName: String
        ) : StreamPart()
        data class ToolCallDelta(
            val toolCallId: String,
            val argsTextDelta: String
        ) : StreamPart()
        data class ToolCall(
            val toolCallId: String,
            val toolName: String,
            val args: Map<String, Any>
        ) : StreamPart()
        data class ToolResult(
            val toolCallId: String,
            val result: Any
        ) : StreamPart()
        data class FinishStep(
            val finishReason: String,
            val usage: Map<String, Int>,
            val isContinued: Boolean
        ) : StreamPart()
        data class FinishMessage(
            val finishReason: String,
            val usage: Map<String, Int>
        ) : StreamPart()
    }

    /**
     * Parses a line from the stream and returns the corresponding StreamPart.
     * @param line The line to parse
     * @return The parsed StreamPart, or null if the line is empty or incomplete
     */
    fun parseLine(line: String): StreamPart? {
        logger.debug("Parsing line: '$line'")
        
        if (line.isEmpty()) {
            logger.debug("Processing empty line in state: $state")
            return when (state) {
                State.IN_TEXT_CONTENT -> {
                    logger.debug("Appending newline to text buffer. Current content: '${textBuffer}'")
                    textBuffer.append('\n')
                    val result = StreamPart.Text(textBuffer.toString())
                    logger.debug("Returning text content: '${result.content}'")
                    result
                }
                State.IN_JSON_CONTENT -> {
                    logger.debug("Appending newline to JSON buffer. Current content: '${jsonBuffer}'")
                    jsonBuffer.append('\n')
                    null
                }
                else -> {
                    logger.debug("Ignoring empty line in state: $state")
                    null
                }
            }
        }

        // Check for new message type
        val typeMatch = TYPE_PATTERN.find(line)
        if (typeMatch != null) {
            val newType = typeMatch.groupValues[1]
            val content = line.substring(typeMatch.value.length)
            
            // Reset buffers on type change
            if (newType != currentType) {
                logger.debug("Type change detected: $currentType -> $newType")
                logger.debug("Clearing buffers. Text buffer was: '$textBuffer', JSON buffer was: '$jsonBuffer'")
                textBuffer.clear()
                jsonBuffer.clear()
            }
            
            currentType = newType
            logger.debug("Processing content for type $newType")
            return when (newType) {
                "0" -> {
                    logger.debug("Processing text content: $content")
                    state = State.IN_TEXT_CONTENT
                    handleTextContent(content, true)
                }
                "2" -> {
                    logger.debug("Processing data content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "3" -> {
                    logger.debug("Processing error content: $content")
                    state = State.EXPECT_TYPE
                    StreamPart.Error(content.trim('"'))
                }
                "8" -> {
                    logger.debug("Processing message annotation content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "9" -> {
                    logger.debug("Processing tool call content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "a" -> {
                    logger.debug("Processing tool result content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "b" -> {
                    logger.debug("Processing tool call stream start content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "c" -> {
                    logger.debug("Processing tool call delta content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "d" -> {
                    logger.debug("Processing finish message content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                "e" -> {
                    logger.debug("Processing finish step content: $content")
                    state = State.IN_JSON_CONTENT
                    jsonBuffer.append(content)
                    tryParseJson()
                }
                else -> {
                    logger.debug("Unknown message type: $newType with content: $content")
                    null
                }
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

    private fun handleTextContent(text: String, isNewType: Boolean = false): StreamPart.Text {
        logger.debug("Handling text content: text='$text', isNewType=$isNewType")
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
            logger.debug("Attempting to parse JSON content: ${jsonBuffer}")
            val result = when (currentType) {
                "2" -> {
                    state = State.EXPECT_TYPE
                    val data = StreamPart.Data(gson.fromJson(jsonBuffer.toString(), Array<Any>::class.java).toList())
                    logger.debug("Parsed data part: $data")
                    data
                }
                "8" -> {
                    state = State.EXPECT_TYPE
                    val annotation = StreamPart.MessageAnnotation(gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>)
                    logger.debug("Parsed message annotation part: $annotation")
                    annotation
                }
                "9" -> {
                    state = State.EXPECT_TYPE
                    try {
                        val toolCallData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                        val toolCallId = toolCallData["toolCallId"]?.toString() 
                            ?: throw IllegalStateException("Missing toolCallId in tool call")
                        val toolName = toolCallData["toolName"]?.toString()
                            ?: throw IllegalStateException("Missing toolName in tool call")
                        val args = toolCallData["args"] as? Map<String, Any>
                            ?: throw IllegalStateException("Missing or invalid args in tool call")
                        
                        val toolCall = StreamPart.ToolCall(
                            toolCallId = toolCallId,
                            toolName = toolName,
                            args = args
                        )
                        logger.debug("Parsed tool call part: $toolCall")
                        toolCall
                    } catch (e: Exception) {
                        logger.error("Failed to parse tool call: ${e.message}", e)
                        StreamPart.Error("Failed to parse tool call: ${e.message}")
                    }
                }
                "a" -> {
                    state = State.EXPECT_TYPE
                    val toolResultData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    val toolResult = StreamPart.ToolResult(
                        toolCallId = toolResultData["toolCallId"] as String,
                        result = toolResultData["result"]!!
                    )
                    logger.debug("Parsed tool result part: $toolResult")
                    toolResult
                }
                "b" -> {
                    state = State.EXPECT_TYPE
                    val streamStartData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    val streamStart = StreamPart.ToolCallStreamStart(
                        toolCallId = streamStartData["toolCallId"] as String,
                        toolName = streamStartData["toolName"] as String
                    )
                    logger.debug("Parsed tool call stream start part: $streamStart")
                    streamStart
                }
                "c" -> {
                    state = State.EXPECT_TYPE
                    val deltaData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    val delta = StreamPart.ToolCallDelta(
                        toolCallId = deltaData["toolCallId"] as String,
                        argsTextDelta = deltaData["argsTextDelta"] as String
                    )
                    logger.debug("Parsed tool call delta part: $delta")
                    delta
                }
                "d" -> {
                    state = State.EXPECT_TYPE
                    val finishData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    val usageMap = (finishData["usage"] as Map<String, Double>).mapValues { it.value.toInt() }
                    val finish = StreamPart.FinishMessage(
                        finishReason = finishData["finishReason"] as String,
                        usage = usageMap
                    )
                    logger.debug("Parsed finish message part: $finish")
                    finish
                }
                "e" -> {
                    state = State.EXPECT_TYPE
                    val finishStepData = gson.fromJson(jsonBuffer.toString(), Map::class.java) as Map<String, Any>
                    val usageMap = (finishStepData["usage"] as Map<String, Double>).mapValues { it.value.toInt() }
                    val finishStep = StreamPart.FinishStep(
                        finishReason = finishStepData["finishReason"] as String,
                        usage = usageMap,
                        isContinued = finishStepData["isContinued"] as Boolean
                    )
                    logger.debug("Parsed finish step part: $finishStep")
                    finishStep
                }
                else -> null
            }
            return result
        } catch (e: JsonSyntaxException) {
            logger.debug("JSON parsing incomplete or failed: ${e.message}")
            // If JSON is incomplete, continue accumulating
            return null
        }
        return null
    }

    /**
     * Resets the parser state to its initial values.
     */
    fun reset() {
        logger.debug("Resetting parser state")
        state = State.EXPECT_TYPE
        currentType = null
        textBuffer.clear()
        jsonBuffer.clear()
    }
}