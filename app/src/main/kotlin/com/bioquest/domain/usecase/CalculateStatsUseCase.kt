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
 * Transparent stats engine. Every stat is a base plus a list of labelled
 * contributions, clamped to 0..100. Because contributions are kept, the app can
 * always explain "why is Vitality 72?" via [ExplainStatUseCase].
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
    ): CharacterStats {
        val today = DayMetricsAggregator.forDay(referenceDate, logs, rules, stepsToday, zone)
        val consistencyDays = consistency(referenceDate, logs)

        val values = mapOf(
            StatType.VITALITY to vitality(today, goals),
            StatType.RECOVERY to recovery(today, goals),
            StatType.NUTRITION to nutrition(today, goals, corruption),
            StatType.STRENGTH to strength(today, goals),
            StatType.FOCUS to focus(today, consistencyDays),
            StatType.CORRUPTION to corruptionStat(corruption),
        )
        return CharacterStats(values)
    }

    private fun vitality(m: DayMetrics, g: UserGoal): StatValue {
        val c = mutableListOf(StatContribution("Base", 10))
        c += StatContribution("Hidratacion", ratio(m.waterMl, g.waterMlGoal, 25))
        c += StatContribution("Pasos", ratio(m.steps, g.stepsGoal, 25))
        c += StatContribution("Nutricion del dia", ratio(m.fruitCount + m.healthyMealCount, g.fruitGoal + 1, 20))
        c += StatContribution("Sueno", ratioD(m.sleepHours, g.sleepHoursGoal, 20))
        return finalize(StatType.VITALITY, c)
    }

    private fun recovery(m: DayMetrics, g: UserGoal): StatValue {
        val c = mutableListOf(StatContribution("Base", 10))
        c += StatContribution("Sueno", ratioD(m.sleepHours, g.sleepHoursGoal, 40))
        c += StatContribution("Pausas", ratio(m.restBreaks, 4, 20))
        c += StatContribution("Mood", moodPoints(m.moodValue, 30))
        return finalize(StatType.RECOVERY, c)
    }

    private fun nutrition(m: DayMetrics, g: UserGoal, corruption: CorruptionResult): StatValue {
        val c = mutableListOf(StatContribution("Base", 40))
        c += StatContribution("Fruta", ratio(m.fruitCount, g.fruitGoal, 30))
        c += StatContribution("Comida saludable", ratio(m.healthyMealCount, 2, 30))
        // Corruption drags nutrition down by its effective (post-mitigation) load.
        val penalty = -min(60, corruption.effectivePoints)
        if (penalty != 0) c += StatContribution("Penalizacion corrupcion", penalty)
        return finalize(StatType.NUTRITION, c)
    }

    private fun strength(m: DayMetrics, g: UserGoal): StatValue {
        val c = mutableListOf(StatContribution("Base", 10))
        c += StatContribution("Ejercicio", ratio(m.exerciseMinutes, g.exerciseMinutesGoal, 50))
        c += StatContribution("Pasos", ratio(m.steps, g.stepsGoal, 40))
        return finalize(StatType.STRENGTH, c)
    }

    private fun focus(m: DayMetrics, consistencyDays: Int): StatValue {
        val c = mutableListOf(StatContribution("Base", 10))
        c += StatContribution("Mood", moodPoints(m.moodValue, 30))
        c += StatContribution("Pausas", ratio(m.restBreaks, 4, 20))
        c += StatContribution("Consistencia 7 dias", ratio(consistencyDays, 7, 40))
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

    private fun ratio(actual: Int, goal: Int, maxPoints: Int): Int {
        if (goal <= 0) return maxPoints
        return (min(1.0, actual.toDouble() / goal) * maxPoints).roundToInt()
    }

    private fun ratioD(actual: Double?, goal: Double, maxPoints: Int): Int {
        if (actual == null || goal <= 0.0) return 0
        return (min(1.0, actual / goal) * maxPoints).roundToInt()
    }

    private fun moodPoints(mood: Int?, maxPoints: Int): Int {
        if (mood == null) return 0
        // Mood 1..5 -> 0..maxPoints.
        return (((mood - 1).coerceIn(0, 4) / 4.0) * maxPoints).roundToInt()
    }
}
