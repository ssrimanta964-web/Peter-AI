package com.example.domain.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object DuckDuckGoFetcher {
    private val client = OkHttpClient()

    suspend fun search(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (response.isSuccessful) {
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText", "")
                if (abstractText.isNotBlank()) {
                    return@withContext abstractText
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
