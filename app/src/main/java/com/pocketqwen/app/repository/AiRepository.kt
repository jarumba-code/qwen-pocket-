package com.pocketqwen.app.repository

import com.pocketqwen.app.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI chat responses.
 *
 * This interface isolates AI inference logic so that a future llama.cpp
 * JNI implementation can replace the mock implementation cleanly.
 * No cloud AI APIs are used.
 */
interface AiRepository {
    /**
     * Returns a flow of assistant response chunks for a given user message.
     * The first emission may be a loading placeholder; subsequent emissions
     * contain the response text incrementally.
     */
    fun generateResponse(message: String): Flow<String>

    /**
     * Returns whether a local AI model is currently available for inference.
     * Always false until llama.cpp integration is implemented.
     */
    fun isModelAvailable(): Boolean

    /**
     * Returns a human-readable model status string for display in settings.
     */
    fun getModelStatus(): String
}
