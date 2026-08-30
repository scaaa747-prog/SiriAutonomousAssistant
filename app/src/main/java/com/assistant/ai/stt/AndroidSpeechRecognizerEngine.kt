package com.assistant.ai.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidSpeechRecognizerEngine(private val context: Context) : SpeechRecognizerEngine {

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    override val state: StateFlow<SttState> = _state.asStateFlow()

    private val _audioRmsFlow = MutableStateFlow(0f)
    override val audioRmsFlow: StateFlow<Float> = _audioRmsFlow.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false
    private var lastLanguageLocale = "en-US"

    override suspend fun startListening(languageLocale: String) {
        lastLanguageLocale = languageLocale
        isListeningActive = true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SttState.Error("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup previous speech recognizer error: ${e.message}")
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val locale = try {
            val parts = languageLocale.split("-", "_")
            if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(languageLocale)
        } catch (e: Exception) {
            Locale.getDefault()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
            // Extended timeouts to prevent mic from closing after 1 sec of silence
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
        }

        _state.value = SttState.Listening
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listener", e)
            _state.value = SttState.Error("Could not start speech recognition: ${e.localizedMessage}")
        }
    }

    override suspend fun stopListening() {
        isListeningActive = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Stop listening error: ${e.message}")
        }
    }

    override fun cancel() {
        isListeningActive = false
        try {
            speechRecognizer?.cancel()
            _state.value = SttState.Idle
        } catch (e: Exception) {
            Log.w(TAG, "Cancel error: ${e.message}")
        }
    }

    override fun destroy() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SttState.Listening
            }

            override fun onBeginningOfSpeech() {
                _state.value = SttState.Processing("")
            }

            override fun onRmsChanged(rmsdB: Float) {
                _audioRmsFlow.value = (rmsdB.coerceIn(0f, 10f) / 10f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.value = SttState.Processing()
            }

            override fun onError(error: Int) {
                val isTimeoutOrNoMatch = error == SpeechRecognizer.ERROR_NO_MATCH ||
                                          error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Speech recognition error code: $error"
                }

                Log.w(TAG, "STT Error ($error): $errorMsg")

                if (isListeningActive && isTimeoutOrNoMatch) {
                    // Auto-restart listening if timeout occurred while user is still in listening mode
                    kotlinx.coroutines.MainScope().kotlinx.coroutines.launch {
                        kotlinx.coroutines.delay(300)
                        if (isListeningActive) {
                            startListening(lastLanguageLocale)
                        }
                    }
                } else {
                    _state.value = SttState.Error(errorMsg)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _state.value = SttState.Success(text)
                } else if (isListeningActive) {
                    kotlinx.coroutines.MainScope().kotlinx.coroutines.launch {
                        kotlinx.coroutines.delay(300)
                        if (isListeningActive) {
                            startListening(lastLanguageLocale)
                        }
                    }
                } else {
                    _state.value = SttState.Error("No text transcribed.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _state.value = SttState.Processing(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    companion object {
        private const val TAG = "AndroidSpeechRecognizer"
    }
}
