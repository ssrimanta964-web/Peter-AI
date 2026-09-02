package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ChatMessage
import com.example.core.model.IntentType
import com.example.core.model.PeterState
import com.example.data.local.PeterDatabase
import com.example.data.local.PeterPreferences
import com.example.data.local.PeterRepository
import com.example.data.local.PeterSettings
import com.example.domain.ai.AIBrain
import com.example.domain.device.AndroidDeviceController
import com.example.domain.device.BatteryStatus
import com.example.domain.device.DeviceInfo
import com.example.domain.device.NetworkStatus
import com.example.domain.device.VolumeStatus
import com.example.domain.router.CommandRouter
import com.example.feature.voice.PeterSpeechRecognizer
import com.example.feature.voice.PeterTextToSpeech
import com.example.feature.voice.PeterWakeWordDetector
import com.example.service.PeterWakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeterViewModel(application: Application) : AndroidViewModel(application) {

    val deviceController = AndroidDeviceController(application)
    val preferences = PeterPreferences(application)
    private val database = PeterDatabase.getDatabase(application)
    val repository = PeterRepository(database, preferences)
    private val aiBrain = AIBrain(deviceController, preferences)
    private val commandRouter = CommandRouter(application, deviceController, aiBrain)

    // UI States
    private val _peterState = MutableStateFlow(PeterState.IDLE)
    val peterState: StateFlow<PeterState> = _peterState.asStateFlow()

    private val _isScreenAnalyzing = MutableStateFlow(false)
    val isScreenAnalyzing: StateFlow<Boolean> = _isScreenAnalyzing.asStateFlow()

    private val _screenCaptureRequested = MutableStateFlow(false)
    val screenCaptureRequested: StateFlow<Boolean> = _screenCaptureRequested.asStateFlow()

    private val _statusText = MutableStateFlow("PETER standing by. Ready for input.")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _batteryStatus = MutableStateFlow(deviceController.getBatteryInfo())
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _networkStatus = MutableStateFlow(deviceController.getNetworkInfo())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _volumeStatus = MutableStateFlow(deviceController.adjustVolume(0))
    val volumeStatus: StateFlow<VolumeStatus> = _volumeStatus.asStateFlow()

    val settings: StateFlow<PeterSettings> = preferences.settings

    val messages: StateFlow<List<ChatMessage>> = repository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Engine components
    private var speechRecognizer: PeterSpeechRecognizer? = null
    private var tts: PeterTextToSpeech? = null
    private var inAppWakeWordDetector: PeterWakeWordDetector? = null

    val rmsLevel: StateFlow<Float> get() = speechRecognizer?.rmsLevel ?: MutableStateFlow(0f)
    val isListening: StateFlow<Boolean> get() = speechRecognizer?.isListening ?: MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> get() = tts?.isSpeaking ?: MutableStateFlow(false)
    val availableVoices: StateFlow<List<String>> get() = tts?.availableVoices ?: MutableStateFlow(listOf("Default"))

    init {
        initTts(application)
        initSpeechRecognizer(application)
        initWakeWordDetector(application)
        refreshTelemetry()

        // Auto-start background wake word service if enabled
        if (preferences.settings.value.wakeWordEnabled) {
            try {
                PeterWakeWordService.startService(application)
            } catch (e: Exception) {
                // Background start can be deferred until activity is ready
            }
        }
    }

    private fun initTts(context: Context) {
        tts = PeterTextToSpeech(context) { isSpeaking ->
            if (isSpeaking) {
                _peterState.value = PeterState.SPEAKING
            } else if (_peterState.value == PeterState.SPEAKING) {
                _peterState.value = PeterState.IDLE
                resumeInAppWakeWord()
            }
        }
    }

    private fun initWakeWordDetector(context: Context) {
        inAppWakeWordDetector = PeterWakeWordDetector(context) { detectedPhrase ->
            viewModelScope.launch(Dispatchers.Main) {
                if (_peterState.value == PeterState.LISTENING || _peterState.value == PeterState.PROCESSING || _peterState.value == PeterState.THINKING) {
                    return@launch
                }

                inAppWakeWordDetector?.stop()

                val cleaned = detectedPhrase.trim()
                // Check if user spoke a full command with wake word, e.g. "hey peter turn on flashlight"
                val stripped = cleaned
                    .replace(Regex("(?i)^(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|হে|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spiderman|spider-man|পিটার|पीटर)\\s*"), "")
                    .trim()

                if (stripped.isNotBlank() && stripped.length > 2) {
                    executeUserPrompt(cleaned)
                } else {
                    // Just wake word greeted
                    val lang = com.example.domain.ai.LanguageHelper.detectLanguage(preferences.settings.value.preferredLanguage)
                    val greet = when (lang) {
                        com.example.domain.ai.SupportedLanguage.BENGALI -> "হ্যাঁ বস! বলুন আমি শুনছি!"
                        com.example.domain.ai.SupportedLanguage.HINDI -> "हाँ बॉस! बोलिए, मैं सुन रहा हूँ!"
                        com.example.domain.ai.SupportedLanguage.ENGLISH -> "Yes Boss! I'm listening mate, what's up?"
                    }
                    _statusText.value = greet
                    if (preferences.settings.value.autoSpeakResponses) {
                        val s = preferences.settings.value
                        tts?.speak(greet, s.speechRate, s.speechPitch, s.voiceName, onDone = {
                            startListening()
                        })
                    } else {
                        // Start active listening directly for the user's command
                        startListening()
                    }
                }
            }
        }

        if (preferences.settings.value.wakeWordEnabled) {
            inAppWakeWordDetector?.startContinuousWakeWordListening()
        }
    }

    fun startWakeWordDetection() {
        if (preferences.settings.value.wakeWordEnabled) {
            inAppWakeWordDetector?.startContinuousWakeWordListening()
        }
    }

    private fun resumeInAppWakeWord() {
        if (preferences.settings.value.wakeWordEnabled && !(_peterState.value == PeterState.LISTENING || _peterState.value == PeterState.SPEAKING)) {
            inAppWakeWordDetector?.startContinuousWakeWordListening()
        }
    }

    private fun initSpeechRecognizer(context: Context) {
        speechRecognizer = PeterSpeechRecognizer(
            context = context,
            onResult = { recognizedText ->
                executeUserPrompt(recognizedText)
            },
            onError = { errorMsg ->
                _peterState.value = PeterState.ERROR
                _statusText.value = errorMsg
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (_peterState.value == PeterState.ERROR) {
                        _peterState.value = PeterState.IDLE
                        _statusText.value = "PETER standing by."
                        resumeInAppWakeWord()
                    }
                }
            },
            onStateChange = { listening ->
                if (listening) {
                    tts?.stop()
                    inAppWakeWordDetector?.stop()
                    _peterState.value = PeterState.LISTENING
                    _statusText.value = "Listening to your voice..."
                } else if (_peterState.value == PeterState.LISTENING) {
                    _peterState.value = PeterState.PROCESSING
                    _statusText.value = "Processing audio stream..."
                }
            }
        )
    }

    fun startListening() {
        tts?.stop()
        inAppWakeWordDetector?.stop()
        speechRecognizer?.startListening(settings.value.preferredLanguage)
    }

    fun updatePreferredLanguage(language: String) {
        preferences.updatePreferredLanguage(language)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_peterState.value == PeterState.LISTENING) {
            _peterState.value = PeterState.IDLE
            _statusText.value = "Listening cancelled."
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_peterState.value == PeterState.SPEAKING) {
            _peterState.value = PeterState.IDLE
        }
    }

    fun executeUserPrompt(rawPrompt: String) {
        if (rawPrompt.isBlank()) return

        viewModelScope.launch(Dispatchers.Main) {
            _peterState.value = PeterState.THINKING
            _statusText.value = "Analyzing command: \"$rawPrompt\""

            // Save user message in stream
            repository.saveMessage(
                ChatMessage(
                    text = rawPrompt,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Route command
            val commandResult = commandRouter.routeAndExecute(rawPrompt)
            refreshTelemetry()

            // Save assistant reply
            repository.saveMessage(
                ChatMessage(
                    text = commandResult.spokenResponse,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    intentType = commandResult.intentType,
                    statusSuccess = commandResult.success,
                    searchQuery = commandResult.searchQuery
                )
            )

            repository.recordAuditLog(rawPrompt, commandResult)

            _statusText.value = commandResult.spokenResponse

            // Emergency Lockdown Activation Trigger
            if (commandResult.intentType == IntentType.EMERGENCY_LOCKDOWN) {
                activateLockdownInternal()
            }

            // Voice response
            if (preferences.settings.value.autoSpeakResponses) {
                val s = preferences.settings.value
                tts?.speak(commandResult.spokenResponse, s.speechRate, s.speechPitch, s.voiceName)
            } else {
                _peterState.value = if (commandResult.success) PeterState.IDLE else PeterState.ERROR
            }
        }
    }

    private fun activateLockdownInternal() {
        stopListening()
        preferences.setLockdownActive(true)
        _statusText.value = "⚠️ CODE RED: FULL EMERGENCY LOCKDOWN ENGAGED"
    }

    fun activateLockdown(spokenMessage: String? = null) {
        stopListening()
        preferences.setLockdownActive(true)
        _statusText.value = "⚠️ CODE RED: FULL EMERGENCY LOCKDOWN ENGAGED"
        val msg = spokenMessage ?: "CODE RED PROTOCOL ACTIVATED! Full security lockdown engaged! Enter authorization password to deactivate."
        if (preferences.settings.value.autoSpeakResponses) {
            val s = preferences.settings.value
            tts?.speak(msg, s.speechRate, s.speechPitch, s.voiceName)
        }
    }

    fun deactivateLockdown(passwordAttempt: String): Boolean {
        val trimmed = passwordAttempt.trim()
        val isCorrect = trimmed.equals("Daddy is home", ignoreCase = true)
        if (isCorrect) {
            preferences.setLockdownActive(false)
            _statusText.value = "🔓 Lockdown deactivated. Normal operations restored."
            
            val lang = com.example.domain.ai.LanguageHelper.detectLanguage(preferences.settings.value.preferredLanguage)
            val unlockMsg = when (lang) {
                com.example.domain.ai.SupportedLanguage.BENGALI ->
                    "লকডাউন মোড নিষ্ক্রিয় করা হয়েছে! স্বাগতম বস! সমস্ত সিস্টেম পুনরুদ্ধার করা হয়েছে!"
                com.example.domain.ai.SupportedLanguage.HINDI ->
                    "लॉकडाउन मोड बंद कर दिया गया है! वेलकम बैक बॉस! सारे सिस्टम्स वापस चालू हो गए हैं!"
                com.example.domain.ai.SupportedLanguage.ENGLISH ->
                    "Lockdown deactivated! Welcome back, Boss! All systems and controls restored."
            }

            viewModelScope.launch {
                repository.saveMessage(
                    ChatMessage(
                        text = "🔓 [CODE RED OVERRIDE] Lockdown deactivated. Authorization accepted.",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        intentType = IntentType.EMERGENCY_LOCKDOWN,
                        statusSuccess = true
                    )
                )
            }

            if (preferences.settings.value.autoSpeakResponses) {
                val s = preferences.settings.value
                tts?.speak(unlockMsg, s.speechRate, s.speechPitch, s.voiceName)
            }
            return true
        } else {
            val failMsg = "Access Denied: Invalid security passphrase."
            if (preferences.settings.value.autoSpeakResponses) {
                val s = preferences.settings.value
                tts?.speak(failMsg, s.speechRate, s.speechPitch, s.voiceName)
            }
            return false
        }
    }

    fun requestScreenShare() {
        _screenCaptureRequested.value = true
    }

    fun onScreenCaptureHandled() {
        _screenCaptureRequested.value = false
    }

    fun analyzeSharedScreen(bitmap: android.graphics.Bitmap, userPrompt: String = "") {
        _isScreenAnalyzing.value = true
        _peterState.value = PeterState.PROCESSING
        _statusText.value = "Peter's Spider-Sense analyzing screen with Multimodal AI..."

        val prompt = userPrompt.ifBlank { "What is on my screen? Search and explain it in detail." }

        viewModelScope.launch {
            repository.saveMessage(
                ChatMessage(
                    text = "🖥️ [SHARED SCREEN] Analyzing screen content...",
                    isUser = true,
                    timestamp = System.currentTimeMillis(),
                    intentType = IntentType.SCREEN_SEARCH
                )
            )

            val aiResponse = aiBrain.analyzeScreen(bitmap, prompt)
            _isScreenAnalyzing.value = false

            val spokenAnswer = aiResponse.directAnswer ?: "I reviewed your screen, mate!"
            val searchQuery = aiResponse.intent.query.ifBlank { "Screen Search" }

            repository.saveMessage(
                ChatMessage(
                    text = spokenAnswer,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    intentType = IntentType.SCREEN_SEARCH,
                    statusSuccess = aiResponse.error == null,
                    searchQuery = searchQuery
                )
            )

            _statusText.value = spokenAnswer
            _peterState.value = PeterState.IDLE

            if (preferences.settings.value.autoSpeakResponses) {
                val s = preferences.settings.value
                tts?.speak(spokenAnswer, s.speechRate, s.speechPitch, s.voiceName)
            }
        }
    }

    fun searchWeb(query: String) {
        deviceController.searchWeb(query)
    }

    fun showSearchProof(query: String? = null) {
        val targetQuery = query?.ifBlank { null } ?: commandRouter.getLastSearchQuery() ?: "Google Search"
        deviceController.searchWeb(targetQuery)
        val lang = com.example.domain.ai.LanguageHelper.detectLanguage(preferences.settings.value.preferredLanguage)
        val spoken = when (lang) {
            com.example.domain.ai.SupportedLanguage.BENGALI -> "এই যে '$targetQuery' এর গুগলের প্রমাণের পেজ খুলে দিয়েছি বন্ধু!"
            com.example.domain.ai.SupportedLanguage.HINDI -> "ये रहा '$targetQuery' के लिए गूगल पेज का प्रमाण, स्क्रीन पर खोल दिया है दोस्त!"
            com.example.domain.ai.SupportedLanguage.ENGLISH -> "Opening the Google search page as verified proof for '$targetQuery', mate!"
        }
        _statusText.value = spoken
        viewModelScope.launch {
            repository.saveMessage(
                ChatMessage(
                    text = "🌐 [PROOF VERIFIED] Opened Google Search page for: \"$targetQuery\"",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    intentType = IntentType.SHOW_PROOF,
                    statusSuccess = true,
                    searchQuery = targetQuery
                )
            )
        }
        if (preferences.settings.value.autoSpeakResponses) {
            val s = preferences.settings.value
            tts?.speak(spoken, s.speechRate, s.speechPitch, s.voiceName)
        }
    }

    fun refreshTelemetry() {
        _batteryStatus.value = deviceController.getBatteryInfo()
        _networkStatus.value = deviceController.getNetworkInfo()
        _volumeStatus.value = deviceController.adjustVolume(0)
    }

    fun toggleWakeWord(enabled: Boolean) {
        preferences.updateWakeWordEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            inAppWakeWordDetector?.startContinuousWakeWordListening()
            try {
                PeterWakeWordService.startService(context)
            } catch (e: Exception) {
                // Ignore service start if in restricted state
            }
        } else {
            inAppWakeWordDetector?.stop()
            PeterWakeWordService.stopService(context)
        }
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _statusText.value = "Conversation and command logs cleared."
        }
    }

    fun clearChatHistory() {
        clearConversationHistory()
    }

    fun triggerEmergencyLockdown() {
        activateLockdown()
    }

    fun unlockFromLockdown(password: String): Boolean {
        return deactivateLockdown(password)
    }

    fun updateSpeechRate(rate: Float) = preferences.updateSpeechRate(rate)
    fun updateSpeechPitch(pitch: Float) = preferences.updateSpeechPitch(pitch)
    fun updateVoiceName(voiceName: String) = preferences.updateVoiceName(voiceName)
    fun updateAiProvider(provider: String) = preferences.updateAiProvider(provider)
    fun updateLowPowerMode(lowPower: Boolean) = preferences.updateLowPowerMode(lowPower)
    fun updateAutoSpeak(autoSpeak: Boolean) = preferences.updateAutoSpeak(autoSpeak)
    fun updateBossProfile(name: String, title: String, details: String, nickname: String) =
        preferences.updateBossProfile(name, title, details, nickname)

    override fun onCleared() {
        speechRecognizer?.stopListening()
        tts?.shutdown()
        inAppWakeWordDetector?.stop()
        super.onCleared()
    }
}
