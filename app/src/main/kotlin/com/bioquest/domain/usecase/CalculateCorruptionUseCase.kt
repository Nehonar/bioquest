package com.bioquest.domain.usecase

import com.bioquest.domain.model.CorruptionLevel
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.DailyCorruption
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.UserGoal
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

/**
 * Turns a habit history into a corruption assessment over a moving window.
 *
 * Three mechanisms, exactly as specified in the product plan:
 *  1. Accumulation  — raw points summed across [windowDays]; drives the band.
 *  2. Chain damage  — 3+ consecutive days with any risk food.
 *  3. Positive-day mitigation — water/fruit/steps soften a day's damage but
 *     can never erase more than half of it ("sin borrar lo ocurrido").
 *
 * The band is derived from RAW points so the plan's worked examples hold:
 *   4 helados x2 = 8 -> NORMAL ; +15 croissants x3 => 53 -> CRITICAL.
 */
class CalculateCorruptionUseCase(
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    operator fun invoke(
        logs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule> = FoodImpactRule.defaultsById(),
        referenceDate: LocalDate = LocalDate.now(zone),
        goals: UserGoal = UserGoal.DEFAULT,
        stepsByDate: Map<LocalDate, Int> = emptyMap(),
        windowDays: Int = 30,
    ): CorruptionResult {
        val windowStart = referenceDate.minusDays((windowDays - 1).toLong())
        val inWindow = logs.filter {
            val d = it.dateAt(zone)
            !d.isBefore(windowStart) && !d.isAfter(referenceDate)
        }
        if (inWindow.none { it.type == com.bioquest.domain.model.HabitType.RISK_FOOD }) {
            return CorruptionResult.empty(goals)
        }

        val metricsByDate = DayMetricsAggregator.aggregate(inWindow, rules, stepsByDate, zone)

        val perDay = mutableListOf<DailyCorruption>()
        var rawTotal = 0
        var mitigatedTotal = 0

        // Walk every calendar day in the window so gaps break chains correctly.
        var cursor = windowStart
        while (!cursor.isAfter(referenceDate)) {
            val m = metricsByDate[cursor]
            val raw = m?.riskFoodPoints ?: 0
            val mitigation = if (raw > 0 && m != null) dailyMitigation(m, goals, raw) else 0
            perDay += DailyCorruption(
                date = cursor,
                rawPoints = raw,
                mitigatedPoints = mitigation,
                hasRiskFood = (m?.hasRiskFood == true),
            )
            rawTotal += raw
            mitigatedTotal += mitigation
            cursor = cursor.plusDays(1)
        }

        val chainLength = longestRiskRun(perDay)
        val chainDamage = chainLength >= 3
        val effectiveTotal = (rawTotal - mitigatedTotal).coerceAtLeast(0)
        val level = CorruptionLevel.fromPoints(rawTotal, goals)

        return CorruptionResult(
            rawPoints = rawTotal,
            mitigatedPoints = mitigatedTotal,
            effectivePoints = effectiveTotal,
            level = level,
            chainDamage = chainDamage,
            chainLength = chainLength,
            perDay = perDay.filter { it.hasRiskFood },
            explanation = buildExplanation(rawTotal, mitigatedTotal, level, chainDamage, chainLength),
        )
    }

    /**
     * Mitigation points earned by a good day, capped at half the day's raw
     * damage. Water reaching goal, one fruit, hitting step goal and a healthy
     * meal each chip away at the day's impact.
     */
    private fun dailyMitigation(m: DayMetrics, goals: UserGoal, raw: Int): Int {
        var mitigation = 0
        if (m.waterMl >= goals.waterMlGoal) mitigation += 2
        if (m.fruitCount >= 1) mitigation += 2
        if (goals.stepsGoal > 0 && m.steps >= goals.stepsGoal) mitigation += 2
        if (m.healthyMealCount >= 1) mitigation += 1
        val cap = floor(raw * 0.5).toInt()
        return mitigation.coerceAtMost(cap)
    }

    private fun longestRiskRun(perDay: List<DailyCorruption>): Int {
        var best = 0
        var current = 0
        for (day in perDay) {
            if (day.hasRiskFood) {
                current += 1
                best = maxOf(best, current)
            } else {
                current = 0
            }
        }
        return best
    }

    private fun buildExplanation(
        raw: Int,
        mitigated: Int,
        level: CorruptionLevel,
        chainDamage: Boolean,
        chainLength: Int,
    ): List<String> = buildList {
        add("Acumulacion 30 dias: $raw pts -> ${level.label}.")
        if (mitigated > 0) add("Contexto positivo redujo $mitigated pts del impacto efectivo.")
        if (chainDamage) {
            add("CHAIN DAMAGE: $chainLength dias seguidos con comida de riesgo.")
        } else {
            add("Sin cadena de riesgo activa.")
        }
    }
}
