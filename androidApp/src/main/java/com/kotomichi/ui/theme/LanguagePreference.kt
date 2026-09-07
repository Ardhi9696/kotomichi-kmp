package com.kotomichi.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguagePreference {
    const val LANG_SYSTEM = "system"
    const val LANG_ID = "id"
    const val LANG_EN = "en"

    private const val PREFS_NAME = "kotomichi_prefs"
    private const val KEY_LANGUAGE = "language"

    private val _mode = MutableStateFlow(LANG_SYSTEM)
    val mode: StateFlow<String> = _mode.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _mode.value = prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setMode(context: Context, newMode: String) {
        _mode.value = newMode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, newMode)
            .apply()
    }
}