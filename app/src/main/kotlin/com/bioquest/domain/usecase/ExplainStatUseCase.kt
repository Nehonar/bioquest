package com.bioquest.domain.usecase

import com.bioquest.domain.model.CharacterStats
import com.bioquest.domain.model.StatType

/**
 * Produces human-readable, signed reasons for a stat's current value. The
 * "transparencia" principle: every stat explains why it went up or down.
 */
class ExplainStatUseCase {
    operator fun invoke(stats: CharacterStats, type: StatType): List<String> {
        val stat = stats.of(type)
        if (stat.contributions.isEmpty()) {
            return listOf("Sin datos suficientes para ${type.label}.")
        }
        return stat.contributions
            .filter { it.delta != 0 }
            .sortedByDescending { kotlin.math.abs(it.delta) }
            .map { "${if (it.delta >= 0) "+" else ""}${it.delta}  ${it.label}" }
            .ifEmpty { listOf("Sin cambios registrados hoy.") }
    }
}
