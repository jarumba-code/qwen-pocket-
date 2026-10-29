package com.pocketqwen.app.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
        val response = placeholderResponses[message.hashCode().absoluteValue % placeholderResponses.size]
        emit(response)
    }

    override fun isModelAvailable(): Boolean = false

    override fun getModelStatus(): String = "Local model not installed"
}
