package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.PeterDatabase
import com.example.data.local.PeterPreferences
import com.example.data.local.PeterRepository
import com.example.domain.ai.AIBrain
import com.example.domain.device.AndroidDeviceController
import com.example.domain.router.CommandRouter
import com.example.feature.voice.PeterTextToSpeech
import com.example.feature.voice.PeterWakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PeterWakeWordService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeWordDetector: PeterWakeWordDetector? = null
    private var tts: PeterTextToSpeech? = null
    private var commandRouter: CommandRouter? = null
    private var repository: PeterRepository? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val deviceController = AndroidDeviceController(this)
        val preferences = PeterPreferences(this)
        val aiBrain = AIBrain(deviceController, preferences)
        val db = PeterDatabase.getDatabase(this)
        repository = PeterRepository(db, preferences)
        commandRouter = CommandRouter(this, deviceController, aiBrain)

        tts = PeterTextToSpeech(this) {}

        wakeWordDetector = PeterWakeWordDetector(this) { detectedText ->
            serviceScope.launch {
                val result = commandRouter?.routeAndExecute(detectedText)
                if (result != null) {
                    if (preferences.settings.value.autoSpeakResponses) {
                        val s = preferences.settings.value
                        tts?.speak(result.spokenResponse, s.speechRate, s.speechPitch, s.voiceName)
                    }
                    repository?.recordAuditLog(detectedText, result)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeWordDetector?.startContinuousWakeWordListening()

        return START_STICKY
    }

    override fun onDestroy() {
        wakeWordDetector?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PETER Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening service for 'Hey Peter' wake word"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PETER AI Assistant Active")
            .setContentText("Listening for 'Hey Peter' • Low-Power Background Engine")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "peter_wake_word_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, PeterWakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PeterWakeWordService::class.java)
            context.stopService(intent)
        }
    }
}
