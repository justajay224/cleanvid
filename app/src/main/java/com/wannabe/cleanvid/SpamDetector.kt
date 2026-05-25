package com.wannabe.cleanvid

import java.text.Normalizer

object SpamDetector {

    private val highRiskKeywords = setOf(
        "mona4d", "garudahoki", "maxwin", "lapakwd", "pulauwin", "banteng hoki", "spinharta", "sasafun", "ri188", "st789", "sloidr", "BBCA4D", "istanatempur",
        "e88vip", "okgame", "remi101n", "h5hiwin", "badak178", "dewi11", "hoki88", "slotbola", "rajaspin", "inibet", "tkp303", "berkah99", "nasagaming",
        "hariantoto", "pulau777", "kyt4d", "kytad", "pulauttt", "slotgacor", "toto", "kemang88", "garudahok", "himalaya4d", "himalayaad", "wg4d", "junior88",
    )

    // 2. TINGKATAN KATA KUNCI RISIKO MENENGAH (Bisa jadi bahasa gaul/normal) -> Skor 1.5
    private val mediumRiskKeywords = setOf(
        "slot", "gacor", "wd", "jp", "jepe", "jepi","togel", "deposit","hoki", "withdraw"
    )

    private const val ABNORMAL_CHAR_THRESHOLD = 5

    private fun applyHomoglyphTrstn(text: String): String {
        var t = text
        val replacements = mapOf(
            Regex("[аᴀａαάΑΆ]") to "a",
            Regex("[ʙｂВвΒβ]") to "b",
            Regex("[сᴄｃСсϲϹ]") to "c",
            Regex("[ᴅｄ]") to "d",
            Regex("[еᴇｅЕеΕέεέ]") to "e",
            Regex("[ꜰｆ]") to "f",
            Regex("[ɢｇԌԍ]") to "g",
            Regex("[нʜｈНнΗήηή]") to "h",
            Regex("[ɪіıｉІіΙίιί]") to "i",
            Regex("[ᴊｊЈј]") to "j",
            Regex("[кᴋｋКкΚκ]") to "k",
            Regex("[ʟｌ]") to "l",
            Regex("[мᴍｍМмΜμ]") to "m",
            Regex("[ɴｎΝν]") to "n",
            Regex("[оᴏｏОоΟόοό]") to "o",
            Regex("[рᴘｐРрΡρ]") to "p",
            Regex("[ǫｑ]") to "q",
            Regex("[ʀｒ]") to "r",
            Regex("[ꜱｓЅѕ]") to "s",
            Regex("[ᴛｔТтΤτ]") to "t",
            Regex("[ᴜｕ]") to "u",
            Regex("[ᴠｖѴѵν]") to "v",
            Regex("[ᴡｗԜԝ]") to "w",
            Regex("[хｘХхΧχ]") to "x",
            Regex("[уʏｙУуΥύυύ]") to "y",
            Regex("[ᴢｚΖζ]") to "z"
        )

        for ((regex, replacement) in replacements) {
            t = t.replace(regex, replacement)
        }
        return t
    }

