package com.assistant.ai.data

import android.content.Context
import android.content.SharedPreferences
import com.assistant.ai.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_US,
    val lowEndDeviceOptimization: Boolean = true,
    val confirmSensitiveActions: Boolean = true,
    val wakeWordEnabled: Boolean = false
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private fun loadSettings(): SettingsState {
        val langCode = prefs.getString(KEY_VOICE_LANGUAGE, VoiceLanguage.ENGLISH_US.code) ?: VoiceLanguage.ENGLISH_US.code
        val lowEnd = prefs.getBoolean(KEY_LOW_END_OPTIMIZATION, true)
        val confirm = prefs.getBoolean(KEY_CONFIRM_SENSITIVE, true)
        val wakeWord = prefs.getBoolean(KEY_WAKE_WORD, false)

        return SettingsState(
            voiceLanguage = VoiceLanguage.fromCode(langCode),
            lowEndDeviceOptimization = lowEnd,
            confirmSensitiveActions = confirm,
            wakeWordEnabled = wakeWord
        )
    }

    fun setVoiceLanguage(language: VoiceLanguage) {
        prefs.edit().putString(KEY_VOICE_LANGUAGE, language.code).apply()
        _settingsState.value = _settingsState.value.copy(voiceLanguage = language)
    }

    fun setLowEndDeviceOptimization(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_END_OPTIMIZATION, enabled).apply()
        _settingsState.value = _settingsState.value.copy(lowEndDeviceOptimization = enabled)
    }

    fun setConfirmSensitiveActions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_SENSITIVE, enabled).apply()
        _settingsState.value = _settingsState.value.copy(confirmSensitiveActions = enabled)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply()
        _settingsState.value = _settingsState.value.copy(wakeWordEnabled = enabled)
    }

    companion object {
        private const val PREF_NAME = "assistant_settings"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_LOW_END_OPTIMIZATION = "low_end_optimization"
        private const val KEY_CONFIRM_SENSITIVE = "confirm_sensitive"
        private const val KEY_WAKE_WORD = "wake_word_enabled"
    }
}
