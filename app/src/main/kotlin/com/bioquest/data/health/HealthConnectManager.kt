package com.bioquest.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/**
 * Thin, optional wrapper around Health Connect. Every entry point degrades
 * gracefully: if the SDK is unavailable or permissions are missing the app
 * keeps working with manual data. Nothing here is required for the MVP to run.
 */
class HealthConnectManager(private val context: Context) {

    private val zone: ZoneId = ZoneId.systemDefault()

    val availability: Int get() = HealthConnectClient.getSdkStatus(context)

    val isAvailable: Boolean get() = availability == HealthConnectClient.SDK_AVAILABLE

    private val clientOrNull: HealthConnectClient?
        get() = if (isAvailable) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    suspend fun hasStepsPermission(): Boolean {
        val client = clientOrNull ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        }.getOrDefault(false)
    }

    /** Aggregated step totals per day for [start]..[end]. Empty on any failure. */
    suspend fun readSteps(start: LocalDate, end: LocalDate): Map<LocalDate, Int> {
        val client = clientOrNull ?: return emptyMap()
        if (!hasStepsPermission()) return emptyMap()
        val result = mutableMapOf<LocalDate, Int>()
        var day = start
        while (!day.isAfter(end)) {
            val startInstant = day.atStartOfDay(zone).toInstant()
            val endInstant = day.plusDays(1).atStartOfDay(zone).toInstant()
            val steps = runCatching {
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                    ),
                )[StepsRecord.COUNT_TOTAL]
            }.getOrNull()
            if (steps != null) result[day] = steps.toInt()
            day = day.plusDays(1)
        }
        return result
    }
}
