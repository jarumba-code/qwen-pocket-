package com.pocketqwen.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                text = "Welcome to PocketQwen. Choose a local GGUF model in Settings if one is not loaded yet. " +
                    "Once loaded, responses are generated on this device.",
                isUser = false,
                status = MessageStatus.SENT
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
            status = MessageStatus.SENT
        )

        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                aiRepository.generateResponse(trimmed).collect { response ->
                    when {
                        response == "[Thinking...]" -> {
                            _isLoading.value = true
                        }
                        response.startsWith("[End of") -> {
                            _isLoading.value = false
                        }
                        else -> {
                            val assistantMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = response,
                                isUser = false,
                                status = MessageStatus.SENT
                            )
                            _messages.value = _messages.value + assistantMessage
                            _isLoading.value = false
                        }
                    }
                }
            } catch (error: Exception) {
                _error.value = error.message ?: "An unexpected inference error occurred."
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
