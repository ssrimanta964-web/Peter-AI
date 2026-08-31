package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PeterSettings(
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceName: String = "Default",
    val aiProvider: String = "Auto (Gemini + Local)", // "Auto (Gemini + Local)", "Local Only", "Cloud Only"
    val wakeWordEnabled: Boolean = false,
    val lowPowerMode: Boolean = false,
    val animationQuality: String = "High", // "High", "Medium", "Low"
    val autoSpeakResponses: Boolean = true
)

class PeterPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("peter_assistant_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PeterSettings> = _settings.asStateFlow()

    private fun loadSettings(): PeterSettings {
        return PeterSettings(
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f),
            voiceName = prefs.getString(KEY_VOICE_NAME, "Default") ?: "Default",
            aiProvider = prefs.getString(KEY_AI_PROVIDER, "Auto (Gemini + Local)") ?: "Auto (Gemini + Local)",
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD, false),
            lowPowerMode = prefs.getBoolean(KEY_LOW_POWER, false),
            animationQuality = prefs.getString(KEY_ANIM_QUALITY, "High") ?: "High",
            autoSpeakResponses = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        )
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
        _settings.value = _settings.value.copy(speechPitch = pitch)
    }

    fun updateVoiceName(voiceName: String) {
        prefs.edit().putString(KEY_VOICE_NAME, voiceName).apply()
        _settings.value = _settings.value.copy(voiceName = voiceName)
    }

    fun updateAiProvider(provider: String) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider).apply()
        _settings.value = _settings.value.copy(aiProvider = provider)
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply()
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
    }

    fun updateLowPowerMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_POWER, enabled).apply()
        _settings.value = _settings.value.copy(lowPowerMode = enabled)
    }

    fun updateAnimationQuality(quality: String) {
        prefs.edit().putString(KEY_ANIM_QUALITY, quality).apply()
        _settings.value = _settings.value.copy(animationQuality = quality)
    }

    fun updateAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
        _settings.value = _settings.value.copy(autoSpeakResponses = enabled)
    }

    companion object {
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_VOICE_NAME = "key_voice_name"
        private const val KEY_AI_PROVIDER = "key_ai_provider"
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_LOW_POWER = "key_low_power"
        private const val KEY_ANIM_QUALITY = "key_anim_quality"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
    }
}
