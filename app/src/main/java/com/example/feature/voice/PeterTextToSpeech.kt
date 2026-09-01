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
    private var targetRate: Float = 1.06f
    private var targetPitch: Float = 1.10f
    private var targetVoiceName: String = "Tom Holland Male (Default)"

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                tts?.setLanguage(Locale.US)
                isInitialized = true
            }
            
            // Query available voices and prioritize young British male (Tom Holland accent)
            val allVoices = tts?.voices?.toList() ?: emptyList()
            val maleVoices = allVoices.filter { isMaleVoice(it) }.sortedByDescending { voiceScore(it) }.map { it.name }
            val otherVoices = allVoices.filter { !isMaleVoice(it) }.map { it.name }

            _availableVoices.value = listOf("Tom Holland Male (Default)") + maleVoices + otherVoices

            // Apply best Tom Holland-like voice initially
            applyMaleVoice()

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

    private fun detectLanguage(text: String): String {
        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) return "hi" // Devanagari (Hindi)
            if (code in 0x0980..0x09FF) return "bn" // Bengali (Bangla)
        }
        return "en"
    }

    /**
     * Cleans text and transforms laughter cues (*laughs*, *chuckles*, haha) into
     * expressive, natural human phonetic pauses and laughs for fluent TTS delivery.
     */
    private fun preprocessTextForNaturalSpeech(rawText: String): String {
        var text = rawText
            // Clean markdown syntax that causes mechanical pronunciation
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`+([^`]+)`+"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("[-•]\\s+"), "")
            // Transform asterisks / bracketed action descriptions into natural laughter
            .replace(Regex("(?i)\\*(laughs|chuckles|giggles|laughs out loud|snickers)\\*"), "Haha!")
            .replace(Regex("(?i)\\((laughs|chuckles|giggles|snickers)\\)"), "Haha!")
            .replace(Regex("(?i)\\[(laughs|chuckles|giggles)\\]"), "Haha!")
            // Enhance expressiveness of laughs
            .replace(Regex("(?i)\\bhaha+\\b"), "Haha!")
            .replace(Regex("(?i)\\bhehe+\\b"), "Hehe!")
            .replace(Regex("(?i)\\blol\\b"), "Haha!")
            .replace(Regex("(?i)\\blmao\\b"), "Haha, oh man!")
            .replace(Regex("\\s+"), " ")
            .trim()

        return text
    }

    private fun applyVoiceForLanguage(langCode: String) {
        tts?.let { engine ->
            val allVoices = engine.voices ?: return
            when (langCode) {
                "hi" -> {
                    engine.setLanguage(Locale.Builder().setLanguage("hi").setRegion("IN").build())
                    val hiVoice = allVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.language == "hi" || n.contains("hi-in") || n.contains("hin")) &&
                                (n.contains("male") || n.contains("-m-") || !n.contains("female"))
                    } ?: allVoices.firstOrNull { it.locale.language == "hi" }
                    if (hiVoice != null) engine.voice = hiVoice
                }
                "bn" -> {
                    engine.setLanguage(Locale.Builder().setLanguage("bn").setRegion("IN").build())
                    val bnVoice = allVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.language == "bn" || n.contains("bn-in") || n.contains("bn-bd") || n.contains("ben")) &&
                                (n.contains("male") || n.contains("-m-") || !n.contains("female"))
                    } ?: allVoices.firstOrNull { it.locale.language == "bn" }
                    if (bnVoice != null) engine.voice = bnVoice
                }
                else -> {
                    engine.setLanguage(Locale.UK)
                    applyMaleVoice()
                }
            }
        }
    }

    private fun voiceScore(voice: Voice): Int {
        val name = voice.name.lowercase(Locale.ROOT)
        var score = 0
        // Tom Holland British natural tone prioritization (UK English young male)
        if (voice.locale.country.equals("GB", ignoreCase = true) || voice.locale.language.equals("en_GB", ignoreCase = true) || name.contains("en-gb")) score += 70
        if (name.contains("en-gb-x-rjs") || name.contains("en-gb-x-gbd") || name.contains("en-gb-x-gbb") || name.contains("en-gb-language")) score += 50
        if (name.contains("neural") || name.contains("wavenet") || name.contains("natural") || name.contains("high-quality")) score += 40
        if (name.contains("male") || name.contains("-m-")) score += 30
        if (name.contains("en-us-x-iol") || name.contains("en-us-x-tpd")) score += 20
        if (voice.quality == Voice.QUALITY_VERY_HIGH || voice.quality == Voice.QUALITY_HIGH) score += 25
        if (voice.latency == Voice.LATENCY_VERY_LOW || voice.latency == Voice.LATENCY_LOW) score += 10
        return score
    }

    private fun isMaleVoice(voice: Voice): Boolean {
        val name = voice.name.lowercase(Locale.ROOT)
        // Check for common Android / Google TTS female identifiers
        val isExplicitFemale = name.contains("female") || name.contains("-f-") || name.contains("woman") || 
                name.contains("en-us-x-tpf") || name.contains("en-us-x-iob") || name.contains("en-us-x-sfg#female") ||
                name.contains("en-gb-x-gba") || name.contains("en-gb-x-gbc") || name.contains("en-gb-x-fis")
        if (isExplicitFemale) return false

        val isExplicitMale = name.contains("male") || name.contains("-m-") || name.contains("man") || 
                name.contains("en-us-x-iol") || name.contains("en-us-x-tpd") || name.contains("en-gb-x-rjs") || 
                name.contains("en-gb-x-gbd") || name.contains("en-gb-x-gbb") || name.contains("en-in-x-cfl") || name.contains("en-au-x-aub")
        
        return isExplicitMale || (!isExplicitFemale && voice.locale.language == "en")
    }

    private fun applyMaleVoice() {
        tts?.let { engine ->
            val allVoices = engine.voices ?: return
            // Find highest scored young male voice (UK / British young male prioritized for Tom Holland)
            val bestVoice = allVoices.filter { isMaleVoice(it) }.maxByOrNull { voiceScore(it) }
                ?: allVoices.firstOrNull { isMaleVoice(it) }

            if (bestVoice != null) {
                engine.voice = bestVoice
            }
        }
    }

    fun speak(text: String, rate: Float = 1.06f, pitch: Float = 1.10f, voiceName: String = "Tom Holland Male (Default)") {
        targetRate = rate
        targetPitch = pitch
        targetVoiceName = voiceName

        if (!isInitialized) {
            pendingSpeechText = text
            return
        }

        tts?.let { engine ->
            val processedText = preprocessTextForNaturalSpeech(text)

            // Dynamic Pitch Modulation for Laughter & Excitement (Tom Holland higher energetic inflection)
            val containsHumorOrLaugh = processedText.contains("Haha", ignoreCase = true) ||
                    processedText.contains("Hehe", ignoreCase = true) ||
                    processedText.contains("!", ignoreCase = true)
            
            val adjustedPitch = if (containsHumorOrLaugh) (pitch * 1.04f).coerceAtMost(1.35f) else pitch
            val adjustedRate = if (containsHumorOrLaugh) (rate * 1.02f).coerceAtMost(1.30f) else rate

            engine.setSpeechRate(adjustedRate)
            engine.setPitch(adjustedPitch)

            val detectedLang = detectLanguage(processedText)
            if (voiceName != "Tom Holland Male (Default)" && voiceName != "Spider-Man Male (Default)" && voiceName != "Default") {
                val matchedVoice = engine.voices?.firstOrNull { it.name == voiceName }
                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                } else {
                    applyVoiceForLanguage(detectedLang)
                }
            } else {
                applyVoiceForLanguage(detectedLang)
            }

            val utteranceId = "PETER_TTS_${System.currentTimeMillis()}"
            // Queue mode QUEUE_FLUSH stops previous speech to prevent overlapping and ensure smooth conversational flow
            engine.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
