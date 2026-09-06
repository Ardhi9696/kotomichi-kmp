package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class Vocabulary(
    val id: Long,
    val kanji: String? = null,
    val hiragana: String,
    val romaji: String? = null,
    val jlptLevel: JlptLevel? = null,
    val partOfSpeech: String? = null,
    val isActive: Boolean = true,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val jftBasic: Boolean = false,
    val godanVerb: Boolean = false,
    val ichidanVerb: Boolean = false,
    val fukisoku: Boolean = false,
    val iAdjective: Boolean = false,
    val naAdjective: Boolean = false,
    val jidoushi: Boolean = false,
    val tadoushi: Boolean = false,
    val verbCollocation: Boolean = false,
    val translations: List<VocabularyTranslation> = emptyList(),
    val exampleSentences: List<ExampleSentence> = emptyList(),
    val collocations: List<VerbCollocation> = emptyList(),
    val audioAssets: List<AudioAsset> = emptyList()
) {
    val meaningIndonesian: String
        get() = translations.firstOrNull { it.locale == "id" || it.locale == "ind" }?.meaning
            ?: translations.firstOrNull { it.locale == "en" }?.meaning
            ?: ""

    val meaningEnglish: String?
        get() = translations.firstOrNull { it.locale == "en" }?.meaning
            ?: translations.firstOrNull { it.locale == "id" || it.locale == "ind" }?.meaning

    val displayText: String
        get() = kanji ?: hiragana
}

@Serializable
data class VocabularyTranslation(
    val vocabularyId: Long,
    val locale: String,
    val meaning: String
)

@Serializable
data class ExampleSentence(
    val id: Long,
    val vocabularyId: Long,
    val japanese: String,
    val createdAt: Long = System.currentTimeMillis(),
    val translations: List<ExampleSentenceTranslation> = emptyList()
) {
    val indonesian: String
        get() = translations.firstOrNull { it.locale == "id" || it.locale == "ind" }?.translation
            ?: ""
    val english: String?
        get() = translations.firstOrNull { it.locale == "en" }?.translation
}

@Serializable
data class ExampleSentenceTranslation(
    val exampleSentenceId: Long,
    val locale: String,
    val translation: String
)

@Serializable
data class VerbCollocation(
    val id: Long,
    val vocabularyId: Long,
    val collocation: String,
    val meaning: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AudioAsset(
    val id: Long,
    val vocabularyId: Long,
    val storageKey: String,
    val lang: String = "ja",
    val filename: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class JlptLevel {
    N5, N4, N3, N2, N1
}