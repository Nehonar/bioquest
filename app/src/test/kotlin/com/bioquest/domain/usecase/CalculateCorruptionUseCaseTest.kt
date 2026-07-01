package com.bioquest.domain.usecase

import com.bioquest.domain.model.CorruptionLevel
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.UserGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalculateCorruptionUseCaseTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val useCase = CalculateCorruptionUseCase(zone)
    private val rules = FoodImpactRule.defaultsById()
    private val reference = LocalDate.of(2026, 6, 30)

    private fun riskLog(rule: String, daysAgo: Long, qty: Double = 1.0): HabitLogEntry {
        val date = reference.minusDays(daysAgo)
        val millis = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3_600_000L
        return HabitLogEntry(
            type = HabitType.RISK_FOOD,
            timestampMillis = millis,
            quantity = qty,
            foodRuleId = rule,
        )
    }

    @Test
    fun `no risk food yields normal empty result`() {
        val result = useCase(emptyList(), rules, reference)
        assertEquals(0, result.rawPoints)
        assertEquals(CorruptionLevel.NORMAL, result.level)
        assertFalse(result.chainDamage)
    }

    @Test
    fun `four ice creams over the window stays normal`() {
        // 4 helados x 2 pts = 8 pts -> NORMAL (plan example).
        val logs = (0L until 4L).map { riskLog("ice_cream", daysAgo = it * 3) }
        val result = useCase(logs, rules, reference)
        assertEquals(8, result.rawPoints)
        assertEquals(CorruptionLevel.NORMAL, result.level)
    }

    @Test
    fun `four ice creams plus fifteen croissants is a critical event`() {
        // 4*2 + 15*3 = 8 + 45 = 53 pts -> CRITICAL (plan example).
        val iceCreams = (0L until 4L).map { riskLog("ice_cream", daysAgo = it * 3) }
        // Spread croissants so they don't all collapse into one chain unnaturally.
        val croissants = (0L until 15L).map { riskLog("pastry", daysAgo = it) }
        val result = useCase(iceCreams + croissants, rules, reference)
        assertEquals(53, result.rawPoints)
        assertEquals(CorruptionLevel.CRITICAL, result.level)
    }

    @Test
    fun `quantity on a single log accumulates points`() {
        // One log of 4 ice creams == four separate logs.
        val single = listOf(riskLog("ice_cream", daysAgo = 1, qty = 4.0))
        assertEquals(8, useCase(single, rules, reference).rawPoints)
    }

    @Test
    fun `band boundaries follow the thresholds`() {
        val goal = UserGoal.DEFAULT
        // 12 pts (fast_food x3) sits on the NORMAL upper bound.
        val twelve = listOf(riskLog("fast_food", 1, qty = 3.0))
        assertEquals(CorruptionLevel.NORMAL, useCase(twelve, rules, reference, goal).level)

        // 14 pts crosses into WATCH (13..24).
        val watchLogs = listOf(riskLog("fast_food", 1, qty = 3.0), riskLog("ice_cream", 5))
        assertEquals(CorruptionLevel.WATCH, useCase(watchLogs, rules, reference, goal).level)

        // 25 pts crosses into HIGH_RISK (25..40); 41+ is CRITICAL.
        val highRisk = listOf(riskLog("binge", 1, qty = 3.0), riskLog("water_250", 2)) // 24 -> WATCH
        assertEquals(CorruptionLevel.WATCH, useCase(highRisk, rules, reference, goal).level)
        val critical = listOf(riskLog("binge", 1, qty = 6.0)) // 48 -> CRITICAL
        assertEquals(CorruptionLevel.CRITICAL, useCase(critical, rules, reference, goal).level)
    }

    @Test
    fun `three consecutive risk days trigger chain damage`() {
        val logs = listOf(
            riskLog("ice_cream", 0),
            riskLog("ice_cream", 1),
            riskLog("ice_cream", 2),
        )
        val result = useCase(logs, rules, reference)
        assertTrue(result.chainDamage)
        assertEquals(3, result.chainLength)
    }

    @Test
    fun `a gap breaks the chain`() {
        val logs = listOf(
            riskLog("ice_cream", 0),
            riskLog("ice_cream", 1),
            // gap on day 2
            riskLog("ice_cream", 3),
        )
        val result = useCase(logs, rules, reference)
        assertFalse(result.chainDamage)
        assertEquals(2, result.chainLength)
    }

    @Test
    fun `positive context mitigates but never erases more than half`() {
        val date = reference
        val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        // Day with 8 raw corruption points (binge) plus strong positives.
        val logs = listOf(
            HabitLogEntry(type = HabitType.RISK_FOOD, timestampMillis = millis + 1, quantity = 1.0, foodRuleId = "binge"),
            HabitLogEntry(type = HabitType.WATER, timestampMillis = millis + 2, quantity = 2000.0),
            HabitLogEntry(type = HabitType.FRUIT, timestampMillis = millis + 3, quantity = 1.0),
            HabitLogEntry(type = HabitType.HEALTHY_MEAL, timestampMillis = millis + 4, quantity = 1.0),
        )
        val steps = mapOf(date to 8000)
        val result = useCase(logs, rules, reference, UserGoal.DEFAULT, steps)

        assertEquals(8, result.rawPoints)
        // Mitigation capacity: water(2) + fruit(2) + steps(2) + healthy(1) = 7,
        // capped at floor(8*0.5)=4.
        assertEquals(4, result.mitigatedPoints)
        assertEquals(4, result.effectivePoints)
        // Level still driven by raw points.
        assertEquals(CorruptionLevel.NORMAL, result.level)
    }

    @Test
    fun `events outside the 30 day window are ignored`() {
        val old = riskLog("binge", daysAgo = 45)
        val result = useCase(listOf(old), rules, reference)
        assertEquals(0, result.rawPoints)
        assertEquals(CorruptionLevel.NORMAL, result.level)
    }
}
