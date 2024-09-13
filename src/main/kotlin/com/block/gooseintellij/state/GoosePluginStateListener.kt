package com.block.gooseintellij.state

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.ui.Messages

class GoosePluginStateListener : PluginStateListener {

    override fun install(pluginDescriptor: IdeaPluginDescriptor) {
        showRestartNotification()
    }
    
    private fun showRestartNotification() {
        val application = ApplicationManager.getApplication() as ApplicationEx
        Messages.showInfoMessage("To complete the installation of the Goose plugin, IDE restart is required.", "Restart Required")
        application.restart(true)
    }
}
