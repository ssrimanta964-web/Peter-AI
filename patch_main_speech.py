with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_intent = """                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak command to P.E.T.E.R. (English, Hindi, Bengali)...")
                            }"""

new_intent = """                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak command to P.E.T.E.R. (English, Hindi, Bengali)...")
                                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
                                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
                                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
                            }"""

content = content.replace(old_intent, new_intent)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
