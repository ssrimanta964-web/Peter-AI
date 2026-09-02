package com.example.feature.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.PeterPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GeminiSpeechSynthesizer(
    private val context: Context,
    private val preferences: PeterPreferences? = null,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    companion object {
        private const val TAG = "GeminiSpeech"
        // Google Gemini TTS model for lifelike neural audio
        private const val TTS_MODEL = "gemini-2.5-flash-preview-tts"
        // 'Puck' is Google's youthful, energetic, charming British boyish hero voice (Tom Holland soundalike)
        const val TOM_HOLLAND_PUCK_VOICE = "Puck"
        const val CHARON_VOICE = "Charon"
        const val FENRIR_VOICE = "Fenrir"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var currentSynthesisJob: Job? = null

    /**
     * Synthesizes text into lifelike Tom Holland neural voice and plays it.
     * Returns true if successfully synthesized and playback started, false otherwise.
     */
    suspend fun synthesizeAndPlay(
        text: String,
        voiceName: String = TOM_HOLLAND_PUCK_VOICE,
        onDone: (() -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = try {
            val custom = preferences?.settings?.value?.customApiKey?.trim() ?: ""
            if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            preferences?.settings?.value?.customApiKey?.trim() ?: ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid Gemini API key available for Neural TTS")
            return@withContext false
        }

        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`+([^`]+)`+"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("[-•]\\s+"), "")
            .trim()

        if (cleanText.isBlank()) return@withContext false

        // Instruct model to speak in Tom Holland's natural British, fast-paced, charming and friendly cadence
        val promptText = cleanText

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", promptText))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$TTS_MODEL:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini TTS request failed: code ${response.code}")
                return@withContext false
            }

            val bodyString = response.body?.string() ?: return@withContext false
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates") ?: return@withContext false
            if (candidates.length() == 0) return@withContext false

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return@withContext false
            val parts = content.optJSONArray("parts") ?: return@withContext false
            if (parts.length() == 0) return@withContext false

            var audioBase64: String? = null
            var mimeType: String = "audio/wav"

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    audioBase64 = inlineData.optString("data", "")
                    mimeType = inlineData.optString("mimeType", "audio/wav")
                    break
                }
            }

            if (audioBase64.isNullOrBlank()) {
                Log.w(TAG, "No audio inlineData found in Gemini TTS response")
                return@withContext false
            }

            val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
            if (rawAudioBytes.isEmpty()) return@withContext false

            val finalAudioBytes = if (isWavOrMp3(rawAudioBytes)) {
                rawAudioBytes
            } else {
                // If raw 24kHz 16-bit Mono PCM, prepend standard 44-byte WAV header
                createWavHeader(rawAudioBytes.size, sampleRate = 24000, channels = 1, bitsPerSample = 16) + rawAudioBytes
            }

            val tempFile = File(context.cacheDir, "peter_tom_holland_neural_voice.wav")
            FileOutputStream(tempFile).use { fos ->
                fos.write(finalAudioBytes)
                fos.flush()
            }

            withContext(Dispatchers.Main) {
                playAudioFile(tempFile, onDone)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Neural Voice synthesis exception", e)
            return@withContext false
        }
    }

    private fun isWavOrMp3(data: ByteArray): Boolean {
        if (data.size < 4) return false
        // RIFF header
        if (data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
            data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte()
        ) return true
        // ID3 header (MP3)
        if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) return true
        // MPEG sync word
        if (data[0] == 0xFF.toByte() && (data[1].toInt() and 0xE0) == 0xE0) return true
        return false
    }

    private fun createWavHeader(
        pcmDataLength: Int,
        sampleRate: Int = 24000,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ): ByteArray {
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // SubChunk1Size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat (1 for PCM)
        header[21] = 0
        header[22] = (channels.toInt() and 0xff).toByte()
        header[23] = ((channels.toInt() shr 8) and 0xff).toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = ((channels * bitsPerSample / 8) and 0xff).toByte()
        header[33] = 0
        header[34] = (bitsPerSample.toInt() and 0xff).toByte()
        header[35] = ((bitsPerSample.toInt() shr 8) and 0xff).toByte()

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        return header
    }

    private fun playAudioFile(file: File, onDone: (() -> Unit)?) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    onSpeakingStateChanged(true)
                    start()
                }
                setOnCompletionListener {
                    onSpeakingStateChanged(false)
                    releasePlayer()
                    onDone?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    onSpeakingStateChanged(false)
                    releasePlayer()
                    onDone?.invoke()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer initialization error", e)
            onSpeakingStateChanged(false)
            releasePlayer()
            onDone?.invoke()
        }
    }

    fun stop() {
        currentSynthesisJob?.cancel()
        currentSynthesisJob = null
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                releasePlayer()
            }
        } catch (e: Exception) {
            // Ignore stop errors
        }
        onSpeakingStateChanged(false)
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore release errors
        } finally {
            mediaPlayer = null
        }
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true
}
