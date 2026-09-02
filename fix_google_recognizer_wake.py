with open('app/src/main/java/com/example/feature/voice/PeterWakeWordDetector.kt', 'r') as f:
    content = f.read()

old_create = """        try {
            speechRecognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else null
            }"""

new_create = """        try {
            val googleComponent = android.content.ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
            )
            speechRecognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context, googleComponent)
            } catch (e: Exception) {
                try {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } catch (e2: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else null
                }
            }"""

content = content.replace(old_create, new_create)

with open('app/src/main/java/com/example/feature/voice/PeterWakeWordDetector.kt', 'w') as f:
    f.write(content)
