package com.example.core.model

/**
 * PETER AI Core operational states
 */
enum class PeterState {
    IDLE,
    LISTENING,
    PROCESSING,
    THINKING,
    SPEAKING,
    ERROR
}

/**
 * Standardized structured intents extracted by AI Brain or Offline Pattern Engine
 */
enum class IntentType {
    OPEN_APP,
    FLASHLIGHT,
    BATTERY_STATUS,
    VOLUME_CONTROL,
    MEDIA_CONTROL,
    OPEN_SETTINGS,
    TIME_AND_DATE,
    ALARM,
    TIMER,
    PHONE_STATUS,
    NETWORK_STATUS,
    WEB_SEARCH,
    SHOW_PROOF,
    SCREEN_SEARCH,
    EMERGENCY_LOCKDOWN,
    AI_QUERY,
    UNKNOWN
}

/**
 * Structured Command Intent
 */
data class PeterIntent(
    val type: IntentType,
    val rawText: String,
    val action: String = "",
    val targetApp: String = "",
    val targetSetting: String = "",
    val value: Int = 0,
    val query: String = "",
    val confidence: Float = 1.0f
)

/**
 * Result of a routed command execution
 */
data class CommandResult(
    val success: Boolean,
    val intentType: IntentType,
    val spokenResponse: String,
    val displayDetails: String = "",
    val requiresPermission: String? = null,
    val errorMessage: String? = null,
    val searchQuery: String? = null
)

/**
 * Message in conversation stream
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val intentType: IntentType? = null,
    val statusSuccess: Boolean? = null,
    val searchQuery: String? = null
)

