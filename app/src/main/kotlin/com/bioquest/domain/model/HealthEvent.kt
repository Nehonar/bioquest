package com.bioquest.domain.model

enum class HealthEventType(val code: String) {
    CHAIN_DAMAGE("CHAIN DAMAGE"),
    CRITICAL_CORRUPTION("CRITICAL CORRUPTION"),
    RECOVERY_DEBT("RECOVERY DEBT"),
    HYDRATION_LOW("HYDRATION LOW"),
    FRUIT_DEFICIT("FRUIT DEFICIT"),
    CORE_STABLE("CORE STABLE"),
}

enum class EventSeverity { INFO, WARNING, CRITICAL }

/** A system-log style event surfaced on the Dashboard and History timeline. */
data class HealthEvent(
    val type: HealthEventType,
    val severity: EventSeverity,
    val message: String,
    val timestampMillis: Long,
)
