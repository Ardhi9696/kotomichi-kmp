package com.kotomichi.repository

import com.kotomichi.model.Vocabulary
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckVocabulary
import kotlinx.coroutines.flow.Flow

interface VocabRepository {
    suspend fun getVocabularyById(id: Long): Vocabulary?
    suspend fun getVocabularyByIds(ids: List<Long>): List<Vocabulary>
    suspend fun getVocabIdsInDeck(deckId: Long): List<Long>
    suspend fun searchVocabulary(query: String, limit: Int): List<Vocabulary>
    suspend fun getAllVocabulary(limit: Int, offset: Int): List<Vocabulary>
    suspend fun insertVocabulary(vocab: Vocabulary): Long
    suspend fun updateVocabulary(vocab: Vocabulary)
    suspend fun deleteVocabulary(id: Long)
    suspend fun linkVocabToDeck(deckVocab: DeckVocabulary)
    suspend fun unlinkVocabFromDeck(deckId: Long, vocabId: Long)
    fun observeVocabulary(vocabId: Long): Flow<Vocabulary?>
    suspend fun getVocabularyByDeck(deckId: Long): List<Vocabulary>
}

interface DeckRepository {
    suspend fun getDeckById(id: Long): Deck?
    suspend fun getPublishedDecks(): List<Deck>
    suspend fun getAllDecks(): List<Deck>
    suspend fun getVocabularyCount(): Int
    suspend fun getDecksByJlptLevel(level: com.kotomichi.model.JlptLevel): List<Deck>
    suspend fun insertDeck(deck: Deck): Long
    suspend fun updateDeck(deck: Deck)
    suspend fun deleteDeck(id: Long)
    suspend fun publishDeck(id: Long, published: Boolean)
    fun observeDeck(deckId: Long): Flow<Deck?>
    fun observePublishedDecks(): Flow<List<Deck>>
}