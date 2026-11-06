package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class WakeMode(val title: String) {
    GENTLE("Gentle"),
    FOCUS("Focus"),
    BEAST("Beast")
}

enum class DismissType(val title: String) {
    MATH("Math"),
    SHAKE("Shake"),
    BOTH("Math & Shake"),
    COLOR_TILES("Color Tiles"),
    TYPING("Typing"),
    STEP("Step"),
    QR_CODE("QR / Barcode"),
    OFF("No Mission")
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: String = "DAILY",
    val isEnabled: Boolean = true,
    val wakeMode: WakeMode = WakeMode.FOCUS,
    val dismissType: DismissType = DismissType.MATH,
    val sound: String = "Good Morningggg",
    val wallpaper: String = WallpaperCatalog.DEFAULT_WALLPAPER_ID,
    val strictNoSnooze: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val missions: String = "MATH",
    val gradualVolume: Boolean = false,
    val gentleMissionVolume: Boolean = false
) {
    fun getMissions(): List<DismissType> {
        if (missions.isNotBlank()) {
            val list = missions.split(",").mapNotNull {
                try {
                    DismissType.valueOf(it.trim())
                } catch (e: Exception) {
                    null
                }
            }.flatMap {
                if (it == DismissType.BOTH) listOf(DismissType.MATH, DismissType.SHAKE)
                else listOf(it)
            }
            if (list.isNotEmpty()) return list
        }
        return if (dismissType == DismissType.BOTH) {
            listOf(DismissType.MATH, DismissType.SHAKE)
        } else {
            listOf(dismissType)
        }
    }
}

@Entity(
    tableName = "alarm_events",
    indices = [Index(value = ["alarmId"]), Index(value = ["triggeredAt"])]
)
data class AlarmEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val triggeredAt: Long,
    val dismissedAt: Long,
    val dismissDurationSeconds: Int,
    val dismissType: DismissType,
    val attempts: Int = 1,
    val success: Boolean = true,
    val wakeMode: WakeMode = WakeMode.FOCUS
)

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val durationMinutes: Int,
    val sleepGoalMet: Boolean,
    val source: String = "MANUAL",
    val notes: String = "",
    val movementCount: Int = 0,
    val deepSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val restlessMinutes: Int = 0,
    val sleepScore: Int = 85,
    val movementRating: String = "Peaceful",
    val snoreEventCount: Int = 0,
    val snoreDurationMinutes: Int = 0,
    val snorePercentage: Int = 0,
    val avgDecibels: Float = 0f,
    val peakDecibels: Float = 0f
)

@Entity(
    tableName = "snore_events",
    indices = [Index(value = ["sessionId"])]
)
data class SnoreEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Int,
    val peakScore: Float,
    val avgDecibels: Float,
    val soundLabel: String = "Snoring"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Morning Visionary",
    val email: String? = null,
    val photoUrl: String? = null,
    val isGoogleLinked: Boolean = false,
    val sleepGoalMinutes: Int = 480,
    val defaultWakeMode: WakeMode = WakeMode.FOCUS,
    val wakeDifficulty: String = "Medium",
    val strictNoSnoozeDefault: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val selectedMission: String = "Math",
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val selectedAlarmHour: Int = 7,
    val selectedAlarmMinute: Int = 0,
    val selectedWallpaper: String = WallpaperCatalog.DEFAULT_WALLPAPER_ID,
    val selectedVibe: String = "Serene"
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val score: Long = 0L,
    val points: Long = 0L,
    val totalApplesCollected: Int = 0,
    val streakCount: Int = 0,
    val isCurrentUser: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timePill: String? = null,
    val iconType: String = "WATER",
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val reminderTime: String? = null,
    val scheduledDate: String? = null, // "yyyy-MM-dd", e.g. "2026-11-05"
    val isDaily: Boolean = true,       // true if repeating daily routine, false if single-date scheduled
    val lastCompletedDate: String? = null // "yyyy-MM-dd" of when it was completed
) {
    /**
     * Returns true if this task belongs to the routine for targetDate ("yyyy-MM-dd").
     * Daily habits belong to every day; single-date tasks belong strictly to their scheduled date.
     */
    fun isScheduledForDate(targetDate: String): Boolean {
        return if (isDaily) true else (scheduledDate == targetDate)
    }

    /**
     * Returns true if this task is completed for targetDate ("yyyy-MM-dd").
     * For daily routines, checks if lastCompletedDate matches targetDate.
     * For one-off tasks, checks isCompleted.
     */
    fun isCompletedForDate(targetDate: String): Boolean {
        return if (isDaily) {
            lastCompletedDate == targetDate
        } else {
            isCompleted
        }
    }
}