    // ---------------------------
    // Normalisasi Super Agresif
    // ---------------------------
//    private fun normalizeSuperAggressively(text: String): String {
//        var t = text
//            .replace("🅰", "a").replace("🅐", "a").replace("ⓐ", "a")
//            .replace("🅱", "b").replace("🅑", "b").replace("ⓑ", "b")
//            .replace("🅲", "c").replace("🅒", "c").replace("ⓒ", "c")
//            .replace("🅳", "d").replace("🅓", "d").replace("ⓓ", "d")
//            .replace("🅴", "e").replace("🅔", "e").replace("ⓔ", "e")
//            .replace("🅵", "f").replace("🅕", "f").replace("ⓕ", "f")
//            .replace("🅶", "g").replace("🅖", "g").replace("ⓖ", "g")
//            .replace("🅷", "h").replace("🅗", "h").replace("ⓗ", "h")
//            .replace("🅸", "i").replace("🅘", "i").replace("ⓘ", "i").replace("ɪ", "i")
//            .replace("🅹", "j").replace("🅙", "j").replace("ⓙ", "j")
//            .replace("🅺", "k").replace("🅚", "k").replace("ⓚ", "k")
//            .replace("🅻", "l").replace("🅛", "l").replace("ⓛ", "l")
//            .replace("🅼", "m").replace("🅜", "m").replace("ⓜ", "m")
//            .replace("🅽", "n").replace("🅝", "n").replace("ⓝ", "n")
//            .replace("🅾", "o").replace("🅞", "o").replace("ⓞ", "o")
//            .replace("🅿", "p").replace("🅟", "p").replace("ⓟ", "p")
//            .replace("🆀", "q").replace("🅠", "q").replace("ⓠ", "q")
//            .replace("🆁", "r").replace("🅡", "r").replace("ⓡ", "r")
//            .replace("🆂", "s").replace("🅢", "s").replace("ⓢ", "s")
//            .replace("🆃", "t").replace("🅣", "t").replace("ⓣ", "t")
//            .replace("🆄", "u").replace("🅤", "u").replace("ⓤ", "u")
//            .replace("🆅", "v").replace("🅥", "v").replace("ⓥ", "v")
//            .replace("🆆", "w").replace("🅦", "w").replace("ⓦ", "w")
//            .replace("🆇", "x").replace("🅧", "x").replace("ⓧ", "x")
//            .replace("🆈", "y").replace("🅨", "y").replace("ⓨ", "y")
//            .replace("🆉", "z").replace("🅩", "z").replace("ⓩ", "z")
//
//        t = Normalizer.normalize(t, Normalizer.Form.NFKC)
//            .lowercase()
//
//        // simple leetspeak normalization
//        t = t.replace("1", "i")
//            .replace("0", "o")
//            .replace("4", "a")
//            .replace("3", "e")
//            .replace("5", "s")
//            .replace("7", "t")
//            .replace("8", "b")
//            .replace("6", "g")
//            .replace("9", "g")
//
//        // remove all non-letter chars (we keep only a-z)
//        return t.replace(Regex("[^a-z]"), "")
//    }

    private fun normalizeSuperAggressively(text: String): String {
        var t = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase()

        // 1. Terjemahkan huruf palsu (Small Caps / Cyrillic)
        t = applyHomoglyphTrstn(t)

        // 2. Terjemahkan Bubble Letters (seperti ⓐ atau 🅰)
        t = t.replace(Regex("[🅐ⓐ🅰]"), "a").replace(Regex("[🅑ⓑ🅱]"), "b").replace(Regex("[🅒ⓒ🅲]"), "c")
            .replace(Regex("[🅓ⓓ🅳]"), "d").replace(Regex("[🅔ⓔ🅴]"), "e").replace(Regex("[🅕ⓕ🅵]"), "f")
            .replace(Regex("[🅖ⓖ🅶]"), "g").replace(Regex("[🅗ⓗ🅷]"), "h").replace(Regex("[🅘ⓘ🅸]"), "i")
            .replace(Regex("[🅙ⓙ🅹]"), "j").replace(Regex("[🅚ⓚ🅺]"), "k").replace(Regex("[🅛ⓛ🅻]"), "l")
            .replace(Regex("[🅜ⓜ🅼]"), "m").replace(Regex("[🅝ⓝ🅽]"), "n").replace(Regex("[🅞ⓞ🅾]"), "o")
            .replace(Regex("[🅟ⓟ🅿]"), "p").replace(Regex("[🅠ⓠ🆀]"), "q").replace(Regex("[🅡ⓡ🆁]"), "r")
            .replace(Regex("[🅢ⓢ🆂]"), "s").replace(Regex("[🅣ⓣ🆃]"), "t").replace(Regex("[🅤ⓤ🆄]"), "u")
            .replace(Regex("[🅥ⓥ🆅]"), "v").replace(Regex("[🅦ⓦ🆆]"), "w").replace(Regex("[🅧ⓧ🆇]"), "x")
            .replace(Regex("[🅨ⓨ🆈]"), "y").replace(Regex("[🅩ⓩ🆉]"), "z")

        // 3. Terjemahkan Leetspeak (angka jadi huruf)
        t = t.replace("1", "i").replace("0", "o").replace("4", "a").replace("3", "e")
            .replace("5", "s").replace("7", "t").replace("8", "b").replace("6", "g")
            .replace("9", "g")

        // 4. Terakhir, HAPUS semua yang bukan huruf a-z murni
        return t.replace(Regex("[^a-z]"), "")
    }

