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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.PeterState
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderLight
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedCyanFaint
import com.example.ui.theme.SophisticatedCyanGlow
import com.example.ui.theme.SophisticatedCyanMedium
import com.example.ui.theme.SophisticatedCyanStrong
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.StatusRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PeterCoreVisualizer(
    state: PeterState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    lowPowerMode: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PeterSophisticatedCoreTransition")

    // Slow rotation for dashed ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == PeterState.PROCESSING || state == PeterState.THINKING) 4000 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CoreRotation"
    )

    // Breathing pulse for core
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == PeterState.LISTENING) 700 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    val primaryColor = when (state) {
        PeterState.ERROR -> StatusRed
        else -> SophisticatedCyan
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main Core Container with Badge
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f

                // Outer Ring: 100% border #1A1A1A
                drawCircle(
                    color = SophisticatedSurface,
                    radius = baseRadius * 0.96f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Outer Dashed Ring: 85% border #333333 with gentle rotation
                rotate(if (lowPowerMode) 0f else rotation, pivot = center) {
                    drawCircle(
                        color = SophisticatedBorderLight,
                        radius = baseRadius * 0.82f,
                        center = center,
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    )
                }

                // Inner Circle: 60% with gradient from #00E5FF11 to transparent & border #00E5FF33
                val midRadius = baseRadius * 0.60f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                        center = center,
                        radius = midRadius
                    ),
                    radius = midRadius,
                    center = center
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Glowing Halo around core
                val corePulse = if (state == PeterState.LISTENING) (1f + rmsLevel * 0.4f) else breathingPulse
                val coreRadius = baseRadius * 0.28f * corePulse

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.45f), Color.Transparent),
                        center = center,
                        radius = coreRadius * 2.2f
                    ),
                    radius = coreRadius * 2.2f,
                    center = center
                )

                // Core Solid Orb: #00E5FF with shadow
                drawCircle(
                    color = primaryColor,
                    radius = coreRadius,
                    center = center
                )

                // Dark Inner Ring: border-2 border-black opacity-20
                drawCircle(
                    color = SophisticatedBlack.copy(alpha = 0.25f),
                    radius = coreRadius * 0.8f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Center Dot
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }

            // Top Status Badge (e.g. LISTENING / IDLE)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(SophisticatedBlack)
                    .border(1.dp, SophisticatedBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = state.name,
                    color = primaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Equalizer soundwave bars below
        SophisticatedWaveformBars(
            state = state,
            rmsLevel = rmsLevel,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun SophisticatedWaveformBars(
    state: PeterState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveformPulse"
    )

    val isActive = state == PeterState.LISTENING || state == PeterState.SPEAKING
    val baseRms = if (state == PeterState.LISTENING) rmsLevel.coerceIn(0f, 1f) else (if (isActive) waveAnim else 0f)

    val barConfigs = listOf(
        Pair(16.dp, 0.40f),
        Pair(26.dp, 0.60f),
        Pair(42.dp, 1.00f),
        Pair(34.dp, 0.80f),
        Pair(22.dp, 0.50f),
        Pair(30.dp, 0.70f),
        Pair(18.dp, 0.40f)
    )

    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barConfigs.forEachIndexed { index, (baseHeight, alpha) ->
            val dynamicScale = if (isActive) {
                (0.35f + (baseRms * (0.65f + 0.35f * sin((index + 1) * 1.5).toFloat()))).coerceIn(0.2f, 1.3f)
            } else {
                0.25f
            }
            val height = (baseHeight.value * dynamicScale).coerceAtLeast(4f).dp

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (state == PeterState.ERROR) StatusRed.copy(alpha = alpha)
                        else SophisticatedCyan.copy(alpha = if (isActive) alpha else 0.25f)
                    )
            )
        }
    }
}

