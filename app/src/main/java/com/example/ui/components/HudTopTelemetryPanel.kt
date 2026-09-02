package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.device.BatteryStatus
import com.example.domain.device.NetworkStatus
import com.example.domain.device.VolumeStatus
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudBlueNeon
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan
import com.example.ui.theme.HudTextDim
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextWhite
import kotlin.math.sin

@Composable
fun HudTopTelemetryPanel(
    batteryStatus: BatteryStatus,
    networkStatus: NetworkStatus,
    volumeStatus: VolumeStatus,
    isWakeWordServiceRunning: Boolean,
    onToggleWakeWord: () -> Unit,
    onRefreshTelemetry: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "TopSparklineAnim")
    val sparkWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SparkPhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(HudBgDark.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = HudPanelBorder,
                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Telemetry: Cores 1 & 2 + Swap / Battery
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudCoreMetric(
                    label = "C1",
                    value = "${(8 + (sin(sparkWave) * 3).toInt()).coerceIn(1, 99)}%",
                    phase = sparkWave,
                    color = HudCyanNeon
                )
                HudCoreMetric(
                    label = "C2",
                    value = "${(32 + (sin(sparkWave + 1.2f) * 6).toInt()).coerceIn(1, 99)}%",
                    phase = sparkWave + 1.2f,
                    color = HudCyanNeon
                )

                // SWAP telemetry
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudPanelBg)
                        .border(1.dp, HudPanelBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SWAP: 87%",
                        color = HudTextCyan,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "BAT: ${batteryStatus.percentage}%",
                        color = if (batteryStatus.isCharging) StatusGreen else HudTextDim,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 2. Center CPU Indicator & Wake Word Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Central CPU box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudPanelBg)
                        .border(1.2.dp, HudCyanNeon, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "0${(22 + (sin(sparkWave * 1.5f) * 5).toInt()).coerceIn(10, 99)}%",
                            color = HudCyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "CPU",
                            color = HudTextCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // AI Peter Status / Wake Word listener pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudPanelBg)
                        .border(1.dp, if (isWakeWordServiceRunning) HudCyanNeon else HudPanelBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleWakeWord)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("top_wake_word_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isWakeWordServiceRunning) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                        contentDescription = "Wake Word Status",
                        tint = if (isWakeWordServiceRunning) HudCyanNeon else HudTextDim,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isWakeWordServiceRunning) "PETER ACTIVE" else "WAKE PAUSED",
                        color = if (isWakeWordServiceRunning) HudCyanNeon else HudTextDim,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Refresh button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(HudPanelBg)
                        .border(1.dp, HudPanelBorder, CircleShape)
                        .clickable(onClick = onRefreshTelemetry),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Telemetry",
                        tint = HudTextCyan,
                        modifier = Modifier.size(11.dp)
                    )
                }

                // Settings button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(HudPanelBg)
                        .border(1.dp, HudCyanNeon, CircleShape)
                        .clickable(onClick = onSettingsClick)
                        .testTag("top_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = HudCyanNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 3. Right Telemetry: RAM + Cores 3 & 4
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RAM Box
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudPanelBg)
                        .border(1.dp, HudPanelBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "RAM: 76%",
                        color = HudTextCyan,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "NET: ${if (networkStatus.isConnected) networkStatus.connectionType.take(8) else "OFF"}",
                        color = if (networkStatus.isConnected) StatusGreen else StatusAmber,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HudCoreMetric(
                    label = "C3",
                    value = "${(5 + (sin(sparkWave + 2f) * 4).toInt()).coerceIn(1, 99)}%",
                    phase = sparkWave + 2f,
                    color = HudCyanNeon
                )
                HudCoreMetric(
                    label = "C4",
                    value = "${(40 + (sin(sparkWave + 3.14f) * 8).toInt()).coerceIn(1, 99)}%",
                    phase = sparkWave + 3.14f,
                    color = HudCyanNeon
                )
            }
        }
    }
}

@Composable
fun HudCoreMetric(
    label: String,
    value: String,
    phase: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HudPanelBg)
            .border(1.dp, HudPanelBorder.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HudTextDim,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = color,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Mini Sparkline Canvas
        Canvas(modifier = Modifier.width(36.dp).height(10.dp)) {
            val path = Path()
            val points = 7
            val dx = size.width / (points - 1)
            for (i in 0 until points) {
                val x = i * dx
                val wave = sin(phase + i * 0.8).toFloat()
                val y = size.height * (0.5f + wave * 0.35f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
