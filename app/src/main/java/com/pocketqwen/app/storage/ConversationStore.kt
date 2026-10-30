package com.pocketqwen.app.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.Conversation
import com.pocketqwen.app.model.MessageStatus

/**
 * SQLite-backed, app-private conversation history.
 *
 * Conversations and messages are stored in separate relational tables. On first
 * open, this store imports history from the previous SharedPreferences JSON
 * format so existing local chats are not discarded during the storage upgrade.
 */
class ConversationStore(context: Context) {
    private val appContext = context.applicationContext
    private val dbHelper = ConversationDatabase(appContext)

    fun loadAll(): List<Conversation> {
        val db = dbHelper.readableDatabase
        val result = mutableListOf<Conversation>()
        db.query(
            ConversationDatabase.TABLE_CONVERSATIONS,
            arrayOf("id", "title", "updated_at"),
            null, null, null, null,
            "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                result += Conversation(
                    id = id,
                    title = cursor.getString(1),
                    messages = loadMessages(db, id),
                    updatedAt = cursor.getLong(2)
                )
            }
        }
        return result
    }

    fun save(conversations: List<Conversation>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val keepIds = conversations.map { it.id }.toSet()
            if (keepIds.isEmpty()) {
                db.delete(ConversationDatabase.TABLE_CONVERSATIONS, null, null)
            } else {
                val placeholders = keepIds.joinToString(",") { "?" }
                db.delete(
                    ConversationDatabase.TABLE_CONVERSATIONS,
                    "id NOT IN ($placeholders)",
                    keepIds.toTypedArray()
                )
            }

            conversations.forEach { conversation ->
                val values = ContentValues().apply {
                    put("id", conversation.id)
                    put("title", conversation.title)
                    put("updated_at", conversation.updatedAt)
                }
                db.insertWithOnConflict(
                    ConversationDatabase.TABLE_CONVERSATIONS,
                    null, values, SQLiteDatabase.CONFLICT_REPLACE
                )
                db.delete(
                    ConversationDatabase.TABLE_MESSAGES,
                    "conversation_id = ?",
                    arrayOf(conversation.id)
                )
                conversation.messages.forEachIndexed { index, message ->
                    val messageValues = ContentValues().apply {
                        put("id", message.id)
                        put("conversation_id", conversation.id)
                        put("text", message.text)
                        put("is_user", if (message.isUser) 1 else 0)
                        put("timestamp", message.timestamp)
                        put("status", message.status.name)
                        put("position", index)
                    }
                    db.insertOrThrow(ConversationDatabase.TABLE_MESSAGES, null, messageValues)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: String) {
        dbHelper.writableDatabase.delete(
            ConversationDatabase.TABLE_CONVERSATIONS, "id = ?", arrayOf(id)
        )
    }

    private fun loadMessages(db: SQLiteDatabase, conversationId: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        db.query(
            ConversationDatabase.TABLE_MESSAGES,
            arrayOf("id", "text", "is_user", "timestamp", "status"),
            "conversation_id = ?", arrayOf(conversationId),
            null, null, "position ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val status = runCatching { MessageStatus.valueOf(cursor.getString(4)) }
                    .getOrDefault(MessageStatus.SENT)
                messages += ChatMessage(
                    id = cursor.getString(0),
                    text = cursor.getString(1),
                    isUser = cursor.getInt(2) != 0,
                    timestamp = cursor.getLong(3),
                    status = status
                )
            }
        }
        return messages
    }

    private class ConversationDatabase(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE $TABLE_CONVERSATIONS (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE $TABLE_MESSAGES (
                    id TEXT NOT NULL PRIMARY KEY,
                    conversation_id TEXT NOT NULL,
                    text TEXT NOT NULL,
                    is_user INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    FOREIGN KEY(conversation_id) REFERENCES $TABLE_CONVERSATIONS(id) ON DELETE CASCADE
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX idx_messages_conversation_position ON $TABLE_MESSAGES(conversation_id, position)")
            migrateLegacyPreferences(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future schema changes should use explicit migrations; never drop user chat history.
        }

        private fun migrateLegacyPreferences(db: SQLiteDatabase) {
            val preferences = context.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
            val raw = preferences.getString(LEGACY_KEY, null) ?: return
            try {
                val conversations = JSONArray(raw)
                db.beginTransaction()
                try {
                    for (i in 0 until conversations.length()) {
                        val item = conversations.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isBlank()) continue
                        val values = ContentValues().apply {
                            put("id", id)
                            put("title", item.optString("title", "Conversation"))
                            put("updated_at", item.optLong("updatedAt", 0L))
                        }
                        db.insertWithOnConflict(TABLE_CONVERSATIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        val messages = item.optJSONArray("messages") ?: JSONArray()
                        for (j in 0 until messages.length()) {
                            val message = messages.optJSONObject(j) ?: continue
                            val messageId = message.optString("id")
                            if (messageId.isBlank()) continue
                            val status = runCatching {
                                MessageStatus.valueOf(message.optString("status", "SENT"))
                            }.getOrDefault(MessageStatus.SENT)
                            val messageValues = ContentValues().apply {
                                put("id", messageId)
                                put("conversation_id", id)
                                put("text", message.optString("text"))
                                put("is_user", if (message.optBoolean("isUser")) 1 else 0)
                                put("timestamp", message.optLong("timestamp", System.currentTimeMillis()))
                                put("status", status.name)
                                put("position", j)
                            }
                            db.insertWithOnConflict(TABLE_MESSAGES, null, messageValues, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                // Remove legacy JSON only after the transaction has committed.
                preferences.edit().remove(LEGACY_KEY).apply()
            } catch (_: Exception) {
                // Keep the legacy value if import fails so it can be recovered later.
            }
        }

        companion object {
            private const val DATABASE_NAME = "pocketqwen_history.db"
            private const val DATABASE_VERSION = 1
            const val TABLE_CONVERSATIONS = "conversations"
            const val TABLE_MESSAGES = "messages"
            private const val LEGACY_PREFERENCES = "pocketqwen_conversations"
            private const val LEGACY_KEY = "conversations_json"
        }
    }
}
