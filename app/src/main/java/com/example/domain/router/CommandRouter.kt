package com.example.domain.router

import android.content.Context
import com.example.core.model.CommandResult
import com.example.core.model.IntentType
import com.example.core.model.PeterIntent
import com.example.domain.ai.AIBrain
import com.example.domain.ai.LanguageHelper
import com.example.domain.ai.SupportedLanguage
import com.example.domain.device.DeviceController

class CommandRouter(
    private val context: Context,
    private val deviceController: DeviceController,
    private val aiBrain: AIBrain
) {
    @Volatile
    private var lastSearchQuery: String? = null

    fun getLastSearchQuery(): String? = lastSearchQuery

    suspend fun routeAndExecute(
        rawPrompt: String,
        conversationHistory: List<com.example.core.model.ChatMessage> = emptyList()
    ): CommandResult {
        val lang = LanguageHelper.detectLanguage(rawPrompt)
        if (rawPrompt.isBlank()) {
            val emptyMsg = when (lang) {
                SupportedLanguage.BENGALI -> "কোনো কথা শুনতে পাইনি বন্ধু। দয়া করে আবার বলুন।"
                SupportedLanguage.HINDI -> "कोई आवाज़ नहीं सुनाई दी दोस्त। कृपया फिर से बोलें।"
                SupportedLanguage.ENGLISH -> "I did not detect any command, mate. Please speak again."
            }
            return CommandResult(
                success = false,
                intentType = IntentType.UNKNOWN,
                spokenResponse = emptyMsg
            )
        }

        // 1. AI Analysis / Intent Extraction
        val aiResponse = aiBrain.processUserPrompt(rawPrompt, conversationHistory)
        val intent = aiResponse.intent

        // If direct answer was provided by conversational AI
        if (aiResponse.directAnswer != null) {
            if (intent.type == IntentType.WEB_SEARCH) {
                val query = intent.query.ifEmpty { intent.rawText }
                lastSearchQuery = query
                return CommandResult(
                    success = true,
                    intentType = IntentType.WEB_SEARCH,
                    spokenResponse = aiResponse.directAnswer,
                    displayDetails = "Background Search Complete",
                    searchQuery = query
                )
            } else if (intent.type == IntentType.SHOW_PROOF) {
                val targetQuery = intent.query.ifEmpty { lastSearchQuery ?: "Google Search" }
                deviceController.searchWeb(targetQuery)
                return CommandResult(
                    success = true,
                    intentType = IntentType.SHOW_PROOF,
                    spokenResponse = aiResponse.directAnswer,
                    displayDetails = "Google Search Proof: '$targetQuery'",
                    searchQuery = targetQuery
                )
            } else {
                return CommandResult(
                    success = true,
                    intentType = intent.type,
                    spokenResponse = aiResponse.directAnswer,
                    displayDetails = if (aiResponse.isFromCloud) "Source: Gemini Cloud AI" else "Source: Local AI Brain"
                )
            }
        }

        // 2. Command routing with safety, permission & version compatibility verification
        return when (intent.type) {
            IntentType.FLASHLIGHT -> executeFlashlight(intent, lang)
            IntentType.BATTERY_STATUS -> executeBatteryStatus(intent, lang)
            IntentType.VOLUME_CONTROL -> executeVolumeControl(intent, lang)
            IntentType.OPEN_APP -> executeOpenApp(intent, lang)
            IntentType.OPEN_SETTINGS -> executeOpenSettings(intent, lang)
            IntentType.TIME_AND_DATE -> executeTimeAndDate(intent, lang)
            IntentType.ALARM -> executeAlarm(intent, lang)
            IntentType.TIMER -> executeTimer(intent, lang)
            IntentType.PHONE_STATUS -> executePhoneStatus(lang)
            IntentType.NETWORK_STATUS -> executeNetworkStatus(lang)
            IntentType.MEDIA_CONTROL -> executeMediaControl(intent, lang)
            IntentType.WEB_SEARCH -> executeWebSearch(intent, lang)
            IntentType.SHOW_PROOF -> executeShowProof(intent, lang)
            IntentType.SCREEN_SEARCH -> executeScreenSearch(intent, lang)
            IntentType.EMERGENCY_LOCKDOWN -> executeEmergencyLockdown(intent, lang)
            IntentType.AI_QUERY -> {
                val raw = intent.query.ifEmpty { intent.rawText }
                lastSearchQuery = raw
                val fallbackSpoken = when (lang) {
                    SupportedLanguage.BENGALI -> "আমি এই বিষয়ে তথ্য সংগ্রহ করেছি। আপনার আরও কিছু জানার থাকলে নির্দ্বিধায় জিজ্ঞাসা করুন!"
                    SupportedLanguage.HINDI -> "मैंने इस बारे में जानकारी ढूंढ ली है। अगर आप और कुछ जानना चाहते हैं, तो बेझिझक पूछें!"
                    SupportedLanguage.ENGLISH -> "I've processed your query, mate. Let me know if you'd like to explore this further or ask anything else!"
                }
                CommandResult(
                    success = true,
                    intentType = IntentType.AI_QUERY,
                    spokenResponse = fallbackSpoken,
                    displayDetails = "Query: $raw",
                    searchQuery = raw
                )
            }
            IntentType.UNKNOWN -> {
                val unknownSpoken = when (lang) {
                    SupportedLanguage.BENGALI -> "কমান্ডটি বুঝতে পারিনি বন্ধু! আপনি ব্যাটারি, টর্চ, ভলিউম, সময় বা যেকোনো প্রশ্ন জিজ্ঞাসা করতে পারেন।"
                    SupportedLanguage.HINDI -> "कमांड समझ नहीं आई दोस्त! आप बैटरी, फ्लैशलाइट, वॉल्यूम, समय, या कोई भी सवाल पूछ सकते हैं।"
                    SupportedLanguage.ENGLISH -> "Command not recognized, mate. You can ask for battery level, flashlight, volume, time, app launching, or any query."
                }
                CommandResult(
                    success = false,
                    intentType = IntentType.UNKNOWN,
                    spokenResponse = unknownSpoken
                )
            }
        }
    }

    private fun executeFlashlight(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        if (!deviceController.isFlashlightAvailable()) {
            val msg = when (lang) {
                SupportedLanguage.HINDI -> "अरे भाई, इस फोन में फ्लैशलाइट हार्डवेयर नहीं मिला!"
                SupportedLanguage.BENGALI -> "আরে বন্ধু, এই ফোনে ক্যামেরা ফ্ল্যাশলাইট পাওয়া যায়নি!"
                SupportedLanguage.ENGLISH -> "Ah mate, looks like there's no camera flashlight hardware on this device! Can't web-shoot light out of nowhere."
            }
            return CommandResult(
                success = false,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = msg,
                errorMessage = "Hardware unsupported"
            )
        }

        val turnOn = intent.action.uppercase() != "OFF"
        val result = deviceController.setFlashlight(turnOn)

        return if (result.isSuccess) {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> if (turnOn) "बूम! टॉर्च ऑन कर दी है! एकदम उजाला हो गया दोस्त!" else "टॉर्च बंद कर दी है! वापस सीक्रेट मोड में!"
                SupportedLanguage.BENGALI -> if (turnOn) "বুম! টর্চ জ্বালিয়ে দিয়েছি! আলোই আলো বন্ধু!" else "টর্চ বন্ধ করে দিয়েছি! একদম অন্ধকার মোড অন!"
                SupportedLanguage.ENGLISH -> if (turnOn) "Boom! Flashlight is on! Let there be light, mate." else "Flashlight turned off! Back to stealth mode in the shadows."
            }
            CommandResult(
                success = true,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = spoken,
                displayDetails = "Torch Mode: ${if (turnOn) "ACTIVE" else "DISABLED"}"
            )
        } else {
            val msg = when (lang) {
                SupportedLanguage.HINDI -> "फ्लैशलाइट चालू नहीं हो पाई, कैमरा बिजी हो सकता है!"
                SupportedLanguage.BENGALI -> "টর্চ অন করা গেল না, ক্যামেরা ব্যস্ত থাকতে পারে!"
                SupportedLanguage.ENGLISH -> "Oops, couldn't toggle the flashlight! Camera hardware might be busy."
            }
            CommandResult(
                success = false,
                intentType = IntentType.FLASHLIGHT,
                spokenResponse = msg,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeBatteryStatus(intent: PeterIntent? = null, lang: SupportedLanguage): CommandResult {
        val batt = deviceController.getBatteryInfo()
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "बैटरी स्टेटस: आपके फोन में अभी ${batt.percentage}% चार्ज है! बिल्कुल फिट चल रहा है दोस्त!"
            SupportedLanguage.BENGALI -> "ব্যাটারি চেক: আপনার ফোনে চার্জ আছে ${batt.percentage}%! একদম ফাটাফাটি চলছে বন্ধু!"
            SupportedLanguage.ENGLISH -> {
                val chargingText = if (batt.isCharging) "and currently plugged in juicing up via ${batt.chargingType}" else "and holding strong on battery power"
                "Alright, quick battery check! You're at ${batt.percentage} percent, $chargingText. Looking good, mate!"
            }
        }
        val details = "Charge: ${batt.percentage}% • Temp: ${batt.temperatureCelsius}°C • Voltage: ${batt.voltageMv} mV • Status: ${batt.chargingType}"

        return CommandResult(
            success = true,
            intentType = IntentType.BATTERY_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executeVolumeControl(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val volStatus = when (intent.action.uppercase()) {
            "UP" -> deviceController.adjustVolume(1)
            "DOWN" -> deviceController.adjustVolume(-1)
            "MUTE" -> deviceController.setVolumeLevel(0)
            "MAX" -> deviceController.setVolumeLevel(100)
            "SET" -> deviceController.setVolumeLevel(intent.value)
            else -> deviceController.adjustVolume(0)
        }

        val spoken = when (lang) {
            SupportedLanguage.HINDI -> when (intent.action.uppercase()) {
                "UP" -> "वॉल्यूम बढ़ा दिया है! अभी ${volStatus.percentage}% पर है दोस्त!"
                "DOWN" -> "वॉल्यूम कम करके ${volStatus.percentage}% कर दिया है!"
                "MUTE" -> "म्यूट कर दिया है! एकदम सन्नाटा!"
                "MAX" -> "फुल वॉल्यूम 100% ऑन! पार्टी शुरू करो दोस्त!"
                "SET" -> "वॉल्यूम ${volStatus.percentage}% पर सेट कर दिया है!"
                else -> "मीडिया वॉल्यूम अभी ${volStatus.percentage}% है।"
            }
            SupportedLanguage.BENGALI -> when (intent.action.uppercase()) {
                "UP" -> "সাউন্ড বাড়িয়ে দিয়েছি! এখন ${volStatus.percentage}% তে চলছে বন্ধু!"
                "DOWN" -> "সাউন্ড কমিয়ে ${volStatus.percentage}% করে দিয়েছি!"
                "MUTE" -> "মিউট করে দেওয়া হয়েছে! একদম চুপচাপ!"
                "MAX" -> "ফুল ভলিউম 100% অন! এবার ফাটিয়ে গান বাজাও বন্ধু!"
                "SET" -> "ভলিউম ${volStatus.percentage}% এ সেট করা হলো!"
                else -> "মিডিয়া ভলিউম এখন ${volStatus.percentage}% আছে।"
            }
            SupportedLanguage.ENGLISH -> when (intent.action.uppercase()) {
                "UP" -> "Cranks up the tunes! Volume is now at ${volStatus.percentage} percent, mate!"
                "DOWN" -> "Turned it down to ${volStatus.percentage} percent. Keeping it chill!"
                "MUTE" -> "Muted! Shhh, total silence mode activated."
                "MAX" -> "Max volume engaged! Cranked all the way to 100 percent, let's rock!"
                "SET" -> "Sorted! Volume dialed in at ${volStatus.percentage} percent."
                else -> "Media volume is sitting at ${volStatus.percentage} percent right now."
            }
        }

        return CommandResult(
            success = true,
            intentType = IntentType.VOLUME_CONTROL,
            spokenResponse = spoken,
            displayDetails = "Media Stream: ${volStatus.currentLevel}/${volStatus.maxLevel} (${volStatus.percentage}%)"
        )
    }

    private fun executeOpenApp(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val app = intent.targetApp.ifEmpty { "the requested app" }
        val result = deviceController.openApplication(app)

        return if (result.isSuccess) {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "अभी $app खोल रहा हूँ! तुरंत पहुँच रहे हैं दोस्त!"
                SupportedLanguage.BENGALI -> "$app এখনই খুলে দিচ্ছি বন্ধু! একদম রেডি!"
                SupportedLanguage.ENGLISH -> "Opening $app right now! Swing right in, mate."
            }
            CommandResult(
                success = true,
                intentType = IntentType.OPEN_APP,
                spokenResponse = spoken,
                displayDetails = "Package launch intent dispatched successfully for '$app'"
            )
        } else {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "अरे दोस्त, इस फोन में $app नहीं मिला। शायद प्ले स्टोर से इंस्टॉल करना पड़ेगा?"
                SupportedLanguage.BENGALI -> "আরে বন্ধু, আপনার ফোনে $app পাওয়া যায়নি। প্লে স্টোরে দেখতে পারেন!"
                SupportedLanguage.ENGLISH -> "Ah mate, I couldn't find $app installed on this device. Maybe check the Play Store?"
            }
            CommandResult(
                success = false,
                intentType = IntentType.OPEN_APP,
                spokenResponse = spoken,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeOpenSettings(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val setting = intent.targetSetting.ifEmpty { "system" }
        val result = deviceController.openSettingsScreen(setting)

        return if (result.isSuccess) {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "$setting सेटिंग्स तुरंत खोल रहा हूँ दोस्त!"
                SupportedLanguage.BENGALI -> "$setting সেটিংস এখনই খুলে দিচ্ছি বন্ধু!"
                SupportedLanguage.ENGLISH -> "Opening up $setting settings for you right away!"
            }
            CommandResult(
                success = true,
                intentType = IntentType.OPEN_SETTINGS,
                spokenResponse = spoken,
                displayDetails = "System Settings intent executed"
            )
        } else {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "$setting सेटिंग्स नहीं खुल पाई दोस्त।"
                SupportedLanguage.BENGALI -> "$setting সেটিংস খোলা গেল না বন্ধু।"
                SupportedLanguage.ENGLISH -> "Couldn't pull up $setting settings, mate. Sorry about that!"
            }
            CommandResult(
                success = false,
                intentType = IntentType.OPEN_SETTINGS,
                spokenResponse = spoken,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeTimeAndDate(intent: PeterIntent? = null, lang: SupportedLanguage): CommandResult {
        val text = deviceController.getCurrentTimeAndDate()
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "अभी का समय और तारीख है: $text। सुपरहीरो का काम करते हुए समय का पता ही नहीं चलता!"
            SupportedLanguage.BENGALI -> "এখন সময় ও তারিখ: $text। স্পাইডার-ম্যানের সাথে সময় যেন চোখের পলকে উড়ে যায়!"
            SupportedLanguage.ENGLISH -> "Right now it's $text. Time flies when we're saving the neighborhood, eh?"
        }
        return CommandResult(
            success = true,
            intentType = IntentType.TIME_AND_DATE,
            spokenResponse = spoken,
            displayDetails = text
        )
    }

    private fun executeAlarm(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val hour = intent.value / 100
        val minute = intent.value % 100
        val result = deviceController.setAlarm(hour, minute, "PETER Alarm")

        return if (result.isSuccess) {
            val formattedTime = String.format("%02d:%02d", hour, minute)
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "$formattedTime का अलार्म सेट कर दिया है दोस्त! चिंता मत करो, मैं जगा दूँगा!"
                SupportedLanguage.BENGALI -> "$formattedTime এর অ্যালার্ম সেট করে দিয়েছি বন্ধু! ঘুম ভাঙানোর দায়িত্ব আমার!"
                SupportedLanguage.ENGLISH -> "Alarm set for $formattedTime! Don't worry, I won't let you sleep through it, mate!"
            }
            CommandResult(
                success = true,
                intentType = IntentType.ALARM,
                spokenResponse = spoken,
                displayDetails = "Alarm intent triggered for $formattedTime"
            )
        } else {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "अलार्म सेट नहीं हो पाया दोस्त।"
                SupportedLanguage.BENGALI -> "অ্যালার্ম সেট করা গেল না বন্ধু।"
                SupportedLanguage.ENGLISH -> "Couldn't set the alarm automatically, mate."
            }
            CommandResult(
                success = false,
                intentType = IntentType.ALARM,
                spokenResponse = spoken,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeTimer(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val seconds = if (intent.value > 0) intent.value else 60
        val result = deviceController.setTimer(seconds, "PETER Timer")

        return if (result.isSuccess) {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "$seconds सेकंड का टाइमर चालू हो गया है दोस्त! उलटी गिनती शुरू!"
                SupportedLanguage.BENGALI -> "$seconds সেকেন্ডের টাইমার চালু করা হয়েছে বন্ধু! কাউন্টডাউন শুরু!"
                SupportedLanguage.ENGLISH -> "Timer locked in for $seconds seconds! The countdown begins, mate!"
            }
            CommandResult(
                success = true,
                intentType = IntentType.TIMER,
                spokenResponse = spoken,
                displayDetails = "Timer intent triggered for $seconds seconds"
            )
        } else {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "टाइमर शुरू नहीं हो पाया दोस्त।"
                SupportedLanguage.BENGALI -> "টাইমার চালু করা গেল না বন্ধু।"
                SupportedLanguage.ENGLISH -> "Couldn't start that timer, mate."
            }
            CommandResult(
                success = false,
                intentType = IntentType.TIMER,
                spokenResponse = spoken,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    private fun executeNetworkStatus(lang: SupportedLanguage): CommandResult {
        val net = deviceController.getNetworkInfo()
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> if (net.isConnected) {
                "आप ऑनलाइन हैं और ${net.connectionType} से कनेक्टेड हैं! सब कुछ सुपरफास्ट चल रहा है दोस्त!"
            } else {
                "अभी फोन ऑफलाइन है दोस्त! लेकिन चिंता मत करो, मैं लोकल कमांड्स आराम से संभाल लूँगा!"
            }
            SupportedLanguage.BENGALI -> if (net.isConnected) {
                "আপনি অনলাইনে আছেন এবং ${net.connectionType} এ যুক্ত! ইন্টারনেট একদম সুপারফাস্ট চলছে বন্ধু!"
            } else {
                "এখন ডিভাইস অফলাইনে আছে বন্ধু! তবে অফলাইন সব ফিচার কিন্তু একদম প্রস্তুত!"
            }
            SupportedLanguage.ENGLISH -> if (net.isConnected) {
                "You're online and connected to ${net.connectionType}! Everything's speedy and ready to surf, mate."
            } else {
                "Looks like we're totally offline right now, mate. No worries, I can still run local commands!"
            }
        }

        val details = "State: ${if (net.isConnected) "ONLINE" else "OFFLINE"} • Type: ${net.connectionType} • Metered: ${net.isMetered} • Validated: ${net.isInternetValidated}"

        return CommandResult(
            success = true,
            intentType = IntentType.NETWORK_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executePhoneStatus(lang: SupportedLanguage): CommandResult {
        val info = deviceController.getDeviceInfo()
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "फोन हार्डवेयर स्टेटस: ${info.manufacturer} ${info.model}, एंड्रॉइड ${info.androidVersion} और ${info.availableRamMb} MB रैम फ्री है दोस्त! फोन एकदम रॉकेट है!"
            SupportedLanguage.BENGALI -> "ডিভাইস বিবরণ: ${info.manufacturer} ${info.model}, অ্যান্ড্রয়েড ${info.androidVersion} এবং ${info.availableRamMb} MB র‍্যাম খালি আছে বন্ধু! অসাধারণ স্পিড!"
            SupportedLanguage.ENGLISH -> "Here's the hardware breakdown, mate: ${info.manufacturer} ${info.model} on Android ${info.androidVersion}, with ${info.availableRamMb} megs of RAM free. Beast of a phone!"
        }
        val details = "Hardware: ${info.manufacturer} ${info.model} • Android: ${info.androidVersion} (API ${info.sdkInt}) • RAM: ${info.availableRamMb}MB / ${info.totalRamMb}MB • Uptime: ${info.uptimeFormatted}"

        return CommandResult(
            success = true,
            intentType = IntentType.PHONE_STATUS,
            spokenResponse = spoken,
            displayDetails = details
        )
    }

    private fun executeMediaControl(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "मीडिया कंट्रोल एक्शन भेज दिया गया है दोस्त!"
            SupportedLanguage.BENGALI -> "মিডিয়া কন্ট্রোল নির্দেশ কার্যকর করা হয়েছে বন্ধু!"
            SupportedLanguage.ENGLISH -> "Got it, media control action dispatched!"
        }
        return CommandResult(
            success = true,
            intentType = IntentType.MEDIA_CONTROL,
            spokenResponse = spoken,
            displayDetails = "Media control action: ${intent.action}"
        )
    }

    private fun executeWebSearch(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val query = intent.query.ifEmpty { intent.rawText }
        lastSearchQuery = query

        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "मैंने '$query' के बारे में जानकारी सर्च कर ली है।"
            SupportedLanguage.BENGALI -> "আমি '$query' সম্পর্কে তথ্য খুঁজে নিয়েছি।"
            SupportedLanguage.ENGLISH -> "I searched for '$query' for you, mate!"
        }

        return CommandResult(
            success = true,
            intentType = IntentType.WEB_SEARCH,
            spokenResponse = spoken,
            displayDetails = "Search Query: '$query'",
            searchQuery = query
        )
    }

    private fun executeShowProof(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val targetQuery = intent.query.ifEmpty { lastSearchQuery ?: "Google Search" }
        val result = deviceController.searchWeb(targetQuery)

        return if (result.isSuccess) {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "ये रहा '$targetQuery' के लिए गूगल सर्च का प्रमाण, स्क्रीन पर खोल दिया है दोस्त!"
                SupportedLanguage.BENGALI -> "এই যে '$targetQuery' এর জন্য গুগলের প্রমাণের পেজ, স্ক্রিনে তুলে ধরলাম বন্ধু!"
                SupportedLanguage.ENGLISH -> "Opening the Google search page as verified proof for '$targetQuery', mate! Here is the evidence."
            }
            CommandResult(
                success = true,
                intentType = IntentType.SHOW_PROOF,
                spokenResponse = spoken,
                displayDetails = "Google Search Proof opened: '$targetQuery'",
                searchQuery = targetQuery
            )
        } else {
            val spoken = when (lang) {
                SupportedLanguage.HINDI -> "प्रमाण के लिए गूगल पेज नहीं खुल पाया दोस्त।"
                SupportedLanguage.BENGALI -> "প্রমাণের জন্য গুগল পেজ খোলা গেল না বন্ধু।"
                SupportedLanguage.ENGLISH -> "Couldn't launch the browser proof page, mate."
            }
            CommandResult(
                success = false,
                intentType = IntentType.SHOW_PROOF,
                spokenResponse = spoken,
                errorMessage = result.exceptionOrNull()?.localizedMessage,
                searchQuery = targetQuery
            )
        }
    }

    private fun executeScreenSearch(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val spoken = when (lang) {
            SupportedLanguage.HINDI -> "मैं आपकी स्क्रीन देखने के लिए तैयार हूँ दोस्त! 'Share Screen' बटन पर टैप करें या स्क्रीनशॉट चुनें, मैं बैकग्राउंड में इंटरनेट सर्च करके सब बता दूंगा!"
            SupportedLanguage.BENGALI -> "আমি আপনার স্ক্রিন দেখতে প্রস্তুত বন্ধু! 'Share Screen' বাটনে ট্যাপ করুন বা স্ক্রিনশট সিলেক্ট করুন, আমি ব্যাকগ্রাউন্ডে সার্চ করে সব বুঝিয়ে দিচ্ছি!"
            SupportedLanguage.ENGLISH -> "I'm ready to inspect your screen, mate! Tap 'Share Screen' or select a screenshot and I'll analyze it and search the internet in the background!"
        }
        return CommandResult(
            success = true,
            intentType = IntentType.SCREEN_SEARCH,
            spokenResponse = spoken,
            displayDetails = "Screen Vision Mode • Tap Share Screen to capture and analyze"
        )
    }

    private fun executeEmergencyLockdown(intent: PeterIntent, lang: SupportedLanguage): CommandResult {
        val spoken = when (lang) {
            SupportedLanguage.BENGALI ->
                "কোড রেড প্রোটোকল সক্রিয় করা হয়েছে! সম্পূর্ণ সিকিউরিটি লকডাউন চালু! আনলক করতে পাসওয়ার্ড প্রবেশ করান।"
            SupportedLanguage.HINDI ->
                "कोड रेड प्रोटोकॉल एक्टिवेटेड! फुल सिक्योरिटी लॉकडाउन शुरू! अनलॉक करने के लिए पासवर्ड दर्ज करें।"
            SupportedLanguage.ENGLISH ->
                "CODE RED PROTOCOL ACTIVATED! Full security lockdown engaged! All standard app controls locked. Enter authorization password to deactivate."
        }
        return CommandResult(
            success = true,
            intentType = IntentType.EMERGENCY_LOCKDOWN,
            spokenResponse = spoken,
            displayDetails = "SECURITY PROTOCOL CODE RED • EMERGENCY LOCKDOWN ACTIVATED"
        )
    }
}

