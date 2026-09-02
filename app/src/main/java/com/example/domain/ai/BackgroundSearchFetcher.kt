package com.example.domain.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object BackgroundSearchFetcher {
    private val client = OkHttpClient()

    suspend fun search(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            
            // 1. Try DuckDuckGo
            val ddgRequest = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1")
                .build()
            
            val ddgResponse = client.newCall(ddgRequest).execute()
            val ddgBody = ddgResponse.body?.string() ?: ""
            if (ddgResponse.isSuccessful && ddgBody.isNotBlank()) {
                val json = JSONObject(ddgBody)
                val abstractText = json.optString("AbstractText", "")
                if (abstractText.isNotBlank()) {
                    return@withContext abstractText
                }
            }

            // 2. Try Wikipedia
            val wikiRequest = Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=$encodedQuery&format=json")
                .build()
                
            val wikiResponse = client.newCall(wikiRequest).execute()
            val wikiBody = wikiResponse.body?.string() ?: ""
            if (wikiResponse.isSuccessful && wikiBody.isNotBlank()) {
                val json = JSONObject(wikiBody)
                val pages = json.optJSONObject("query")?.optJSONObject("pages")
                if (pages != null) {
                    val firstKey = pages.keys().next()
                    if (firstKey != "-1") {
                        val extract = pages.optJSONObject(firstKey)?.optString("extract", "")
                        if (!extract.isNullOrBlank()) {
                            // Trim to first 2 sentences for conciseness
                            val sentences = extract.split(Regex("(?<=[.!?])\\s+"))
                            return@withContext sentences.take(2).joinToString(" ")
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
