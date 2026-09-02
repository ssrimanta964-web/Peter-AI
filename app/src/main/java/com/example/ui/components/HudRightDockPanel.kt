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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan

@Composable
fun HudRightDockPanel(
    onVoiceAssistantClick: () -> Unit,
    onScreenShareClick: () -> Unit,
    onWakeWordClick: () -> Unit,
    onLockdownClick: () -> Unit,
    onTorchClick: () -> Unit,
    onVolumeToggleClick: () -> Unit,
    onGoogleProofClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Right Holographic Radial Node List
        Column(
            modifier = Modifier.padding(end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            HudHoloNodeItem(label = "E-MAIL", onClick = onVoiceAssistantClick, isLeftAligned = false, testTag = "node_email")
            HudHoloNodeItem(label = "SCREEN REC", onClick = onScreenShareClick, isLeftAligned = false, testTag = "node_screen_rec")
            HudHoloNodeItem(label = "SOUND REC", onClick = onWakeWordClick, isLeftAligned = false, testTag = "node_sound_rec")
            HudHoloNodeItem(label = "LOCKDOWN", onClick = onLockdownClick, isLeftAligned = false, testTag = "node_lockdown")
            HudHoloNodeItem(label = "BURNING", onClick = onTorchClick, isLeftAligned = false, testTag = "node_burning")
            HudHoloNodeItem(label = "REMOTE", onClick = onVolumeToggleClick, isLeftAligned = false, testTag = "node_remote")
            HudHoloNodeItem(label = "TORRENTS", onClick = onGoogleProofClick, isLeftAligned = false, testTag = "node_torrents")
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Rightmost Sci-Fi Beveled App Dock
        Column(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(HudBgDark.copy(alpha = 0.95f))
                .border(
                    width = 1.2.dp,
                    color = HudPanelBorder,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HudAppBevelIcon(abbr = "Ts", label = "VOICE MIC", onClick = onVoiceAssistantClick, testTag = "app_dock_teamspeak")
            HudAppBevelIcon(abbr = "Td", label = "NEWS / FEED", onClick = onGoogleProofClick, testTag = "app_dock_tweetdeck")
            HudAppBevelIcon(abbr = "M", label = "CHAT HUD", onClick = onVoiceAssistantClick, testTag = "app_dock_messenger")
            HudAppBevelIcon(abbr = "Cl", label = "CALCULATOR", onClick = onCalculatorClick, testTag = "app_dock_calculator")
            HudAppBevelIcon(abbr = "Fb", label = "BROWSER", onClick = onGoogleProofClick, testTag = "app_dock_facebook")
            HudAppBevelIcon(abbr = "G+", label = "GOOGLE PROOF", onClick = onGoogleProofClick, testTag = "app_dock_google")
            HudAppBevelIcon(abbr = "5", label = "500PX VISION", onClick = onScreenShareClick, testTag = "app_dock_500px")
        }
    }
}
