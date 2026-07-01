package com.bioquest.data.local

import com.bioquest.data.local.entity.FoodRuleEntity
import com.bioquest.data.local.entity.HabitLogEntity
import com.bioquest.data.local.entity.UserGoalEntity
import com.bioquest.domain.model.FoodCategory
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.UserGoal

fun HabitLogEntity.toDomain() = HabitLogEntry(
    id = id,
    type = HabitType.valueOf(type),
    timestampMillis = timestampMillis,
    quantity = quantity,
    foodRuleId = foodRuleId,
    moodValue = moodValue,
    weightKg = weightKg,
    sleepHours = sleepHours,
    note = note,
)

fun HabitLogEntry.toEntity() = HabitLogEntity(
    id = id,
    type = type.name,
    timestampMillis = timestampMillis,
    quantity = quantity,
    foodRuleId = foodRuleId,
    moodValue = moodValue,
    weightKg = weightKg,
    sleepHours = sleepHours,
    note = note,
)

fun FoodRuleEntity.toDomain() = FoodImpactRule(
    id = id,
    label = label,
    category = FoodCategory.valueOf(category),
    nutritionXp = nutritionXp,
    vitalityXp = vitalityXp,
    corruptionPoints = corruptionPoints,
    editable = editable,
)

fun FoodImpactRule.toEntity() = FoodRuleEntity(
    id = id,
    label = label,
    category = category.name,
    nutritionXp = nutritionXp,
    vitalityXp = vitalityXp,
    corruptionPoints = corruptionPoints,
    editable = editable,
)

fun UserGoalEntity.toDomain() = UserGoal(
    waterMlGoal = waterMlGoal,
    stepsGoal = stepsGoal,
    fruitGoal = fruitGoal,
    sleepHoursGoal = sleepHoursGoal,
    exerciseMinutesGoal = exerciseMinutesGoal,
    targetWeightKg = targetWeightKg,
    corruptionWatchThreshold = corruptionWatchThreshold,
    corruptionHighThreshold = corruptionHighThreshold,
    corruptionCriticalThreshold = corruptionCriticalThreshold,
)

fun UserGoal.toEntity() = UserGoalEntity(
    id = 0,
    waterMlGoal = waterMlGoal,
    stepsGoal = stepsGoal,
    fruitGoal = fruitGoal,
    sleepHoursGoal = sleepHoursGoal,
    exerciseMinutesGoal = exerciseMinutesGoal,
    targetWeightKg = targetWeightKg,
    corruptionWatchThreshold = corruptionWatchThreshold,
    corruptionHighThreshold = corruptionHighThreshold,
    corruptionCriticalThreshold = corruptionCriticalThreshold,
)
