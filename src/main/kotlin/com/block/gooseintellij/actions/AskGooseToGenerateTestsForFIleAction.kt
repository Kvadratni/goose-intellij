package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.actionSystem.ActionUpdateThread

class AskGooseToGenerateTestsForFileAction : AnAction() {

    private val logger = Logger.getInstance(AskGooseToGenerateTestsForFileAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        logger.info("ActionPerformed triggered")

        val project = event.project

        if (!GooseActionHelper.checkGooseAvailability(project)) return

        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        logger.info("Project: $project, VirtualFile: $virtualFile")

        if (virtualFile != null) {
            val gooseTerminal = GooseActionHelper.getGooseTerminal(event)
            val question = "Analyze the current repo for the correct location of unit tests. Generate unit tests for the file: ${virtualFile.path}?"
            GooseActionHelper.askGooseToGenerateTests(gooseTerminal, question)
        } else {
            Messages.showMessageDialog(
                "No file or directory selected.", "Warning", Messages.getWarningIcon()
            )
            logger.warn("No file or directory selected.")
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
