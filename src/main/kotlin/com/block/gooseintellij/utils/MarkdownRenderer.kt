package com.block.gooseintellij.utils

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.util.ui.HTMLEditorKitBuilder
import java.awt.Font
import javax.swing.JEditorPane
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.StyleSheet
import java.net.URI
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import com.intellij.util.ui.UIUtil

/**
 * Utility class for rendering Markdown content in the chat UI.
 */
object MarkdownRenderer {
    private val flavour = CommonMarkFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    private val parserDelegator = ParserDelegator()
    
    private val defaultStyleSheet = StyleSheet()

    // Define theme-aware colors using JBColor
    private val backgroundColor = JBColor(0xFFFFFF, 0x2b2d30)
    private val codeBackgroundColor = JBColor(0xf6f8fa, 0x2b2d30)
    private val textColor = JBColor(0x000000, 0xbababa)
    private val linkColor = JBColor(0x4A90E2, 0x589df6)
    private val quoteBorderColor = JBColor(0xdddddd, 0x3d3f41)
    private val quoteTextColor = JBColor(0x666666, 0x9b9b9b)
    
    // Syntax highlighting colors
    private val keywordColor = JBColor(0x000080, 0xcc7832)
    private val stringColor = JBColor(0x008000, 0x6a8759)
    private val numberColor = JBColor(0x0000ff, 0x6897bb)
    private val commentColor = JBColor(0x808080, 0x808080)
    private val functionColor = JBColor(0x000000, 0xffc66d)
    private val typeColor = JBColor(0x008080, 0xa9b7c6)
    private val variableColor = JBColor(0x000000, 0xa9b7c6)
    private val constantColor = JBColor(0x660e7a, 0x9876aa)
    private val operatorColor = JBColor(0x000000, 0xa9b7c6)

    init {
        updateStyleSheet()
    }

    private fun updateStyleSheet() {
        defaultStyleSheet.addRule(
            """
            body { 
                font-family: ${Font.SANS_SERIF}; 
                margin: 8px; 
                line-height: 1.4;
                color: ${colorToCSS(textColor)};
                background-color: transparent; 
            }
            code { 
                font-family: monospace; 
                background-color: ${colorToCSS(codeBackgroundColor)}; 
                padding: 2px 4px; 
                border-radius: 3px;
                color: ${colorToCSS(functionColor)}; 
            }
            pre { 
                background-color: ${colorToCSS(codeBackgroundColor)}; 
                padding: 16px; 
                border-radius: 6px; 
                overflow: auto; 
            }
            pre code { 
                background-color: transparent; 
                padding: 0; 
            }
            /* Syntax highlighting colors */
            .keyword { 
                color: ${colorToCSS(keywordColor)}; 
                font-weight: bold;
            }
            .string { color: ${colorToCSS(stringColor)}; }
            .number { color: ${colorToCSS(numberColor)}; }
            .comment { color: ${colorToCSS(commentColor)}; font-style: italic; }
            .function { color: ${colorToCSS(functionColor)}; }
            .type { color: ${colorToCSS(typeColor)}; }
            .variable { color: ${colorToCSS(variableColor)}; }
            .constant { color: ${colorToCSS(constantColor)}; }
            .operator { color: ${colorToCSS(operatorColor)}; }
            .punctuation { color: ${colorToCSS(textColor)}; }
            a { 
                color: ${colorToCSS(linkColor)}; 
                text-decoration: none; 
            }
            a:hover { 
                text-decoration: underline; 
            }
            p { 
                margin: 0 0 16px 0; 
            }
            h1 { 
                font-size: 2em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            h2 { 
                font-size: 1.5em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            h3 { 
                font-size: 1.17em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            h4 { 
                font-size: 1em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            h5 { 
                font-size: 0.83em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            h6 { 
                font-size: 0.67em; 
                margin-top: 24px; 
                margin-bottom: 16px; 
            }
            ul, ol { 
                margin: 0 0 16px 0; 
                padding-left: 24px; 
            }
            blockquote { 
                margin: 0 0 16px 0; 
                padding: 0 16px; 
                border-left: 4px solid ${colorToCSS(quoteBorderColor)}; 
                color: ${colorToCSS(quoteTextColor)}; 
            }
        """.trimIndent())
    }

    /**
     * Converts a JBColor to a CSS color string, respecting the current theme.
     */
    private fun colorToCSS(color: JBColor): String {
        return "rgb(${color.red}, ${color.green}, ${color.blue})"
    }

    /**
     * Creates a JEditorPane configured for displaying Markdown content.
     */
    fun createMarkdownPane(): JEditorPane {
        val styleSheet = defaultStyleSheet
        val editorKit = HTMLEditorKitBuilder()
            .withStyleSheet(styleSheet)
            .build()

        return JEditorPane().apply {
            setEditorKit(editorKit)
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            document = createEmptyDocument()
        }
    }

    private fun createEmptyDocument(): HTMLDocument {
        return HTMLDocument(defaultStyleSheet).apply {
            putProperty("IgnoreCharsetDirective", true)
            setPreservesUnknownTags(false)
            parser = parserDelegator
        }
    }

    /**
     * Renders Markdown text to HTML and updates the provided JEditorPane.
     */
    fun setMarkdownContent(editorPane: JEditorPane, markdownText: String, append: Boolean = false) {
        if (markdownText.isBlank() && !append) {
            editorPane.document = createEmptyDocument()
            return
        }

        // Parse markdown to AST
        val parsedTree = parser.buildMarkdownTreeFromString(markdownText)

        // Generate HTML
        val html = HtmlGenerator(
            markdownText,
            parsedTree,
            flavour.createHtmlGeneratingProviders(LinkMap(mapOf()), URI("file:///"))
        ).generateHtml()

        val processedHtml = html.substringAfter("<body>").substringBefore("</body>")

        if (append && editorPane.document is HTMLDocument) {
            val htmlDoc = editorPane.document as HTMLDocument
            try {
                val root = htmlDoc.defaultRootElement
                for (i in 0 until root.elementCount) {
                    val elem = root.getElement(i)
                    if (elem.name == "body") {
                        htmlDoc.insertBeforeEnd(elem, processedHtml)
                        editorPane.caretPosition = editorPane.document.length
                        return
                    }
                }
            } catch (e: Exception) {
                editorPane.document = createEmptyDocument()
            }
        }

        // Full replace if not appending or if append failed
        val wrappedHtml = """
            <!DOCTYPE html>
            <html>
                <head>
                    <meta charset="UTF-8">
                    <style type="text/css">
                        body { font-family: ${Font.SANS_SERIF}; }
                    </style>
                </head>
                <body>
                    $processedHtml
                </body>
            </html>
        """.trimIndent()

        editorPane.document = createEmptyDocument()
        editorPane.text = wrappedHtml
    }
}
