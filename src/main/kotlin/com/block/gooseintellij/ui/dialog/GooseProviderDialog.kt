package com.block.gooseintellij.ui.dialog

import com.block.gooseintellij.service.KeychainService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class GooseProviderDialog(private val project: Project) : DialogWrapper(project) {
    private val providerTypeComboBox = ComboBox<String>()
    private val apiKeyField = JBPasswordField()

    init {
        title = "Configure Goose Provider"
        providerTypeComboBox.addItem("openai")
        providerTypeComboBox.addItem("anthropic")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Provider Type:"),
                providerTypeComboBox,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel("API Key:"),
                apiKeyField,
                1,
                false
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun doValidate(): ValidationInfo? {
        val apiKey = String(apiKeyField.password)
        if (apiKey.isBlank()) {
            return ValidationInfo("API Key cannot be empty", apiKeyField)
        }
        return null
    }

    override fun doOKAction() {
        val apiKey = String(apiKeyField.password)
        val selectedProvider = getSelectedProvider()
        
        try {
            KeychainService.getInstance(project).setApiKey(selectedProvider, apiKey)
            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to save API key: ${e.message}",
                "Error Saving API Key"
            )
        }
    }

    fun getSelectedProvider(): String = providerTypeComboBox.selectedItem as String

    fun setSelectedProvider(providerType: String) {
        providerTypeComboBox.selectedItem = providerType
    }
}