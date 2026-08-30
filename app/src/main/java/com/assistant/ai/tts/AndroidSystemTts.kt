package com.assistant.ai.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidSystemTts(private val context: Context) : SpeechSynthesizer, TextToSpeech.OnInitListener {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false
    private var currentLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_US

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguage(currentLanguage)
            setupListener()
            Log.i(TAG, "Local TextToSpeech engine initialized successfully.")
        } else {
            Log.e(TAG, "Failed to initialize Local TextToSpeech engine.")
            _state.value = TtsState.Error("Failed to initialize TTS engine")
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsState.Speaking(utteranceId ?: "")
            }

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error("Speech output error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = TtsState.Error("Speech error code: $errorCode")
            }
        })
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return

        if (!isInitialized) {
            Log.w(TAG, "TTS requested before initialization complete. Waiting...")
            // Brief wait if initializing
            var retries = 0
            while (!isInitialized && retries < 10) {
                kotlinx.coroutines.delay(100)
                retries++
            }
        }

        if (tts == null || !isInitialized) {
            _state.value = TtsState.Error("TTS Engine not ready")
            return
        }

        _state.value = TtsState.Speaking(text)

        val params = HashMap<String, String>()
        val utteranceId = text.take(20)
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _state.value = TtsState.Error("Failed to speak text")
        }
    }

    override fun stop() {
        try {
            tts?.stop()
            _state.value = TtsState.Idle
        } catch (e: Exception) {
            Log.w(TAG, "TTS stop error: ${e.message}")
        }
    }

    override fun setLanguage(voiceLanguage: VoiceLanguage): Boolean {
        currentLanguage = voiceLanguage
        if (tts == null || !isInitialized) return false
        val result = tts?.setLanguage(voiceLanguage.locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "AndroidSystemTts"
    }
}
