package com.bioquest.domain.model

/** The six core RPG stats. Each is normalised to 0..100. */
enum class StatType(val label: String) {
    VITALITY("VITALITY"),
    RECOVERY("RECOVERY"),
    NUTRITION("NUTRITION"),
    STRENGTH("STRENGTH"),
    FOCUS("FOCUS"),
    CORRUPTION("CORRUPTION"),
}

/**
 * One transparent reason a stat moved. The sum of contributions (clamped 0..100)
 * equals the stat value, so [com.bioquest.domain.usecase.ExplainStatUseCase] can
 * always answer "why did this go up or down?".
 */
data class StatContribution(
    val label: String,
    val delta: Int,
)

data class StatValue(
    val type: StatType,
    val value: Int,
    val contributions: List<StatContribution>,
)

/** Snapshot of all six stats for a given day. */
data class CharacterStats(
    val values: Map<StatType, StatValue>,
) {
    fun of(type: StatType): StatValue =
        values[type] ?: StatValue(type, 0, emptyList())

    val vitality get() = of(StatType.VITALITY).value
    val recovery get() = of(StatType.RECOVERY).value
    val nutrition get() = of(StatType.NUTRITION).value
    val strength get() = of(StatType.STRENGTH).value
    val focus get() = of(StatType.FOCUS).value
    val corruption get() = of(StatType.CORRUPTION).value
}
