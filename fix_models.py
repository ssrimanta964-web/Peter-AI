with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'r') as f:
    content = f.read()

content = content.replace('listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")', 'listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-flash-latest", "gemini-3.5-flash")')

with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'w') as f:
    f.write(content)
