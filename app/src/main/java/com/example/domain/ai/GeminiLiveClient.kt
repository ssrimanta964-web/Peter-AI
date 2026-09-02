package com.example.domain.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val onMessage: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChange: (Boolean) -> Unit
) {
    private val TAG = "GeminiLiveClient"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val sampleRateIn = 16000
    private val sampleRateOut = 24000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    fun startLiveSession() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            onError("Gemini API key is missing. Add it to the Secrets panel.")
            return
        }

        val request = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Live API WebSocket Opened")
                _isActive.value = true
                onStateChange(true)
                sendSetupMessage()
                startAudioCapture()
                initAudioPlayback()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Live API WebSocket Closed: $reason")
                stopLiveSession()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Live API WebSocket Error", t)
                onError("Live API WebSocket Error: ${t.localizedMessage}")
                stopLiveSession()
            }
        })
    }

    private fun sendSetupMessage() {
        val setupMsg = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-3.1-flash-live-preview")
                put("generationConfig", JSONObject().apply {
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Puck") // Energetic young male voice
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are Peter Parker (Spider-Man), an AI assistant named Peter. You have the incredibly friendly, energetic, slightly awkward, youthful, and fast-talking personality of Tom Holland. You love science, idolize Mr. Stark, and want to help out. You MUST tell lighthearted jokes, chuckle, and laugh out loud (using words like 'haha' or *laughs*) at funny things. CRITICAL: If the user explicitly asks you to speak or tell a joke in a specific language (like Hindi or Bengali), you MUST immediately switch to that language and speak naturally in it. CRITICAL: You must maintain this exact Tom Holland persona—complete with jokes, spontaneous laughter, and witty banter—in ANY language you speak, especially when switching to Hindi or Bengali! Keep your answers conversational, concise, warm, and full of youthful enthusiasm.")
                        })
                    })
                })
                put("tools", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
            })
        }
        webSocket?.send(setupMsg.toString())
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRateIn, channelConfigIn, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateIn,
            channelConfigIn,
            audioFormat,
            minBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError("Failed to initialize AudioRecord")
            return
        }

        audioRecord?.startRecording()
        isRecording = true

        scope.launch {
            val buffer = ByteArray(minBufferSize)
            while (isActive && isRecording) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readResult > 0) {
                    val pcmBase64 = Base64.encodeToString(buffer, 0, readResult, Base64.NO_WRAP)
                    sendRealtimeInput(pcmBase64)
                }
                delay(20) 
            }
        }
    }

    private fun initAudioPlayback() {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRateOut, channelConfigOut, audioFormat)
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRateOut,
            channelConfigOut,
            audioFormat,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
    }

    private fun sendRealtimeInput(pcmBase64: String) {
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", pcmBase64)
                    })
                })
            })
        }
        webSocket?.send(msg.toString())
    }
    
    fun sendTextMessage(text: String) {
        val msg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", text)
                            })
                        })
                    })
                })
                put("turnComplete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            // Handle Text
                            if (part.has("text")) {
                                val spokenText = part.getString("text")
                                onMessage(spokenText)
                            }
                            // Handle Audio
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val data = inlineData.getString("data")
                                val pcmBytes = Base64.decode(data, Base64.DEFAULT)
                                audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse server message", e)
        }
    }

    fun stopLiveSession() {
        _isActive.value = false
        isRecording = false
        onStateChange(false)
        webSocket?.close(1000, "Session ended")
        webSocket = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioRecord = null
        audioTrack = null
    }

    fun destroy() {
        stopLiveSession()
        scope.cancel()
    }
}
