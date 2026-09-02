package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.PeterState
import com.example.ui.components.HudArcReactorCore
import com.example.ui.components.HudBottomTelemetryPanel
import com.example.ui.components.HudCommandInputBar
import com.example.ui.components.HudConversationOverlay
import com.example.ui.components.HudLeftDockPanel
import com.example.ui.components.HudRightDockPanel
import com.example.ui.components.HudTopTelemetryPanel
import com.example.ui.components.LockdownSecurityScreen
import com.example.ui.components.PermissionsDashboardDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderLight
import com.example.ui.theme.SophisticatedPanel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun PeterHomeScreen(
    viewModel: PeterViewModel,
    onStartScreenShare: () -> Unit = {},
    onPickScreenshot: () -> Unit = {},
    onEnableDeviceAdmin: () -> Unit = {},
    onLaunchSystemVoiceInput: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val peterState by viewModel.peterState.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val battery by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val network by viewModel.networkStatus.collectAsStateWithLifecycle()
    val volume by viewModel.volumeStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val isScreenAnalyzing by viewModel.isScreenAnalyzing.collectAsStateWithLifecycle()
    val screenCaptureRequested by viewModel.screenCaptureRequested.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showScreenShareSheet by remember { mutableStateOf(false) }
    var showConversationOverlay by remember { mutableStateOf(false) }

    // Handle ViewModel requested screen capture (e.g. triggered via voice)
    LaunchedEffect(screenCaptureRequested) {
        if (screenCaptureRequested) {
            viewModel.onScreenCaptureHandled()
            showScreenShareSheet = true
        }
    }

    // Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshTelemetry()
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.startWakeWordDetection()
            if (settings.wakeWordEnabled) {
                viewModel.toggleWakeWord(true)
            }
        }
    }

    val requestPermissions = {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Proactively request microphone permission on initial launch & start wake word if already granted
    LaunchedEffect(Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            requestPermissions()
        } else {
            viewModel.startWakeWordDetection()
        }
    }

    val onMicTrigger = {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            requestPermissions()
        } else {
            if (isListening) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(HudBgDark)
            .imePadding(),
        containerColor = HudBgDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF03182B),
                            HudBgDark
                        ),
                        radius = 1200f
                    )
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // MAIN LANDSCAPE SCI-FI HUD LAYOUT
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP TELEMETRY PANEL (CPU Cores, RAM, Swap, Wake Word)
                HudTopTelemetryPanel(
                    batteryStatus = battery,
                    networkStatus = network,
                    volumeStatus = volume,
                    isWakeWordServiceRunning = settings.wakeWordEnabled,
                    onToggleWakeWord = {
                        viewModel.toggleWakeWord(!settings.wakeWordEnabled)
                    },
                    onRefreshTelemetry = { viewModel.refreshTelemetry() },
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )

                // 2. MIDDLE ROW (Left Dock + Center Arc Reactor Core + Right Dock)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT DOCK PANEL (App shortcuts + Holo Nodes)
                    HudLeftDockPanel(
                        onPhotosClick = { onPickScreenshot() },
                        onTorchClick = { viewModel.executeUserPrompt("toggle flashlight") },
                        onSnipScreenClick = { showScreenShareSheet = true },
                        onMediaClick = { viewModel.executeUserPrompt("play spider man theme") },
                        onChatLogClick = { showConversationOverlay = true },
                        onSettingsClick = { showSettingsDialog = true },
                        onInternetClick = { viewModel.executeUserPrompt("search tech news") },
                        onShowProofClick = { viewModel.showSearchProof("quantum computing") },
                        onPermissionsClick = { showPermissionsDialog = true },
                        onClearChatClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.fillMaxHeight()
                    )

                    // CENTER ARC REACTOR CORE (Live Clock, Holo Rings, Power / Actions)
                    HudArcReactorCore(
                        state = peterState,
                        rmsLevel = rmsLevel,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        onMicClick = onMicTrigger,
                        onScreenShareClick = { showScreenShareSheet = true },
                        onChatClick = { showConversationOverlay = true },
                        onSettingsClick = { showSettingsDialog = true },
                        onSecurityClick = { viewModel.triggerEmergencyLockdown() },
                        modifier = Modifier.weight(1f)
                    )

                    // RIGHT DOCK PANEL (Holo Nodes + App shortcuts)
                    HudRightDockPanel(
                        onVoiceAssistantClick = onMicTrigger,
                        onScreenShareClick = { showScreenShareSheet = true },
                        onWakeWordClick = {
                            viewModel.toggleWakeWord(!settings.wakeWordEnabled)
                        },
                        onLockdownClick = { viewModel.triggerEmergencyLockdown() },
                        onTorchClick = { viewModel.executeUserPrompt("toggle flashlight") },
                        onVolumeToggleClick = { viewModel.executeUserPrompt("volume status") },
                        onGoogleProofClick = { viewModel.showSearchProof("Google AI Studio") },
                        onCalculatorClick = { viewModel.executeUserPrompt("what is 42 * 98?") },
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // 3. COMMAND INPUT & TYPING SECTION
                HudCommandInputBar(
                    onSendCommand = { prompt ->
                        viewModel.executeUserPrompt(prompt)
                    },
                    onMicClick = onMicTrigger,
                    isListening = isListening,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )

                // 4. BOTTOM TELEMETRY PANEL (Network Download/Upload Spectrum, Holographic Globe, OS Tray)
                HudBottomTelemetryPanel(
                    networkStatus = network,
                    statusMessage = statusText,
                    onStatusClick = { showConversationOverlay = true },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // HOLOGRAPHIC CONVERSATION & AI TERMINAL OVERLAY
            AnimatedVisibility(
                visible = showConversationOverlay,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                HudConversationOverlay(
                    messages = messages,
                    peterState = peterState,
                    isListening = isListening,
                    onSendMessage = { prompt ->
                        viewModel.executeUserPrompt(prompt)
                    },
                    onMicClick = onMicTrigger,
                    onSearchWeb = { query ->
                        viewModel.showSearchProof(query)
                    },
                    onDismiss = { showConversationOverlay = false }
                )
            }

            // SCREEN SHARE DIALOG
            if (showScreenShareSheet) {
                ScreenShareDialog(
                    onLiveCapture = {
                        showScreenShareSheet = false
                        onStartScreenShare()
                    },
                    onPickScreenshot = {
                        showScreenShareSheet = false
                        onPickScreenshot()
                    },
                    onDismiss = { showScreenShareSheet = false }
                )
            }

            // MULTIMODAL SCREEN SCANNER OVERLAY
            AnimatedVisibility(
                visible = isScreenAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SophisticatedPanel),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, HudCyanNeon),
                        modifier = Modifier.fillMaxWidth(0.65f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(HudCyanNeon.copy(alpha = 0.2f))
                                    .border(1.dp, HudCyanNeon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                                    contentDescription = "Screen Scanning",
                                    tint = HudCyanNeon,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Peter's Spider-Sense Visual Scan",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Analyzing your screen with Multimodal AI and searching background facts on Google...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = HudCyanNeon,
                                trackColor = SophisticatedBorder
                            )
                        }
                    }
                }
            }

            // SETTINGS DIALOG
            if (showSettingsDialog) {
                SettingsDialog(
                    settings = settings,
                    availableVoices = availableVoices,
                    onSpeechRateChange = { viewModel.updateSpeechRate(it) },
                    onSpeechPitchChange = { viewModel.updateSpeechPitch(it) },
                    onVoiceChange = { viewModel.updateVoiceName(it) },
                    onUseNeuralVoiceChange = { viewModel.updateUseNeuralStudioVoice(it) },
                    onPreviewVoice = { viewModel.previewVoice() },
                    onPreferredLanguageChange = { viewModel.updatePreferredLanguage(it) },
                    onAiProviderChange = { viewModel.updateAiProvider(it) },
                    onWakeWordChange = { viewModel.toggleWakeWord(it) },
                    onLowPowerChange = { viewModel.updateLowPowerMode(it) },
                    onAutoSpeakChange = { viewModel.updateAutoSpeak(it) },
                    onUpdateBossProfile = { name, title, details, nickname ->
                        viewModel.updateBossProfile(name, title, details, nickname)
                    },
                    onCustomApiKeyChange = { viewModel.updateCustomApiKey(it) },
                    onTriggerLockdown = { viewModel.triggerEmergencyLockdown() },
                    onClearHistory = { viewModel.clearConversationHistory() },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // PERMISSIONS DIALOG
            if (showPermissionsDialog) {
                PermissionsDashboardDialog(
                    onRequestPermissions = { requestPermissions() },
                    onEnableDeviceAdmin = onEnableDeviceAdmin,
                    isDeviceAdminActive = viewModel.isDeviceAdminActive(),
                    onDismiss = { showPermissionsDialog = false }
                )
            }

            // EMERGENCY LOCKDOWN FULLSCREEN OVERLAY
            if (settings.isLockdownActive) {
                LockdownSecurityScreen(
                    onUnlockAttempt = { pin ->
                        viewModel.unlockFromLockdown(pin)
                    },
                    isDeviceAdminActive = viewModel.isDeviceAdminActive(),
                    onEnableDeviceAdmin = onEnableDeviceAdmin,
                    onHardwareLock = { viewModel.executeHardwareLock() }
                )
            }
        }
    }
}

@Composable
fun ScreenShareDialog(
    onLiveCapture: () -> Unit,
    onPickScreenshot: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedPanel),
            border = BorderStroke(1.dp, HudCyanNeon.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HudCyanNeon.copy(alpha = 0.15f))
                        .border(1.dp, HudCyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                        contentDescription = "Screen Vision",
                        tint = HudCyanNeon,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Share Screen with Peter",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = "Peter will visually inspect your screen with Gemini Multimodal AI, answer questions, and perform background searches with proof on demand!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Option 1: Live Screen Capture
                Button(
                    onClick = onLiveCapture,
                    colors = ButtonDefaults.buttonColors(containerColor = HudCyanNeon),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_capture_live_screen")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                        contentDescription = "Capture Live Screen",
                        tint = SophisticatedBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Capture & Analyze Screen",
                        color = SophisticatedBlack,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Option 2: Choose Screenshot
                OutlinedButton(
                    onClick = onPickScreenshot,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HudCyanNeon),
                    border = BorderStroke(1.dp, SophisticatedBorderLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_pick_screenshot")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Screenshot",
                        tint = HudCyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Screenshot / Image",
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        }
    }
}
