package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.device.BatteryStatus
import com.example.domain.device.NetworkStatus
import com.example.domain.device.VolumeStatus
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SophisticatedTelemetryCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
    testTag: String = "telemetry_card"
) {
    val cardShape = RoundedCornerShape(12.dp)
    val iconBoxShape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .testTag(testTag)
            .clip(cardShape)
            .background(SophisticatedBlack)
            .border(1.dp, SophisticatedBorder, cardShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon Box 32x32 #1A1A1A
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(iconBoxShape)
                .background(SophisticatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) SophisticatedCyan else StatusAmber,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TelemetryBar(
    battery: BatteryStatus,
    network: NetworkStatus,
    volume: VolumeStatus,
    wakeWordActive: Boolean,
    onBatteryClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onWakeWordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SophisticatedTelemetryCard(
                icon = if (battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                label = "Battery",
                value = "${battery.percentage}% / " + if (battery.isCharging) "Charging" else if (battery.percentage > 20) "Optimal" else "Low",
                isActive = battery.percentage > 15,
                onClick = onBatteryClick,
                testTag = "pill_battery",
                modifier = Modifier.weight(1f)
            )

            SophisticatedTelemetryCard(
                icon = if (network.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                label = "Network",
                value = if (network.isConnected) "Secure / ${network.connectionType.split(" ").first()}" else "Offline",
                isActive = network.isConnected,
                onClick = onNetworkClick,
                testTag = "pill_network",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SophisticatedTelemetryCard(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Audio Volume",
                value = "${volume.percentage}% / Master",
                isActive = true,
                onClick = onVolumeClick,
                testTag = "pill_volume",
                modifier = Modifier.weight(1f)
            )

            SophisticatedTelemetryCard(
                icon = Icons.Default.Hearing,
                label = "Wake Word",
                value = if (wakeWordActive) "Listening / Active" else "Standby / Off",
                isActive = wakeWordActive,
                onClick = onWakeWordClick,
                testTag = "pill_wake_word",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

