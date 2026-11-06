package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Uthja Wake-Up"
        val wakeMode = intent.getStringExtra(AlarmScheduler.EXTRA_WAKE_MODE) ?: "FOCUS"
        val dismissType = intent.getStringExtra(AlarmScheduler.EXTRA_DISMISS_TYPE) ?: "MATH"
        val missions = intent.getStringExtra(AlarmScheduler.EXTRA_MISSIONS) ?: dismissType
        val sound = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_SOUND) ?: "Good Morningggg"
        val wallpaper = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_WALLPAPER) ?: com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID
        val gradualVolume = intent.getBooleanExtra(AlarmScheduler.EXTRA_GRADUAL_VOLUME, true)
        val gentleMissionVolume = intent.getBooleanExtra(AlarmScheduler.EXTRA_GENTLE_MISSION_VOLUME, true)

        Log.d("AlarmReceiver", "Alarm triggered! ID=$alarmId, Mode=$wakeMode, Dismiss=$dismissType, Missions=$missions, Sound=$sound, Wallpaper=$wallpaper, Gradual=$gradualVolume, GentleMission=$gentleMissionVolume")

        // 1. Acquire temporary wake lock to ensure CPU stays alive while service starts
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Lune:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(15_000L)

        // 2. Start the Foreground Ringing Service (sound, vibration, full-screen notification)
        AlarmRingingService.start(
            context = context,
            alarmId = alarmId,
            title = title,
            wakeMode = wakeMode,
            dismissType = dismissType,
            sound = sound,
            wallpaper = wallpaper,
            missions = missions,
            gradualVolume = gradualVolume,
            gentleMissionVolume = gentleMissionVolume
        )

        // 3. Immediately launch MainActivity over lock screen
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("launch_alarm_active", true)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_WAKE_MODE, wakeMode)
            putExtra(AlarmScheduler.EXTRA_DISMISS_TYPE, dismissType)
            putExtra(AlarmScheduler.EXTRA_MISSIONS, missions)
            putExtra(AlarmScheduler.EXTRA_ALARM_SOUND, sound)
            putExtra(AlarmScheduler.EXTRA_ALARM_WALLPAPER, wallpaper)
            putExtra(AlarmScheduler.EXTRA_GRADUAL_VOLUME, gradualVolume)
            putExtra(AlarmScheduler.EXTRA_GENTLE_MISSION_VOLUME, gentleMissionVolume)
        }

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Direct activity start failed, full screen intent will trigger", e)
        }
    }
}
