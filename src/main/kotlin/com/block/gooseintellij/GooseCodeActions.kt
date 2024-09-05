package com.block.gooseintellij

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
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
        val document = editor?.document
        val caretModel = editor?.caretModel
        val offset = caretModel?.offset

        if (document != null && offset != null) {
            val lineNumber = document.getLineNumber(offset) + 1

            // Send command to Goose for code completion
            val message = "There is some unfinished code around line: $lineNumber in file: ${file?.virtualFile?.path}, can you please try to complete it as best makes sense."
            val gooseTerminalPanel = GooseTerminalPanel() // You would generally retrieve the singleton instance
            gooseTerminalPanel.printOutput(message)
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }
}
