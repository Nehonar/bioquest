package com.bioquest.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.bioquest.domain.model.HabitType
import com.bioquest.settings.AppPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "bioquest_reminders"
const val CONTEXTUAL_NOTIFICATION_ID = 1001
const val NUDGE_NOTIFICATION_ID = 1002

/**
 * Contextual, non-spammy reminders. A single periodic worker runs on the
 * user-configured cadence and posts at most one nudge per run:
 *
 *  1. Inactivity check-in (priority): if a whole interval passes with no log
 *     during waking hours, ask "¿has bebido o comido?" with one-tap actions.
 *  2. Otherwise a single contextual nudge (fruit deficit, hydration, movement,
 *     recovery, chain damage) when the live snapshot warrants it.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = BioQuestApplication.from(applicationContext as android.app.Application).container
        if (!container.preferences.notificationsEnabled.first()) return Result.success()

        val now = LocalTime.now()

        // 1. Inactivity check-in takes priority.
        if (container.preferences.inactivityNudgeEnabled.first() && isWakingHour(now)) {
            val intervalHours = container.preferences.reminderIntervalHours.first()
            if (isInactive(container, intervalHours)) {
                notifyInactivity()
                return Result.success()
            }
        }

        // 2. Fall back to a single contextual nudge.
        val snapshot = runCatching { container.engine.snapshot() }.getOrNull() ?: return Result.success()
        val message = pickReminder(snapshot, now) ?: return Result.success()
        notifyText(message)
        return Result.success()
    }

    private fun isWakingHour(now: LocalTime): Boolean = now.hour in 9..22

    /** True when nothing has been logged for at least [intervalHours]. */
    private suspend fun isInactive(
        container: com.bioquest.di.AppContainer,
        intervalHours: Int,
    ): Boolean {
        val logs = runCatching { container.repository.recentLogs(2) }.getOrDefault(emptyList())
        val lastMillis = logs.maxOfOrNull { it.timestampMillis } ?: return true
        val elapsedHours = (System.currentTimeMillis() - lastMillis) / 3_600_000.0
        return elapsedHours >= intervalHours
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

    // --- Notifications ---------------------------------------------------------

    private fun notifyText(message: String) {
        val ctx = applicationContext
        if (!hasNotificationPermission(ctx)) return
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bio_core)
            .setContentTitle("BIO CORE // STATUS")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(CONTEXTUAL_NOTIFICATION_ID, notification) }
    }

    /** The "did you eat/drink?" check-in with one-tap logging actions. */
    private fun notifyInactivity() {
        val ctx = applicationContext
        if (!hasNotificationPermission(ctx)) return
        val text = "¿Has bebido o comido algo? Registralo en 1 toque."
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bio_core)
            .setContentTitle("BIO CORE // CHECK-IN")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .addAction(0, "AGUA", logActionIntent(ctx, HabitType.WATER, 250.0, null, requestCode = 10))
            .addAction(0, "SANO", logActionIntent(ctx, HabitType.HEALTHY_MEAL, 1.0, null, requestCode = 11))
            .addAction(0, "BOLLERIA", logActionIntent(ctx, HabitType.RISK_FOOD, 1.0, "pastry", requestCode = 12))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NUDGE_NOTIFICATION_ID, notification) }
    }

    private fun logActionIntent(
        ctx: Context,
        type: HabitType,
        quantity: Double,
        ruleId: String?,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(ctx, HabitLogReceiver::class.java).apply {
            putExtra(HabitLogReceiver.EXTRA_TYPE, type.name)
            putExtra(HabitLogReceiver.EXTRA_QUANTITY, quantity)
            if (ruleId != null) putExtra(HabitLogReceiver.EXTRA_RULE, ruleId)
        }
        return PendingIntent.getBroadcast(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent? {
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: return null
        return PendingIntent.getActivity(
            ctx,
            1,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun hasNotificationPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

object ReminderScheduler {
    private const val WORK_NAME = "bioquest_reminders"

    /** Schedule on app start using the stored cadence; keeps any existing work. */
    suspend fun schedule(context: Context) {
        ensureChannel(context)
        val hours = runCatching {
            BioQuestApplication.from(context.applicationContext as android.app.Application)
                .container.preferences.reminderIntervalHours.first()
        }.getOrDefault(AppPreferences.DEFAULT_REMINDER_HOURS)
        enqueue(context, hours, ExistingPeriodicWorkPolicy.KEEP)
    }

    /** Called from Settings when the user changes the cadence; replaces work. */
    fun reschedule(context: Context, hours: Int) {
        ensureChannel(context)
        enqueue(context, hours, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(context: Context, hours: Int, policy: ExistingPeriodicWorkPolicy) {
        val safe = hours.coerceIn(AppPreferences.MIN_REMINDER_HOURS, AppPreferences.MAX_REMINDER_HOURS)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(safe.toLong(), TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
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
