package com.pocketqwen.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus
import com.pocketqwen.app.memory.LocalMemoryStore
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.coroutineContext

class ChatViewModel(
    private val aiRepository: AiRepository,
    private val memoryStore: LocalMemoryStore? = null
) : ViewModel() {
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
    private val _contextTokensUsed = MutableStateFlow<Int?>(null)
    val contextTokensUsed: StateFlow<Int?> = _contextTokensUsed.asStateFlow()
    val contextWindowTokens: Int get() = aiRepository.contextWindowTokens
    private var activeKey = NEW_CHAT_KEY

    fun refreshContextUsage() {
        val snapshot = _messages.value
        viewModelScope.launch(Dispatchers.IO) {
            _contextTokensUsed.value = runCatching { aiRepository.countContextTokens(snapshot) }.getOrNull()
        }
    }

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
        conversations.getOrPut(key) { mutableListOf() }.add(
            ChatMessage(UUID.randomUUID().toString(), trimmed, true, status = MessageStatus.SENT)
        )
        _error.value = null
        startGeneration(key)
    }

    fun editMessage(messageId: String, revisedText: String) {
        val text = revisedText.trim()
        if (text.isBlank()) return
        val key = activeKey
        if (jobs[key]?.isActive == true) return
        val list = conversations[key] ?: return
        val index = list.indexOfFirst { it.id == messageId && it.isUser }
        if (index < 0) return
        list[index] = list[index].copy(text = text, status = MessageStatus.SENT)
        while (list.size > index + 1) list.removeAt(list.lastIndex)
        _error.value = null
        startGeneration(key)
    }

    private fun startGeneration(key: String) {
        val conversation = conversations.getOrPut(key) { mutableListOf() }
        val contextMessages = conversation.toList()
        val query = contextMessages.lastOrNull { it.isUser }?.text.orEmpty()
        memoryStore?.rememberConversation(key, contextMessages)
        val memoryContext = memoryStore?.relevantContext(key, query).orEmpty()
        val assistantId = UUID.randomUUID().toString()
        conversation += ChatMessage(assistantId, "", false, status = MessageStatus.SENDING)
        if (key == activeKey) {
            _isLoading.value = true
            _thinking.value = true
            publishActive()
        }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val output = StringBuilder()
            var contextLimit = false
            var nativeError = false
            try {
                val retrievedContext = memoryContext.takeIf { it.isNotBlank() }
                var lastUiPublishNs = 0L
                aiRepository.generateResponse(contextMessages, retrievedContext).collect { chunk ->
                    when (chunk) {
                        "__POCKETQWEN_STATUS__:CONTEXT_LIMIT" -> contextLimit = true
                        "__POCKETQWEN_STATUS__:INCOMPLETE_ERROR" -> nativeError = true
                        else -> if (chunk.isNotEmpty() && !chunk.startsWith("[End of")) {
                            output.append(chunk)
                            val nowNs = System.nanoTime()
                            if (nowNs - lastUiPublishNs >= 60_000_000L || chunk.contains('\n')) {
                                updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                                lastUiPublishNs = nowNs
                            }
                            if (key == activeKey) _thinking.value = false
                        }
                    }
                }
                if (output.isEmpty()) {
                    updateAssistant(key, assistantId, "Generation ended without text. Try again.", MessageStatus.ERROR)
                } else {
                    if (contextLimit || nativeError) output.append(
                        if (contextLimit) "\n\nThe local context window filled before the model ended its response."
                        else "\n\nGeneration stopped unexpectedly; this response may be incomplete."
                    )
                    updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                }
            } catch (e: CancellationException) {
                updateAssistant(key, assistantId, output.toString().ifBlank { "Generation stopped." }, MessageStatus.SENT)
                throw e
            } catch (e: Exception) {
                if (output.isNotEmpty()) updateAssistant(key, assistantId, output.toString(), MessageStatus.SENT)
                else updateAssistant(
                    key, assistantId,
                    "Generation failed. ${e.message ?: "Check model status and retry."}",
                    MessageStatus.ERROR
                )
                if (key == activeKey) _error.value = e.message ?: "Unable to generate a response."
            } finally {
                if (jobs[key] === coroutineContext[Job]) jobs.remove(key)
                if (key == activeKey) {
                    _isLoading.value = false
                    _thinking.value = false
                    publishActive()
                    refreshContextUsage()
                }
            }
        }
        jobs[key] = job
        job.start()
    }

    private fun updateAssistant(key: String, id: String, text: String, status: MessageStatus) {
        val list = conversations[key] ?: return
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) list[index] = list[index].copy(text = text, status = status)
        if (key == activeKey) publishActive()
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
        refreshContextUsage()
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
        _contextTokensUsed.value = 0
        publishActive()
    }

    private fun publishActive() {
        _messages.value = conversations[activeKey]?.toList() ?: emptyList()
        _isLoading.value = jobs[activeKey]?.isActive == true
    }

    companion object { private const val NEW_CHAT_KEY = "__pocketqwen_new_chat__" }
}
