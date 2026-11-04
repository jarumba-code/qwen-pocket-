package com.pocketqwen.app.repository

import com.pocketqwen.app.model.ChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiRepositoryTest {
    private val repository = MockAiRepository()

    @Test
    fun modelIsNotReportedAsAvailableInPreviewMode() {
        assertFalse(repository.isModelAvailable())
        assertTrue(repository.getModelStatus().contains("simulated", ignoreCase = true))
    }

    @Test
    fun responseIsClearlyMarkedAsMock() = runBlocking {
        val emissions = mutableListOf<String>()
        repository.generateResponse(
            listOf(ChatMessage(id = "test-user", text = "hello", isUser = true))
        ).collect { emissions += it }

        assertTrue(emissions.any { it.startsWith("[Mock response]") })
        assertTrue(emissions.any { it.startsWith("[End of placeholder") })
    }
}
