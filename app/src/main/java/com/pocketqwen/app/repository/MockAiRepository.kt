package com.pocketqwen.app.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * UI-preview repository. Responses are simulated and never use a language model.
 */
class MockAiRepository : AiRepository {
    override fun isModelAvailable(): Boolean = false

    override fun getModelStatus(): String = "No model loaded — chat responses are simulated"

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")
        delay(350)
        emit("[Mock response] PocketQwen's chat interface is working, but local AI inference is not connected yet. Your message was: \"$message\"")
        emit("[End of placeholder response]")
    }
}