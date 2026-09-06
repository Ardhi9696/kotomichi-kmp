package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val jlptLevel: JlptLevel? = null,
    val orderIndex: Int = 0,
    val isPublished: Boolean = false,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val jftBasic: Boolean = false,
    val vocabularyCount: Int = 0,
    val masteryPercent: Double = 0.0
)

@Serializable
data class DeckVocabulary(
    val deckId: Long,
    val vocabularyId: Long,
    val orderInDeck: Int? = null
)