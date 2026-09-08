/**
 * File: FuriganaFormat.kt
 * Responsibility: Menyusun string berformat Furiganable ([teks[reading]])
 *                 sehingga furigana hanya dirender di atas karakter kanji,
 *                 bukan di atas hiragana yang sudah dituliskan (okurigana).
 */
package com.kotomichi.util

private const val KANJI_START = 0x4E00
private const val KANJI_END = 0x9FFF

/**
 * Deteksi apakah string mengandung karakter kanji (CJK Unified Ideographs
 * U+4E00–U+9FFF), termasuk penanda iterasi 々 dan 〇.
 */
fun containsKanji(text: String): Boolean = text.any { isKanji(it) }

private fun isKanji(c: Char): Boolean {
    val code = c.code
    return code in KANJI_START..KANJI_END || c == '\u3005' || c == '\u3007'
}

/**
 * Menyusun format furigana untuk library Furiganable.
 *
 * Contoh: word = "食べる", reading = "たべる" menghasilkan
 * `[食[た]べる]` — furigana hanya di atas kanji, okurigana dibiarkan polos.
 *
 * @param word Teks tampilan (bisa campuran kanji + hiragana/katakana)
 * @param reading Seluruh cara baca (hiragana) dari kata
 * @return String berformat Furiganable, atau null bila tidak valid sehingga
 *         pemanggil harus menggunakan fallback.
 */
fun buildFuriganaFormat(word: String, reading: String): String? {
    if (reading.isEmpty() || word.none { isKanji(it) }) return null

    // Pecah kata menjadi segmen kanji dan kana yang berselang-seling.
    val segments = mutableListOf<String>()
    val isKana = mutableListOf<Boolean>()
    var i = 0
    while (i < word.length) {
        val kanji = isKanji(word[i])
        var j = i
        while (j < word.length && isKanji(word[j]) == kanji) j++
        segments.add(word.substring(i, j))
        isKana.add(!kanji)
        i = j
    }

    val out = StringBuilder()
    var ri = 0
    var pendingKanji = StringBuilder()
    var kanjiStart = -1

    fun flushKanji(endExclusive: Int): Boolean {
        if (pendingKanji.isEmpty()) return true
        if (endExclusive <= kanjiStart || endExclusive > reading.length) return false
        val slice = reading.substring(kanjiStart, endExclusive)
        if (slice.isEmpty()) return false
        out.append("[${pendingKanji}[$slice]]")
        pendingKanji = StringBuilder()
        kanjiStart = -1
        return true
    }

    for (idx in segments.indices) {
        val seg = segments[idx]
        if (!isKana[idx]) {
            if (pendingKanji.isEmpty()) kanjiStart = ri
            pendingKanji.append(seg)
            continue
        }
        // Segmen kana harus muncul persis di dalam reading mulai posisi ri.
        val found = reading.indexOf(seg, ri)
        if (found < 0) return null
        if (!flushKanji(found)) return null
        out.append(seg)
        ri = found + seg.length
    }

    // Kanji di akhir kata mengambil sisa reading.
    if (pendingKanji.isNotEmpty()) {
        if (kanjiStart < 0 || kanjiStart >= reading.length) return null
        val slice = reading.substring(kanjiStart)
        if (slice.isEmpty()) return null
        out.append("[${pendingKanji}[$slice]]")
        ri = reading.length
    }

    if (ri != reading.length) return null
    return out.toString()
}