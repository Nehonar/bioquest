package com.bioquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioquest.data.local.dao.DailyStatSnapshotDao
import com.bioquest.data.local.dao.FoodRuleDao
import com.bioquest.data.local.dao.HabitLogDao
import com.bioquest.data.local.dao.HealthEventDao
import com.bioquest.data.local.dao.QuestCompletionDao
import com.bioquest.data.local.dao.StepCountDao
import com.bioquest.data.local.dao.UserGoalDao
import com.bioquest.data.local.dao.WidgetActionConfigDao
import com.bioquest.data.local.entity.DailyStatSnapshotEntity
import com.bioquest.data.local.entity.FoodRuleEntity
import com.bioquest.data.local.entity.HabitLogEntity
import com.bioquest.data.local.entity.HealthEventEntity
import com.bioquest.data.local.entity.QuestCompletionEntity
import com.bioquest.data.local.entity.StepCountEntity
import com.bioquest.data.local.entity.UserGoalEntity
import com.bioquest.data.local.entity.WidgetActionConfigEntity

@Database(
    entities = [
        HabitLogEntity::class,
        FoodRuleEntity::class,
        UserGoalEntity::class,
        StepCountEntity::class,
        DailyStatSnapshotEntity::class,
        QuestCompletionEntity::class,
        HealthEventEntity::class,
        WidgetActionConfigEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BioQuestDatabase : RoomDatabase() {
    abstract fun habitLogDao(): HabitLogDao
    abstract fun foodRuleDao(): FoodRuleDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun stepCountDao(): StepCountDao
    abstract fun dailyStatSnapshotDao(): DailyStatSnapshotDao
    abstract fun questCompletionDao(): QuestCompletionDao
    abstract fun healthEventDao(): HealthEventDao
    abstract fun widgetActionConfigDao(): WidgetActionConfigDao

    companion object {
        const val NAME = "bioquest.db"
    }
}
