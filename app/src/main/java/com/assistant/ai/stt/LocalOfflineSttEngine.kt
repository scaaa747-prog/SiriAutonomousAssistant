package com.assistant.ai.stt

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalOfflineSttEngine(private val context: Context) : SpeechRecognizerEngine {

    private val fallbackEngine = AndroidSpeechRecognizerEngine(context)

    override val state: StateFlow<SttState> = fallbackEngine.state
    override val audioRmsFlow: StateFlow<Float> = fallbackEngine.audioRmsFlow

    override suspend fun startListening(languageLocale: String) {
        fallbackEngine.startListening(languageLocale)
    }

    override suspend fun stopListening() {
        fallbackEngine.stopListening()
    }

    override fun cancel() {
        fallbackEngine.cancel()
    }

    override fun destroy() {
        fallbackEngine.destroy()
    }
}
