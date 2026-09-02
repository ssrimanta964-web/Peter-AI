package com.example.domain.ai

import com.example.core.model.IntentType
import com.example.core.model.PeterIntent

data class AIResponse(
    val intent: PeterIntent,
    val directAnswer: String? = null,
    val isFromCloud: Boolean = false,
    val error: String? = null
)

interface AIProvider {
    val name: String
    suspend fun analyzeCommand(
        prompt: String,
        conversationHistory: List<com.example.core.model.ChatMessage> = emptyList()
    ): AIResponse
}
