package com.assistant.ai.voice

import android.util.Log
import com.assistant.ai.agent.AgentState
import com.assistant.ai.agent.AutonomousAgent
import com.assistant.ai.data.SettingsRepository
import com.assistant.ai.stt.SpeechRecognizerEngine
import com.assistant.ai.stt.SttState
import com.assistant.ai.tts.SpeechSynthesizer
import com.assistant.ai.tts.TtsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoiceStateManager(
    private val sttEngine: SpeechRecognizerEngine,
    private val ttsEngine: SpeechSynthesizer,
    private val autonomousAgent: AutonomousAgent,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    val agentState: StateFlow<AgentState> = autonomousAgent.agentState
    val sttState: StateFlow<SttState> = sttEngine.state
    val ttsState: StateFlow<TtsState> = ttsEngine.state
    val audioRmsFlow: StateFlow<Float> = sttEngine.audioRmsFlow

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private var processingJob: Job? = null
    private var didJustSpeak = false

    init {
        observeSettings()
        observeSttState()
        observeTtsState()
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settingsState.collectLatest { settings ->
                ttsEngine.setLanguage(settings.voiceLanguage)
            }
        }
    }

    private fun observeSttState() {
        scope.launch {
            sttEngine.state.collectLatest { state ->
                when (state) {
                    is SttState.Processing -> {
                        if (state.partialText.isNotBlank()) {
                            _spokenText.value = state.partialText
                        }
                    }
                    is SttState.Success -> {
                        _spokenText.value = state.text
                        Log.d(TAG, "STT Success: '${state.text}'")
                        processUserSpeech(state.text)
                    }
                    is SttState.Error -> {
                        Log.w(TAG, "STT Error: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeTtsState() {
        scope.launch {
            ttsEngine.state.collectLatest { state ->
                when (state) {
                    is TtsState.Idle -> {
                        // When TTS finishes speaking, reset agent to Idle
                        if (didJustSpeak) {
                            didJustSpeak = false
                            autonomousAgent.resetToIdle()

                            // Continuous Listening: auto-restart mic after response
                            if (settingsRepository.settingsState.value.continuousListeningEnabled) {
                                delay(500)
                                startListening()
                            }
                        }
                    }
                    is TtsState.Speaking -> {
                        didJustSpeak = true
                    }
                    else -> {}
                }
            }
        }
    }

    fun startListening() {
        scope.launch {
            ttsEngine.stop()
            val currentLanguage = settingsRepository.settingsState.value.voiceLanguage
            _spokenText.value = ""
            autonomousAgent.setListening()
            Log.d(TAG, "Starting STT listening with language: ${currentLanguage.code}")
            sttEngine.startListening(currentLanguage.code)
        }
    }

    fun stopListening() {
        scope.launch {
            sttEngine.stopListening()
            autonomousAgent.resetToIdle()
        }
    }

    fun processUserSpeech(userInput: String) {
        if (userInput.isBlank()) return

        // Cancel any previous processing job
        processingJob?.cancel()
        processingJob = scope.launch {
            sttEngine.stopListening()
            Log.d(TAG, "Processing user speech: '$userInput'")
            val response = autonomousAgent.executeTask(userInput)
            Log.d(TAG, "Agent response: '$response'")
            if (response.isNotBlank()) {
                ttsEngine.speak(response)
            } else {
                autonomousAgent.resetToIdle()
            }
        }
    }

    fun confirmAction(toolName: String, arguments: Map<String, Any?>) {
        scope.launch {
            val response = autonomousAgent.continueConfirmedAction(toolName, arguments)
            if (response.isNotBlank()) {
                ttsEngine.speak(response)
            }
        }
    }

    fun cancelAction() {
        processingJob?.cancel()
        autonomousAgent.cancelTask()
        ttsEngine.stop()
        sttEngine.cancel()
        _spokenText.value = ""
    }

    fun cleanup() {
        sttEngine.destroy()
        ttsEngine.shutdown()
    }

    companion object {
        private const val TAG = "VoiceStateManager"
    }
}
