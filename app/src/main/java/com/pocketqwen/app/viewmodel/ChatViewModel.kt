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

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        _messages.value = _messages.value + ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true,
            status = MessageStatus.SENT
        )
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                aiRepository.generateResponse(trimmed).collect { response ->
                    when {
                        response == "[Thinking...]" -> _isLoading.value = true
                        response.startsWith("[End of placeholder") ||
                            response == "[End of response]" -> _isLoading.value = false
                        else -> {
                            _messages.value = _messages.value + ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = response,
                                isUser = false,
                                status = MessageStatus.SENT
                            )
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to generate a response."
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