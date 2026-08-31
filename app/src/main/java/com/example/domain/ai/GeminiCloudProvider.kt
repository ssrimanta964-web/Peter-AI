package com.example.domain.ai

import com.example.BuildConfig
import com.example.core.model.IntentType
import com.example.core.model.PeterIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiCloudProvider : AIProvider {
    override val name: String = "Gemini AI Brain (Online)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeCommand(prompt: String): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AIResponse(
                intent = PeterIntent(
                    type = IntentType.AI_QUERY,
                    rawText = prompt,
                    query = prompt
                ),
                error = "API key not configured in Secrets panel. Falling back to local AI brain."
            )
        }

        try {
            val systemPrompt = """
                You are PETER, an advanced professional Android AI assistant.
                When the user speaks to you, determine if their request is an Android Device Command or a Conversational / Knowledge question.
                
                Respond ONLY with a JSON object in this format:
                {
                  "intent": "OPEN_APP" | "FLASHLIGHT" | "BATTERY_STATUS" | "VOLUME_CONTROL" | "OPEN_SETTINGS" | "TIME_AND_DATE" | "ALARM" | "TIMER" | "PHONE_STATUS" | "NETWORK_STATUS" | "AI_QUERY",
                  "action": "ON" | "OFF" | "UP" | "DOWN" | "MAX" | "MUTE" | "SET" | "",
                  "target_app": "youtube" | "chrome" | "camera" | "calculator" | "maps" | "clock" | "settings" | "spotify" | "whatsapp" | "",
                  "target_setting": "wifi" | "bluetooth" | "display" | "sound" | "battery" | "date" | "location" | "",
                  "value": 0,
                  "spoken_response": "Short, natural, professional voice response suitable for TTS (1-2 sentences)"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AIResponse(
                    intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                    error = "HTTP ${response.code}: $responseBody"
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (text.isNotBlank()) {
                val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(cleanJson)
                val intentStr = parsed.optString("intent", "AI_QUERY")
                val action = parsed.optString("action", "")
                val targetApp = parsed.optString("target_app", "")
                val targetSetting = parsed.optString("target_setting", "")
                val value = parsed.optInt("value", 0)
                val spoken = parsed.optString("spoken_response", "")

                val intentType = runCatching { IntentType.valueOf(intentStr) }.getOrDefault(IntentType.AI_QUERY)

                return@withContext AIResponse(
                    intent = PeterIntent(
                        type = intentType,
                        rawText = prompt,
                        action = action,
                        targetApp = targetApp,
                        targetSetting = targetSetting,
                        value = value,
                        query = prompt
                    ),
                    directAnswer = if (intentType == IntentType.AI_QUERY) spoken.ifEmpty { null } else null,
                    isFromCloud = true
                )
            }

            AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                error = "Empty response from Gemini API"
            )
        } catch (e: Exception) {
            AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                error = e.localizedMessage ?: "Network connection error"
            )
        }
    }
}
