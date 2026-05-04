package com.openclaw.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.app.OpenClawApp
import com.openclaw.app.data.api.OpenClawApi
import com.openclaw.app.data.model.*
import com.openclaw.app.data.repository.ConversationRepository
import com.openclaw.app.data.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OpenClawApp
    private val api = OpenClawApi()
    private val conversationRepo = ConversationRepository()
    private val imageRepo = ImageRepository(application)

    val conversations = conversationRepo.conversations
    val currentConversation = conversationRepo.currentConversation

    val messages: StateFlow<List<ChatMessage>> = currentConversation
        .map { it?.messages ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Voice input state
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Pending image attachment
    private val _pendingImageUri = MutableStateFlow<String?>(null)
    val pendingImageUri: StateFlow<String?> = _pendingImageUri.asStateFlow()

    private var systemPrompt = "你是 OpenClaw，一个智能 AI 助手。请用中文回复。"

    init {
        viewModelScope.launch {
            combine(
                app.settingsRepository.apiEndpoint,
                app.settingsRepository.apiKey,
                app.settingsRepository.model
            ) { endpoint, key, model ->
                Triple(endpoint, key, model)
            }.collect { (endpoint, key, model) ->
                api.updateConfig(endpoint, key, model)
            }
        }

        viewModelScope.launch {
            app.settingsRepository.systemPrompt.collect {
                systemPrompt = it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        api.cancelActiveRequest()
    }

    // ---- Image attachment ----

    fun attachImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = imageRepo.encodeImageToBase64(uri)
            result.onSuccess { base64 ->
                _pendingImageUri.value = base64
            }.onFailure { e ->
                _error.value = e.message ?: "图片处理失败"
            }
        }
    }

    fun removePendingImage() {
        _pendingImageUri.value = null
    }

    // ---- Voice input ----

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    // ---- Send message ----

    fun sendMessage(content: String, imageBase64: String? = null) {
        val img = imageBase64 ?: _pendingImageUri.value
        if (content.isBlank() && img == null) return
        if (_isGenerating.value) return
        if (!api.isConfigured()) {
            _error.value = "请先在设置中配置 API 端点和 Key"
            return
        }

        _error.value = null
        _pendingImageUri.value = null

        // Add user message (with optional image reference)
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content.ifBlank { "[图片]" },
            imageUri = img
        )
        conversationRepo.addMessage(userMessage)

        // Add placeholder assistant message
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )
        conversationRepo.addMessage(assistantMessage)

        _isGenerating.value = true

        // Build API messages (with multimodal support)
        val apiMessages = buildApiMessages()

        // Stream response
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                api.sendMessageStream(
                    messages = apiMessages,
                    onChunk = { chunk ->
                        viewModelScope.launch {
                            val current = currentConversation.value
                            val lastMsg = current?.messages?.lastOrNull()
                            if (lastMsg?.role == MessageRole.ASSISTANT) {
                                conversationRepo.updateLastMessage(lastMsg.content + chunk)
                            }
                        }
                    },
                    onComplete = {
                        viewModelScope.launch {
                            conversationRepo.markLastMessageStreaming(false)
                            _isGenerating.value = false
                        }
                    },
                    onError = { errorMsg ->
                        viewModelScope.launch {
                            conversationRepo.markLastMessageError(errorMsg)
                            _isGenerating.value = false
                            _error.value = errorMsg
                        }
                    }
                )
            }
        }
    }

    // ---- Stop generation ----

    fun stopGeneration() {
        api.cancelActiveRequest()
        conversationRepo.markLastMessageStreaming(false)
        _isGenerating.value = false
    }

    // ---- Retry ----

    fun retryLastMessage() {
        val current = currentConversation.value ?: return
        val msgs = current.messages
        if (msgs.isEmpty()) return

        val lastAssistant = msgs.lastOrNull()
        val hasError = lastAssistant?.role == MessageRole.ASSISTANT && lastAssistant.error != null
        val isStreaming = lastAssistant?.role == MessageRole.ASSISTANT && lastAssistant.isStreaming

        // Find the user message before the assistant
        val lastUserIdx = msgs.indexOfLast { it.role == MessageRole.USER }
        if (lastUserIdx < 0) return

        val lastUserMsg = msgs[lastUserIdx]

        // Remove failed/streaming assistant message
        if (hasError || isStreaming) {
            conversationRepo.removeLastMessages(msgs.size - lastUserIdx - 1)
        }

        sendMessage(lastUserMsg.content, lastUserMsg.imageUri)
    }

    // ---- Conversation management ----

    fun newConversation() {
        conversationRepo.createNewConversation()
    }

    fun switchConversation(id: String) {
        conversationRepo.switchConversation(id)
    }

    fun deleteConversation(id: String) {
        conversationRepo.deleteConversation(id)
    }

    fun clearError() {
        _error.value = null
    }

    // ---- Build API messages with multimodal support ----

    private fun buildApiMessages(): List<ApiMessage> {
        val msgs = mutableListOf<ApiMessage>()

        // System prompt
        if (systemPrompt.isNotBlank()) {
            msgs.add(ApiMessage(role = "system", content = systemPrompt))
        }

        // Conversation messages
        val convMessages = currentConversation.value?.messages ?: emptyList()
        convMessages
            .filter {
                it.error == null &&
                    (it.role != MessageRole.ASSISTANT || it.content.isNotBlank()) &&
                    !it.isStreaming
            }
            .forEach { msg ->
                if (msg.imageUri != null && msg.role == MessageRole.USER) {
                    // Multimodal message with image
                    val parts = mutableListOf<ContentPart>()
                    if (msg.content.isNotBlank() && msg.content != "[图片]") {
                        parts.add(ContentPart(type = "text", text = msg.content))
                    }
                    parts.add(ContentPart(
                        type = "image_url",
                        image_url = ImageUrl(url = msg.imageUri)
                    ))
                    msgs.add(ApiMessage(role = msg.role.value, content = parts))
                } else {
                    msgs.add(ApiMessage(role = msg.role.value, content = msg.content))
                }
            }

        return msgs
    }
}
