package com.pocketqwen.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val aiRepository: AiRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        _messages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Hello! I'm PocketQwen, your private on-device AI assistant. " +
                    "Note: AI inference is not yet connected — this is a UI preview. " +
                    "Ask me anything to see the chat interface in action.",
                isUser = false,
                status = ChatMessage.MessageStatus.SENT
            )
        )
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true,
            status = ChatMessage.MessageStatus.SENT
        )

        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                aiRepository.generateResponse(trimmed).collect { response ->
                    if (response == "[Thinking...]") {
                        _isLoading.value = true
                    } else if (response == "[End of placeholder response]") {
                        _isLoading.value = false
                    } else {
                        val assistantMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = response,
                            isUser = false,
                            status = ChatMessage.MessageStatus.SENT
                        )
                        _messages.value = _messages.value + assistantMessage
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }
}
