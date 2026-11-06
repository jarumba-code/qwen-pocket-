package com.pocketqwen.app.memory

import android.content.Context
import com.pocketqwen.app.model.ChatMessage
import org.json.JSONArray
import java.util.Locale

/**
 * Small extractive memory index stored in the app's private SharedPreferences.
 * Full messages remain in ConversationStore/SQLite; these notes are only a retrieval aid.
 */
class LocalMemoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pocketqwen_local_memory", Context.MODE_PRIVATE
    )

    @Synchronized
    fun rememberConversation(conversationId: String, messages: List<ChatMessage>) {
        val global = readNotes(GLOBAL_KEY).toMutableList()
        val localKey = conversationKey(conversationId)
        val local = readNotes(localKey).toMutableList()

        messages.asSequence()
            .filter { it.isUser }
            .map { it.text.trim().replace(Regex("\\s+"), " ").take(MAX_NOTE_CHARS) }
            .filter(::isMemoryWorthy)
            .forEach { note ->
                val target = if (isGlobalPreference(note)) global else local
                if (target.none { it.equals(note, ignoreCase = true) }) target += note
            }

        writeNotes(GLOBAL_KEY, global.takeLast(MAX_GLOBAL_NOTES))
        writeNotes(localKey, local.takeLast(MAX_CONVERSATION_NOTES))
    }

    @Synchronized
    fun relevantContext(conversationId: String, query: String): String {
        val global = readNotes(GLOBAL_KEY)
        val local = readNotes(conversationKey(conversationId))
        val notes = (global + local).distinctBy { it.lowercase(Locale.ROOT) }
        if (notes.isEmpty()) return ""

        val queryTerms = terms(query)
        val ranked = notes.mapIndexed { index, note ->
            val noteTerms = terms(note)
            val overlap = queryTerms.count { it in noteTerms }
            val preferenceBonus = if (note in global.takeLast(4)) 1 else 0
            Triple(note, overlap + preferenceBonus, index)
        }.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })

        val matching = ranked.filter { it.second > 1 }.take(MAX_RETRIEVED_NOTES)
        val selected = if (matching.isNotEmpty()) matching.map { it.first }
            else (global.takeLast(2) + local.takeLast(2)).distinct().takeLast(MAX_RETRIEVED_NOTES)
        if (selected.isEmpty()) return ""
        return buildString {
            append("Relevant saved memory from this device. Use only when it helps answer the latest request; current user instructions take priority.\n")
            selected.forEach { append("- ").append(it).append('\n') }
        }.trim()
    }

    private fun isMemoryWorthy(note: String): Boolean {
        if (note.length < 18 || note.length > MAX_NOTE_CHARS) return false
        val lower = note.lowercase(Locale.ROOT)
        val sensitive = listOf(
            "password", "passphrase", "private key", "seed phrase", "recovery phrase",
            "api key", "access token", "secret key", "bank account", "account number",
            "card number", "medical history", "diagnosis", "medication", "health condition"
        )
        if (sensitive.any { it in lower }) return false
        val signals = listOf(
            "remember", "keep in mind", "from now on", "i prefer", "my preference",
            "i use", "i'm using", "i am using", "my project", "my goal", "we decided",
            "i'm building", "i am building", "for this app", "this app should",
            "always", "my role", "my name is", "my workflow"
        )
        return signals.any { it in lower }
    }

    private fun isGlobalPreference(note: String): Boolean {
        val lower = note.lowercase(Locale.ROOT)
        return listOf(
            "from now on", "i prefer", "my preference", "always", "my role",
            "my name is", "my goal", "i use"
        ).any { it in lower }
    }

    private fun terms(value: String): Set<String> =
        value.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()

    private fun readNotes(key: String): List<String> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun writeNotes(key: String, notes: List<String>) {
        val array = JSONArray()
        notes.forEach { note -> array.put(note) }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private fun conversationKey(id: String) = "conversation:$id"

    private companion object {
        const val GLOBAL_KEY = "global"
        const val MAX_NOTE_CHARS = 280
        const val MAX_GLOBAL_NOTES = 24
        const val MAX_CONVERSATION_NOTES = 16
        const val MAX_RETRIEVED_NOTES = 6
    }
}
