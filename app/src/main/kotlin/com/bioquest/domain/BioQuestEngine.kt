package com.bioquest.domain

import com.bioquest.domain.model.CharacterClass
import com.bioquest.domain.repository.BioQuestRepository
import com.bioquest.domain.usecase.BuildWidgetStateUseCase
import com.bioquest.domain.usecase.CalculateCorruptionUseCase
import com.bioquest.domain.usecase.CalculateStatsUseCase
import com.bioquest.domain.usecase.DetectHealthEventsUseCase
import com.bioquest.domain.usecase.GenerateDailyQuestsUseCase
import java.time.LocalDate
import java.time.ZoneId

/**
 * Orchestrates the pure use cases against repository data to produce a single
 * [GameSnapshot]. This is the one place that touches the repository; every
 * calculation below it is a pure function and independently tested.
 */
class BioQuestEngine(
    private val repository: BioQuestRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val calculateCorruption: CalculateCorruptionUseCase = CalculateCorruptionUseCase(zone),
    private val calculateStats: CalculateStatsUseCase = CalculateStatsUseCase(zone),
    private val generateQuests: GenerateDailyQuestsUseCase = GenerateDailyQuestsUseCase(zone),
    private val detectEvents: DetectHealthEventsUseCase = DetectHealthEventsUseCase(),
    private val buildWidgetState: BuildWidgetStateUseCase = BuildWidgetStateUseCase(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun snapshot(today: LocalDate = LocalDate.now(zone)): GameSnapshot {
        val windowStart = today.minusDays(29)
        val logs = repository.logsBetween(windowStart, today)
        val goals = repository.currentGoals()
        val rules = repository.foodRules().associateBy { it.id }
        val stepsByDate = repository.stepsByDate(windowStart, today)
        val stepsToday = stepsByDate[today] ?: 0

        val corruption = calculateCorruption(logs, rules, today, goals, stepsByDate)
        val stats = calculateStats(today, logs, corruption, goals, rules, stepsToday, stepsByDate)
        val quests = generateQuests(today, logs, corruption, goals, rules, stepsToday)
        val events = detectEvents(stats, corruption, now())
        val widgetState = buildWidgetState(stats, corruption, quests)

        return GameSnapshot(
            stats = stats,
            corruption = corruption,
            quests = quests,
            events = events,
            characterClass = CharacterClass.fromStats(stats),
            widgetState = widgetState,
            goals = goals,
        )
    }
}
