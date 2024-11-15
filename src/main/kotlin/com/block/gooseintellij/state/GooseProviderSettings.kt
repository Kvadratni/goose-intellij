package com.block.gooseintellij.state

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "GooseProviderSettings",
    storages = [Storage("gooseProviderSettings.xml")]
)
class GooseProviderSettings : PersistentStateComponent<GooseProviderSettings.State> {
    data class State(
        var providerType: String? = null
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var providerType: String?
        get() = myState.providerType
        set(value) {
            myState.providerType = value
        }

    companion object {
        fun getInstance(project: Project): GooseProviderSettings =
            project.getService(GooseProviderSettings::class.java)
    }
}