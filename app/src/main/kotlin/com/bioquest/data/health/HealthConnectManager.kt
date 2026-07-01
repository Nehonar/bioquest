package com.bioquest.data.health

import android.content.Context
import java.time.LocalDate

/**
 * Thin, optional wrapper around Health Connect. Temporarily disabled due to
 * network restrictions; falls back to SensorManager. Every entry point degrades
 * gracefully: if the SDK is unavailable the app keeps working with manual data.
 */
class HealthConnectManager(private val context: Context) {

    val availability: Int get() = SDK_UNAVAILABLE

    val isAvailable: Boolean get() = false

    val permissions: Set<String> = emptySet()

    suspend fun hasStepsPermission(): Boolean = false

    suspend fun readSteps(start: LocalDate, end: LocalDate): Map<LocalDate, Int> = emptyMap()

    companion object {
        const val SDK_UNAVAILABLE = 0
    }
}
