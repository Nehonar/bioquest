package com.bioquest.domain.usecase

import com.bioquest.domain.model.CharacterClass
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.StatType
import com.bioquest.domain.model.UserGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalculateStatsUseCaseTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val useCase = CalculateStatsUseCase(zone)
    private val goals = UserGoal.DEFAULT
    private val reference = LocalDate.of(2026, 6, 30)

    private fun log(
        type: HabitType,
        quantity: Double = 1.0,
        mood: Int? = null,
        sleep: Double? = null,
        daysAgo: Long = 0,
    ): HabitLogEntry {
        val millis = reference.minusDays(daysAgo)
            .atStartOfDay(zone).toInstant().toEpochMilli() + 9 * 3_600_000L
        return HabitLogEntry(
            type = type,
            timestampMillis = millis,
            quantity = quantity,
            moodValue = mood,
            sleepHours = sleep,
        )
    }

    @Test
    fun `empty day produces stats in range`() {
        val stats = useCase(reference, emptyList(), CorruptionResult.empty(goals), goals)
        StatType.values().forEach {
            val v = stats.of(it).value
            assertTrue("${it.label} in range", v in 0..100)
        }
        assertEquals(0, stats.corruption)
    }

    @Test
    fun `fresh start lands in a natural functional state, not wrecked`() {
        // No history at all: natural base + neutral momentum. A new user is a
        // functional Balanced Human, never a corpse or a Desk Goblin.
        val stats = useCase(reference, emptyList(), CorruptionResult.empty(goals), goals)
        assertTrue("vitality should be natural (>=40), was ${stats.vitality}", stats.vitality >= 40)
        assertTrue("recovery should be natural (>=50), was ${stats.recovery}", stats.recovery >= 50)
        assertTrue("strength should be natural (>=40), was ${stats.strength}", stats.strength >= 40)
        assertEquals(CharacterClass.BALANCED_HUMAN, CharacterClass.fromStats(stats))
    }

    @Test
    fun `a strong tracked week carries momentum into an empty morning`() {
        // Exercise + steps every day last week, nothing yet today.
        val history = (1L..7L).map { log(HabitType.EXERCISE, 30.0, daysAgo = it) }
        val stepsByDate = (1L..7L).associate { reference.minusDays(it) to 7500 }

        val withMomentum = useCase(
            reference, history, CorruptionResult.empty(goals), goals,
            stepsByDate = stepsByDate,
        )
        val freshStart = useCase(reference, emptyList(), CorruptionResult.empty(goals), goals)

        assertTrue(
            "a good week (${withMomentum.strength}) must beat a fresh start (${freshStart.strength})",
            withMomentum.strength > freshStart.strength,
        )
    }

    @Test
    fun `unlogged sleep is neutral, never worse than terrible sleep`() {
        val noSleepLog = useCase(reference, emptyList(), CorruptionResult.empty(goals), goals)
        val terribleSleep = useCase(
            reference, listOf(log(HabitType.SLEEP_MANUAL, sleep = 3.0)),
            CorruptionResult.empty(goals), goals,
        )
        assertTrue(
            "not logging (${noSleepLog.recovery}) must not read worse than 3h sleep (${terribleSleep.recovery})",
            noSleepLog.recovery >= terribleSleep.recovery,
        )
    }

    @Test
    fun `hitting all goals pushes vitality high`() {
        val logs = listOf(
            log(HabitType.WATER, 2000.0),
            log(HabitType.FRUIT, 2.0),
            log(HabitType.HEALTHY_MEAL, 2.0),
            log(HabitType.SLEEP_MANUAL, sleep = 8.0),
        )
        val stats = useCase(reference, logs, CorruptionResult.empty(goals), goals, stepsToday = 8000)
        assertTrue("vitality should be strong", stats.vitality >= 80)
    }

    @Test
    fun `contributions sum to the clamped stat value`() {
        val logs = listOf(log(HabitType.WATER, 1000.0), log(HabitType.FRUIT, 1.0))
        val stats = useCase(reference, logs, CorruptionResult.empty(goals), goals)
        val vitality = stats.of(StatType.VITALITY)
        val expected = vitality.contributions.sumOf { it.delta }.coerceIn(0, 100)
        assertEquals(expected, vitality.value)
    }

    @Test
    fun `corruption penalty lowers nutrition`() {
        val logs = listOf(log(HabitType.FRUIT, 2.0), log(HabitType.HEALTHY_MEAL, 2.0))
        val clean = useCase(reference, logs, CorruptionResult.empty(goals), goals)

        val corrupted = CorruptionResult(
            rawPoints = 40, mitigatedPoints = 0, effectivePoints = 40,
            level = com.bioquest.domain.model.CorruptionLevel.HIGH_RISK,
            chainDamage = false, chainLength = 0, perDay = emptyList(),
            explanation = emptyList(),
        )
        val penalised = useCase(reference, logs, corrupted, goals)

        assertTrue("nutrition should drop under corruption",
            penalised.nutrition < clean.nutrition)
    }

    @Test
    fun `corruption stat scales with raw points and caps at 100`() {
        val heavy = CorruptionResult(
            rawPoints = 80, mitigatedPoints = 0, effectivePoints = 80,
            level = com.bioquest.domain.model.CorruptionLevel.CRITICAL,
            chainDamage = true, chainLength = 5, perDay = emptyList(), explanation = emptyList(),
        )
        val stats = useCase(reference, emptyList(), heavy, goals)
        assertEquals(100, stats.corruption)
    }

    @Test
    fun `exercise and steps raise strength`() {
        val logs = listOf(log(HabitType.EXERCISE, 30.0))
        val stats = useCase(reference, logs, CorruptionResult.empty(goals), goals, stepsToday = 7500)
        assertTrue("strength should be high with full exercise + steps", stats.strength >= 90)
    }

    @Test
    fun `good mood lifts recovery and focus`() {
        val logs = listOf(log(HabitType.MOOD, mood = 5), log(HabitType.SLEEP_MANUAL, sleep = 8.0))
        val stats = useCase(reference, logs, CorruptionResult.empty(goals), goals)
        assertTrue(stats.recovery >= 60)
        assertTrue(stats.focus > 10)
    }
}
