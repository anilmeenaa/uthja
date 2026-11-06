package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.LuneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_nudges_channel"
        const val CHANNEL_NAME = "Task Reminders & Nudges"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, 0L)
        val taskTitle = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE) ?: "Task"
        val reminderTime = intent.getStringExtra(TaskReminderScheduler.EXTRA_REMINDER_TIME) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = TaskReminderScheduler.NOTIFICATION_BASE_ID + (taskId % 100000).toInt()

        if (action == TaskReminderScheduler.ACTION_MARK_TASK_DONE) {
            // Dismiss notification immediately
            notificationManager?.cancel(notificationId)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Mark task completed in database
                    val db = LuneDatabase.getDatabase(context)
                    db.taskDao().setTaskCompleted(taskId, true)

                    // 2. Mark task completed in SharedPreferences
                    val todayDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val prefs = context.getSharedPreferences("uthja_tasks_prefs", Context.MODE_PRIVATE)
                    val currentCompletions = prefs.getStringSet("completions_$todayDateStr", emptySet())
                        ?.toMutableSet() ?: mutableSetOf()
                    currentCompletions.add(taskId.toString())
                    prefs.edit().putStringSet("completions_$todayDateStr", currentCompletions).apply()

                    // If reaches 4 completions, mark streak
                    if (currentCompletions.size >= 4) {
                        prefs.edit().putBoolean("streak_checked_$todayDateStr", true).apply()
                    }

                    // Show success toast on UI thread
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Marked as done! 🎉", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error marking task done from notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action == TaskReminderScheduler.ACTION_TASK_NUDGE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Check if task is already completed
                    val todayDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val prefs = context.getSharedPreferences("uthja_tasks_prefs", Context.MODE_PRIVATE)
                    val completedSet = prefs.getStringSet("completions_$todayDateStr", emptySet()) ?: emptySet()

                    if (completedSet.contains(taskId.toString())) {
                        Log.d("TaskReminderReceiver", "Task $taskId is already completed today, omitting nudge notification.")
                        return@launch
                    }

                    val db = LuneDatabase.getDatabase(context)
                    // Check task in database
                    val allTasks = db.taskDao().getAllTasks()
                    // If it was already completed in Room:
                    // Note: We can also query all tasks or check count
                    createNotificationChannel(context)

                    // Build Notification
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("open_tab", "TASKS")
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        notificationId + 1000,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Action: Mark Done
                    val markDoneIntent = Intent(context, TaskReminderReceiver::class.java).apply {
                        this.action = TaskReminderScheduler.ACTION_MARK_TASK_DONE
                        putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
                    }
                    val markDonePendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId + 2000,
                        markDoneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val timeLabel = if (reminderTime.isNotBlank()) " ($reminderTime)" else ""
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle("Task Reminder • Time to finish! ⏰")
                        .setContentText("Don't forget: \"$taskTitle\"$timeLabel. Tap to check it off!")
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText("You haven't completed \"$taskTitle\"$timeLabel yet. Take a moment to finish it now and keep your streak strong! 💪")
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setColor(0xFFF43F5E.toInt())
                        .setAutoCancel(true)
                        .setContentIntent(openAppPendingIntent)
                        .addAction(android.R.drawable.checkbox_on_background, "Mark Done ✓", markDonePendingIntent)
                        .addAction(android.R.drawable.ic_menu_agenda, "Open Tasks", openAppPendingIntent)
                        .build()

                    notificationManager?.notify(notificationId, notification)
                    Log.d("TaskReminderReceiver", "Posted task nudge notification for '$taskTitle'")
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error delivering task reminder notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends a gentle nudge if scheduled tasks are not completed on time"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
