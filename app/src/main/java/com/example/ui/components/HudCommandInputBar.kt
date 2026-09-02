package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudBgDark
import com.example.ui.theme.HudBlueNeon
import com.example.ui.theme.HudCyanNeon
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextCyan
import com.example.ui.theme.HudTextDim
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextWhite

@Composable
fun HudCommandInputBar(
    onSendCommand: (String) -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val infiniteTransition = rememberInfiniteTransition(label = "PromptBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isListening -> HudCyanNeon
            textInput.isNotBlank() -> HudCyanNeon.copy(alpha = 0.9f)
            else -> HudPanelBorder
        },
        label = "InputBorderColor"
    )

    val handleSend = {
        val trimmed = textInput.trim()
        if (trimmed.isNotEmpty()) {
            onSendCommand(trimmed)
            textInput = ""
            keyboardController?.hide()
        }
    }

    val quickCommands = listOf(
        "⚡ Flashlight" to "toggle flashlight",
        "🌐 Search Tech News" to "search tech news",
        "🕷️ Tell Spidey Joke" to "tell me a spidey joke",
        "🔊 Vol 80%" to "set volume to 80%",
        "🧠 Who Are You?" to "who are you peter?",
        "🔒 Lockdown" to "emergency lockdown"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Quick Action Chips Row (Scrollable horizontally)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickCommands.forEach { (label, command) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HudPanelBg.copy(alpha = 0.85f))
                        .border(0.8.dp, HudCyanNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable {
                            onSendCommand(command)
                        }
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        color = HudTextCyan,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Main Holographic Cyber Command Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF001B30),
                            HudBgDark.copy(alpha = 0.95f),
                            Color(0xFF001B30)
                        )
                    )
                )
                .border(
                    width = if (isListening || textInput.isNotBlank()) 1.2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Terminal Prompt Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Command Prompt",
                        tint = if (isListening) HudCyanNeon else HudCyanNeon.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "CMD:>",
                        color = HudCyanNeon,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Interactive Typing Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textInput.isEmpty() && !isListening) {
                        Text(
                            text = "Type command here (e.g. 'Turn on flashlight', 'Search AI', 'Tell a joke')...",
                            color = HudTextDim.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    } else if (isListening && textInput.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LISTENING FOR VOICE... ",
                                color = HudCyanNeon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HudCyanNeon.copy(alpha = cursorAlpha))
                            )
                        }
                    }

                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hud_command_input"),
                        textStyle = TextStyle(
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(HudCyanNeon),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { handleSend() }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Action Controls (Clear, Mic, Send)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Clear Button
                    AnimatedVisibility(
                        visible = textInput.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(HudPanelBg)
                                .clickable { textInput = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Input",
                                tint = HudTextDim,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Mic Voice Trigger
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isListening) HudCyanNeon.copy(alpha = 0.25f) else HudPanelBg)
                            .border(
                                width = 1.dp,
                                color = if (isListening) HudCyanNeon else HudPanelBorder,
                                shape = CircleShape
                            )
                            .clickable(onClick = onMicClick)
                            .testTag("command_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Input",
                            tint = if (isListening) HudCyanNeon else HudTextCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Transmit / Send Button
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (textInput.isNotBlank()) HudCyanNeon else HudCyanNeon.copy(alpha = 0.2f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (textInput.isNotBlank()) HudCyanNeon else HudPanelBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                enabled = textInput.isNotBlank(),
                                onClick = handleSend
                            )
                            .padding(horizontal = 8.dp)
                            .testTag("command_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "TRANSMIT",
                                color = if (textInput.isNotBlank()) HudBgDark else HudTextDim,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Transmit Command",
                                tint = if (textInput.isNotBlank()) HudBgDark else HudTextDim,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
