package com.assistant.ai.data

import android.content.Context
import android.content.SharedPreferences

interface AssistantMemory {
    suspend fun save(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun search(query: String): Map<String, String>
    suspend fun clear()
}

class LocalAssistantMemory(context: Context) : AssistantMemory {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override suspend fun save(key: String, value: String) {
        prefs.edit().putString(key.lowercase().trim(), value).apply()
    }

    override suspend fun get(key: String): String? {
        return prefs.getString(key.lowercase().trim(), null)
    }

    override suspend fun search(query: String): Map<String, String> {
        val results = mutableMapOf<String, String>()
        val q = query.lowercase().trim()
        prefs.all.forEach { (key, value) ->
            val valStr = value?.toString() ?: ""
            if (key.lowercase().contains(q) || valStr.lowercase().contains(q)) {
                results[key] = valStr
            }
        }
        return results
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "assistant_user_memory"
    }
}
