package com.example

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import com.example.admin.AntiCheatProtectionManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.alarm.AlarmScheduler
import com.example.data.model.AlarmEntity
import com.example.data.model.DismissType
import com.example.data.model.WakeMode
import com.example.ui.LuneViewModel
import com.example.ui.components.LuneBottomNavigation
import com.example.ui.components.LuneTab
import com.example.ui.dialogs.AlarmEditDialog
import com.example.ui.dialogs.AlarmEditScreen
import com.example.ui.screens.ActiveAlarmScreen
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ColorTilesMissionScreen
import com.example.ui.screens.HabitAlarmScreen
import com.example.ui.screens.MathMissionScreen
import com.example.ui.screens.QrBarcodeMissionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.components.CubeeMascotView
import com.example.ui.screens.SleepDurationVisualizerScreen
import com.example.ui.screens.SleepScreen
import com.example.ui.screens.StepMissionScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TypingMissionScreen
import com.example.ui.screens.WakeUpMissionPickerScreen
import com.example.ui.screens.WallpaperAlarmRingingScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ThemeMode
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.LuneBackground
import com.example.ui.theme.LuneColors
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LuneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            AntiCheatProtectionManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to init AntiCheatProtectionManager", e)
        }
        enableEdgeToEdge()

        try {
            checkAlarmIntent(intent)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to check alarm intent", e)
        }

        setContent {
            MyApplicationTheme(darkTheme = false) {
                LuneApp(
                    viewModel = viewModel,
                    themeMode = ThemeMode.LIGHT,
                    onThemeModeChanged = { /* Theme change disabled: Light mode only */ }
                )
            }
        }
    }

    fun applyLockScreenAndWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {})
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    fun clearLockScreenAndWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
        @Suppress("DEPRECATION")
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    fun setImmersiveMode(enable: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enable) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.activeAlarm.value.isRinging) {
            applyLockScreenAndWakeFlags()
            setImmersiveMode(true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val isAlarmRinging = viewModel.activeAlarm.value.isRinging && !viewModel.activeAlarm.value.isConquered
        if (hasFocus && isAlarmRinging) {
            setImmersiveMode(true)
        } else if (!hasFocus && isAlarmRinging && AntiCheatProtectionManager.isPreventPowerOffEnabled.value) {
            // System power dialog or status bar pulled down during active alarm
            AntiCheatProtectionManager.recordPowerOffBlockAttempt(this, "Window focus lost to system dialog")
            val bringToFront = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            try {
                startActivity(bringToFront)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isAlarmRinging = viewModel.activeAlarm.value.isRinging && !viewModel.activeAlarm.value.isConquered
        if (isAlarmRinging && AntiCheatProtectionManager.isPreventPowerOffEnabled.value) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_POWER,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        AntiCheatProtectionManager.recordPowerOffBlockAttempt(this, "Hardware button pressed during alarm")
                        Toast.makeText(this, "🔒 Power-off is disabled during wake-up mission!", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isAlarmActive = intent.getBooleanExtra("launch_alarm_active", false)
        if (isAlarmActive) {
            applyLockScreenAndWakeFlags()
            setImmersiveMode(true)
        }
        checkAlarmIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        val alarm = viewModel.activeAlarm.value
        if (alarm.isConquered || !alarm.isRinging) {
            viewModel.stopAllAlarmEffects()
        }
    }

    private fun checkAlarmIntent(intent: Intent?) {
        if (intent == null) return
        val isAlarmActive = intent.getBooleanExtra("launch_alarm_active", false)
        if (isAlarmActive) {
            val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
            lifecycleScope.launch {
                val dbAlarm = if (alarmId > 0L) {
                    viewModel.getAlarmById(alarmId) ?: viewModel.alarms.value.firstOrNull { it.id == alarmId }
                } else null

                val title = dbAlarm?.title ?: (intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Uthja Wake-Up")
                val wakeMode = dbAlarm?.wakeMode ?: run {
                    val modeStr = intent.getStringExtra(AlarmScheduler.EXTRA_WAKE_MODE) ?: "FOCUS"
                    try { WakeMode.valueOf(modeStr) } catch (e: Exception) { WakeMode.FOCUS }
                }
                val dismissType = dbAlarm?.dismissType ?: run {
                    val dtStr = intent.getStringExtra(AlarmScheduler.EXTRA_DISMISS_TYPE) ?: "MATH"
                    try { DismissType.valueOf(dtStr) } catch (e: Exception) { DismissType.MATH }
                }
                val missionsList = if (dbAlarm != null) {
                    dbAlarm.getMissions()
                } else {
                    val missionsStr = intent.getStringExtra(AlarmScheduler.EXTRA_MISSIONS) ?: ""
                    if (missionsStr.isNotBlank()) {
                        missionsStr.split(",").mapNotNull {
                            try { DismissType.valueOf(it.trim()) } catch (e: Exception) { null }
                        }
                    } else {
                        emptyList()
                    }.ifEmpty {
                        if (dismissType == DismissType.BOTH) listOf(DismissType.MATH, DismissType.SHAKE)
                        else listOf(dismissType)
                    }
                }
                val sound = dbAlarm?.sound ?: (intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_SOUND) ?: "Good Morningggg")
                val wallpaper = dbAlarm?.wallpaper ?: (intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_WALLPAPER) ?: com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID)
                val gradualVolume = dbAlarm?.gradualVolume ?: intent.getBooleanExtra(AlarmScheduler.EXTRA_GRADUAL_VOLUME, true)
                val gentleMissionVolume = dbAlarm?.gentleMissionVolume ?: intent.getBooleanExtra(AlarmScheduler.EXTRA_GENTLE_MISSION_VOLUME, true)

                viewModel.triggerAlarmRinging(
                    alarmId = alarmId,
                    title = title,
                    wakeMode = wakeMode,
                    dismissType = dismissType,
                    sound = sound,
                    wallpaper = wallpaper,
                    missions = missionsList,
                    gradualVolume = gradualVolume,
                    gentleMissionVolume = gentleMissionVolume
                )
            }
        }
    }
}

@Composable
fun LuneApp(
    viewModel: LuneViewModel,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    val alarms by viewModel.alarms.collectAsState()
    val sleepSessions by viewModel.sleepSessions.collectAsState()
    val snoreEvents by viewModel.snoreEvents.collectAsState()
    val alarmEvents by viewModel.alarmEvents.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeAlarm by viewModel.activeAlarm.collectAsState()
    val activeSleep by viewModel.activeSleep.collectAsState()
    val googleAuthState by viewModel.googleAuthState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val isAlarmMuted by viewModel.isAlarmMuted.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentDate()
    }

    val intentOpenTab = (LocalContext.current as? Activity)?.intent?.getStringExtra("open_tab")
    var currentTab by remember { mutableStateOf(if (intentOpenTab == "TASKS") LuneTab.TASKS else LuneTab.ALARMS) }
    var alarmToEdit by remember { mutableStateOf<AlarmEntity?>(null) }
    var isAddEditAlarmOpen by remember { mutableStateOf(false) }
    var isHabitAlarmCreatorOpen by remember { mutableStateOf(false) }
    var isSleepGuidanceVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Android 13+ Notification Permission request
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
            viewModel.triggerAlarmRinging(
                alarmId = 994L,
                title = "Scan Any Barcode Challenge",
                wakeMode = mode,
                dismissType = DismissType.QR_CODE
            )
        }
    }

    val stepPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(activeAlarm.isRinging) {
        (context as? MainActivity)?.let { activity ->
            if (activeAlarm.isRinging) {
                activity.applyLockScreenAndWakeFlags()
                activity.setImmersiveMode(true)
            } else {
                activity.clearLockScreenAndWakeFlags()
                activity.setImmersiveMode(false)
            }
        }
    }

    val lastBlockedMsg by AntiCheatProtectionManager.lastBlockedMessage.collectAsState()
    LaunchedEffect(lastBlockedMsg) {
        lastBlockedMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            AntiCheatProtectionManager.clearBlockedMessage()
        }
    }

    val onAttemptExitBlocked: () -> Unit = {
        AntiCheatProtectionManager.recordPowerOffBlockAttempt(
            context,
            "Mission exit or power-off attempted during active alarm"
        )
        scope.launch {
            snackbarHostState.showSnackbar("🛡️ Prevent Power-off Active! Complete the challenge to exit or switch off.")
        }
        Unit
    }

    // Block back button while alarm is ringing until challenge is completed
    val isAlarmActiveAndUnsolved = activeAlarm.isRinging && !activeAlarm.isConquered
    BackHandler(enabled = isAlarmActiveAndUnsolved) {
        onAttemptExitBlocked()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
            // Screen content occupies 100% full screen
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    LuneTab.ALARMS -> {
                        AlarmsScreen(
                            alarms = alarms,
                            onToggleAlarm = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
                            onEditAlarm = { alarm ->
                                alarmToEdit = alarm
                                isAddEditAlarmOpen = true
                            },
                            onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm) },
                            onTestAlarm = { alarm ->
                                viewModel.triggerAlarmRinging(
                                    alarm.id,
                                    alarm.title,
                                    alarm.wakeMode,
                                    alarm.dismissType,
                                    alarm.sound,
                                    alarm.wallpaper,
                                    alarm.getMissions(),
                                    alarm.gradualVolume,
                                    alarm.gentleMissionVolume
                                )
                            },
                            onAddNewAlarm = {
                                alarmToEdit = null
                                isAddEditAlarmOpen = true
                            },
                            onAddHabitAlarm = {
                                isHabitAlarmCreatorOpen = true
                            },
                            onSaveQuickAlarm = { mins, ringSec, sound, vol, vib ->
                                viewModel.saveQuickAlarm(mins, ringSec, sound, vol, vib)
                                scope.launch {
                                    snackbarHostState.showSnackbar("⚡ Quick alarm set for $mins min (rings for ${ringSec}s)")
                                }
                            },
                            onOpenSleepVisualizer = null
                        )
                    }
                    LuneTab.SLEEP -> {
                        SleepScreen(
                            activeSleep = activeSleep,
                            sleepSessions = sleepSessions,
                            snoreEvents = snoreEvents,
                            alarms = alarms,
                            alarmEvents = alarmEvents,
                            userProfile = userProfile,
                            onStartSleep = { viewModel.startSleepSession() },
                            onFinishSleep = { viewModel.finishSleepSession() },
                            onDeleteSession = { session -> viewModel.deleteSleepSession(session) },
                            onSimulateMovement = { isRestless -> viewModel.simulateSleepMovement(isRestless) },
                            onSimulateOvernight = { viewModel.simulateOvernightTracking() },
                            onViewReport = { session ->
                                viewModel.generateMorningReport("Overnight Rest Session")
                            },
                            onAddHabitAlarm = {
                                isHabitAlarmCreatorOpen = true
                            },
                            onGuidanceVisibilityChanged = { isVisible ->
                                isSleepGuidanceVisible = isVisible
                            }
                        )
                    }
                    LuneTab.TASKS -> {
                        TasksScreen(
                            tasks = tasks,
                            currentDate = currentDate,
                            userProfile = userProfile,
                            onToggleTask = { task -> viewModel.toggleTask(task) },
                            onAddTask = { title, timePill, iconType, reminderTime, scheduledDate, isDaily ->
                                viewModel.addTask(title, timePill, iconType, reminderTime, scheduledDate, isDaily)
                            },
                            onDeleteTask = { task -> viewModel.deleteTask(task) },
                            onResetTasks = { viewModel.resetAllTasks() },
                            onUpdateTaskReminder = { task, reminderTime -> viewModel.updateTaskReminder(task, reminderTime) }
                        )
                    }
                    LuneTab.SETTINGS -> {
                        SettingsScreen(
                            userProfile = userProfile,
                            googleAuthState = googleAuthState,
                            effectiveGoogleClientId = viewModel.getEffectiveGoogleClientId(),
                            themeMode = themeMode,
                            onThemeModeChanged = onThemeModeChanged,
                            onSignInGoogle = {
                                (context as? Activity)?.let { act ->
                                    viewModel.signInWithGoogle(act) { success, err ->
                                        if (!success && err != null) {
                                            scope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    }
                                }
                            },
                            onSignInGoogleWithClientId = { clientId ->
                                (context as? Activity)?.let { act ->
                                    viewModel.signInWithGoogle(act, clientId) { success, err ->
                                        if (!success && err != null) {
                                            scope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    }
                                }
                            },
                            onSignInGoogleDemo = { email, name ->
                                viewModel.signInWithGoogleDemo(email, name) {
                                    scope.launch { snackbarHostState.showSnackbar("Signed in as $email") }
                                }
                            },
                            onSignOutGoogle = {
                                viewModel.signOutGoogle {
                                    scope.launch { snackbarHostState.showSnackbar("Signed out of Google account") }
                                }
                            },
                            onClearGoogleAuthError = { viewModel.clearGoogleAuthError() },
                            onUpdateProfile = { updated -> viewModel.updateProfile(updated) },
                            onTestAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                viewModel.triggerAlarmRinging(
                                    alarmId = 999L,
                                    title = "Uthja Math Wake Test",
                                    wakeMode = mode,
                                    dismissType = DismissType.MATH
                                )
                            },
                            onTestShakeAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                viewModel.triggerAlarmRinging(
                                    alarmId = 998L,
                                    title = "Morning Alarm Preview",
                                    wakeMode = mode,
                                    dismissType = DismissType.SHAKE,
                                    sound = com.example.data.model.RingtoneCatalog.MEME_UTH_JA.name
                                )
                            },
                            onTestColorTilesAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                viewModel.triggerAlarmRinging(
                                    alarmId = 997L,
                                    title = "Color Tiles Challenge",
                                    wakeMode = mode,
                                    dismissType = DismissType.COLOR_TILES
                                )
                            },
                            onTestTypingAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                viewModel.triggerAlarmRinging(
                                    alarmId = 996L,
                                    title = "Typing Alert Challenge",
                                    wakeMode = mode,
                                    dismissType = DismissType.TYPING
                                )
                            },
                            onTestStepAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val hasActivity = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACTIVITY_RECOGNITION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasActivity) {
                                        stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    }
                                }
                                viewModel.triggerAlarmRinging(
                                    alarmId = 995L,
                                    title = "30-Step Walk Challenge",
                                    wakeMode = mode,
                                    dismissType = DismissType.STEP
                                )
                            },
                            onTestBarcodeAlarm = {
                                val mode = userProfile?.defaultWakeMode ?: WakeMode.FOCUS
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasCam) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    viewModel.triggerAlarmRinging(
                                        alarmId = 994L,
                                        title = "Scan Any Barcode Challenge",
                                        wakeMode = mode,
                                        dismissType = DismissType.QR_CODE
                                    )
                                }
                            },
                            onTestMorningReport = {
                                viewModel.generateMorningReport("Morning Test")
                            },
                            onRestartOnboarding = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("You are on Uthja. Wake-up missions are directly customized in alarms.")
                                }
                            }
                        )
                    }
                }

                // Full Screen Create / Edit Alarm Page
                AnimatedVisibility(
                    visible = isAddEditAlarmOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AlarmEditScreen(
                        initialAlarm = alarmToEdit,
                        onDismiss = {
                            isAddEditAlarmOpen = false
                            alarmToEdit = null
                        },
                        onSave = { savedAlarm ->
                            viewModel.saveAlarm(savedAlarm)
                            isAddEditAlarmOpen = false
                            alarmToEdit = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Alarm saved successfully")
                            }
                        },
                        onDelete = { alarmToDelete ->
                            viewModel.deleteAlarm(alarmToDelete)
                            isAddEditAlarmOpen = false
                            alarmToEdit = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Alarm deleted")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Full Screen Habit Alarm Creator Page
                AnimatedVisibility(
                    visible = isHabitAlarmCreatorOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    HabitAlarmScreen(
                        onClose = {
                            isHabitAlarmCreatorOpen = false
                        },
                        onSaveHabitAlarm = { habitTitle, hour, minute, mission, sound, wallpaper ->
                            viewModel.completeHabitOnboarding(habitTitle, hour, minute, mission, sound, wallpaper)
                            isHabitAlarmCreatorOpen = false
                            scope.launch {
                                val tag = if (com.example.data.model.RingtoneCatalog.isMemeRingtone(sound)) "👾 " else ""
                                snackbarHostState.showSnackbar("Habit alarm scheduled for $habitTitle with $tag$sound")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Full-Screen Ringing Mission Overlay with Wallpaper Display
                var isMissionStarted by remember(activeAlarm.startTimestamp) { mutableStateOf(false) }

                LaunchedEffect(activeAlarm.isConquered, activeAlarm.isRinging) {
                    if (activeAlarm.isConquered || !activeAlarm.isRinging) {
                        isMissionStarted = false
                        viewModel.setMissionFocusMode(false)
                    }
                }

                AnimatedVisibility(
                    visible = activeAlarm.isRinging,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (!isMissionStarted) {
                        WallpaperAlarmRingingScreen(
                            state = activeAlarm,
                            onStartMission = {
                                if (activeAlarm.isQuickAlarm || activeAlarm.currentMission == DismissType.OFF) {
                                    viewModel.conquerAndDismissAlarm()
                                } else {
                                    isMissionStarted = true
                                    viewModel.setMissionFocusMode(true)
                                }
                            },
                            onAttemptExitBlocked = onAttemptExitBlocked,
                            isMuted = isAlarmMuted,
                            onToggleMute = { viewModel.toggleAlarmMute() },
                            onAttemptSnooze = {
                                viewModel.attemptEmergencySnooze(
                                    onBlocked = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Snooze not available")
                                        }
                                    },
                                    onGranted = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Alarm snoozed for 5 minutes")
                                        }
                                    }
                                )
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Active Challenge Screen
                            when (activeAlarm.currentMission) {
                                DismissType.MATH -> {
                                    MathMissionScreen(
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() }
                                    )
                                }
                                DismissType.COLOR_TILES -> {
                                    ColorTilesMissionScreen(
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() }
                                    )
                                }
                                DismissType.TYPING -> {
                                    TypingMissionScreen(
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() }
                                    )
                                }
                                DismissType.STEP -> {
                                    StepMissionScreen(
                                        targetSteps = 30,
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() }
                                    )
                                }
                                DismissType.QR_CODE -> {
                                    QrBarcodeMissionScreen(
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() }
                                    )
                                }
                                DismissType.SHAKE, DismissType.BOTH -> {
                                    ActiveAlarmScreen(
                                        state = activeAlarm,
                                        onDismissConquered = {
                                            viewModel.onMissionCompleted()
                                        },
                                        onAttemptSnooze = {
                                            viewModel.attemptEmergencySnooze(
                                                onBlocked = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Snooze not available")
                                                    }
                                                },
                                                onGranted = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Alarm snoozed for 5 minutes")
                                                    }
                                                }
                                            )
                                        },
                                        isMuted = isAlarmMuted,
                                        isLockedMission = true,
                                        onAttemptExitBlocked = onAttemptExitBlocked,
                                        onToggleMute = { viewModel.toggleAlarmMute() },
                                        onShake = { viewModel.onShakeRegistered() }
                                    )
                                }
                                DismissType.OFF -> {
                                    LaunchedEffect(Unit) {
                                        viewModel.conquerAndDismissAlarm()
                                    }
                                }
                            }

                            // Optional Multi-Mission sequence indicator badge at top if more than 1 mission
                            if (activeAlarm.totalMissions > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .statusBarsPadding()
                                        .padding(top = 8.dp)
                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Mission ${activeAlarm.currentMissionIndex + 1} of ${activeAlarm.totalMissions}: ${activeAlarm.currentMission.title}",
                                        color = Color(0xFFE9D5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cubee Animated Mascot Companion Overlay & Navigation Visibility Flags
            val isSleepTrackingActiveOnTab = currentTab == LuneTab.SLEEP && activeSleep.isTracking
            val isMascotVisible = !activeAlarm.isRinging &&
                    !isHabitAlarmCreatorOpen &&
                    !isAddEditAlarmOpen &&
                    !isSleepTrackingActiveOnTab &&
                    !isSleepGuidanceVisible

            // Floating Navigation Bar overlaying smoothly on the full-screen canvas
            if (!activeAlarm.isRinging && !isHabitAlarmCreatorOpen && !isAddEditAlarmOpen && !isSleepTrackingActiveOnTab && !isSleepGuidanceVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    LuneBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }

            // Floating Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )

            // Cubee Mascot View anchored at bottom-start (left edge) to guarantee it never blocks
            // the primary action buttons (Add Alarm / Add Task FAB) located at bottom-end.
            CubeeMascotView(
                isVisible = isMascotVisible,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 96.dp)
            )
        }
}
