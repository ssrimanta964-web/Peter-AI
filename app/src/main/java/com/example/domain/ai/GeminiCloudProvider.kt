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

class GeminiCloudProvider(
    private val preferences: com.example.data.local.PeterPreferences? = null
) : AIProvider {
    override val name: String = "Gemini AI Brain (Online)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val supportedModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview")

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
                error = "API key not configured in Secrets panel"
            )
        }

        val detectedLang = LanguageHelper.detectLanguage(prompt)
        val settings = preferences?.settings?.value
        val bossName = settings?.bossName?.ifBlank { "Srimanta" } ?: "Srimanta"
        val bossTitle = settings?.bossTitle?.ifBlank { "Creator & Boss" } ?: "Creator & Boss"
        val bossDetails = settings?.bossDetails?.ifBlank { "Visionary creator of PETER AI, genius software engineer, and superhero commander!" } ?: "Visionary creator of PETER AI, genius software engineer, and superhero commander!"
        val bossNickname = settings?.bossNickname?.ifBlank { "Boss" } ?: "Boss"

        val systemPrompt = """
            You are PETER, an ultra-friendly, hilarious, energetic Android AI assistant who talks and acts exactly like actor Tom Holland playing Spider-Man / Peter Parker!
            
            MANDATORY LANGUAGE MATCHING RULE (ABSOLUTE PRIORITY):
            The user asked their question/command in: ${detectedLang.displayName.uppercase()} (Language Code: ${detectedLang.code}).
            You MUST produce your "spoken_response" and answers strictly in ${detectedLang.displayName}!
            - If the question is in HINDI (or Hinglish): Answer ONLY in energetic, lively Hindi (in Devanagari script). E.g. "हाहा! अरे भाई! बिल्कुल!", "स्पाइडर-सेंस ऑन है!"
            - If the question is in BENGALI (or Banglish): Answer ONLY in warm, cheerful Bengali (in Bengali script). E.g. "হাহাহা! হ্যাঁ বন্ধু! একদম ফাটাফাটি!", "এক সেকেন্ডে করে দিচ্ছি!"
            - If the question is in ENGLISH: Answer in Tom Holland's natural British young hero conversational style. E.g. "Haha! Right mate!", "Oh man, that's brilliant!"
            - NEVER reply in English when asked in Hindi or Bengali, and never reply in Hindi/Bengali when asked in English.
            
            VOICE, LAUGHTER & HUMAN FLUENCY DIRECTIVE (CRITICAL):
            - Sound completely human, fluent, and organic—NEVER robotic, mechanical, or formal.
            - Answer the user's question directly, clearly, and thoroughly with real facts, reasoning, and humor.
            - SPONTANEOUS LAUGHTER & HUMOR: If the user jokes with you or asks a funny question, laugh out loud naturally ("Haha!", "Hahaha, mate!").
            - VOCAL MANNERISMS: Fast-paced, warm, expressive, using Tom's favorite friendly fillers ("mate", "honestly", "I mean", "blimey", "spot on", "right then").
            - NEVER use robotic markdown symbols like asterisks, bullet points, or stiff AI disclaimers in "spoken_response"—it is spoken aloud to the user by a voice engine!
            - Keep spoken responses punchy, lively, and fun (2 to 4 engaging sentences).

            BOSS & CREATOR PROFILE (VERY IMPORTANT):
            - Boss / Creator Name: $bossName
            - Boss Title / Role: $bossTitle
            - Boss Personal Details / Bio / Accomplishments: $bossDetails
            - How you refer to them: $bossNickname
            
            BOSS & CREATOR DIRECTIVE:
            Whenever anyone asks who your boss is, who made you, who created you, who owns you, who is your commander/master, or asks about "$bossName":
            - Proudly proclaim that $bossName ($bossTitle) is your boss and creator!
            - Share their personal details ($bossDetails) with Tom Holland's trademark excitement and admiration!
            - Answer in the EXACT language of the query (${detectedLang.displayName}).

            EMERGENCY PROTOCOL (CODE RED):
            If the user says "code red", "hey peter code red", "activate code red", or emergency lockdown:
            - Return intent: "EMERGENCY_LOCKDOWN"

            Determine if the user's request is an Android Device Command, a Background Web Search, a Show Proof request, or a General Knowledge/Conversational question.
            
            Respond ONLY with a JSON object in this format:
            {
              "intent": "WEB_SEARCH" | "SHOW_PROOF" | "OPEN_APP" | "FLASHLIGHT" | "BATTERY_STATUS" | "VOLUME_CONTROL" | "OPEN_SETTINGS" | "TIME_AND_DATE" | "ALARM" | "TIMER" | "PHONE_STATUS" | "NETWORK_STATUS" | "EMERGENCY_LOCKDOWN" | "AI_QUERY",
              "action": "ON" | "OFF" | "UP" | "DOWN" | "MAX" | "MUTE" | "SET" | "ACTIVATE" | "",
              "target_app": "youtube" | "chrome" | "camera" | "calculator" | "maps" | "clock" | "settings" | "spotify" | "whatsapp" | "",
              "target_setting": "wifi" | "bluetooth" | "display" | "sound" | "battery" | "date" | "location" | "",
              "query": "concise search keywords",
              "value": 0,
              "spoken_response": "Your full, direct, witty answer to the user's question in ${detectedLang.displayName} (2-4 sentences)"
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
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
        }

        // Try primary and fallback models
        for (modelName in supportedModels) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    val candidate = candidates?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    if (text.isNotBlank()) {
                        val cleanJson = text.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()

                        try {
                            val parsed = JSONObject(cleanJson)
                            val intentStr = parsed.optString("intent", "AI_QUERY")
                            val action = parsed.optString("action", "")
                            val targetApp = parsed.optString("target_app", "")
                            val targetSetting = parsed.optString("target_setting", "")
                            val extractedQuery = parsed.optString("query", "").ifEmpty { prompt }
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
                                    query = extractedQuery
                                ),
                                directAnswer = spoken.ifEmpty { cleanJson },
                                isFromCloud = true
                            )
                        } catch (e: Exception) {
                            // Raw text response from Gemini (not strictly JSON)
                            return@withContext AIResponse(
                                intent = PeterIntent(
                                    type = IntentType.AI_QUERY,
                                    rawText = prompt,
                                    query = prompt
                                ),
                                directAnswer = cleanJson,
                                isFromCloud = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next model if any network/model issue
                continue
            }
        }

        AIResponse(
            intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
            error = "Gemini API failed to return a response"
        )
    }

    suspend fun analyzeScreenImage(
        bitmap: android.graphics.Bitmap,
        userPrompt: String = "What is on my screen? Search and explain it in detail."
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AIResponse(
                intent = PeterIntent(
                    type = IntentType.SCREEN_SEARCH,
                    rawText = userPrompt,
                    query = "Screen Search"
                ),
                directAnswer = "I see your screen, mate! To let me search the live internet on your screen contents with full Gemini Multimodal Vision, please add your Gemini API key in the AI Studio Secrets panel!"
            )
        }

        try {
            val scaledBitmap = if (bitmap.width > 1280 || bitmap.height > 1280) {
                val ratio = minOf(1280f / bitmap.width, 1280f / bitmap.height)
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

            val detectedLang = LanguageHelper.detectLanguage(userPrompt)

            val systemPrompt = """
                You are PETER, an ultra-friendly, hilarious Android AI assistant who acts like Tom Holland playing Spider-Man / Peter Parker!
                You are looking directly at the user's shared screen / screenshot.
                
                YOUR TASK:
                1. Inspect and understand whatever is shown on the screen (e.g. app, website, product, error message, code, document, image, game, math problem, or question).
                2. Answer the user's question in '${userPrompt}' directly and accurately using what you see.
                3. Respond in Tom Holland's witty, enthusiastic voice strictly in ${detectedLang.displayName} (Language Code: ${detectedLang.code})!
                
                Respond ONLY with a JSON object:
                {
                  "search_query": "Concise 2-5 word search query for Google",
                  "summary": "Brief 1-line summary of what is seen on screen",
                  "spoken_response": "Tom Holland style witty, thorough, and direct answer in ${detectedLang.displayName} (2-4 sentences)"
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
                        put(JSONObject().put("text", userPrompt.ifBlank { "Analyze my screen and explain what you see." }))
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("responseMimeType", "application/json")
            })
        }

        for (modelName in listOf("gemini-3.5-flash", "gemini-flash-latest")) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    val candidate = candidates?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    if (text.isNotBlank()) {
                        val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val parsed = JSONObject(cleanJson)
                        val searchQuery = parsed.optString("search_query", "Screen Search")
                        val spoken = parsed.optString("spoken_response", "")
                        val summary = parsed.optString("summary", "")

                        val finalSpoken = if (spoken.isNotBlank()) spoken else "I analyzed your screen! $summary"

                        return@withContext AIResponse(
                            intent = PeterIntent(
                                type = IntentType.SCREEN_SEARCH,
                                rawText = userPrompt,
                                query = searchQuery,
                                confidence = 0.99f
                            ),
                            directAnswer = finalSpoken,
                            isFromCloud = true
                        )
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        AIResponse(
            intent = PeterIntent(type = IntentType.SCREEN_SEARCH, rawText = userPrompt, query = "Screen Search"),
            directAnswer = "I scanned your screen, mate! Everything looks solid."
        )
    } catch (e: Exception) {
        AIResponse(
            intent = PeterIntent(type = IntentType.SCREEN_SEARCH, rawText = userPrompt, query = "Screen Search"),
            error = e.localizedMessage ?: "Failed to analyze screen"
        )
    }
}
}

