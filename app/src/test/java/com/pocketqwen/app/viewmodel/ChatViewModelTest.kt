package com.pocketqwen.app.viewmodel

import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.repository.MockAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestRepository : AiRepository {
        val messages = mutableListOf<String>()

        override fun generateResponse(message: String): Flow<String> {
            messages.add(message)
            return flow {
                emit("Placeholder response for: $message")
                emit("[End of placeholder response]")
            }
        }

        override fun isModelAvailable(): Boolean = false
        override fun getModelStatus(): String = "Not installed"
    }

    @Test
    fun `initial message is present`() {
        val vm = ChatViewModel(TestRepository())
        val messages = vm.messages.value
        assertNotNull(messages)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `sending empty message does not add message`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("")
        vm.sendMessage("   ")
        assertTrue(vm.messages.value.isEmpty())
        assertEquals(0, repo.messages.size)
    }

    @Test
    fun `sending message adds user and assistant messages`() {
        val vm = ChatViewModel(TestRepository())
        vm.sendMessage("Hello")
        val messages = vm.messages.value
        assertEquals(2, messages.size)
        assertTrue(messages[0].isUser)
        assertEquals("Hello", messages[0].text)
        assertFalse(messages[1].isUser)
        assertEquals("Placeholder response for: Hello", messages[1].text)
    }

    @Test
    fun `repository receives message`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("Test message")
        assertEquals(listOf("Test message"), repo.messages)
    }

    @Test
    fun `model not available`() {
        val repo = MockAiRepository()
        assertFalse(repo.isModelAvailable())
        assertTrue(repo.getModelStatus().contains("simulated", ignoreCase = true))
    }

    @Test
    fun `clearMessages removes all messages`() {
        val vm = ChatViewModel(TestRepository())
        vm.sendMessage("Test")
        assertEquals(2, vm.messages.value.size)
        vm.clearMessages()
        assertTrue(vm.messages.value.isEmpty())
    }
}
