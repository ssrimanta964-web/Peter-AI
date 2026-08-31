package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PeterSettings
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedPanel
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun SettingsDialog(
    settings: PeterSettings,
    availableVoices: List<String>,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onAiProviderChange: (String) -> Unit,
    onWakeWordChange: (Boolean) -> Unit,
    onLowPowerChange: (Boolean) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .testTag("settings_dialog")
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PETER CONFIGURATION",
                        style = MaterialTheme.typography.titleLarge,
                        color = SophisticatedCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                // 1. VOICE SYNTHESIS SETTINGS
                SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "VOICE ENGINE")

                // Auto Speak Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Voice Responses (TTS)", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        Text("Speak responses aloud automatically", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = settings.autoSpeakResponses,
                        onCheckedChange = onAutoSpeakChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SophisticatedBlack,
                            checkedTrackColor = SophisticatedCyan,
                            uncheckedTrackColor = SophisticatedSurface
                        ),
                        modifier = Modifier.testTag("switch_auto_speak")
                    )
                }

                // Speech Speed Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speech Speed", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(String.format("%.1fx", settings.speechRate), color = SophisticatedCyan, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = settings.speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = SophisticatedCyan, activeTrackColor = SophisticatedCyan),
                        modifier = Modifier.testTag("slider_speech_speed")
                    )
                }

                // Speech Pitch Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Voice Pitch", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(String.format("%.1fx", settings.speechPitch), color = SophisticatedCyan, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = settings.speechPitch,
                        onValueChange = onSpeechPitchChange,
                        valueRange = 0.5f..1.5f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = SophisticatedCyan, activeTrackColor = SophisticatedCyan),
                        modifier = Modifier.testTag("slider_speech_pitch")
                    )
                }

                // 2. AI BRAIN SELECTION
                SettingsSectionHeader(icon = Icons.Default.Psychology, title = "AI BRAIN CORE")
                val aiModes = listOf("Auto (Gemini + Local)", "Local Only", "Cloud Only")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aiModes.forEach { mode ->
                        val isSelected = settings.aiProvider == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SophisticatedCyan.copy(alpha = 0.15f) else SophisticatedCard)
                                .border(1.dp, if (isSelected) SophisticatedCyan else SophisticatedBorder, RoundedCornerShape(10.dp))
                                .clickable { onAiProviderChange(mode) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.replace(" (Gemini + Local)", ""),
                                color = if (isSelected) SophisticatedCyan else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 3. WAKE WORD SETTINGS
                SettingsSectionHeader(icon = Icons.Default.Hearing, title = "WAKE WORD (HEY PETER)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Wake Word", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        Text("Listen for 'Hey Peter' via Foreground Service", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = onWakeWordChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SophisticatedBlack,
                            checkedTrackColor = SophisticatedCyan,
                            uncheckedTrackColor = SophisticatedSurface
                        ),
                        modifier = Modifier.testTag("switch_wake_word")
                    )
                }
                Text(
                    text = "Note: Continuous listening complies with Android battery & privacy restrictions using a low-power persistent notification.",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )

                // 4. PERFORMANCE & PRIVACY
                SettingsSectionHeader(icon = Icons.Default.Security, title = "PERFORMANCE & PRIVACY")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Low-Power Mode", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                        Text("Reduces orbital animations for RAM & battery saving", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = settings.lowPowerMode,
                        onCheckedChange = onLowPowerChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SophisticatedBlack,
                            checkedTrackColor = SophisticatedCyan,
                            uncheckedTrackColor = SophisticatedSurface
                        ),
                        modifier = Modifier.testTag("switch_low_power")
                    )
                }

                // Clear History Button
                Button(
                    onClick = {
                        onClearHistory()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_clear_history")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Clear Conversation History", color = StatusRed, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SophisticatedCyan, modifier = Modifier.size(15.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = SophisticatedCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

