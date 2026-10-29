package com.pocketqwen.app.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    ERROR
}

data class Conversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: Long
)
