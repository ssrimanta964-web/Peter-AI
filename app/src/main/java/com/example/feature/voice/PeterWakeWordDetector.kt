package com.example.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var restartJob: Job? = null

    private val _isWakeWordActive = MutableStateFlow(false)
    val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

    private val wakePatterns = listOf(
        "hey peter", "hello peter", "hi peter", "ok peter", "okay peter",
        "hey piter", "hello piter", "hi piter", "namaste peter", "peter"
    )

    private fun isWakeWord(rawText: String): Boolean {
        val text = rawText.lowercase(Locale.ROOT).trim()
        return wakePatterns.any { pattern -> text.contains(pattern) }
    }

    fun startContinuousWakeWordListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        isListening = true
        _isWakeWordActive.value = true
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!isListening) return

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }

        val recognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isListening) {
                        scheduleRestart(400)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                    
                    if (isWakeWord(text)) {
                        onWakeWordDetected(text)
                    }

                    if (isListening) {
                        scheduleRestart(250)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                    
                    if (isWakeWord(text)) {
                        onWakeWordDetected(text)
                        stop()
                        scheduleRestart(1000)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            if (isListening) {
                scheduleRestart(1000)
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

    fun stop() {
        isListening = false
        _isWakeWordActive.value = false
        restartJob?.cancel()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
