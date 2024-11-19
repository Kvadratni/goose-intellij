package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.service.GooseChatService
import com.block.gooseintellij.utils.GooseIcons
import com.block.gooseintellij.ui.components.common.LoadingIndicatorPanel
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ChatPanel(
  private val project: Project,
) : JPanel(BorderLayout()) {
  private val chatService: GooseChatService = project.getService(GooseChatService::class.java)
  private val messagesContainer = MessagesContainerPanel()
  private val loadingIndicator = LoadingIndicatorPanel()

  private val inputPanel = ChatInputPanel(
    project = project,
    icon = GooseIcons.SendToGoose,
    sendAction = { message, filePills -> handleSendMessage(message, filePills) }
  )

  init {
    isOpaque = true
    background = JBColor.background()

    val mainPanel = JPanel(BorderLayout()).apply {
      isOpaque = true
      background = JBColor.background()
      add(messagesContainer, BorderLayout.CENTER)
      border = JBUI.Borders.empty(10)
    }

    val centerPanel = JPanel(BorderLayout()).apply {
      isOpaque = true
      background = JBColor.background()
      add(mainPanel, BorderLayout.CENTER)
      add(loadingIndicator, BorderLayout.SOUTH)
    }

    add(centerPanel, BorderLayout.CENTER)
    // Create a panel for input and file selection
    val bottomPanel = JPanel(BorderLayout()).apply {
      isOpaque = true
      background = JBColor.background()

      // Add the input panel
      add(inputPanel, BorderLayout.CENTER)

      // Add file selection button below with a separator
      val fileSelectionPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, 5, 8)).apply {  // Added vertical padding
          isOpaque = true
          background = JBColor.background()
          border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)  // Top border as separator
          add(FileSelectionButton(project, { fileName ->
            // When a file is selected, append it to the input
            inputPanel.appendFileTag(fileName)
          },
            { inputPanel.getFilePills() }
          ))
        }
      add(fileSelectionPanel, BorderLayout.SOUTH)
    }

    add(bottomPanel, BorderLayout.SOUTH)

    // Set preferred size for better initial display
    preferredSize = JBUI.size(400, 600)
    minimumSize = JBUI.size(200, 200)

    // Start the goosed process when panel is created
    chatService.startGoosedProcess().thenRun {
      appendSystemMessage("Goose Chat initialized and ready!")
      revalidate()
      repaint()
    }.exceptionally { e ->
      appendSystemMessage("Error initializing Goose Chat: ${e.message}")
      revalidate()
      repaint()
      null
    }
  }

  private fun handleSendMessage(
    message: String,
    filePills: Map<FilePillComponent, String>? = null
  ) {
    // Add user message bubble with unique pills
    val uniquePills = filePills?.let { FilePillComponent.getUniquePills(it) }
    addMessageBubble(message, true, uniquePills)

    // Show loading indicator
    showLoadingIndicator()

    // Start streaming response
    // Build message with file context if files are attached
    val messageWithContext = buildString {
      append(message)
      if (!filePills.isNullOrEmpty()) {
        append("\n\nAttached files:\n")
        filePills.forEach { (_, path) ->
          append("- $path\n")
        }
      }
    }

    chatService.sendMessage(
      message = messageWithContext,
      streaming = true,
      streamHandler = object : GooseChatService.StreamHandler {
        private var responseBubble: ChatBubbleComponent? = null

        override fun onText(text: String) {
          if (responseBubble == null) {
            responseBubble = addMessageBubble(text, false)
          } else {
            // Update with minimal repaints
            responseBubble?.setText(text, append = true)
          }
        }

        override fun onData(data: List<Any>) {
          // Handle structured data if needed
        }

        override fun onError(error: String) {
          SwingUtilities.invokeLater {
            addMessageBubble("Error: $error", false)
          }
        }

        override fun onMessageAnnotation(annotation: Map<String, Any>) {
          // Handle annotations if needed
        }

        override fun onFinish(finishReason: String, usage: Map<String, Int>) {
          SwingUtilities.invokeLater {
            responseBubble = null
            hideLoadingIndicator()
          }
        }
      }
    ).exceptionally { e ->
      addMessageBubble("Error sending message: ${e.message}", false)
      null
    }
  }

  private fun addMessageBubble(
    message: String,
    isUserMessage: Boolean,
    filePills: Map<FilePillComponent, String>? = null
  ): ChatBubbleComponent {
    val wrapper = JPanel(BorderLayout()).apply {
      isOpaque = false
      alignmentX = LEFT_ALIGNMENT
    }

    val bubble = ChatBubbleComponent(message, isUserMessage, filePills)
    wrapper.add(bubble, if (isUserMessage) BorderLayout.EAST else BorderLayout.WEST)

    messagesContainer.addMessageComponent(wrapper)
    return bubble
  }

  private fun appendSystemMessage(message: String) {
    addMessageBubble(message, false)
  }

  fun appendMessage(message: String, isUserMessage: Boolean, filePills: Map<FilePillComponent, String>? = null) {
    SwingUtilities.invokeLater {
      if (!isUserMessage) {
        // Try to find the last message bubble
        val lastComponent = messagesContainer.getMessagesPanel().components.lastOrNull()
        if (lastComponent is JPanel) {
          val bubble =
            lastComponent.components.firstOrNull { it is ChatBubbleComponent } as? ChatBubbleComponent
          if (bubble != null && !bubble.isUserMessage) {
            bubble.setText(message, true)
            return@invokeLater
          }
        }
      }
      // If we can't append, or it's a user message, create a new bubble
      addMessageBubble(message, isUserMessage, filePills)
    }
  }

  fun showLoadingIndicator() {
    loadingIndicator.startLoading()
  }

  fun hideLoadingIndicator() {
    loadingIndicator.stopLoading()
  }

  fun dispose() {
    chatService.stopGoosedProcess()
  }
}
