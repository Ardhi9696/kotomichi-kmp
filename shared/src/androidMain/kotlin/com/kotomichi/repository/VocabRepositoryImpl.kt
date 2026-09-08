package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.Vocabulary
import com.kotomichi.db.DeckVocabulary
import com.kotomichi.db.VocabularyTranslation
import com.kotomichi.model.Vocabulary as ModelVocabulary
import com.kotomichi.model.DeckVocabulary as ModelDeckVocabulary
import com.kotomichi.model.VocabularyTranslation as ModelVocabularyTranslation
import com.kotomichi.model.JlptLevel
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
import io.ktor.http.contentType
import io.ktor.client.request.setBody
import io.ktor.client.call.body
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
    
    private fun Vocabulary.toModel(translations: List<ModelVocabularyTranslation> = emptyList()): ModelVocabulary =
        ModelVocabulary(
            id = id,
            kanji = kanji,
            hiragana = hiragana,
            furigana = furigana,
            romaji = romaji,
            jlptLevel = jlpt_level?.let { JlptLevel.valueOf(it) },
            partOfSpeech = part_of_speech,
            isActive = is_active == 1L,
            createdBy = created_by,
            createdAt = created_at,
            updatedAt = updated_at,
            jftBasic = jft_basic == 1L,
            godanVerb = godan_verb == 1L,
            ichidanVerb = ichidan_verb == 1L,
            fukisoku = fukisoku == 1L,
            iAdjective = i_adjective == 1L,
            naAdjective = na_adjective == 1L,
            jidoushi = jidoushi == 1L,
            tadoushi = tadoushi == 1L,
            verbCollocation = verb_collocation == 1L,
            translations = translations
        )

    private fun List<Vocabulary>.withTranslations(): List<ModelVocabulary> {
        if (isEmpty()) return emptyList()
        val byVocab = vocabQueries.selectTranslations(map { it.id })
            .executeAsList()
            .groupBy { it.vocabulary_id }
        return map { row -> row.toModel(byVocab[row.id].orEmpty().map { it.toModel() }) }
    }

    private fun VocabularyTranslation.toModel(): ModelVocabularyTranslation =
        ModelVocabularyTranslation(
            vocabularyId = vocabulary_id,
            locale = locale,
            meaning = meaning
        )
    
    override suspend fun getVocabularyById(id: Long): ModelVocabulary? = withContext(Dispatchers.IO) {
        vocabQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            row.toModel(vocabQueries.selectTranslationsByVocabularyId(id)
                .executeAsList()
                .map { it.toModel() })
        }
    }
    
    override suspend fun getVocabularyByIds(ids: List<Long>): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        vocabQueries.selectByIds(ids).executeAsList().withTranslations()
    }
    
    override suspend fun getVocabIdsInDeck(deckId: Long): List<Long> = withContext(Dispatchers.IO) {
        deckVocabQueries.selectVocabIdsByDeck(deckId).executeAsList()
    }
    
    override suspend fun searchVocabulary(query: String, limit: Int): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        vocabQueries.searchByKanjiOrHiragana("%$query%", limit.toLong()).executeAsList().withTranslations()
    }
    
    override suspend fun getAllVocabulary(limit: Int, offset: Int): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        // TODO: Implement pagination
        vocabQueries.selectAll(limit.toLong(), offset.toLong()).executeAsList().withTranslations()
    }
    
    override suspend fun insertVocabulary(vocab: ModelVocabulary): Long = withContext(Dispatchers.IO) {
        val entity = vocab.toEntity()
        vocabQueries.insert(entity)
        entity.id
    }
    
    override suspend fun updateVocabulary(vocab: ModelVocabulary) = withContext(Dispatchers.IO) {
        vocabQueries.insert(vocab.toEntity())
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
        return vocabQueries.observeById(vocabId).asFlow().map {
            it.executeAsOneOrNull()?.let { row ->
                row.toModel(vocabQueries.selectTranslationsByVocabularyId(vocabId)
                    .executeAsList()
                    .map { t -> t.toModel() })
            }
        }
    }
    
    // Remote sync methods
    suspend fun pullVocabularyFromRemote(since: Long): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.get("$baseUrl/vocabulary?since=$since")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseVocabularyRow>>().map { it.toModel() }
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

    override suspend fun getVocabularyByDeck(deckId: Long): List<ModelVocabulary> = withContext(Dispatchers.IO) {
        val ids = deckVocabQueries.selectVocabIdsByDeck(deckId).executeAsList()
        if (ids.isEmpty()) return@withContext emptyList()
        vocabQueries.selectByIds(ids).executeAsList().withTranslations()
    }
}

internal fun ModelVocabulary.toEntity(): Vocabulary = Vocabulary(
    id = id,
    kanji = kanji,
    hiragana = hiragana,
    furigana = furigana,
    romaji = romaji,
    jlpt_level = jlptLevel?.name,
    part_of_speech = partOfSpeech,
    is_active = if (isActive) 1L else 0L,
    created_by = createdBy,
    created_at = createdAt,
    updated_at = updatedAt,
    jft_basic = if (jftBasic) 1L else 0L,
    godan_verb = if (godanVerb) 1L else 0L,
    ichidan_verb = if (ichidanVerb) 1L else 0L,
    fukisoku = if (fukisoku) 1L else 0L,
    i_adjective = if (iAdjective) 1L else 0L,
    na_adjective = if (naAdjective) 1L else 0L,
    jidoushi = if (jidoushi) 1L else 0L,
    tadoushi = if (tadoushi) 1L else 0L,
    verb_collocation = if (verbCollocation) 1L else 0L
)

private fun DeckVocabulary.toModel(): ModelDeckVocabulary = ModelDeckVocabulary(
    deckId = deck_id,
    vocabularyId = vocabulary_id,
    orderInDeck = order_in_deck?.toInt()
)

private fun ModelDeckVocabulary.toEntity(): DeckVocabulary = DeckVocabulary(
    deck_id = deckId,
    vocabulary_id = vocabularyId,
    order_in_deck = orderInDeck?.toLong()
)