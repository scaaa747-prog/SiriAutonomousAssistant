package com.assistant.ai.tts

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class LocalNeuralTtsEngine(private val context: Context) : SpeechSynthesizer {

    private val fallbackSystemTts = AndroidSystemTts(context)
    override val state: StateFlow<TtsState> = fallbackSystemTts.state

    override suspend fun speak(text: String) {
        fallbackSystemTts.speak(text)
    }

    override fun stop() {
        fallbackSystemTts.stop()
    }

    override fun setLanguage(voiceLanguage: VoiceLanguage): Boolean {
        return fallbackSystemTts.setLanguage(voiceLanguage)
    }

    override fun shutdown() {
        fallbackSystemTts.shutdown()
    }
}
