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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PeterState
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudBlueNeon
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudDeepGlow
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan
import com.example.ui.theme.HudTextDim
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudArcReactorCore(
    state: PeterState,
    rmsLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClick: () -> Unit,
    onScreenShareClick: () -> Unit,
    onChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentAmPm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentAmPm = amPmFormat.format(now)
            delay(1000)
        }
    }

    val transition = rememberInfiniteTransition(label = "HudReactorAnim")

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == PeterState.PROCESSING || state == PeterState.THINKING) 3000 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterDialRotation"
    )

    val innerCounterRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == PeterState.PROCESSING || state == PeterState.THINKING) 4000 else 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerCounterRotation"
    )

    val neonPulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening || isSpeaking) 600 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NeonEnergyPulse"
    )

    val primaryHudColor = when (state) {
        PeterState.ERROR -> StatusRed
        else -> HudCyanNeon
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Futuristic Arc Reactor Hologram
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMicClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f

                // 1. Cyber Bracket Wings Top & Bottom (Holographic energy conduits from reference image)
                val wingPathTop = Path().apply {
                    moveTo(center.x - baseRadius * 0.75f, center.y - baseRadius * 0.45f)
                    cubicTo(
                        center.x - baseRadius * 0.45f, center.y - baseRadius * 0.95f,
                        center.x + baseRadius * 0.45f, center.y - baseRadius * 0.95f,
                        center.x + baseRadius * 0.75f, center.y - baseRadius * 0.45f
                    )
                }
                drawPath(
                    path = wingPathTop,
                    color = HudBlueNeon.copy(alpha = 0.4f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                val wingPathBottom = Path().apply {
                    moveTo(center.x - baseRadius * 0.75f, center.y + baseRadius * 0.45f)
                    cubicTo(
                        center.x - baseRadius * 0.45f, center.y + baseRadius * 0.95f,
                        center.x + baseRadius * 0.45f, center.y + baseRadius * 0.95f,
                        center.x + baseRadius * 0.75f, center.y + baseRadius * 0.45f
                    )
                }
                drawPath(
                    path = wingPathBottom,
                    color = HudBlueNeon.copy(alpha = 0.4f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2. Glowing outer energy halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryHudColor.copy(alpha = 0.35f * neonPulse),
                            HudBlueNeon.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 0.98f
                    ),
                    radius = baseRadius * 0.98f,
                    center = center
                )

                // 3. Outermost Cyan Cyber Ring with Tachometer Segments
                drawCircle(
                    color = HudPanelBorder.copy(alpha = 0.6f),
                    radius = baseRadius * 0.90f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Segmented rotating outer arc
                rotate(outerRotation, pivot = center) {
                    for (i in 0 until 12) {
                        val angle = i * 30f
                        drawArc(
                            color = primaryHudColor.copy(alpha = if (i % 2 == 0) 0.85f else 0.4f),
                            startAngle = angle,
                            sweepAngle = 18f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 0.86f, center.y - baseRadius * 0.86f),
                            size = Size(baseRadius * 1.72f, baseRadius * 1.72f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Outer calibration tick marks
                    for (i in 0 until 36) {
                        val rad = Math.toRadians(i * 10.0)
                        val r1 = baseRadius * 0.88f
                        val r2 = baseRadius * (if (i % 3 == 0) 0.93f else 0.90f)
                        val startX = center.x + (r1 * cos(rad)).toFloat()
                        val startY = center.y + (r1 * sin(rad)).toFloat()
                        val endX = center.x + (r2 * cos(rad)).toFloat()
                        val endY = center.y + (r2 * sin(rad)).toFloat()

                        drawLine(
                            color = primaryHudColor.copy(alpha = if (i % 3 == 0) 0.9f else 0.35f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i % 3 == 0) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }

                // 4. Middle Concentric Reactor Ring with Counter-Rotation
                rotate(innerCounterRotation, pivot = center) {
                    drawCircle(
                        color = HudBlueNeon.copy(alpha = 0.5f),
                        radius = baseRadius * 0.68f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f, 4f, 8f), 0f)
                        )
                    )

                    // 4 glowing cardinal brackets
                    for (deg in listOf(0f, 90f, 180f, 270f)) {
                        drawArc(
                            color = primaryHudColor,
                            startAngle = deg - 10f,
                            sweepAngle = 20f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 0.72f, center.y - baseRadius * 0.72f),
                            size = Size(baseRadius * 1.44f, baseRadius * 1.44f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // 5. Inner Cyber Core Circle (Clock Housing)
                val coreRadius = baseRadius * 0.50f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HudPanelBg,
                            SophisticatedBlack
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )

                // Neon glowing core border
                val activePulse = if (isListening) (1f + rmsLevel * 0.5f) else neonPulse
                drawCircle(
                    color = primaryHudColor.copy(alpha = 0.9f),
                    radius = coreRadius * activePulse.coerceIn(0.95f, 1.15f),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Circular shadow ring
                drawCircle(
                    color = primaryHudColor.copy(alpha = 0.15f),
                    radius = coreRadius * 0.85f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Digital Clock & Status in the Center Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Mini Status Indicator
                Text(
                    text = state.name,
                    color = primaryHudColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Sci-Fi Clock Digits Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudBgDark.copy(alpha = 0.85f))
                        .border(1.dp, HudPanelBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currentTime.ifBlank { "03:08" },
                            color = primaryHudColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentAmPm.ifBlank { "PM" },
                            color = HudTextCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Small center pulse dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isListening || isSpeaking) primaryHudColor else HudBlueNeon)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick HUD Control Nodes underneath the Arc Core (matching reference UI)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Power / Voice Assistant Mic
            HudQuickNodeButton(
                icon = if (isListening) Icons.Default.Hearing else Icons.Default.Mic,
                label = "VOICE",
                isActive = isListening,
                activeColor = primaryHudColor,
                onClick = onMicClick,
                testTag = "hud_node_voice"
            )

            // 2. Screen Share / Vision Search
            HudQuickNodeButton(
                icon = Icons.AutoMirrored.Filled.ScreenShare,
                label = "SCREEN",
                isActive = false,
                activeColor = HudCyanNeon,
                onClick = onScreenShareClick,
                testTag = "hud_node_screen"
            )

            // 3. Holographic Chat HUD
            HudQuickNodeButton(
                icon = Icons.Default.Chat,
                label = "HUD CHAT",
                isActive = true,
                activeColor = HudCyanNeon,
                onClick = onChatClick,
                testTag = "hud_node_chat"
            )

            // 4. Settings
            HudQuickNodeButton(
                icon = Icons.Default.Settings,
                label = "CONFIG",
                isActive = false,
                activeColor = HudTextCyan,
                onClick = onSettingsClick,
                testTag = "hud_node_settings"
            )

            // 5. Lockdown Security
            HudQuickNodeButton(
                icon = Icons.Default.Security,
                label = "LOCK",
                isActive = false,
                activeColor = StatusRed,
                onClick = onSecurityClick,
                testTag = "hud_node_lockdown"
            )
        }
    }
}

@Composable
fun HudQuickNodeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "hud_node"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor.copy(alpha = 0.25f) else HudBgDark)
            .border(
                width = 1.2.dp,
                color = if (isActive) activeColor else HudPanelBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else HudTextCyan,
            modifier = Modifier.size(16.dp)
        )
    }
}
