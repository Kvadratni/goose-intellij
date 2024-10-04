package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class AskGooseToAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val editor = event.getData(CommonDataKeys.EDITOR)
    val psiFile = event.getData(CommonDataKeys.PSI_FILE)
    val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)

    editor?.let {
      val selectedText = it.selectionModel.selectedText
      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = "$userInput. Context: %s\n in file: $%s\n"
      sendToGoose(event, enhancedPromptTemplate, selectedText, psiFile?.virtualFile?.path, true)
    } ?: virtualFile?.let {
      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = "$userInput. Context: file: $%s\n"
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
      "No selection found.",
      "Error",
      Messages.getErrorIcon()
    )
  }
}
