package com.bioquest.di

import android.content.Context
import androidx.room.Room
import com.bioquest.data.demo.DemoDataSeeder
import com.bioquest.data.health.HealthConnectManager
import com.bioquest.data.local.BioQuestDatabase
import com.bioquest.data.repository.BioQuestRepositoryImpl
import com.bioquest.domain.BioQuestEngine
import com.bioquest.domain.repository.BioQuestRepository
import com.bioquest.domain.usecase.ExplainStatUseCase
import com.bioquest.domain.usecase.LogHabitUseCase
import com.bioquest.settings.AppPreferences

/**
 * Manual dependency container. Kept intentionally simple (no Hilt) so the MVP
 * has no annotation-processing surprises. Constructed once in the Application.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: BioQuestDatabase = Room.databaseBuilder(
        appContext,
        BioQuestDatabase::class.java,
        BioQuestDatabase.NAME,
    ).fallbackToDestructiveMigration().build()

    val repository: BioQuestRepository = BioQuestRepositoryImpl(database)

    val preferences: AppPreferences = AppPreferences(appContext)

    val healthConnectManager: HealthConnectManager = HealthConnectManager(appContext)

    val engine: BioQuestEngine = BioQuestEngine(repository)

    val logHabit: LogHabitUseCase = LogHabitUseCase(repository)

    val explainStat: ExplainStatUseCase = ExplainStatUseCase()

    val demoSeeder: DemoDataSeeder = DemoDataSeeder(repository)
}
