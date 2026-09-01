package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.ui.PeterHomeScreen
import com.example.ui.PeterViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PeterViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenDisplay()
        setContent {
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
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
