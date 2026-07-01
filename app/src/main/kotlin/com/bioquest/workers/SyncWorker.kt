package com.bioquest.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bioquest.BioQuestApplication
import com.bioquest.data.local.entity.DailyStatSnapshotEntity
import com.bioquest.widget.BioCoreWidget
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Periodic, inexact sync: pulls automatic steps from Health Connect when
 * available, persists a daily stat snapshot and refreshes the widget. Never
 * blocks the app — all reads degrade gracefully.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = BioQuestApplication.from(applicationContext as android.app.Application).container
        val today = LocalDate.now()

        // Optional Health Connect step import.
        runCatching {
            if (container.healthConnectManager.hasStepsPermission()) {
                val steps = container.healthConnectManager.readSteps(today.minusDays(6), today)
                steps.forEach { (date, count) -> container.repository.setSteps(date, count) }
            }
        }

        // Persist today's snapshot for History/trends.
        runCatching {
            val s = container.engine.snapshot(today).stats
            container.database.dailyStatSnapshotDao().upsert(
                DailyStatSnapshotEntity(
                    epochDay = today.toEpochDay(),
                    vitality = s.vitality, recovery = s.recovery, nutrition = s.nutrition,
                    strength = s.strength, focus = s.focus, corruption = s.corruption,
                ),
            )
        }

        runCatching { BioCoreWidget.requestUpdate(applicationContext) }
        return Result.success()
    }
}

object SyncScheduler {
    private const val WORK_NAME = "bioquest_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(3, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
