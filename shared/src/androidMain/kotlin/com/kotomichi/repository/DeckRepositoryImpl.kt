package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.Deck
import com.kotomichi.model.Deck as ModelDeck
import com.kotomichi.model.JlptLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeckRepositoryImpl(
    private val database: KotomichiDatabase
) : DeckRepository {
    
    private val deckQueries = database.deckQueries
    
    override suspend fun getDeckById(id: Long): ModelDeck? = withContext(Dispatchers.IO) {
        deckQueries.selectById(id)?.toModel()
    }
    
    override suspend fun getPublishedDecks(): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectPublished().map { it.toModel() }
    }
    
    override suspend fun getAllDecks(): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectAll().map { it.toModel() }
    }
    
    override suspend fun getDecksByJlptLevel(level: JlptLevel): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectByJlptLevel(level.name).map { it.toModel() }
    }
    
    override suspend fun insertDeck(deck: ModelDeck): Long = withContext(Dispatchers.IO) {
        deckQueries.insert(deck.toEntity())
    }
    
    override suspend fun updateDeck(deck: ModelDeck) = withContext(Dispatchers.IO) {
        deckQueries.update(deck.toEntity())
    }
    
    override suspend fun deleteDeck(id: Long) = withContext(Dispatchers.IO) {
        deckQueries.deleteById(id)
    }
    
    override suspend fun publishDeck(id: Long, published: Boolean) = withContext(Dispatchers.IO) {
        deckQueries.updatePublished(id, if (published) 1 else 0)
    }
    
    override fun observeDeck(deckId: Long): Flow<ModelDeck?> {
        return deckQueries.observeById(deckId).map { it?.toModel() }
    }
    
    override fun observePublishedDecks(): Flow<List<ModelDeck>> {
        return deckQueries.observePublished().map { it.map { it.toModel() } }
    }
}

private fun Deck.toModel(): ModelDeck = ModelDeck(
    id = id,
    title = title,
    description = description,
    jlptLevel = JlptLevel.valueOf(jlpt_level),
    orderIndex = order_index,
    isPublished = is_published == 1,
    createdBy = created_by,
    createdAt = created_at,
    updatedAt = updated_at,
    vocabularyCount = vocabulary_count,
    masteryPercent = mastery_percent
)

private fun ModelDeck.toEntity(): Deck = Deck(
    id = id,
    title = title,
    description = description,
    jlpt_level = jlptLevel.name,
    order_index = orderIndex,
    is_published = if (isPublished) 1 else 0,
    created_by = createdBy,
    created_at = createdAt,
    updated_at = updatedAt,
    vocabulary_count = vocabularyCount,
    mastery_percent = masteryPercent
)