package com.openclaw.app.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val imageUri: String? = null, // Local URI of attached image
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val error: String? = null
)

enum class MessageRole(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant")
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// API Request/Response models
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 4096,
    val stream: Boolean = true
)

// Multimodal content part
data class ContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String, // base64 data URI or HTTP URL
    val detail: String = "auto"
)

// API message with multimodal support
data class ApiMessage(
    val role: String,
    val content: Any // String for text-only, List<ContentPart> for multimodal
)

data class ChatResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: ApiError?
)

data class Choice(
    val index: Int,
    val message: ApiMessage?,
    val delta: Delta?,
    val finish_reason: String?
)

data class Delta(
    val role: String?,
    val content: String?
)

data class ApiError(
    val message: String,
    val type: String?,
    val code: String?
)
