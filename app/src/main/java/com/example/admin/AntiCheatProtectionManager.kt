package com.example.admin

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.example.MainActivity
import com.example.alarm.HapticsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates anti-cheat and security features for wake-up missions:
 * 1. Prevent Power-Off: Intercepts screen-off and power-off dialogs during active alarms,
 *    reasserting full-screen wake-lock, bringing the mission back to front, and counting blocks.
 * 2. Prevent App Uninstall: Manages Device Admin rights so Android refuses uninstallation
 *    or force-stop attempts while wake-up challenges are active.
 */
object AntiCheatProtectionManager {

    private const val TAG = "AntiCheatManager"
    private const val PREFS_NAME = "lune_anticheat_prefs"
    private const val KEY_PREVENT_POWER_OFF = "prevent_power_off_enabled"
    private const val KEY_POWER_OFF_BLOCKS_COUNT = "power_off_blocks_count"

    private var initialized = false
    private var prefs: SharedPreferences? = null

    private val _isPreventPowerOffEnabled = MutableStateFlow(true)
    val isPreventPowerOffEnabled: StateFlow<Boolean> = _isPreventPowerOffEnabled.asStateFlow()

    private val _powerOffBlocksCount = MutableStateFlow(0)
    val powerOffBlocksCount: StateFlow<Int> = _powerOffBlocksCount.asStateFlow()

    private val _isAlarmMissionActive = MutableStateFlow(false)
    val isAlarmMissionActive: StateFlow<Boolean> = _isAlarmMissionActive.asStateFlow()

    private val _lastBlockedMessage = MutableStateFlow<String?>(null)
    val lastBlockedMessage: StateFlow<String?> = _lastBlockedMessage.asStateFlow()

    private var receiverRegistered = false
    private var powerOffReceiver: BroadcastReceiver? = null

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedPreventPowerOff = prefs?.getBoolean(KEY_PREVENT_POWER_OFF, true) ?: true
        val savedBlocksCount = prefs?.getInt(KEY_POWER_OFF_BLOCKS_COUNT, 0) ?: 0

        _isPreventPowerOffEnabled.value = savedPreventPowerOff
        _powerOffBlocksCount.value = savedBlocksCount
        initialized = true
        Log.d(TAG, "AntiCheatProtectionManager initialized: powerOff=$savedPreventPowerOff, blocks=$savedBlocksCount")
    }

    fun setPreventPowerOffEnabled(context: Context, enabled: Boolean) {
        init(context)
        _isPreventPowerOffEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_PREVENT_POWER_OFF, enabled)?.apply()
        Log.d(TAG, "Prevent power-off setting changed to: $enabled")
    }

    fun isAntiUninstallActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, AntiUninstallAdminReceiver::class.java)
        return try {
            dpm?.isAdminActive(adminComponent) == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device admin status", e)
            false
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun getOverlayPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun getDeviceAdminActivationIntent(context: Context): Intent {
        val adminComponent = ComponentName(context, AntiUninstallAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Prevents turning off, deleting or force-stopping the alarm app to cheat morning wake-up challenges."
            )
        }
    }

    fun setAlarmMissionActive(context: Context, active: Boolean) {
        init(context)
        _isAlarmMissionActive.value = active
        Log.d(TAG, "Alarm mission active state updated: $active")

        if (active && _isPreventPowerOffEnabled.value) {
            registerPowerOffInterception(context)
        } else {
            unregisterPowerOffInterception(context)
        }
    }

    fun recordPowerOffBlockAttempt(context: Context, reason: String = "Power-off attempt blocked") {
        init(context)
        val currentCount = _powerOffBlocksCount.value + 1
        _powerOffBlocksCount.value = currentCount
        prefs?.edit()?.putInt(KEY_POWER_OFF_BLOCKS_COUNT, currentCount)?.apply()

        _lastBlockedMessage.value = "🛡️ Power-off Blocked! Prevent Power-off is active. Complete mission to turn off."

        try {
            val haptics = HapticsController(context)
            haptics.pulseError()
        } catch (e: Exception) {
            Log.w(TAG, "Haptic pulse failed on block event", e)
        }

        Log.w(TAG, "Power-off or exit cheat blocked (#$currentCount): $reason")
    }

    fun clearBlockedMessage() {
        _lastBlockedMessage.value = null
    }

    @Suppress("DEPRECATION")
    private fun registerPowerOffInterception(context: Context) {
        if (receiverRegistered) return
        val appContext = context.applicationContext

        powerOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context, intent: Intent) {
                val action = intent.action
                Log.d(TAG, "Intercepted system action during active alarm: $action")

                if (!_isAlarmMissionActive.value || !_isPreventPowerOffEnabled.value) {
                    return
                }

                if (action == Intent.ACTION_SCREEN_OFF ||
                    action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS
                ) {
                    recordPowerOffBlockAttempt(
                        recvContext,
                        if (action == Intent.ACTION_SCREEN_OFF) "Screen-off pressed" else "Power menu closed system dialogs"
                    )

                    // Re-wake device immediately using PowerManager WakeLock
                    try {
                        val pm = recvContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        val wl = pm?.newWakeLock(
                            PowerManager.FULL_WAKE_LOCK or
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                                    PowerManager.ON_AFTER_RELEASE,
                            "Lune:AntiPowerOffWakeLock"
                        )
                        wl?.acquire(5000L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to acquire re-wake lock", e)
                    }

                    // Bring MainActivity directly back to foreground over any power menu
                    val resumeIntent = Intent(recvContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("launch_alarm_active", true)
                        putExtra("power_off_blocked_event", true)
                    }
                    try {
                        recvContext.startActivity(resumeIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to re-launch MainActivity on power-off block", e)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(powerOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(powerOffReceiver, filter)
            }
            receiverRegistered = true
            Log.d(TAG, "Power-off interceptor receiver successfully registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register power-off interceptor receiver", e)
        }
    }

    private fun unregisterPowerOffInterception(context: Context) {
        if (!receiverRegistered) return
        val appContext = context.applicationContext
        try {
            powerOffReceiver?.let { appContext.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering power-off receiver", e)
        } finally {
            powerOffReceiver = null
            receiverRegistered = false
            Log.d(TAG, "Power-off interceptor receiver unregistered")
        }
    }
}
