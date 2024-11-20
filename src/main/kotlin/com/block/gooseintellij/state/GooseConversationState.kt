package com.block.gooseintellij.state

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.block.gooseintellij.model.ChatMessage
import java.time.Instant

@State(
    name = "GooseConversationState",
    storages = [Storage("gooseConversationState.xml")]
)
class GooseConversationState : PersistentStateComponent<GooseConversationState.State> {
    data class ConversationMessage(
        val role: String,
        val content: String,
        val timestamp: Long = Instant.now().epochSecond
    ) {
        fun toChatMessage(): ChatMessage = ChatMessage(role, content)
    }

    data class Conversation(
        val id: String,
        val messages: MutableList<ConversationMessage> = mutableListOf(),
        val createdAt: Long = Instant.now().epochSecond,
        var updatedAt: Long = Instant.now().epochSecond
    )

    data class State(
        var currentConversationId: String? = null,
        var conversations: MutableMap<String, Conversation> = mutableMapOf()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getCurrentConversation(): Conversation? =
        myState.currentConversationId?.let { myState.conversations[it] }

    fun createNewConversation(): Conversation {
        val conversation = Conversation(id = generateConversationId())
        myState.conversations[conversation.id] = conversation
        myState.currentConversationId = conversation.id
        return conversation
    }

    fun addMessage(role: String, content: String) {
        val conversation = getCurrentConversation() ?: createNewConversation()
        conversation.messages.add(ConversationMessage(role, content))
        conversation.updatedAt = Instant.now().epochSecond
    }

    fun clearCurrentConversation() {
        myState.currentConversationId = null
    }

    fun getConversationHistory(): List<ConversationMessage> =
        getCurrentConversation()?.messages ?: emptyList()

    private fun generateConversationId(): String =
        java.util.UUID.randomUUID().toString()

    companion object {
        fun getInstance(project: Project): GooseConversationState =
            project.getService(GooseConversationState::class.java)
    }
}