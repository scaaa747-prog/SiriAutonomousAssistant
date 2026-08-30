package com.assistant.ai.tts

import kotlinx.coroutines.flow.StateFlow

sealed class TtsState {
    object Idle : TtsState()
    data class Speaking(val text: String) : TtsState()
    data class Error(val message: String) : TtsState()
}

interface SpeechSynthesizer {
    val state: StateFlow<TtsState>

    suspend fun speak(text: String)
    fun stop()
    fun setLanguage(voiceLanguage: VoiceLanguage): Boolean
    fun shutdown()
}
