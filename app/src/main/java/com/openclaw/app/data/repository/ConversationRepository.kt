package com.openclaw.app.data.repository

import com.openclaw.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    init {
        createNewConversation()
    }

    fun createNewConversation(): Conversation {
        val conversation = Conversation()
        _conversations.value = _conversations.value + conversation
        _currentConversation.value = conversation
        return conversation
    }

    fun switchConversation(id: String) {
        _currentConversation.value = _conversations.value.find { it.id == id }
    }

    fun addMessage(message: ChatMessage) {
        val current = _currentConversation.value ?: return
        val updated = current.copy(
            messages = current.messages + message,
            updatedAt = System.currentTimeMillis(),
            title = if (current.messages.isEmpty() && message.role == MessageRole.USER) {
                message.content.take(30).ifBlank { "图片消息" } +
                    if (message.content.length > 30) "..." else ""
            } else current.title
        )
        updateConversation(updated)
    }

    fun updateLastMessage(content: String) {
        val current = _currentConversation.value ?: return
        val messages = current.messages.toMutableList()
        if (messages.isNotEmpty()) {
            val last = messages.last()
            messages[messages.lastIndex] = last.copy(content = content)
            val updated = current.copy(messages = messages, updatedAt = System.currentTimeMillis())
            updateConversation(updated)
        }
    }

    fun markLastMessageStreaming(isStreaming: Boolean) {
        val current = _currentConversation.value ?: return
        val messages = current.messages.toMutableList()
        if (messages.isNotEmpty()) {
            val last = messages.last()
            messages[messages.lastIndex] = last.copy(isStreaming = isStreaming)
            val updated = current.copy(messages = messages)
            updateConversation(updated)
        }
    }

    fun markLastMessageError(error: String) {
        val current = _currentConversation.value ?: return
        val messages = current.messages.toMutableList()
        if (messages.isNotEmpty()) {
            val last = messages.last()
            messages[messages.lastIndex] = last.copy(error = error, isStreaming = false)
            val updated = current.copy(messages = messages)
            updateConversation(updated)
        }
    }

    fun clearCurrentConversation() {
        val current = _currentConversation.value ?: return
        val cleared = current.copy(messages = emptyList(), updatedAt = System.currentTimeMillis())
        updateConversation(cleared)
    }

    fun deleteConversation(id: String) {
        _conversations.value = _conversations.value.filter { it.id != id }
        if (_currentConversation.value?.id == id) {
            _currentConversation.value = _conversations.value.firstOrNull()
            // Always have at least one conversation
            if (_currentConversation.value == null) {
                createNewConversation()
            }
        }
    }

    /**
     * Remove the last N messages from the current conversation.
     */
    fun removeLastMessages(count: Int) {
        val current = _currentConversation.value ?: return
        if (current.messages.size < count) return
        val updated = current.copy(
            messages = current.messages.dropLast(count),
            updatedAt = System.currentTimeMillis()
        )
        updateConversation(updated)
    }

    private fun updateConversation(conversation: Conversation) {
        _currentConversation.value = conversation
        _conversations.value = _conversations.value.map {
            if (it.id == conversation.id) conversation else it
        }
    }
}
