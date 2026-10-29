package com.pocketqwen.app.repository

import com.pocketqwen.app.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun generateResponse(message: String): Flow<String>
    fun isModelAvailable(): Boolean
    fun getModelStatus(): String
}
