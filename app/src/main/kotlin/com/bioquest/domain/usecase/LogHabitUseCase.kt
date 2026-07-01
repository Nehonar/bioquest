package com.bioquest.domain.usecase

import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.repository.BioQuestRepository

/**
 * One-tap logging. Normalises the entry (defaults, timestamp) and persists it.
 * Callers pass only what varies; the rest is inferred from [HabitType].
 */
class LogHabitUseCase(
    private val repository: BioQuestRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        type: HabitType,
        quantity: Double = defaultQuantity(type),
        foodRuleId: String? = null,
        moodValue: Int? = null,
        weightKg: Double? = null,
        sleepHours: Double? = null,
        note: String? = null,
    ): Long {
        val entry = HabitLogEntry(
            type = type,
            timestampMillis = now(),
            quantity = quantity,
            foodRuleId = foodRuleId,
            moodValue = moodValue?.coerceIn(1, 5),
            weightKg = weightKg,
            sleepHours = sleepHours,
            note = note,
        )
        return repository.addLog(entry)
    }

    companion object {
        fun defaultQuantity(type: HabitType): Double = when (type) {
            HabitType.WATER -> 250.0
            HabitType.FRUIT -> 1.0
            HabitType.HEALTHY_MEAL -> 1.0
            HabitType.RISK_FOOD -> 1.0
            HabitType.EXERCISE -> 30.0
            HabitType.MOOD -> 3.0
            HabitType.WEIGHT -> 0.0
            HabitType.SLEEP_MANUAL -> 7.0
            HabitType.REST_BREAK -> 1.0
        }
    }
}
