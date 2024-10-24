package com.block.gooseintellij.components

import com.block.gooseintellij.utils.GooseIcons
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

private const val MAX_LINE_COUNT = 25

class ChatInputPanel(
  icon: Icon,
  private val editor: EditorEx,
  private val sendAction: (String) -> Unit
) :
  JPanel(BorderLayout()) {
  private val bd = IdeBorderFactory.createRoundedBorder(9, 1)
  private val scrollPane: JScrollPane
  private val inputField: JTextArea = JTextArea().apply {
    background = JBColor.background()
    margin = JBUI.insets(10)
    lineWrap = true
    wrapStyleWord = true
    // Enable text wrapping
    // Key bindings for Enter and Shift + Enter
    val inputMap = getInputMap(JComponent.WHEN_FOCUSED)
    val actionMap = actionMap
    inputMap.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage")
    inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "insertNewLine")
    actionMap.put("sendMessage", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        triggerSendAction()
      }
    })
    actionMap.put("insertNewLine", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        append("\n")
        revalidatePanelSize()
      }
    })
    addKeyListener(object : java.awt.event.KeyAdapter() {
      override fun keyPressed(e: java.awt.event.KeyEvent) {
        revalidatePanelSize()
        toggleSendButton()
      }
    })

    // Listen for document changes to adjust panel size
    document.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
      }

      override fun removeUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
      }

      override fun changedUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
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
    addActionListener { triggerSendAction() }
    background = JBColor.background()
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    isEnabled = false
  }
  private val iconLabel = JLabel(GooseIcons.GooseAction).apply {
    border = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
  }

  init {
    putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
    focusTraversalPolicy =
      CustomFocusTraversalPolicy(
        listOf(
          inputField,
          sendButton,
          iconLabel
        )
      ) // Direct focus adjustments on inputField itself
    isFocusCycleRoot = true
    setFocusable(true)
    bd.setColor(JBColor.GRAY)
    val padding = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
    border = BorderFactory.createCompoundBorder(padding, bd)


    add(iconLabel, BorderLayout.WEST)
    scrollPane = JBScrollPane(inputField)
    scrollPane.border = null
    scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
    val lineHeight = inputField.getFontMetrics(inputField.font).height
    scrollPane.preferredSize = null
    scrollPane.maximumSize = Dimension(Int.MAX_VALUE, lineHeight * MAX_LINE_COUNT)
    add(scrollPane, BorderLayout.CENTER)
    add(sendButton, BorderLayout.EAST)
    maximumSize = Dimension(Int.MAX_VALUE, lineHeight * MAX_LINE_COUNT)

    // Enable/Disable send button based on input text
    inputField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent) {
        toggleSendButton()
      }

      override fun removeUpdate(e: javax.swing.event.DocumentEvent) {
        toggleSendButton()
      }

      override fun changedUpdate(e: javax.swing.event.DocumentEvent) {
        toggleSendButton()
      }
    })

    // Adjust panel size dynamically based on content
    inputField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
      }

      override fun removeUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
      }

      override fun changedUpdate(e: javax.swing.event.DocumentEvent) {
        revalidatePanelSize()
      }
    })

    // Autofocus to inputField when the panel is displayed
    SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
  }

  private fun toggleSendButton() {
    sendButton.isEnabled = inputField.text.trim().isNotEmpty()
    sendButton.icon =
      if (sendButton.isEnabled) GooseIcons.SendToGoose else GooseIcons.SendToGooseDisabled
  }

  fun adjustViewport() {
    val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this)
    parent?.validate()
    parent?.revalidate()
    parent?.repaint()
  }

  fun revalidatePanelSize() {
    repaint()
    invalidate()
    SwingUtilities.invokeLater {
      updateEditorLayout()
      adjustSizeBasedOnContent()
      revalidate()
      repaint()
      adjustViewport()
    }
  }

  private fun adjustSizeBasedOnContent() {
    val lineHeight = inputField.getFontMetrics(inputField.font).height
    val contentHeight = inputField.preferredSize.height
    val numLines = contentHeight / lineHeight

    if (numLines > MAX_LINE_COUNT) {
      scrollPane.preferredSize = Dimension(inputField.width, lineHeight * MAX_LINE_COUNT)
      scrollPane.maximumSize = Dimension(Int.MAX_VALUE, lineHeight * MAX_LINE_COUNT)
      scrollPane.scrollRectToVisible(Rectangle(0, scrollPane.height, 1, 1))
    } else {
      scrollPane.preferredSize = Dimension(inputField.width, contentHeight)
    }
    revalidate()
    repaint()
  }

  private fun updateEditorLayout() {
    editor.contentComponent.revalidate()
    editor.contentComponent.repaint()
  }

  private fun triggerSendAction() {
    val text = inputField.text.trim()
    if (text.isNotEmpty()) {
      sendAction(text.replace("\n", " "))
      inputField.text = ""
    }
  }
}
