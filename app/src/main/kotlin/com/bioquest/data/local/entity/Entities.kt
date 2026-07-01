package com.bioquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted habit log row. Mirrors the domain [com.bioquest.domain.model.HabitLogEntry]. */
@Entity(tableName = "habit_log")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val timestampMillis: Long,
    val quantity: Double,
    val foodRuleId: String? = null,
    val moodValue: Int? = null,
    val weightKg: Double? = null,
    val sleepHours: Double? = null,
    val note: String? = null,
)

/** Editable food-impact rule. Seeded from defaults, editable in Settings. */
@Entity(tableName = "food_rule")
data class FoodRuleEntity(
    @PrimaryKey val id: String,
    val label: String,
    val category: String,
    val nutritionXp: Int,
    val vitalityXp: Int,
    val corruptionPoints: Int,
    val editable: Boolean,
)

/** Single-row (id = 0) user goals table. */
@Entity(tableName = "user_goal")
data class UserGoalEntity(
    @PrimaryKey val id: Int = 0,
    val waterMlGoal: Int,
    val stepsGoal: Int,
    val fruitGoal: Int,
    val sleepHoursGoal: Double,
    val exerciseMinutesGoal: Int,
    val targetWeightKg: Double?,
    val corruptionWatchThreshold: Int,
    val corruptionHighThreshold: Int,
    val corruptionCriticalThreshold: Int,
)

/** Cached automatic step totals per date (Health Connect / SensorManager). */
@Entity(tableName = "step_count")
data class StepCountEntity(
    @PrimaryKey val epochDay: Long,
    val steps: Int,
)

/** Daily persisted snapshot of computed stats, written by the sync worker. */
@Entity(tableName = "daily_stat_snapshot")
data class DailyStatSnapshotEntity(
    @PrimaryKey val epochDay: Long,
    val vitality: Int,
    val recovery: Int,
    val nutrition: Int,
    val strength: Int,
    val focus: Int,
    val corruption: Int,
)

/** Marks a quest claimed on a given day (dedupes reward XP). */
@Entity(tableName = "quest_completion", primaryKeys = ["questType", "epochDay"])
data class QuestCompletionEntity(
    val questType: String,
    val epochDay: Long,
    val xpAwarded: Int,
)

/** System-log style event surfaced on Dashboard / History. */
@Entity(tableName = "health_event")
data class HealthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val severity: String,
    val message: String,
    val timestampMillis: Long,
)

/** Per-widget configuration for the 1x1 Quick Action widget. */
@Entity(tableName = "widget_action_config")
data class WidgetActionConfigEntity(
    @PrimaryKey val glanceId: Int,
    val quickAction: String,
)
