package com.example.domain.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface DeviceController {
    fun setFlashlight(enabled: Boolean): Result<Boolean>
    fun isFlashlightAvailable(): Boolean
    fun getBatteryInfo(): BatteryStatus
    fun adjustVolume(direction: Int): VolumeStatus // 1 = UP, -1 = DOWN, 0 = query
    fun setVolumeLevel(levelPercent: Int): VolumeStatus
    fun openApplication(appName: String): Result<String>
    fun openSettingsScreen(settingName: String): Result<String>
    fun setAlarm(hour: Int, minute: Int, message: String): Result<String>
    fun setTimer(lengthSeconds: Int, message: String): Result<String>
    fun getNetworkInfo(): NetworkStatus
    fun getDeviceInfo(): DeviceInfo
    fun getCurrentTimeAndDate(): String
}

data class BatteryStatus(
    val percentage: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val temperatureCelsius: Float,
    val voltageMv: Int
)

data class VolumeStatus(
    val currentLevel: Int,
    val maxLevel: Int,
    val percentage: Int
)

data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: String, // "Wi-Fi", "Cellular", "Ethernet", "None"
    val isMetered: Boolean,
    val isInternetValidated: Boolean
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val uptimeFormatted: String
)

class AndroidDeviceController(private val context: Context) : DeviceController {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private var isTorchOn = false

    override fun isFlashlightAvailable(): Boolean {
        return try {
            val cm = cameraManager ?: return false
            cm.cameraIdList.any { id ->
                val characteristics = cm.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun setFlashlight(enabled: Boolean): Result<Boolean> {
        return try {
            val cm = cameraManager ?: return Result.failure(IllegalStateException("Camera service unavailable"))
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("No flashlight hardware detected on this device"))

            cm.setTorchMode(cameraId, enabled)
            isTorchOn = enabled
            Result.success(enabled)
        } catch (e: CameraAccessException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getBatteryInfo(): BatteryStatus {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery Discharging"
        }

        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempRaw / 10f
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        return BatteryStatus(
            percentage = percentage,
            isCharging = isCharging,
            chargingType = plugType,
            temperatureCelsius = tempCelsius,
            voltageMv = voltage
        )
    }

    override fun adjustVolume(direction: Int): VolumeStatus {
        val am = audioManager ?: return VolumeStatus(0, 0, 0)
        val stream = AudioManager.STREAM_MUSIC
        if (direction > 0) {
            am.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        } else if (direction < 0) {
            am.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        val current = am.getStreamVolume(stream)
        val max = am.getStreamMaxVolume(stream)
        val pct = if (max > 0) (current * 100 / max) else 0
        return VolumeStatus(current, max, pct)
    }

    override fun setVolumeLevel(levelPercent: Int): VolumeStatus {
        val am = audioManager ?: return VolumeStatus(0, 0, 0)
        val stream = AudioManager.STREAM_MUSIC
        val max = am.getStreamMaxVolume(stream)
        val clampedPercent = levelPercent.coerceIn(0, 100)
        val target = (max * clampedPercent) / 100
        am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
        val current = am.getStreamVolume(stream)
        val pct = if (max > 0) (current * 100 / max) else 0
        return VolumeStatus(current, max, pct)
    }

    override fun openApplication(appName: String): Result<String> {
        val pm = context.packageManager
        val query = appName.trim().lowercase()

        // 1. Direct package matches for popular apps
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "clock" to "com.google.android.deskclock",
            "settings" to "com.android.settings",
            "spotify" to "com.spotify.music",
            "whatsapp" to "com.whatsapp",
            "gmail" to "com.google.android.gm",
            "messages" to "com.google.android.apps.messaging",
            "photos" to "com.google.android.apps.photos",
            "files" to "com.google.android.apps.nbu.files",
            "contacts" to "com.google.android.contacts",
            "calendar" to "com.google.android.calendar"
        )

        val targetPkg = knownPackages[query]
        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Result.success("Opening $appName")
            }
        }

        // 2. Scan installed apps for name match
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            val matched = apps.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                label.contains(query) || query.contains(label)
            }

            if (matched != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Result.success("Opening ${matched.loadLabel(pm)}")
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.failure(IllegalArgumentException("Application '$appName' is not installed on this device"))
    }

    override fun openSettingsScreen(settingName: String): Result<String> {
        val action = when (settingName.trim().lowercase()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "date", "time" -> Settings.ACTION_DATE_SETTINGS
            "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "applications", "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "privacy", "security" -> Settings.ACTION_SECURITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opening ${settingName.ifEmpty { "system" }} settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setAlarm(hour: Int, minute: Int, message: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifEmpty { "PETER Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            Result.success("Alarm configured for $timeStr")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setTimer(lengthSeconds: Int, message: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, lengthSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifEmpty { "PETER Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = lengthSeconds / 60
            val seconds = lengthSeconds % 60
            val durStr = if (minutes > 0) "$minutes minute${if (minutes > 1) "s" else ""}" else "$seconds seconds"
            Result.success("Timer set for $durStr")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNetworkInfo(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus(false, "None", isMetered = false, isInternetValidated = false)
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)

        if (capabilities == null) {
            return NetworkStatus(false, "None", isMetered = false, isInternetValidated = false)
        }

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected Network"
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        return NetworkStatus(
            isConnected = hasInternet,
            connectionType = type,
            isMetered = isMetered,
            isInternetValidated = isValidated
        )
    }

    override fun getDeviceInfo(): DeviceInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val availMb = memInfo.availMem / (1024 * 1024)
        val totalMb = memInfo.totalMem / (1024 * 1024)

        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = (uptimeMs / (1000 * 60 * 60)) % 24
        val days = uptimeMs / (1000 * 60 * 60 * 24)
        val uptimeStr = if (days > 0) "${days}d ${hours}h" else "${hours}h"

        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            availableRamMb = availMb,
            totalRamMb = totalMb,
            uptimeFormatted = uptimeStr
        )
    }

    override fun getCurrentTimeAndDate(): String {
        val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
        val sdfDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val now = Date()
        return "It is currently ${sdfTime.format(now)} on ${sdfDate.format(now)}."
    }
}
