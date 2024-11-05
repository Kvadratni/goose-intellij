package com.block.gooseintellij.service.impl

import com.block.gooseintellij.model.Configuration
import com.block.gooseintellij.service.ConfigurationService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "GoosePluginConfiguration",
    storages = [Storage("goose-plugin-config.xml")]
)
class ConfigurationServiceImpl : ConfigurationService {
    private val configMap = mutableMapOf<String, String>()

    override fun getConfig(key: String): String? {
        return configMap[key]
    }

    override fun setConfig(key: String, value: String) {
        configMap[key] = value
    }

    override fun clearConfig(key: String) {
        configMap.remove(key)
    }

    override fun hasConfig(key: String): Boolean {
        return configMap.containsKey(key)
    }
}