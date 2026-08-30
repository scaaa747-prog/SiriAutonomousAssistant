package com.assistant.ai.ui

import android.app.Application
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

    val isAccessibilityEnabled: StateFlow<Boolean> = AssistantAccessibilityService.isServiceActive

    private val _batteryLevel = MutableStateFlow(systemExecutor.getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _hasMicPermission = MutableStateFlow(permissionManager.hasRecordAudioPermission())
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    init {
        registerTools()
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

    fun refreshPermissions() {
        _hasMicPermission.value = permissionManager.hasRecordAudioPermission()
        _batteryLevel.value = systemExecutor.getBatteryLevel()
    }

    fun onMicClick() {
        refreshPermissions()
        if (agentState.value is AgentState.Listening) {
            voiceStateManager.stopListening()
        } else {
            voiceStateManager.startListening()
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

    override fun onCleared() {
        super.onCleared()
        voiceStateManager.cleanup()
        viewModelScope.launch {
            modelManager.releaseResources()
        }
    }
}
