package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.ui.components.common.RoundedPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Ref
import java.awt.BorderLayout
import javax.swing.*

class InlineChatPanel(
    editor: EditorEx,
    event: AnActionEvent,
    inlayRef: Ref<Disposable>,
    onSend: (String) -> Unit
) : RoundedPanel(BorderLayout()) {
    init {
        val action = object : AnAction({ "Close" }, AllIcons.Actions.Close) {
            override fun actionPerformed(e: AnActionEvent) {
                inlayRef.get()?.dispose()
            }
        }
        val closeButton = ActionButton(
            action,
            action.templatePresentation.clone(),
            ActionPlaces.TOOLBAR,
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
        
        val chatInputPanel = ChatInputPanel(com.block.gooseintellij.utils.GooseIcons.SendToGooseDisabled, editor) { userInput ->
            onSend(userInput)
            com.intellij.openapi.actionSystem.ActionManager.getInstance().tryToExecute(action, event.inputEvent, null, ActionPlaces.UNKNOWN, true)
        }

        add(chatInputPanel, BorderLayout.CENTER)
        add(closeButton, BorderLayout.EAST)

        addHierarchyListener { e ->
            if ((e.changeFlags and java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) {
                SwingUtilities.invokeLater {
                    revalidate()
                    repaint()
                    editor.contentComponent.validate()
                }
            }
        }

        addAncestorListener(object : javax.swing.event.AncestorListener {
            override fun ancestorAdded(e: javax.swing.event.AncestorEvent) {
                SwingUtilities.invokeLater {
                    revalidate()
                    repaint()
                    val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this@InlineChatPanel)
                    parent?.validate()
                    parent?.revalidate()
                    parent?.repaint()
                }
            }
            override fun ancestorRemoved(e: javax.swing.event.AncestorEvent) {}
            override fun ancestorMoved(e: javax.swing.event.AncestorEvent) {}
        })
    }
}
