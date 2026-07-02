package com.bioquest

import android.app.Application
import com.bioquest.di.AppContainer
import com.bioquest.notifications.ReminderScheduler
import com.bioquest.workers.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BioQuestApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch { seedOnFirstRun() }

        SyncScheduler.schedule(this)
        appScope.launch { ReminderScheduler.schedule(this@BioQuestApplication) }
    }

    private suspend fun seedOnFirstRun() {
        val prefs = container.preferences
        if (!prefs.demoSeeded.first()) {
            // Only default food rules: a fresh install starts with a clean
            // history and natural-baseline stats. Demo history is opt-in from
            // Settings ("Cargar datos demo").
            container.demoSeeder.seedFoodRules()
            prefs.setDemoSeeded(true)
        }
    }

    companion object {
        fun from(app: Application): BioQuestApplication = app as BioQuestApplication
    }
}
