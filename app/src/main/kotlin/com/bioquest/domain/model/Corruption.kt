package com.bioquest.domain.model

import java.time.LocalDate

/**
 * Corruption band over the 30-day moving window. Bands are derived from raw
 * accumulated points so the product examples hold exactly:
 *   - 4 helados x2 = 8 pts -> NORMAL
 *   - 4 helados x2 + 15 croissants x3 = 53 pts -> CRITICAL
 */
enum class CorruptionLevel(val label: String) {
    NORMAL("CORE STABLE"),
    WATCH("VIGILANCIA"),
    HIGH_RISK("RIESGO ALTO"),
    CRITICAL("EVENTO CRITICO");

    companion object {
        fun fromPoints(points: Int, goal: UserGoal): CorruptionLevel = when {
            points <= goal.corruptionWatchThreshold -> NORMAL
            points <= goal.corruptionHighThreshold -> WATCH
            points <= goal.corruptionCriticalThreshold -> HIGH_RISK
            else -> CRITICAL
        }
    }
}

/** Corruption contribution for a single day inside the window. */
data class DailyCorruption(
    val date: LocalDate,
    val rawPoints: Int,
    val mitigatedPoints: Int,
    val hasRiskFood: Boolean,
) {
    val effectivePoints: Int get() = (rawPoints - mitigatedPoints).coerceAtLeast(0)
}

/**
 * Full result of [com.bioquest.domain.usecase.CalculateCorruptionUseCase].
 *
 * - [rawPoints] drives the [level] (pure accumulation, matches the plan).
 * - [effectivePoints] is what feeds the Nutrition penalty, after positive-day
 *   mitigation. Mitigation never erases more than half of a day's damage, so
 *   "sin borrar lo ocurrido" holds.
 * - [chainDamage] flags 3+ consecutive risk-food days.
 */
data class CorruptionResult(
    val rawPoints: Int,
    val mitigatedPoints: Int,
    val effectivePoints: Int,
    val level: CorruptionLevel,
    val chainDamage: Boolean,
    val chainLength: Int,
    val perDay: List<DailyCorruption>,
    val explanation: List<String>,
) {
    companion object {
        fun empty(goal: UserGoal) = CorruptionResult(
            rawPoints = 0,
            mitigatedPoints = 0,
            effectivePoints = 0,
            level = CorruptionLevel.NORMAL,
            chainDamage = false,
            chainLength = 0,
            perDay = emptyList(),
            explanation = listOf("Sin patrones de riesgo en la ventana de 30 dias."),
        )
    }
}
