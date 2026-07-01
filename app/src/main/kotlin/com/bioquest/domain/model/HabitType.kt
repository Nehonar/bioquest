package com.bioquest.domain.model

/**
 * Every discrete action the user can register. The widget and Daily Log map
 * one tap to one [HabitType]. Food-related risk is modelled through
 * [RISK_FOOD] + a [FoodImpactRule] rather than a hard "forbidden food" flag.
 */
enum class HabitType {
    WATER,
    FRUIT,
    HEALTHY_MEAL,
    NORMAL_MEAL,
    RISK_FOOD,
    EXERCISE,
    MOOD,
    WEIGHT,
    SLEEP_MANUAL,
    REST_BREAK,
}
