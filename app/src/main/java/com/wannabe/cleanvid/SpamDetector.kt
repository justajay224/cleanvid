package com.wannabe.cleanvid

object SpamDetector {

    private val spamKeywords = setOf(
        "judi", "judol", "slot", "gacor", "maxwin", "situs", "rtp", "pragmatic", "wd", "jp", "lapakwd", "jepe", "garudahoki", "jepi",
        "pulauwin"
    )

    private const val FANCY_TEXT_THRESHOLD = 4

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]"), "") // Hanya ambil huruf dan angka
            .replace("1", "i")
            .replace("0", "o")
            .replace("4", "a")
            .replace("3", "e")
            .replace("5", "s")
            .replace("7", "t")
            .replace("8", "b")
            .replace("6", "g")
            .replace("9", "g")
    }

    private fun isKeywordSpam(comment: String): Boolean {
        val normalizedComment = normalizeText(comment)
        if (normalizedComment.isEmpty()) return false
        return spamKeywords.any { keyword ->
            normalizedComment.contains(keyword)
        }
    }

    private fun containsAbnormalText(text: String): Boolean {

        val emojiRegex = Regex("[\\p{So}\\p{Cs}\\p{Cf}\\p{Co}]+")

        val textWithoutEmojis = text.replace(emojiRegex, "")

        val abnormalCharsOnly = textWithoutEmojis.lowercase()
            .replace(Regex("[a-z0-9\\s.,!?@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|<>/?~`]+"), "")

        return abnormalCharsOnly.length > FANCY_TEXT_THRESHOLD
    }

    fun isSpam(comment: String): Boolean {
        if (containsAbnormalText(comment)) {
            return true
        }

        if (isKeywordSpam(comment)) {
            return true
        }

        return false
    }
}