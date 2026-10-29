package com.pocketqwen.app.repository

import com.pocketqwen.app.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of [AiRepository] for UI development only.
 *
 * This implementation does NOT perform real AI inference. It returns a
 * clearly identified placeholder response so that the chat UI can be
 * tested and demonstrated before llama.cpp integration.
 *
 * Replace this implementation with a llama.cpp-backed repository when
 * native inference is ready.
 */
class MockAiRepository : AiRepository {

    private val placeholderResponses = listOf(
        "This is a placeholder response. Real AI inference is not yet available.",
        "I'm a mock assistant. llama.cpp integration is pending.",
        "PocketQwen is ready for on-device AI once the model is integrated.",
        "Placeholder: your message was received, but no model is loaded."
    )

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")
        delay(1500)
        val response = placeholderResponses[message.hashCode() % placeholderResponses.size]
        emit(response)
        delay(800)
        emit("[End of placeholder response]")
    }

    override fun isModelAvailable(): Boolean = false

    override fun getModelStatus(): String = "Not installed"
}
