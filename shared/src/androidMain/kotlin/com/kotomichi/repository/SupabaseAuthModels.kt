package com.kotomichi.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class SupabaseSession(
    val access_token: String = "",
    val refresh_token: String = "",
    val expires_in: Long = 0,
    val token_type: String = "bearer",
    val user: SupabaseUser? = null
)

@Serializable
internal data class SupabaseUser(
    val id: String,
    val email: String? = null,
    val role: String? = null,
    val email_confirmed_at: String? = null,
    val created_at: String? = null,
    val user_metadata: JsonObject = JsonObject(emptyMap()),
    val app_metadata: JsonObject = JsonObject(emptyMap())
)