package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.service.ChatPanelService
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
    private val inlayRef: Ref<Disposable>
) : RoundedPanel(BorderLayout()) {
    private var messageHandler: ((String, Map<FilePillComponent, String>?) -> Unit)? = null
    private val project = event.project!!
    
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
        
        val chatInputPanel = ChatInputPanel(project, com.block.gooseintellij.utils.GooseIcons.SendToGooseDisabled, editor) { userInput, filePills ->
            messageHandler?.invoke(userInput, filePills)
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
    
    fun setMessageHandler(handler: (String, Map<FilePillComponent, String>?) -> Unit) {
        messageHandler = { userInput, _ ->
            // Then handle the message with unique pills
            handler(userInput, null)
            // Close the inline panel after sending
            SwingUtilities.invokeLater {
                inlayRef.get()?.dispose()
            }
        }
    }
    
    // Removed unused response methods since responses go directly to main chat panel
}
