package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedPanel
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun PermissionsDashboardDialog(
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .testTag("permissions_dialog")
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SophisticatedCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "SECURITY MATRIX",
                            style = MaterialTheme.typography.titleLarge,
                            color = SophisticatedCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_permissions_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "PETER requires specific Android runtime permissions to process voice streams, control device features, and run low-power background operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Permission Items
                PermissionStatusCard(
                    icon = Icons.Default.Mic,
                    title = "Microphone Access",
                    description = "Required for Speech-to-Text voice recognition and 'Hey Peter' wake word.",
                    isGranted = hasMic
                )

                PermissionStatusCard(
                    icon = Icons.Default.FlashlightOn,
                    title = "Camera Hardware",
                    description = "Used exclusively for CameraManager flashlight/torch toggle.",
                    isGranted = hasCamera
                )

                PermissionStatusCard(
                    icon = Icons.Default.Notifications,
                    title = "System Notifications",
                    description = "Allows low-power foreground notification for continuous background wake-word.",
                    isGranted = hasNotif
                )

                Spacer(modifier = Modifier.size(4.dp))

                Button(
                    onClick = {
                        onRequestPermissions()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SophisticatedCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_request_all_permissions")
                ) {
                    Text("Grant / Verify Permissions", color = SophisticatedBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SophisticatedCard)
            .border(1.dp, if (isGranted) SophisticatedCyan.copy(alpha = 0.3f) else StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) SophisticatedCyan else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) SophisticatedCyan else StatusRed,
            modifier = Modifier.size(20.dp)
        )
    }
}

