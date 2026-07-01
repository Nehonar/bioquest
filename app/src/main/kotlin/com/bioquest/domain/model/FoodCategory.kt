package com.bioquest.domain.model

/**
 * Coarse grouping used only to organise [FoodImpactRule]s in Settings and to
 * pick sensible defaults. It never implies "forbidden" — impact is always
 * expressed as accumulated corruption points, not bans.
 */
enum class FoodCategory {
    HEALTHY,
    TREAT,
    ULTRA_PROCESSED,
    SUGARY,
    FAST_FOOD,
    CUSTOM,
}
