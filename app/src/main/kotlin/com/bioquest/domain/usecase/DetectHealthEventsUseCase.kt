package com.bioquest.domain.usecase

import com.bioquest.domain.model.CharacterStats
import com.bioquest.domain.model.CorruptionLevel
import com.bioquest.domain.model.CorruptionResult
import com.bioquest.domain.model.EventSeverity
import com.bioquest.domain.model.HealthEvent
import com.bioquest.domain.model.HealthEventType
import com.bioquest.domain.model.StatType

/**
 * Derives system-log events from the current stats + corruption picture. Pure:
 * given the same snapshot it always yields the same events.
 */
class DetectHealthEventsUseCase {
    operator fun invoke(
        stats: CharacterStats,
        corruption: CorruptionResult,
        nowMillis: Long,
    ): List<HealthEvent> {
        val events = mutableListOf<HealthEvent>()

        if (corruption.chainDamage) {
            events += HealthEvent(
                HealthEventType.CHAIN_DAMAGE, EventSeverity.CRITICAL,
                "Cadena de ${corruption.chainLength} dias de riesgo. Rompe la racha hoy.",
                nowMillis,
            )
        }
        when (corruption.level) {
            CorruptionLevel.CRITICAL -> events += HealthEvent(
                HealthEventType.CRITICAL_CORRUPTION, EventSeverity.CRITICAL,
                "Evento critico: ${corruption.rawPoints} pts en 30 dias.", nowMillis,
            )
            CorruptionLevel.HIGH_RISK -> events += HealthEvent(
                HealthEventType.CRITICAL_CORRUPTION, EventSeverity.WARNING,
                "Riesgo alto: ${corruption.rawPoints} pts en 30 dias.", nowMillis,
            )
            else -> Unit
        }
        if (stats.recovery < 35) {
            events += HealthEvent(
                HealthEventType.RECOVERY_DEBT, EventSeverity.WARNING,
                "Recovery debt: descanso y mood bajos.", nowMillis,
            )
        }
        if (stats.of(StatType.VITALITY).value < 40) {
            events += HealthEvent(
                HealthEventType.HYDRATION_LOW, EventSeverity.INFO,
                "Vitality baja: revisa agua, pasos y nutricion.", nowMillis,
            )
        }
        if (events.isEmpty()) {
            events += HealthEvent(
                HealthEventType.CORE_STABLE, EventSeverity.INFO,
                "Core estable. Sin eventos activos.", nowMillis,
            )
        }
        return events
    }
}
