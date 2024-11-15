package com.block.gooseintellij.utils

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.HTMLEditorKitBuilder
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import javax.swing.JEditorPane
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.StyleSheet

/**
 * Utility class for rendering Markdown content in the chat UI.
 */
object MarkdownRenderer {
    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder()
        .softbreak("<br/>")
        .build()

    private val styleSheet = StyleSheet().apply {
        addRule("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif; margin: 0; padding: 0; }")
        addRule("code { font-family: 'JetBrains Mono', monospace; background-color: #f6f8fa; padding: 2px 4px; border-radius: 3px; }")
        addRule("pre { background-color: #f6f8fa; padding: 8px; border-radius: 6px; overflow-x: auto; }")
        addRule("pre > code { background-color: transparent; padding: 0; }")
        addRule("a { color: #4A9EEA; }")
        addRule("p { margin: 0 0 8px 0; }")
        addRule("ul, ol { margin: 0 0 8px 0; padding-left: 20px; }")
        addRule("blockquote { margin: 0 0 8px 0; padding-left: 8px; border-left: 3px solid #d0d7de; color: #656d76; }")
    }

    /**
     * Creates a JEditorPane configured for displaying Markdown content.
     */
    fun createMarkdownPane(): JEditorPane {
        return JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder()
                .withStyleSheet(styleSheet)
                .build()
            isEditable = false
            isOpaque = false
            addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        }
    }

    /**
     * Renders Markdown text to HTML and updates the provided JEditorPane.
     */
    fun setMarkdownContent(editorPane: JEditorPane, markdownText: String) {
        val document = parser.parse(markdownText)
        val html = renderer.render(document)
        val wrappedHtml = """
            <html>
                <body>
                    $html
                </body>
            </html>
        """.trimIndent()
        
        editorPane.document = HTMLDocument().apply {
            putProperty("IgnoreCharsetDirective", true)
            setPreservesUnknownTags(false)
        }
        editorPane.text = wrappedHtml
    }
}