package com.bioquest.domain.usecase

import com.bioquest.domain.model.CharacterStats
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.StatContribution
import com.bioquest.domain.model.StatType
import com.bioquest.domain.model.StatValue
import com.bioquest.domain.model.UserGoal
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Transparent stats engine. Every stat is a sum of labelled contributions,
 * clamped to 0..100, so the app can always explain "why is Vitality 72?".
 *
 * Design principles (reworked after playtesting feedback):
 *
 *  1. NATURAL BASELINE — a rested human who hasn't logged anything is not a
 *     corpse. Every positive stat starts from [NATURAL_BASE] ("Estado natural").
 *
 *  2. INERTIA — stats carry momentum from the previous 7 days ("Momentum
 *     7 dias", 0..[MOMENTUM_MAX]). You wake up roughly where you've been
 *     trending instead of resetting to zero at midnight. With no recent
 *     history the momentum is *neutral* ([MOMENTUM_NEUTRAL]), never punishing:
 *     a fresh install starts as a functional human.
 *
 *  3. MISSING DATA IS NEUTRAL, NOT BAD — un-logged sleep or mood contributes
 *     partial credit ("sin registro"), so forgetting to track something never
 *     reads as having done it badly.
 *
 *  4. TODAY STILL MATTERS — today's actions contribute up to ~50 points, so
 *     logging water/steps/food visibly moves the bars within the day.
 */
class CalculateStatsUseCase(
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    operator fun invoke(
        referenceDate: LocalDate,
        logs: List<HabitLogEntry>,
        corruption: CorruptionResult,
        goals: UserGoal = UserGoal.DEFAULT,
        rules: Map<String, FoodImpactRule> = FoodImpactRule.defaultsById(),
        stepsToday: Int = 0,
        stepsByDate: Map<LocalDate, Int> = emptyMap(),
    ): CharacterStats {
        val today = DayMetricsAggregator.forDay(referenceDate, logs, rules, stepsToday, zone)
        val consistencyDays = consistency(referenceDate, logs)
        val momentum = momentumByStat(referenceDate, logs, rules, goals, stepsByDate)

        val values = mapOf(
            StatType.VITALITY to vitality(today, goals, momentum),
            StatType.RECOVERY to recovery(today, goals, momentum),
            StatType.NUTRITION to nutrition(today, goals, corruption, momentum),
            StatType.STRENGTH to strength(today, goals, momentum),
            StatType.FOCUS to focus(today, consistencyDays, momentum),
            StatType.CORRUPTION to corruptionStat(corruption),
        )
        return CharacterStats(values)
    }

    // --- Per-stat builders ------------------------------------------------------

    private fun vitality(m: DayMetrics, g: UserGoal, momentum: Momentum): StatValue {
        val c = base(momentum, StatType.VITALITY)
        c += StatContribution("Hidratacion", ratio(m.waterMl, g.waterMlGoal, 20))
        c += StatContribution("Pasos", ratio(m.steps, g.stepsGoal, 15))
        c += StatContribution("Nutricion del dia", ratio(m.fruitCount + m.healthyMealCount, g.fruitGoal + 1, 15))
        return finalize(StatType.VITALITY, c)
    }

    private fun recovery(m: DayMetrics, g: UserGoal, momentum: Momentum): StatValue {
        val c = base(momentum, StatType.RECOVERY)
        c += if (m.sleepHours != null) {
            StatContribution("Sueno", ratioD(m.sleepHours, g.sleepHoursGoal, 25))
        } else {
            // Neutral, not punishing: un-logged sleep is unknown, not bad.
            StatContribution("Sueno (sin registro)", 12)
        }
        c += StatContribution("Pausas", ratio(m.restBreaks, 4, 10))
        c += moodContribution(m.moodValue, 15)
        return finalize(StatType.RECOVERY, c)
    }

    private fun nutrition(m: DayMetrics, g: UserGoal, corruption: CorruptionResult, momentum: Momentum): StatValue {
        val c = base(momentum, StatType.NUTRITION)
        c += StatContribution("Fruta", ratio(m.fruitCount, g.fruitGoal, 25))
        c += StatContribution("Comida saludable", ratio(m.healthyMealCount, 2, 25))
        // Corruption drags nutrition down by its effective (post-mitigation) load.
        val penalty = -min(60, corruption.effectivePoints)
        if (penalty != 0) c += StatContribution("Penalizacion corrupcion", penalty)
        return finalize(StatType.NUTRITION, c)
    }

    private fun strength(m: DayMetrics, g: UserGoal, momentum: Momentum): StatValue {
        val c = base(momentum, StatType.STRENGTH)
        c += StatContribution("Ejercicio", ratio(m.exerciseMinutes, g.exerciseMinutesGoal, 30))
        c += StatContribution("Pasos", ratio(m.steps, g.stepsGoal, 20))
        return finalize(StatType.STRENGTH, c)
    }

    private fun focus(m: DayMetrics, consistencyDays: Int, momentum: Momentum): StatValue {
        val c = base(momentum, StatType.FOCUS)
        c += moodContribution(m.moodValue, 15)
        c += StatContribution("Pausas", ratio(m.restBreaks, 4, 10))
        c += StatContribution("Consistencia 7 dias", ratio(consistencyDays, 7, 25))
        return finalize(StatType.FOCUS, c)
    }

    private fun corruptionStat(corruption: CorruptionResult): StatValue {
        // Higher value = worse. Scaled so that a critical event approaches 100.
        val value = min(100, corruption.rawPoints * 2)
        val c = listOf(
            StatContribution("Puntos ventana 30d", value),
        )
        return StatValue(StatType.CORRUPTION, value, c)
    }

    // --- Momentum (7-day inertia) -----------------------------------------------

    /** Momentum per stat, or neutral when there is no recent history at all. */
    private class Momentum(val byStat: Map<StatType, Int>?, val neutral: Int)

    private fun momentumByStat(
        referenceDate: LocalDate,
        logs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule>,
        goals: UserGoal,
        stepsByDate: Map<LocalDate, Int>,
    ): Momentum {
        val start = referenceDate.minusDays(7)
        val end = referenceDate.minusDays(1)
        val prevLogs = logs.filter {
            val d = it.dateAt(zone)
            !d.isBefore(start) && !d.isAfter(end)
        }
        if (prevLogs.isEmpty()) return Momentum(null, MOMENTUM_NEUTRAL)

        val metrics = DayMetricsAggregator.aggregate(prevLogs, rules, stepsByDate, zone)
        fun avg(fraction: (DayMetrics) -> Double): Int {
            var total = 0.0
            var day = start
            while (!day.isAfter(end)) {
                val m = metrics[day]
                if (m != null) total += fraction(m).coerceIn(0.0, 1.0)
                day = day.plusDays(1)
            }
            return ((total / 7.0) * MOMENTUM_MAX).roundToInt()
        }

        val byStat = mapOf(
            StatType.VITALITY to avg { m ->
                0.4 * frac(m.waterMl, goals.waterMlGoal) +
                    0.3 * frac(m.steps, goals.stepsGoal) +
                    0.3 * frac(m.fruitCount + m.healthyMealCount, goals.fruitGoal + 1)
            },
            StatType.RECOVERY to avg { m ->
                0.5 * (m.sleepHours?.let { fracD(it, goals.sleepHoursGoal) } ?: 0.5) +
                    0.2 * frac(m.restBreaks, 4) +
                    0.3 * moodFrac(m.moodValue)
            },
            StatType.NUTRITION to avg { m ->
                0.5 * frac(m.fruitCount, goals.fruitGoal) + 0.5 * frac(m.healthyMealCount, 2)
            },
            StatType.STRENGTH to avg { m ->
                0.6 * frac(m.exerciseMinutes, goals.exerciseMinutesGoal) + 0.4 * frac(m.steps, goals.stepsGoal)
            },
            StatType.FOCUS to avg { m ->
                0.5 * moodFrac(m.moodValue) + 0.2 * frac(m.restBreaks, 4) + 0.3 * (if (m.hasAnyLog) 1.0 else 0.0)
            },
        )
        return Momentum(byStat, MOMENTUM_NEUTRAL)
    }

    /** Shared "Estado natural" + momentum opening for every positive stat. */
    private fun base(momentum: Momentum, type: StatType): MutableList<StatContribution> {
        val c = mutableListOf(StatContribution("Estado natural", NATURAL_BASE))
        val byStat = momentum.byStat
        c += if (byStat == null) {
            StatContribution("Momentum 7 dias (sin historial)", momentum.neutral)
        } else {
            StatContribution("Momentum 7 dias", byStat[type] ?: 0)
        }
        return c
    }

    // --- Helpers ---------------------------------------------------------------

    private fun consistency(referenceDate: LocalDate, logs: List<HabitLogEntry>): Int {
        val start = referenceDate.minusDays(6)
        return logs.asSequence()
            .map { it.dateAt(zone) }
            .filter { !it.isBefore(start) && !it.isAfter(referenceDate) }
            .distinct()
            .count()
    }

    private fun finalize(type: StatType, contributions: List<StatContribution>): StatValue {
        val raw = contributions.sumOf { it.delta }
        return StatValue(type, raw.coerceIn(0, 100), contributions)
    }

    private fun moodContribution(mood: Int?, maxPoints: Int): StatContribution =
        if (mood != null) {
            StatContribution("Mood", (moodFrac(mood) * maxPoints).roundToInt())
        } else {
            StatContribution("Mood (sin registro)", maxPoints / 2)
        }

    private fun ratio(actual: Int, goal: Int, maxPoints: Int): Int {
        if (goal <= 0) return maxPoints
        return (min(1.0, actual.toDouble() / goal) * maxPoints).roundToInt()
    }

    private fun ratioD(actual: Double?, goal: Double, maxPoints: Int): Int {
        if (actual == null || goal <= 0.0) return 0
        return (min(1.0, actual / goal) * maxPoints).roundToInt()
    }

    private fun frac(actual: Int, goal: Int): Double =
        if (goal <= 0) 1.0 else min(1.0, actual.toDouble() / goal)

    private fun fracD(actual: Double, goal: Double): Double =
        if (goal <= 0.0) 1.0 else min(1.0, actual / goal)

    private fun moodFrac(mood: Int?): Double =
        if (mood == null) 0.5 else ((mood - 1).coerceIn(0, 4)) / 4.0

    companion object {
        /** A rested, untracked human: functional, not a corpse. */
        const val NATURAL_BASE = 35

        /** Max points the 7-day trend can add on top of the natural base. */
        const val MOMENTUM_MAX = 15

        /** Momentum used when there is no recent history (fresh start). */
        const val MOMENTUM_NEUTRAL = 7
    }
}
