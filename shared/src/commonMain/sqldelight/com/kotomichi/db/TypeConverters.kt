package com.kotomichi.db

import com.kotomichi.model.Direction
import com.kotomichi.model.CardState
import com.kotomichi.model.Rating
import com.kotomichi.model.JlptLevel
import com.kotomichi.model.UserRole
import com.kotomichi.model.SyncStatus

object TypeConverters {
    fun directionToInt(direction: Direction): Int = direction.ordinal
    fun intToDirection(value: Int): Direction = Direction.values()[value]
    
    fun cardStateToInt(state: CardState): Int = state.ordinal
    fun intToCardState(value: Int): CardState = CardState.values()[value]
    
    fun ratingToInt(rating: Rating): Int = rating.ordinal
    fun intToRating(value: Int): Rating = Rating.values()[value]
    
    fun jlptLevelToString(level: JlptLevel): String = level.name
    fun stringToJlptLevel(value: String): JlptLevel = JlptLevel.valueOf(value)
    
    fun userRoleToString(role: UserRole): String = role.name
    fun stringToUserRole(value: String): UserRole = UserRole.valueOf(value)
    
    fun syncStatusToInt(status: SyncStatus): Int = status.value
    fun intToSyncStatus(value: Int): SyncStatus = SyncStatus.fromValue(value)
}