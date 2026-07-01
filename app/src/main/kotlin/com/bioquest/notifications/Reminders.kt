package com.bioquest.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bioquest.BioQuestApplication
import com.bioquest.R
import com.bioquest.domain.GameSnapshot
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "bioquest_reminders"

/**
 * Contextual, non-spammy reminders. A single periodic worker inspects the live
 * snapshot and posts at most one nudge per run, only when the context warrants
 * it (fruit deficit in the afternoon, hydration gap, low movement, recovery).
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = BioQuestApplication.from(applicationContext as android.app.Application).container
        if (!container.preferences.notificationsEnabled.first()) return Result.success()

        val snapshot = runCatching { container.engine.snapshot() }.getOrNull() ?: return Result.success()
        val message = pickReminder(snapshot, LocalTime.now()) ?: return Result.success()
        notify(message)
        return Result.success()
    }

    private fun pickReminder(snapshot: GameSnapshot, now: LocalTime): String? {
        val afternoon = now.hour in 15..19
        val evening = now.hour in 20..23
        return when {
            snapshot.corruption.chainDamage ->
                "CHAIN DAMAGE activo: rompe la racha hoy para proteger tu core."
            afternoon && snapshot.stats.nutrition < 45 ->
                "FRUIT DEFICIT: aun no hay fruta suficiente hoy. +1 pieza?"
            afternoon && snapshot.stats.vitality < 40 ->
                "HYDRATION LOW: llevas horas sin agua. +250 ml?"
            afternoon && snapshot.stats.strength < 35 ->
                "MOVEMENT CORE: pasos bajos. Un paseo corto suma."
            evening && snapshot.stats.recovery < 40 ->
                "RECOVERY PROTOCOL: registra una pausa o sueno reparador."
            else -> null
        }
    }

    private fun notify(message: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bio_core)
            .setContentTitle("BIO CORE // STATUS")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(1001, notification) }
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "bioquest_reminders"

    fun schedule(context: Context) {
        ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BioQuest reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Recordatorios contextuales de salud" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
