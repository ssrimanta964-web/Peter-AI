with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'r') as f:
    content = f.read()

old_b = """                if (!response.isSuccessful) {
                    android.util.Log.e("GeminiAPI", "Failed request to $modelName: ${response.code} ${response.message} - Body: $responseBody")
                }"""

new_b = """                if (!response.isSuccessful) {
                    android.util.Log.e("GeminiAPI", "Failed request to $modelName: ${response.code} ${response.message} - Body: $responseBody")
                    
                    if (response.code == 401 || response.code == 403) {
                        return@withContext AIResponse(
                            intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                            error = "Invalid API Key",
                            directAnswer = "Boss, the Google Cloud servers rejected my API key. Please check your settings and make sure the key is copied completely and correctly!",
                            isFromCloud = false
                        )
                    }
                }"""

content = content.replace(old_b, new_b)

with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'w') as f:
    f.write(content)
