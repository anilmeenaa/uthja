package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.SleepSessionEntity
import com.example.data.model.SnoreEventEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("UPDATE alarms SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean)
}

@Dao
interface AlarmEventDao {
    @Query("SELECT * FROM alarm_events ORDER BY triggeredAt DESC")
    fun getAllEvents(): Flow<List<AlarmEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AlarmEventEntity): Long

    @Query("SELECT * FROM alarm_events ORDER BY triggeredAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 20): Flow<List<AlarmEventEntity>>
}

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions ORDER BY endedAt DESC")
    fun getAllSleepSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1")
    suspend fun getSleepSessionById(id: Long): SleepSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSession(session: SleepSessionEntity): Long

    @Delete
    suspend fun deleteSleepSession(session: SleepSessionEntity)
}

@Dao
interface SnoreEventDao {
    @Query("SELECT * FROM snore_events ORDER BY startTimestamp DESC")
    fun getAllSnoreEvents(): Flow<List<SnoreEventEntity>>

    @Query("SELECT * FROM snore_events WHERE sessionId = :sessionId ORDER BY startTimestamp ASC")
    fun getSnoreEventsForSession(sessionId: Long): Flow<List<SnoreEventEntity>>

    @Query("SELECT * FROM snore_events WHERE sessionId = :sessionId ORDER BY startTimestamp ASC")
    suspend fun getSnoreEventsForSessionDirect(sessionId: Long): List<SnoreEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnoreEvent(event: SnoreEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnoreEvents(events: List<SnoreEventEntity>)

    @Query("DELETE FROM snore_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Long)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC, updatedAt ASC")
    fun getAllLeaderboard(): Flow<List<com.example.data.model.LeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: com.example.data.model.LeaderboardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<com.example.data.model.LeaderboardEntity>)

    @Query("DELETE FROM leaderboard WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM leaderboard WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserEntry(): com.example.data.model.LeaderboardEntity?

    @Query("DELETE FROM leaderboard")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllTasks(): Flow<List<com.example.data.model.TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: com.example.data.model.TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<com.example.data.model.TaskEntity>)

    @Update
    suspend fun updateTasks(tasks: List<com.example.data.model.TaskEntity>)

    @Update
    suspend fun updateTask(task: com.example.data.model.TaskEntity)

    @Delete
    suspend fun deleteTask(task: com.example.data.model.TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, lastCompletedDate = :completedDate WHERE id = :id")
    suspend fun setTaskCompletedWithDate(id: Long, isCompleted: Boolean, completedDate: String?)

    @Query("UPDATE tasks SET isCompleted = 0")
    suspend fun resetAllTasks()

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int
}

