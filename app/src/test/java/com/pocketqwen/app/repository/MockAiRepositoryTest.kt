package com.pocketqwen.app.repository

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
        repository.generateResponse("hello").collect { emissions += it }
        assertTrue(emissions.any { it.startsWith("[Mock response]") })
        assertTrue(emissions.any { it.startsWith("[End of placeholder") })
    }
}