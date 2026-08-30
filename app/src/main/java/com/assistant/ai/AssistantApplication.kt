package com.assistant.ai

import android.app.Application
import android.util.Log

class AssistantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AssistantApplication initialized.")
    }

    companion object {
        private const val TAG = "AssistantApplication"
        lateinit var instance: AssistantApplication
            private set
    }
}
