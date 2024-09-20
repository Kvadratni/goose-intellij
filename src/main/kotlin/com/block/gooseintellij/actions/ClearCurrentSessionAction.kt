package com.block.gooseintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ide.util.PropertiesComponent
import java.io.File

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
            val propertiesComponent = PropertiesComponent.getInstance(project)
            val sessionName = propertiesComponent.getValue("goose.saved.session")
            if (sessionName != null) {
                val sessionFile = File(System.getProperty("user.home"), ".config/goose/sessions/$sessionName")
                if (sessionFile.exists()) {
                    sessionFile.delete()
                }
                val startupActivity = GoosePluginStartupActivity()
                startupActivity.reInitializeGooseTerminal(project)
            }
        }
    }
}
