package com.kotomichi.furigana

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuriganaFormatterTest {

    @Test
    fun `mono ruby kurasi dipakai apa adanya untuk jukugo`() {
        val result = FuriganaFormatter.formatted(
            kanji = "日本語学校",
            hiragana = "にほんごがっこう",
            curated = "日[に]本[ほん]語[ご]学[がっ]校[こう]"
        )
        assertEquals("日[に]本[ほん]語[ご]学[がっ]校[こう]", result)
    }

    @Test
    fun `mono ruby dengan okurigana`() {
        val result = FuriganaFormatter.formatted(
            kanji = "慣れる",
            hiragana = "なれる",
            curated = "慣[な]れる"
        )
        assertEquals("慣[な]れる", result)
    }

    @Test
    fun `group ruby untuk kata irregular yang terkurasi (今日 = きょう)`() {
        val result = FuriganaFormatter.formatted(
            kanji = "今日",
            hiragana = "きょう",
            curated = "今日[きょう]"
        )
        assertEquals("今日[きょう]", result)
    }

    @Test
    fun `fallback group ruby bila belum ada data kurasi`() {
        val result = FuriganaFormatter.formatted(
            kanji = "大人",
            hiragana = "おとな",
            curated = null
        )
        assertEquals("大人[おとな]", result)
    }

    @Test
    fun `fallback mempertahankan okurigana di luar ruby (食べる)`() {
        val result = FuriganaFormatter.formatted(
            kanji = "食べる",
            hiragana = "たべる",
            curated = null
        )
        assertEquals("食[た]べる", result)
    }

    @Test
    fun `kata kana murni tidak perlu furigana`() {
        assertNull(FuriganaFormatter.formatted("おかげさまで", "おかげさまで", null))
        assertNull(FuriganaFormatter.formatted("ホテル", "ホテル", null))
    }

    @Test
    fun `kanji kosong tidak perlu furigana`() {
        assertNull(FuriganaFormatter.formatted(null, "まだ", null))
        assertNull(FuriganaFormatter.formatted("", "まだ", null))
    }

    @Test
    fun `kurasi kosong diperlakukan seperti null`() {
        val result = FuriganaFormatter.formatted("仕事", "しごと", "")
        assertEquals("仕事[しごと]", result)
    }
}