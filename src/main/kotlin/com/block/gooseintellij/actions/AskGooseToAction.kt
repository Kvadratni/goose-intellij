package com.block.gooseintellij.actions

import com.block.gooseintellij.service.GooseChatService
import com.block.gooseintellij.service.ChatPanelService
import com.block.gooseintellij.service.StreamHandler
import com.block.gooseintellij.ui.components.editor.EditorComponentInlaysManager
import com.block.gooseintellij.ui.components.chat.InlineChatPanel
import com.block.gooseintellij.ui.components.chat.FilePillComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.*

class AskGooseToAction : AnAction() {
  private fun getChatService(project: Project): GooseChatService {
    return project.getService(GooseChatService::class.java)
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val editor = event.getData(CommonDataKeys.EDITOR) as? EditorEx
    val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
    val psiFile = event.getData(CommonDataKeys.PSI_FILE)
    
    // Create file pills map if we have a valid file
    val filePills = mutableMapOf<FilePillComponent, String>()
    psiFile?.virtualFile?.let { vFile ->
      // Create a pill component with the virtual file
      val pill = FilePillComponent(project, vFile, true) { /* Remove handler not needed for this context */ }
      pill.removeButton()
      // Only add if path is not already present
      if (!FilePillComponent.hasFilePathInPills(vFile.path, filePills)) {
        filePills[pill] = vFile.path
      }
    }
    
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
      inlineChatPanel.setMessageHandler { userInput, _ ->
        val selectedText = editor.selectionModel.selectedText
        val enhancedPromptTemplate = buildPrompt(userInput, selectedText, psiFile?.virtualFile?.path, filePills)
        // Ensure unique pills before handling the message
        val uniquePills = filePills.let { pills -> FilePillComponent.getUniquePills(pills) }
        // First post user message to main chat panel
        val chatPanelService = ChatPanelService.getInstance(project)
        chatPanelService.appendMessage(userInput, true, uniquePills)
        chatPanelService.showLoadingIndicator()
        
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
      // Ensure the Goose Chat tool window is visible
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Chat")
      toolWindow?.show()

      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = buildPrompt(userInput, null, psiFile?.virtualFile?.path, filePills)
      // Ensure unique pills before handling the message
      val uniquePills = filePills.let { pills -> FilePillComponent.getUniquePills(pills) }
      
      // Get the chat panel service and prepare it
      val chatPanelService = ChatPanelService.getInstance(project)
      chatPanelService.appendMessage(userInput, true, uniquePills)
      chatPanelService.showLoadingIndicator()
      
      // Get the chat service and send the message
      getChatService(project).sendMessage(
        message = enhancedPromptTemplate,
        streaming = true,
        streamHandler = createFileViewStreamHandler(project)
      ).exceptionally { e ->
        SwingUtilities.invokeLater {
          chatPanelService.appendMessage("Error sending message: ${e.message}", false)
          Messages.showErrorDialog(project, "Error sending message: ${e.message}", "Error")
        }
        null
      }
    } ?: showErrorMessage(project)
  }

  private fun buildPrompt(userInput: String, selectedText: String?, filePath: String?, filePills: Map<FilePillComponent, String>? = null): String {
    val contextBuilder = StringBuilder()
    
    // Add selection context if present
    when {
        selectedText != null -> contextBuilder.append("Context: $selectedText in file: $filePath")
        filePath != null -> contextBuilder.append("Context: file: $filePath")
    }
    
    // Add file pills context if present
    if (!filePills.isNullOrEmpty()) {
        if (contextBuilder.isNotEmpty()) {
            contextBuilder.append("\n")
        }
        contextBuilder.append("Additional files:\n")
        filePills.forEach { (_, path) ->
            contextBuilder.append("- $path\n")
        }
    }
    
    return if (contextBuilder.isNotEmpty()) "$userInput\n\n${contextBuilder.toString()}" else userInput
  }

  private fun createFileViewStreamHandler(project: Project): StreamHandler {
    val chatPanelService = ChatPanelService.getInstance(project)
    
    return object : StreamHandler {
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
      
      override fun onToolCallDelta(toolCallId: String, argsTextDelta: String) {
        SwingUtilities.invokeLater {
          chatPanelService.appendMessage(argsTextDelta, false)
        }
      }
      
      override fun onToolCall(toolCallId: String, toolName: String, args: Map<String, Any>) {
        SwingUtilities.invokeLater {
          chatPanelService.appendMessage("\n🛠️ Calling tool: $toolName", false)
        }
      }
      
      override fun onToolResult(toolCallId: String, result: Any) {
        SwingUtilities.invokeLater {
          chatPanelService.appendMessage("\n✅ Tool result: $result\n", false)
        }
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
