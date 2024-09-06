package com.block.gooseintellij

import com.block.gooseintellij.toolWindow.GooseTerminalPanel
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.annotations.NotNull

class AskGooseToCompleteCode : IntentionAction {
    @NotNull
    override fun getText(): String {
        return "Ask Goose to complete the code"
    }

    @NotNull
    override fun getFamilyName(): String {
        return text
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Goose Terminal")
        val contentManager = toolWindow?.contentManager
        val content = contentManager?.getContent(0)
        val gooseTerminalPanel = content?.component as? GooseTerminalPanel

        val document = editor?.document
        val caretModel = editor?.caretModel
        val offset = caretModel?.offset

        if (document != null && offset != null) {
            val lineNumber = document.getLineNumber(offset) + 1
            val command = "Please complete the code around line: $lineNumber in file: ${file?.virtualFile?.path}"
            gooseTerminalPanel?.processInput(command)
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }
}
