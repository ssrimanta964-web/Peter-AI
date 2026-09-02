package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PeterSettings(
    val speechRate: Float = 1.10f,
    val speechPitch: Float = 1.12f,
    val voiceName: String = "Tom Holland Male (British Young Hero)",
    val useNeuralStudioVoice: Boolean = true,
    val preferredLanguage: String = "Auto Detect (English, Hindi, Bengali)", // "Auto Detect (English, Hindi, Bengali)", "English", "Hindi (हिन्दी)", "Bengali (বাংলা)"
    val aiProvider: String = "Auto (Gemini + Local)", // "Auto (Gemini + Local)", "Local Only", "Cloud Only"
    val wakeWordEnabled: Boolean = true,
    val lowPowerMode: Boolean = false,
    val animationQuality: String = "High", // "High", "Medium", "Low"
    val autoSpeakResponses: Boolean = true,
    val bossName: String = "Srimanta",
    val bossTitle: String = "Creator & Boss",
    val bossDetails: String = "Visionary creator of PETER AI, genius software engineer, tech innovator, and superhero commander!",
    val bossNickname: String = "Boss",
    val isLockdownActive: Boolean = false,
    val customApiKey: String = ""
)

class PeterPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("peter_assistant_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PeterSettings> = _settings.asStateFlow()

    private fun loadSettings(): PeterSettings {
        return PeterSettings(
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.10f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.12f),
            voiceName = prefs.getString(KEY_VOICE_NAME, "Tom Holland Male (British Young Hero)") ?: "Tom Holland Male (British Young Hero)",
            useNeuralStudioVoice = prefs.getBoolean(KEY_NEURAL_VOICE, true),
            preferredLanguage = prefs.getString(KEY_LANGUAGE, "Auto Detect (English, Hindi, Bengali)") ?: "Auto Detect (English, Hindi, Bengali)",
            aiProvider = prefs.getString(KEY_AI_PROVIDER, "Auto (Gemini + Local)") ?: "Auto (Gemini + Local)",
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD, true),
            lowPowerMode = prefs.getBoolean(KEY_LOW_POWER, false),
            animationQuality = prefs.getString(KEY_ANIM_QUALITY, "High") ?: "High",
            autoSpeakResponses = prefs.getBoolean(KEY_AUTO_SPEAK, true),
            bossName = prefs.getString(KEY_BOSS_NAME, "Srimanta") ?: "Srimanta",
            bossTitle = prefs.getString(KEY_BOSS_TITLE, "Creator & Boss") ?: "Creator & Boss",
            bossDetails = prefs.getString(KEY_BOSS_DETAILS, "Visionary creator of PETER AI, genius software engineer, tech innovator, and superhero commander!") ?: "Visionary creator of PETER AI, genius software engineer, tech innovator, and superhero commander!",
            bossNickname = prefs.getString(KEY_BOSS_NICKNAME, "Boss") ?: "Boss",
            isLockdownActive = prefs.getBoolean(KEY_LOCKDOWN_ACTIVE, false),
            customApiKey = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        )
    }

    fun updateBossProfile(name: String, title: String, details: String, nickname: String = "Boss") {
        prefs.edit()
            .putString(KEY_BOSS_NAME, name.trim())
            .putString(KEY_BOSS_TITLE, title.trim())
            .putString(KEY_BOSS_DETAILS, details.trim())
            .putString(KEY_BOSS_NICKNAME, nickname.trim())
            .apply()
        _settings.value = _settings.value.copy(
            bossName = name.trim().ifEmpty { "Srimanta" },
            bossTitle = title.trim().ifEmpty { "Creator & Boss" },
            bossDetails = details.trim().ifEmpty { "Visionary creator of PETER AI" },
            bossNickname = nickname.trim().ifEmpty { "Boss" }
        )
    }

    fun updatePreferredLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        _settings.value = _settings.value.copy(preferredLanguage = language)
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

    fun updateUseNeuralStudioVoice(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEURAL_VOICE, enabled).apply()
        _settings.value = _settings.value.copy(useNeuralStudioVoice = enabled)
    }

    fun setLockdownActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKDOWN_ACTIVE, active).apply()
        _settings.value = _settings.value.copy(isLockdownActive = active)
    }

    fun updateCustomApiKey(apiKey: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, apiKey.trim()).apply()
        _settings.value = _settings.value.copy(customApiKey = apiKey.trim())
    }

    companion object {
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_VOICE_NAME = "key_voice_name"
        private const val KEY_NEURAL_VOICE = "key_neural_voice"
        private const val KEY_LANGUAGE = "key_preferred_language"
        private const val KEY_AI_PROVIDER = "key_ai_provider"
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_LOW_POWER = "key_low_power"
        private const val KEY_ANIM_QUALITY = "key_anim_quality"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_BOSS_NAME = "key_boss_name"
        private const val KEY_BOSS_TITLE = "key_boss_title"
        private const val KEY_BOSS_DETAILS = "key_boss_details"
        private const val KEY_BOSS_NICKNAME = "key_boss_nickname"
        private const val KEY_LOCKDOWN_ACTIVE = "key_lockdown_active"
        private const val KEY_CUSTOM_API_KEY = "key_custom_gemini_api_key"
    }
}
