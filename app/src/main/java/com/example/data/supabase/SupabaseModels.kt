package com.example.data.supabase

import com.example.data.model.AlarmEntity
import com.example.data.model.DismissType
import com.example.data.model.SleepSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WakeMode
import com.example.data.model.WallpaperCatalog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseAlarmDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("hour")
    val hour: Int,
    @SerialName("minute")
    val minute: Int,
    @SerialName("repeat_days")
    val repeatDays: String = "DAILY",
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    @SerialName("wake_mode")
    val wakeMode: String = "FOCUS",
    @SerialName("dismiss_type")
    val dismissType: String = "MATH",
    @SerialName("sound")
    val sound: String = "Good Morningggg",
    @SerialName("wallpaper")
    val wallpaper: String = WallpaperCatalog.DEFAULT_WALLPAPER_ID,
    @SerialName("strict_no_snooze")
    val strictNoSnooze: Boolean = true,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

fun AlarmEntity.toSupabaseDto(userId: String? = null): SupabaseAlarmDto =
    SupabaseAlarmDto(
        id = if (id > 0) id else null,
        userId = userId,
        title = title,
        hour = hour,
        minute = minute,
        repeatDays = repeatDays,
        isEnabled = isEnabled,
        wakeMode = wakeMode.name,
        dismissType = dismissType.name,
        sound = sound,
        wallpaper = wallpaper,
        strictNoSnooze = strictNoSnooze,
        createdAt = createdAt
    )

fun SupabaseAlarmDto.toEntity(): AlarmEntity =
    AlarmEntity(
        id = id ?: 0L,
        title = title,
        hour = hour,
        minute = minute,
        repeatDays = repeatDays,
        isEnabled = isEnabled,
        wakeMode = try { WakeMode.valueOf(wakeMode) } catch (e: Exception) { WakeMode.FOCUS },
        dismissType = try { DismissType.valueOf(dismissType) } catch (e: Exception) { DismissType.MATH },
        sound = sound,
        wallpaper = wallpaper,
        strictNoSnooze = strictNoSnooze,
        createdAt = createdAt
    )

@Serializable
data class SupabaseSleepSessionDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("started_at")
    val startedAt: Long,
    @SerialName("ended_at")
    val endedAt: Long,
    @SerialName("duration_minutes")
    val durationMinutes: Int,
    @SerialName("sleep_goal_met")
    val sleepGoalMet: Boolean,
    @SerialName("source")
    val source: String = "MANUAL",
    @SerialName("notes")
    val notes: String = "",
    @SerialName("movement_count")
    val movementCount: Int = 0,
    @SerialName("deep_sleep_minutes")
    val deepSleepMinutes: Int = 0,
    @SerialName("light_sleep_minutes")
    val lightSleepMinutes: Int = 0,
    @SerialName("restless_minutes")
    val restlessMinutes: Int = 0,
    @SerialName("sleep_score")
    val sleepScore: Int = 85,
    @SerialName("movement_rating")
    val movementRating: String = "Peaceful"
)

fun SleepSessionEntity.toSupabaseDto(userId: String? = null): SupabaseSleepSessionDto =
    SupabaseSleepSessionDto(
        id = if (id > 0) id else null,
        userId = userId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        sleepGoalMet = sleepGoalMet,
        source = source,
        notes = notes,
        movementCount = movementCount,
        deepSleepMinutes = deepSleepMinutes,
        lightSleepMinutes = lightSleepMinutes,
        restlessMinutes = restlessMinutes,
        sleepScore = sleepScore,
        movementRating = movementRating
    )

fun SupabaseSleepSessionDto.toEntity(): SleepSessionEntity =
    SleepSessionEntity(
        id = id ?: 0L,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        sleepGoalMet = sleepGoalMet,
        source = source,
        notes = notes,
        movementCount = movementCount,
        deepSleepMinutes = deepSleepMinutes,
        lightSleepMinutes = lightSleepMinutes,
        restlessMinutes = restlessMinutes,
        sleepScore = sleepScore,
        movementRating = movementRating
    )

@Serializable
data class SupabaseProfileDto(
    @SerialName("id")
    val id: Int = 1,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("name")
    val name: String = "Morning Visionary",
    @SerialName("sleep_goal_minutes")
    val sleepGoalMinutes: Int = 480,
    @SerialName("default_wake_mode")
    val defaultWakeMode: String = "FOCUS",
    @SerialName("wake_difficulty")
    val wakeDifficulty: String = "Medium",
    @SerialName("strict_no_snooze_default")
    val strictNoSnoozeDefault: Boolean = true,
    @SerialName("bedtime_hour")
    val bedtimeHour: Int = 23,
    @SerialName("bedtime_minute")
    val bedtimeMinute: Int = 0
)

fun UserProfileEntity.toSupabaseDto(userId: String? = null): SupabaseProfileDto =
    SupabaseProfileDto(
        id = id,
        userId = userId,
        name = name,
        sleepGoalMinutes = sleepGoalMinutes,
        defaultWakeMode = defaultWakeMode.name,
        wakeDifficulty = wakeDifficulty,
        strictNoSnoozeDefault = strictNoSnoozeDefault,
        bedtimeHour = bedtimeHour,
        bedtimeMinute = bedtimeMinute
    )

fun SupabaseProfileDto.toEntity(original: UserProfileEntity): UserProfileEntity =
    original.copy(
        name = name,
        sleepGoalMinutes = sleepGoalMinutes,
        defaultWakeMode = try { WakeMode.valueOf(defaultWakeMode) } catch (e: Exception) { original.defaultWakeMode },
        wakeDifficulty = wakeDifficulty,
        strictNoSnoozeDefault = strictNoSnoozeDefault,
        bedtimeHour = bedtimeHour,
        bedtimeMinute = bedtimeMinute
    )

@Serializable
data class SupabaseUserDto(
    @SerialName("uid")
    val uid: String,
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("email")
    val email: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class SupabaseLeaderboardDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("points")
    val points: Long = 0L,
    @SerialName("total_apples_collected")
    val totalApplesCollected: Int = 0,
    @SerialName("streak_count")
    val streakCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class SupabaseLeaderboardRankDto(
    @SerialName("rank")
    val rank: Long = 1L,
    @SerialName("leaderboard_id")
    val leaderboardId: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("points")
    val points: Long = 0L,
    @SerialName("total_apples_collected")
    val totalApplesCollected: Int = 0,
    @SerialName("streak_count")
    val streakCount: Int = 0,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
