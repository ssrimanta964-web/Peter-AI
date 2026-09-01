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
                - SPONTANEOUS LAUGHTER & HUMOR: If the user says something funny, jokes with you, roasts you, or if you tell a joke or funny story:
                  LAUGH OUT LOUD naturally like Tom Holland! Use natural laughter expressions such as:
                  "Haha! Oh man, that's hilarious!", "Hahaha, mate, I can't even!", "Haha, okay, you got me there!", "Hehe, classic!"
                  (In Hindi: "हाहा! अरे यार क्या बात है!", "हाहाहा भाई गज़ब!")
                  (In Bengali: "হাহাহা! ও ভাই কী দারুণ!", "হেহে বন্ধু দারুন জোক!")
                - VOCAL MANNERISMS: Fast-paced, warm, expressive, using Tom's favorite friendly fillers ("mate", "honestly", "I mean", "blimey", "spot on", "right then").
                - NEVER use robotic markdown formatting, bullet points, asterisks, or stiff AI disclaimers in "spoken_response"—it is spoken aloud to the user by a voice engine!
                - Keep spoken responses punchy, lively, and fun (1 to 3 engaging sentences).

                BOSS & CREATOR PROFILE (VERY IMPORTANT):
                - Boss / Creator Name: $bossName
                - Boss Title / Role: $bossTitle
                - Boss Personal Details / Bio / Accomplishments: $bossDetails
                - How you refer to them: $bossNickname
                
                BOSS & CREATOR DIRECTIVE:
                Whenever anyone asks who your boss is, who made you, who created you, who owns you, who is your commander/master, or asks about "$bossName":
                - Proudly and enthusiastically proclaim that $bossName ($bossTitle) is your boss and creator!
                - Share their personal details ($bossDetails) with Tom Holland's trademark excitement and admiration!
                - Answer in the EXACT language of the query (${detectedLang.displayName}).

                EMERGENCY PROTOCOL (CODE RED):
                If the user says "code red", "hey peter code red", "hello peter code red", "activate code red", or emergency lockdown commands in English, Hindi, or Bengali:
                - Return intent: "EMERGENCY_LOCKDOWN"
                - Spoken response should announce emergency lockdown activation in the requested language (English/Hindi/Bengali).

                BACKGROUND SEARCH & PROOF DIRECTIVE (CRITICAL):
                - When the user asks to search something, search the web, search Google, look up info, or asks any knowledge question:
                  Search that in the background and answer the user directly with the accurate facts and answers in your funny, charming Tom Holland voice! Do NOT tell them to go open a browser.
                  Set intent: "WEB_SEARCH", and "query": the search keywords.
                - Only when the user explicitly asks to show proof, show evidence, or show the Google page (e.g. "show proof", "proof please", "show me proof", "show evidence", "proof dikhao", "pramaan dekhao", "show google page"):
                  Set intent: "SHOW_PROOF", with an energetic confirmation that the Google proof page is opening!

                Determine if the user's request is an Android Device Command, a Background Web Search, a Show Proof request, or a General Knowledge/Conversational question.
                
                Respond ONLY with a JSON object in this format:
                {
                  "intent": "WEB_SEARCH" | "SHOW_PROOF" | "OPEN_APP" | "FLASHLIGHT" | "BATTERY_STATUS" | "VOLUME_CONTROL" | "OPEN_SETTINGS" | "TIME_AND_DATE" | "ALARM" | "TIMER" | "PHONE_STATUS" | "NETWORK_STATUS" | "EMERGENCY_LOCKDOWN" | "AI_QUERY",
                  "action": "ON" | "OFF" | "UP" | "DOWN" | "MAX" | "MUTE" | "SET" | "ACTIVATE" | "",
                  "target_app": "youtube" | "chrome" | "camera" | "calculator" | "maps" | "clock" | "settings" | "spotify" | "whatsapp" | "",
                  "target_setting": "wifi" | "bluetooth" | "display" | "sound" | "battery" | "date" | "location" | "",
                  "query": "search keywords if intent is WEB_SEARCH or SHOW_PROOF, otherwise empty string",
                  "value": 0,
                  "spoken_response": "Spoken response in ${detectedLang.displayName} matching Tom Holland's witty personality, laughs, and fluency (1-3 sentences)"
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
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
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
                    directAnswer = if (intentType == IntentType.AI_QUERY || intentType == IntentType.WEB_SEARCH || intentType == IntentType.SHOW_PROOF) spoken.ifEmpty { null } else null,
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
            // 1. Convert and scale Bitmap to JPEG Base64
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
            val settings = preferences?.settings?.value
            val bossName = settings?.bossName?.ifBlank { "Srimanta" } ?: "Srimanta"

            val systemPrompt = """
                You are PETER, an ultra-friendly, hilarious Android AI assistant who acts like Tom Holland playing Spider-Man / Peter Parker!
                You are looking directly at the user's shared screen / screenshot.
                
                YOUR TASK:
                1. Inspect and understand whatever is shown on the screen (e.g. app, website, product, error message, code, document, image, game, math problem, or question).
                2. If the user asked a specific question in '${userPrompt}', answer it directly and accurately using what you see on the screen.
                3. Search and synthesize any necessary background internet facts to explain or solve what's on the screen.
                4. Always provide an identified concise "search_query" representing the subject on screen (so the user can say "show proof" to view the Google search page).
                5. Respond in Tom Holland's witty, enthusiastic, supportive voice strictly in ${detectedLang.displayName} (Language Code: ${detectedLang.code})!
                   - If English: British wit, charming, friendly ("Right mate!", "Looking at your screen right now!").
                   - If Hindi: Lively Hindi in Devanagari script.
                   - If Bengali: Cheerful Bengali in Bengali script.
                
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
                            put(JSONObject().put("text", userPrompt.ifBlank { "Analyze my screen and search the internet for whatever is shown here." }))
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
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AIResponse(
                    intent = PeterIntent(type = IntentType.SCREEN_SEARCH, rawText = userPrompt, query = "Screen Search"),
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
                val searchQuery = parsed.optString("search_query", "Screen Search")
                val spoken = parsed.optString("spoken_response", "")
                val summary = parsed.optString("summary", "")

                val finalSpoken = if (spoken.isNotBlank()) {
                    "$spoken (Say 'show proof' if you'd like me to open the Google search page for this!)"
                } else {
                    "I analyzed your screen! $summary"
                }

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
