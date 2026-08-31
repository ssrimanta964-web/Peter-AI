package com.example.domain.router

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.core.model.CommandResult
import com.example.core.model.IntentType
import com.example.core.model.PeterIntent
import com.example.domain.ai.AIBrain
import com.example.domain.device.DeviceController

class CommandRouter(
    private val context: Context,
    private val deviceController: DeviceController,
    private val aiBrain: AIBrain
) {
    suspend fun routeAndExecute(rawPrompt: String): CommandResult {
        if (rawPrompt.isBlank()) {
            return CommandResult(
                success = false,
                intentType = IntentType.UNKNOWN,
                spokenResponse = "I did not detect any command. Please speak again."
            )
        }

        // 1. AI Analysis / Intent Extraction
        val aiResponse = aiBrain.processUserPrompt(rawPrompt)
        val intent = aiResponse.intent

        // If direct answer was provided by conversational AI
        if (aiResponse.directAnswer != null) {
            return CommandResult(
                success = true,
                intentType = IntentType.AI_QUERY,
                spokenResponse = aiResponse.directAnswer,
                displayDetails = if (aiResponse.isFromCloud) "Source: Gemini Cloud AI" else "Source: Local AI Brain"
            )
        }

        // 2. Command routing with safety, permission & version compatibility verification
        return when (intent.type) {
            IntentType.FLASHLIGHT -> executeFlashlight(intent)
            IntentType.BATTERY_STATUS -> executeBatteryStatus()
            IntentType.VOLUME_CONTROL -> executeVolumeControl(intent)
            IntentType.OPEN_APP -> executeOpenApp(intent)
            IntentType.OPEN_SETTINGS -> executeOpenSettings(intent)
            IntentType.TIME_AND_DATE -> executeTimeAndDate()
            IntentType.ALARM -> executeAlarm(intent)
            IntentType.TIMER -> executeTimer(intent)
            IntentType.PHONE_STATUS -> executePhoneStatus()
            IntentType.NETWORK_STATUS -> executeNetworkStatus()
            IntentType.MEDIA_CONTROL -> executeMediaControl(intent)
            IntentType.AI_QUERY -> {
                // If query reached here without direct answer
                CommandResult(
                    success = true,
                    intentType = IntentType.AI_QUERY,
                    spokenResponse = "I processed your query. Connect an active internet connection for extended online knowledge.",
                    displayDetails = "Query: ${intent.rawText}"
                )
            }
            IntentType.UNKNOWN -> {
                CommandResult(
                    success = false,
                    intentType = IntentType.UNKNOWN,
                    spokenResponse = "Command not recognized. You can ask for battery level, flashlight, volume, time, app launching, or any query."
                )
            }
        }
    }

    private fun executeFlashlight(intent: PeterIntent): CommandResult {
        if (!deviceController.isFlashlightAvailable()) {
            return CommandResult(
                success = false,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = "No camera flashlight hardware was detected on this device.",
                errorMessage = "Hardware unsupported"
            )
        }

        val turnOn = intent.action.uppercase() != "OFF"
        val result = deviceController.setFlashlight(turnOn)

        return if (result.isSuccess) {
            val spoken = if (turnOn) "Flashlight turned on." else "Flashlight turned off."
            CommandResult(
                success = true,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = spoken,
                displayDetails = "Torch Mode: ${if (turnOn) "ACTIVE" else "DISABLED"}"
            )
        } else {
            CommandResult(
                success = false,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = "Unable to toggle flashlight. Device camera service may be in use.",
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeBatteryStatus(): CommandResult {
        val batt = deviceController.getBatteryInfo()
        val chargingText = if (batt.isCharging) "and currently charging via ${batt.chargingType}" else "and running on battery power"
        val spoken = "Your battery level is ${batt.percentage} percent, $chargingText."
        val details = "Charge: ${batt.percentage}% • Temp: ${batt.temperatureCelsius}°C • Voltage: ${batt.voltageMv} mV • Status: ${batt.chargingType}"

        return CommandResult(
            success = true,
            intentType = IntentType.BATTERY_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executeVolumeControl(intent: PeterIntent): CommandResult {
        val volStatus = when (intent.action.uppercase()) {
            "UP" -> deviceController.adjustVolume(1)
            "DOWN" -> deviceController.adjustVolume(-1)
            "MUTE" -> deviceController.setVolumeLevel(0)
            "MAX" -> deviceController.setVolumeLevel(100)
            "SET" -> deviceController.setVolumeLevel(intent.value)
            else -> deviceController.adjustVolume(0)
        }

        val spoken = when (intent.action.uppercase()) {
            "UP" -> "Media volume increased to ${volStatus.percentage} percent."
            "DOWN" -> "Media volume decreased to ${volStatus.percentage} percent."
            "MUTE" -> "Media audio muted."
            "MAX" -> "Media volume set to maximum."
            "SET" -> "Volume set to ${volStatus.percentage} percent."
            else -> "Current media volume is at ${volStatus.percentage} percent."
        }

        return CommandResult(
            success = true,
            intentType = IntentType.VOLUME_CONTROL,
            spokenResponse = spoken,
            displayDetails = "Media Stream: ${volStatus.currentLevel}/${volStatus.maxLevel} (${volStatus.percentage}%)"
        )
    }

    private fun executeOpenApp(intent: PeterIntent): CommandResult {
        val app = intent.targetApp.ifEmpty { "the requested app" }
        val result = deviceController.openApplication(app)

        return if (result.isSuccess) {
            CommandResult(
                success = true,
                intentType = IntentType.OPEN_APP,
                spokenResponse = "Opening $app.",
                displayDetails = "Package launch intent dispatched successfully for '$app'"
            )
        } else {
            CommandResult(
                success = false,
                intentType = IntentType.OPEN_APP,
                spokenResponse = "I could not find $app installed on this device.",
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeOpenSettings(intent: PeterIntent): CommandResult {
        val setting = intent.targetSetting.ifEmpty { "system" }
        val result = deviceController.openSettingsScreen(setting)

        return if (result.isSuccess) {
            CommandResult(
                success = true,
                intentType = IntentType.OPEN_SETTINGS,
                spokenResponse = "Opening $setting settings.",
                displayDetails = "System Settings intent executed"
            )
        } else {
            CommandResult(
                success = false,
                intentType = IntentType.OPEN_SETTINGS,
                spokenResponse = "Failed to launch $setting settings screen.",
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeTimeAndDate(): CommandResult {
        val text = deviceController.getCurrentTimeAndDate()
        return CommandResult(
            success = true,
            intentType = IntentType.TIME_AND_DATE,
            spokenResponse = text,
            displayDetails = text
        )
    }

    private fun executeAlarm(intent: PeterIntent): CommandResult {
        val hour = intent.value / 100
        val minute = intent.value % 100
        val result = deviceController.setAlarm(hour, minute, "PETER Alarm")

        return if (result.isSuccess) {
            CommandResult(
                success = true,
                intentType = IntentType.ALARM,
                spokenResponse = result.getOrNull() ?: "Alarm set.",
                displayDetails = "Alarm intent triggered for ${String.format("%02d:%02d", hour, minute)}"
            )
        } else {
            CommandResult(
                success = false,
                intentType = IntentType.ALARM,
                spokenResponse = "Unable to set alarm automatically.",
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeTimer(intent: PeterIntent): CommandResult {
        val seconds = if (intent.value > 0) intent.value else 60
        val result = deviceController.setTimer(seconds, "PETER Timer")

        return if (result.isSuccess) {
            CommandResult(
                success = true,
                intentType = IntentType.TIMER,
                spokenResponse = result.getOrNull() ?: "Timer configured.",
                displayDetails = "Timer intent triggered for $seconds seconds"
            )
        } else {
            CommandResult(
                success = false,
                intentType = IntentType.TIMER,
                spokenResponse = "Unable to set timer.",
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeNetworkStatus(): CommandResult {
        val net = deviceController.getNetworkInfo()
        val spoken = if (net.isConnected) {
            "You are connected to ${net.connectionType} with active internet access."
        } else {
            "Your device is currently offline with no active network connectivity."
        }

        val details = "State: ${if (net.isConnected) "ONLINE" else "OFFLINE"} • Type: ${net.connectionType} • Metered: ${net.isMetered} • Validated: ${net.isInternetValidated}"

        return CommandResult(
            success = true,
            intentType = IntentType.NETWORK_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executePhoneStatus(): CommandResult {
        val info = deviceController.getDeviceInfo()
        val spoken = "Device is ${info.manufacturer} ${info.model} running Android ${info.androidVersion}. Available memory is ${info.availableRamMb} megabytes out of ${info.totalRamMb} megabytes."
        val details = "Hardware: ${info.manufacturer} ${info.model} • Android: ${info.androidVersion} (API ${info.sdkInt}) • RAM: ${info.availableRamMb}MB / ${info.totalRamMb}MB • Uptime: ${info.uptimeFormatted}"

        return CommandResult(
            success = true,
            intentType = IntentType.PHONE_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executeMediaControl(intent: PeterIntent): CommandResult {
        return CommandResult(
            success = true,
            intentType = IntentType.MEDIA_CONTROL,
            spokenResponse = "Media playback command received.",
            displayDetails = "Media control action: ${intent.action}"
        )
    }
}
