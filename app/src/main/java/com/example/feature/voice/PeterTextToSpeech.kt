package com.example.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.local.PeterPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class PeterTextToSpeech(
    private val context: Context,
    private val preferences: PeterPreferences? = null,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "PeterTTS"
        const val DEFAULT_TOM_HOLLAND_VOICE = "Tom Holland Male (British Young Hero)"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private var pendingSpeechText: String? = null
    private var targetRate: Float = 1.10f
    private var targetPitch: Float = 1.12f
    private var targetVoiceName: String = DEFAULT_TOM_HOLLAND_VOICE

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val geminiSynthesizer = GeminiSpeechSynthesizer(context, preferences) { speaking ->
        _isSpeaking.value = speaking
        onSpeakingStateChanged(speaking)
    }

    init {
        // Prefer Google TTS engine for highest quality natural neural male voices, fallback to default
        try {
            tts = TextToSpeech(context.applicationContext, this, "com.google.android.tts")
        } catch (e: Exception) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    private var currentOnDoneCallback: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                tts?.setLanguage(Locale.US)
                isInitialized = true
            }
            
            // Query available voices and strictly prioritize Tom Holland British young male voices
            val allVoices = tts?.voices?.toList() ?: emptyList()
            val maleVoices = allVoices.filter { isMaleVoice(it) }.sortedByDescending { tomHollandScore(it) }
            val otherVoices = allVoices.filter { !isMaleVoice(it) }

            val voiceList = mutableListOf(
                DEFAULT_TOM_HOLLAND_VOICE,
                "⚡ Gemini Neural Studio (Puck - Lifelike Tom Holland)",
                "Tom Holland Male (British Fast & Energetic)"
            )
            maleVoices.forEach { voiceList.add(it.name) }
            otherVoices.forEach { voiceList.add(it.name) }
            _availableVoices.value = voiceList

            // Apply best Tom Holland-like voice immediately
            applyTomHollandMaleVoice()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged(false)
                    val callback = currentOnDoneCallback
                    currentOnDoneCallback = null
                    callback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged(false)
                    val callback = currentOnDoneCallback
                    currentOnDoneCallback = null
                    callback?.invoke()
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
     * Cleans text and transforms conversational cues (*laughs*, *chuckles*, haha) into
     * expressive, natural human phonetic pauses and laughs for authentic Tom Holland delivery.
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
            // Transform action descriptions into lively conversational fillers
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

    private fun isExplicitlyFemale(voice: Voice): Boolean {
        val name = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) } ?: emptyList()

        if (features.any { it.contains("female") || it.contains("gender=2") || it.contains("gender=female") }) return true
        if (name.contains("female") || name.contains("#female") || name.contains("-f-") || name.contains("_female") || name.contains("woman")) return true

        // Known Android Google TTS & Samsung female voice identifiers
        val femaleCodes = listOf(
            "en-gb-x-gba", "en-gb-x-gbc", "en-gb-x-gbe", "en-gb-x-gbf", "en-gb-x-gbg", "en-gb-x-fis",
            "en-us-x-tpf", "en-us-x-iob", "en-us-x-iog", "en-us-x-tpe", "en-us-x-iof", "en-us-x-sfg",
            "en-au-x-aua", "en-au-x-auc",
            "en-in-x-cfa", "en-in-x-cfb",
            "hi-in-x-hia", "hi-in-x-hib", "hi-in-x-cfa", "hi-in-x-cfb",
            "bn-in-x-bna", "bn-in-x-bnb", "bn-bd-x-bda", "bn-bd-x-bdb"
        )
        return femaleCodes.any { name.contains(it) }
    }

    private fun isMaleVoice(voice: Voice): Boolean {
        if (isExplicitlyFemale(voice)) return false
        val name = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) } ?: emptyList()

        if (features.any { it.contains("male") || it.contains("gender=1") || it.contains("gender=male") }) return true
        if (name.contains("male") || name.contains("#male") || name.contains("-m-") || name.contains("_male") || name.contains("man")) return true

        // Known Android Google TTS male voice identifiers
        val maleCodes = listOf(
            "en-gb-x-rjs", "en-gb-x-gbd", "en-gb-x-gbb", "en-gb-x-rpj", "en-gb-x-rpk",
            "en-us-x-iol", "en-us-x-tpd", "en-us-x-iom", "en-us-x-tpc",
            "en-au-x-aub", "en-au-x-aud",
            "en-in-x-cfl", "en-in-x-cfd",
            "hi-in-x-hie", "hi-in-x-hid", "hi-in-x-hic", "hi-in-x-cfc", "hi-in-x-cfd",
            "bn-in-x-bnc", "bn-in-x-bnd", "bn-bd-x-ban"
        )
        return maleCodes.any { name.contains(it) }
    }

    private fun tomHollandScore(voice: Voice): Int {
        if (isExplicitlyFemale(voice)) return -1000
        val name = voice.name.lowercase(Locale.ROOT)
        var score = 0

        // #1 Top priority: British young male voices (Tom Holland soundalike)
        if (name.contains("en-gb-x-rjs")) score += 500 // High-energy British young hero
        if (name.contains("en-gb-x-gbd")) score += 450
        if (name.contains("en-gb-x-gbb")) score += 400
        if (name.contains("en-gb-x-rpj")) score += 350

        // General British English male
        if ((voice.locale.country.equals("GB", ignoreCase = true) ||
             voice.locale.language.equals("en_GB", ignoreCase = true) ||
             name.contains("en-gb")) && isMaleVoice(voice)) {
            score += 250
        }

        // US Male natural neural fallback
        if (name.contains("en-us-x-iol") || name.contains("en-us-x-tpd") || name.contains("en-us-x-iom")) score += 150
        if (isMaleVoice(voice)) score += 100

        if (voice.quality == Voice.QUALITY_VERY_HIGH) score += 50
        if (voice.quality == Voice.QUALITY_HIGH) score += 30
        if (name.contains("neural") || name.contains("wavenet") || name.contains("natural")) score += 40

        return score
    }

    private fun applyTomHollandMaleVoice() {
        tts?.let { engine ->
            val allVoices = engine.voices?.toList() ?: return
            
            // 1. Strict search: pick the highest scored British male voice
            val bestTomHollandVoice = allVoices
                .filter { isMaleVoice(it) }
                .maxByOrNull { tomHollandScore(it) }

            if (bestTomHollandVoice != null) {
                Log.d(TAG, "Selected Tom Holland voice: ${bestTomHollandVoice.name}")
                engine.voice = bestTomHollandVoice
                return
            }

            // 2. Fallback: Any English male voice
            val anyEnglishMale = allVoices.firstOrNull { it.locale.language == "en" && isMaleVoice(it) }
            if (anyEnglishMale != null) {
                engine.voice = anyEnglishMale
                return
            }

            // 3. Fallback: Set UK language
            engine.setLanguage(Locale.UK)
        }
    }

    private fun applyVoiceForLanguage(langCode: String) {
        tts?.let { engine ->
            val allVoices = engine.voices?.toList() ?: return
            when (langCode) {
                "hi" -> {
                    engine.setLanguage(Locale.Builder().setLanguage("hi").setRegion("IN").build())
                    val hiMaleVoice = allVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.language == "hi" || n.contains("hi-in") || n.contains("hin")) && isMaleVoice(v)
                    } ?: allVoices.firstOrNull { it.locale.language == "hi" && !isExplicitlyFemale(it) }
                    if (hiMaleVoice != null) engine.voice = hiMaleVoice
                }
                "bn" -> {
                    engine.setLanguage(Locale.Builder().setLanguage("bn").setRegion("IN").build())
                    val bnMaleVoice = allVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.language == "bn" || n.contains("bn-in") || n.contains("bn-bd") || n.contains("ben")) && isMaleVoice(v)
                    } ?: allVoices.firstOrNull { it.locale.language == "bn" && !isExplicitlyFemale(it) }
                    if (bnMaleVoice != null) engine.voice = bnMaleVoice
                }
                else -> {
                    applyTomHollandMaleVoice()
                }
            }
        }
    }

    fun speak(
        text: String,
        rate: Float = 1.10f,
        pitch: Float = 1.12f,
        voiceName: String = DEFAULT_TOM_HOLLAND_VOICE,
        onDone: (() -> Unit)? = null
    ) {
        targetRate = rate
        targetPitch = pitch
        targetVoiceName = voiceName
        currentOnDoneCallback = onDone

        val processedText = preprocessTextForNaturalSpeech(text)
        val detectedLang = detectLanguage(processedText)

        // Stop any current playback
        stop()

        val shouldUseNeural = preferences?.settings?.value?.useNeuralStudioVoice ?: true

        // If Neural Studio Voice is enabled, attempt realistic Gemini Neural Synthesis (voice "Puck" - Tom Holland soundalike)
        if (shouldUseNeural && detectedLang == "en") {
            scope.launch {
                val neuralSuccess = geminiSynthesizer.synthesizeAndPlay(
                    text = processedText,
                    voiceName = GeminiSpeechSynthesizer.TOM_HOLLAND_PUCK_VOICE,
                    onDone = onDone
                )

                if (!neuralSuccess) {
                    // Fallback to local Android TTS with fine-tuned Tom Holland parameters
                    speakLocal(processedText, rate, pitch, voiceName, detectedLang)
                }
            }
            return
        }

        // Local Speech Path
        speakLocal(processedText, rate, pitch, voiceName, detectedLang)
    }

    private fun speakLocal(
        processedText: String,
        rate: Float,
        pitch: Float,
        voiceName: String,
        detectedLang: String
    ) {
        if (!isInitialized) {
            pendingSpeechText = processedText
            return
        }

        tts?.let { engine ->
            // Dynamic Pitch & Energy Modulation for Laughter & Excitement (Tom Holland higher energetic inflection)
            val containsHumorOrLaugh = processedText.contains("Haha", ignoreCase = true) ||
                    processedText.contains("Hehe", ignoreCase = true) ||
                    processedText.contains("!", ignoreCase = true)
            
            // Tom Holland base pitch is youthful (~1.12f), with enthusiastic modulation
            val effectivePitch = if (pitch <= 1.05f) 1.12f else pitch
            val effectiveRate = if (rate <= 1.05f) 1.10f else rate

            val adjustedPitch = if (containsHumorOrLaugh) (effectivePitch * 1.04f).coerceAtMost(1.30f) else effectivePitch
            val adjustedRate = if (containsHumorOrLaugh) (effectiveRate * 1.02f).coerceAtMost(1.28f) else effectiveRate

            engine.setSpeechRate(adjustedRate)
            engine.setPitch(adjustedPitch)

            if (voiceName != DEFAULT_TOM_HOLLAND_VOICE &&
                !voiceName.startsWith("Tom Holland") &&
                !voiceName.startsWith("⚡") &&
                voiceName != "Default"
            ) {
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
            engine.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        geminiSynthesizer.stop()
        tts?.stop()
        _isSpeaking.value = false
        onSpeakingStateChanged(false)
    }

    fun shutdown() {
        geminiSynthesizer.stop()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
