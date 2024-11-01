package com.block.gooseintellij.ui.dialog

import com.block.gooseintellij.ui.components.editor.MultiSelectComboBox
import com.block.gooseintellij.ui.components.editor.MultiselectCellEditor
import com.block.gooseintellij.utils.GooseUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.table.JBTable
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ProfileSelectionDialog(
    private val profiles: MutableMap<String, MutableMap<String, Any>>,
    savedProfile: String?,
    private val availableProviders: List<String>,
    private val project: Project
) : DialogWrapper(true) {

    private val profileList = JBList(profiles.keys.toList())
    private val providerComboBox = ComboBox(availableProviders.toTypedArray())

    private val toolkitsTableModel: DefaultTableModel =
        object : DefaultTableModel(
            arrayOf(arrayOf("", mutableListOf<String>())),
            arrayOf("Toolkit", "Requires")
        ) {

            override fun getColumnClass(columnIndex: Int): Class<*> {
                return if (columnIndex == 0) JComboBox::class.java else MultiSelectComboBox::class.java
            }
        }
    private var toolkitComboBox: JComboBox<String> =
        ComboBox(GooseUtils.getToolkitsWithDescriptions().keys.toTypedArray())

    private val toolkitsTable = JBTable(toolkitsTableModel)

    private val saveButton = JButton("Save and Select")
    private val processorComboBox = ComboBox(
        arrayOf(
            "gpt-4o",
            "claude-3-5-sonnet-20240620",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-5-sonnet-2"
        )
    ).apply {
        isEditable = true
    }
    private val acceleratorComboBox = ComboBox(arrayOf("gpt-4o-mini")).apply {
        isEditable = true
    }
    private val moderatorComboBox =
        ComboBox(arrayOf("passive", "truncate", "summarize", "synopsis")).apply {
            isEditable = true
        }

    init {
        title = "Select Goose Profile"
        setOKButtonText("Select")
        init()

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
        savedProfile?.let { profileList.setSelectedValue(it, true) }
            ?: run { profileList.selectedIndex = 0 }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val listPanel = JPanel(BorderLayout()).apply { border = BorderFactory.createEmptyBorder(0, 0, 0, 10) }
        listPanel.add(ToolbarDecorator.createDecorator(profileList).setAddAction {
            addNewProfile()
            getButton(okAction)?.isEnabled = true
        }.setRemoveAction {
            val selectedProfile = profileList.selectedValue
            if (selectedProfile != null) {
                val confirmation =
                    Messages.showYesNoDialog(
                        project,
                        "Are you sure you want to delete the profile '$selectedProfile'?",
                        "Delete Profile",
                        Messages.getQuestionIcon()
                    )
                if (confirmation == Messages.YES) {
                    profiles.remove(selectedProfile)
                    profileList.setListData(profiles.keys.toTypedArray())
                }
            }
        }.addExtraAction(object :
            AnAction("Copy Profile", "Copy selected profile", AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val selectedProfile = profileList.selectedValue
                if (selectedProfile != null) {
                    val profileCopy =
                        profiles[selectedProfile]?.let { it as Map<String, Any> }?.toMutableMap()
                    val newProfileName = JOptionPane.showInputDialog(
                        null,
                        "Enter name for the copied profile:",
                        "Copy Profile",
                        JOptionPane.PLAIN_MESSAGE
                    ) ?: "${selectedProfile}_copy"
                    if (profileCopy != null) {
                        profiles[newProfileName] = profileCopy
                        profileList.setListData(profiles.keys.toTypedArray())
                    }
                }
            }
        }).createPanel())

        // Detail panel on the right using GridBagLayout
        val detailPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
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
        val toolkitEditor = DefaultCellEditor(JComboBox(toolkitComboBox.model))
        (toolkitEditor.component as JComboBox<*>).isEditable = true
        val requiresEditor =
            MultiselectCellEditor(GooseUtils.getToolkitsWithDescriptions().keys.toTypedArray())

        toolkitsTable.columnModel.getColumn(0).cellEditor = toolkitEditor
        toolkitsTable.columnModel.getColumn(1).cellEditor = requiresEditor
        toolkitsTable.rowHeight = 30
        toolkitsTable.fillsViewportHeight = true
        toolkitsTable.preferredScrollableViewportSize = Dimension(500, 300)

        toolkitsTable.setRowSelectionAllowed(true)
        toolkitsTable.setColumnSelectionAllowed(false)
        toolkitsTable.setSelectionBackground(JBColor.GRAY)
        toolkitsTable.setSelectionForeground(JBColor.BLACK)
        detailPanel.add(JLabel("Toolkits:"), constraints)
        detailPanel.add(ToolbarDecorator.createDecorator(toolkitsTable).setAddAction {
            toolkitsTableModel.addRow(arrayOf("", emptyList<String>()))
        }.setRemoveAction {
            val selectedRow = toolkitsTable.selectedRow
            if (selectedRow != -1) {
                toolkitsTableModel.removeRow(selectedRow)
            }
        }.createPanel(), constraints)
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS); add(saveButton)
        }
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
            val toolkits =
                (profileDetails["toolkits"] as? List<Map<String, Any>>)?.associate { it["name"].toString() to (it["requires"] as? Map<String, Any>)?.keys!!.toList() }
            toolkits?.forEach { (key, value) -> toolkitsTableModel.addRow(arrayOf(key, value)) }
        }
        toolkitsTableModel.addRow(
            arrayOf(
                "",
                emptyList<String>()
            )
        ) // Add an empty row for adding new toolkits
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
            "processor" to processorComboBox.selectedItem!!.toString(),
            "accelerator" to acceleratorComboBox.selectedItem!!.toString(),
            "moderator" to moderatorComboBox.selectedItem!!.toString()
        )
        profileList.setListData(profiles.keys.toTypedArray())
        profileList.setSelectedValue(newProfileName, true)
    }

    override fun doOKAction() {
        val selectedProfile = profileList.selectedValue ?: return
        providerComboBox.selectedItem?.let {
            profiles[selectedProfile]?.set(
                "provider",
                it.toString()
            )
        }
        processorComboBox.selectedItem?.let {
            profiles[selectedProfile]?.set(
                "processor",
                it.toString()
            )
        }
        acceleratorComboBox.selectedItem?.let {
            profiles[selectedProfile]?.set(
                "accelerator",
                it.toString()
            )
        }
        moderatorComboBox.selectedItem?.let {
            profiles[selectedProfile]?.set(
                "moderator",
                it.toString()
            )
        }
        val toolkitsMap = mutableListOf<Map<String, Any>>()
        for (row in 0 until toolkitsTableModel.rowCount) {
            val toolkit = toolkitsTableModel.getValueAt(row, 0) as String
            val requiresList =
                (toolkitsTableModel.getValueAt(row, 1) as? Collection<String>)?.toList() ?: emptyList()
            if (toolkit.isNotBlank()) {
                val requiresMap = requiresList.associateWith { it }
                toolkitsMap.add(mapOf("name" to toolkit, "requires" to requiresMap))
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
