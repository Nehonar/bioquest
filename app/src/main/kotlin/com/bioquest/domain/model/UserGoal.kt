package com.bioquest.domain.model

/**
 * Editable daily targets. Corruption thresholds are tunable so the engine can
 * be adjusted from Settings without touching code.
 */
data class UserGoal(
    val waterMlGoal: Int = 2000,
    val stepsGoal: Int = 7500,
    val fruitGoal: Int = 2,
    val sleepHoursGoal: Double = 7.5,
    val exerciseMinutesGoal: Int = 30,
    val targetWeightKg: Double? = null,
    // 30-day corruption window thresholds (inclusive upper bound of each band).
    val corruptionWatchThreshold: Int = 12,
    val corruptionHighThreshold: Int = 24,
    val corruptionCriticalThreshold: Int = 40,
) {
    companion object {
        val DEFAULT = UserGoal()
    }
}
