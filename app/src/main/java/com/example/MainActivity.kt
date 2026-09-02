package com.example

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.receiver.PeterDeviceAdminReceiver
import com.example.ui.PeterHomeScreen
import com.example.ui.PeterViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PeterViewModel by viewModels()

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (PeterDeviceAdminReceiver.isDeviceAdminActive(this)) {
            Toast.makeText(this, "🛡️ System Device Administrator Activated for P.E.T.E.R.", Toast.LENGTH_LONG).show()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = captureWindowBitmap()
            if (bitmap != null) {
                viewModel.analyzeSharedScreen(bitmap, "Analyze my current screen, search background facts, and answer my question!")
            } else {
                Toast.makeText(this, "Screen capture ready. Analyzing window snapshot...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.analyzeSharedScreen(bitmap, "Analyze this screenshot, search background facts on Google, and answer what is shown!")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load screenshot: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val speechInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull { it.isNotBlank() }
            if (!spokenText.isNullOrBlank()) {
                viewModel.executeUserPrompt(spokenText)
            }
        }
    }

    private val wakeWordReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.example.WAKE_WORD_DETECTED") {
                val detectedText = intent.getStringExtra("detectedText") ?: ""
                val stripped = detectedText
                    .replace(Regex("(?i)^(hey|hello|hi|ok|okay|namaste|yo|he|hay|hai|listen|হেই|হ্যালো|শোনো|নমস্কার|হে|सुनो|नमस्ते)?\\s*(peter|piter|pete|pita|pitar|spiderman|spider-man|পিটার|पीटर)\\s*"), "")
                    .trim()
                
                if (stripped.isBlank() || stripped.length <= 2) {
                    viewModel.startListening()
                } else {
                    viewModel.executeUserPrompt(detectedText)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, android.content.IntentFilter("com.example.WAKE_WORD_DETECTED"), android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, android.content.IntentFilter("com.example.WAKE_WORD_DETECTED"))
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (e: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            startActivityForResult(intent, 1001)
        }
        enableEdgeToEdge()
        configureLockScreenDisplay()
        
        // Start Wake Word Service safely from foreground Activity
        try {
            com.example.service.PeterWakeWordService.startService(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start wake word service", e)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()

            // SYSTEM-WIDE FULL ANDROID RESTRICTION CONTROLLER
            LaunchedEffect(settings.isLockdownActive) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (settings.isLockdownActive) {
                    // 1. Full immersive system bar hiding & blocking
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())

                    // 2. Hardware screen lock if Device Admin is enabled
                    val lockedHardware = PeterDeviceAdminReceiver.lockEntireDevice(this@MainActivity)
                    if (lockedHardware) {
                        Log.d("MainActivity", "DevicePolicyManager lockNow executed successfully")
                    }

                    // 3. Android Screen Pinning / Kiosk Mode (Locks Home, Recents, and Status Bar gestures)
                    try {
                        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        val isAlreadyPinned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
                        } else false

                        if (!isAlreadyPinned) {
                            startLockTask()
                        }
                    } catch (e: Exception) {
                        Log.w("MainActivity", "startLockTask attempt: ${e.message}")
                    }
                } else {
                    // Restore standard system bars and release Screen Pinning
                    try {
                        stopLockTask()
                    } catch (_: Exception) {}
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MyApplicationTheme {
                PeterHomeScreen(
                    viewModel = viewModel,
                    onStartScreenShare = {
                        startScreenCapture()
                    },
                    onPickScreenshot = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onEnableDeviceAdmin = {
                        try {
                            val intent = PeterDeviceAdminReceiver.getDeviceAdminPromptIntent(this)
                            deviceAdminLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to open Device Admin settings: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLaunchSystemVoiceInput = {
                        try {
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak command to P.E.T.E.R. (English, Hindi, Bengali)...")
                            }
                            speechInputLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Voice recognition service unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    private fun startScreenCapture() {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mediaProjectionManager != null) {
                screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            } else {
                // Fallback to window snapshot
                val bitmap = captureWindowBitmap()
                if (bitmap != null) {
                    viewModel.analyzeSharedScreen(bitmap, "Analyze what is on the screen right now.")
                }
            }
        } catch (e: Exception) {
            // Direct window snapshot
            val bitmap = captureWindowBitmap()
            if (bitmap != null) {
                viewModel.analyzeSharedScreen(bitmap, "Analyze what is on the screen right now.")
            } else {
                Toast.makeText(this, "Screen capture unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureWindowBitmap(): Bitmap? {
        return try {
            val view = window.decorView.rootView
            val w = view.width.coerceAtLeast(400)
            val h = view.height.coerceAtLeast(600)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun configureLockScreenDisplay() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
