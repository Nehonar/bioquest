package com.bioquest.domain.model

enum class QuestCadence { DAILY, WEEKLY }

enum class QuestType(val defaultTitle: String) {
    DRINK_WATER("Drink Water Protocol"),
    EAT_FRUIT("Fruit Deficit Fix"),
    MOVEMENT("Movement Core Activation"),
    MOOD_CHECK("Mood Check"),
    RECOVERY_PROTOCOL("Recovery Protocol"),
    AVOID_CORRUPTION_CHAIN("Avoid Corruption Chain"),
    WEEKLY_WORKOUTS("Weekly Training x3"),
    WEEKLY_FRUIT_DAYS("Fruit 5 Days"),
    WEEKLY_WATER_DAYS("Hydration 4 Days"),
    WEEKLY_STEP_DAYS("Steps 3 Days"),
    WEEKLY_LOW_CORRUPTION("Contain Corruption"),
}

/**
 * A generated quest with live progress. [progress]/[target] drives the bar and
 * completion; [completed] is derived so widget and board stay consistent.
 */
data class Quest(
    val type: QuestType,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val xpReward: Int,
    val cadence: QuestCadence,
) {
    val completed: Boolean get() = progress >= target
    val fraction: Float get() = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
}
