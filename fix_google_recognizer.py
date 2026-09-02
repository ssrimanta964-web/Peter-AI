import re

with open('app/src/main/java/com/example/feature/voice/PeterSpeechRecognizer.kt', 'r') as f:
    content = f.read()

old_create = """                // Create recognizer on Main thread
                speechRecognizer = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isOnline && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed creating preferred recognizer, fallback to default", e)
                    SpeechRecognizer.createSpeechRecognizer(context)
                }"""

new_create = """                // Create recognizer on Main thread
                val googleComponent = android.content.ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
                )
                
                speechRecognizer = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isOnline && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context, googleComponent)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed creating Google recognizer, fallback to default", e)
                    try {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    } catch (e2: Exception) {
                        null
                    }
                }"""

content = content.replace(old_create, new_create)

with open('app/src/main/java/com/example/feature/voice/PeterSpeechRecognizer.kt', 'w') as f:
    f.write(content)
