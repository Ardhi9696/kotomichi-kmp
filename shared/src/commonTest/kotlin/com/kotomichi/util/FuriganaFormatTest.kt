package com.kotomichi.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuriganaFormatTest {

    @Test
    fun `full kanji renders whole word furigana`() {
        assertEquals("[学校[がっこう]]", buildFuriganaFormat("学校", "がっこう"))
        assertEquals("[日本語[にほんご]]", buildFuriganaFormat("日本語", "にほんご"))
    }

    @Test
    fun `kanji with trailing okurigana puts furigana only on kanji`() {
        assertEquals("[走[はし]]る", buildFuriganaFormat("走る", "はしる"))
        assertEquals("[聞[き]]く", buildFuriganaFormat("聞く", "きく"))
        assertEquals("[話[はな]]す", buildFuriganaFormat("話す", "はなす"))
    }

    @Test
    fun `multi kana okurigana`() {
        assertEquals("[見[み]]える", buildFuriganaFormat("見える", "みえる"))
        assertEquals("[大[おお]]きい", buildFuriganaFormat("大きい", "おおきい"))
        assertEquals("[上[あ]]がる", buildFuriganaFormat("上がる", "あがる"))
    }

    @Test
    fun `kanji separated by middle kana`() {
        assertEquals("[買[か]]い[物[もの]]", buildFuriganaFormat("買い物", "かいもの"))
    }

    @Test
    fun `leading kana before kanji`() {
        assertEquals("お[茶[ちゃ]]", buildFuriganaFormat("お茶", "おちゃ"))
    }

    @Test
    fun `single kanji with segment reading`() {
        assertEquals("[上[かみ]]", buildFuriganaFormat("上", "かみ"))
    }

    @Test
    fun `no kanji returns null`() {
        assertNull(buildFuriganaFormat("ありがとう", "ありがとう"))
        assertNull(buildFuriganaFormat("カタカナ", "カタカナ"))
    }

    @Test
    fun `empty inputs return null`() {
        assertNull(buildFuriganaFormat("", ""))
        assertNull(buildFuriganaFormat("食べる", ""))
    }

    @Test
    fun `mismatched reading returns null`() {
        assertNull(buildFuriganaFormat("食べる", "たべた"))
    }
}