package com.block.gooseintellij.ui.components.chat

import com.intellij.openapi.project.Project
import com.block.gooseintellij.ui.components.common.CustomFocusTraversalPolicy
import com.block.gooseintellij.utils.GooseIcons
import com.block.gooseintellij.ui.components.common.GooseRoundedActionButton
import com.block.gooseintellij.viewmodel.ChatViewModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent

/**
 * A panel for chat input that supports both text input and file attachments.
 *
 * @param project The current IntelliJ project
 * @param icon The icon to display in the send button
 * @param editor Optional editor instance
 * @param sendAction Callback invoked when sending a message. Takes the message text and an optional map of file pills.
 *                  The map contains FilePillComponent instances mapped to their file paths, or null if no files are attached.
 */
class ChatInputPanel(
  private val project: Project,
  icon: Icon,
  private val editor: EditorEx? = null,
  private val sendAction: (String, Map<FilePillComponent, String>?) -> Unit
) : JPanel(BorderLayout()) {
  private val viewModel: ChatViewModel = ChatViewModel(editor)
  private val bd = IdeBorderFactory.createRoundedBorder(9, 1)
  private val scrollPane: JScrollPane
  private var filePillPanel: JPanel? = null

  private fun createFilePillPanel(): JPanel {
    return JPanel().apply {
      layout = FlowLayout(FlowLayout.LEFT, 5, 5)
      isOpaque = false
    }
  }

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
          sendMessage(message)
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
      viewModel.handleSendAction(inputField.text) { processedMessage ->
        sendMessage(processedMessage)
      }
    }
    background = JBColor.background()
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    isEnabled = false
  }

  private fun sendMessage(processedMessage: String) {
    val pillsToSend = if (filePills.isNotEmpty()) {
        // Ensure unique pills before sending
        FilePillComponent.getUniquePills(filePills.toMap())
    } else null
    pillsToSend?.let { it.keys.forEach { pill -> pill.removeButton() } }
    sendAction(processedMessage, pillsToSend)
    inputField.text = ""
    if (filePills.isNotEmpty()) {
      val pillEntry = filePills.entries.first();
      filePills = mutableMapOf(pillEntry.key to pillEntry.value)
      pillEntry.key.onRemove(pillEntry.key.file.name);
    }
    adjustViewport()
  }

  private val iconLabel = JLabel(GooseIcons.GooseAction).apply {
    border = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
  }

  // Store file references for pills
  private var filePills = mutableMapOf<FilePillComponent, String>()

  fun getFilePills(): Map<FilePillComponent, String> = filePills.toMap()

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
    maximumSize = Dimension(
      Int.MAX_VALUE,
      inputField.getFontMetrics(inputField.font).height * ChatViewModel.MAX_LINE_COUNT
    )

    SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
  }

  private fun toggleSendButton() {
    val hasContent = inputField.document.length > 0 || filePills.isNotEmpty()
    sendButton.isEnabled = hasContent
    sendButton.icon = if (hasContent) GooseIcons.SendToGoose else GooseIcons.SendToGooseDisabled
  }

  fun adjustViewport() {
    val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this)
    parent?.validate()
    parent?.revalidate()
    parent?.repaint()
  }

  fun appendFileTag(virtualFile: VirtualFile) {
    // Create a new pill component with close button
    val pill = FilePillComponent(project, virtualFile, showCloseButton = true) {
      // Remove the pill from the panel
      val pill = filePills.entries.find { (pill, filepath) -> pill.file.name == it }?.key
      filePills.remove(pill)
      filePillPanel?.remove(pill)

      // If no more pills, remove the panel
      if (filePills.isEmpty()) {
        filePillPanel?.let { panel ->
          remove(panel)
          filePillPanel = null
          revalidate()
          repaint()
        }
      }
      adjustViewport()
    }

    // Check if file path already exists
    if (FilePillComponent.hasFilePathInPills(virtualFile.path, filePills)) {
        return // Skip adding duplicate file
    }
    
    // Store the file path
    filePills[pill] = virtualFile.path

    // Create and add filePillPanel if it doesn't exist
    if (filePillPanel == null) {
      filePillPanel = createFilePillPanel()
      filePillPanel?.let { add(it, BorderLayout.SOUTH) }
      revalidate()
      repaint()
    }

    filePillPanel?.add(pill)
    adjustViewport()
  }
}
