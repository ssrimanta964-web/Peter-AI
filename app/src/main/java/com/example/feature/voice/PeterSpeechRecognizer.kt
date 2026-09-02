package com.example.feature.voice

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

class PeterSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChange: (Boolean) -> Unit
) {
    companion object {
        private const val TAG = "PeterSpeechRecognizer"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var retryCount = 0
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun startListening(preferredLanguage: String = "Auto Detect (English, Hindi, Bengali)", isRetry: Boolean = false) {
        mainHandler.post {
            try {
                stopListeningInternal()
                if (!isRetry) retryCount = 0

                val isOnline = isNetworkAvailable()

                // Create recognizer on Main thread
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
                }

                if (speechRecognizer == null) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    onStateChange(false)
                    onError("Unable to initialize speech recognizer service.")
                    return@post
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech: microphone open")
                        _isListening.value = true
                        onStateChange(true)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech: speech detected")
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS dB typically ranging from -2 to 10
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        onStateChange(false)
                    }

                    override fun onError(error: Int) {
                        Log.e(TAG, "SpeechRecognizer onError: code $error")
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        onStateChange(false)

                        if ((error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == 11) && retryCount < 2) {
                            retryCount++
                            Log.w(TAG, "Recognizer busy. Scheduling retry $retryCount...")
                            _isListening.value = false
                            onStateChange(false)
                            
                            // Try again after 600ms
                            scope.launch {
                                kotlinx.coroutines.delay(600)
                                startListening(preferredLanguage, isRetry = true)
                            }
                            return
                        }

                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Voice input cancelled or client error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network offline: please connect to internet or use text prompt."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap microphone to try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service was busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Voice recognition server error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap microphone to speak."
                            11 -> "Speech server disconnected. Please try again."
                            12 -> "Language not supported by voice recognizer."
                            else -> "Speech error (Code $error)"
                        }

                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        onStateChange(false)

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d(TAG, "SpeechRecognizer onResults: $matches")
                        val recognized = matches?.firstOrNull { it.isNotBlank() }

                        if (!recognized.isNullOrBlank()) {
                            onResult(recognized)
                        } else {
                            onError("No speech recognized. Tap microphone to speak.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            Log.d(TAG, "Live partial speech: $partial")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val langTag = when {
                    preferredLanguage.contains("Hindi", ignoreCase = true) -> "hi-IN"
                    preferredLanguage.contains("Bengali", ignoreCase = true) -> "bn-IN"
                    preferredLanguage.contains("English", ignoreCase = true) -> "en-US"
                    else -> Locale.getDefault().toLanguageTag()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", arrayOf("en-US", "hi-IN", "bn-IN", "en-GB"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _isListening.value = false
                _rmsLevel.value = 0f
                onStateChange(false)
                onError(e.localizedMessage ?: "Failed to open microphone.")
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speech recognizer", e)
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            _rmsLevel.value = 0f
            onStateChange(false)
            stopListeningInternal()
        }
    }

    fun cancel() {
        mainHandler.post {
            _isListening.value = false
            _rmsLevel.value = 0f
            onStateChange(false)
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling speech recognizer", e)
            }
        }
    }
}

