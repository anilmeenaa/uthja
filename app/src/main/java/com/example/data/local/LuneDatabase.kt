package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.DismissType
import com.example.data.model.LeaderboardEntity
import com.example.data.model.SleepSessionEntity
import com.example.data.model.SnoreEventEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WakeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AlarmEntity::class,
        AlarmEventEntity::class,
        SleepSessionEntity::class,
        SnoreEventEntity::class,
        UserProfileEntity::class,
        LeaderboardEntity::class,
        TaskEntity::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LuneDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmEventDao(): AlarmEventDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun snoreEventDao(): SnoreEventDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: LuneDatabase? = null

        fun getDatabase(context: Context): LuneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    buildDatabase(context)
                } catch (e: Throwable) {
                    android.util.Log.e("LuneDatabase", "Failed to open Room database. Resetting corrupted database.", e)
                    try {
                        context.applicationContext.deleteDatabase("lune_database")
                    } catch (_: Throwable) {}
                    buildDatabase(context)
                }.also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LuneDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LuneDatabase::class.java,
                "lune_database"
            )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed starter data in coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val database = getDatabase(context)
                                seedInitialData(database)
                            } catch (e: Throwable) {
                                android.util.Log.e("LuneDatabase", "Error during onCreate seeding", e)
                            }
                        }
                    }
                })
                .build()
        }

        private suspend fun seedInitialData(database: LuneDatabase) {
            try {
                val userProfileDao = database.userProfileDao()
                val alarmDao = database.alarmDao()
                val sleepSessionDao = database.sleepSessionDao()
                val alarmEventDao = database.alarmEventDao()

                userProfileDao.insertOrUpdate(
                    UserProfileEntity(
                        id = 1,
                        name = "Alex",
                        sleepGoalMinutes = 480, // 8h
                        defaultWakeMode = WakeMode.FOCUS,
                        wakeDifficulty = "Medium",
                        strictNoSnoozeDefault = true,
                        isOnboardingCompleted = true,
                        bedtimeHour = 23,
                        bedtimeMinute = 15
                    )
                )

                val alarm1Id = alarmDao.insertAlarm(
                    AlarmEntity(
                        title = "Rise & Focus",
                        hour = 7,
                        minute = 0,
                        repeatDays = "MON,TUE,WED,THU,FRI",
                        isEnabled = true,
                        wakeMode = WakeMode.FOCUS,
                        dismissType = DismissType.MATH,
                        sound = "Morning Glow",
                        strictNoSnooze = true
                    )
                )

                alarmDao.insertAlarm(
                    AlarmEntity(
                        title = "Deep Sleep Breakthrough",
                        hour = 8,
                        minute = 30,
                        repeatDays = "SAT,SUN",
                        isEnabled = false,
                        wakeMode = WakeMode.BEAST,
                        dismissType = DismissType.SHAKE,
                        sound = "Beast Siren",
                        strictNoSnooze = true
                    )
                )

                val now = System.currentTimeMillis()
                val dayMs = 86_400_000L
                val hourMs = 3_600_000L

                // Seed alarm events
                val eventSeeds = listOf(
                    Triple(32, 1, true),
                    Triple(45, 2, true),
                    Triple(28, 1, true),
                    Triple(54, 3, true),
                    Triple(39, 1, true)
                )

                eventSeeds.forEachIndexed { index, (seconds, attempts, success) ->
                    alarmEventDao.insertEvent(
                        AlarmEventEntity(
                            alarmId = alarm1Id,
                            triggeredAt = now - (index * dayMs) - (2 * hourMs),
                            dismissedAt = now - (index * dayMs) - (2 * hourMs) + (seconds * 1000L),
                            dismissDurationSeconds = seconds,
                            dismissType = if (index % 2 == 0) DismissType.MATH else DismissType.SHAKE,
                            attempts = attempts,
                            success = success,
                            wakeMode = WakeMode.FOCUS
                        )
                    )
                }

                // Seed initial tasks matching today's plan design
                val taskDao = database.taskDao()
                val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                taskDao.insertTasks(
                    listOf(
                        TaskEntity(title = "Drink water", timePill = "2 glasses", iconType = "WATER", isCompleted = true, orderIndex = 0, isDaily = true, lastCompletedDate = todayDateStr),
                        TaskEntity(title = "Reading", timePill = "10 pages", iconType = "BOOK", isCompleted = true, orderIndex = 1, isDaily = true, lastCompletedDate = todayDateStr),
                        TaskEntity(title = "Meditation", timePill = "10 min", iconType = "MEDITATION", isCompleted = false, orderIndex = 2, isDaily = true),
                        TaskEntity(title = "Take a walk", timePill = "20 min", iconType = "WORKOUT", isCompleted = false, orderIndex = 3, isDaily = true),
                        TaskEntity(title = "Mindful break", timePill = "5 min", iconType = "COFFEE", isCompleted = false, orderIndex = 4, isDaily = true),
                        TaskEntity(title = "Wind down", timePill = "10:30 PM", iconType = "MOON", isCompleted = false, orderIndex = 5, isDaily = true)
                    )
                )
            } catch (e: Throwable) {
                android.util.Log.e("LuneDatabase", "Error seeding initial database data", e)
            }
        }
    }
}
