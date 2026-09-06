package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.Vocabulary
import com.kotomichi.db.Deck
import com.kotomichi.db.DeckVocabulary
import com.kotomichi.db.ExampleSentence
import com.kotomichi.db.Collocation
import com.kotomichi.model.Vocabulary as ModelVocabulary
import com.kotomichi.model.Deck as ModelDeck
import com.kotomichi.model.DeckVocabulary as ModelDeckVocabulary
import com.kotomichi.model.ExampleSentence as ModelExampleSentence
import com.kotomichi.model.Collocation as ModelCollocation
import com.kotomichi.model.JlptLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.HttpStatusCode
import android.content.Context

class VocabRepositoryImpl(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val context: Context
) : VocabRepository {
    
    private val vocabQueries = database.vocabularyQueries
    private val deckQueries = database.deckQueries
    private val deckVocabQueries = database.deckVocabularyQueries
    
    override suspend fun getVocabularyById(id: Long): ModelVocabulary? = withContext(Dispatchers.IO) {
        vocabQueries.selectById(id)?.toModel()
    }
    
    override suspend fun getVocabularyByIds(ids: List<Long>): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        vocabQueries.selectByIds(ids).map { it.toModel() }
    }
    
    override suspend fun getVocabIdsInDeck(deckId: Long): List<Long> = withContext(Dispatchers.IO) {
        deckVocabQueries.selectVocabIdsByDeck(deckId)
    }
    
    override suspend fun searchVocabulary(query: String, limit: Int): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        vocabQueries.searchByKanjiOrHiragana("%$query%", limit).map { it.toModel() }
    }
    
    override suspend fun getAllVocabulary(limit: Int, offset: Int): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        // TODO: Implement pagination
        vocabQueries.selectAll(limit, offset).map { it.toModel() }
    }
    
    override suspend fun insertVocabulary(vocab: ModelVocabulary): Long = withContext(Dispatchers.IO) {
        val entity = vocab.toEntity()
        vocabQueries.insert(entity)
        entity.id
    }
    
    override suspend fun updateVocabulary(vocab: ModelVocabulary) = withContext(Dispatchers.IO) {
        vocabQueries.update(vocab.toEntity())
    }
    
    override suspend fun deleteVocabulary(id: Long) = withContext(Dispatchers.IO) {
        vocabQueries.deleteById(id)
    }
    
    override suspend fun linkVocabToDeck(deckVocab: ModelDeckVocabulary) = withContext(Dispatchers.IO) {
        deckVocabQueries.insert(deckVocab.toEntity())
    }
    
    override suspend fun unlinkVocabFromDeck(deckId: Long, vocabId: Long) = withContext(Dispatchers.IO) {
        deckVocabQueries.delete(deckId, vocabId)
    }
    
    override fun observeVocabulary(vocabId: Long): Flow<ModelVocabulary?> {
        return vocabQueries.observeById(vocabId).map { it?.toModel() }
    }
    
    // Remote sync methods
    suspend fun pullVocabularyFromRemote(since: Long): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.get("$baseUrl/vocabulary?since=$since")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<ModelVocabulary>>()
        } else {
            emptyList()
        }
    }
    
    suspend fun pushVocabularyToRemote(vocab: ModelVocabulary): Boolean = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.post("$baseUrl/vocabulary") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(vocab)
        }
        response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
    }
}

private fun Vocabulary.toModel(): ModelVocabulary = ModelVocabulary(
    id = id,
    kanji = kanji,
    hiragana = hiragana,
    romaji = romaji,
    meaningIndonesian = meaning_indonesian,
    meaningEnglish = meaning_english,
    partOfSpeech = part_of_speech,
    jlptLevel = jlpt_level?.let { JlptLevel.valueOf(it) },
    frequencyRank = frequency_rank,
    audioUrlKanji = audio_url_kanji,
    audioUrlHiragana = audio_url_hiragana,
    createdAt = created_at,
    updatedAt = updated_at
)

private fun ModelVocabulary.toEntity(): Vocabulary = Vocabulary(
    id = id,
    kanji = kanji,
    hiragana = hiragana,
    romaji = romaji,
    meaning_indonesian = meaningIndonesian,
    meaning_english = meaningEnglish,
    part_of_speech = partOfSpeech,
    jlpt_level = jlptLevel?.name,
    frequency_rank = frequencyRank,
    audio_url_kanji = audioUrlKanji,
    audio_url_hiragana = audioUrlHiragana,
    created_at = createdAt,
    updated_at = updatedAt,
    last_synced = System.currentTimeMillis()
)

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

private fun DeckVocabulary.toModel(): ModelDeckVocabulary = ModelDeckVocabulary(
    deckId = deck_id,
    vocabularyId = vocabulary_id,
    orderInDeck = order_in_deck
)

private fun ModelDeckVocabulary.toEntity(): DeckVocabulary = DeckVocabulary(
    deck_id = deckId,
    vocabulary_id = vocabularyId,
    order_in_deck = orderInDeck
)