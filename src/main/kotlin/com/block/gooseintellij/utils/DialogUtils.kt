package com.block.gooseintellij.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

object DialogUtils {
    /**
     * Shows an error dialog with the given message and title.
     * Logs the error and returns false to indicate failure.
     */
    fun showError(project: Project?, message: String, title: String = "Error"): Boolean {
        Messages.showErrorDialog(project, message, title)
        return false
    }

    /**
     * Shows an error dialog for an exception.
     * Formats the error message appropriately and returns false to indicate failure.
     */
    fun showError(project: Project?, throwable: Throwable, title: String = "Error"): Boolean {
        val message = throwable.message ?: "An unknown error occurred"
        return showError(project, message, title)
    }

    /**
     * Shows a warning dialog with the given message and title.
     */
    fun showWarning(project: Project?, message: String, title: String = "Warning") {
        Messages.showWarningDialog(project, message, title)
    }

    /**
     * Shows an info dialog with the given message and title.
     */
    fun showInfo(project: Project?, message: String, title: String = "Information") {
        Messages.showInfoMessage(project, message, title)
    }
}