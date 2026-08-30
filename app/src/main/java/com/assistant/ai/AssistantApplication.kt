package com.assistant.ai

import android.app.Application
import android.util.Log

class AssistantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            com.assistant.ai.android.VoiceAssistantForegroundService.startService(this)
        } catch (e: Exception) {
            Log.w(TAG, "Could not start foreground service: ${e.message}")
        }
        Log.d(TAG, "AssistantApplication initialized.")
    }

    companion object {
        private const val TAG = "AssistantApplication"
        lateinit var instance: AssistantApplication
            private set
    }
}
