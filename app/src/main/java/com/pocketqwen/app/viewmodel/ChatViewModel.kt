package com.pocketqwen.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Streams text into one assistant bubble and keeps jobs associated with their originating chat. */
class ChatViewModel(private val aiRepository: AiRepository) : ViewModel() {
    private val conversations = mutableMapOf<String, MutableList<ChatMessage>>()
    private val jobs = mutableMapOf<String, Job>()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private var activeKey: String = NEW_CHAT_KEY

    fun activateConversation(id: String?, savedMessages: List<ChatMessage> = emptyList()) {
        activeKey = id ?: NEW_CHAT_KEY
        if (!conversations.containsKey(activeKey)) conversations[activeKey] = savedMessages.toMutableList()
        else if (conversations[activeKey].isNullOrEmpty() && savedMessages.isNotEmpty()) {
            conversations[activeKey] = savedMessages.toMutableList()
        }
        _error.value = null
        publishActive()
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val key = activeKey
        if (jobs[key]?.isActive == true) return
        val list = conversations.getOrPut(key) { mutableListOf() }
        list += ChatMessage(UUID.randomUUID().toString(), trimmed, true, status = MessageStatus.SENT)
        val assistantId = UUID.randomUUID().toString()
        list += ChatMessage(assistantId, "", false, status = MessageStatus.SENDING)
        _error.value = null
        if (key == activeKey) { _isLoading.value = true; _thinking.value = true; publishActive() }

        // LAZY start ensures the map owns this exact Job before its finally block can run.
        val generationJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val output = StringBuilder()
            try {
                aiRepository.generateResponse(trimmed).collect { chunk ->
                    if (chunk.isNotEmpty() && !chunk.startsWith("[End of")) {
                        output.append(chunk)
                        updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                        if (key == activeKey) { _thinking.value = false; publishActive() }
                    }
                }
                if (output.isEmpty()) updateAssistant(key, assistantId, "Generation ended without text. Try again.", MessageStatus.ERROR)
            } catch (e: CancellationException) {
                if (output.isNotEmpty()) updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                else updateAssistant(key, assistantId, "Generation stopped.", MessageStatus.SENT)
                throw e
            } catch (e: Exception) {
                if (output.isNotEmpty()) updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                else updateAssistant(key, assistantId, "Generation failed. " + (e.message ?: "Check model status and retry."), MessageStatus.ERROR)
                if (key == activeKey) _error.value = e.message ?: "Unable to generate a response."
            } finally {
                if (jobs[key] === coroutineContext[Job]) jobs.remove(key)
                if (key == activeKey) { _isLoading.value = false; _thinking.value = false; publishActive() }
            }
        }
        jobs[key] = generationJob
        generationJob.start()
    }

    private fun updateAssistant(key: String, id: String, text: String, status: MessageStatus) {
        val list = conversations[key] ?: return
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) list[index] = list[index].copy(text = text, status = status)
    }

    fun stopGeneration() {
        val key = activeKey
        aiRepository.cancelGeneration()
        jobs[key]?.cancel()
        jobs.remove(key)
        val list = conversations[key]
        val pending = list?.indexOfLast { !it.isUser && it.status == MessageStatus.SENDING } ?: -1
        if (list != null && pending >= 0) list[pending] = list[pending].copy(
            text = list[pending].text.ifBlank { "Generation stopped." }, status = MessageStatus.SENT
        )
        _isLoading.value = false
        _thinking.value = false
        publishActive()
    }

    fun loadMessages(savedMessages: List<ChatMessage>) {
        activateConversation(activeKey.takeUnless { it == NEW_CHAT_KEY }, savedMessages)
    }

    fun clearMessages() {
        conversations[activeKey] = mutableListOf()
        activeKey = NEW_CHAT_KEY
        conversations.getOrPut(activeKey) { mutableListOf() }
        _error.value = null
        _thinking.value = false
        publishActive()
    }

    private fun publishActive() {
        _messages.value = conversations[activeKey]?.toList() ?: emptyList()
        _isLoading.value = jobs[activeKey]?.isActive == true
    }

    companion object { private const val NEW_CHAT_KEY = "__pocketqwen_new_chat__" }
}
