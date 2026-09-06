package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class Vocabulary(
    val id: Long,
    val kanji: String,
    val hiragana: String,
    val romaji: String,
    val meaningIndonesian: String,
    val meaningEnglish: String? = null,
    val partOfSpeech: String? = null,
    val jlptLevel: JlptLevel? = null,
    val frequencyRank: Int? = null,
    val audioUrlKanji: String? = null,
    val audioUrlHiragana: String? = null,
    val exampleSentences: List<ExampleSentence> = emptyList(),
    val collocations: List<Collocation> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ExampleSentence(
    val id: Long,
    val vocabularyId: Long,
    val japanese: String,
    val indonesian: String,
    val english: String? = null,
    val audioUrl: String? = null
)

@Serializable
data class Collocation(
    val id: Long,
    val vocabularyId: Long,
    val phrase: String,
    val meaning: String,
    val audioUrl: String? = null
)

enum class JlptLevel {
    N5, N4, N3, N2, N1
}