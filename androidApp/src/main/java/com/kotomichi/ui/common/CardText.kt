package com.kotomichi.ui.common

import com.kotomichi.furigana.FuriganaFormatter
import com.kotomichi.model.Direction
import com.kotomichi.model.Vocabulary

fun getQuestionText(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> card.kanji ?: card.hiragana
        Direction.KANJI_TO_HIRAGANA -> card.kanji ?: card.hiragana
        Direction.HIRAGANA_TO_MEANING -> card.hiragana
        Direction.MEANING_TO_HIRAGANA -> card.meaningIndonesian
        Direction.HIRAGANA_TO_KANJI -> card.hiragana
        Direction.MEANING_TO_KANJI -> card.meaningIndonesian
    }
}

fun getPronunciation(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_KANJI -> card.hiragana
        else -> ""
    }
}

fun getAnswerText(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> "${card.meaningIndonesian} (${card.hiragana})"
        Direction.KANJI_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_MEANING -> "${card.meaningIndonesian} (${card.kanji ?: card.hiragana})"
        Direction.MEANING_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_KANJI -> card.kanji ?: card.hiragana
        Direction.MEANING_TO_KANJI -> card.kanji ?: card.hiragana
    }
}

/** Jawaban polos tanpa hint kanji/hiragana dalam tanda kurung (untuk kuis Belajar). */
fun getPlainAnswerText(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING, Direction.HIRAGANA_TO_MEANING -> card.meaningIndonesian
        Direction.KANJI_TO_HIRAGANA, Direction.MEANING_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_KANJI, Direction.MEANING_TO_KANJI -> card.kanji ?: card.hiragana
    }
}

/**
 * Teks kakas (untuk FuriganaText) yang menampilkan kanji dengan furigana mono/group ruby,
 * atau `null` bila tampilan tidak berbentuk kanji (artinya tampil sebagai teks biasa).
 */
fun getQuestionFurigana(card: Vocabulary, direction: Direction): String? {
    return when (direction) {
        Direction.KANJI_TO_MEANING,
        Direction.KANJI_TO_HIRAGANA,
        Direction.HIRAGANA_TO_KANJI,
        Direction.MEANING_TO_KANJI -> furiganaFor(card)
        else -> null
    }
}

/** Teks kakas untuk jawaban yang menampilkan kanji (agar ikut diberi furigana). */
fun getAnswerFurigana(card: Vocabulary, direction: Direction): String? {
    return when (direction) {
        Direction.HIRAGANA_TO_KANJI,
        Direction.MEANING_TO_KANJI -> furiganaFor(card)
        else -> null
    }
}

private fun furiganaFor(card: Vocabulary): String? =
    FuriganaFormatter.formatted(card.kanji, card.hiragana, card.furigana)
