package com.pocketqwen.app.viewmodel

import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus
import com.pocketqwen.app.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAiRepository
    private lateinit var viewModel: ChatViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAiRepository()
        viewModel = ChatViewModel(repository)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun sendMessage_streamsChunksIntoSingleAssistantMessage() = runTest(dispatcher) {
        repository.response = flowOf("Hello", ", ", "world!")
        viewModel.sendMessage("  Say hello  ")
        advanceUntilIdle()
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertTrue(messages[0].isUser)
        assertEquals("Say hello", messages[0].text)
        assertFalse(messages[1].isUser)
        assertEquals("Hello, world!", messages[1].text)
        assertEquals(MessageStatus.SENT, messages[1].status)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.thinking.value)
    }

    @Test fun blankMessageIsIgnored() = runTest(dispatcher) {
        viewModel.sendMessage("   ")
        advanceUntilIdle()
        assertTrue(viewModel.messages.value.isEmpty())
        assertEquals(0, repository.generateCalls)
    }

    @Test fun generationFailureIsShownAsErrorMessage() = runTest(dispatcher) {
        repository.response = flow { throw IllegalStateException("Model unavailable") }
        viewModel.sendMessage("Test")
        advanceUntilIdle()
        val assistant = viewModel.messages.value.last()
        assertFalse(assistant.isUser)
        assertEquals(MessageStatus.ERROR, assistant.status)
        assertTrue(assistant.text.contains("Model unavailable"))
        assertFalse(viewModel.isLoading.value)
    }

    @Test fun switchingConversationRestoresItsMessages() = runTest(dispatcher) {
        val saved = listOf(
            ChatMessage(id = "saved-user", text = "Previously asked", isUser = true),
            ChatMessage(id = "saved-assistant", text = "Previously answered", isUser = false)
        )
        viewModel.activateConversation("conversation-1", saved)
        assertEquals(saved, viewModel.messages.value)
    }

    @Test fun separateConversationsKeepTheirOwnMessages() = runTest(dispatcher) {
        repository.response = flowOf("Answer one")
        viewModel.activateConversation("one")
        viewModel.sendMessage("Question one")
        advanceUntilIdle()
        val firstConversation = viewModel.messages.value
        viewModel.activateConversation("two")
        assertTrue(viewModel.messages.value.isEmpty())
        repository.response = flowOf("Answer two")
        viewModel.sendMessage("Question two")
        advanceUntilIdle()
        assertEquals("Answer one", firstConversation.last().text)
        assertEquals("Answer two", viewModel.messages.value.last().text)
        viewModel.activateConversation("one")
        assertEquals(firstConversation, viewModel.messages.value)
    }

    @Test fun cancellationIsNotPresentedAsGenerationFailure() = runTest(dispatcher) {
        repository.response = flow {
            emit("Partial ")
            awaitCancellation()
        }
        viewModel.sendMessage("Keep this partial")
        runCurrent()
        viewModel.stopGeneration()
        advanceUntilIdle()
        val assistant = viewModel.messages.value.last()
        assertEquals("Partial ", assistant.text)
        assertEquals(MessageStatus.SENT, assistant.status)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test fun stoppingAnOldJobDoesNotRemoveANewerJobForSameChat() = runTest(dispatcher) {
        repository.response = flow {
            emit("Old partial")
            awaitCancellation()
        }
        viewModel.sendMessage("First")
        runCurrent()
        viewModel.stopGeneration()
        repository.response = flowOf("New answer")
        viewModel.sendMessage("Second")
        advanceUntilIdle()
        assertEquals("New answer", viewModel.messages.value.last().text)
        assertFalse(viewModel.isLoading.value)
    }

    private class FakeAiRepository : AiRepository {
        var response: Flow<String> = flowOf("Test response")
        var generateCalls = 0
            private set
        override fun generateResponse(message: String): Flow<String> {
            generateCalls++
            return response
        }
        override fun isModelAvailable(): Boolean = true
        override fun getModelStatus(): String = "Test model ready"
    }
}
