package com.block.gooseintellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBList
import com.intellij.ui.table.JBTable
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.ComboBox
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.io.File
import javax.swing.table.DefaultTableModel

class SelectGooseProfileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val propertiesComponent = PropertiesComponent.getInstance(project)

        // Load existing profile from settings
        val savedProfile = propertiesComponent.getValue("goose.selected.profile")

        val profilesFile = File(System.getProperty("user.home"), ".config/goose/profiles.yaml")
        if (!profilesFile.exists()) {
            Messages.showErrorDialog("Profiles file not found.", "Error")
            return
        }

        val yaml = Yaml()
        val profiles: MutableMap<String, MutableMap<String, Any>> = yaml.load(profilesFile.readText())

        val availableProviders = getAvailableProviders()

        val dialog = ProfileSelectionDialog(profiles, savedProfile, availableProviders)
        if (dialog.showAndGet()) {
            val selectedProfile = dialog.getSelectedProfile()
            if (selectedProfile != null) {
                propertiesComponent.setValue("goose.selected.profile", selectedProfile)
                saveProfiles(profiles, profilesFile)
                Messages.showInfoMessage("Goose will be restarted with selected profile: $selectedProfile", "Profile Selected")
                GoosePluginStartupActivity().reInitializeGooseTerminal(project)
            }
        }
    }

    private fun saveProfiles(profiles: Map<String, Map<String, Any>>, file: File) {
        val yaml = Yaml(DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
        file.writeText(yaml.dump(profiles))
    }

    private fun getAvailableProviders(): List<String> {
        val providers = listOf("openai", "anthropic", "databricks")
        if (isSqGooseInstalled()) {
            return providers + "block"
        }
        return providers
    }

    private fun isSqGooseInstalled(): Boolean {
        val command = "sq version"
        return try {
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private class ProfileSelectionDialog(
        private val profiles: MutableMap<String, MutableMap<String, Any>>,
        savedProfile: String?,
        private val availableProviders: List<String>,
    ) : DialogWrapper(true) {

        private val profileList = JBList(profiles.keys.toList())
        private val providerComboBox = ComboBox(availableProviders.toTypedArray())
        private val toolkitsTableModel = DefaultTableModel(arrayOf(arrayOf<Any>("", "")), arrayOf("Key", "Value"))
        private val toolkitsTable = JBTable(toolkitsTableModel)
        private val newProfileButton = JButton("Add New Profile")
        private val addToolkitButton = JButton("+")
        private val removeToolkitButton = JButton("-")
        private val saveButton = JButton("Save and Select")
        private val processorComboBox = ComboBox(arrayOf("gpt-4o", "claude-3-5-sonnet-20240620", "claude-3-opus-20240229", "claude-3-sonnet-20240229"))
        private val acceleratorComboBox = ComboBox(arrayOf("gpt-4o-mini", "claude-3-haiku-20240307"))
        private val moderatorComboBox = ComboBox(arrayOf("passive", "truncate"))

        init {
            title = "Select Goose Profile"
            setOKButtonText("Select")
            init()

            addToolkitButton.addActionListener { toolkitsTableModel.addRow(arrayOf("", "")) }
            removeToolkitButton.addActionListener { 
                val selectedRow = toolkitsTable.selectedRow
                if (selectedRow != -1) {
                    toolkitsTableModel.removeRow(selectedRow)
                }
            }

            // Detect changes to enable the save button
            saveButton.isEnabled = false
            toolkitsTableModel.addTableModelListener {
                saveButton.isEnabled = true
            }
            profileList.addListSelectionListener {
                saveButton.isEnabled = true
            }

            saveButton.addActionListener {
                doOKAction()
            }

            profileList.addListSelectionListener { updateDetails(profileList.selectedValue) }
            newProfileButton.addActionListener { addNewProfile(); getButton(okAction)?.isEnabled = true }
            savedProfile?.let { profileList.setSelectedValue(it, true) } ?: run { profileList.selectedIndex = 0 }
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())

            // List panel on the left
            val listPanel = JPanel(BorderLayout())
            listPanel.add(JScrollPane(profileList), BorderLayout.CENTER)
            listPanel.add(newProfileButton, BorderLayout.SOUTH)

            // Detail panel on the right using GridBagLayout
            val detailPanel = JPanel(GridBagLayout())
            val constraints = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.NORTHWEST
                weightx = 1.0
                gridx = 0
                gridy = GridBagConstraints.RELATIVE
            }
            detailPanel.add(JLabel("Provider:"), constraints)
            providerComboBox.isEditable = false
            detailPanel.add(providerComboBox, constraints)
            detailPanel.add(JLabel("Processor:"), constraints)
            detailPanel.add(processorComboBox, constraints)
            detailPanel.add(JLabel("Accelerator:"), constraints)
            detailPanel.add(acceleratorComboBox, constraints)
            detailPanel.add(JLabel("Moderator:"), constraints)
            detailPanel.add(moderatorComboBox, constraints)
            detailPanel.add(JLabel("Toolkits:"), constraints)
            toolkitsTable.fillsViewportHeight = true
            toolkitsTable.preferredScrollableViewportSize = Dimension(250, 100)
            detailPanel.add(JScrollPane(toolkitsTable), constraints)
            val buttonPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.LINE_AXIS); add(addToolkitButton); add(removeToolkitButton); add(saveButton) }
            detailPanel.add(buttonPanel, constraints)
            panel.add(listPanel, BorderLayout.WEST)
            panel.add(detailPanel, BorderLayout.CENTER)
            return panel
        }

        private fun updateDetails(profileName: String?) {
            toolkitsTableModel.dataVector.removeAllElements() // Clear existing rows
            if (profileName != null) {
                val profileDetails = profiles[profileName] ?: return
                providerComboBox.selectedItem = profileDetails["provider"] as String? ?: ""
                processorComboBox.selectedItem = profileDetails["processor"] as String? ?: "gpt-4o"
                acceleratorComboBox.selectedItem = profileDetails["accelerator"] as String? ?: "gpt-4o-mini"
                moderatorComboBox.selectedItem = profileDetails["moderator"] as String? ?: "passive"
                val toolkits = (profileDetails["toolkits"] as? List<Map<String, Any>>)?.associate { it ["name"].toString() to it ["requires"].toString() }
                toolkits?.forEach { (key, value) -> toolkitsTableModel.addRow(arrayOf(key, value)) }
            }
            toolkitsTableModel.addRow(arrayOf("", "")) // Add an empty row for adding new toolkits
        }

        private fun addNewProfile() {
            val newProfileName = JOptionPane.showInputDialog("Enter profile name:")
            if (newProfileName.isNullOrBlank()) {
                Messages.showErrorDialog("Profile name cannot be empty.", "Error")
                return
            }
            if (profiles.containsKey(newProfileName)) {
                Messages.showErrorDialog("Profile name already exists.", "Error")
                return
            }
            profiles[newProfileName] = mutableMapOf(
                "provider" to availableProviders.first(),
                "toolkits" to emptyMap<String, String>(),
                "processor" to processorComboBox.selectedItem.toString(),
                "accelerator" to acceleratorComboBox.selectedItem.toString(),
                "moderator" to moderatorComboBox.selectedItem.toString()
            )
            profileList.setListData(profiles.keys.toTypedArray())
            profileList.setSelectedValue(newProfileName, true)
        }

        override fun doOKAction() {
            val selectedProfile = profileList.selectedValue ?: return
            profiles[selectedProfile]?.set("provider", providerComboBox.selectedItem.toString())
            profiles[selectedProfile]?.set("processor", processorComboBox.selectedItem.toString())
            profiles[selectedProfile]?.set("accelerator", acceleratorComboBox.selectedItem.toString())
            profiles[selectedProfile]?.set("moderator", moderatorComboBox.selectedItem.toString())
            val toolkitsMap = mutableListOf<Map<String, Any>>()
            for (row in 0 until toolkitsTableModel.rowCount) {
                val key = toolkitsTableModel.getValueAt(row, 0) as String
                val value = toolkitsTableModel.getValueAt(row, 1) as String
                if (key.isNotBlank() && value.isNotBlank()) {
                    toolkitsMap.add(mapOf("name" to key, "requires" to emptyMap<String, Any>()))
                }
            }
            profiles[selectedProfile]?.set("toolkits", toolkitsMap)
            super.doOKAction()
        }

        override fun doValidate(): ValidationInfo? {
            // Ensure no duplicate keys in the table
            val keys = mutableSetOf<String>()
            for (row in 0 until toolkitsTableModel.rowCount) {
                val key = toolkitsTableModel.getValueAt(row, 0) as String
                if (key.isNotBlank()) {
                    if (!keys.add(key)) {
                        return ValidationInfo("Duplicate toolkit key: $key", toolkitsTable)
                    }
                }
            }
            return null
        }

        fun getSelectedProfile(): String? = profileList.selectedValue
    }
}
