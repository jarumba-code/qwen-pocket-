package com.pocketqwen.app.viewmodel

import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.repository.MockAiRepository
import com.pocketqwen.app.repository.AiRepository
import org.junit.Test
import org.junit.Assert.*

class ChatViewModelTest {

    private class TestRepository : AiRepository {
        var messages: MutableList<String> = mutableListOf()
        override fun generateResponse(message: String): kotlinx.coroutines.flow.Flow<String> {
            messages.add(message)
            return kotlinx.coroutines.flow.flow {
                emit("Placeholder response for: $message")
            }
        }
        override fun isModelAvailable(): Boolean = false
        override fun getModelStatus(): String = "Not installed"
    }

    @Test
    fun `initial message is present`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        val messages = vm.messages.value
        assertNotNull(messages)
        assertTrue(messages.isNotEmpty())
        assertEquals(1, messages.size)
        assertFalse(messages[0].isUser)
    }

    @Test
    fun `sending empty message does not add message`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("")
        vm.sendMessage("   ")
        assertEquals(1, vm.messages.value.size)
        assertEquals(0, repo.messages.size)
    }

    @Test
    fun `sending message adds user message`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("Hello")
        val messages = vm.messages.value
        assertEquals(2, messages.size)
        assertTrue(messages[1].isUser)
        assertEquals("Hello", messages[1].text)
    }

    @Test
    fun `repository receives message`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("Test message")
        assertEquals(1, repo.messages.size)
        assertEquals("Test message", repo.messages[0])
    }

    @Test
    fun `model not available`() {
        val repo = MockAiRepository()
        assertFalse(repo.isModelAvailable())
        assertEquals("Not installed", repo.getModelStatus())
    }

    @Test
    fun `clearMessages removes all messages`() {
        val repo = TestRepository()
        val vm = ChatViewModel(repo)
        vm.sendMessage("Test")
        assertEquals(2, vm.messages.value.size)
        vm.clearMessages()
        assertTrue(vm.messages.value.isEmpty())
    }
}
