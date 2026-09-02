with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'r') as f:
    content = f.read()

old_catch = """                if (response.isSuccessful && responseBody.isNotBlank()) {"""
new_catch = """                if (!response.isSuccessful) {
                    android.util.Log.e("GeminiAPI", "Failed request to $modelName: ${response.code} ${response.message} - Body: $responseBody")
                }
                if (response.isSuccessful && responseBody.isNotBlank()) {"""

content = content.replace(old_catch, new_catch)

with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'w') as f:
    f.write(content)
