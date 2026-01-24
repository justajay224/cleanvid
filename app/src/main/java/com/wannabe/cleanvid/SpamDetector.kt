package com.wannabe.cleanvid

import java.text.Normalizer

object SpamDetector {

    private val spamKeywords = setOf(
        "slot", "gacor", "maxwin", "rtp", "pragmatic", "wd", "jp",
        "lapakwd", "jepe", "garudahoki", "hoki", "jepi", "pulauwin",
        "hariantoto", "pulau777", "kyt4d", "kytad", "pulauttt",
        "togel", "toto", "bonus", "deposit", "withdraw"
    )

    private const val ABNORMAL_CHAR_THRESHOLD = 5

    // ---------------------------
    // Normalisasi Super Agresif
    // ---------------------------
    private fun normalizeSuperAggressively(text: String): String {
        var t = text
            .replace("🅰", "a").replace("🅐", "a").replace("ⓐ", "a")
            .replace("🅱", "b").replace("🅑", "b").replace("ⓑ", "b")
            .replace("🅲", "c").replace("🅒", "c").replace("ⓒ", "c")
            .replace("🅳", "d").replace("🅓", "d").replace("ⓓ", "d")
            .replace("🅴", "e").replace("🅔", "e").replace("ⓔ", "e")
            .replace("🅵", "f").replace("🅕", "f").replace("ⓕ", "f")
            .replace("🅶", "g").replace("🅖", "g").replace("ⓖ", "g")
            .replace("🅷", "h").replace("🅗", "h").replace("ⓗ", "h")
            .replace("🅸", "i").replace("🅘", "i").replace("ⓘ", "i")
            .replace("🅹", "j").replace("🅙", "j").replace("ⓙ", "j")
            .replace("🅺", "k").replace("🅚", "k").replace("ⓚ", "k")
            .replace("🅻", "l").replace("🅛", "l").replace("ⓛ", "l")
            .replace("🅼", "m").replace("🅜", "m").replace("ⓜ", "m")
            .replace("🅽", "n").replace("🅝", "n").replace("ⓝ", "n")
            .replace("🅾", "o").replace("🅞", "o").replace("ⓞ", "o")
            .replace("🅿", "p").replace("🅟", "p").replace("ⓟ", "p")
            .replace("🆀", "q").replace("🅠", "q").replace("ⓠ", "q")
            .replace("🆁", "r").replace("🅡", "r").replace("ⓡ", "r")
            .replace("🆂", "s").replace("🅢", "s").replace("ⓢ", "s")
            .replace("🆃", "t").replace("🅣", "t").replace("ⓣ", "t")
            .replace("🆄", "u").replace("🅤", "u").replace("ⓤ", "u")
            .replace("🆅", "v").replace("🅥", "v").replace("ⓥ", "v")
            .replace("🆆", "w").replace("🅦", "w").replace("ⓦ", "w")
            .replace("🆇", "x").replace("🅧", "x").replace("ⓧ", "x")
            .replace("🆈", "y").replace("🅨", "y").replace("ⓨ", "y")
            .replace("🆉", "z").replace("🅩", "z").replace("ⓩ", "z")

        t = Normalizer.normalize(t, Normalizer.Form.NFKC)
            .lowercase()

        // simple leetspeak normalization
        t = t.replace("1", "i")
            .replace("0", "o")
            .replace("4", "a")
            .replace("3", "e")
            .replace("5", "s")
            .replace("7", "t")
            .replace("8", "b")
            .replace("6", "g")
            .replace("9", "g")

        // remove all non-letter chars (we keep only a-z)
        return t.replace(Regex("[^a-z]"), "")
    }

