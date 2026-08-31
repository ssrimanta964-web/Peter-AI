package com.example.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class PeterTextToSpeech(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private var pendingSpeechText: String? = null
    private var targetRate: Float = 1.0f
    private var targetPitch: Float = 1.0f
    private var targetVoiceName: String = "Default"

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            
            // Query available voices
            val voices = tts?.voices?.map { it.name } ?: emptyList()
            _availableVoices.value = listOf("Default") + voices

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged(false)
                }
            })

            pendingSpeechText?.let { text ->
                speak(text, targetRate, targetPitch, targetVoiceName)
                pendingSpeechText = null
            }
        } else {
            isInitialized = false
        }
    }

    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f, voiceName: String = "Default") {
        targetRate = rate
        targetPitch = pitch
        targetVoiceName = voiceName

        if (!isInitialized) {
            pendingSpeechText = text
            return
        }

        tts?.let { engine ->
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)

            if (voiceName != "Default") {
                val matchedVoice = engine.voices?.firstOrNull { it.name == voiceName }
                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }

            val utteranceId = "PETER_TTS_${System.currentTimeMillis()}"
            // Queue mode QUEUE_FLUSH stops previous speech to prevent overlapping
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        onSpeakingStateChanged(false)
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
