with open('app/src/main/java/com/example/service/PeterWakeWordService.kt', 'r') as f:
    content = f.read()

old_bcast = """                // Broadcast to MainActivity if it's alive to handle UI transition
                val broadcastIntent = Intent("com.example.WAKE_WORD_DETECTED").apply {
                    putExtra("detectedText", detectedText)
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)

                val stripped = detectedText
                    .replace(Regex("(?i)^(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|হে|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spiderman|spider-man|পিটার|पीटर)\\s*"), "")
                    .trim()

                if (stripped.isBlank() || stripped.length <= 2) {
                    val uiIntent = Intent(this@PeterWakeWordService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        val pendingIntent = PendingIntent.getActivity(this@PeterWakeWordService, 0, uiIntent, PendingIntent.FLAG_IMMUTABLE)
                        pendingIntent.send()
                    } catch (e: Exception) {
                        startActivity(uiIntent)
                    }
                    return@launch
                }

                val result = commandRouter?.routeAndExecute(detectedText)
                if (result != null) {"""

new_bcast = """                // Check if MainActivity is in foreground
                val isAppOpen = com.example.MainActivity.isForeground
                
                if (isAppOpen) {
                    // Let MainActivity handle it so the UI updates natively
                    val broadcastIntent = Intent("com.example.WAKE_WORD_DETECTED").apply {
                        putExtra("detectedText", detectedText)
                        setPackage(packageName)
                    }
                    sendBroadcast(broadcastIntent)
                    return@launch
                }

                // If app is in background, execute it here
                val stripped = detectedText
                    .replace(Regex("(?i)^(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|হে|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spiderman|spider-man|পিটার|पीटर)\\s*"), "")
                    .trim()

                if (stripped.isBlank() || stripped.length <= 2) {
                    val uiIntent = Intent(this@PeterWakeWordService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        val pendingIntent = PendingIntent.getActivity(this@PeterWakeWordService, 0, uiIntent, PendingIntent.FLAG_IMMUTABLE)
                        pendingIntent.send()
                    } catch (e: Exception) {
                        startActivity(uiIntent)
                    }
                    return@launch
                }

                val result = commandRouter?.routeAndExecute(detectedText)
                if (result != null) {"""

content = content.replace(old_bcast, new_bcast)

with open('app/src/main/java/com/example/service/PeterWakeWordService.kt', 'w') as f:
    f.write(content)
