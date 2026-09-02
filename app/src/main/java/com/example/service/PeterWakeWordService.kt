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
import android.os.PowerManager
import com.example.core.model.IntentType
import com.example.data.local.PeterDatabase
import com.example.data.local.PeterPreferences
import com.example.data.local.PeterRepository
import com.example.domain.ai.AIBrain
import com.example.domain.device.AndroidDeviceController
import com.example.domain.router.CommandRouter
import com.example.feature.voice.PeterTextToSpeech
import com.example.feature.voice.PeterWakeWordDetector
import com.example.service.LockdownOverlayManager
import kotlinx.coroutines.flow.collectLatest
import android.provider.Settings
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

    private var partialWakeLock: PowerManager.WakeLock? = null
    private var overlayManager: LockdownOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            partialWakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Peter:PersistentWakeLock")
            partialWakeLock?.acquire()
        } catch (e: Exception) {
            // Ignore
        }

        createNotificationChannel()
        val deviceController = AndroidDeviceController(this)
        val preferences = PeterPreferences(this)
        val aiBrain = AIBrain(deviceController, preferences)
        val db = PeterDatabase.getDatabase(this)
        repository = PeterRepository(db, preferences)
        commandRouter = CommandRouter(this, deviceController, aiBrain)

        tts = PeterTextToSpeech(this, preferences) {}
        overlayManager = LockdownOverlayManager(this)
        serviceScope.launch {
            preferences.settings.collectLatest { settings ->
                if (settings.isLockdownActive) {
                    if (Settings.canDrawOverlays(this@PeterWakeWordService)) {
                        overlayManager?.showOverlay(onUnlockAttempt = { password -> 
                            val isUnlocked = password.trim().equals("Daddy is home", ignoreCase = true)
                            if (isUnlocked) preferences.setLockdownActive(false)
                            isUnlocked
                        })
                    }
                } else {
                    overlayManager?.hideOverlay()
                }
            }
        }


        wakeWordDetector = PeterWakeWordDetector(this) { detectedText ->
            serviceScope.launch {
                try {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    @Suppress("DEPRECATION")
                    val wakeLock = powerManager?.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "Peter:WakeWordWakeLock"
                    )
                    wakeLock?.acquire(3000L)
                } catch (e: Exception) {
                    // Ignore wake lock error
                }

                // Broadcast to MainActivity if it's alive to handle UI transition
                val broadcastIntent = Intent("com.example.WAKE_WORD_DETECTED").apply {
                    putExtra("detectedText", detectedText)
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)

                val stripped = detectedText
                    .replace(Regex("(?i)^(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|হে|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spiderman|spider-man|পিটার|पीटर)\\s*"), "")
                    .trim()

                if (stripped.isBlank() || stripped.length <= 2) {
                    val uiIntent = Intent(this@PeterWakeWordService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        val pendingIntent = PendingIntent.getActivity(this@PeterWakeWordService, 0, uiIntent, PendingIntent.FLAG_IMMUTABLE)
                        pendingIntent.send()
                    } catch (e: Exception) {
                        startActivity(uiIntent)
                    }
                    return@launch
                }

                val result = commandRouter?.routeAndExecute(detectedText)
                if (result != null) {
                    if (result.intentType == IntentType.EMERGENCY_LOCKDOWN) {
                        preferences.setLockdownActive(true)
                    }
                    
                    val uiIntent = Intent(this@PeterWakeWordService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        val pendingIntent = PendingIntent.getActivity(this@PeterWakeWordService, 1, uiIntent, PendingIntent.FLAG_IMMUTABLE)
                        pendingIntent.send()
                    } catch (e: Exception) {
                        startActivity(uiIntent)
                    }

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
        try {
            partialWakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {}
        
        wakeWordDetector?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PETER Silent Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background listening service for 'Hey Peter' wake word"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
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
            .setContentTitle("PETER AI Active")
            .setContentText("Listening for 'Hey Peter' • Low-Power Background Engine")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setSound(null)
            .setVibrate(null)
            .setDefaults(0)
            .setPriority(NotificationCompat.PRIORITY_MIN)
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
