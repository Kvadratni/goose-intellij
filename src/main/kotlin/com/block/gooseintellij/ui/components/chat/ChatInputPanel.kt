package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.ui.components.common.CustomFocusTraversalPolicy
import com.block.gooseintellij.utils.GooseIcons
import com.block.gooseintellij.ui.components.common.GooseRoundedActionButton
import com.block.gooseintellij.viewmodel.ChatViewModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent

class ChatInputPanel(
    icon: Icon,
    private val editor: EditorEx,
    private val sendAction: (String) -> Unit
) : JPanel(BorderLayout()) {
    private val viewModel: ChatViewModel = ChatViewModel(editor)
    private val bd = IdeBorderFactory.createRoundedBorder(9, 1)
    private val scrollPane: JScrollPane
    private val inputField: JTextArea = JTextArea().apply {
        background = JBColor.background()
        margin = JBUI.insets(10)
        lineWrap = true
        wrapStyleWord = true
        
        val inputMap = getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = actionMap
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage")
        inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "insertNewLine")
        
        actionMap.put("sendMessage", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                viewModel.handleSendAction(text) { message ->
                    sendAction(message)
                    text = ""
                }
            }
        })
        
        actionMap.put("insertNewLine", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                append("\n")
                viewModel.handleTextChange(this@apply)
            }
        })
        
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                viewModel.handleTextChange(this@apply)
                toggleSendButton()
            }
        })

        document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) {
                viewModel.handleTextChange(this@apply)
            }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) {
                viewModel.handleTextChange(this@apply)
            }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) {
                viewModel.handleTextChange(this@apply)
            }
        })

        addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) {
                bd.setColor(JBColor.BLUE)
            }
            override fun focusLost(e: java.awt.event.FocusEvent) {
                bd.setColor(JBColor.GRAY)
            }
        })
    }

    private val sendButton = GooseRoundedActionButton(icon, 10).apply {
        addActionListener { 
            viewModel.handleSendAction(inputField.text) { message ->
                sendAction(message)
                inputField.text = ""
            }
        }
        background = JBColor.background()
        cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        isEnabled = false
    }

    private val iconLabel = JLabel(GooseIcons.GooseAction).apply {
        border = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
    }

    init {
        putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
        focusTraversalPolicy = CustomFocusTraversalPolicy(listOf(inputField, sendButton, iconLabel))
        isFocusCycleRoot = true
        setFocusable(true)
        
        bd.setColor(JBColor.GRAY)
        val padding = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
        border = BorderFactory.createCompoundBorder(padding, bd)

        add(iconLabel, BorderLayout.WEST)
        scrollPane = JBScrollPane(inputField).apply {
            border = null
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            val lineHeight = inputField.getFontMetrics(inputField.font).height
            preferredSize = null
            maximumSize = Dimension(Int.MAX_VALUE, lineHeight * ChatViewModel.MAX_LINE_COUNT)
        }
        
        add(scrollPane, BorderLayout.CENTER)
        add(sendButton, BorderLayout.EAST)
        maximumSize = Dimension(Int.MAX_VALUE, inputField.getFontMetrics(inputField.font).height * ChatViewModel.MAX_LINE_COUNT)

        SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
    }

    private fun toggleSendButton() {
        sendButton.isEnabled = inputField.text.trim().isNotEmpty()
        sendButton.icon = if (sendButton.isEnabled) GooseIcons.SendToGoose else GooseIcons.SendToGooseDisabled
    }

    fun adjustViewport() {
        val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this)
        parent?.validate()
        parent?.revalidate()
        parent?.repaint()
    }
}
