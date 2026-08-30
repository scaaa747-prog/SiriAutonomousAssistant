package com.assistant.ai.stt

import kotlinx.coroutines.flow.StateFlow

sealed class SttState {
    object Idle : SttState()
    object Listening : SttState()
    data class Processing(val partialText: String = "") : SttState()
    data class Success(val text: String) : SttState()
    data class Error(val message: String) : SttState()
}

interface SpeechRecognizerEngine {
    val state: StateFlow<SttState>
    val audioRmsFlow: StateFlow<Float>

    suspend fun startListening(languageLocale: String = "en-US")
    suspend fun stopListening()
    fun cancel()
    fun destroy()
}
