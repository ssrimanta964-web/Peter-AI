with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'r') as f:
    content = f.read()

old_listen = """    fun startListening() {
        tts?.stop()

        val pauseIntent = android.content.Intent("com.example.ACTION_PAUSE_WAKE_WORD").apply { setPackage(getApplication<android.app.Application>().packageName) }
        getApplication<android.app.Application>().sendBroadcast(pauseIntent)

        speechRecognizer?.cancel() // Clear any busy state
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200) // Delay to let hardware mic release
            speechRecognizer?.startListening(settings.value.preferredLanguage)
        }
    }"""

new_listen = """    fun startListening(greeting: String? = null) {
        tts?.stop()
        
        if (greeting != null) {
            tts?.speak(greeting, settings.value.speechRate, settings.value.speechPitch, settings.value.voiceName)
        }

        val pauseIntent = android.content.Intent("com.example.ACTION_PAUSE_WAKE_WORD").apply { setPackage(getApplication<android.app.Application>().packageName) }
        getApplication<android.app.Application>().sendBroadcast(pauseIntent)

        speechRecognizer?.cancel() // Clear any busy state
        viewModelScope.launch {
            if (greeting != null) {
                kotlinx.coroutines.delay(1500) // Give TTS time to speak
            } else {
                kotlinx.coroutines.delay(1200) // Normal mic release delay
            }
            speechRecognizer?.startListening(settings.value.preferredLanguage)
        }
    }"""

content = content.replace(old_listen, new_listen)

with open('app/src/main/java/com/example/ui/PeterViewModel.kt', 'w') as f:
    f.write(content)
