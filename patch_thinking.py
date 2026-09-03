with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'r') as f:
    content = f.read()

old_models = """private val supportedModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-flash-latest", "gemini-3.5-flash")"""
new_models = """private val supportedModels = listOf("gemini-3.5-flash", "gemini-3.6-flash", "gemini-flash-latest")"""

content = content.replace(old_models, new_models)

with open('app/src/main/java/com/example/domain/ai/GeminiCloudProvider.kt', 'w') as f:
    f.write(content)
