package com.example.domain.ai

import java.util.Locale

enum class SupportedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi"),
    BENGALI("bn", "Bengali")
}

object LanguageHelper {

    private val BENGALI_KEYWORDS = setOf(
        "kemon", "acho", "achis", "korcho", "korchis", "bhalo", "tomar", "tomader", "apnar",
        "amar", "koutuk", "golpo", "shomoy", "tarikh", "jalao", "nibhao", "bondho", "chalu",
        "khulo", "dhonnobad", "shonao", "khobor", "kichu", "kobe", "kothay", "eta", "ota",
        "bolo", "bolun", "shunao", "shune", "dekhao", "gaan", "bos", "sroshta", "ke", "ki",
        "koto", "koren", "korchen", "achen", "shunte", "parcho", "shon", "shuncho"
    )

    private val HINDI_KEYWORDS = setOf(
        "kya", "hai", "kaise", "karo", "kare", "karna", "batao", "sunao", "kitna", "kitni",
        "kaun", "tumhara", "tumhari", "aapka", "aapki", "mera", "meri", "namaste", "shukriya",
        "dhanyawad", "badhao", "ghatao", "band", "chalu", "kholo", "samay", "tareekh", "baat",
        "bolo", "hoga", "hogi", "bhai", "yaar", "kuch", "kaunsa", "kaisi", "kaise", "kahan",
        "kisko", "hum", "aap", "tum", "malik", "suno", "sunao", "gaana", "baatein"
    )

    fun detectLanguage(text: String): SupportedLanguage {
        if (text.isBlank()) return SupportedLanguage.ENGLISH

        // 1. Script checks (Direct Unicode)
        var devanagariCount = 0
        var bengaliCount = 0
        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) devanagariCount++
            if (code in 0x0980..0x09FF) bengaliCount++
        }

        if (bengaliCount > 0 && bengaliCount >= devanagariCount) {
            return SupportedLanguage.BENGALI
        }
        if (devanagariCount > 0) {
            return SupportedLanguage.HINDI
        }

        // 2. Romanized keywords analysis (Hinglish & Banglish)
        val words = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        var bnKeywordScore = 0
        var hiKeywordScore = 0

        for (w in words) {
            if (BENGALI_KEYWORDS.contains(w)) bnKeywordScore++
            if (HINDI_KEYWORDS.contains(w)) hiKeywordScore++
        }

        return when {
            bnKeywordScore > hiKeywordScore && bnKeywordScore > 0 -> SupportedLanguage.BENGALI
            hiKeywordScore > bnKeywordScore && hiKeywordScore > 0 -> SupportedLanguage.HINDI
            else -> SupportedLanguage.ENGLISH
        }
    }
}
