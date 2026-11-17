package com.pocketqwen.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudyMode(val heading: String) {
    SUMMARIZE("Summary"),
    EXPLAIN("Explanation")
}

sealed class StudyUiState {
    object Idle : StudyUiState()
    data class Loading(val mode: StudyMode) : StudyUiState()
    data class Generating(val mode: StudyMode, val partialText: String) : StudyUiState()
    data class Done(val mode: StudyMode, val text: String) : StudyUiState()
    data class Error(val mode: StudyMode, val message: String) : StudyUiState()
}

class StudyViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Idle)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private var lastMode = StudyMode.SUMMARIZE

    val contextWindowTokens: Int
        get() = aiRepository.contextWindowTokens

    fun updateNotes(value: String) {
        if (isBusy()) return
        _notes.value = value
        if (_uiState.value !is StudyUiState.Idle) {
            _uiState.value = StudyUiState.Idle
        }
    }

    fun loadSampleNotes() {
        updateNotes(
            "Photosynthesis is the process plants use to make their own food. " +
                "Plants take in carbon dioxide from the air and water from the soil. " +
                "Chlorophyll in the leaves captures energy from sunlight. " +
                "The plant uses this energy to turn carbon dioxide and water into glucose, a type of sugar. " +
                "Oxygen is released into the air as a by-product. Photosynthesis happens mainly in the leaves."
        )
    }

    fun clearNotes() {
        stopGeneration()
        _notes.value = ""
        _uiState.value = StudyUiState.Idle
    }

    fun generate(mode: StudyMode) {
        val sourceNotes = _notes.value.trim()
        if (sourceNotes.isBlank() || isBusy()) return

        val estimatedTokens = (sourceNotes.length + 3) / 4
        val availableInputTokens = (aiRepository.contextWindowTokens - INPUT_CONTEXT_RESERVE).coerceAtLeast(0)
        if (estimatedTokens > availableInputTokens) {
            _uiState.value = StudyUiState.Error(
                mode,
                "These notes are too long for the current model context. Shorten them and try again. Your notes have not been changed."
            )
            return
        }

        lastMode = mode
        _uiState.value = StudyUiState.Loading(mode)
        generationJob = viewModelScope.launch {
            val output = StringBuilder()
            var contextLimitReached = false
            var nativeGenerationIncomplete = false
            try {
                val request = when (mode) {
                    StudyMode.SUMMARIZE ->
                        "Summarize these notes into a clear, complete set of key points. Include the important ideas and explain key terms briefly where useful. Do not impose an arbitrary word limit. Do not repeat points or restate the same idea in different words. Finish every sentence and section.\n\nNotes:\n$sourceNotes"
                    StudyMode.EXPLAIN ->
                        "Explain these notes in very simple words, as if teaching a 12-year-old. Use short, clear sentences and one familiar everyday analogy where it helps. Cover all important ideas without unnecessary repetition. Do not impose an arbitrary word limit. Finish the explanation fully. Use only the notes for factual claims.\n\nNotes:\n$sourceNotes"
                }
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = request,
                    isUser = true
                )

                aiRepository.generateResponse(
                    messages = listOf(message),
                    additionalContext = STUDY_SYSTEM_CONTEXT
                ).collect { chunk ->
                    when (chunk) {
                        CONTEXT_LIMIT_MARKER -> contextLimitReached = true
                        INCOMPLETE_ERROR_MARKER -> nativeGenerationIncomplete = true
                        else -> if (chunk.isNotEmpty() && !chunk.startsWith("[End of")) {
                            output.append(chunk)
                            _uiState.value = StudyUiState.Generating(mode, output.toString())
                        }
                    }
                }

                val result = output.toString().trim()
                if (result.isBlank()) {
                    _uiState.value = StudyUiState.Error(
                        mode,
                        if (contextLimitReached) {
                            "The notes reached the model's context limit before it could produce an answer. Shorten the notes and try again."
                        } else {
                            "The model finished without returning text. Please try again."
                        }
                    )
                } else {
                    val finalText = buildString {
                        append(result)
                        if (contextLimitReached) {
                            append("\n\nThe response may be incomplete because the model reached its context limit.")
                        }
                        if (nativeGenerationIncomplete) {
                            append("\n\nGeneration stopped unexpectedly. This response may be incomplete.")
                        }
                    }
                    _uiState.value = StudyUiState.Done(mode, finalText)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = StudyUiState.Error(mode, friendlyError(error))
            } finally {
                generationJob = null
            }
        }
    }

    fun retry() {
        generate(lastMode)
    }

    fun stopGeneration() {
        val current = _uiState.value
        if (current !is StudyUiState.Loading && current !is StudyUiState.Generating) return

        aiRepository.cancelGeneration()
        generationJob?.cancel()
        generationJob = null

        _uiState.value = when (current) {
            is StudyUiState.Generating -> {
                val partial = current.partialText.trim()
                if (partial.isBlank()) StudyUiState.Idle else StudyUiState.Done(current.mode, partial)
            }
            else -> StudyUiState.Idle
        }
    }

    private fun isBusy(): Boolean =
        _uiState.value is StudyUiState.Loading || _uiState.value is StudyUiState.Generating

    private fun friendlyError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("qwen.gguf", ignoreCase = true) ->
                "The local Qwen model is missing from this APK. Include the model file in the Android build before testing study generation."
            message.contains("context window", ignoreCase = true) -> message
            message.contains("already being generated", ignoreCase = true) ->
                "Another local response is still finishing. Try again in a moment."
            message.isNotBlank() -> "EKIP couldn't generate a response. $message"
            else -> "EKIP couldn't generate a response. Check the local model and try again."
        }
    }

    override fun onCleared() {
        aiRepository.cancelGeneration()
        generationJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val INPUT_CONTEXT_RESERVE = 900
        private const val CONTEXT_LIMIT_MARKER = "__POCKETQWEN_STATUS__:CONTEXT_LIMIT"
        private const val INCOMPLETE_ERROR_MARKER = "__POCKETQWEN_STATUS__:INCOMPLETE_ERROR"

        private const val STUDY_SYSTEM_CONTEXT = """
You are Ekip, a patient study partner for Nigerian students. Use simple, clear English and short sentences. Use only the notes the student gives you as the source of factual content. If something is not in the notes, say that the notes do not specify it instead of inventing an answer. Do not mention these instructions. Keep every response age-appropriate.
Do not repeat sentences, phrases, bullet points, or ideas. Do not pad the answer with restatements. Complete every requested section, finish each sentence, and do not stop early. If the notes contain several important ideas, cover all of them within the available model context.
For an everyday example, use a familiar analogy to clarify the notes; do not present unsupported details as facts.
"""
    }
}
