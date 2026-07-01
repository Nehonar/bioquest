package com.bioquest.domain.usecase

import com.bioquest.domain.model.CharacterStats
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.Quest
import com.bioquest.domain.model.WidgetState

/**
 * Flattens stats + corruption + the next open quest into the immutable
 * [WidgetState] the Glance widget renders. Chooses the most useful warning to
 * surface and the highest-priority uncompleted daily quest.
 */
class BuildWidgetStateUseCase {
    operator fun invoke(
        stats: CharacterStats,
        corruption: CorruptionResult,
        quests: List<Quest>,
    ): WidgetState {
        val nextQuest = quests
            .filter { it.cadence == com.bioquest.domain.model.QuestCadence.DAILY && !it.completed }
            .minByOrNull { it.fraction }

        val warning = when {
            corruption.chainDamage -> "CHAIN DAMAGE x${corruption.chainLength}"
            corruption.level.ordinal >= 2 -> corruption.level.label
            stats.nutrition < 40 -> "FRUIT DEFICIT"
            stats.recovery < 35 -> "RECOVERY DEBT"
            else -> null
        }

        return WidgetState(
            statusLabel = corruption.level.label,
            vitality = stats.vitality,
            recovery = stats.recovery,
            nutrition = stats.nutrition,
            focus = stats.focus,
            corruption = stats.corruption,
            nextQuestTitle = nextQuest?.title ?: "ALL CLEAR",
            nextQuestDetail = nextQuest?.let { "${it.progress}/${it.target}" } ?: "Quests diarias completas",
            warning = warning,
        )
    }
}
