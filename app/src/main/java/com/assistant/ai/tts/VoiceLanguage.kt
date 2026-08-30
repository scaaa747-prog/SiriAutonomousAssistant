package com.assistant.ai.tts

import java.util.Locale

data class VoiceLanguage(
    val code: String,
    val displayName: String,
    val locale: Locale
) {
    companion object {
        val ENGLISH_US = VoiceLanguage("en-US", "English (US)", Locale.US)
        val ENGLISH_UK = VoiceLanguage("en-GB", "English (UK)", Locale.UK)
        val SPANISH = VoiceLanguage("es-ES", "Spanish (Spain)", Locale("es", "ES"))
        val FRENCH = VoiceLanguage("fr-FR", "French", Locale.FRANCE)
        val GERMAN = VoiceLanguage("de-DE", "German", Locale.GERMANY)
        val ITALIAN = VoiceLanguage("it-IT", "Italian", Locale.ITALY)
        val JAPANESE = VoiceLanguage("ja-JP", "Japanese", Locale.JAPAN)
        val CHINESE = VoiceLanguage("zh-CN", "Chinese (Simplified)", Locale.CHINA)
        val HINDI = VoiceLanguage("hi-IN", "Hindi", Locale("hi", "IN"))

        val SUPPORTED_LANGUAGES = listOf(
            ENGLISH_US,
            ENGLISH_UK,
            SPANISH,
            FRENCH,
            GERMAN,
            ITALIAN,
            JAPANESE,
            CHINESE,
            HINDI
        )

        fun fromCode(code: String): VoiceLanguage {
            return SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: ENGLISH_US
        }
    }
}
