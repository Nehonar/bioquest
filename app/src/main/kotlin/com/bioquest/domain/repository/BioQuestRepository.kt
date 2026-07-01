package com.bioquest.domain.repository

import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.UserGoal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain port. The data layer (Room + DataStore) implements it; use cases and
 * UI depend only on this interface, keeping the domain Android-free.
 */
interface BioQuestRepository {
    fun observeLogs(): Flow<List<HabitLogEntry>>

    suspend fun logsBetween(start: LocalDate, end: LocalDate): List<HabitLogEntry>

    suspend fun recentLogs(days: Int): List<HabitLogEntry>

    suspend fun addLog(entry: HabitLogEntry): Long

    suspend fun deleteLog(id: Long)

    fun observeGoals(): Flow<UserGoal>

    suspend fun currentGoals(): UserGoal

    suspend fun updateGoals(goals: UserGoal)

    suspend fun foodRules(): List<FoodImpactRule>

    fun observeFoodRules(): Flow<List<FoodImpactRule>>

    suspend fun upsertFoodRule(rule: FoodImpactRule)

    /** Cached step totals per date (Health Connect / SensorManager). */
    suspend fun stepsByDate(start: LocalDate, end: LocalDate): Map<LocalDate, Int>

    suspend fun setSteps(date: LocalDate, steps: Int)
}
