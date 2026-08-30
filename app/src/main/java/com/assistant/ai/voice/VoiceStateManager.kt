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

    private var sttObserveJob: Job? = null
    private var ttsObserveJob: Job? = null

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
        sttObserveJob?.cancel()
        sttObserveJob = scope.launch {
            sttEngine.state.collectLatest { state ->
                when (state) {
                    is SttState.Processing -> {
                        if (state.partialText.isNotBlank()) {
                            _spokenText.value = state.partialText
                        }
                    }
                    is SttState.Success -> {
                        _spokenText.value = state.text
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
        ttsObserveJob?.cancel()
        ttsObserveJob = scope.launch {
            ttsEngine.state.collectLatest { state ->
                if (state is TtsState.Idle && settingsRepository.settingsState.value.continuousListeningEnabled) {
                    // Continuous Listening Loop: Auto-restart microphone when TTS finishes speaking
                    delay(600)
                    if (agentState.value is AgentState.Speaking) {
                        startListening()
                    }
                }
            }
        }
    }

    fun startListening() {
        scope.launch {
            ttsEngine.stop()
            val currentLanguage = settingsRepository.settingsState.value.voiceLanguage
            _spokenText.value = ""
            sttEngine.startListening(currentLanguage.code)
        }
    }

    fun stopListening() {
        scope.launch {
            sttEngine.stopListening()
        }
    }

    fun processUserSpeech(userInput: String) {
        if (userInput.isBlank()) return
        scope.launch {
            sttEngine.stopListening()
            val response = autonomousAgent.executeTask(userInput)
            if (response.isNotBlank()) {
                ttsEngine.speak(response)
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
        autonomousAgent.cancelTask()
        ttsEngine.stop()
        sttEngine.cancel()
    }

    fun cleanup() {
        sttEngine.destroy()
        ttsEngine.shutdown()
    }

    companion object {
        private const val TAG = "VoiceStateManager"
    }
}
