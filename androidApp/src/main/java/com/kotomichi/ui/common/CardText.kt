package com.kotomichi.ui.common

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