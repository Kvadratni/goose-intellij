package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

const val GOOSE_COMMAND_FORMAT = "Please analyze the following code %s\n in file: %s\n"

class SendToGooseAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        GooseActionHelper.checkAndSendToGoose(event, GOOSE_COMMAND_FORMAT) { e ->
            val editor = e.getData(CommonDataKeys.EDITOR)
            val selectedText = editor?.selectionModel?.selectedText
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
            Pair(selectedText, psiFile?.virtualFile?.path)
        }
    }
}
