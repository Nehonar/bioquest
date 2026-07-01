package com.bioquest.domain.model

/**
 * Flavour class shown on the Dashboard, derived purely from current stats.
 * It is descriptive, never a judgement — "Chaos Snacker" is a nudge, not a scold.
 */
enum class CharacterClass(val title: String, val blurb: String) {
    DESK_GOBLIN("Desk Goblin", "Poco movimiento. Levanta el nucleo."),
    RECOVERY_MONK("Recovery Monk", "Descanso y calma en equilibrio."),
    HYDRATION_MAGE("Hydration Mage", "Hidratacion sobresaliente."),
    IRON_MONK("Iron Monk", "Fuerza y movimiento constantes."),
    CHAOS_SNACKER("Chaos Snacker", "Patrones de riesgo acumulados."),
    BALANCED_HUMAN("Balanced Human", "Todo en orden. Sigue asi.");

    companion object {
        fun fromStats(stats: CharacterStats): CharacterClass = when {
            stats.corruption >= 60 -> CHAOS_SNACKER
            stats.strength >= 70 -> IRON_MONK
            stats.vitality >= 70 && stats.nutrition >= 70 -> HYDRATION_MAGE
            stats.recovery >= 70 && stats.focus >= 60 -> RECOVERY_MONK
            stats.strength <= 30 && stats.vitality <= 40 -> DESK_GOBLIN
            else -> BALANCED_HUMAN
        }
    }
}
