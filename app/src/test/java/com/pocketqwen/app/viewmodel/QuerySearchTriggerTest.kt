package com.pocketqwen.app.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuerySearchTriggerTest {
    @Test fun detectsCurrentAndTimeSensitiveQuestions() {
        assertTrue(QuerySearchTrigger.shouldSearch("What is the latest Qwen release?"))
        assertTrue(QuerySearchTrigger.shouldSearch("What is the weather today?"))
        assertTrue(QuerySearchTrigger.shouldSearch("Find the current exchange rate"))
        assertTrue(QuerySearchTrigger.shouldSearch("What happened in the news this week?"))
    }

    @Test fun doesNotSearchForStableKnowledgeQuestions() {
        assertFalse(QuerySearchTrigger.shouldSearch("Explain how a compiler works"))
        assertFalse(QuerySearchTrigger.shouldSearch("What is a binary tree?"))
    }

    @Test fun explicitOfflineInstructionWinsOverCurrentnessTrigger() {
        assertFalse(QuerySearchTrigger.shouldSearch("Don't search online; explain today's date format"))
    }
}
