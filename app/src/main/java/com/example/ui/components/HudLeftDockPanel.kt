package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan
import com.example.ui.theme.HudTextDim
import com.example.ui.theme.TextWhite

@Composable
fun HudLeftDockPanel(
    onPhotosClick: () -> Unit,
    onTorchClick: () -> Unit,
    onSnipScreenClick: () -> Unit,
    onMediaClick: () -> Unit,
    onChatLogClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInternetClick: () -> Unit,
    onShowProofClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onClearChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Leftmost Sci-Fi Beveled App Dock
        Column(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(HudBgDark.copy(alpha = 0.95f))
                .border(
                    width = 1.2.dp,
                    color = HudPanelBorder,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HudAppBevelIcon(abbr = "Ps", label = "PHOTOSHOP", onClick = onPhotosClick, testTag = "app_dock_ps")
            HudAppBevelIcon(abbr = "L", label = "LIGHTROOM", onClick = onTorchClick, testTag = "app_dock_light")
            HudAppBevelIcon(abbr = "St", label = "SNIP TOOL", onClick = onSnipScreenClick, testTag = "app_dock_snip")
            HudAppBevelIcon(abbr = "V", label = "VEGAS PRO", onClick = onMediaClick, testTag = "app_dock_vegas")
            HudAppBevelIcon(abbr = "Sn", label = "STICKY NOTE", onClick = onChatLogClick, testTag = "app_dock_notes")
            HudAppBevelIcon(abbr = "Np", label = "NOTEPAD", onClick = onSettingsClick, testTag = "app_dock_notepad")
            HudAppBevelIcon(abbr = "Wd", label = "WORD", onClick = onInternetClick, testTag = "app_dock_word")
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Left Holographic Radial Node List (from reference image)
        Column(
            modifier = Modifier.padding(start = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            HudHoloNodeItem(label = "INTERNET", onClick = onInternetClick, isLeftAligned = true, testTag = "node_internet")
            HudHoloNodeItem(label = "DOCUMENTS", onClick = onShowProofClick, isLeftAligned = true, testTag = "node_documents")
            HudHoloNodeItem(label = "DOWNLOADS", onClick = onShowProofClick, isLeftAligned = true, testTag = "node_downloads")
            HudHoloNodeItem(label = "PICTURES", onClick = onSnipScreenClick, isLeftAligned = true, testTag = "node_pictures")
            HudHoloNodeItem(label = "CONTROL", onClick = onPermissionsClick, isLeftAligned = true, testTag = "node_control")
            HudHoloNodeItem(label = "NETWORK", onClick = onInternetClick, isLeftAligned = true, testTag = "node_network")
            HudHoloNodeItem(label = "RECYCLE", onClick = onClearChatClick, isLeftAligned = true, testTag = "node_recycle")
        }
    }
}

@Composable
fun HudAppBevelIcon(
    abbr: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "hud_app_icon"
) {
    Column(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(HudPanelBg)
                .border(1.dp, HudCyanNeon.copy(alpha = 0.8f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abbr,
                color = HudCyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = label.take(6),
            color = HudTextDim,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
fun HudHoloNodeItem(
    label: String,
    onClick: () -> Unit,
    isLeftAligned: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "hud_holo_node"
) {
    Row(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLeftAligned) {
            Text(
                text = label,
                color = HudTextCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(HudBgDark)
                    .border(1.2.dp, HudCyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(HudCyanNeon)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(HudBgDark)
                    .border(1.2.dp, HudCyanNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(HudCyanNeon)
                )
            }
            Text(
                text = label,
                color = HudTextCyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
