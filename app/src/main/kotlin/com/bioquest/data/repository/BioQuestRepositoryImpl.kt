package com.bioquest.data.repository

import com.bioquest.data.local.BioQuestDatabase
import com.bioquest.data.local.entity.StepCountEntity
import com.bioquest.data.local.toDomain
import com.bioquest.data.local.toEntity
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.UserGoal
import com.bioquest.domain.repository.BioQuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class BioQuestRepositoryImpl(
    private val db: BioQuestDatabase,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : BioQuestRepository {

    private val habitDao = db.habitLogDao()
    private val ruleDao = db.foodRuleDao()
    private val goalDao = db.userGoalDao()
    private val stepDao = db.stepCountDao()

    override fun observeLogs(): Flow<List<HabitLogEntry>> =
        habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun logsBetween(start: LocalDate, end: LocalDate): List<HabitLogEntry> {
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return habitDao.between(startMillis, endMillis).map { it.toDomain() }
    }

    override suspend fun recentLogs(days: Int): List<HabitLogEntry> {
        val end = LocalDate.now(zone)
        return logsBetween(end.minusDays((days - 1).toLong()), end)
    }

    override suspend fun addLog(entry: HabitLogEntry): Long = habitDao.insert(entry.toEntity())

    override suspend fun deleteLog(id: Long) = habitDao.delete(id)

    override fun observeGoals(): Flow<UserGoal> =
        goalDao.observe().map { it?.toDomain() ?: UserGoal.DEFAULT }

    override suspend fun currentGoals(): UserGoal = goalDao.get()?.toDomain() ?: UserGoal.DEFAULT

    override suspend fun updateGoals(goals: UserGoal) = goalDao.upsert(goals.toEntity())

    override suspend fun foodRules(): List<FoodImpactRule> {
        val stored = ruleDao.all()
        return if (stored.isEmpty()) FoodImpactRule.DEFAULTS else stored.map { it.toDomain() }
    }

    override fun observeFoodRules(): Flow<List<FoodImpactRule>> =
        ruleDao.observeAll().map { list ->
            if (list.isEmpty()) FoodImpactRule.DEFAULTS else list.map { it.toDomain() }
        }

    override suspend fun upsertFoodRule(rule: FoodImpactRule) = ruleDao.upsert(rule.toEntity())

    override suspend fun stepsByDate(start: LocalDate, end: LocalDate): Map<LocalDate, Int> =
        stepDao.between(start.toEpochDay(), end.toEpochDay())
            .associate { LocalDate.ofEpochDay(it.epochDay) to it.steps }

    override suspend fun setSteps(date: LocalDate, steps: Int) =
        stepDao.upsert(StepCountEntity(date.toEpochDay(), steps))
}
