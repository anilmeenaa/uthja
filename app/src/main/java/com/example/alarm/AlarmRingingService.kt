package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.DismissType
import com.example.data.model.WakeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Foreground Service that keeps the device awake, plays continuous alarm audio & vibration,
 * and maintains the persistent full-screen alarm notification over the lock screen until the
 * challenge is completed.
 */
class AlarmRingingService : Service() {

    companion object {
        const val CHANNEL_ID = "lune_smart_alarms"
        const val CHANNEL_NAME = "Uthja Anti-Snooze Alarms"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_ALARM = "com.aistudio.lune.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.aistudio.lune.action.STOP_ALARM"
        const val ACTION_SET_MISSION_MODE = "com.aistudio.lune.action.SET_MISSION_MODE"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_WAKE_MODE = "extra_wake_mode"
        const val EXTRA_DISMISS_TYPE = "extra_dismiss_type"
        const val EXTRA_MISSIONS = "extra_missions"
        const val EXTRA_ALARM_SOUND = "extra_alarm_sound"
        const val EXTRA_ALARM_WALLPAPER = "extra_alarm_wallpaper"
        const val EXTRA_IS_MISSION_ACTIVE = "extra_is_mission_active"

        const val EXTRA_GRADUAL_VOLUME = "extra_gradual_volume"
        const val EXTRA_GENTLE_MISSION_VOLUME = "extra_gentle_mission_volume"

        @Volatile
        var instance: AlarmRingingService? = null

        fun setMissionMode(context: Context, isMissionActive: Boolean) {
            val current = instance ?: return
            if (!current.isRinging) return
            current.setMissionActive(isMissionActive)
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_SET_MISSION_MODE
                putExtra(EXTRA_IS_MISSION_ACTIVE, isMissionActive)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        fun start(
            context: Context,
            alarmId: Long,
            title: String,
            wakeMode: String,
            dismissType: String,
            sound: String = "Good Morningggg",
            wallpaper: String = com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID,
            missions: String = "",
            gradualVolume: Boolean = true,
            gentleMissionVolume: Boolean = true
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_TITLE, title)
                putExtra(EXTRA_WAKE_MODE, wakeMode)
                putExtra(EXTRA_DISMISS_TYPE, dismissType)
                putExtra(EXTRA_MISSIONS, missions.ifBlank { dismissType })
                putExtra(EXTRA_ALARM_SOUND, sound)
                putExtra(EXTRA_ALARM_WALLPAPER, wallpaper)
                putExtra(EXTRA_GRADUAL_VOLUME, gradualVolume)
                putExtra(EXTRA_GENTLE_MISSION_VOLUME, gentleMissionVolume)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("AlarmRingingService", "Failed to start foreground service", e)
            }
        }

