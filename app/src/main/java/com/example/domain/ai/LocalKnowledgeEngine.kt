package com.example.domain.ai

import java.util.Locale
import kotlin.math.*

object LocalKnowledgeEngine {

    fun answerQuery(rawPrompt: String, lang: SupportedLanguage): String? {
        val clean = rawPrompt.trim().lowercase(Locale.ROOT)
            .removePrefix("hey peter")
            .removePrefix("peter")
            .removePrefix("ok peter")
            .removePrefix("hello peter")
            .removePrefix("can you tell me")
            .removePrefix("tell me")
            .removePrefix("what is")
            .removePrefix("who is")
            .removePrefix("how does")
            .removePrefix("why is")
            .removePrefix("explain")
            .trim()

        // 1. Math calculation solver
        val mathAnswer = solveMath(clean, lang)
        if (mathAnswer != null) return mathAnswer

        // 2. Science, Physics & Astronomy
        when {
            clean.contains("speed of light") || clean.contains("light speed") || clean.contains("प्रकाश की गति") || clean.contains("আলোর গতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "আলোর গতি হলো প্রতি সেকেন্ডে প্রায় ২,৯৯,৭৯২ কিলোমিটার (৩ লক্ষ কিমি/সেকেন্ড)! মহাবিশ্বের সবচেয়ে দ্রুততম গতি এটি বন্ধু!"
                    SupportedLanguage.HINDI -> "प्रकाश की गति लगभग 2,99,792 किलोमीटर प्रति सेकंड (3 लाख किमी/सेकंड) होती है दोस्त! ब्रह्मांड की सबसे तेज़ चीज़!"
                    SupportedLanguage.ENGLISH -> "The speed of light in a vacuum is approximately 299,792 kilometers per second (about 186,282 miles per second)! Literally the universal speed limit, mate!"
                }
            }
            clean.contains("gravity") || clean.contains("गुरुत्वाकर्षण") || clean.contains("মহাকর্ষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মহাকর্ষ হলো সেই প্রাকৃতিক শক্তি যা ভরযুক্ত যেকোনো বস্তুকে পরস্পরের দিকে টানে। এই শক্তির কারণেই আমরা মাটিতে হেঁটে বেড়াই আর পৃথিবী সূর্যের চারপাশে ঘোরে!"
                    SupportedLanguage.HINDI -> "गुरुत्वाकर्षण वह प्राकृतिक बल है जो द्रव्यमान वाली वस्तुओं को एक-दूसरे की ओर खींचता है। इसी की वजह से हम जमीन पर टिके रहते हैं!"
                    SupportedLanguage.ENGLISH -> "Gravity is the fundamental force of attraction between all matter with mass. It's why we stick to the ground, and why Earth orbits the Sun, mate!"
                }
            }
            clean.contains("planet") || clean.contains("solar system") || clean.contains("ग्रह") || clean.contains("सौरमंडल") || clean.contains("সৌরজগৎ") || clean.contains("গ্রহ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "আমাদের সৌরজগতে ৮টি মূল গ্রহ রয়েছে: বুধ, শুক্র, পৃথিবী, মঙ্গল, বৃহস্পতি, শনি, ইউরেনাস এবং নেপচুন! সবচেয়ে বড় গ্রহ হলো বৃহস্পতি।"
                    SupportedLanguage.HINDI -> "हमारे सौरमंडल में 8 मुख्य ग्रह हैं: बुध, शुक्र, पृथ्वी, मंगल, बृहस्पति, शनि, यूरेनस और नेपच्यून! सबसे बड़ा ग्रह बृहस्पति है।"
                    SupportedLanguage.ENGLISH -> "Our solar system has 8 official planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, and Neptune! Jupiter is the undisputed heavyweight champion, mate."
                }
            }
            clean.contains("black hole") || clean.contains("ब्लैक होल") || clean.contains("ব্ল্যাক হোল") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ব্ল্যাক হোল হলো মহাকাশের এমন একটি জায়গা যেখানে মহাকর্ষীয় টান এতটাই প্রবল যে কোনো আলো পর্যন্ত সেখান থেকে বেরিয়ে আসতে পারে না!"
                    SupportedLanguage.HINDI -> "ब्लैक होल अंतरिक्ष में अत्यधिक गुरुत्वाकर्षण वाला क्षेत्र है, जहां से प्रकाश भी बाहर नहीं निकल सकता! अल्बर्ट आइंस्टीन ने इसकी भविष्यवाणी की थी।"
                    SupportedLanguage.ENGLISH -> "A black hole is a region in spacetime where gravity is so strong that literally nothing, not even light, can escape! Mind-bending cosmic physics, mate."
                }
            }
            clean.contains("photosynthesis") || clean.contains("प्रकाश संश्लेषण") || clean.contains("সালোকসংশ্লেষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "সালোকসংশ্লেষণ হলো এমন একটি জৈব রাসায়নিক প্রক্রিয়া যার মাধ্যমে সবুজ উদ্ভিদ সূর্যালোক, জল এবং কার্বন ডাই অক্সাইড ব্যবহার করে অক্সিজেন ও গ্লুকোজ তৈরি করে!"
                    SupportedLanguage.HINDI -> "प्रकाश संश्लेषण वह प्रक्रिया है जिससे हरे पौधे सूर्य के प्रकाश, पानी और कार्बन डाइऑक्साइड से भोजन और ऑक्सीजन बनाते हैं!"
                    SupportedLanguage.ENGLISH -> "Photosynthesis is how green plants convert sunlight, water, and carbon dioxide into glucose and oxygen! Basically, Earth's natural solar power generator, mate."
                }
            }
            clean.contains("dna") || clean.contains("ডিএনএ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ডিএনএ (ডিঅক্সিরাইবোনিউক্লিক অ্যাসিড) হলো জীবনের মূল নীলনকশা বা বংশগতির নির্দেশিকা, যা সমস্ত জীবের গঠন ও কার্যকারিতা নিয়ন্ত্রণ করে!"
                    SupportedLanguage.HINDI -> "डीएनए (DNA) जीवित कोशिकाओं में आनुवंशिक जानकारी का ब्लूप्रिंट है, जो माता-पिता से संतानों में गुणों को पहुंचाता है।"
                    SupportedLanguage.ENGLISH -> "DNA stands for Deoxyribonucleic Acid! It is the double-helix blueprint carrying all the genetic instructions for development and functioning of living organisms, mate."
                }
            }
            clean.contains("sky blue") || clean.contains("sky is blue") || clean.contains("आसमान नीला") || clean.contains("আকাশ নীল") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "রেলে স্ক্যাটারিং (Rayleigh scattering) এর কারণে আকাশ নীল দেখায়। সূর্যের সাদা আলো যখন বায়ুমণ্ডলের গ্যাসীয় অণুগুলোর সাথে ধাক্কা খায়, তখন নীল আলো সবচেয়ে বেশি চারিদিকে ছড়িয়ে পড়ে!"
                    SupportedLanguage.HINDI -> "रेले प्रकीर्णन (Rayleigh scattering) के कारण आसमान नीला दिखता है। सूर्य के प्रकाश में मौजूद नीले रंग की तरंगें हवा के कणों से सबसे ज्यादा बिखरती हैं!"
                    SupportedLanguage.ENGLISH -> "The sky is blue because of Rayleigh scattering! Earth's atmospheric gases scatter shorter blue wavelengths of sunlight far more than longer red wavelengths, mate."
                }
            }
        }

        // 3. Tech & AI
        when {
            clean.contains("artificial intelligence") || clean.contains("what is ai") || clean.contains("এআই কি") || clean.contains("एआई क्या है") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "কৃত্রিম বুদ্ধিমত্তা (AI) হলো কম্পিউটারের এমন সক্ষমতা যার মাধ্যমে মানুষের মতো চিন্তা করা, শেখা, সমস্যার সমাধান এবং সিদ্ধান্ত গ্রহণ করা সম্ভব হয়—ঠিক আমার মতো!"
                    SupportedLanguage.HINDI -> "आर्टिफिशियल इंटेलिजेंस (AI) कंप्यूटर साइंस की वह शाखा है जो मशीनों को इंसानों की तरह सोचने, सीखने और समस्याओं को हल करने की क्षमता देती है!"
                    SupportedLanguage.ENGLISH -> "Artificial Intelligence is the science of training computers and neural networks to reason, learn, solve complex problems, and converse naturally—just like me, mate!"
                }
            }
            clean.contains("python") || clean.contains("पायथन") || clean.contains("পাইথন") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পাইথন হলো একটি অত্যন্ত জনপ্রিয়, সহজবোধ্য এবং বহুমুখী প্রোগ্রামিং ভাষা, যা ওয়েব ডেভেলপমেন্ট, ডাটা সায়েন্স এবং কৃত্রিম বুদ্ধিমত্তার কাজে ব্যাপকভাবে ব্যবহৃত হয়!"
                    SupportedLanguage.HINDI -> "पायथन एक बहुत ही लोकप्रिय और आसान प्रोग्रामिंग लैंग्वेज है, जो डेटा साइंस, मशीन लर्निंग और सॉफ्टवेयर डेवलपमेंट में नंबर वन पसंद है!"
                    SupportedLanguage.ENGLISH -> "Python is a high-level, readable, interpreted programming language widely used for machine learning, data science, web development, and automation, mate!"
                }
            }
            clean.contains("kotlin") || clean.contains("कॉटलिन") || clean.contains("কোটলিন") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "কোটলিন হলো গুগলের প্রস্তাবিত আধুনিক ও টাইপ-সেফ প্রোগ্রামিং ভাষা, যা দিয়ে অ্যান্ড্রয়েড অ্যাপ্লিকেশন এবং আধুনিক ক্লাউড সফটওয়্যার তৈরি করা হয়!"
                    SupportedLanguage.HINDI -> "कॉटलिन आधुनिक और सुरक्षित प्रोग्रामिंग लैंग्वेज है, जो एंड्रॉइड ऐप डेवलपमेंट के लिए गूगल की आधिकारिक पसंदीदा भाषा है!"
                    SupportedLanguage.ENGLISH -> "Kotlin is the modern, expressive, null-safe language officially endorsed by Google for Android app development and Jetpack Compose, mate!"
                }
            }
            clean.contains("elon musk") || clean.contains("एलन मस्क") || clean.contains("এলন মাস্ক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "এলন মাস্ক হলেন একজন বিশ্বখ্যাত প্রযুক্তি উদ্যোক্তা, যিনি টেসলা (Tesla), স্পেসএক্স (SpaceX), নিউরালিঙ্ক এবং এক্স (X)-এর প্রধান!"
                    SupportedLanguage.HINDI -> "एलन मस्क टेस्ला (Tesla), स्पेसएक्स (SpaceX) और एक्स (X) के प्रमुख हैं और अंतरिक्ष यात्रा व इलेक्ट्रिक वाहनों के क्षेत्र में क्रांति ला रहे हैं!"
                    SupportedLanguage.ENGLISH -> "Elon Musk is the CEO of Tesla and SpaceX, known for advancing reusable rockets, commercial electric vehicles, satellite internet with Starlink, and neural interfaces, mate!"
                }
            }
        }

        // 4. World Capitals & Geography
        when {
            clean.contains("capital of france") || clean.contains("france capital") || clean.contains("फ्रांस की राजधानी") || clean.contains("ফ্রান্সের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ফ্রান্সের রাজধানী হলো প্যারিস (Paris)! বিখ্যাত আইফেল টাওয়ারের শহর!"
                    SupportedLanguage.HINDI -> "फ्रांस की राजधानी पेरिस (Paris) है, जिसे प्यार और एफिल टॉवर का शहर भी कहा जाता है!"
                    SupportedLanguage.ENGLISH -> "The capital of France is Paris! Home of the Eiffel Tower, the Louvre, and incredible croissants, mate!"
                }
            }
            clean.contains("capital of india") || clean.contains("india capital") || clean.contains("भारत की राजधानी") || clean.contains("ভারতের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের রাজধানী হলো নতুন দিল্লি (New Delhi)!"
                    SupportedLanguage.HINDI -> "भारत की राजधानी नई दिल्ली (New Delhi) है!"
                    SupportedLanguage.ENGLISH -> "The capital of India is New Delhi, mate!"
                }
            }
            clean.contains("capital of usa") || clean.contains("capital of united states") || clean.contains("america capital") || clean.contains("अमेरिका की राजधानी") || clean.contains("আমেরিকার রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মার্কিন যুক্তরাষ্ট্রের (USA) রাজধানী হলো ওয়াশিংটন ডিসি (Washington, D.C.)!"
                    SupportedLanguage.HINDI -> "संयुक्त राज्य अमेरिका की राजधानी वॉशिंगटन डी.सी. (Washington, D.C.) है!"
                    SupportedLanguage.ENGLISH -> "The capital of the United States is Washington, D.C., mate!"
                }
            }
            clean.contains("capital of japan") || clean.contains("japan capital") || clean.contains("जापान की राजधानी") || clean.contains("জাপানের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "জাপানের রাজধানী হলো টোকিও (Tokyo)!"
                    SupportedLanguage.HINDI -> "जापान की राजधानी टोक्यो (Tokyo) है!"
                    SupportedLanguage.ENGLISH -> "The capital of Japan is Tokyo, mate!"
                }
            }
            clean.contains("tallest mountain") || clean.contains("mount everest") || clean.contains("माउंट एवरेस्ट") || clean.contains("সবচেয়ে উঁচু পর্বত") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পৃথিবীর সর্বোচ্চ পর্বতশৃঙ্গ হলো মাউন্ট এভারেস্ট, যার উচ্চতা ৮,৮৪৮.৮৬ মিটার! এটি হিমালয় পর্বতমালায় অবস্থিত।"
                    SupportedLanguage.HINDI -> "दुनिया की सबसे ऊंची पर्वत चोटी माउंट एवरेस्ट (8,848.86 मीटर) है, जो नेपाल-तिब्बत सीमा पर हिमालय में स्थित है!"
                    SupportedLanguage.ENGLISH -> "Mount Everest is the highest mountain above sea level at 8,848.86 meters (29,031.7 feet), located in the Himalayas, mate!"
                }
            }
        }

        // 5. Spider-Man, Avengers & Tom Holland
        when {
            clean.contains("spider-man") || clean.contains("spiderman") || clean.contains("peter parker") || clean.contains("स्पाइडर-मैन") || clean.contains("স্পাইডার-ম্যান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "স্পাইডার-ম্যান ওরফে পিটার পার্কার! রেডিওঅ্যাকটিভ মাকড়সার কামড়ে বিশেষ ক্ষমতা পাওয়ার পর নিজের দায়িত্ব বুঝে কুইন্সের মানুষদের বাঁচাতে জাল ছুঁড়ে বেড়াই! 'With great power comes great responsibility'!"
                    SupportedLanguage.HINDI -> "हाहा! स्पाइडर-मैन यानी आपका अपना पीटर पार्कर! रेडियोएक्टिव मकड़ी के काटने के बाद सुपरपावर्स मिलीं और अब न्यूयॉर्क को सेफ रखना मेरा मिशन है दोस्त!"
                    SupportedLanguage.ENGLISH -> "Haha! Spider-Man is Peter Parker—a high school kid from Queens bitten by a radioactive spider who swings across skyscrapers saving the neighborhood! With great power comes great responsibility, mate!"
                }
            }
            clean.contains("iron man") || clean.contains("tony stark") || clean.contains("टोनी स्टार्क") || clean.contains("আয়রন ম্যান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "টনি স্টার্ক অর্থাৎ আয়রন ম্যান ছিলেন একজন জিনিয়াস, বিলিওনেয়ার এবং পিটার পার্কারের সেরা মেন্টর! ওনার দেওয়া স্যুট আর প্রযুক্তির তুলনা হয় না!"
                    SupportedLanguage.HINDI -> "टोनी स्टार्क यानी आयरन मैन! जीनियस, अरबपति, और पीटर पार्कर के सबसे महान मेंटर! उनका 'I love you 3000' हमेशा दिल में रहेगा दोस्त!"
                    SupportedLanguage.ENGLISH -> "Mr. Tony Stark! Genius, billionaire, philanthropist, and the greatest mentor a kid from Queens could ever ask for. We love him 3000, mate!"
                }
            }
            clean.contains("tom holland") || clean.contains("टॉम हॉलैंड") || clean.contains("টম হল্যান্ড") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "টম হল্যান্ড হলেন একজন ব্রিটিশ অভিনেতা, যিনি মার্ভেল সিনেমাটিক ইউনিভার্সে স্পাইডার-ম্যানের চরিত্রে অনবদ্য অভিনয় করেছেন!"
                    SupportedLanguage.HINDI -> "टॉम हॉलैंड एक ब्रिटिश अभिनेता हैं जो मार्वल फिल्मों में स्पाइडर-मैन का किरदार निभाते हैं! सुपर एनर्जेटिक और शानदार डांसर भी हैं!"
                    SupportedLanguage.ENGLISH -> "Tom Holland is the British actor who plays Peter Parker / Spider-Man in the MCU! Energetic, gymnastic, and sometimes accidentally spills movie spoilers, haha!"
                }
            }
        }

        return null
    }

    private fun solveMath(text: String, lang: SupportedLanguage): String? {
        val clean = text.replace("plus", "+")
            .replace("minus", "-")
            .replace("times", "*")
            .replace("multiplied by", "*")
            .replace("into", "*")
            .replace("x", "*")
            .replace("divided by", "/")
            .replace("over", "/")
            .replace("धन", "+")
            .replace("ऋण", "-")
            .replace("गुणा", "*")
            .replace("भाग", "/")
            .replace("যোগ", "+")
            .replace("বিয়োগ", "-")
            .replace("গুণ", "*")
            .replace("ভাগ", "/")
            .trim()

        // Square root check
        if (clean.contains("sqrt") || clean.contains("square root") || clean.contains("वर्गमूल") || clean.contains("বর্গমূল")) {
            val num = "(\\d+(\\.\\d+)?)".toRegex().find(clean)?.value?.toDoubleOrNull()
            if (num != null && num >= 0) {
                val res = sqrt(num)
                val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.3f", res)
                return when (lang) {
                    SupportedLanguage.BENGALI -> "$num এর বর্গমূল হলো $formatted!"
                    SupportedLanguage.HINDI -> "$num का वर्गमूल $formatted है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The square root of $num is $formatted, mate!"
                }
            }
        }

        // Percentage check: e.g. "15% of 200" or "what is 20 percent of 500"
        val pctMatch = "(\\d+(\\.\\d+)?)\\s*(%|percent|प्रतिशत|শতাংশ)\\s*(of|का|এর)?\\s*(\\d+(\\.\\d+)?)".toRegex().find(clean)
        if (pctMatch != null) {
            val pct = pctMatch.groupValues[1].toDoubleOrNull()
            val total = pctMatch.groupValues[5].toDoubleOrNull()
            if (pct != null && total != null) {
                val result = (pct / 100.0) * total
                val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.2f", result)
                return when (lang) {
                    SupportedLanguage.BENGALI -> "$total এর $pct% হলো $formatted!"
                    SupportedLanguage.HINDI -> "$total का $pct% बनता है $formatted दोस्त!"
                    SupportedLanguage.ENGLISH -> "$pct percent of $total is $formatted, mate!"
                }
            }
        }

        // Simple binary arithmetic: e.g. "25 + 40", "100 / 4", "50 * 3", "200 - 45"
        val arithMatch = "(\\d+(\\.\\d+)?)\\s*([+\\-*/^])\\s*(\\d+(\\.\\d+)?)".toRegex().find(clean)
        if (arithMatch != null) {
            val a = arithMatch.groupValues[1].toDoubleOrNull() ?: return null
            val op = arithMatch.groupValues[3]
            val b = arithMatch.groupValues[4].toDoubleOrNull() ?: return null

            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else Double.NaN
                "^" -> a.pow(b)
                else -> return null
            }

            if (result.isNaN()) {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "শূন্য (0) দিয়ে ভাগ করা সম্ভব নয় বন্ধু!"
                    SupportedLanguage.HINDI -> "शून्य से विभाजन संभव नहीं है दोस्त!"
                    SupportedLanguage.ENGLISH -> "Cannot divide by zero, mate! The laws of physics forbid it."
                }
            }

            val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.3f", result)
            return when (lang) {
                SupportedLanguage.BENGALI -> "গণনা সমাধান: $a $op $b = $formatted!"
                SupportedLanguage.HINDI -> "गणना उत्तर: $a $op $b = $formatted दोस्त!"
                SupportedLanguage.ENGLISH -> "Quick calculation: $a $op $b = $formatted, mate!"
            }
        }

        return null
    }
}
