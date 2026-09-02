package com.example.domain.ai

import java.util.Locale
import kotlin.math.*

object LocalKnowledgeEngine {

    fun answerQuery(rawPrompt: String, lang: SupportedLanguage): String? {
        val clean = rawPrompt.trim().lowercase(Locale.ROOT)
            .replace(Regex("[,?!.'\"-]+"), " ")
            .trim()

        // 1. Math calculation solver
        val mathAnswer = solveMath(clean, lang)
        if (mathAnswer != null) return mathAnswer

        // 2. Indian Government, Leaders & Politics
        when {
            // President of India / Rashtrapati
            containsAny(clean, "president of india", "indian president", "rashtrapati of india", "bharat ke rashtrapati", "bharater rastrapati", "who is the president of india", "who is president of india", "who is indian president", "current president of india", "राष्ट्रपति", "রাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের বর্তমান ও ১৫তম মহামান্য রাষ্ট্রপতি হলেন শ্রীমতি দ্রৌপদী মুর্মু (Smt. Droupadi Murmu)! তিনি ভারতের সর্বোচ্চ সাংবিধানিক পদে আসীন হওয়া প্রথম আদিবাসী মহিলা রাষ্ট্রপতি।"
                    SupportedLanguage.HINDI -> "भारत की वर्तमान और 15वीं राष्ट्रपति महामहिम श्रीमती द्रौपदी मुर्मू जी (Smt. Droupadi Murmu) हैं! वह भारत के सर्वोच्च संवैधानिक पद पर आसीन होने वाली पहली आदिवासी महिला राष्ट्रपति हैं।"
                    SupportedLanguage.ENGLISH -> "The President of India is Smt. Droupadi Murmu, the 15th President of India and the first tribal woman to hold this highest constitutional office of the Republic of India, mate!"
                }
            }

            // Prime Minister of India / PM of India
            containsAny(clean, "prime minister of india", "pm of india", "indian prime minister", "bharat ke pradhanmantri", "bharater prodhanmontri", "who is the pm of india", "who is the prime minister of india", "who is prime minister of india", "current pm of india", "प्रधानमंत्री", "প্রধানমন্ত্রী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের বর্তমান ও ১৪তম প্রধানমন্ত্রী হলেন শ্রী নরেন্দ্র মোদী (Narendra Modi), যিনি ২০১৪ সাল থেকে একটানা দেশের সেবায় নিয়োজিত রয়েছেন!"
                    SupportedLanguage.HINDI -> "भारत के वर्तमान और 14वें प्रधानमंत्री श्री नरेंद्र मोदी जी हैं, जो 2014 से लगातार देश का नेतृत्व कर रहे हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Prime Minister of India is Shri Narendra Modi, serving as the head of government of the Republic of India since 2014, mate!"
                }
            }