        fun stop(context: Context) {
            // 1. Immediately terminate active service in-process if available
            try {
                instance?.let { service ->
                    service.stopAlarm()
                    service.stopForeground(STOP_FOREGROUND_REMOVE)
                    service.stopSelf()
                }
            } catch (e: Exception) {
                Log.e("AlarmRingingService", "Error stopping service instance directly", e)
            } finally {
                instance = null
            }

            // 2. Deliver stop intent to service safely
            try {
                val intent = Intent(context, AlarmRingingService::class.java).apply {
                    action = ACTION_STOP_ALARM
                }
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e("AlarmRingingService", "Failed to stop service via stopService intent", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeAudioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null
    private var isRinging = false

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentVolume: Float = 0.10f
    private var isMissionActive: Boolean = false
    private var gradualVolumeEnabled: Boolean = true
    private var gentleMissionVolumeEnabled: Boolean = true
    private var fadeJob: Job? = null
    private var playbackJob: Job? = null

    fun setMissionActive(active: Boolean) {
        isMissionActive = active
        applyVolume()
    }

    private fun applyVolume() {
        val effectiveVolume = if (isMissionActive && gentleMissionVolumeEnabled) {
            currentVolume.coerceAtMost(0.22f)
        } else {
            currentVolume
        }
        try {
            mediaPlayer?.setVolume(effectiveVolume, effectiveVolume)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            activeAudioTrack?.setVolume(effectiveVolume)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_SET_MISSION_MODE) {
            if (!isRinging) {
                stopSelf()
                return START_NOT_STICKY
            }
            val missionActive = intent.getBooleanExtra(EXTRA_IS_MISSION_ACTIVE, false)
            setMissionActive(missionActive)
            return START_STICKY
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, 0L) ?: 0L
        val title = intent?.getStringExtra(EXTRA_ALARM_TITLE) ?: "Uthja Wake-Up"
        val wakeMode = intent?.getStringExtra(EXTRA_WAKE_MODE) ?: "FOCUS"
        val dismissType = intent?.getStringExtra(EXTRA_DISMISS_TYPE) ?: "MATH"
        val missions = intent?.getStringExtra(EXTRA_MISSIONS) ?: dismissType
        val sound = intent?.getStringExtra(EXTRA_ALARM_SOUND) ?: "Good Morningggg"
        val wallpaper = intent?.getStringExtra(EXTRA_ALARM_WALLPAPER) ?: com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID
        val gradualVolume = intent?.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true) ?: true
        val gentleMissionVolume = intent?.getBooleanExtra(EXTRA_GENTLE_MISSION_VOLUME, true) ?: true

        startAlarm(alarmId, title, wakeMode, dismissType, sound, wallpaper, missions, gradualVolume, gentleMissionVolume)

        return START_STICKY
    }

    private fun startAlarm(
        alarmId: Long,
        title: String,
        wakeModeStr: String,
        dismissTypeStr: String,
        sound: String = "Good Morningggg",
        wallpaper: String = com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID,
        missionsStr: String = "",
        gradualVolume: Boolean = true,
        gentleMissionVolume: Boolean = true
    ) {
        if (isRinging) return
        isRinging = true
        gradualVolumeEnabled = gradualVolume
        gentleMissionVolumeEnabled = gentleMissionVolume

        // Notify Anti-Cheat manager that an alarm mission is ringing
        com.example.admin.AntiCheatProtectionManager.setAlarmMissionActive(applicationContext, true)

        acquireWakeLock()

        // 1. Create notification channel
        createNotificationChannel()

        // 2. Build Full-Screen Intent directly targeting MainActivity
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("launch_alarm_active", true)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_WAKE_MODE, wakeModeStr)
            putExtra(EXTRA_DISMISS_TYPE, dismissTypeStr)
            putExtra(EXTRA_MISSIONS, missionsStr.ifBlank { dismissTypeStr })
            putExtra(EXTRA_ALARM_SOUND, sound)
            putExtra(EXTRA_ALARM_WALLPAPER, wallpaper)
            putExtra(EXTRA_GRADUAL_VOLUME, gradualVolume)
            putExtra(EXTRA_GENTLE_MISSION_VOLUME, gentleMissionVolume)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            pendingFlags
        )

        val isQuickAlarm = title.contains("Quick Alarm", ignoreCase = true)
        val quickDurationSec = if (title.contains("10s")) 10 else 5

        if (isQuickAlarm) {
            serviceScope.launch {
                delay(quickDurationSec * 1000L)
                if (isRinging) {
                    stopAlarm()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        val challengeDescription = if (isQuickAlarm) {
            "Quick Alarm Active! Auto-stops in ${quickDurationSec}s."
        } else when (dismissTypeStr) {
            "SHAKE" -> "Shake Phone Challenge Active!"
            "MATH" -> "Cognitive Math Challenge Active!"
            "BOTH" -> "Math + Shake Challenges Active!"
            else -> "Awaken & Complete Challenge!"
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(if (isQuickAlarm) challengeDescription else "$challengeDescription Cannot close until completed.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(!isQuickAlarm)
            .setAutoCancel(isQuickAlarm)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // 3. Play alarm audio in a loop (Nature Birds for quick alarm)
        startAudioPlayback(if (isQuickAlarm) "Birds" else sound, wakeModeStr)

        // 4. Start pulsing vibration in a loop
        startVibration()

        // 5. Explicitly launch MainActivity so it opens over the lock screen immediately
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Failed to launch MainActivity directly", e)
        }
    }

    private fun startAudioPlayback(sound: String? = null, wakeModeStr: String = "FOCUS") {
        playbackJob?.cancel()
        playbackJob = null
        try {
            val targetVolume = when (wakeModeStr.uppercase()) {
                "GENTLE" -> 0.75f
                "BEAST" -> 1.00f
                else -> 0.85f
            }

            currentVolume = if (gradualVolumeEnabled) 0.08f else targetVolume

            val isBirds = sound?.equals("Birds", ignoreCase = true) == true ||
                    sound?.contains("bird", ignoreCase = true) == true

            if (isBirds) {
                startBirdsAudioPlayback()
                return
            }

            val ringtoneItem = sound?.let { com.example.data.model.RingtoneCatalog.findByNameOrId(it) }
            val initialVol = if (isMissionActive && gentleMissionVolumeEnabled) {
                currentVolume.coerceAtMost(0.22f)
            } else {
                currentVolume
            }

            if (ringtoneItem?.rawResId != null) {
                mediaPlayer = MediaPlayer.create(applicationContext, ringtoneItem.rawResId).apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setVolume(initialVol, initialVol)
                    start()
                }
            } else {
                val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alertUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    setVolume(initialVol, initialVol)
                    start()
                }
            }

            fadeJob?.cancel()
            if (gradualVolumeEnabled) {
                // Smooth gradual volume fade-in from gentle 8% to target over ~25s
                fadeJob = serviceScope.launch {
                    val steps = 25
                    val stepIncrement = (targetVolume - 0.08f) / steps
                    while (isActive && isRinging && currentVolume < targetVolume) {
                        delay(1000L)
                        currentVolume = (currentVolume + stepIncrement).coerceAtMost(targetVolume)
                        applyVolume()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error starting MediaPlayer", e)
        }
    }

    private fun startBirdsAudioPlayback() {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.e("AlarmRingingService", "Failed to init AudioTrack for birds", e)
                null
            } ?: return@launch

            activeAudioTrack = track
            try {
                track.play()
            } catch (e: Exception) {
                return@launch
            }

            val frequencies = listOf(1760.00, 2093.00, 2349.32, 2637.02, 2093.00, 2637.02)
            val noteDurationSeconds = 0.12
            val pauseBetween = 280L

            while (isActive && isRinging) {
                for (freq in frequencies) {
                    if (!isActive || !isRinging) break
                    val numSamples = (sampleRate * noteDurationSeconds).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val envelope = if (i < numSamples * 0.1) {
                            i / (numSamples * 0.1)
                        } else if (i > numSamples * 0.8) {
                            (numSamples - i) / (numSamples * 0.2)
                        } else 1.0

                        val vol = currentVolume
                        val wave = sin(2.0 * Math.PI * freq * t) + 0.35 * sin(4.0 * Math.PI * freq * t)
                        val sampleValue = (wave * envelope * 0.7 * vol * Short.MAX_VALUE).toInt()
                        buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    try {
                        if (isRinging && isActive) {
                            track.write(buffer, 0, buffer.size)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
                if (!isRinging || !isActive) break
                delay(pauseBetween)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 700, 300, 700, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error starting vibrator", e)
        }
    }

    fun stopAlarm() {
        isRinging = false
        fadeJob?.cancel()
        fadeJob = null
        playbackJob?.cancel()
        playbackJob = null
        isMissionActive = false
        com.example.admin.AntiCheatProtectionManager.setAlarmMissionActive(applicationContext, false)
        try {
            mediaPlayer?.let { player ->
                try { player.pause() } catch (_: Exception) {}
                try { player.stop() } catch (_: Exception) {}
                try { player.reset() } catch (_: Exception) {}
                try { player.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            activeAudioTrack?.let { track ->
                try { track.pause() } catch (_: Exception) {}
                try { track.flush() } catch (_: Exception) {}
                try { track.stop() } catch (_: Exception) {}
                try { track.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error releasing AudioTrack", e)
        } finally {
            activeAudioTrack = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error stopping vibrator", e)
        } finally {
            vibrator = null
        }

        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Lune:AlarmRingingServiceWakeLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes max safety limit
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Error releasing wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarms that display over the lock screen requiring challenge dismissal."
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setSound(alarmUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }
}
