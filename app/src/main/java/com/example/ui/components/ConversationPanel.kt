package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ChatMessage
import com.example.core.model.IntentType
import com.example.ui.theme.SophisticatedBlack
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderLight
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedCyan
import com.example.ui.theme.SophisticatedCyanFaint
import com.example.ui.theme.SophisticatedPanel
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun QuickCommandChips(
    onCommandSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickCommands = listOf(
        "Share screen with Peter 🖥️",
        "Look at my screen 🔍",
        "मेरी स्क्रीन देखो 🖥️",
        "আমার স্ক্রিন দেখো 🖥️",
        "Hey Peter code red 🚨",
        "Who is your boss? 👑",
        "तुम्हारा बॉस कौन है? 👑",
        "তোমার বস কে? 👑",
        "कोड रेड 🚨",
        "কোড রেড 🚨",
        "Tell me a joke!",
        "एक जोक सुनाओ! 🇮🇳",
        "একটি কৌতুক বলো! 🇧🇩",
        "Search on Google for Spider-Man",
        "Turn on flashlight",
        "फ्लैशलाइट ऑन करो",
        "টর্চ জ্বালাও",
        "Battery percentage",
        "बैटरी कितनी है?",
        "ব্যাটারি কত?",
        "Who are you?",
        "तुम कौन हो?",
        "তুমি কে?",
        "What time is it?",
        "সময় কত?",
        "समय क्या है?"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickCommands) { cmd ->
            val isSearch = cmd.startsWith("Search")
            Box(
                modifier = Modifier
                    .testTag("chip_${cmd.replace(" ", "_").lowercase()}")
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSearch) SophisticatedCyan.copy(alpha = 0.15f) else SophisticatedCard)
                    .border(1.dp, if (isSearch) SophisticatedCyan.copy(alpha = 0.5f) else SophisticatedBorder, RoundedCornerShape(20.dp))
                    .clickable { onCommandSelect(cmd) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSearch) Icons.Default.Search else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SophisticatedCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = cmd,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSearch) TextWhite else TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onWebSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) SophisticatedSurface else SophisticatedPanel
    val border = if (isUser) SophisticatedCyan.copy(alpha = 0.35f) else SophisticatedBorder
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = null,
                tint = if (isUser) SophisticatedCyan else TextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isUser) "YOU" else "PETER AI",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) SophisticatedCyan else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            if (message.intentType != null && !isUser) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SophisticatedBlack)
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = message.intentType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.intentType == IntentType.WEB_SEARCH) SophisticatedCyan else TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .testTag(if (isUser) "user_message_bubble" else "peter_message_bubble")
                .clip(shape)
                .background(bg)
                .border(1.dp, border, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) TextWhite else TextPrimary,
                    lineHeight = 20.sp
                )

                // If this is a Web Search message or contains a query, show interactive proof button
                if (!isUser && message.searchQuery != null && onWebSearch != null) {
                    Box(
                        modifier = Modifier
                            .testTag("btn_open_google_search")
                            .clip(RoundedCornerShape(8.dp))
                            .background(SophisticatedCyan.copy(alpha = 0.15f))
                            .border(1.dp, SophisticatedCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onWebSearch(message.searchQuery) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Show Google Proof",
                                tint = SophisticatedCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Show Google Proof: \"${message.searchQuery}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = SophisticatedCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = SophisticatedCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onWebSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = SophisticatedCyan.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "PETER V1.0.4 - ONLINE",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"Search for Spider-Man news\" or \"Turn on flashlight\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onWebSearch = onWebSearch
                )
            }
        }
    }
}


