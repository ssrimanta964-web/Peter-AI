with open('app/src/main/java/com/example/feature/voice/PeterWakeWordDetector.kt', 'r') as f:
    content = f.read()

old_is = """    fun isWakeWord(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        val text = rawText.lowercase(Locale.ROOT).trim()

        // Direct pattern check
        if (wakePatterns.any { pattern -> text.contains(pattern) }) {
            return true
        }

        // Regex pattern check
        if (wakeRegex.containsMatchIn(text)) {
            return true
        }

        // Token starts-with check (e.g. "peter", "piter", "spiderman", "পিটার", "पीटर")
        val tokens = text.split(Regex("[\\s,?.!]+"))
        return tokens.any { token ->
            token == "peter" || token == "piter" || token == "pete" || token == "spiderman" || token == "spider-man" || token == "পিটার" || token == "पीटर"
        }
    }"""

new_is = """    fun isWakeWord(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        val text = rawText.lowercase(java.util.Locale.ROOT).trim()
        
        // Very permissive fallback: if the phrase has any of these words anywhere
        val buzzwords = listOf("peter", "piter", "pete", "spiderman", "spider-man", "hey", "hello", "hi")
        
        // Direct pattern check
        if (wakePatterns.any { pattern -> text.contains(pattern) }) {
            return true
        }

        if (wakeRegex.containsMatchIn(text)) {
            return true
        }

        val tokens = text.split(Regex("[\\s,?.!]+"))
        if (tokens.any { token -> token in buzzwords }) {
            return true
        }
        
        return false
    }"""

content = content.replace(old_is, new_is)

with open('app/src/main/java/com/example/feature/voice/PeterWakeWordDetector.kt', 'w') as f:
    f.write(content)
