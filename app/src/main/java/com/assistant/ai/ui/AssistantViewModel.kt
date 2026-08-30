package com.assistant.ai.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assistant.ai.accessibility.AssistantAccessibilityService
import com.assistant.ai.android.PermissionManager
import com.assistant.ai.android.SystemCommandExecutor
import com.assistant.ai.agent.AgentState
import com.assistant.ai.agent.AutonomousAgent
import com.assistant.ai.core.ToolRegistry
import com.assistant.ai.core.tools.GetBatteryTool
import com.assistant.ai.core.tools.GetBrightnessTool
import com.assistant.ai.core.tools.GetCurrentAppTool
import com.assistant.ai.core.tools.GetVolumeTool
import com.assistant.ai.core.tools.LongPressElementTool
import com.assistant.ai.core.tools.OpenAppTool
import com.assistant.ai.core.tools.OpenSettingsTool
import com.assistant.ai.core.tools.PauseMediaTool
import com.assistant.ai.core.tools.PlayMediaTool
import com.assistant.ai.core.tools.PressBackTool
import com.assistant.ai.core.tools.PressHomeTool
import com.assistant.ai.core.tools.ReadScreenTool
import com.assistant.ai.core.tools.ScrollTool
import com.assistant.ai.core.tools.SetBrightnessTool
import com.assistant.ai.core.tools.SetVolumeTool
import com.assistant.ai.core.tools.TapElementTool
import com.assistant.ai.core.tools.TypeTextTool
import com.assistant.ai.data.ConversationContext
import com.assistant.ai.data.SettingsRepository
import com.assistant.ai.data.SettingsState
import com.assistant.ai.model.ModelManager
import com.assistant.ai.stt.AndroidSpeechRecognizerEngine
import com.assistant.ai.stt.SttState
import com.assistant.ai.tts.AndroidSystemTts
import com.assistant.ai.tts.TtsState
import com.assistant.ai.tts.VoiceLanguage
import com.assistant.ai.voice.VoiceStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    val permissionManager = PermissionManager(application)
    val systemExecutor = SystemCommandExecutor(application)
    val settingsRepository = SettingsRepository(application)
    val conversationContext = ConversationContext()

    private val toolRegistry = ToolRegistry()
    private val modelManager = ModelManager(application)
    private val sttEngine = AndroidSpeechRecognizerEngine(application)
    private val ttsEngine = AndroidSystemTts(application)

    private val autonomousAgent = AutonomousAgent(
        toolRegistry = toolRegistry,
        modelManager = modelManager,
        conversationContext = conversationContext,
        settingsRepository = settingsRepository
    )

    val voiceStateManager = VoiceStateManager(
        sttEngine = sttEngine,
        ttsEngine = ttsEngine,
        autonomousAgent = autonomousAgent,
        settingsRepository = settingsRepository,
        scope = viewModelScope
    )

    val agentState: StateFlow<AgentState> = voiceStateManager.agentState
    val sttState: StateFlow<SttState> = voiceStateManager.sttState
    val ttsState: StateFlow<TtsState> = voiceStateManager.ttsState
    val spokenText: StateFlow<String> = voiceStateManager.spokenText
    val rmsAmplitude: StateFlow<Float> = voiceStateManager.audioRmsFlow
    val settingsState: StateFlow<SettingsState> = settingsRepository.settingsState

    // Unified UI state that combines agent + stt state for the UI layer
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    val isAccessibilityEnabled: StateFlow<Boolean> = AssistantAccessibilityService.isServiceActive

    private val _batteryLevel = MutableStateFlow(systemExecutor.getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _hasMicPermission = MutableStateFlow(permissionManager.hasRecordAudioPermission())
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    // Last response from the assistant for display
    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    init {
        registerTools()
        observeStates()
    }

    private fun registerTools() {
        toolRegistry.registerTool(OpenAppTool(systemExecutor))
        toolRegistry.registerTool(OpenSettingsTool(systemExecutor))
        toolRegistry.registerTool(PressBackTool())
        toolRegistry.registerTool(PressHomeTool())
        toolRegistry.registerTool(GetCurrentAppTool())
        toolRegistry.registerTool(TapElementTool())
        toolRegistry.registerTool(LongPressElementTool())
        toolRegistry.registerTool(ScrollTool())
        toolRegistry.registerTool(TypeTextTool())
        toolRegistry.registerTool(ReadScreenTool())
        toolRegistry.registerTool(SetVolumeTool(systemExecutor))
        toolRegistry.registerTool(GetVolumeTool(systemExecutor))
        toolRegistry.registerTool(SetBrightnessTool(systemExecutor))
        toolRegistry.registerTool(GetBrightnessTool(systemExecutor))
        toolRegistry.registerTool(GetBatteryTool(systemExecutor))
        toolRegistry.registerTool(PlayMediaTool(systemExecutor))
        toolRegistry.registerTool(PauseMediaTool(systemExecutor))
    }

    private fun observeStates() {
        // Track listening state from STT engine
        viewModelScope.launch {
            sttState.collect { state ->
                _isListening.value = state is SttState.Listening || state is SttState.Processing
            }
        }
        // Track agent responses for display
        viewModelScope.launch {
            agentState.collect { state ->
                when (state) {
                    is AgentState.Speaking -> _lastResponse.value = state.message
                    is AgentState.Error -> _lastResponse.value = state.message
                    else -> {}
                }
            }
        }
    }

    fun refreshPermissions() {
        _hasMicPermission.value = permissionManager.hasRecordAudioPermission()
        _batteryLevel.value = systemExecutor.getBatteryLevel()
    }

    fun onMicClick() {
        refreshPermissions()
        if (!_hasMicPermission.value) {
            Log.w(TAG, "Microphone permission not granted")
            return
        }

        val currentSttState = sttState.value
        val currentAgentState = agentState.value

        Log.d(TAG, "onMicClick: sttState=$currentSttState, agentState=$currentAgentState")

        when {
            // If currently listening, stop
            currentSttState is SttState.Listening || currentSttState is SttState.Processing -> {
                voiceStateManager.stopListening()
            }
            // If agent is busy (thinking/acting/speaking), cancel and start fresh
            currentAgentState is AgentState.Thinking ||
            currentAgentState is AgentState.Acting ||
            currentAgentState is AgentState.Speaking -> {
                voiceStateManager.cancelAction()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(200)
                    voiceStateManager.startListening()
                }
            }
            // Default: start listening
            else -> {
                voiceStateManager.startListening()
            }
        }
    }

    fun onStopClick() {
        voiceStateManager.cancelAction()
    }

    fun onQuickActionClick(actionText: String) {
        voiceStateManager.processUserSpeech(actionText)
    }

    fun confirmAction(toolName: String, arguments: Map<String, Any?>) {
        voiceStateManager.confirmAction(toolName, arguments)
    }

    fun cancelAction() {
        voiceStateManager.cancelAction()
    }

    fun setVoiceLanguage(language: VoiceLanguage) {
        settingsRepository.setVoiceLanguage(language)
    }

    fun setLowEndOptimization(enabled: Boolean) {
        settingsRepository.setLowEndDeviceOptimization(enabled)
    }

    fun setConfirmSensitiveActions(enabled: Boolean) {
        settingsRepository.setConfirmSensitiveActions(enabled)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        settingsRepository.setWakeWordEnabled(enabled)
    }

    fun setContinuousListeningEnabled(enabled: Boolean) {
        settingsRepository.setContinuousListeningEnabled(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        voiceStateManager.cleanup()
        viewModelScope.launch {
            modelManager.releaseResources()
        }
    }

    companion object {
        private const val TAG = "AssistantViewModel"
    }
}
