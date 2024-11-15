package com.block.gooseintellij.actions

import com.block.gooseintellij.service.GooseChatService
import com.block.gooseintellij.service.ChatPanelService
import com.block.gooseintellij.ui.components.editor.EditorComponentInlaysManager
import com.block.gooseintellij.ui.components.chat.InlineChatPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Ref
import javax.swing.*

class AskGooseToAction : AnAction() {
  private fun getChatService(project: Project): GooseChatService {
    return project.getService(GooseChatService::class.java)
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val editor = event.getData(CommonDataKeys.EDITOR) as? EditorEx
    val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
    
    editor?.let {
      val manager = EditorComponentInlaysManager.from(editor)
      // Close any existing inline chat panels
      manager.dispose()

      val lineNumber = it.document.getLineNumber(editor.caretModel.offset)
      val inlayRef = Ref<Disposable>()
      
      // Create the chat panel
      val inlineChatPanel = InlineChatPanel(it, event, inlayRef)
      val inlay = manager.insertAfter(lineNumber, inlineChatPanel)
      inlayRef.set(inlay)

      // Set up the chat panel's message handler
      inlineChatPanel.setMessageHandler { userInput ->
        val selectedText = editor.selectionModel.selectedText
        val enhancedPromptTemplate = buildPrompt(userInput, selectedText, event.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.path)
        
        // Get the chat service and send the message with streaming
        getChatService(project).sendMessage(
          message = enhancedPromptTemplate,
          streaming = true,
          streamHandler = createFileViewStreamHandler(project)
        ).exceptionally { e ->
          SwingUtilities.invokeLater {
            ChatPanelService.getInstance(project).appendMessage("Error sending message: ${e.message}", false)
          }
          null
        }
      }
      
      val viewport = it.scrollPane.viewport
      viewport.dispatchEvent(java.awt.event.ComponentEvent(viewport, java.awt.event.ComponentEvent.COMPONENT_RESIZED))
    } ?: virtualFile?.let {
      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = buildPrompt(userInput, null, virtualFile.path)
      
      // Get the chat service and send the message
      getChatService(project).sendMessage(
        message = enhancedPromptTemplate,
        streaming = true,
        streamHandler = createFileViewStreamHandler(project)
      ).exceptionally { e ->
        SwingUtilities.invokeLater {
          Messages.showErrorDialog(project, "Error sending message: ${e.message}", "Error")
        }
        null
      }
    } ?: showErrorMessage(project)
  }

  private fun buildPrompt(userInput: String, selectedText: String?, filePath: String?): String {
    val context = when {
      selectedText != null -> "Context: $selectedText in file: $filePath"
      filePath != null -> "Context: file: $filePath"
      else -> ""
    }
    return if (context.isNotEmpty()) "$userInput. $context" else userInput
  }

  private fun createFileViewStreamHandler(project: Project): GooseChatService.StreamHandler {
    val chatPanelService = ChatPanelService.getInstance(project)
    
    return object : GooseChatService.StreamHandler {
      override fun onText(text: String) {
        SwingUtilities.invokeLater {
          // Update the chat panel in the tool window
          chatPanelService.appendMessage(text, false)
          chatPanelService.hideLoadingIndicator()
        }
      }
      
      override fun onData(data: List<Any>) {
        // Handle structured data if needed
      }
      
      override fun onError(error: String) {
        SwingUtilities.invokeLater {
          chatPanelService.appendMessage("Error: $error", false)
          Messages.showErrorDialog(project, error, "Error")
        }
      }
      
      override fun onMessageAnnotation(annotation: Map<String, Any>) {
        // Handle annotations if needed
      }
      
      override fun onFinish(finishReason: String, usage: Map<String, Int>) {
        // No need to handle completion as ChatPanel manages message state
      }
    }
  }

  private fun fetchUserInput(project: Project): String? {
    return Messages.showInputDialog(
      project,
      "Ask Goose to:",
      "Ask Goose",
      Messages.getQuestionIcon(),
      "",
      null
    )
  }

  private fun showErrorMessage(project: Project) {
    Messages.showMessageDialog(
      project,
      "Failed to ask goose.",
      "Error",
      Messages.getErrorIcon()
    )
  }
}
