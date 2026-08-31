package com.example.domain.ai

import com.example.core.model.IntentType
import com.example.data.local.PeterPreferences
import com.example.domain.device.DeviceController

class AIBrain(
    private val deviceController: DeviceController,
    private val preferences: PeterPreferences,
    private val cloudProvider: AIProvider = GeminiCloudProvider(),
    private val offlineProvider: AIProvider = OfflineCommandProvider()
) {
    suspend fun processUserPrompt(prompt: String): AIResponse {
        val mode = preferences.settings.value.aiProvider
        val netInfo = deviceController.getNetworkInfo()

        // If user forced Local Only or no internet is available, use Offline Provider directly
        if (mode == "Local Only" || !netInfo.isConnected) {
            return offlineProvider.analyzeCommand(prompt)
        }

        // Fast path: if the command is a direct, obvious local device command (e.g. flashlight, volume, battery), offline can resolve instantly without network latency
        val localAttempt = offlineProvider.analyzeCommand(prompt)
        if (localAttempt.intent.type != IntentType.AI_QUERY && localAttempt.intent.confidence >= 0.90f) {
            return localAttempt
        }

        // Otherwise, send to Cloud Provider (Gemini)
        val cloudResponse = cloudProvider.analyzeCommand(prompt)
        if (cloudResponse.error == null) {
            return cloudResponse
        }

        // Fallback: If cloud had network failure or missing API key, fall back to offline provider
        return localAttempt
    }
}
