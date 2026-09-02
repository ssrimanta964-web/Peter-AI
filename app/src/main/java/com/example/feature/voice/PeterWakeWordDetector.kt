package com.example.feature.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class PeterWakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    companion object {
        private const val TAG = "PeterWakeWordDetector"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var restartJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isWakeWordActive = MutableStateFlow(false)
    val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

    private var lastTriggerTimestamp = 0L

    private val wakePatterns = listOf(
        // English & Transliterated
        "hey peter", "hello peter", "hi peter", "ok peter", "okay peter",
        "hey piter", "hello piter", "hi piter", "namaste peter", "peter",
        "piter", "pete", "hey pete", "hello pete", "hi pete",
        "hay peter", "he peter", "hai peter", "hey spider-man", "hey spiderman",
        "spiderman", "spider-man", "yo peter", "listen peter", "are you there peter",
        "peter hear me", "peter parker", "hey spider", "hey pita", "ok pita",
        "hey feature", "hey heater", "hey better", "a peter", "the peter",

        // Hindi Devanagari & Hinglish
        "हे पीटर", "नमस्ते पीटर", "सुनो पीटर", "पीटर", "हेय पीटर", "हेलो पीटर",
        "हाय पीटर", "ओके पीटर", "स्पाइडर मैन", "स्पाइडरमैन", "hey pitar", "namaste pitar",

        // Bengali & Banglish
        "হেই পিটার", "হ্যালো পিটার", "পিটার", "শোনো পিটার", "নমস্কার পিটার", "হাই পিটার",
        "ওকে পিটার", "স্পাইডারম্যান", "পিটার শোনো"
    )

    private val wakeRegex = Regex(
        "\\b(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|हे|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spider-man|spiderman|পিটার|पीटर)\\b",
        RegexOption.IGNORE_CASE
    )

    fun isWakeWord(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        val text = rawText.lowercase(Locale.ROOT).trim()

        // Direct pattern check
        if (wakePatterns.any { pattern -> text.contains(pattern) }) {
            return true
        }

        // Regex pattern check
        if (wakeRegex.containsMatchIn(text)) {
            return true
        }

        // Token starts-with check (e.g. "peter", "piter", "spiderman", "পিটার", "पीटर")
        val tokens = text.split(Regex("[\\s,?.!]+"))
        return tokens.any { token ->
            token == "peter" || token == "piter" || token == "pete" ||
            token == "spiderman" || token == "spider-man" ||
            token == "পিটার" || token == "पीटर" ||
            token.startsWith("peter") || token.startsWith("piter")
        }
    }

    fun startContinuousWakeWordListening() {
        mainHandler.post {
            isListening = true
            _isWakeWordActive.value = true
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!isListening) return

        // 1. Verify Microphone Permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Audio permission not granted yet. Waiting to initialize wake word.")
            scheduleRestart(2000L)
            return
        }

        // 2. Clean up previous instance
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up previous recognizer: ${e.localizedMessage}")
        }

        // 3. Create Speech Recognizer
        try {
            speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                try {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (e: Exception) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        Log.d(TAG, "WakeWord recognizer error code: $error")
                        if (isListening) {
                            val delayMs = when (error) {
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_AUDIO -> 700L
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 120L
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 2500L
                                else -> 300L
                            }
                            scheduleRestart(delayMs)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        Log.d(TAG, "WakeWord onResults candidates: $matches")

                        val matchedPhrase = matches.firstOrNull { isWakeWord(it) } ?: matches.firstOrNull() ?: ""
                        val now = System.currentTimeMillis()

                        if (isWakeWord(matchedPhrase) && now - lastTriggerTimestamp > 1800L) {
                            lastTriggerTimestamp = now
                            onWakeWordDetected(matchedPhrase)
                        }

                        if (isListening) {
                            scheduleRestart(200L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        val matchedPhrase = matches.firstOrNull { isWakeWord(it) } ?: ""

                        val now = System.currentTimeMillis()
                        if (matchedPhrase.isNotBlank() && isWakeWord(matchedPhrase) && now - lastTriggerTimestamp > 1800L) {
                            lastTriggerTimestamp = now
                            Log.d(TAG, "WakeWord triggered onPartialResults: $matchedPhrase")
                            onWakeWordDetected(matchedPhrase)
                            stopListeningInternal()
                            scheduleRestart(1200L)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", arrayOf("en-US", "hi-IN", "bn-IN", "en-GB"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.localizedMessage}")
            if (isListening) {
                scheduleRestart(1000L)
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isListening) {
                initRecognizer()
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        isListening = false
        _isWakeWordActive.value = false
        restartJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

