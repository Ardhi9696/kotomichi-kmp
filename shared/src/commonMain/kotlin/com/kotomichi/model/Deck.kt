package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val id: Long,
    val title: String,
    val description: String? = null,
    val jlptLevel: JlptLevel,
    val orderIndex: Int,
    val isPublished: Boolean,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val vocabularyCount: Int = 0,
    val masteryPercent: Double = 0.0
)

@Serializable
data class DeckVocabulary(
    val deckId: Long,
    val vocabularyId: Long,
    val orderInDeck: Int
)