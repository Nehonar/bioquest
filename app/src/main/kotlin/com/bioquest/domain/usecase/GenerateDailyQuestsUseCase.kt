package com.bioquest.domain.usecase

import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.Quest
import com.bioquest.domain.model.QuestCadence
import com.bioquest.domain.model.QuestType
import com.bioquest.domain.model.UserGoal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Builds 3–5 daily quests adapted to today's data and the last 7 days, plus a
 * weekly set. Quests are recomputed from source data, never stored as mutable
 * progress, so they stay honest with the log.
 */
class GenerateDailyQuestsUseCase(
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    operator fun invoke(
        referenceDate: LocalDate,
        logs: List<HabitLogEntry>,
        corruption: CorruptionResult,
        goals: UserGoal = UserGoal.DEFAULT,
        rules: Map<String, FoodImpactRule> = FoodImpactRule.defaultsById(),
        stepsToday: Int = 0,
    ): List<Quest> {
        val today = DayMetricsAggregator.forDay(referenceDate, logs, rules, stepsToday, zone)
        val last7 = last7Metrics(referenceDate, logs, rules)

        val daily = mutableListOf<Quest>()

        daily += Quest(
            type = QuestType.DRINK_WATER,
            title = QuestType.DRINK_WATER.defaultTitle,
            description = "Alcanza ${goals.waterMlGoal} ml de agua.",
            progress = today.waterMl,
            target = goals.waterMlGoal,
            xpReward = 20,
            cadence = QuestCadence.DAILY,
        )

        daily += Quest(
            type = QuestType.EAT_FRUIT,
            title = QuestType.EAT_FRUIT.defaultTitle,
            description = "Come ${goals.fruitGoal} piezas de fruta.",
            progress = today.fruitCount,
            target = goals.fruitGoal,
            xpReward = 15,
            cadence = QuestCadence.DAILY,
        )

        daily += Quest(
            type = QuestType.MOVEMENT,
            title = QuestType.MOVEMENT.defaultTitle,
            description = "Llega a ${goals.stepsGoal} pasos.",
            progress = today.steps,
            target = goals.stepsGoal,
            xpReward = 25,
            cadence = QuestCadence.DAILY,
        )

        daily += Quest(
            type = QuestType.MOOD_CHECK,
            title = QuestType.MOOD_CHECK.defaultTitle,
            description = "Registra tu estado de animo.",
            progress = if (today.moodValue != null) 1 else 0,
            target = 1,
            xpReward = 10,
            cadence = QuestCadence.DAILY,
        )

        // Adaptive quest: only surface Avoid Corruption Chain when a chain is
        // forming; otherwise offer a Recovery Protocol on low sleep/mood.
        if (corruption.chainDamage || corruption.level.ordinal >= 1) {
            daily += Quest(
                type = QuestType.AVOID_CORRUPTION_CHAIN,
                title = QuestType.AVOID_CORRUPTION_CHAIN.defaultTitle,
                description = "Pasa el dia sin comida de riesgo.",
                progress = if (today.hasRiskFood) 0 else 1,
                target = 1,
                xpReward = 30,
                cadence = QuestCadence.DAILY,
            )
        } else if (lowRecoverySignal(last7)) {
            daily += Quest(
                type = QuestType.RECOVERY_PROTOCOL,
                title = QuestType.RECOVERY_PROTOCOL.defaultTitle,
                description = "Registra una pausa o sueno reparador.",
                progress = if (today.restBreaks > 0 || (today.sleepHours ?: 0.0) >= goals.sleepHoursGoal) 1 else 0,
                target = 1,
                xpReward = 20,
                cadence = QuestCadence.DAILY,
            )
        }

        return (daily.take(5) + weeklyQuests(referenceDate, logs, corruption, goals, rules))
    }

    private fun weeklyQuests(
        referenceDate: LocalDate,
        logs: List<HabitLogEntry>,
        corruption: CorruptionResult,
        goals: UserGoal,
        rules: Map<String, FoodImpactRule>,
    ): List<Quest> {
        val week = last7Metrics(referenceDate, logs, rules)
        val workouts = week.count { it.exerciseMinutes >= 15 }
        val fruitDays = week.count { it.fruitCount >= 1 }
        val waterDays = week.count { it.waterMl >= goals.waterMlGoal }
        val stepDays = week.count { it.steps >= goals.stepsGoal }
        val weeklyCorruption = week.sumOf { it.riskFoodPoints }

        return listOf(
            Quest(QuestType.WEEKLY_WORKOUTS, QuestType.WEEKLY_WORKOUTS.defaultTitle,
                "3 entrenos esta semana.", workouts, 3, 60, QuestCadence.WEEKLY),
            Quest(QuestType.WEEKLY_FRUIT_DAYS, QuestType.WEEKLY_FRUIT_DAYS.defaultTitle,
                "5 dias con fruta.", fruitDays, 5, 50, QuestCadence.WEEKLY),
            Quest(QuestType.WEEKLY_WATER_DAYS, QuestType.WEEKLY_WATER_DAYS.defaultTitle,
                "4 dias cumpliendo agua.", waterDays, 4, 50, QuestCadence.WEEKLY),
            Quest(QuestType.WEEKLY_STEP_DAYS, QuestType.WEEKLY_STEP_DAYS.defaultTitle,
                "3 dias con ${goals.stepsGoal}+ pasos.", stepDays, 3, 50, QuestCadence.WEEKLY),
            // Progress = headroom under the weekly threshold (12 pts).
            Quest(QuestType.WEEKLY_LOW_CORRUPTION, QuestType.WEEKLY_LOW_CORRUPTION.defaultTitle,
                "Corrupcion semanal por debajo de 12 pts.",
                (12 - weeklyCorruption).coerceAtLeast(0), 12, 60, QuestCadence.WEEKLY),
        )
    }

    private fun last7Metrics(
        referenceDate: LocalDate,
        logs: List<HabitLogEntry>,
        rules: Map<String, FoodImpactRule>,
    ): List<DayMetrics> {
        val start = referenceDate.minusDays(6)
        val agg = DayMetricsAggregator.aggregate(
            logs.filter { val d = it.dateAt(zone); !d.isBefore(start) && !d.isAfter(referenceDate) },
            rules,
            zone = zone,
        )
        return (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            agg[date] ?: DayMetrics(date)
        }
    }

    private fun lowRecoverySignal(week: List<DayMetrics>): Boolean {
        val recent = week.takeLast(3)
        val lowSleep = recent.mapNotNull { it.sleepHours }.any { it < 6.0 }
        val lowMood = recent.mapNotNull { it.moodValue }.any { it <= 2 }
        return lowSleep || lowMood
    }
}
