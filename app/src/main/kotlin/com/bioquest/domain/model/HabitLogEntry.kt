package com.bioquest.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A single registered action. [quantity] carries the magnitude when relevant:
 * millilitres for [HabitType.WATER], units for [HabitType.FRUIT], minutes for
 * [HabitType.EXERCISE]. Food risk entries reference a [FoodImpactRule] by id.
 */
data class HabitLogEntry(
    val id: Long = 0L,
    val type: HabitType,
    val timestampMillis: Long,
    val quantity: Double = 1.0,
    val foodRuleId: String? = null,
    val moodValue: Int? = null,
    val weightKg: Double? = null,
    val sleepHours: Double? = null,
    val note: String? = null,
) {
    fun dateAt(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
}
