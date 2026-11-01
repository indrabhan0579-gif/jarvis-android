package com.jarvis.assistant.data.repository

import android.content.Context
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.MessageSender
import org.json.JSONArray
import org.json.JSONObject

class ConversationRepository(private val context: Context) {

    private val PREFS_NAME = "jarvis_history"
    private val KEY_MESSAGES = "chat_messages"

    fun loadMessages(): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MESSAGES, null) ?: return getInitialGreeting()

        val list = mutableListOf<ChatMessage>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val senderStr = obj.optString("sender", "JARVIS")
                val sender = try { MessageSender.valueOf(senderStr) } catch (e: Exception) { MessageSender.JARVIS }
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        sender = sender,
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            return getInitialGreeting()
        }
        return if (list.isEmpty()) getInitialGreeting() else list
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        // Save the last 50 messages to keep storage fast
        messages.takeLast(50).forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("sender", msg.sender.name)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MESSAGES, jsonArray.toString()).apply()
    }

    fun clearMessages() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MESSAGES).apply()
    }

    fun getInitialGreeting(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                sender = MessageSender.JARVIS,
                text = "Good day, Sir. All core diagnostics online. Ready for your directive."
            )
        )
    }
}