            // Vice President of India
            containsAny(clean, "vice president of india", "vp of india", "uparashtrapati", "उपराष्ट्रपति", "উপরাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের বর্তমান উপরাষ্ট্রপতি এবং রাজ্যসভার চেয়ারম্যান হলেন শ্রী জগদীপ ধনখড় (Jagdeep Dhankhar)!"
                    SupportedLanguage.HINDI -> "भारत के वर्तमान उपराष्ट्रपति और राज्य सभा के सभापति श्री जगदीप धनखड़ जी हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Vice President of India and Chairman of the Rajya Sabha is Shri Jagdeep Dhankhar, mate!"
                }
            }

            // First President of India
            containsAny(clean, "first president of india", "pehle rashtrapati", "প্রথম রাষ্ট্রপতি", "पहले राष्ट्रपति") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "স্বাধীন ভারতের প্রথম রাষ্ট্রপতি ছিলেন ড. রাজেন্দ্র প্রসাদ (Dr. Rajendra Prasad), যিনি ১৯৫০ থেকে ১৯৬২ সাল পর্যন্ত দায়িত্ব পালন করেন!"
                    SupportedLanguage.HINDI -> "स्वतंत्र भारत के पहले राष्ट्रपति डॉ. राजेंद्र प्रसाद जी थे, जिन्होंने 1950 से 1962 तक सेवा की थी!"
                    SupportedLanguage.ENGLISH -> "The first President of independent India was Dr. Rajendra Prasad, serving from 1950 to 1962, mate!"
                }
            }

            // First Prime Minister of India
            containsAny(clean, "first prime minister of india", "first pm of india", "pehle pradhanmantri", "প্রথম প্রধানমন্ত্রী", "पहले प्रधानमंत्री") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "স্বাধীন ভারতের প্রথম প্রধানমন্ত্রী ছিলেন পণ্ডিত জওহরলাল নেহেরু (Jawaharlal Nehru), যিনি ১৯৪৭ থেকে ১৯৬৪ সাল পর্যন্ত দেশের নেতৃত্ব দেন!"
                    SupportedLanguage.HINDI -> "स्वतंत्र भारत के पहले प्रधानमंत्री पंडित जवाहरलाल नेहरू जी थे, जिन्होंने 1947 से 1964 तक देश का नेतृत्व किया था!"
                    SupportedLanguage.ENGLISH -> "The first Prime Minister of independent India was Pandit Jawaharlal Nehru, serving from 1947 to 1964, mate!"
                }
            }

            // Chief Minister of West Bengal
            containsAny(clean, "chief minister of west bengal", "cm of west bengal", "cm of bengal", "paschim banga cm", "মুখ্যমন্ত্রী মমতা", "পশ্চিমবঙ্গের মুখ্যমন্ত্রী", "पश्चिम बंगाल के मुख्यमंत्री") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পশ্চিমবঙ্গের বর্তমান মাননীয়া মুখ্যমন্ত্রী হলেন শ্রীমতি মমতা বন্দ্যোপাধ্যায় (Mamata Banerjee), যিনি ২০১১ সাল থেকে রাজ্যের দায়িত্বে আছেন!"
                    SupportedLanguage.HINDI -> "पश्चिम बंगाल की वर्तमान मुख्यमंत्री सुश्री ममता बनर्जी जी हैं!"
                    SupportedLanguage.ENGLISH -> "The Chief Minister of West Bengal is Ms. Mamata Banerjee, serving since 2011, mate!"
                }
            }

            // Chief Minister of Uttar Pradesh
            containsAny(clean, "chief minister of uttar pradesh", "cm of up", "cm of uttar pradesh", "up ke cm", "यूपी के मुख्यमंत्री") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "উত্তর প্রদেশের বর্তমান মুখ্যমন্ত্রী হলেন শ্রী যোগী আদিত্যনাথ (Yogi Adityanath)!"
                    SupportedLanguage.HINDI -> "उत्तर प्रदेश के वर्तमान मुख्यमंत्री श्री योगी आदित्यनाथ जी हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Chief Minister of Uttar Pradesh is Shri Yogi Adityanath, mate!"
                }
            }

            // Chief Minister of Maharashtra
            containsAny(clean, "chief minister of maharashtra", "cm of maharashtra", "महाराष्ट्र के मुख्यमंत्री") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মহারাষ্ট্রের বর্তমান মুখ্যমন্ত্রী হলেন শ্রী দেবেন্দ্র ফড়নবীশ / একনাথ শিন্ডে!"
                    SupportedLanguage.HINDI -> "महाराष्ट्र के वर्तमान मुख्यमंत्री श्री देवेंद्र फडणवीस जी हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Chief Minister of Maharashtra is Shri Devendra Fadnavis, mate!"
                }
            }

            // Chief Minister of Tamil Nadu
            containsAny(clean, "chief minister of tamil nadu", "cm of tamil nadu", "cm of tn", "तमिलनाडु के मुख्यमंत्री") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "তামিলনাড়ুর বর্তমান মুখ্যমন্ত্রী হলেন শ্রী এম. কে. স্ট্যালিন (M. K. Stalin)!"
                    SupportedLanguage.HINDI -> "तमिलनाडु के वर्तमान मुख्यमंत्री श्री एम. के. स्टालिन जी हैं!"
                    SupportedLanguage.ENGLISH -> "The Chief Minister of Tamil Nadu is Shri M. K. Stalin, mate!"
                }
            }

            // ISRO Chairman
            containsAny(clean, "isro chairman", "isro chief", "head of isro", "इसरो के अध्यक्ष", "ইসরোর চেয়ারম্যান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতীয় মহাকাশ গবেষণা সংস্থা (ISRO)-এর বর্তমান চেয়ারম্যান হলেন শ্রী এস. সোমনাথ (S. Somanath), যার নেতৃত্বে চন্দ্রযান-৩ ও আদিত্য-L1 সফল হয়েছে!"
                    SupportedLanguage.HINDI -> "भारतीय अंतरिक्ष अनुसंधान संगठन (ISRO) के वर्तमान अध्यक्ष श्री एस. सोमनाथ (S. Somanath) हैं, जिनके नेतृत्व में चंद्रयान-3 ने इतिहास रचा था!"
                    SupportedLanguage.ENGLISH -> "The Chairman of the Indian Space Research Organisation (ISRO) is Shri S. Somanath, who successfully led the Chandrayaan-3 Moon landing and Aditya-L1 solar mission, mate!"
                }
            }
        }

        // 3. International Leaders & World Figures
        when {
            // President of USA / US President
            containsAny(clean, "president of usa", "president of america", "us president", "who is the us president", "who is president of usa", "who is the president of the united states", "अमेरिका के राष्ट्रपति", "আমেরিকার রাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মার্কিন যুক্তরাষ্ট্রের (USA) বর্তমান রাষ্ট্রপতি হলেন জো বাইডেন (Joe Biden) এবং পরবর্তী নির্বাচিত রাষ্ট্রপতি ডোনাল্ড ট্রাম্প (Donald Trump)!"
                    SupportedLanguage.HINDI -> "संयुक्त राज्य अमेरिका (USA) के राष्ट्रपति जो बाइडेन (Joe Biden) हैं और नव-निर्वाचित 47वें राष्ट्रपति डोनाल्ड ट्रंप (Donald Trump) हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The President of the United States is Joe Biden (the 46th President), and Donald Trump was elected as the 47th President of the United States, mate!"
                }
            }

            // Prime Minister of UK
            containsAny(clean, "prime minister of uk", "pm of uk", "british prime minister", "uk prime minister", "who is the prime minister of uk", "ब्रिटेन के प्रधानमंत्री", "যুক্তরাজ্যের প্রধানমন্ত্রী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "যুক্তরাজ্যের (UK) বর্তমান প্রধানমন্ত্রী হলেন স্যার কিয়ার স্টারমার (Keir Starmer)!"
                    SupportedLanguage.HINDI -> "यूनाइटेड किंगडम (ब्रिटेन) के वर्तमान प्रधानमंत्री सर कीयर स्टारमर (Sir Keir Starmer) हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Prime Minister of the United Kingdom is Sir Keir Starmer, representing the Labour Party at 10 Downing Street, mate!"
                }
            }

            // President of Russia
            containsAny(clean, "president of russia", "russian president", "who is president of russia", "रूस के राष्ट्रपति", "রাশিয়ার রাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "রাশিয়ার বর্তমান রাষ্ট্রপতি হলেন ভ্লাদিমির পুতিন (Vladimir Putin)!"
                    SupportedLanguage.HINDI -> "रूस के वर्तमान राष्ट्रपति व्लादिमीर पुतिन (Vladimir Putin) हैं!"
                    SupportedLanguage.ENGLISH -> "The President of the Russian Federation is Vladimir Putin, mate!"
                }
            }

            // President of France
            containsAny(clean, "president of france", "french president", "फ्रांस के राष्ट्रपति", "ফ্রান্সের রাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ফ্রান্সের বর্তমান রাষ্ট্রপতি হলেন ইমানুয়েল ম্যাক্রোঁ (Emmanuel Macron)!"
                    SupportedLanguage.HINDI -> "फ्रांस के वर्तमान राष्ट्रपति इमैनुएल मैक्रों (Emmanuel Macron) हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The President of the French Republic is Emmanuel Macron, mate!"
                }
            }

            // President of China
            containsAny(clean, "president of china", "chinese president", "चीन के राष्ट्रपति", "চীনের রাষ্ট্রপতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "চীনের বর্তমান রাষ্ট্রপতি হলেন শি জিনপিং (Xi Jinping)!"
                    SupportedLanguage.HINDI -> "चीन के वर्तमान राष्ट्रपति शी जिनपिंग (Xi Jinping) हैं!"
                    SupportedLanguage.ENGLISH -> "The President of China is Xi Jinping, General Secretary of the CCP, mate!"
                }
            }

            // Elon Musk
            containsAny(clean, "elon musk", "एलन मस्क", "এলন মাস্ক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "এলন মাস্ক হলেন একজন বিশ্বখ্যাত প্রযুক্তি উদ্যোক্তা ও বিশ্বের অন্যতম ধনী ব্যক্তি, যিনি টেসলা (Tesla), স্পেসএক্স (SpaceX), নিউরালিঙ্ক এবং এক্স (X)-এর প্রধান!"
                    SupportedLanguage.HINDI -> "एलन मस्क दुनिया के सबसे अमीर व प्रभावशाली टेक उद्योगपति हैं, जो टेस्ला (Tesla), स्पेसएक्स (SpaceX), न्यूरालिंक और X के प्रमुख हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "Elon Musk is the CEO of Tesla and SpaceX, pioneer in reusable orbital rockets, electric cars, Starlink satellite internet, and neural tech, mate!"
                }
            }

            // Richest person in the world
            containsAny(clean, "richest person in the world", "richest man in the world", "duniya ka sabse amir", "সবচেয়ে ধনী ব্যক্তি", "दुनिया का सबसे अमीर") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "বর্তমান বিশ্বের সবচেয়ে ধনী ব্যক্তিদের শীর্ষে রয়েছেন এলন মাস্ক (Elon Musk), যার মোট সম্পদ প্রায় ২০০-৩০০ বিলিয়ন ডলার ছাড়িয়ে গেছে!"
                    SupportedLanguage.HINDI -> "वर्तमान में दुनिया के सबसे अमीर व्यक्ति एलन मस्क (Elon Musk) हैं, जिनकी कुल संपत्ति 200 से 300 बिलियन डॉलर से अधिक है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The richest person in the world is Elon Musk, with a net worth surpassing $250+ billion powered by Tesla and SpaceX, mate!"
                }
            }
        }

        // 4. Indian National Symbols & Freedom History
        when {
            // Father of the Nation
            containsAny(clean, "father of the nation", "father of nation in india", "rashtrapita", "राष्ट्रपिता", "জাতির জনক", "মহাত্মা গান্ধী", "mahatma gandhi") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের জাতির জনক হলেন মোহনদাস করমচাঁদ গান্ধী (মহাত্মা গান্ধী), যিনি অহিংসা ও সত্যাগ্রহের মাধ্যমে ভারতের স্বাধীনতা আন্দোলনকে নেতৃত্ব দিয়েছিলেন!"
                    SupportedLanguage.HINDI -> "भारत के राष्ट्रपिता महात्मा गांधी (मोहनदास करमचंद गांधी) हैं, जिन्होंने सत्य और अहिंसा के मार्ग पर चलकर देश को आजादी दिलाई!"
                    SupportedLanguage.ENGLISH -> "The Father of the Nation in India is Mahatma Gandhi (Mohandas Karamchand Gandhi), who championed non-violence (Ahimsa) and truth to lead India to freedom, mate!"
                }
            }

            // Father of Indian Constitution
            containsAny(clean, "father of indian constitution", "architect of constitution", "dr b r ambedkar", "ambedkar", "संविधान के निर्माता", "সংবিধানের জনক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের সংবিধানের প্রধান রূপকার ও জনক হলেন ড. বি. আর. আম্বেদকর (Dr. B.R. Ambedkar), যিনি ড্রাফটিং কমিটির চেয়ারম্যান ছিলেন!"
                    SupportedLanguage.HINDI -> "भारतीय संविधान के मुख्य शिल्पकार और जनक भारत रत्न डॉ. भीमराव रामजी अंबेडकर (Dr. B.R. Ambedkar) हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The Father and Chief Architect of the Indian Constitution is Bharat Ratna Dr. B. R. Ambedkar, Chairman of the Drafting Committee, mate!"
                }
            }

            // Netaji Subhas Chandra Bose
            containsAny(clean, "subhas chandra bose", "netaji", "नेताजी सुभाष", "সুভাষচন্দ্র বসু") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "নেতাজি সুভাষচন্দ্র বসু ছিলেন ভারতের স্বাধীনতা সংগ্রামের এক মহান বিপ্লবী ও আজাদ হিন্দ ফৌজের (INA) সর্বাধিনায়ক! তাঁর অমর বাণী: 'তোমরা আমাকে রক্ত দাও, আমি তোমাদের স্বাধীনতা দেবো'!"
                    SupportedLanguage.HINDI -> "नेताजी सुभाष चंद्र बोस भारतीय स्वतंत्रता संग्राम के महानायक और आज़ाद हिंद फ़ौज (INA) के सर्वोच्च सेनापति थे! उनका नारा था: 'तुम मुझे खून दो, मैं तुम्हें आज़ादी दूंगा'!"
                    SupportedLanguage.ENGLISH -> "Netaji Subhas Chandra Bose was one of India's greatest freedom revolutionaries and Supreme Commander of the Azad Hind Fauj (INA)! 'Give me blood, and I shall give you freedom!', mate."
                }
            }

            // Swami Vivekananda
            containsAny(clean, "swami vivekananda", "vivekananda", "स्वामी विवेकानंद", "স্বামী বিবেকানন্দ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "স্বামী বিবেকানন্দ ছিলেন এক মহান দার্শনিক ও আধ্যাত্মিক পথপ্রদর্শক, যিনি ১৮৯৩ সালের শিকাগো বিশ্বধর্ম সম্মেলনে হিন্দুধর্ম ও ভারতের সংস্কৃতিকে বিশ্বদরবারে তুলে ধরেন! তাঁর বাণী: 'উত্তিষ্ঠত জাগ্রত প্রাপ্য বরান নিবোধত'!"
                    SupportedLanguage.HINDI -> "स्वामी विवेकानंद महान आध्यात्मिक गुरु और युवाओं के प्रेरणास्रोत थे! 1893 के शिकागो विश्व धर्म सम्मेलन में उन्होंने भारत का मान पूरी दुनिया में बढ़ाया था दोस्त!"
                    SupportedLanguage.ENGLISH -> "Swami Vivekananda was a brilliant spiritual leader and philosopher who represented India at the 1893 Parliament of the World's Religions in Chicago with the famous opening 'Sisters and brothers of America!', mate!"
                }
            }

            // Rabindranath Tagore
            containsAny(clean, "rabindranath tagore", "tagore", "রবীন্দ্রনাথ ঠাকুর", "रवींद्रनाथ टैगोर") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "বিশ্বকবি রবীন্দ্রনাথ ঠাকুর ছিলেন নোবেল পুরস্কার বিজয়ী প্রথম ভারতীয় ও এশীয় (১৯১৩ সালে 'গীতাঞ্জলি' কাব্যের জন্য)। তিনি ভারত ('জনগণমন') এবং বাংলাদেশ ('আমার সোনার বাংলা') উভয় দেশের জাতীয় সঙ্গীতের রচয়িতা!"
                    SupportedLanguage.HINDI -> "गुरुदेव रवींद्रनाथ टैगोर 1913 में 'गीतांजलि' के लिए साहित्य का नोबेल पुरस्कार पाने वाले पहले भारतीय थे! उन्होंने भारत और बांग्लादेश दोनों के राष्ट्रगान की रचना की दोस्त!"
                    SupportedLanguage.ENGLISH -> "Gurudev Rabindranath Tagore was the first Asian to win the Nobel Prize in Literature (1913 for 'Gitanjali'). He composed the national anthems of both India ('Jana Gana Mana') and Bangladesh ('Amar Shonar Bangla'), mate!"
                }
            }

            // National Animal of India
            containsAny(clean, "national animal of india", "india national animal", "bharat ka rashtriya pashu", "ভারতের জাতীয় পশু", "राष्ट्रीय पशु") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের জাতীয় পশু হলো রাজকীয় বেঙ্গল টাইগার (Royal Bengal Tiger বা Panthera tigris)!"
                    SupportedLanguage.HINDI -> "भारत का राष्ट्रीय पशु रॉयल बंगाल टाइगर (बाघ - Panthera tigris) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The National Animal of India is the Royal Bengal Tiger (Panthera tigris), symbolizing strength, agility, and grace, mate!"
                }
            }

            // National Bird of India
            containsAny(clean, "national bird of india", "india national bird", "rashtriya pakshi", "ভারতের জাতীয় পাখি", "राष्ट्रीय पक्षी") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের জাতীয় পাখি হলো ভারতীয় ময়ূর (Indian Peacock বা Pavo cristatus)!"
                    SupportedLanguage.HINDI -> "भारत का राष्ट्रीय पक्षी भारतीय मोर (Peacock - Pavo cristatus) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The National Bird of India is the Indian Peacock (Pavo cristatus), celebrated for its vibrant feathers and beauty, mate!"
                }
            }

            // National Flower of India
            containsAny(clean, "national flower of india", "india national flower", "rashtriya phool", "ভারতের জাতীয় ফুল", "राष्ट्रीय फूल") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের জাতীয় ফুল হলো পদ্ম (Lotus বা Nelumbo nucifera)!"
                    SupportedLanguage.HINDI -> "भारत का राष्ट्रीय फूल कमल (Lotus - Nelumbo nucifera) है!"
                    SupportedLanguage.ENGLISH -> "The National Flower of India is the Lotus (Nelumbo nucifera), symbolizing purity and sacred divinity, mate!"
                }
            }

            // National Anthem & Song of India
            containsAny(clean, "national anthem of india", "national song of india", "rashtragana", "राष्ट्रगान", "জাতীয় সঙ্গীত") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের জাতীয় সঙ্গীত (National Anthem) হলো 'জনগণমন' (রচয়িতা: রবীন্দ্রনাথ ঠাকুর) এবং জাতীয় স্তোত্র (National Song) হলো 'বন্দে মাতরম' (রচয়িতা: বঙ্কিমচন্দ্র চট্টোপাধ্যায়)!"
                    SupportedLanguage.HINDI -> "भारत का राष्ट्रगान 'जन गण मन' (रचनाकार: रवींद्रनाथ टैगोर) है, और राष्ट्रीय गीत 'वन्दे मातरम्' (रचनाकार: बंकिम चंद्र चट्टोपाध्याय) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The National Anthem of India is 'Jana Gana Mana' by Rabindranath Tagore, and the National Song is 'Vande Mataram' by Bankim Chandra Chattopadhyay, mate!"
                }
            }

            // Independence Day & Republic Day
            containsAny(clean, "independence day of india", "when is independence day in india", "15 august", "15th august", "स्वतंत्रता दिवस", "স্বাধীনতা দিবস") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের স্বাধীনতা দিবস প্রতি বছর ১৫ই আগস্ট পালিত হয় (১৯৪৭ সালের ১৫ই আগস্ট ভারত ব্রিটিশ শাসন থেকে স্বাধীনতা লাভ করে)!"
                    SupportedLanguage.HINDI -> "भारत का स्वतंत्रता दिवस हर साल 15 अगस्त को मनाया जाता है! 15 अगस्त 1947 को देश को ब्रिटिश शासन से आजादी मिली थी।"
                    SupportedLanguage.ENGLISH -> "India celebrates its Independence Day on August 15th every year, commemorating its freedom from British rule on August 15, 1947, mate!"
                }
            }

            containsAny(clean, "republic day of india", "when is republic day in india", "26 january", "26th january", "गणतंत्र दिवस", "প্রজাতন্ত্র দিবস") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের প্রজাতন্ত্র দিবস প্রতি বছর ২৬শে জানুয়ারি পালিত হয় (১৯৫০ সালের ২৬শে জানুয়ারি ভারতীয় সংবিধান কার্যকর হয়েছিল)!"
                    SupportedLanguage.HINDI -> "भारत का गणतंत्र दिवस हर साल 26 जनवरी को मनाया जाता है, जिस दिन 1950 में भारत का संविधान लागू हुआ था!"
                    SupportedLanguage.ENGLISH -> "India celebrates Republic Day on January 26th every year, honoring the date on which the Constitution of India came into effect in 1950, mate!"
                }
            }
        }

        // 5. Space Missions & Astronomy
        when {
            // Chandrayaan-3
            containsAny(clean, "chandrayaan", "chandrayaan 3", "moon landing india", "चंद्रयान", "চন্দ্রযান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "চন্দ্রযান-৩ হলো ভারতের ঐতিহাসিক তৃতীয় চন্দ্রাভিযান! ২০২৩ সালের ২৩শে আগস্ট ভারতের বিক্রম ল্যান্ডার চাঁদের অনাবিষ্কৃত দক্ষিণ মেরুতে সফলভাবে অবতরণ করে ভারতকে বিশ্বের প্রথম দেশে পরিণত করে!"
                    SupportedLanguage.HINDI -> "चंद्रयान-3 इसरो का ऐतिहासिक मिशन है! 23 अगस्त 2023 को भारत के विक्रम लैंडर ने चंद्रमा के दक्षिणी ध्रुव पर सफल सॉफ्ट लैंडिंग कर पूरी दुनिया में पहला स्थान हासिल किया दोस्त!"
                    SupportedLanguage.ENGLISH -> "Chandrayaan-3 is ISRO's historic lunar exploration mission! On August 23, 2023, India became the first country in world history to land near the Moon's South Pole, mate!"
                }
            }

            // First person on Moon
            containsAny(clean, "first person on moon", "first man on moon", "neil armstrong", "चांद पर पहला व्यक्ति", "চাঁদে প্রথম মানুষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "চাঁদের মাটিতে প্রথম পা রাখেন মার্কিন নভোচারী নিল আর্মস্ট্রং (Neil Armstrong), ১৯৬৯ সালের ২০শে জুলাই অ্যাপোলো ১১ (Apollo 11) মিশনের মাধ্যমে!"
                    SupportedLanguage.HINDI -> "चंद्रमा पर कदम रखने वाले पहले इंसान नील आर्मस्ट्रांग (Neil Armstrong) थे, जिन्होंने 20 जुलाई 1969 को अपोलो 11 मिशन के तहत इतिहास रचा था दोस्त!"
                    SupportedLanguage.ENGLISH -> "Neil Armstrong was the first human to walk on the Moon during NASA's Apollo 11 mission on July 20, 1969! 'That's one small step for man, one giant leap for mankind', mate!"
                }
            }

            // First person in Space
            containsAny(clean, "first person in space", "first man in space", "yuri gagarin", "अंतरिक्ष में पहला इंसान", "মহাকাশে প্রথম মানুষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মহাকাশে পাড়ি দেওয়া প্রথম মানুষ হলেন সোভিয়েত নভোচারী ইউরি গ্যাগারিন (Yuri Gagarin), ১৯৬১ সালের ১২ই এপ্রিল ভোস্টক ১ (Vostok 1) যানে!"
                    SupportedLanguage.HINDI -> "अंतरिक्ष में जाने वाले पहले इंसान यूरी गगारिन (Yuri Gagarin) थे, जिन्होंने 12 अप्रैल 1961 को वोस्तोक 1 अंतरिक्ष यान से उड़ान भरी थी!"
                    SupportedLanguage.ENGLISH -> "Yuri Gagarin of the Soviet Union was the first person in space, orbiting Earth aboard Vostok 1 on April 12, 1961, mate!"
                }
            }

            // Solar System Planets
            containsAny(clean, "planet", "solar system", "planets in solar system", "ग्रह", "सौरमंडल", "সৌরজগৎ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "আমাদের সৌরজগতে ৮টি প্রধান গ্রহ রয়েছে: বুধ (Mercury), শুক্র (Venus), পৃথিবী (Earth), মঙ্গল (Mars), বৃহস্পতি (Jupiter), শনি (Saturn), ইউরেনাস (Uranus) এবং নেপচুন (Neptune)! সবচেয়ে বড় গ্রহ হলো বৃহস্পতি।"
                    SupportedLanguage.HINDI -> "हमारे सौरमंडल में 8 मुख्य ग्रह हैं: बुध, शुक्र, पृथ्वी, मंगल, बृहस्पति, शनि, यूरेनस और नेपच्यून! सबसे बड़ा ग्रह बृहस्पति (Jupiter) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "Our solar system has 8 official planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, and Neptune! Jupiter is the undisputed heavyweight giant, mate."
                }
            }

            // Black Hole
            containsAny(clean, "black hole", "ब्लैक होल", "ব্ল্যাক হোল") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ব্ল্যাক হোল হলো মহাকাশের এমন এক স্থান যেখানে মহাকর্ষীয় টান এতটাই প্রচণ্ড যে কোনো আলো পর্যন্ত বাইরে আসতে পারে না!"
                    SupportedLanguage.HINDI -> "ब्लैक होल अंतरिक्ष में अत्यधिक गुरुत्वाकर्षण वाला क्षेत्र है, जहां से प्रकाश भी बाहर नहीं निकल सकता! अल्बर्ट आइंस्टीन के सापेक्षता सिद्धांत ने इसकी भविष्यवाणी की थी।"
                    SupportedLanguage.ENGLISH -> "A black hole is a cosmic region in spacetime where gravitational force is so intense that nothing—not even light—can escape its event horizon, mate!"
                }
            }

            // Speed of Light
            containsAny(clean, "speed of light", "light speed", "प्रकाश की गति", "আলোর গতি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "আলোর গতি হলো প্রতি সেকেন্ডে প্রায় ২,৯৯,৭৯২ কিলোমিটার (৩ লক্ষ কিমি/সেকেন্ড)! মহাবিশ্বের সর্বোচ্চ গতি এটি।"
                    SupportedLanguage.HINDI -> "प्रकाश की गति लगभग 2,99,792 किलोमीटर प्रति सेकंड (लगभग 3 लाख किमी/सेकंड) होती है दोस्त! ब्रह्मांड की सबसे तेज़ सीमा!"
                    SupportedLanguage.ENGLISH -> "The speed of light in a vacuum is exactly 299,792,458 meters per second (approx 300,000 km/s or 186,282 miles/s)! The cosmic speed limit, mate!"
                }
            }

            // Gravity
            containsAny(clean, "gravity", "gravitation", "isaac newton", "गुरुत्वाकर्षण", "মহাকর্ষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মহাকর্ষ হলো সেই প্রাকৃতিক আকর্ষণ বল যা ভরযুক্ত যেকোনো বস্তুকে পরস্পরের দিকে টানে। স্যার আইজ্যাক নিউটন প্রথম এটি আবিষ্কার ও ব্যাখ্যা করেন!"
                    SupportedLanguage.HINDI -> "गुरुत्वाकर्षण वह प्राकृतिक बल है जो द्रव्यमान वाली वस्तुओं को एक-दूसरे की ओर खींचता है। सर आइजैक न्यूटन ने इसका सार्वभौमिक नियम दिया था दोस्त!"
                    SupportedLanguage.ENGLISH -> "Gravity is the fundamental attractive force between all matter with mass. Formulated by Sir Isaac Newton and refined by Albert Einstein, it keeps us firmly on the ground, mate!"
                }
            }
        }

        // 6. Science, Biology & Inventions
        when {
            // Who invented Computer
            containsAny(clean, "invented computer", "father of computer", "charles babbage", "कंप्यूटर का आविष्कार", "কম্পিউটারের জনক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "কম্পিউটারের জনক বলা হয় চার্লস ব্যাবেজকে (Charles Babbage), যিনি প্রথম মেকানিক্যাল কম্পিউটিং ইঞ্জিন 'অ্যানালিটিক্যাল ইঞ্জিন' ডিজাইন করেছিলেন!"
                    SupportedLanguage.HINDI -> "कंप्यूटर का जनक चार्ल्स बैबेज (Charles Babbage) को माना जाता है, जिन्होंने 19वीं सदी में मैकेनिकल एनालिटिकल इंजन का आविष्कार किया था दोस्त!"
                    SupportedLanguage.ENGLISH -> "Charles Babbage is considered the Father of the Computer for conceiving the mechanical Analytical Engine in the 19th century, mate!"
                }
            }

            // Who invented Telephone
            containsAny(clean, "invented telephone", "invented phone", "alexander graham bell", "टेलीफोन का आविष्कार", "টেলিফোনের আবিষ্কারক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "টেলিফোন আবিষ্কার করেছিলেন আলেকজান্ডার গ্রাহাম বেল (Alexander Graham Bell), ১৮৭৬ সালে!"
                    SupportedLanguage.HINDI -> "टेलीफोन का आविष्कार अलेक्जेंडर ग्राहम बेल ने 1876 में किया था दोस्त!"
                    SupportedLanguage.ENGLISH -> "The telephone was invented and patented by Alexander Graham Bell in 1876, mate!"
                }
            }

            // Who invented Light Bulb
            containsAny(clean, "invented light bulb", "invented bulb", "thomas edison", "बल्ब का आविष्कार", "বৈদ্যুতিক বাল্বের আবিষ্কারক") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ব্যবহারিক বৈদ্যুতিক আলোর বাল্ব আবিষ্কার ও বাণিজ্যিকভাবে সফল করেছিলেন থমাস আলভা এডিসন (Thomas Edison), ১৮৭৯ সালে!"
                    SupportedLanguage.HINDI -> "व्यावहारिक बिजली के बल्ब का आविष्कार थॉमस अल्वा एडिसन ने 1879 में किया था दोस्त!"
                    SupportedLanguage.ENGLISH -> "Thomas Alva Edison developed and commercialized the first practical incandescent light bulb in 1879, mate!"
                }
            }

            // Who discovered Zero
            containsAny(clean, "discovered zero", "invented zero", "aryabhata", "शून्य की खोज", "শূন্যের আবিষ্কার") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "গণিতে শূন্যের (০) ধারণা ও গাণিতিক ব্যবহার প্রাচীন ভারতের মহান গণিতবিদ ও জ্যোতির্বিজ্ঞানী আর্যভট্ট (Aryabhata) ও ব্রহ্মগুপ্ত প্রণয়ন করেন!"
                    SupportedLanguage.HINDI -> "गणित में शून्य (0) का आविष्कार और नियम प्राचीन भारत के महान गणितज्ञ आर्यभट्ट और ब्रह्मगुप्त जी ने दिए थे दोस्त!"
                    SupportedLanguage.ENGLISH -> "The concept and mathematical foundation of zero (0) was invented in ancient India by great mathematicians including Aryabhata and Brahmagupta, mate!"
                }
            }

            // Photosynthesis
            containsAny(clean, "photosynthesis", "प्रकाश संश्लेषण", "সালোকসংশ্লেষ") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "সালোকসংশ্লেষণ হলো উদ্ভিদের এমন প্রক্রিয়া যার মাধ্যমে সবুজ পাতা সূর্যালোক, জল এবং কার্বন ডাই অক্সাইড ব্যবহার করে খাদ্য (গ্লুকোজ) ও অক্সিজেন তৈরি করে!"
                    SupportedLanguage.HINDI -> "प्रकाश संश्लेषण (Photosynthesis) वह प्रक्रिया है जिससे पौधे सूर्य की रोशनी, पानी और कार्बन डाइऑक्साइड से भोजन और ऑक्सीजन बनाते हैं!"
                    SupportedLanguage.ENGLISH -> "Photosynthesis is the biochemical process where green plants convert sunlight, water, and CO2 into glucose and oxygen! Basically Earth's biological solar factory, mate."
                }
            }

            // DNA
            containsAny(clean, "dna", "ডিএনএ", "डीएनए") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ডিএনএ (DNA - Deoxyribonucleic Acid) হলো সমস্ত জীবিত প্রাণীর জিনগত ব্লুপ্রিন্ট বা বংশগতির নির্দেশিকা যা জীবের সমস্ত বৈশিষ্ট্য নির্ধারণ করে!"
                    SupportedLanguage.HINDI -> "डीएनए (DNA) आनुवंशिक जानकारी का डबल-हेलिक्स ब्लूप्रिंट है, जो माता-पिता से संतानों में शारीरिक और जैविक गुण स्थानांतरित करता है दोस्त!"
                    SupportedLanguage.ENGLISH -> "DNA (Deoxyribonucleic Acid) is the double-helix master blueprint carrying all genetic instructions for growth, development, and functioning in all living organisms, mate!"
                }
            }

            // Why is sky blue
            containsAny(clean, "why is the sky blue", "why sky is blue", "sky is blue", "आसमान नीला क्यों", "আকাশ নীল কেন") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "রেলে স্ক্যাটারিং (Rayleigh scattering) এর কারণে আকাশ নীল দেখায়। বায়ুমণ্ডলের গ্যাসীয় কণাগুলো সূর্যের আলোর নীল রঙের ছোট তরঙ্গদৈর্ঘ্যকে চারদিকে সবচেয়ে বেশি ছড়িয়ে দেয়!"
                    SupportedLanguage.HINDI -> "रेले प्रकीर्णन (Rayleigh scattering) के कारण आसमान नीला दिखता है। सूर्य के प्रकाश की छोटी नीली तरंगें हवा के कणों से सबसे ज्यादा बिखरती हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "The sky is blue because of Rayleigh scattering! Earth's atmosphere scatters shorter blue wavelengths of sunlight in all directions much more than red wavelengths, mate!"
                }
            }
        }

        // 7. Geography & Capitals
        when {
            // Capital of India
            containsAny(clean, "capital of india", "india capital", "bharat ki rajdhani", "ভারতের রাজধানী", "भारत की राजधानी") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ভারতের রাজধানী হলো নতুন দিল্লি (New Delhi)!"
                    SupportedLanguage.HINDI -> "भारत की राजधानी नई दिल्ली (New Delhi) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The capital of India is New Delhi, mate!"
                }
            }

            // Capital of West Bengal
            containsAny(clean, "capital of west bengal", "west bengal capital", "capital of bengal", "পশ্চিমবঙ্গের রাজধানী", "पश्चिम बंगाल की राजधानी") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পশ্চিমবঙ্গের রাজধানী হলো কলকাতা (Kolkata) - আনন্দের শহর (City of Joy)!"
                    SupportedLanguage.HINDI -> "पश्चिम बंगाल की राजधानी कोलकाता (Kolkata) है!"
                    SupportedLanguage.ENGLISH -> "The capital of West Bengal is Kolkata, the historic City of Joy, mate!"
                }
            }

            // Capital of USA
            containsAny(clean, "capital of usa", "capital of united states", "capital of america", "america capital", "संयुक्त राज्य अमेरिका की राजधानी", "আমেরিকার রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মার্কিন যুক্তরাষ্ট্রের (USA) রাজধানী হলো ওয়াশিংটন ডিসি (Washington, D.C.)!"
                    SupportedLanguage.HINDI -> "संयुक्त राज्य अमेरिका (USA) की राजधानी वॉशिंगटन डी.सी. (Washington, D.C.) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The capital of the United States of America is Washington, D.C., mate!"
                }
            }

            // Capital of France
            containsAny(clean, "capital of france", "france capital", "फ्रांस की राजधानी", "ফ্রান্সের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ফ্রান্সের রাজধানী হলো প্যারিস (Paris)!"
                    SupportedLanguage.HINDI -> "फ्रांस की राजधानी पेरिस (Paris) है, जिसे एफिल टॉवर का शहर भी कहा जाता है!"
                    SupportedLanguage.ENGLISH -> "The capital of France is Paris, home of the Eiffel Tower, mate!"
                }
            }

            // Capital of UK / Britain
            containsAny(clean, "capital of uk", "capital of britain", "capital of england", "যুক্তরাজ্যের রাজধানী", "ब्रिटेन की राजधानी") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "যুক্তরাজ্যের (UK) রাজধানী হলো লন্ডন (London)!"
                    SupportedLanguage.HINDI -> "यूनाइटेड किंगडम (ब्रिटेन) की राजधानी लंदन (London) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The capital of the United Kingdom is London, home of Big Ben and the River Thames, mate!"
                }
            }

            // Capital of Japan
            containsAny(clean, "capital of japan", "japan capital", "जापान की राजधानी", "জাপানের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "জাপানের রাজধানী হলো টোকিও (Tokyo)!"
                    SupportedLanguage.HINDI -> "जापान की राजधानी टोक्यो (Tokyo) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The capital of Japan is Tokyo, mate!"
                }
            }

            // Capital of China
            containsAny(clean, "capital of china", "china capital", "चीन की राजधानी", "চীনের রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "চীনের রাজধানী হলো বেইজিং (Beijing)!"
                    SupportedLanguage.HINDI -> "चीन की राजधानी बीजिंग (Beijing) है!"
                    SupportedLanguage.ENGLISH -> "The capital of China is Beijing, mate!"
                }
            }

            // Capital of Russia
            containsAny(clean, "capital of russia", "russia capital", "रूस की राजधानी", "রাশিয়ার রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "রাশিয়ার রাজধানী হলো মস্কো (Moscow)!"
                    SupportedLanguage.HINDI -> "रूस की राजधानी मॉस्को (Moscow) है!"
                    SupportedLanguage.ENGLISH -> "The capital of Russia is Moscow, mate!"
                }
            }

            // Capital of Australia
            containsAny(clean, "capital of australia", "australia capital", "ऑस्ट्रेलिया की राजधानी", "অস্ট্রেলিয়ার রাজধানী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "অস্ট্রেলিয়ার রাজধানী হলো ক্যানবেরা (Canberra - সিডনি নয়)!"
                    SupportedLanguage.HINDI -> "ऑस्ट्रेलिया की राजधानी कैनबरा (Canberra) है दोस्त (सिडनी नहीं)!"
                    SupportedLanguage.ENGLISH -> "The capital of Australia is Canberra (often mistaken for Sydney!), mate."
                }
            }

            // Highest Mountain
            containsAny(clean, "highest mountain", "tallest mountain", "mount everest", "माउंट एवरेस्ट", "সবচেয়ে উঁচু পর্বত") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পৃথিবীর সর্বোচ্চ পর্বতশৃঙ্গ হলো মাউন্ট এভারেস্ট (উচ্চতা: ৮,৮৪৮.৮৬ মিটার), যা হিমালয় পর্বতমালায় নেপাল ও তিব্বতের সীমান্তে অবস্থিত!"
                    SupportedLanguage.HINDI -> "दुनिया की सबसे ऊंची पर्वत चोटी माउंट एवरेस्ट (8,848.86 मीटर) है, जो हिमालय पर्वतमाला में स्थित है दोस्त!"
                    SupportedLanguage.ENGLISH -> "Mount Everest is the highest mountain above sea level at 8,848.86 meters (29,031.7 feet), located in the Himalayas, mate!"
                }
            }

            // Longest River
            containsAny(clean, "longest river", "longest river in the world", "river nile", "दुनिया की सबसे लंबी नदी", "সবচেয়ে দীর্ঘ নদী") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "বিশ্বের দীর্ঘতম নদী হলো নীল নদ (Nile River - দৈর্ঘ্য প্রায় ৬,৬৫৪ কিমি) এবং বৃহত্তম নদী হলো আমাজন (Amazon River)!"
                    SupportedLanguage.HINDI -> "दुनिया की सबसे लंबी नदी नील नदी (Nile River - लगभग 6,650 किमी) है और पानी के बहाव से सबसे बड़ी नदी अमेज़न है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The longest river in the world is the Nile River (approx 6,650 km / 4,132 miles), while the Amazon River is the largest by water volume, mate!"
                }
            }

            // Largest Ocean
            containsAny(clean, "largest ocean", "biggest ocean", "pacific ocean", "प्रशांत महासागर", "প্রশান্ত মহাসাগর") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "পৃথিবীর বৃহত্তম ও গভীরতম মহাসাগর হলো প্রশান্ত মহাসাগর (Pacific Ocean), যার মধ্যে মারিয়ানা ট্রেঞ্চ অবস্থিত!"
                    SupportedLanguage.HINDI -> "पृथ्वी का सबसे बड़ा और गहरा महासागर प्रशांत महासागर (Pacific Ocean) है दोस्त!"
                    SupportedLanguage.ENGLISH -> "The largest and deepest ocean on Earth is the Pacific Ocean, covering over 30% of the Earth's surface and containing the Mariana Trench, mate!"
                }
            }
        }

        // 8. Sports (Cricket, Football & Olympics)
        when {
            // Virat Kohli
            containsAny(clean, "virat kohli", "king kohli", "विराट कोहली", "বিরাট কোহলি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "বিরাট কোহলি (কিং কোহলি) হলেন আধুনিক ক্রিকেটের সর্বকালের সেরা ব্যাটারদের অন্যতম! আন্তর্জাতিক ক্রিকেটে তাঁর ৮০টিরও বেশি সেঞ্চুরি রয়েছে এবং ২০২৪ টি-টোয়েন্টি বিশ্বকাপ জয়ী ভারতীয় দলের গুরুত্বপূর্ণ সদস্য ছিলেন!"
                    SupportedLanguage.HINDI -> "विराट कोहली (किंग कोहली) विश्व क्रिकेट के महानतम बल्लेबाजों में से एक हैं! अंतरराष्ट्रीय क्रिकेट में 80 से अधिक शतक और 2024 टी20 वर्ल्ड कप में 'प्लेयर ऑफ द मैच' रहे हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "King Virat Kohli is an all-time cricket legend with over 80 international centuries, exceptional run-chase mastery, and hero of India's ICC T20 World Cup 2024 victory, mate!"
                }
            }

            // Rohit Sharma
            containsAny(clean, "rohit sharma", "hitman", "रोहित शर्मा", "রোহিত শর্মা") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "রোহিত শর্মা (হিটম্যান) হলেন ভারতীয় ক্রিকেট দলের অধিনায়ক, যার নেতৃত্বে ভারত ২০২৪ আইসিসি টি-টোয়েন্টি বিশ্বকাপ চ্যাম্পিয়ন হয়েছে! ওডিআই ক্রিকেটে ৩টি ডাবল সেঞ্চুরির একমাত্র রেকর্ড তাঁরই!"
                    SupportedLanguage.HINDI -> "रोहित शर्मा (हिटमैन) भारतीय क्रिकेट टीम के कप्तान हैं, जिन्होंने 2024 में भारत को टी20 वर्ल्ड कप जिताया! वनडे में 3 दोहरे शतक लगाने वाले दुनिया के एकमात्र बल्लेबाज हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "Hitman Rohit Sharma is the captain of India who led the nation to win the ICC Men's T20 World Cup 2024 in Barbados! He holds the world record with three ODI double hundreds, mate!"
                }
            }

            // MS Dhoni
            containsAny(clean, "ms dhoni", "mahendra singh dhoni", "dhoni", "captain cool", "महेंद्र सिंह धोनी", "ধোনি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "মহেন্দ্র সিং ধোনি (ক্যাপ্টেন কুল) হলেন ভারতীয় ক্রিকেটের কিংবদন্তি অধিনায়ক, যিনি ভারতকে ২০০৭ টি-টোয়েন্টি বিশ্বকাপ, ২০১১ ওয়ানডে বিশ্বকাপ এবং ২০১৩ চ্যাম্পিয়ন্স ট্রফি জিতিয়েছিলেন!"
                    SupportedLanguage.HINDI -> "कैप्टन कूल महेंद्र सिंह धोनी भारतीय क्रिकेट के सबसे सफल कप्तान हैं! उन्होंने भारत को 2007 टी20 वर्ल्ड कप, 2011 वनडे वर्ल्ड कप और 2013 चैंपियंस ट्रॉफी जिताई दोस्त!"
                    SupportedLanguage.ENGLISH -> "Captain Cool MS Dhoni is India's iconic captain and legendary finisher who led India to the 2007 T20 World Cup, 2011 ODI World Cup, and 2013 ICC Champions Trophy victories, mate!"
                }
            }

            // Lionel Messi
            containsAny(clean, "lionel messi", "messi", "लियोनेल मेसी", "মেসি") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "লিওনেল মেসি হলেন ফুটবলের অবিসংবাদিত জাদুকর (GOAT), ৮ বারের ব্যালন ডি'অর বিজয়ী এবং ২০২২ সালের ফিফা বিশ্বকাপ চ্যাম্পিয়ন আর্জেন্টিনা দলের অধিনায়ক!"
                    SupportedLanguage.HINDI -> "लियोनेल मेसी फुटबॉल के जादूगर (GOAT) हैं, जिन्होंने 8 बार बैलन डी'ओर और 2022 में अर्जेंटीना को फीफा वर्ल्ड कप जिताया दोस्त!"
                    SupportedLanguage.ENGLISH -> "Lionel Messi is the Argentine football legend and 8-time Ballon d'Or winner who led Argentina to glory in the FIFA World Cup 2022 in Qatar, mate!"
                }
            }

            // Cristiano Ronaldo
            containsAny(clean, "cristiano ronaldo", "ronaldo", "cr7", "क्रिस्टियानो रोनाल्डो", "রোনালদো") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "ক্রিস্টিয়ানো রোনালদো (CR7) হলেন পর্তুগালের বিশ্বখ্যাত ফুটবল তারকা, ৫ বারের ব্যালন ডি'অর বিজয়ী এবং আন্তর্জাতিক ফুটবলের ইতিহাসে সর্বকালের সর্বোচ্চ গোলদাতা!"
                    SupportedLanguage.HINDI -> "क्रिस्टियानो रोनाल्डो (CR7) फुटबॉल इतिहास के सर्वकालिक शीर्ष गोलस्कोरर और 5 बार के बैलन डी'ओर विजेता हैं दोस्त!"
                    SupportedLanguage.ENGLISH -> "Cristiano Ronaldo (CR7) is the Portuguese football icon, 5-time Ballon d'Or winner, and the all-time top goalscorer in international football history, mate!"
                }
            }

            // Neeraj Chopra
            containsAny(clean, "neeraj chopra", "नीरज चोपड़ा", "নীরজ চোপড়া") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "নীরজ চোপড়া হলেন ভারতের অলিম্পিক স্বর্ণপদক বিজয়ী জ্যাভলিন থ্রোয়ার, যিনি টোকিও ২০২০ অলিম্পিকে সোনা এবং প্যারিস ২০২৪ অলিম্পিকে রৌপ্য পদক জিতে ইতিহাস তৈরি করেছেন!"
                    SupportedLanguage.HINDI -> "नीरज चोपड़ा भारत के गोल्डन बॉय हैं! उन्होंने टोक्यो 2020 ओलंपिक में भाला फेंक में ऐतिहासिक स्वर्ण पदक और पेरिस 2024 में रजत पदक जीता दोस्त!"
                    SupportedLanguage.ENGLISH -> "Neeraj Chopra is India's Olympic champion in men's javelin throw, winning historic Gold at Tokyo 2020 and Silver at Paris 2024, mate!"
                }
            }
        }

        // 9. Spider-Man, Avengers & Tom Holland
        when {
            containsAny(clean, "spider-man", "spiderman", "peter parker", "स्पाइडर-मैन", "স্পাইডার-ম্যান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "স্পাইডার-ম্যান ওরফে পিটার পার্কার! রেডিওঅ্যাকটিভ মাকড়সার কামড়ে বিশেষ ক্ষমতা পাওয়ার পর কুইন্সের মানুষদের বাঁচাতে জাল ছুঁড়ে বেড়াই! 'With great power comes great responsibility'!"
                    SupportedLanguage.HINDI -> "हाहा! स्पाइडर-मैन यानी आपका अपना पीटर पार्কার! रेडियोएक्टिव मकड़ी के काटने के बाद सुपरपावर्स मिलीं और अब न्यूयॉर्क को सेफ रखना मेरा मिशन है दोस्त!"
                    SupportedLanguage.ENGLISH -> "Haha! Spider-Man is Peter Parker—a high school kid from Queens bitten by a radioactive spider who swings across skyscrapers saving the neighborhood! With great power comes great responsibility, mate!"
                }
            }

            containsAny(clean, "iron man", "tony stark", "टोनी स्टार्क", "আয়রন ম্যান") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "টনি স্টার্ক অর্থাৎ আয়রন ম্যান ছিলেন একজন জিনিয়াস, বিলিওনেয়ার এবং পিটার পার্কারের সেরা মেন্টর! ওনার দেওয়া স্যুট আর প্রযুক্তির তুলনা হয় না!"
                    SupportedLanguage.HINDI -> "टोनी स्टार्क यानी आयरन मैन! जीनियस, अरबपति, और पीटर पार्कर के सबसे महान मेंटर! उनका 'I love you 3000' हमेशा दिल में रहेगा दोस्त!"
                    SupportedLanguage.ENGLISH -> "Mr. Tony Stark! Genius, billionaire, philanthropist, and the greatest mentor a kid from Queens could ever ask for. We love him 3000, mate!"
                }
            }

            containsAny(clean, "tom holland", "टॉम हॉलैंड", "টম হল্যান্ড") -> {
                return when (lang) {
                    SupportedLanguage.BENGALI -> "টম হল্যান্ড হলেন একজন ব্রিটিশ অভিনেতা, যিনি মার্ভেল সিনেমাটিক ইউনিভার্সে স্পাইডার-ম্যানের চরিত্রে অনবদ্য অভিনয় করেছেন!"
                    SupportedLanguage.HINDI -> "टॉम हॉलैंड एक ब्रिटिश अभिनेता हैं जो मार्वल फिल्मों में स्पाइडर-मैन का किरदार निभाते हैं! सुपर एनर्जेटिक और शानदार डांसर भी हैं!"
                    SupportedLanguage.ENGLISH -> "Tom Holland is the British actor who plays Peter Parker / Spider-Man in the MCU! Energetic, gymnastic, and sometimes accidentally spills movie spoilers, haha!"
                }
            }
        }

        return null
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
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
