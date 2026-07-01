package com.bioquest.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bioquest_prefs")

/**
 * Lightweight app preferences kept in DataStore (as opposed to the structured
 * domain data in Room): first-run seeding flag and notification opt-in.
 */
class AppPreferences(private val context: Context) {

    val demoSeeded: Flow<Boolean> = context.dataStore.data.map { it[DEMO_SEEDED] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFS] ?: true }

    suspend fun setDemoSeeded(value: Boolean) {
        context.dataStore.edit { it[DEMO_SEEDED] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[NOTIFS] = value }
    }

    private companion object {
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
        val NOTIFS = booleanPreferencesKey("notifications_enabled")
    }
}
