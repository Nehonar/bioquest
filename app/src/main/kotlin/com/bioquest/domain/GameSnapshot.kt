package com.bioquest.domain

import com.bioquest.domain.model.CharacterClass
import com.bioquest.domain.model.CharacterStats
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.HealthEvent
import com.bioquest.domain.model.Quest
import com.bioquest.domain.model.UserGoal
import com.bioquest.domain.model.WidgetState

/** The fully composed game state for a single day, ready to render anywhere. */
data class GameSnapshot(
    val stats: CharacterStats,
    val corruption: CorruptionResult,
    val quests: List<Quest>,
    val events: List<HealthEvent>,
    val characterClass: CharacterClass,
    val widgetState: WidgetState,
    val goals: UserGoal,
) {
    val dailyQuests get() = quests.filter { it.cadence == com.bioquest.domain.model.QuestCadence.DAILY }
    val weeklyQuests get() = quests.filter { it.cadence == com.bioquest.domain.model.QuestCadence.WEEKLY }
}