    // ==========================================
    // FUNGSI 2: Mempertahankan Spasi
    // Digunakan KHUSUS untuk Medium-Risk Keywords
    // ==========================================
    private fun cleanTextKeepSpaces(text: String): String {
        // Terapkan leetspeak dasar dan normalisasi font
        var t = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase()
        t = t.replace("1", "i").replace("0", "o").replace("4", "a").replace("3", "e")
            .replace("5", "s").replace("7", "t").replace("8", "b").replace("6", "g").replace("9", "g")

        // Buang simbol, TAPI pertahankan a-z dan spasi (\\s)
        return t.replace(Regex("[^a-z\\s]"), "")
    }

    // Algoritma Levenshtein Distance
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
                    dp[i - 1][j] + 1, //jalur atas
                    dp[i][j - 1] + 1,  //jalur kiri
                    dp[i - 1][j - 1] + cost //jalur diagonal
                )
            }
        }
        return dp[a.length][b.length]
    }

    // Regex Pola Promosi Khusus
    private fun containsPromotionPattern(text: String): Boolean {
        val lower = text.lowercase()
        val patterns = listOf(
            Regex("\\b(daftar|join|main|coba|gasin|langsung|klik|ayo|buruan)\\b.*\\b(slot|gacor|maxwin|jp|toto)\\b"),
            Regex("\\bhttps?://\\S+\\b")
        )
        return patterns.any { it.containsMatchIn(lower) }
    }

    private fun containsStylizedUnicodeLetters(text: String): Boolean {
        val regex = Regex("[\\uD835\\uDC00-\\uD835\\uDFFF\\uFF21-\\uFF5A]+")
        return regex.findAll(text).sumOf { it.value.length } >= 3
    }

    private fun containsAlphaNumericEmojiCombo(text: String): Boolean {
        val keycapEmoji = Regex("[\\u0030-\\u0039]\\uFE0F?\\u20E3")
        return keycapEmoji.containsMatchIn(text)
    }

    // ---------------------------
    // FUNGSI UTAMA: PENILAIAN SKOR SPAM
    // ---------------------------
    fun isSpam(comment: String, username: String): Boolean {
        var score = 0.0

        // 1. Teks Hancur Lebur Tanpa Spasi (Pakai fungsi andalan Anda)
        val textNoSpaces = normalizeSuperAggressively(comment)

        // 2. CEK HIGH RISK (Cek di teks TANPA SPASI)
        for (keyword in highRiskKeywords) {
            val nk = normalizeSuperAggressively(keyword)

            // Exact match
            if (textNoSpaces.contains(nk)) {
                score += 2.5
                break
            }

            // Fuzzy match (Toleransi Typo) -> Hanya untuk kata >= 6 huruf
            if (nk.length >= 6 && textNoSpaces.length >= nk.length) {
                for (start in 0..(textNoSpaces.length - nk.length)) {
                    val sub = textNoSpaces.substring(start, start + nk.length)
                    if (levenshtein(sub, nk) <= 1) {
                        score += 2.5
                        break
                    }
                }
            }
        }

        if (score >= 2.5) return true // Langsung buang kalau High Risk

        // 3. CEK MEDIUM RISK (Cek di teks DENGAN SPASI)
        // Mencegah "soto" jadi "toto"
        val textWithSpaces = cleanTextKeepSpaces(comment)
        val words = textWithSpaces.split("\\s+".toRegex()).filter { it.isNotBlank() }

        for (word in words) {
            if (mediumRiskKeywords.contains(word)) {
                score += 1.5
                break // Cukup satu kata ditemukan untuk tambah poin 1.5
            }
        }

        // 4. CEK POLA LAINNYA
        if (containsPromotionPattern(comment)) score += 1.5
        if (containsStylizedUnicodeLetters(comment)) score += 1.5
        if (containsAlphaNumericEmojiCombo(comment)) score += 1.0

        android.util.Log.d("SpamScore", "Comment='$comment' -> Score=$score")

        return score >= 2.5
    }
}
