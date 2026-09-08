package com.kotomichi.furigana

import com.kotomichi.util.buildFuriganaFormat

/**
 * Menentukan teks ber-furigana dalam format Furiganable: `[kanji[reading]]`.
 *
 * Kaidah pemilihan ruby:
 *  - Kata tanpa kanji (semua hiragana/katakana) => tanpa furigana (`null`).
 *  - Mono ruby  (per karakter): dipakai kalau pembacaan tiap kanji diketahui dan
 *    bisa dipetakan 1:1 ke tiap kanji (jukugo biasa), mis. 日本語学校 => 日[に]本[ほん]語[ご]学[がっ]校[こう].
 *    Data ini terkurasi dan disimpan di kolom DB `Vocabulary.furigana`.
 *  - Group ruby (satu bacaan untuk seluruh blok kanji): dipakai untuk bacaan
 *    irregular/ateji yang tidak bisa dipecah per karakter (今日=きょう, 大人=おとな),
 *    atau untuk kata yang belum punya data kurasi => seluruh kanji digabung satu blok
 *    (dengan [buildFuriganaFormat] untuk mengecualikan okurigana yang sudah tertulis).
 */
object FuriganaFormatter {

    /**
     * @param kanji teks utama (kanji, bisa lengkap dengan okurigana, mis. 「慣れる」)
     * @param hiragana bacaan lengkap kata
     * @param curated string kurasi mono/group ruby dalam format `[kanji[reading]]`,
     *                 atau `null`/kosong bila belum ada
     * @return string format Furiganable, atau `null` bila tidak perlu furigana
     */
    fun formatted(kanji: String?, hiragana: String, curated: String?): String? {
        val base = kanji?.trim().takeIf { !it.isNullOrEmpty() } ?: return null
        if (!base.hasKanji()) return null
        val reading = hiragana.trim()
        if (reading.isEmpty()) return null

        curated?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        return buildFuriganaFormat(base, reading) ?: groupRuby(base, reading)
    }

    /** Bungkus seluruh teks kanji menjadi satu blok group ruby. */
    fun groupRuby(kanji: String, hiragana: String): String = "[$kanji[$hiragana]]"

    /** Cek keberadaan karakter kanji (CJK Unified + Ekstensi-A). */
    private fun String.hasKanji(): Boolean =
        any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
}