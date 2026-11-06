package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.AlarmEntity
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_TRIGGER = "com.aistudio.lune.ALARM_TRIGGER"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_alarm_title"
    const val EXTRA_WAKE_MODE = "extra_wake_mode"
    const val EXTRA_DISMISS_TYPE = "extra_dismiss_type"
    const val EXTRA_MISSIONS = "extra_missions"
    const val EXTRA_ALARM_SOUND = "extra_alarm_sound"
    const val EXTRA_ALARM_WALLPAPER = "extra_alarm_wallpaper"
    const val EXTRA_GRADUAL_VOLUME = "extra_gradual_volume"
    const val EXTRA_GENTLE_MISSION_VOLUME = "extra_gentle_mission_volume"

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)

        val effectiveMissions = alarm.getMissions()
        val missionsPayload = effectiveMissions.joinToString(",") { it.name }
        val primaryDismissType = effectiveMissions.firstOrNull()?.name ?: alarm.dismissType.name

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_WAKE_MODE, alarm.wakeMode.name)
            putExtra(EXTRA_DISMISS_TYPE, primaryDismissType)
            putExtra(EXTRA_MISSIONS, missionsPayload)
            putExtra(EXTRA_ALARM_SOUND, alarm.sound)
            putExtra(EXTRA_ALARM_WALLPAPER, alarm.wallpaper)
            putExtra(EXTRA_GRADUAL_VOLUME, alarm.gradualVolume)
            putExtra(EXTRA_GENTLE_MISSION_VOLUME, alarm.gentleMissionVolume)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            flags
        )

        val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("launch_alarm_active", true)
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_WAKE_MODE, alarm.wakeMode.name)
            putExtra(EXTRA_DISMISS_TYPE, primaryDismissType)
            putExtra(EXTRA_MISSIONS, missionsPayload)
            putExtra(EXTRA_ALARM_SOUND, alarm.sound)
            putExtra(EXTRA_ALARM_WALLPAPER, alarm.wallpaper)
            putExtra(EXTRA_GRADUAL_VOLUME, alarm.gradualVolume)
            putExtra(EXTRA_GENTLE_MISSION_VOLUME, alarm.gentleMissionVolume)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt() + 100000,
            showIntent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} via setAlarmClock for $triggerTimeMs")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Exact alarm permission not granted, trying fallback", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (fallbackEx: Exception) {
                Log.e("AlarmScheduler", "Fallback alarm scheduling failed", fallbackEx)
            }
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextTriggerTime(
        hour: Int,
        minute: Int,
        repeatDays: String,
        fromTimeMs: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromTimeMs }
        val target = Calendar.getInstance().apply {
            timeInMillis = fromTimeMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsedDays = parseRepeatDays(repeatDays)

        if (parsedDays.isEmpty() || repeatDays.equals("ONCE", ignoreCase = true)) {
            // One-shot: if time has passed today, schedule for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating schedule: look ahead across the full weekly cycle
        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (parsedDays.contains(dayOfWeek)) {
                if (candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback: 24h later
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    private fun parseRepeatDays(repeatDays: String): Set<Int> {
        val result = mutableSetOf<Int>()
        val trimmed = repeatDays.trim()
        if (trimmed.isBlank() || trimmed.equals("ONCE", ignoreCase = true)) return result

        val upper = trimmed.uppercase()
        if (upper == "DAILY" || upper == "EVERY DAY" || upper == "EVERYDAY") {
            return setOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        }
        if (upper == "WEEKDAYS" || upper == "WEEKDAY") {
            return setOf(
                Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
        }
        if (upper == "WEEKENDS" || upper == "WEEKEND") {
            return setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        }

        val parts = upper.split(',', '|', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
        for (part in parts) {
            when {
                part.startsWith("SUN") -> result.add(Calendar.SUNDAY)
                part.startsWith("MON") -> result.add(Calendar.MONDAY)
                part.startsWith("TUE") -> result.add(Calendar.TUESDAY)
                part.startsWith("WED") -> result.add(Calendar.WEDNESDAY)
                part.startsWith("THU") -> result.add(Calendar.THURSDAY)
                part.startsWith("FRI") -> result.add(Calendar.FRIDAY)
                part.startsWith("SAT") -> result.add(Calendar.SATURDAY)
            }
        }
        return result
    }

    fun formatRemainingTime(triggerTimeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMs = (triggerTimeMs - nowMs).coerceAtLeast(0L)
        val totalMinutes = diffMs / (60 * 1000L)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            diffMs < 60_000L -> "Rings in less than 1 min"
            days > 0 -> {
                when {
                    hours > 0 && minutes > 0 -> "Rings in ${days}d ${hours} hr. ${minutes} min"
                    hours > 0 -> "Rings in ${days}d ${hours} hr."
                    minutes > 0 -> "Rings in ${days}d ${minutes} min"
                    else -> "Rings in ${days}d"
                }
            }
            hours > 0 -> {
                if (minutes > 0) {
                    "Rings in $hours hr. $minutes min"
                } else {
                    "Rings in $hours hr."
                }
            }
            minutes > 0 -> "Rings in $minutes min"
            else -> "Rings in less than 1 min"
        }
    }
}
