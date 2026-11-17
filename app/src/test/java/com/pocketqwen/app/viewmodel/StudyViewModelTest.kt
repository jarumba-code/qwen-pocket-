package com.pocketqwen.app.viewmodel

import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.repository.AiRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAiRepository
    private lateinit var viewModel: StudyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAiRepository()
        viewModel = StudyViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun summarizeUsesOnlyTheSuppliedNotesAndStudyInstructions() = runTest(dispatcher) {
        repository.response = flowOf("Key idea one.\nKey idea two.")
        viewModel.updateNotes("Photosynthesis uses light energy.")
        viewModel.generate(StudyMode.SUMMARIZE)
        advanceUntilIdle()

        val result = viewModel.uiState.value as StudyUiState.Done
        assertEquals(StudyMode.SUMMARIZE, result.mode)
        assertTrue(result.text.contains("Key idea one"))
        assertTrue(repository.lastMessages.single().text.contains("Photosynthesis uses light energy."))
        assertTrue(repository.lastMessages.single().text.contains("at most 5 short bullet points"))
        assertTrue(repository.lastAdditionalContext.contains("patient study partner"))
    }

    @Test
    fun explainPromptRequestsSimpleLanguageAndNigerianEverydayExample() = runTest(dispatcher) {
        repository.response = flowOf("A simple explanation.")
        viewModel.updateNotes("Water evaporates when heated.")
        viewModel.generate(StudyMode.EXPLAIN)
        advanceUntilIdle()

        assertTrue(repository.lastMessages.single().text.contains("daily life in Nigeria"))
        assertTrue(repository.lastMessages.single().text.contains("under 150 words"))
        assertTrue(viewModel.uiState.value is StudyUiState.Done)
    }

    @Test
    fun blankNotesNeverStartGeneration() = runTest(dispatcher) {
        viewModel.generate(StudyMode.SUMMARIZE)
        advanceUntilIdle()

        assertEquals(0, repository.generateCalls)
        assertTrue(viewModel.uiState.value is StudyUiState.Idle)
    }

    @Test
    fun oversizedNotesAreRejectedWithoutChangingTheNotes() = runTest(dispatcher) {
        repository.contextTokens = 1000
        val notes = "Photosynthesis ".repeat(40)
        viewModel.updateNotes(notes)
        viewModel.generate(StudyMode.SUMMARIZE)
        advanceUntilIdle()

        assertEquals(notes, viewModel.notes.value)
        assertEquals(0, repository.generateCalls)
        val error = viewModel.uiState.value as StudyUiState.Error
        assertTrue(error.message.contains("too long"))
    }

    @Test
    fun stopKeepsPartialTextAndDoesNotTurnCancellationIntoAnError() = runTest(dispatcher) {
        repository.response = flow {
            emit("A partial explanation")
            awaitCancellation()
        }
        viewModel.updateNotes("Some notes")
        viewModel.generate(StudyMode.EXPLAIN)
        runCurrent()

        assertTrue(viewModel.uiState.value is StudyUiState.Generating)
        viewModel.stopGeneration()
        advanceUntilIdle()

        val result = viewModel.uiState.value as StudyUiState.Done
        assertEquals("A partial explanation", result.text)
        assertFalse(viewModel.uiState.value is StudyUiState.Error)
    }

    @Test
    fun incompleteNativeGenerationIsClearlyMarked() = runTest(dispatcher) {
        repository.response = flowOf("Partial answer", "__POCKETQWEN_STATUS__:INCOMPLETE_ERROR")
        viewModel.updateNotes("Some class notes")
        viewModel.generate(StudyMode.SUMMARIZE)
        advanceUntilIdle()

        val result = viewModel.uiState.value as StudyUiState.Done
        assertTrue(result.text.contains("Partial answer"))
        assertTrue(result.text.contains("response may be incomplete"))
    }

    private class FakeAiRepository : AiRepository {
        var response: Flow<String> = flowOf("Test response")
        var contextTokens = 2048
        var generateCalls = 0
            private set
        var lastMessages: List<ChatMessage> = emptyList()
            private set
        var lastAdditionalContext: String = ""
            private set

        override val contextWindowTokens: Int
            get() = contextTokens

        override fun generateResponse(messages: List<ChatMessage>): Flow<String> {
            return generateResponse(messages, null)
        }

        override fun generateResponse(
            messages: List<ChatMessage>,
            additionalContext: String?
        ): Flow<String> {
            generateCalls++
            lastMessages = messages
            lastAdditionalContext = additionalContext.orEmpty()
            return response
        }

        override fun isModelAvailable(): Boolean = true
        override fun getModelStatus(): String = "Test model ready"
    }
}
