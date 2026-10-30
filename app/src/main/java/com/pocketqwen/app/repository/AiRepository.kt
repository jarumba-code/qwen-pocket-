package com.pocketqwen.app.repository

import kotlinx.coroutines.flow.Flow

/** Isolates local inference from the UI. Implementations must not send prompts to a cloud service. */
interface AiRepository {
    fun generateResponse(message: String): Flow<String>
    fun isModelAvailable(): Boolean
    fun getModelStatus(): String

    /** Prepare the local model if supported. Called off the main thread by the settings view model. */
    suspend fun initializeModel(): Boolean = isModelAvailable()

    /** Request cancellation of an active local generation, if supported. */
    fun cancelGeneration() {}
}
