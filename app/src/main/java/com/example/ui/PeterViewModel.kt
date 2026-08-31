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
        refreshTelemetry()
    }

    private fun initTts(context: Context) {
        tts = PeterTextToSpeech(context) { isSpeaking ->
            if (isSpeaking) {
                _peterState.value = PeterState.SPEAKING
            } else if (_peterState.value == PeterState.SPEAKING) {
                _peterState.value = PeterState.IDLE
            }
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
                    }
                }
            },
            onStateChange = { listening ->
                if (listening) {
                    tts?.stop()
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
        speechRecognizer?.startListening()
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
                    statusSuccess = commandResult.success
                )
            )

            repository.recordAuditLog(rawPrompt, commandResult)

            _statusText.value = commandResult.spokenResponse

            // Voice response
            if (preferences.settings.value.autoSpeakResponses) {
                val s = preferences.settings.value
                tts?.speak(commandResult.spokenResponse, s.speechRate, s.speechPitch, s.voiceName)
            } else {
                _peterState.value = if (commandResult.success) PeterState.IDLE else PeterState.ERROR
            }
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
            PeterWakeWordService.startService(context)
        } else {
            PeterWakeWordService.stopService(context)
        }
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _statusText.value = "Conversation and command logs cleared."
        }
    }

    override fun onCleared() {
        speechRecognizer?.stopListening()
        tts?.shutdown()
        inAppWakeWordDetector?.stop()
        super.onCleared()
    }
}
