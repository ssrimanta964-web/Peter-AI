package com.example

import com.example.domain.ai.GeminiCloudProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestGemini {
    @Test
    fun testCloudProvider() = runBlocking {
        val provider = GeminiCloudProvider()
        val response = provider.analyzeCommand("Hello Peter", emptyList())
        println("RESPONSE_ERROR: " + response.error)
        println("RESPONSE_ANSWER: " + response.directAnswer)
    }
}
