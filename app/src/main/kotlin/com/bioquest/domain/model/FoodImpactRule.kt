package com.bioquest.domain.model

/**
 * Editable rule that translates a food/drink into XP and corruption points.
 * Positive foods add XP; risk foods add [corruptionPoints]. Nothing is banned:
 * a single treat barely matters, an accumulation over 30 days does.
 */
data class FoodImpactRule(
    val id: String,
    val label: String,
    val category: FoodCategory,
    val nutritionXp: Int = 0,
    val vitalityXp: Int = 0,
    val corruptionPoints: Int = 0,
    val editable: Boolean = true,
) {
    companion object {
        // Default rule set matching the product plan's corruption table.
        val DEFAULTS: List<FoodImpactRule> = listOf(
            FoodImpactRule("water_250", "Agua 250 ml", FoodCategory.HEALTHY, vitalityXp = 1),
            FoodImpactRule("fruit", "Fruta", FoodCategory.HEALTHY, nutritionXp = 2),
            FoodImpactRule("healthy_meal", "Comida saludable", FoodCategory.HEALTHY, nutritionXp = 3),
            FoodImpactRule("ice_cream", "Helado", FoodCategory.SUGARY, corruptionPoints = 2),
            FoodImpactRule("pastry", "Croissant / bolleria", FoodCategory.SUGARY, corruptionPoints = 3),
            FoodImpactRule("fast_food", "Fast food / pizza", FoodCategory.FAST_FOOD, corruptionPoints = 4),
            FoodImpactRule("binge", "Atracon ultraprocesado", FoodCategory.ULTRA_PROCESSED, corruptionPoints = 8),
        )

        fun defaultsById(): Map<String, FoodImpactRule> = DEFAULTS.associateBy { it.id }
    }
}
