package com.block.gooseintellij.actions

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
      // Ensure previous inlay components are resized or removed
      manager.dispose()
      
      val chatPanel = InlineChatPanel(it, event, inlayRef) { userInput ->
        val selectedText = editor.selectionModel.selectedText
        val enhancedPromptTemplate = "$userInput. Context: %s in file: $%s"
        sendToGoose(event, enhancedPromptTemplate, selectedText, event.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.path, true)
      }
      
      val inlay = manager.insertAfter(lineNumber, chatPanel)
      inlayRef.set(inlay)
      
      val viewport = it.scrollPane.viewport
      viewport.dispatchEvent(java.awt.event.ComponentEvent(viewport, java.awt.event.ComponentEvent.COMPONENT_RESIZED))
    } ?: virtualFile?.let {
      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = "$userInput. Context: file: $%s"
      sendToGoose(event, enhancedPromptTemplate, "", virtualFile.path, false)
    } ?: showErrorMessage(project)
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

  private fun sendToGoose(
    event: AnActionEvent,
    prompt: String,
    selectedText: String?,
    filePath: String?,
    isEditor: Boolean,
  ) {
    GooseActionHelper.checkAndSendToGoose(event, prompt) {
      Triple(selectedText, filePath, isEditor)
    }
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
