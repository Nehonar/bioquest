package com.bioquest.data.demo

import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.repository.BioQuestRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Seeds default food rules and ~3 weeks of believable demo history so the app,
 * widget and corruption engine are visible without configuring Health Connect.
 * Idempotent via the AppPreferences.demoSeeded flag.
 */
class DemoDataSeeder(
    private val repository: BioQuestRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun seedFoodRules() {
        // Persist the defaults so they show up as editable in Settings.
        FoodImpactRule.DEFAULTS.forEach { repository.upsertFoodRule(it) }
    }

    suspend fun seedHistory(today: LocalDate = LocalDate.now(zone)) {
        val rng = Random(42)
        for (offset in 20 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val logs = buildDay(date, offset, rng)
            logs.forEach { repository.addLog(it) }
            // Cache plausible auto steps for the day.
            repository.setSteps(date, 4000 + rng.nextInt(6000))
        }
    }

    private fun buildDay(date: LocalDate, offset: Int, rng: Random): List<HabitLogEntry> {
        val entries = mutableListOf<HabitLogEntry>()
        fun at(hour: Int) = date.atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()

        // Hydration: a few glasses most days.
        repeat(4 + rng.nextInt(4)) { i ->
            entries += HabitLogEntry(type = HabitType.WATER, timestampMillis = at(8 + i), quantity = 250.0)
        }
        // Fruit most days.
        if (rng.nextInt(10) < 7) {
            entries += HabitLogEntry(type = HabitType.FRUIT, timestampMillis = at(11), quantity = (1 + rng.nextInt(2)).toDouble())
        }
        // Healthy meal often.
        if (rng.nextInt(10) < 6) {
            entries += HabitLogEntry(type = HabitType.HEALTHY_MEAL, timestampMillis = at(14), quantity = 1.0)
        }
        // A recent risk-food cluster to make corruption visible (last 4 days).
        if (offset in 0..3 && rng.nextInt(10) < 7) {
            val rule = listOf("ice_cream", "pastry", "fast_food").random(rng)
            entries += HabitLogEntry(type = HabitType.RISK_FOOD, timestampMillis = at(20), quantity = 1.0, foodRuleId = rule)
        } else if (rng.nextInt(10) < 2) {
            entries += HabitLogEntry(type = HabitType.RISK_FOOD, timestampMillis = at(20), quantity = 1.0, foodRuleId = "ice_cream")
        }
        // Exercise a few times a week.
        if (rng.nextInt(10) < 4) {
            entries += HabitLogEntry(type = HabitType.EXERCISE, timestampMillis = at(18), quantity = (20 + rng.nextInt(30)).toDouble())
        }
        // Mood + sleep most days.
        entries += HabitLogEntry(type = HabitType.MOOD, timestampMillis = at(21), quantity = 3.0, moodValue = 2 + rng.nextInt(4))
        entries += HabitLogEntry(type = HabitType.SLEEP_MANUAL, timestampMillis = at(7), quantity = 0.0, sleepHours = 6.0 + rng.nextInt(4) * 0.5)
        return entries
    }
}
