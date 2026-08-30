package com.assistant.ai

import android.app.Application
import android.util.Log

class AssistantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupUncaughtExceptionHandler()
        Log.d(TAG, "AssistantApplication initialized successfully.")
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val TAG = "AssistantApplication"
        lateinit var instance: AssistantApplication
            private set
    }
}
