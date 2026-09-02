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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.device.NetworkStatus
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudBlueNeon
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan
import com.example.ui.theme.HudTextDim
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudBottomTelemetryPanel(
    networkStatus: NetworkStatus,
    statusMessage: String,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeShort by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeShort = format.format(Date())
            delay(1000)
        }
    }

    val transition = rememberInfiniteTransition(label = "BottomTelemetryAnim")
    val globeRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlobeSpin"
    )

    val spectrumWave by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpectrumPulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. Cyber Bezel Telemetry Bracket (Reference image bottom panel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(HudBgDark.copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    color = HudPanelBorder,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: NET DOWNLOAD & Spectrum Waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DOWNLOAD",
                            color = HudTextCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "239.97 M",
                            color = HudTextDim,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Spectrum Bars Canvas
                    Canvas(modifier = Modifier.width(44.dp).height(18.dp)) {
                        val barCount = 10
                        val barWidth = size.width / (barCount * 1.6f)
                        for (i in 0 until barCount) {
                            val hFraction = (0.2f + 0.8f * sin(spectrumWave * 3.14f + i * 0.45).toFloat()).coerceIn(0.15f, 1f)
                            val barHeight = size.height * hFraction
                            val x = i * (barWidth * 1.6f)
                            drawRect(
                                color = HudCyanNeon.copy(alpha = 0.85f),
                                topLeft = Offset(x, size.height - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }
                }

                // Center: Holographic Spinning Wireframe Globe & Speed Readout
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onStatusClick)
                ) {
                    Text(
                        text = "0.0 SPD",
                        color = HudCyanNeon,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Spinning Holographic Globe Core
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(HudPanelBg)
                            .border(1.dp, HudCyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Holographic Globe",
                            tint = HudCyanNeon,
                            modifier = Modifier.size(15.dp).rotate(globeRotation)
                        )
                    }

                    Text(
                        text = "SPD 0.0",
                        color = HudCyanNeon,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Right: NET UPLOAD & Spectrum Waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spectrum Bars Canvas Right
                    Canvas(modifier = Modifier.width(44.dp).height(18.dp)) {
                        val barCount = 10
                        val barWidth = size.width / (barCount * 1.6f)
                        for (i in 0 until barCount) {
                            val hFraction = (0.2f + 0.8f * cos(spectrumWave * 3.14f + i * 0.45).toFloat()).coerceIn(0.15f, 1f)
                            val barHeight = size.height * hFraction
                            val x = i * (barWidth * 1.6f)
                            drawRect(
                                color = HudBlueNeon.copy(alpha = 0.85f),
                                topLeft = Offset(x, size.height - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "UPLOAD",
                            color = HudTextCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "44.38 M",
                            color = HudTextDim,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Sci-Fi OS Status Bar (matching bottom blue bar in reference image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00385C),
                            Color(0xFF001A2E)
                        )
                    )
                )
                .border(0.5.dp, HudPanelBorder)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left start badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HudCyanNeon)
                    )
                    Text(
                        text = statusMessage.take(55),
                        color = TextWhite,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Right Tray (Wi-Fi, Volume, Short Time)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (networkStatus.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Wi-Fi",
                        tint = if (networkStatus.isConnected) HudCyanNeon else HudTextDim,
                        modifier = Modifier.size(9.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Audio",
                        tint = HudTextCyan,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = currentTimeShort.ifBlank { "12:00" },
                        color = HudCyanNeon,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
