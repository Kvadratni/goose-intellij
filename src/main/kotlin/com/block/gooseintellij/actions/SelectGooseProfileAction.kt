package com.block.gooseintellij.actions

import com.block.gooseintellij.ui.dialog.ProfileSelectionDialog
import com.block.gooseintellij.utils.GooseUtils
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

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

    val availableProviders = GooseUtils.getAvailableProviders()

    val dialog =
      ProfileSelectionDialog(profiles, savedProfile, availableProviders, project).apply {
        setSize(800, 800)
      }
    if (dialog.showAndGet()) {
      val selectedProfile = dialog.getSelectedProfile()
      if (selectedProfile != null) {
        propertiesComponent.setValue("goose.selected.profile", selectedProfile)
        saveProfiles(profiles, profilesFile)
        Messages.showInfoMessage(
          "Goose will be restarted with selected profile: $selectedProfile", "Profile Selected"
        )
        GoosePluginStartupActivity().reInitializeGooseTerminal(project)
      }
    }
  }

  private fun saveProfiles(profiles: Map<String, Map<String, Any>>, file: File) {
    val yaml = Yaml(DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    file.writeText(yaml.dump(profiles))
  }
}