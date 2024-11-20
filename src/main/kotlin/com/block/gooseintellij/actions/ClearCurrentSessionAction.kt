package com.block.gooseintellij.actions

import com.block.gooseintellij.state.GooseConversationState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ClearCurrentSessionAction : AnAction("Clear Current Session") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to clear the current session? This will delete the conversation history.",
            "Clear Current Session",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            // Clear conversation state
            val conversationState = GooseConversationState.getInstance(project)
            conversationState.clearCurrentConversation()
        }
    }
}