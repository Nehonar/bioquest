package com.bioquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.bioquest.data.local.entity.DailyStatSnapshotEntity
import com.bioquest.data.local.entity.FoodRuleEntity
import com.bioquest.data.local.entity.HabitLogEntity
import com.bioquest.data.local.entity.HealthEventEntity
import com.bioquest.data.local.entity.QuestCompletionEntity
import com.bioquest.data.local.entity.StepCountEntity
import com.bioquest.data.local.entity.UserGoalEntity
import com.bioquest.data.local.entity.WidgetActionConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_log ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_log WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis DESC")
    suspend fun between(startMillis: Long, endMillis: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_log ORDER BY timestampMillis DESC")
    suspend fun all(): List<HabitLogEntity>

    @Insert
    suspend fun insert(entity: HabitLogEntity): Long

    @Query("DELETE FROM habit_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM habit_log")
    suspend fun count(): Int

    @Query("DELETE FROM habit_log")
    suspend fun clearAll()
}

@Dao
interface FoodRuleDao {
    @Query("SELECT * FROM food_rule ORDER BY corruptionPoints ASC")
    fun observeAll(): Flow<List<FoodRuleEntity>>

    @Query("SELECT * FROM food_rule")
    suspend fun all(): List<FoodRuleEntity>

    @Upsert
    suspend fun upsert(rule: FoodRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<FoodRuleEntity>)

    @Query("SELECT COUNT(*) FROM food_rule")
    suspend fun count(): Int
}

@Dao
interface UserGoalDao {
    @Query("SELECT * FROM user_goal WHERE id = 0")
    fun observe(): Flow<UserGoalEntity?>

    @Query("SELECT * FROM user_goal WHERE id = 0")
    suspend fun get(): UserGoalEntity?

    @Upsert
    suspend fun upsert(goal: UserGoalEntity)
}

@Dao
interface StepCountDao {
    @Query("SELECT * FROM step_count WHERE epochDay BETWEEN :startDay AND :endDay")
    suspend fun between(startDay: Long, endDay: Long): List<StepCountEntity>

    @Upsert
    suspend fun upsert(entity: StepCountEntity)

    @Query("DELETE FROM step_count")
    suspend fun clearAll()
}

@Dao
interface DailyStatSnapshotDao {
    @Upsert
    suspend fun upsert(entity: DailyStatSnapshotEntity)

    @Query("SELECT * FROM daily_stat_snapshot ORDER BY epochDay DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DailyStatSnapshotEntity>

    @Query("DELETE FROM daily_stat_snapshot")
    suspend fun clearAll()
}

@Dao
interface QuestCompletionDao {
    @Upsert
    suspend fun upsert(entity: QuestCompletionEntity)

    @Query("SELECT * FROM quest_completion WHERE epochDay = :epochDay")
    suspend fun forDay(epochDay: Long): List<QuestCompletionEntity>

    @Query("DELETE FROM quest_completion")
    suspend fun clearAll()
}

@Dao
interface HealthEventDao {
    @Insert
    suspend fun insert(entity: HealthEventEntity)

    @Query("SELECT * FROM health_event ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HealthEventEntity>>

    @Query("DELETE FROM health_event")
    suspend fun clearAll()
}

@Dao
interface WidgetActionConfigDao {
    @Upsert
    suspend fun upsert(entity: WidgetActionConfigEntity)

    @Query("SELECT * FROM widget_action_config WHERE glanceId = :glanceId")
    suspend fun get(glanceId: Int): WidgetActionConfigEntity?

    @Query("DELETE FROM widget_action_config WHERE glanceId = :glanceId")
    suspend fun delete(glanceId: Int)
}
