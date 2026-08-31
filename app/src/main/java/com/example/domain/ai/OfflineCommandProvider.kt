package com.example.domain.ai

import com.example.core.model.IntentType
import com.example.core.model.PeterIntent
import java.util.Locale

class OfflineCommandProvider : AIProvider {
    override val name: String = "PETER Offline Engine"

    override suspend fun analyzeCommand(prompt: String): AIResponse {
        val clean = prompt.trim().lowercase(Locale.ROOT)
            .removePrefix("hey peter")
            .removePrefix("peter")
            .removePrefix("ok peter")
            .removePrefix("hello peter")
            .trim()

        // 1. Flashlight
        if (clean.contains("flashlight") || clean.contains("torch") || clean.contains("flash light")) {
            val isOff = clean.contains("off") || clean.contains("disable") || clean.contains("stop")
            val action = if (isOff) "OFF" else "ON"
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.FLASHLIGHT,
                    rawText = prompt,
                    action = action,
                    confidence = 0.98f
                )
            )
        }

        // 2. Battery Status
        if (clean.contains("battery") || clean.contains("power level") || clean.contains("charge level") || clean.contains("percentage")) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.BATTERY_STATUS,
                    rawText = prompt,
                    confidence = 0.95f
                )
            )
        }

        // 3. Volume Control
        if (clean.contains("volume") || clean.contains("sound") || clean.contains("audio level") || clean.contains("mute") || clean.contains("unmute")) {
            val action = when {
                clean.contains("up") || clean.contains("increase") || clean.contains("raise") || clean.contains("louder") || clean.contains("boost") -> "UP"
                clean.contains("down") || clean.contains("decrease") || clean.contains("lower") || clean.contains("quieter") || clean.contains("drop") -> "DOWN"
                clean.contains("mute") || clean.contains("silence") -> "MUTE"
                clean.contains("max") || clean.contains("100") -> "MAX"
                else -> {
                    // Check for percentage e.g. "set volume to 50%"
                    val percentRegex = "(\\d+)\\s*%?".toRegex()
                    val match = percentRegex.find(clean)
                    if (match != null) "SET" else "STATUS"
                }
            }
            val num = "(\\d+)".toRegex().find(clean)?.value?.toIntOrNull() ?: 50
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.VOLUME_CONTROL,
                    rawText = prompt,
                    action = action,
                    value = num,
                    confidence = 0.92f
                )
            )
        }

        // 4. Open App
        if (clean.startsWith("open ") || clean.startsWith("launch ") || clean.startsWith("start ") || clean.contains("open app")) {
            val appTarget = clean
                .removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .removePrefix("the ")
                .removePrefix("app ")
                .trim()
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.OPEN_APP,
                    rawText = prompt,
                    targetApp = appTarget,
                    confidence = 0.90f
                )
            )
        }

        // 5. Open Settings
        if (clean.contains("setting") || clean.contains("wifi") || clean.contains("wi-fi") || clean.contains("bluetooth") || clean.contains("display settings")) {
            val setting = when {
                clean.contains("wifi") || clean.contains("wi-fi") || clean.contains("internet") -> "wifi"
                clean.contains("bluetooth") -> "bluetooth"
                clean.contains("display") || clean.contains("brightness") -> "display"
                clean.contains("sound") || clean.contains("audio") -> "sound"
                clean.contains("battery") -> "battery"
                clean.contains("date") || clean.contains("time") -> "date"
                clean.contains("location") || clean.contains("gps") -> "location"
                else -> "general"
            }
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.OPEN_SETTINGS,
                    rawText = prompt,
                    targetSetting = setting,
                    confidence = 0.91f
                )
            )
        }

        // 6. Time and Date
        if (clean.contains("time") || clean.contains("date") || clean.contains("day is it") || clean.contains("clock") || clean.contains("today")) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.TIME_AND_DATE,
                    rawText = prompt,
                    confidence = 0.96f
                )
            )
        }

        // 7. Timer & Alarm
        if (clean.contains("timer") || clean.contains("countdown")) {
            // Find numbers
            val match = "(\\d+)\\s*(minute|min|sec|second|hour|hr)s?".toRegex().find(clean)
            val totalSeconds = if (match != null) {
                val value = match.groupValues[1].toIntOrNull() ?: 1
                val unit = match.groupValues[2]
                when {
                    unit.startsWith("hour") || unit.startsWith("hr") -> value * 3600
                    unit.startsWith("sec") -> value
                    else -> value * 60
                }
            } else 60
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.TIMER,
                    rawText = prompt,
                    value = totalSeconds,
                    confidence = 0.92f
                )
            )
        }

        if (clean.contains("alarm") || clean.contains("wake me")) {
            val timeMatch = "(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?".toRegex().find(clean)
            var hour = 7
            var minute = 0
            if (timeMatch != null) {
                val rawH = timeMatch.groupValues[1].toIntOrNull() ?: 7
                val rawM = timeMatch.groupValues[2].toIntOrNull() ?: 0
                val ampm = timeMatch.groupValues[3]
                hour = if (ampm == "pm" && rawH < 12) rawH + 12 else if (ampm == "am" && rawH == 12) 0 else rawH
                minute = rawM
            }
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.ALARM,
                    rawText = prompt,
                    value = hour * 100 + minute,
                    confidence = 0.88f
                )
            )
        }

        // 8. Network Status
        if (clean.contains("network") || clean.contains("internet") || clean.contains("wifi status") || clean.contains("connection")) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.NETWORK_STATUS,
                    rawText = prompt,
                    confidence = 0.94f
                )
            )
        }

        // 9. Phone & Hardware Status
        if (clean.contains("device") || clean.contains("phone status") || clean.contains("ram") || clean.contains("system info") || clean.contains("specs")) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.PHONE_STATUS,
                    rawText = prompt,
                    confidence = 0.93f
                )
            )
        }

        // 10. Greetings & Offline basic responses
        if (clean.contains("who are you") || clean.contains("your name") || clean.contains("introduce yourself")) {
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = "I am PETER, your Personal Electronic Telemetry and Execution Resource. I am an advanced Android AI assistant ready to assist you with device control, telemetry, and intelligent queries."
            )
        }

        if (clean.contains("hello") || clean.contains("hi") || clean.contains("hey")) {
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = "Hello! PETER operational and standing by. What command or query can I process for you?"
            )
        }

        if (clean.contains("how are you") || clean.contains("status report")) {
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = "All core systems are nominal. Voice matrix, command router, and device interfaces are operating with full efficiency."
            )
        }

        // Generic query for Cloud or Fallback
        return AIResponse(
            intent = PeterIntent(
                type = IntentType.AI_QUERY,
                rawText = prompt,
                query = prompt,
                confidence = 0.5f
            )
        )
    }
}
