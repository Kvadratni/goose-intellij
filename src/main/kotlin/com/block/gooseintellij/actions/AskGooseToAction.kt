package com.block.gooseintellij.actions

import com.block.gooseintellij.components.ChatInputPanel
import com.block.gooseintellij.components.EditorComponentInlaysManager
import com.block.gooseintellij.utils.GooseIcons.SendToGooseDisabled
import com.intellij.collaboration.ui.codereview.comment.RoundedPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.util.Ref
import com.intellij.ui.AncestorListenerAdapter
import javax.swing.*
import java.awt.BorderLayout
import java.awt.event.ComponentEvent
import javax.swing.event.AncestorEvent

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
      val chatPanel = createInlineChatPanel(it, event, inlayRef)          
      val inlay = manager.insertAfter(lineNumber, chatPanel)
      inlayRef.set(inlay)
      chatPanel.addHierarchyListener { e ->
        if ((e.changeFlags and java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) {
            SwingUtilities.invokeLater {
                chatPanel.revalidate()
                chatPanel.repaint()
                it.contentComponent.validate()
            }
        }
      }

      chatPanel.addAncestorListener(object : AncestorListenerAdapter() {
        override fun ancestorAdded(e: AncestorEvent) {
            SwingUtilities.invokeLater {
              chatPanel.revalidate()
              chatPanel.repaint()
              val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, chatPanel)
              parent?.validate()
              parent?.revalidate()
              parent?.repaint()
            }
        }
      })
      val viewport = it.scrollPane.viewport
      viewport.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))
    } ?: virtualFile?.let {
      val userInput = fetchUserInput(project) ?: return
      val enhancedPromptTemplate = "$userInput. Context: file: $%s"
      sendToGoose(event, enhancedPromptTemplate, "", virtualFile.path, false)
    } ?: showErrorMessage(project)
  }

  private fun createInlineChatPanel(
    editor: EditorEx,
    event: AnActionEvent,
    inlayRef: Ref<Disposable>
  ): JPanel {
    val action = object : AnAction({ "Close" }, AllIcons.Actions.Close) {
      override fun actionPerformed(e: AnActionEvent) {
        inlayRef.get().dispose()
      }
    }
    val closeButton = ActionButton(
      action,
      action.templatePresentation.clone(),
      ActionPlaces.TOOLBAR,
      ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
    val psiFile = event.getData(CommonDataKeys.PSI_FILE)
    val chatInputPanel = ChatInputPanel(SendToGooseDisabled) { userInput ->
      val selectedText = editor.selectionModel.selectedText
      val enhancedPromptTemplate = "$userInput. Context: %s in file: $%s"
      sendToGoose(event, enhancedPromptTemplate, selectedText, psiFile?.virtualFile?.path, true)
      action.actionPerformed(event)
    }

    return RoundedPanel(BorderLayout()).apply {
      add(chatInputPanel, BorderLayout.CENTER)
      add(closeButton, BorderLayout.EAST)
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
