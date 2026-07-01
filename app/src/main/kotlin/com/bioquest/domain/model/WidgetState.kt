package com.bioquest.domain.model

/**
 * Everything the Bio Core 4x2 widget needs to render, precomputed by
 * [com.bioquest.domain.usecase.BuildWidgetStateUseCase] so Glance stays dumb.
 */
data class WidgetState(
    val statusLabel: String,
    val vitality: Int,
    val recovery: Int,
    val nutrition: Int,
    val focus: Int,
    val corruption: Int,
    val nextQuestTitle: String,
    val nextQuestDetail: String,
    val warning: String?,
) {
    companion object {
        val PLACEHOLDER = WidgetState(
            statusLabel = "BOOTING",
            vitality = 0,
            recovery = 0,
            nutrition = 0,
            focus = 0,
            corruption = 0,
            nextQuestTitle = "SYNC CORE",
            nextQuestDetail = "Abre BioQuest para inicializar",
            warning = null,
        )
    }
}

/** The action a Quick Action 1x1 widget performs on tap. */
enum class QuickAction(val label: String, val habit: HabitType, val quantity: Double) {
    WATER("AGUA +250", HabitType.WATER, 250.0),
    FRUIT("FRUTA +1", HabitType.FRUIT, 1.0),
    MOOD("MOOD", HabitType.MOOD, 3.0),
    TREAT("CAPRICHO", HabitType.RISK_FOOD, 1.0),
}
