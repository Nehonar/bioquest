package com.bioquest.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bioquest_prefs")

/**
 * Lightweight app preferences kept in DataStore (as opposed to the structured
 * domain data in Room): first-run seeding flag, notification opt-in, and the
 * contextual-reminder cadence.
 */
class AppPreferences(private val context: Context) {

    val demoSeeded: Flow<Boolean> = context.dataStore.data.map { it[DEMO_SEEDED] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFS] ?: true }

    /** How often the reminder worker runs, in hours (user-configurable). */
    val reminderIntervalHours: Flow<Int> =
        context.dataStore.data.map { it[REMINDER_HOURS] ?: DEFAULT_REMINDER_HOURS }

    /** If a whole [reminderIntervalHours] passes with no log during waking
     *  hours, post a "did you eat/drink?" nudge with one-tap logging. */
    val inactivityNudgeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[INACTIVITY_NUDGE] ?: true }

    suspend fun setDemoSeeded(value: Boolean) {
        context.dataStore.edit { it[DEMO_SEEDED] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[NOTIFS] = value }
    }

    suspend fun setReminderIntervalHours(value: Int) {
        context.dataStore.edit { it[REMINDER_HOURS] = value.coerceIn(MIN_REMINDER_HOURS, MAX_REMINDER_HOURS) }
    }

    suspend fun setInactivityNudgeEnabled(value: Boolean) {
        context.dataStore.edit { it[INACTIVITY_NUDGE] = value }
    }

    companion object {
        const val DEFAULT_REMINDER_HOURS = 4
        const val MIN_REMINDER_HOURS = 1
        const val MAX_REMINDER_HOURS = 12

        private val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
        private val NOTIFS = booleanPreferencesKey("notifications_enabled")
        private val REMINDER_HOURS = intPreferencesKey("reminder_interval_hours")
        private val INACTIVITY_NUDGE = booleanPreferencesKey("inactivity_nudge_enabled")
    }
}
