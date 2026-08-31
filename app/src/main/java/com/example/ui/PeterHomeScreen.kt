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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.PeterState
import com.example.ui.components.ConversationList
import com.example.ui.components.PermissionsDashboardDialog
import com.example.ui.components.PeterCoreVisualizer
import com.example.ui.components.QuickCommandChips
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderLight
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedCyanFaint
import com.example.ui.theme.SophisticatedCyanGlow
import com.example.ui.theme.SophisticatedCyanMedium
import com.example.ui.theme.SophisticatedPanel
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun PeterHomeScreen(
    viewModel: PeterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshTelemetry()
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBlack)
            .imePadding(),
        containerColor = SophisticatedBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
        ) {
            // 1. SOPHISTICATED TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM STATUS",
                        color = SophisticatedCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (peterState == PeterState.ERROR) StatusRed else SophisticatedCyan)
                        )
                        Text(
                            text = "PETER V1.0.4 - ONLINE",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.refreshTelemetry() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, SophisticatedBorderLight, CircleShape)
                            .testTag("btn_refresh_telemetry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Telemetry",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showPermissionsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, SophisticatedBorderLight, CircleShape)
                            .testTag("btn_open_permissions")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions",
                            tint = SophisticatedCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, SophisticatedBorderLight, CircleShape)
                            .testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Divider Line with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 20.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                SophisticatedBorder,
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. CENTRAL ANIMATED AI CORE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeterCoreVisualizer(
                        state = peterState,
                        rmsLevel = rmsLevel,
                        lowPowerMode = settings.lowPowerMode,
                        modifier = Modifier.clickable {
                            if (isListening) viewModel.stopListening() else viewModel.startListening()
                        }
                    )

                    // Prompt / Transcript Subtext
                    Text(
                        text = if (statusText.isNotBlank()) "\"$statusText\"" else "\"Peter, turn on the flashlight and check my current battery level.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.25.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                }
            }

            // Quick Command Chips
            QuickCommandChips(
                onCommandSelect = { cmd ->
                    viewModel.executeUserPrompt(cmd)
                },
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // 3. CONVERSATION LOG
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                ConversationList(
                    messages = messages,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 4. SOPHISTICATED BOTTOM TELEMETRY & CONTROLS SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophisticatedPanel)
                    .border(1.dp, SophisticatedBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Telemetry 2x2 Grid
                TelemetryBar(
                    battery = battery,
                    network = network,
                    volume = volume,
                    wakeWordActive = settings.wakeWordEnabled,
                    onBatteryClick = { viewModel.executeUserPrompt("battery status") },
                    onNetworkClick = { viewModel.executeUserPrompt("network status") },
                    onVolumeClick = { viewModel.executeUserPrompt("volume status") },
                    onWakeWordClick = { showSettingsDialog = true }
                )

                // Bottom Input & Voice Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Settings/Tool circular button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, SophisticatedBorderLight, CircleShape)
                            .background(SophisticatedBlack)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Stop Speaking Button if Active
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StatusRed.copy(alpha = 0.2f))
                                .border(1.dp, StatusRed, CircleShape)
                                .testTag("btn_stop_speaking")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "Stop Speaking",
                                tint = StatusRed
                            )
                        }
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Command PETER...", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SophisticatedBlack,
                            unfocusedContainerColor = SophisticatedBlack,
                            focusedBorderColor = SophisticatedCyan,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.executeUserPrompt(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        })
                    )

                    // Action Button (Send or Center Voice FAB)
                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.executeUserPrompt(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SophisticatedCyan)
                                .testTag("btn_send_command")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = SophisticatedBlack
                            )
                        }
                    } else {
                        // Sophisticated Center Voice Button
                        Box(contentAlignment = Alignment.Center) {
                            // Ambient cyan halo
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(SophisticatedCyan.copy(alpha = 0.15f))
                            )

                            FloatingActionButton(
                                onClick = {
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
                                },
                                containerColor = if (isListening) StatusRed else SophisticatedCyan,
                                contentColor = SophisticatedBlack,
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fab_voice_input")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop Listening" else "Voice Input",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                availableVoices = availableVoices,
                onSpeechRateChange = { viewModel.preferences.updateSpeechRate(it) },
                onSpeechPitchChange = { viewModel.preferences.updateSpeechPitch(it) },
                onVoiceChange = { viewModel.preferences.updateVoiceName(it) },
                onAiProviderChange = { viewModel.preferences.updateAiProvider(it) },
                onWakeWordChange = { viewModel.toggleWakeWord(it) },
                onLowPowerChange = { viewModel.preferences.updateLowPowerMode(it) },
                onAutoSpeakChange = { viewModel.preferences.updateAutoSpeak(it) },
                onClearHistory = { viewModel.clearConversationHistory() },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showPermissionsDialog) {
            PermissionsDashboardDialog(
                onRequestPermissions = { requestPermissions() },
                onDismiss = { showPermissionsDialog = false }
            )
        }
    }
}

