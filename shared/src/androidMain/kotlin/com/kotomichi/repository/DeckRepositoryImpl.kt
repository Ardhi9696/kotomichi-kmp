package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.Deck
import com.kotomichi.model.Deck as ModelDeck
import com.kotomichi.model.JlptLevel
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeckRepositoryImpl(
    private val database: KotomichiDatabase
) : DeckRepository {
    
    private val deckQueries = database.deckQueries
    
    override suspend fun getDeckById(id: Long): ModelDeck? = withContext(Dispatchers.IO) {
        deckQueries.selectById(id).executeAsOneOrNull()?.toModel()
    }
    
    override suspend fun getPublishedDecks(): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectPublished().executeAsList().map { it.toModel() }
    }
    
    override suspend fun getAllDecks(): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectAll().executeAsList().map { it.toModel() }
    }
    
    override suspend fun getDecksByJlptLevel(level: JlptLevel): List<ModelDeck> = withContext(Dispatchers.IO) {
        deckQueries.selectByJlptLevel(level.name).executeAsList().map { it.toModel() }
    }
    
    override suspend fun insertDeck(deck: ModelDeck): Long = withContext(Dispatchers.IO) {
        val entity = deck.toEntity()
        deckQueries.insert(entity)
        entity.id
    }
    
    override suspend fun updateDeck(deck: ModelDeck) = withContext(Dispatchers.IO) {
        deckQueries.insert(deck.toEntity())
    }
    
    override suspend fun deleteDeck(id: Long) = withContext(Dispatchers.IO) {
        deckQueries.deleteById(id)
    }
    
    override suspend fun publishDeck(id: Long, published: Boolean) = withContext(Dispatchers.IO) {
        deckQueries.updatePublished(if (published) 1L else 0L, id)
    }
    
    override fun observeDeck(deckId: Long): Flow<ModelDeck?> {
        return deckQueries.observeById(deckId).asFlow().map { it.executeAsOneOrNull()?.toModel() }
    }
    
    override fun observePublishedDecks(): Flow<List<ModelDeck>> {
        return deckQueries.observePublished().asFlow().map { it.executeAsList().map { m -> m.toModel() } }
    }
}

private fun Deck.toModel(): ModelDeck = ModelDeck(
    id = id,
    title = title,
    subtitle = subtitle,
    jlptLevel = jlpt_level?.let { JlptLevel.valueOf(it) },
    orderIndex = order_index.toInt(),
    isPublished = is_published == 1L,
    createdBy = created_by,
    createdAt = created_at,
    updatedAt = updated_at,
    jftBasic = jft_basic == 1L
)

internal fun ModelDeck.toEntity(): Deck = Deck(
    id = id,
    title = title,
    subtitle = subtitle,
    jlpt_level = jlptLevel?.name,
    order_index = orderIndex.toLong(),
    is_published = if (isPublished) 1L else 0L,
    created_by = createdBy,
    created_at = createdAt,
    updated_at = updatedAt,
    jft_basic = if (jftBasic) 1L else 0L
)