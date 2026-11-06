package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.util.Locale

object TaskReminderScheduler {
    const val ACTION_TASK_NUDGE = "com.aistudio.lune.TASK_NUDGE"
    const val ACTION_MARK_TASK_DONE = "com.aistudio.lune.ACTION_MARK_TASK_DONE"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_REMINDER_TIME = "extra_reminder_time"
    const val NOTIFICATION_BASE_ID = 200000

    /**
     * Parses time strings into (hour24, minute).
     * Supported formats:
     * - "8:30 AM", "08:30 PM", "12:00 PM"
     * - "8:30", "14:45", "07:15"
     * - "8 AM", "8 PM"
     */
    fun parseHourMinute(timeStr: String): Pair<Int, Int>? {
        val trimmed = timeStr.trim().uppercase(Locale.getDefault())
        if (trimmed.isBlank()) return null

        val isPm = trimmed.contains("PM")
        val isAm = trimmed.contains("AM")
        val clean = trimmed.replace("AM", "").replace("PM", "").trim()

        val parts = clean.split(":")
        if (parts.isEmpty()) return null

        val rawHour = parts[0].trim().toIntOrNull() ?: return null
        val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0

        val hour24 = when {
            isPm -> if (rawHour == 12) 12 else rawHour + 12
            isAm -> if (rawHour == 12) 0 else rawHour
            else -> rawHour
        }

        if (hour24 !in 0..23 || minute !in 0..59) return null
        return Pair(hour24, minute)
    }

    /**
     * Calculates the trigger timestamp for the reminder.
     * If isTomorrow is true, schedules for tomorrow's designated time.
     * If isTomorrow is false (today):
     * - If the designated time is still in the future today, schedule for that time.
     * - If the designated time has already passed today, schedule for 1 minute in future as an immediate nudge.
     */
    fun calculateTriggerTimeMs(hour: Int, minute: Int, isTomorrow: Boolean): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (isTomorrow) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        } else if (target.timeInMillis <= now.timeInMillis) {
            // Already past today: nudge in 30 seconds
            return now.timeInMillis + 30_000L
        }

        return target.timeInMillis
    }

    /**
     * Calculates trigger timestamp for a specific target date string (yyyy-MM-dd) or fallback.
     */
    fun calculateTriggerTimeMsForDate(hour: Int, minute: Int, targetDateStr: String?): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!targetDateStr.isNullOrBlank()) {
            val parts = targetDateStr.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()?.minus(1)
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    target.set(Calendar.YEAR, y)
                    target.set(Calendar.MONTH, m)
                    target.set(Calendar.DAY_OF_MONTH, d)
                }
            }
        }

        if (target.timeInMillis <= now.timeInMillis) {
            return now.timeInMillis + 30_000L
        }

        return target.timeInMillis
    }

    /**
     * Formats hour and minute into a clean standard string: e.g. "8:30 AM" or "10:00 PM"
     */
    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        val displayMinute = String.format(Locale.getDefault(), "%02d", minute)
        return "$displayHour:$displayMinute $amPm"
    }

    /**
     * Schedules a nudge notification reminder for a task on a specific scheduled date.
     */
    fun scheduleReminder(
        context: Context,
        id: Long,
        title: String,
        reminderTime: String,
        targetDateStr: String?
    ) {
        val parsed = parseHourMinute(reminderTime) ?: return
        val triggerTimeMs = calculateTriggerTimeMsForDate(parsed.first, parsed.second, targetDateStr)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_NUDGE
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_REMINDER_TIME, reminderTime)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = NOTIFICATION_BASE_ID + (id % 100000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            Log.d("TaskReminderScheduler", "Scheduled task nudge for '$title' (ID: $id) on date $targetDateStr at $triggerTimeMs ($reminderTime)")
        } catch (e: SecurityException) {
            Log.e("TaskReminderScheduler", "Exact alarm permission missing, scheduling with set()", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Exception) {
                Log.e("TaskReminderScheduler", "Failed to schedule task reminder fallback", ex)
            }
        }
    }

    /**
     * Schedules a nudge notification reminder for a task (backward compatibility).
     */
    fun scheduleReminder(
        context: Context,
        id: Long,
        title: String,
        reminderTime: String,
        isTomorrow: Boolean
    ) {
        val targetDateStr = if (isTomorrow) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        } else null
        scheduleReminder(context, id, title, reminderTime, targetDateStr)
    }

    /**
     * Cancels any pending reminder alarm and dismisses any active nudge notification for this task.
     */
    fun cancelReminder(context: Context, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_NUDGE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = NOTIFICATION_BASE_ID + (id % 100000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        alarmManager?.cancel(pendingIntent)

        // Also cancel active notification if already displayed in status bar
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(requestCode)

        Log.d("TaskReminderScheduler", "Cancelled reminder for task ID: $id")
    }
}
