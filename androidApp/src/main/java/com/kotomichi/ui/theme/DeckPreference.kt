package com.kotomichi.ui.theme

import android.content.Context

object DeckPreference {
    private const val PREFS_NAME = "kotomichi_prefs"
    private const val KEY_SELECTED_DECK_ID = "selected_deck_id"

    fun read(context: Context): Long? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_SELECTED_DECK_ID, -1L)
            .takeIf { it > 0 }
    }

    fun write(context: Context, deckId: Long?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (deckId != null) putLong(KEY_SELECTED_DECK_ID, deckId) else remove(KEY_SELECTED_DECK_ID)
            }
            .apply()
    }
}