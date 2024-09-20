package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger

class AskGooseAboutFileOrDirectoryAction : AnAction() {

    private val logger = Logger.getInstance(AskGooseAboutFileOrDirectoryAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
       GooseActionHelper.checkAndSendToGoose(event, "What can you tell me about the file/directory: %s?") { e ->
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            logger.info("Project: ${e.project}, VirtualFile: $virtualFile")
            Pair("", virtualFile?.path)
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
