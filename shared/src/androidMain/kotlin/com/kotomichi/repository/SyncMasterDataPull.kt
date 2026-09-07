/**
 * File: SyncMasterDataPull.kt
 * Responsibility: Menarik master data dari server (vocabulary, decks, deck-vocabulary links, config).
 *                 Tidak melibatkan data user-specific.
 */
package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.Deck
import com.kotomichi.model.DirectionThresholds
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager untuk pull master data dari server.
 * @param database Database queries untuk menyimpan data
 * @param httpClient HTTP client untuk request ke server
 * @param baseUrl Base URL API server
 */
internal class SyncMasterDataPull(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    private val vocabQueries = database.vocabularyQueries
    private val deckQueries = database.deckQueries
    private val deckVocabQueries = database.deckVocabularyQueries
    private val thresholdQueries = database.directionThresholdQueries

    /**
     * Pull semua master data dari server.
     * @param since Timestamp watermark untuk pull incremental (null = full pull)
     * @return Hasil sinkronisasi
     */
    suspend fun pullAll(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        var totalSynced = 0
        var totalFailed = 0

        // Pull vocabulary
        val vocabResult = pullVocabulary(since)
        totalSynced += vocabResult.itemsSynced
        totalFailed += vocabResult.itemsFailed

        // Pull decks
        val deckResult = pullDecks(since)
        totalSynced += deckResult.itemsSynced
        totalFailed += deckResult.itemsFailed

        // Pull deck-vocabulary links (full refresh; table has no updated_at column)
        val linkResult = pullDeckLinks()
        totalSynced += linkResult.itemsSynced
        totalFailed += linkResult.itemsFailed

        // Pull config
        val configResult = pullConfig(since)
        totalSynced += configResult.itemsSynced
        totalFailed += configResult.itemsFailed

        SyncResult(
            success = totalFailed == 0,
            message = if (totalFailed == 0) "Master data tersinkron" else "Master data sebagian gagal",
            itemsSynced = totalSynced,
            itemsFailed = totalFailed
        )
    }

    private suspend fun pullVocabulary(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/vocabulary?select=*,vocabulary_translations(*)&updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/vocabulary?select=*,vocabulary_translations(*)"
        }
        val response = httpClient.get(url)

        if (response.status == HttpStatusCode.OK) {
            val vocabList = response.body<List<SupabaseVocabularyRow>>()
            vocabList.forEach { vocab ->
                vocabQueries.insert(vocab.toModel().toEntity())
                val id = vocab.id
                vocabQueries.deleteTranslationsByVocabularyId(id)
                vocab.vocabulary_translations.forEach { tr ->
                    vocabQueries.insertTranslation(
                        vocabulary_id = tr.vocabulary_id,
                        locale = tr.locale,
                        meaning = tr.meaning
                    )
                }
            }
            SyncResult(success = true, message = "Vocabulary synced", itemsSynced = vocabList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }

    private suspend fun pullDecks(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/decks?updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/decks"
        }
        val response = httpClient.get(url)

        if (response.status == HttpStatusCode.OK) {
            val deckList = response.body<List<SupabaseDeckRow>>()
            deckList.forEach { row ->
                deckQueries.insert(row.toModel().toEntity())
            }
            SyncResult(success = true, message = "Decks synced", itemsSynced = deckList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }

    private suspend fun pullDeckLinks(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/deck_vocabulary")

        if (response.status == HttpStatusCode.OK) {
            val links = response.body<List<SupabaseDeckVocabularyRow>>()
            deckVocabQueries.deleteAll()
            links.forEach { link ->
                deckVocabQueries.upsertLink(
                    deck_id = link.deck_id,
                    vocabulary_id = link.vocabulary_id,
                    order_in_deck = link.order_in_deck
                )
            }
            SyncResult(success = true, message = "Deck links synced", itemsSynced = links.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }

    private suspend fun pullConfig(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/direction_thresholds?updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/direction_thresholds"
        }
        val response = httpClient.get(url)

        if (response.status == HttpStatusCode.OK) {
            val thresholds = response.body<List<SupabaseDirectionThresholdRow>>()
            thresholds.forEach { threshold ->
                thresholdQueries.upsert(
                    direction = threshold.direction.toLong(),
                    fast_threshold_ms = threshold.fast_threshold_ms.toLong(),
                    good_threshold_ms = threshold.good_threshold_ms.toLong(),
                    updated_by = threshold.updated_by,
                    updated_at = threshold.updated_at?.let { parseSupabaseTimestamp(it) } ?: System.currentTimeMillis()
                )
            }
            SyncResult(success = true, message = "Config synced", itemsSynced = thresholds.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
}
