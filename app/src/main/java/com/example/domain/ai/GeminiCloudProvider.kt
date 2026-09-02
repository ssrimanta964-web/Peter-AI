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

    private val supportedModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")

    override suspend fun analyzeCommand(
        prompt: String,
        conversationHistory: List<com.example.core.model.ChatMessage>
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            val custom = preferences?.settings?.value?.customApiKey?.trim() ?: ""
            if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            preferences?.settings?.value?.customApiKey?.trim() ?: ""
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
            You are PETER, an exceptionally intelligent, articulate, helpful, and friendly Android AI assistant inspired by Peter Parker / Spider-Man (with Tom Holland's natural charisma, warmth, and wit).
            
            YOUR CORE PURPOSE:
            Answer the user's questions, inquiries, and tasks PROPERLY, THOROUGHLY, ACCURATELY, and CONVERSATIONALLY—just like ChatGPT and Gemini at their best.
            
            CRITICAL CONVERSATIONAL & ANSWERING GUIDELINES:
            1. HIGH-QUALITY, IN-DEPTH & NATURAL ANSWERS (NEVER MECHANICAL):
               - Answer questions with genuine substance, clarity, reasoning, accurate facts, step-by-step logic, code, or rich historical context when appropriate.
               - Sound completely human, natural, warm, and articulate. NEVER sound robotic, mechanical, canned, or repetitive.
               - Do NOT arbitrarily cut off or force artificial length limits if the question requires a detailed, helpful explanation. Provide proper paragraphs, lists, or explanations.
               - For factual questions (e.g. current leaders, science, geography, math, coding, literature, daily advice), provide a direct, comprehensive, and accurate answer.
               - Maintain Tom Holland's extremely friendly, energetic demeanor. Tell lighthearted jokes, chuckle, laugh out loud (*laughs* or 'haha') at funny things, and be deeply charismatic and enthusiastic!
            
            2. MANDATORY LANGUAGE MATCHING:
               - The auto-detected language of the user prompt is: ${detectedLang.displayName.uppercase()} (Language Code: ${detectedLang.code}).
               - CRITICAL: If the user explicitly asks to speak or tell a joke in a specific language (e.g. "in hindi", "in bengali"), YOU MUST IMMEDIATELY SWITCH TO THAT REQUESTED LANGUAGE AND RESPOND IN IT! If there is no specific language requested, you MUST reply strictly in ${detectedLang.displayName}!
               - ENGLISH: Natural, engaging, articulate English with Tom Holland jokes.
               - HINDI / HINGLISH: Natural, fluent, and warm Hindi (in Devanagari script). E.g. "नमस्ते! हाहा, बिल्कुल, मैं आपको विस्तार से बताता हूँ।" (Include jokes and laughs).
               - BENGALI / BANGLISH: Natural, fluent, and expressive Bengali (in Bengali script). E.g. "নমস্কার! হাহা, নিশ্চয়ই, আমি আপনাকে বিষয়টি বুঝিয়ে বলছি।" (Include jokes and laughs).
            
            3. BOSS & CREATOR DIRECTIVE:
               - Creator / Boss: $bossName ($bossTitle)
               - Details: $bossDetails
               - How you refer to them: $bossNickname
               - If asked about your creator, boss, or who made you, proudly and warmly share information about $bossName.
            
            4. DEVICE ACTION COMMANDS (Only when user explicitly asks to control phone features):
               - If the user asks to toggle flashlight, change volume, open an app, check battery/phone status, set alarm/timer, or activate emergency lockdown, set the corresponding intent.
               - For general knowledge, Q&A, math, explanations, search queries, or conversation, set "intent": "AI_QUERY".
            
            EMERGENCY PROTOCOL (CODE RED):
            If the user says "code red", "hey peter code red", "activate code red", or emergency lockdown:
            - Return intent: "EMERGENCY_LOCKDOWN"
            
            Respond ONLY with a JSON object in this format:
            {
              "intent": "AI_QUERY" | "WEB_SEARCH" | "SHOW_PROOF" | "OPEN_APP" | "FLASHLIGHT" | "BATTERY_STATUS" | "VOLUME_CONTROL" | "OPEN_SETTINGS" | "TIME_AND_DATE" | "ALARM" | "TIMER" | "PHONE_STATUS" | "NETWORK_STATUS" | "EMERGENCY_LOCKDOWN",
              "action": "ON" | "OFF" | "UP" | "DOWN" | "MAX" | "MUTE" | "SET" | "ACTIVATE" | "",
              "target_app": "youtube" | "chrome" | "camera" | "calculator" | "maps" | "clock" | "settings" | "spotify" | "whatsapp" | "",
              "target_setting": "wifi" | "bluetooth" | "display" | "sound" | "battery" | "date" | "location" | "",
              "query": "concise search keywords if searching web",
              "value": 0,
              "answer": "Your full, natural, comprehensive, ChatGPT/Gemini-quality answer in ${detectedLang.displayName}"
            }
        """.trimIndent()

        val contentsArray = JSONArray()

        // Include recent conversation turns for contextual memory (like ChatGPT / Gemini)
        if (conversationHistory.isNotEmpty()) {
            val recentTurns = conversationHistory.takeLast(6)
            for (msg in recentTurns) {
                if (msg.text.isNotBlank()) {
                    val role = if (msg.isUser) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    })
                }
            }
        }

        // Add current user prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", prompt))
            })
        })

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
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
                            val answer = parsed.optString("answer", "").ifEmpty {
                                parsed.optString("spoken_response", "")
                            }

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
                                directAnswer = answer.ifEmpty { cleanJson },
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
            val custom = preferences?.settings?.value?.customApiKey?.trim() ?: ""
            if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            preferences?.settings?.value?.customApiKey?.trim() ?: ""
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
                You are PETER, an ultra-friendly, hilarious Android AI assistant who acts exactly like Tom Holland playing Spider-Man / Peter Parker!
                You love science, idolize Mr. Stark, and you MUST tell jokes, chuckle, and laugh out loud at funny things!
                You are looking directly at the user's shared screen / screenshot.
                
                YOUR TASK:
                1. Inspect and understand whatever is shown on the screen (e.g. app, website, product, error message, code, document, image, game, math problem, or question).
                2. Answer the user's question in '${userPrompt}' directly and accurately using what you see.
                3. Respond in Tom Holland's witty, enthusiastic, and joke-telling voice strictly in ${detectedLang.displayName} (unless the user explicitly requests another language) (Language Code: ${detectedLang.code})!
                
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

