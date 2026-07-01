package com.bioquest.domain.usecase

import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Per-day aggregate derived from raw [HabitLogEntry]s. Keeping this as a pure
 * reduction lets both the stats and corruption engines share one source of
 * truth and stay fully unit-testable.
 */
data class DayMetrics(
    val date: LocalDate,
    val waterMl: Int = 0,
    val fruitCount: Int = 0,
    val healthyMealCount: Int = 0,
    val riskFoodPoints: Int = 0,
    val riskFoodCount: Int = 0,
    val nutritionXp: Int = 0,
    val vitalityXp: Int = 0,
    val exerciseMinutes: Int = 0,
    val restBreaks: Int = 0,
    val steps: Int = 0,
    val moodValue: Int? = null,
    val sleepHours: Double? = null,
    val weightKg: Double? = null,
) {
    val hasRiskFood: Boolean get() = riskFoodCount > 0
    val hasAnyLog: Boolean
        get() = waterMl > 0 || fruitCount > 0 || healthyMealCount > 0 || riskFoodCount > 0 ||
            exerciseMinutes > 0 || restBreaks > 0 || moodValue != null || sleepHours != null ||
            weightKg != null
}

object DayMetricsAggregator {

    /**
     * Collapse logs into one [DayMetrics] per calendar date. [stepsByDate]
     * carries automatic step counts from Health Connect / SensorManager.
     */
    fun aggregate(
        logs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule>,
        stepsByDate: Map<LocalDate, Int> = emptyMap(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, DayMetrics> {
        val byDate = logs.groupBy { it.dateAt(zone) }
        val dates = (byDate.keys + stepsByDate.keys).toSet()
        return dates.associateWith { date ->
            reduceDay(date, byDate[date].orEmpty(), rules, stepsByDate[date] ?: 0)
        }
    }

    fun forDay(
        date: LocalDate,
        logs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule>,
        steps: Int = 0,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DayMetrics = reduceDay(date, logs.filter { it.dateAt(zone) == date }, rules, steps)

    private fun reduceDay(
        date: LocalDate,
        dayLogs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule>,
        steps: Int,
    ): DayMetrics {
        var water = 0
        var fruit = 0
        var healthy = 0
        var riskPoints = 0
        var riskCount = 0
        var nutritionXp = 0
        var vitalityXp = 0
        var exercise = 0
        var breaks = 0
        var mood: Int? = null
        var sleep: Double? = null
        var weight: Double? = null

        for (log in dayLogs) {
            val qty = log.quantity
            when (log.type) {
                HabitType.WATER -> {
                    water += qty.toInt()
                    vitalityXp += 1
                }
                HabitType.FRUIT -> {
                    fruit += qty.toInt()
                    nutritionXp += 2 * qty.toInt()
                }
                HabitType.HEALTHY_MEAL -> {
                    healthy += qty.toInt()
                    nutritionXp += 3 * qty.toInt()
                }
                HabitType.RISK_FOOD -> {
                    val rule = log.foodRuleId?.let { rules[it] }
                    val points = (rule?.corruptionPoints ?: 2) * qty.toInt()
                    riskPoints += points
                    riskCount += qty.toInt()
                    nutritionXp += (rule?.nutritionXp ?: 0) * qty.toInt()
                }
                HabitType.EXERCISE -> exercise += qty.toInt()
                HabitType.REST_BREAK -> breaks += qty.toInt()
                HabitType.MOOD -> mood = (log.moodValue ?: qty.toInt()).coerceIn(1, 5)
                HabitType.SLEEP_MANUAL -> sleep = log.sleepHours ?: qty
                HabitType.WEIGHT -> weight = log.weightKg ?: qty
            }
        }

        return DayMetrics(
            date = date,
            waterMl = water,
            fruitCount = fruit,
            healthyMealCount = healthy,
            riskFoodPoints = riskPoints,
            riskFoodCount = riskCount,
            nutritionXp = nutritionXp,
            vitalityXp = vitalityXp,
            exerciseMinutes = exercise,
            restBreaks = breaks,
            steps = steps,
            moodValue = mood,
            sleepHours = sleep,
            weightKg = weight,
        )
    }
}
