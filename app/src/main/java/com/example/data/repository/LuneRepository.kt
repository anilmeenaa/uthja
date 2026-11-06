package com.example.data.repository

import com.example.data.local.AlarmDao
import com.example.data.local.AlarmEventDao
import com.example.data.local.LeaderboardDao
import com.example.data.local.SleepSessionDao
import com.example.data.local.SnoreEventDao
import com.example.data.local.TaskDao
import com.example.data.local.UserProfileDao
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.LeaderboardEntity
import com.example.data.model.SleepSessionEntity
import com.example.data.model.SnoreEventEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class LuneRepository(
    private val alarmDao: AlarmDao,
    private val alarmEventDao: AlarmEventDao,
    private val sleepSessionDao: SleepSessionDao,
    private val snoreEventDao: SnoreEventDao,
    private val userProfileDao: UserProfileDao,
    private val leaderboardDao: LeaderboardDao,
    private val taskDao: TaskDao
) {
    val alarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()
    val alarmEvents: Flow<List<AlarmEventEntity>> = alarmEventDao.getAllEvents()
    val sleepSessions: Flow<List<SleepSessionEntity>> = sleepSessionDao.getAllSleepSessions()
    val snoreEvents: Flow<List<SnoreEventEntity>> = snoreEventDao.getAllSnoreEvents()
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()
    val leaderboard: Flow<List<LeaderboardEntity>> = leaderboardDao.getAllLeaderboard()
    val tasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun saveAlarm(alarm: AlarmEntity): Long {
        return if (alarm.id == 0L) {
            alarmDao.insertAlarm(alarm)
        } else {
            alarmDao.updateAlarm(alarm)
            alarm.id
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) {
        alarmDao.setAlarmEnabled(id, isEnabled)
    }

    suspend fun logDismissEvent(event: AlarmEventEntity): Long {
        return alarmEventDao.insertEvent(event)
    }

    suspend fun logSleepSession(session: SleepSessionEntity): Long {
        return sleepSessionDao.insertSleepSession(session)
    }

    suspend fun getSleepSessionById(id: Long): SleepSessionEntity? {
        return sleepSessionDao.getSleepSessionById(id)
    }

    suspend fun deleteSleepSession(session: SleepSessionEntity) {
        sleepSessionDao.deleteSleepSession(session)
    }

    suspend fun logSnoreEvent(event: SnoreEventEntity): Long {
        return snoreEventDao.insertSnoreEvent(event)
    }

    suspend fun logSnoreEvents(events: List<SnoreEventEntity>) {
        snoreEventDao.insertSnoreEvents(events)
    }

    fun getSnoreEventsForSession(sessionId: Long): Flow<List<SnoreEventEntity>> {
        return snoreEventDao.getSnoreEventsForSession(sessionId)
    }

    suspend fun getSnoreEventsForSessionDirect(sessionId: Long): List<SnoreEventEntity> {
        return snoreEventDao.getSnoreEventsForSessionDirect(sessionId)
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdate(profile)
    }

    suspend fun getUserProfileDirect(): UserProfileEntity? {
        return userProfileDao.getUserProfileDirect()
    }

    suspend fun updateLeaderboardEntry(entry: LeaderboardEntity) {
        leaderboardDao.insertOrUpdate(entry)
    }

    suspend fun saveAllLeaderboardEntries(entries: List<LeaderboardEntity>) {
        leaderboardDao.insertAll(entries)
    }

    suspend fun getCurrentUserLeaderboard(): LeaderboardEntity? {
        return leaderboardDao.getCurrentUserEntry()
    }

    suspend fun saveTask(task: TaskEntity): Long {
        return if (task.id == 0L) {
            taskDao.insertTask(task)
        } else {
            taskDao.updateTask(task)
            task.id
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
        taskDao.setTaskCompleted(id, isCompleted)
    }

    suspend fun setTaskCompletedWithDate(id: Long, isCompleted: Boolean, completedDate: String?) {
        taskDao.setTaskCompletedWithDate(id, isCompleted, completedDate)
    }

    suspend fun resetAllTasks() {
        taskDao.resetAllTasks()
    }

    suspend fun updateTasksOrder(reorderedTasks: List<TaskEntity>) {
        val updated = reorderedTasks.mapIndexed { index, task ->
            task.copy(orderIndex = index)
        }
        taskDao.insertTasks(updated)
    }

    suspend fun getTaskCount(): Int = taskDao.getTaskCount()

    suspend fun seedInitialTasksIfEmpty(defaultTasks: List<TaskEntity>) {
        if (taskDao.getTaskCount() == 0) {
            taskDao.insertTasks(defaultTasks)
        }
    }
}
