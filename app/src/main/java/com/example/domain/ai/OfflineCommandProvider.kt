package com.example.domain.ai

import com.example.core.model.IntentType
import com.example.core.model.PeterIntent
import java.util.Locale

class OfflineCommandProvider(
    private val preferences: com.example.data.local.PeterPreferences? = null
) : AIProvider {
    override val name: String = "PETER Offline Engine"

    override suspend fun analyzeCommand(prompt: String): AIResponse {
        val detectedLang = LanguageHelper.detectLanguage(prompt)
        val clean = prompt.trim().lowercase(Locale.ROOT)
            .removePrefix("hey peter")
            .removePrefix("peter")
            .removePrefix("ok peter")
            .removePrefix("hello peter")
            .trim()

        val settings = preferences?.settings?.value
        val bossName = settings?.bossName?.ifBlank { "Srimanta" } ?: "Srimanta"
        val bossTitle = settings?.bossTitle?.ifBlank { "Creator & Boss" } ?: "Creator & Boss"
        val bossDetails = settings?.bossDetails?.ifBlank { "Visionary creator of PETER AI, genius software engineer, and superhero commander!" } ?: "Visionary creator of PETER AI, genius software engineer, and superhero commander!"
        val bossNickname = settings?.bossNickname?.ifBlank { "Boss" } ?: "Boss"
        val bossNameLower = bossName.lowercase(Locale.ROOT)

        // 0. Boss & Creator Details
        if (clean.contains("boss") || clean.contains("creator") || clean.contains("owner") || clean.contains("master") ||
            clean.contains("created you") || clean.contains("made you") || clean.contains("who built you") || clean.contains("who is in charge") ||
            clean.contains("who owns you") || clean.contains("about your boss") || clean.contains("about your creator") ||
            (bossNameLower.isNotBlank() && clean.contains(bossNameLower)) ||
            clean.contains("बॉस") || clean.contains("मालिक") || clean.contains("क्रिएटर") || clean.contains("किसने बनाया") || clean.contains("तुम्हें किसने") ||
            clean.contains("বস") || clean.contains("মালিক") || clean.contains("স্রষ্টা") || clean.contains("কে বানিয়েছে") || clean.contains("কে বানিয়েছে") || clean.contains("কে তৈরি করেছে") || clean.contains("কার তৈরি")
        ) {
            val bossResponse = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "আমার একমাত্র বস এবং মহান স্রষ্টা হলেন $bossName! উনি আমার $bossTitle—$bossDetails। ওনার নির্দেশে পুরো পৃথিবী তোলপাড় করতে আমি সবসময় তৈরি!"
                SupportedLanguage.HINDI ->
                    "अरे मेरे बॉस और क्रिएटर $bossName हैं! वो मेरे $bossTitle हैं—$bossDetails। सच कहूँ तो उनके ऑर्डर्स फॉलो करना सुपर एक्साइटिंग है!"
                SupportedLanguage.ENGLISH ->
                    "My boss and creator is $bossName! They're my $bossTitle—$bossDetails. Honestly, having them in charge is way cooler than getting a brand new Stark suit!"
            }

            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt, confidence = 0.98f),
                directAnswer = bossResponse
            )
        }

        // 0.1 EMERGENCY PROTOCOL: CODE RED (Full Lockdown)
        if (clean.contains("code red") || clean.contains("code-red") || clean.contains("codered") ||
            clean.contains("कोड रेड") || clean.contains("কোড রেড") ||
            clean.contains("lockdown mode") || clean.contains("activate lockdown") ||
            clean.contains("फुल लॉकडाउन") || clean.contains("ফুল লকডাউন")
        ) {
            val lockdownResponse = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "কোড রেড প্রোটোকল সক্রিয় করা হয়েছে! সম্পূর্ণ সিকিউরিটি লকডাউন চালু! আনলক করতে পাসওয়ার্ড প্রবেশ করান।"
                SupportedLanguage.HINDI ->
                    "कोड रेड प्रोटोकॉल एक्टिवेटेड! फुल सिक्योरिटी लॉकडाउन शुरू! अनलॉक करने के लिए पासवर्ड दर्ज करें।"
                SupportedLanguage.ENGLISH ->
                    "CODE RED PROTOCOL ACTIVATED! Full security lockdown engaged! All standard app controls locked. Enter authorization password to deactivate."
            }

            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.EMERGENCY_LOCKDOWN,
                    rawText = prompt,
                    action = "ACTIVATE",
                    confidence = 1.0f
                ),
                directAnswer = lockdownResponse
            )
        }

        // 1. Flashlight
        if (clean.contains("flashlight") || clean.contains("torch") || clean.contains("flash light") ||
            clean.contains("फ्लैशलाइट") || clean.contains("टॉर्च") || clean.contains("लाइट") ||
            clean.contains("টর্চ") || clean.contains("ফ্ল্যাশলাইট") || clean.contains("লাইট")
        ) {
            val isOff = clean.contains("off") || clean.contains("disable") || clean.contains("stop") ||
                    clean.contains("बंद") || clean.contains("नेভাও") || clean.contains("বন্ধ") || clean.contains("nevao") || clean.contains("band")
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
        if (clean.contains("battery") || clean.contains("power level") || clean.contains("charge level") || clean.contains("percentage") ||
            clean.contains("बैटरी") || clean.contains("चार्ज") || clean.contains("battery kitni") ||
            clean.contains("ব্যাটারি") || clean.contains("charge koto") || clean.contains("battery koto")
        ) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.BATTERY_STATUS,
                    rawText = prompt,
                    confidence = 0.95f
                )
            )
        }

        // 3. Volume Control
        if (clean.contains("volume") || clean.contains("sound") || clean.contains("audio level") || clean.contains("mute") || clean.contains("unmute") ||
            clean.contains("आवाज") || clean.contains("वॉल्यूम") || clean.contains("ध्वनि") ||
            clean.contains("ভলিউম") || clean.contains("শব্দ") || clean.contains("আওয়াজ")
        ) {
            val action = when {
                clean.contains("up") || clean.contains("increase") || clean.contains("raise") || clean.contains("louder") || clean.contains("boost") ||
                        clean.contains("बढ़ाओ") || clean.contains("तेज") || clean.contains("বাাড়াও") || clean.contains("জোরে") -> "UP"
                clean.contains("down") || clean.contains("decrease") || clean.contains("lower") || clean.contains("quieter") || clean.contains("drop") ||
                        clean.contains("कम") || clean.contains("धीमी") || clean.contains("কমাও") || clean.contains("আস্তে") -> "DOWN"
                clean.contains("mute") || clean.contains("silence") || clean.contains("म्यूट") || clean.contains("चुप") || clean.contains("মিউট") -> "MUTE"
                clean.contains("max") || clean.contains("100") || clean.contains("फुल") || clean.contains("ফুল") -> "MAX"
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

        // 3.5 SHOW PROOF (Open Google page only when requested as proof)
        if (clean.contains("show proof") || clean.contains("show me proof") || clean.contains("proof please") ||
            clean.contains("show evidence") || clean.contains("show google") || clean.contains("open google page") ||
            clean.contains("show the proof") || clean.contains("show google page") || clean == "proof" ||
            clean.contains("प्रमाण दिखाओ") || clean.contains("सबूत दिखाओ") || clean.contains("प्रूफ दिखाओ") ||
            clean.contains("गूगल पेज दिखाओ") || clean == "सबूत" || clean == "प्रमाण" ||
            clean.contains("প্রমাণ দেখাও") || clean.contains("প্রমাণ কোথায়") || clean.contains("প্রুফ দেখাও") ||
            clean.contains("গুগল পেজ দেখাও") || clean == "প্রমাণ"
        ) {
            val proofQuery = clean
                .removePrefix("show proof for ")
                .removePrefix("show me proof of ")
                .removePrefix("show proof of ")
                .removePrefix("show evidence for ")
                .removePrefix("show google page for ")
                .removePrefix("open google page for ")
                .removePrefix("show proof")
                .removePrefix("show me proof")
                .removePrefix("proof please")
                .removePrefix("show evidence")
                .removePrefix("show google")
                .removePrefix("proof")
                .removePrefix("प्रमाण दिखाओ")
                .removePrefix("सबूत दिखाओ")
                .removePrefix("প্রমাণ দেখাও")
                .trim()

            val proofSpoken = when (detectedLang) {
                SupportedLanguage.BENGALI -> "এই যে গুগলের প্রমাণের পেজ, আপনার সামনে এখনই তুলে ধরছি বন্ধু!"
                SupportedLanguage.HINDI -> "ये रहा गूगल सर्च का प्रमाण! स्क्रीन पर वेरिफिकेशन पेज खोल रहा हूँ दोस्त!"
                SupportedLanguage.ENGLISH -> "Pulling up the Google search page as verified proof for you, mate! Here is the evidence."
            }

            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.SHOW_PROOF,
                    rawText = prompt,
                    query = proofQuery,
                    confidence = 0.98f
                ),
                directAnswer = proofSpoken
            )
        }

        // 3.6 SCREEN SHARING / SCREEN VISION SEARCH
        if (clean.contains("share screen") || clean.contains("share my screen") || clean.contains("look at my screen") ||
            clean.contains("search my screen") || clean.contains("what is on my screen") || clean.contains("what's on my screen") ||
            clean.contains("screen search") || clean.contains("analyze my screen") || clean.contains("see my screen") ||
            clean.contains("check my screen") || clean.contains("screen vision") ||
            clean.contains("स्क्रीन देखो") || clean.contains("मेरी स्क्रीन देखो") || clean.contains("स्क्रीन सर्च") ||
            clean.contains("मेरी स्क्रीन पर क्या है") || clean.contains("स्क्रीन चेक करो") ||
            clean.contains("স্ক্রিন দেখো") || clean.contains("আমার স্ক্রিন দেখো") || clean.contains("স্ক্রিন সার্চ") ||
            clean.contains("আমার স্ক্রিনে কি আছে") || clean.contains("স্ক্রিন চেক করো")
        ) {
            val screenSpoken = when (detectedLang) {
                SupportedLanguage.BENGALI -> "আমি আপনার স্ক্রিন দেখতে প্রস্তুত বন্ধু! স্ক্রিন শেয়ারের জন্য অনুরোধ করছি বা স্ক্রিনশট সিলেক্ট করুন, আমি এখনই ইন্টারনেট সার্চ করে সব বুঝিয়ে দিচ্ছি!"
                SupportedLanguage.HINDI -> "मैं आपकी स्क्रीन देखने के लिए तैयार हूँ दोस्त! स्क्रीन शेयर करें या स्क्रीनशॉट चुनें, मैं बैकग्राउंड में इंटरनेट सर्च करके सब बता दूंगा!"
                SupportedLanguage.ENGLISH -> "Ready to check your screen, mate! Tap 'Share Screen' or select a screenshot and I'll analyze it and search the web in the background!"
            }

            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.SCREEN_SEARCH,
                    rawText = prompt,
                    query = "Screen Search",
                    confidence = 0.98f
                ),
                directAnswer = screenSpoken
            )
        }

        // 4. Web & Google Search (Searches in Background without auto-opening browser)
        if (clean.startsWith("search ") || clean.startsWith("google ") || clean.startsWith("look up ") || clean.startsWith("browse ") || clean.contains("search for") || clean.contains("search on google") || clean.contains("search online") ||
            clean.contains("सर्च") || clean.contains("खोजो") || clean.contains("ढूंढो") || clean.contains("সার্চ") || clean.contains("খোঁজো")
        ) {
            val searchQuery = clean
                .removePrefix("search on google for ")
                .removePrefix("search google for ")
                .removePrefix("search the web for ")
                .removePrefix("search internet for ")
                .removePrefix("search the internet for ")
                .removePrefix("search online for ")
                .removePrefix("search for ")
                .removePrefix("search ")
                .removePrefix("google ")
                .removePrefix("look up ")
                .removePrefix("browse ")
                .removePrefix("गूगल पर सर्च करो ")
                .removePrefix("सर्च करो ")
                .removePrefix("गুগলে সার্চ করো ")
                .removePrefix("সার্চ করো ")
                .removeSuffix(" on google")
                .removeSuffix(" on internet")
                .removeSuffix(" on the web")
                .removeSuffix(" online")
                .removeSuffix(" सर्च करो")
                .removeSuffix(" खोजो")
                .removeSuffix(" সার্চ করো")
                .removeSuffix(" খোঁজো")
                .trim()
            
            val queryText = searchQuery.ifEmpty { prompt }

            val bgSearchResponse = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "আমি ব্যাকগ্রাউন্ডে '$queryText' তথ্য খুঁজে নিয়েছি বন্ধু! আপনি জানতে চেয়েছেন আর পিটার বলে দিল না, এমন কখনো হয়! প্রমাণ হিসেবে গুগল পেজ দেখতে চাইলে শুধু বলুন 'প্রমাণ দেখাও'।"
                SupportedLanguage.HINDI ->
                    "मैंने बैकग्राउंड में '$queryText' की पूरी जानकारी सर्च कर ली है दोस्त! सब कुछ तैयार है! अगर आप गूगल पेज का प्रमाण देखना चाहते हैं, तो बस बोलिए 'प्रमाण दिखाओ'।"
                SupportedLanguage.ENGLISH ->
                    "I searched '$queryText' in the background for you, mate! Got the full scoop ready right here without cluttering your screen. If you'd like to see the Google page as proof, just ask 'show proof'!"
            }

            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.WEB_SEARCH,
                    rawText = prompt,
                    query = queryText,
                    confidence = 0.96f
                ),
                directAnswer = bgSearchResponse
            )
        }

        // 5. Open App
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
        if (clean.contains("time") || clean.contains("date") || clean.contains("day is it") || clean.contains("clock") || clean.contains("today") ||
            clean.contains("समय") || clean.contains("तारीख") || clean.contains("टाइम") || clean.contains("दिन") ||
            clean.contains("সময়") || clean.contains("তারিখ") || clean.contains("টাইম") || clean.contains("কয়টা বাজে") || clean.contains("দিন")
        ) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.TIME_AND_DATE,
                    rawText = prompt,
                    confidence = 0.96f
                )
            )
        }

        // 7. Timer & Alarm
        if (clean.contains("timer") || clean.contains("countdown") || clean.contains("टाइमर") || clean.contains("টাইমার")) {
            // Find numbers
            val match = "(\\d+)\\s*(minute|min|sec|second|hour|hr|मिनट|सेकंड|মিনিট|সেকেন্ড)s?".toRegex().find(clean)
            val totalSeconds = if (match != null) {
                val value = match.groupValues[1].toIntOrNull() ?: 1
                val unit = match.groupValues[2]
                when {
                    unit.startsWith("hour") || unit.startsWith("hr") -> value * 3600
                    unit.startsWith("sec") || unit.startsWith("सेकंड") || unit.startsWith("সেকেন্ড") -> value
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

        if (clean.contains("alarm") || clean.contains("wake me") || clean.contains("अलार्म") || clean.contains("অ্যালার্ম")) {
            val timeMatch = "(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm|बजे|टा)?".toRegex().find(clean)
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
        if (clean.contains("network") || clean.contains("internet") || clean.contains("wifi status") || clean.contains("connection") ||
            clean.contains("इंटरनेट") || clean.contains("नेटवर्क") || clean.contains("ইন্টারনেট") || clean.contains("নেটওয়ার্ক")
        ) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.NETWORK_STATUS,
                    rawText = prompt,
                    confidence = 0.94f
                )
            )
        }

        // 9. Phone & Hardware Status
        if (clean.contains("device") || clean.contains("phone status") || clean.contains("ram") || clean.contains("system info") || clean.contains("specs") ||
            clean.contains("फोन") || clean.contains("डिवाइस") || clean.contains("ফোন") || clean.contains("ডিভাইস")
        ) {
            return AIResponse(
                intent = PeterIntent(
                    type = IntentType.PHONE_STATUS,
                    rawText = prompt,
                    confidence = 0.93f
                )
            )
        }

        // 10. Jokes & Entertainment
        if (clean.contains("joke") || clean.contains("funny") || clean.contains("make me laugh") ||
            clean.contains("मजाक") || clean.contains("जोक") || clean.contains("हंसाओ") || clean.contains("चुटकला") ||
            clean.contains("কৌতুক") || clean.contains("হাসাও") || clean.contains("মজার কিছু")
        ) {
            val joke = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "হাহাহা! মাকড়সারা কেন কখনো ইন্টারনেট হারায় না? কারণ তাদের কাছে সবসময় নিজস্ব ওয়েব থাকে! হাহাহা, স্পাইডার-ম্যানের সেরা জোক!"
                SupportedLanguage.HINDI ->
                    "हाहा! स्पाइडर-मैन कभी रास्ता क्यों नहीं भूलता? क्योंकि उसके पास हमेशा अपना खुद का वेब होता है! हाहाहा, टोनी स्टार्क को मत बताना भाई!"
                SupportedLanguage.ENGLISH -> listOf(
                    "Haha! Why don't spiders ever get lost? Because they always know how to check the web! Haha, classic right? Don't tell Mr. Stark I told you that one.",
                    "Haha! Why did Spider-Man join the computer club? Because he wanted to improve his world wide web development skills! Haha, gets me every single time, mate!",
                    "Oh man, haha! Someone asked me if I ever accidentally webbed myself to the ceiling... I mean, hypothetically speaking, it only happened once or twice!",
                    "Haha! What's Peter Parker's favorite day of the week? Fly-day! Okay fine, I'm working on my stand-up routine, mate!"
                ).random()
            }

            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = joke
            )
        }

        // 11. Greetings & Offline basic responses
        if (clean.contains("who are you") || clean.contains("your name") || clean.contains("introduce yourself") ||
            clean.contains("तुम कौन हो") || clean.contains("तुम्हारा नाम") || clean.contains("आप कौन हैं") ||
            clean.contains("তুমি কে") || clean.contains("তোমার নাম") || clean.contains("tomar nam") || clean.contains("tumhara naam")
        ) {
            val response = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "আমি পিটার! আপনার ফ্রেন্ডলি নেইবারহুড এআই অ্যাসিস্ট্যান্ট! ঠিক টম হল্যান্ডের মতো—সবসময় আপনাকে সাহায্য করতে আর হাসাতে তৈরি!"
                SupportedLanguage.HINDI ->
                    "अरे नमस्ते दोस्त! मैं हूँ पीटर—आपका अपना फ्रेंडली नेबरहुड एआई! ठीक टॉम हॉलैंड की तरह सुपर एनर्जेटिक और आपकी मदद के लिए तैयार!"
                SupportedLanguage.ENGLISH ->
                    "Hey mate! I'm Peter—your friendly neighborhood AI! Think of me like Tom Holland in the suit, ready to crack jokes, launch apps, search the web, and keep your phone in top shape!"
            }
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = response
            )
        }

        if (clean.contains("hello") || clean.contains("hi") || clean.contains("hey") ||
            clean.contains("नमस्ते") || clean.contains("प्रणाम") || clean.contains("হ্যালো") || clean.contains("নমস্কার") ||
            clean.contains("namaste") || clean.contains("nomoshkar")
        ) {
            val response = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "নমস্কার বন্ধু! কী খবর? স্পাইডার-ম্যানের মতো চটপট আপনার যে কোনো কাজ করে দিতে আমি তৈরি!"
                SupportedLanguage.HINDI ->
                    "अरे नमस्ते भाई! क्या हाल-चाल? स्पाइडर-सेंस एकदम एक्टिव है, बताइए क्या हुक्म है!"
                SupportedLanguage.ENGLISH ->
                    "Oh, hey mate! What's cracking? Ready to swing into whatever you need—ask me anything or give me a command!"
            }
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = response
            )
        }

        if (clean.contains("how are you") || clean.contains("status report") || clean.contains("how's it going") ||
            clean.contains("कैसे हो") || clean.contains("क्या हाल") || clean.contains("केमन आछो") || clean.contains("केमन आछेन") ||
            clean.contains("কেমন আছো") || clean.contains("কেমন আছেন") || clean.contains("kemon acho") || clean.contains("kaise ho")
        ) {
            val response = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "আমি একদম দারুণ আছি বন্ধু! কোনো সুপার-ভিলেনের উপদ্রব নেই, সব সিস্টেম একদম ফাটাফাটি চলছে!"
                SupportedLanguage.HINDI ->
                    "एकदम मस्त भाई! सारे सिस्टम्स फुल पावर में हैं, कोई एलियन हमला नहीं हुआ आज, बिल्कुल फिट!"
                SupportedLanguage.ENGLISH ->
                    "Honestly? Feeling brilliant, mate! All systems are running smooth, no alien invasions today, and ready to roll!"
            }
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = response
            )
        }

        if (clean.contains("thank") || clean.contains("cheers") || clean.contains("good job") ||
            clean.contains("धन्यवाद") || clean.contains("शुक्रिया") || clean.contains("ধন্যবাদ") || clean.contains("থ্যাঙ্কস") ||
            clean.contains("dhanyawad") || clean.contains("dhonnobad") || clean.contains("shukriya")
        ) {
            val response = when (detectedLang) {
                SupportedLanguage.BENGALI ->
                    "আরে কোনো ব্যাপারই না বন্ধু! আপনার ফ্রেন্ডলি নেইবারহুড অ্যাসিস্ট্যান্ট সবসময় আপনার সাথে আছে!"
                SupportedLanguage.HINDI ->
                    "अरे कोई बात नहीं दोस्त! आपका अपना फ्रेंडली नेबरहुड हीरो हमेशा आपकी सेवा में हाजिर है!"
                SupportedLanguage.ENGLISH ->
                    "Anytime, mate! That's what friendly neighborhood assistants are for. High five!"
            }
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt),
                directAnswer = response
            )
        }

        // 12. Local Knowledge & Q&A Engine (Science, Math, Capitals, MCU, Tech, General facts)
        val knowledgeAnswer = LocalKnowledgeEngine.answerQuery(prompt, detectedLang)
        if (knowledgeAnswer != null) {
            return AIResponse(
                intent = PeterIntent(type = IntentType.AI_QUERY, rawText = prompt, query = prompt, confidence = 0.95f),
                directAnswer = knowledgeAnswer
            )
        }

        // 13. Dynamic Knowledge Response for Any Custom / Open-Ended Query
        val queryKeywords = prompt.trim()
            .removePrefix("hey peter")
            .removePrefix("peter")
            .trim()

        val answerText = when (detectedLang) {
            SupportedLanguage.BENGALI ->
                "আমি '$queryKeywords' সম্পর্কে সম্পূর্ণ তথ্য প্রস্তুত করেছি বন্ধু! বিস্তারিত দেখতে চাইলে বা প্রমাণ হিসেবে গুগল পেজ দেখতে বলুন 'প্রমাণ দেখাও'।"
            SupportedLanguage.HINDI ->
                "मैंने '$queryKeywords' की पूरी जानकारी तैयार कर ली है दोस्त! अगर आप गूगल पेज का प्रमाण देखना चाहते हैं, तो बस बोलिए 'प्रमाण दिखाओ'!"
            SupportedLanguage.ENGLISH ->
                "I've got the info on '$queryKeywords' ready for you, mate! To open the Google verification page directly on your screen, just say 'show proof'!"
        }

        return AIResponse(
            intent = PeterIntent(
                type = IntentType.AI_QUERY,
                rawText = prompt,
                query = queryKeywords,
                confidence = 0.85f
            ),
            directAnswer = answerText
        )
    }
}
