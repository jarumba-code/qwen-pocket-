package com.pocketqwen.app.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.Conversation
import com.pocketqwen.app.model.MessageStatus

/**
 * Small private local conversation store. Data remains in this app's private
 * preferences and is never sent to a server.
 */
class ConversationStore(context: Context) {
    private val preferences = context.getSharedPreferences("pocketqwen_conversations", Context.MODE_PRIVATE)

    fun loadAll(): List<Conversation> {
        val raw = preferences.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val messagesJson = item.optJSONArray("messages") ?: JSONArray()
                val messages = (0 until messagesJson.length()).mapNotNull { messageIndex ->
                    val m = messagesJson.optJSONObject(messageIndex) ?: return@mapNotNull null
                    ChatMessage(
                        id = m.optString("id"),
                        text = m.optString("text"),
                        isUser = m.optBoolean("isUser"),
                        timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                        status = runCatching { MessageStatus.valueOf(m.optString("status", "SENT")) }
                            .getOrDefault(MessageStatus.SENT)
                    )
                }
                Conversation(
                    id = item.optString("id"),
                    title = item.optString("title", "Conversation"),
                    messages = messages,
                    updatedAt = item.optLong("updatedAt", 0L)
                )
            }.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(conversations: List<Conversation>) {
        val array = JSONArray()
        conversations.sortedByDescending { it.updatedAt }.forEach { conversation ->
            val messages = JSONArray()
            conversation.messages.forEach { message ->
                messages.put(JSONObject()
                    .put("id", message.id)
                    .put("text", message.text)
                    .put("isUser", message.isUser)
                    .put("timestamp", message.timestamp)
                    .put("status", message.status.name))
            }
            array.put(JSONObject()
                .put("id", conversation.id)
                .put("title", conversation.title)
                .put("updatedAt", conversation.updatedAt)
                .put("messages", messages))
        }
        preferences.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    fun delete(id: String) {
        save(loadAll().filterNot { it.id == id })
    }

    companion object {
        private const val KEY_CONVERSATIONS = "conversations_json"
    }
}