    // ---------------------------
    // Levenshtein (internal)
    // ---------------------------
    // Mengembalikan distance antara dua string
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[a.length][b.length]
    }

    // ---------------------------
    // Deteksi kata kunci (fuzzy)
    // ---------------------------
    private fun isKeywordSpam(comment: String): Boolean {
        val normalizedComment = normalizeSuperAggressively(comment)
        if (normalizedComment.isEmpty()) return false

        for (keyword in spamKeywords) {
            val nk = normalizeSuperAggressively(keyword)
            // langsung mengandung
            if (normalizedComment.contains(nk)) {
                android.util.Log.d("SpamDetectorKeyword", "Direct keyword match: '$keyword' in '$normalizedComment'")
                return true
            }
            // fuzzy: jika distance kecil terhadap substring manapun ukuran nk
            // cek setiap substring panjang nk..nk+3 (untuk toleransi)
            if (nk.length > 3 && nk.length <= normalizedComment.length) {
                // cek rolling substrings di normalizedComment
                for (start in 0..(normalizedComment.length - nk.length)) {
                    val sub = normalizedComment.substring(start, start + nk.length)
                    val d = levenshtein(sub, nk)
                    if (d <= 1) {
                        android.util.Log.d("SpamDetectorKeyword", "Fuzzy keyword match: '$keyword' ~ '$sub' (d=$d)")
                        return true
                    }
                }
            }
        }
        return false
    }

    // ---------------------------
    // Pola promosi (regex)
    // ---------------------------
    private fun containsPromotionPattern(text: String): Boolean {
        val lower = text.lowercase()
        val patterns = listOf(
            Regex("\\b(daftar|join|main|coba|gasin|langsung|klik|ayo|buruan)\\b.*\\b(slot|gacor|maxwin|jp)\\b"),
            Regex("\\b\\w{3,}\\d{2,}\\b"), // SGI88, Pulau777
            Regex("\\bhttps?://\\S+\\b")   // link
        )
        return patterns.any { it.containsMatchIn(lower) }
    }

    // Deteksi teks dengan huruf gaya emoji (🅰🅱🆎🅾 dsb)
    private fun containsStylizedEmojiLetters(text: String): Boolean {
        // Rentang karakter untuk blok Unicode "Enclosed Alphanumeric" (huruf dalam kotak)
        val regex = Regex("[🄰-🆉🅰-🆉ⓐ-ⓩ]+")
        val matchCount = regex.findAll(text).sumOf { it.value.length }
        return matchCount >= 3 // minimal 3 karakter "emoji letter"
    }

    private fun containsStylizedUnicodeLetters(text: String): Boolean {
        // Rentang Unicode untuk karakter Mathematical Bold, Italic, Fraktur, Script, dsb
        val regex = Regex("[\\uD835\\uDC00-\\uD835\\uDFFF]+")
        val matchCount = regex.findAll(text).sumOf { it.value.length }
        return matchCount >= 3 // minimal 3 karakter stylized
    }

    // Deteksi pola mencurigakan seperti KYT4️⃣D, W1N4, atau kombinasi huruf + emoji angka
    private fun containsAlphaNumericEmojiCombo(text: String): Boolean {
        // Emoji angka/keycap range (\u0030-0039 + \uFE0F + \u20E3)
        val keycapEmoji = Regex("[\\u0030-\\u0039]\\uFE0F?\\u20E3")

        // Pola huruf + angka atau sebaliknya (misalnya KYT4D, W1N, 4DWIN)
        val mixedAlphaNum = Regex("(?i)[A-Z]+\\d+[A-Z]*|\\d+[A-Z]+[0-9A-Z]*")

        // Deteksi simbol panah, variasi tanda, atau unicode khusus (biasanya digunakan spam)
        val weirdSymbols = Regex("[→←↔➡️⬅️⬆️⬇️]+")

        val hasMixed = mixedAlphaNum.containsMatchIn(text)
        val hasKeycapEmoji = keycapEmoji.containsMatchIn(text)
        val hasWeirdSymbols = weirdSymbols.containsMatchIn(text)

        // Jika ada kombinasi huruf+angka dan ada emoji angka atau simbol mencolok
        return (hasMixed && (hasKeycapEmoji || hasWeirdSymbols)) || hasKeycapEmoji
    }



    // ---------------------------
    // Suspicious structure: keyword density
    // ---------------------------
    private fun hasSuspiciousStructure(comment: String): Boolean {
        val words = comment.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        val keywordCount = words.count { w -> spamKeywords.any { k -> w.contains(k) } }
        val ratio = keywordCount.toDouble() / words.size
        return ratio > 0.10 // >10% kata adalah keyword
    }

    // ---------------------------
    // Mixed alpha-numeric detection
    // ---------------------------
    private fun hasMixedAlphaNumeric(comment: String): Boolean {
        return comment.split("\\s+".toRegex()).any { token ->
            token.any(Char::isLetter) && token.any(Char::isDigit)
        }
    }

    // ---------------------------
    // Karakter abnormal (preserve your logic)
    // ---------------------------
    private fun containsAbnormalChars(text: String): Boolean {
        var abnormalCount = 0
        val abnormalCharsFound = mutableListOf<Char>()

        for (ch in text) {
            if (ch.isWhitespace()) continue
            if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9') continue
            if (ch in ". ,!?'\"@#$%^&*()_+-=[]{};:\\|<>`~/") continue

            val type = Character.getType(ch)
            val skipTypes: Set<Int> = setOf(
                Character.SURROGATE.toInt(),
                Character.FORMAT.toInt(),
                Character.OTHER_SYMBOL.toInt(),
                Character.MODIFIER_SYMBOL.toInt(),
                Character.MATH_SYMBOL.toInt(),
                Character.CURRENCY_SYMBOL.toInt(),
                Character.ENCLOSING_MARK.toInt(),
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt()
            )

            if (skipTypes.contains(type)) continue

            abnormalCount++
            abnormalCharsFound.add(ch)
        }

        android.util.Log.d(
            "SpamDetectorDebug",
            "Text: '$text' -> AbnormalCount=$abnormalCount, Chars=$abnormalCharsFound"
        )

        return abnormalCount > ABNORMAL_CHAR_THRESHOLD
    }

    // ---------------------------
    // Fungsi utama: weighted scoring
    // ---------------------------
    fun isSpam(comment: String, username: String): Boolean {
        var score = 0.0

        if (isKeywordSpam(comment)) score += 1.5
        if (containsPromotionPattern(comment)) score += 1.5
        if (hasSuspiciousStructure(comment)) score += 0.5
        if (hasMixedAlphaNumeric(comment)) score += 0.5
        if (containsAbnormalChars(comment)) score += 0.5
        if (containsStylizedEmojiLetters(comment)) score += 1.5
        if (containsStylizedUnicodeLetters(comment)) score += 1.5
        if (containsAlphaNumericEmojiCombo(comment)) score += 1.0

        android.util.Log.d("SpamScore", "Comment='$comment' -> Score=$score")

        return score >= 2.5
    }
}
