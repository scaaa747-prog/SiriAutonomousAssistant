package com.assistant.ai.voice

import android.content.Context
import android.util.Log

interface WakeWordListener {
    fun onWakeWordDetected()
}

class WakeWordDetector(private val context: Context) {

    private var listener: WakeWordListener? = null
    private var isListening = false

    fun start(listener: WakeWordListener) {
        this.listener = listener
        this.isListening = true
        Log.i(TAG, "Lightweight wake-word detector initialized for background activation.")
    }

    fun stop() {
        this.isListening = false
        this.listener = null
        Log.i(TAG, "Wake-word detector stopped.")
    }

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}
