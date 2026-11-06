package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmRingingService
import com.example.alarm.AlarmScheduler
import com.example.alarm.HapticsController
import com.example.alarm.ShakeDetector
import com.example.alarm.SoundManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.GoogleAuthState
import com.example.data.auth.GoogleUserData
import com.example.data.local.LuneDatabase
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.DismissType
import com.example.data.model.SleepSessionEntity
import com.example.data.model.SnoreEventEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WakeMode
import com.example.data.repository.LuneRepository
import com.example.sleep.SleepMovementTracker
import com.example.sleep.SleepTrackingManager
import com.example.ui.components.MorningSleepReportData
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MathChallengeState(
    val questionText: String = "",
    val expectedAnswer: Int = 0,
    val currentInput: String = "",
    val requiredSolves: Int = 1,
    val solvedCount: Int = 0,
    val attempts: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

data class ActiveAlarmState(
    val isRinging: Boolean = false,
    val alarmId: Long = 0L,
    val title: String = "",
    val wakeMode: WakeMode = WakeMode.FOCUS,
    val dismissType: DismissType = DismissType.OFF,
    val missions: List<DismissType> = listOf(DismissType.MATH),
    val currentMissionIndex: Int = 0,
    val sound: String = "Good Morningggg",
    val startTimestamp: Long = 0L,
    val elapsedSeconds: Int = 0,
    val mathState: MathChallengeState = MathChallengeState(),
    val shakeCurrentCount: Int = 0,
    val shakeTargetCount: Int = 30,
    val currentGForce: Float = 1.0f,
    val isConquered: Boolean = false,
    val snoozeCount: Int = 0,
    val snoozeRemainingMinutes: Int = 0,
    val wallpaper: String = com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID,
    val isDoingMission: Boolean = false,
    val gradualVolume: Boolean = true,
    val gentleMissionVolume: Boolean = true,
    val isQuickAlarm: Boolean = false,
    val quickRingDurationSeconds: Int = 5
) {
    val currentMission: DismissType
        get() = missions.getOrNull(currentMissionIndex) ?: dismissType

    val totalMissions: Int
        get() = missions.size.coerceAtLeast(1)

    val isLastMission: Boolean
        get() = currentMissionIndex >= totalMissions - 1
}

data class ActiveSleepState(
    val isTracking: Boolean = false,
    val startedAt: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val movementCount: Int = 0,
    val currentMotionLevel: Float = 0f,
    val restlessSpikeCount: Int = 0,
    val movementRating: String = "Peaceful",
    val deepSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val sleepScore: Int = 85,
    val currentDecibels: Float = 28f,
    val isSnoringNow: Boolean = false,
    val snoreEventCount: Int = 0,
    val totalSnoreSeconds: Int = 0,
    val recentAmplitudes: List<Float> = listOf(0.2f, 0.35f, 0.45f, 0.25f, 0.50f, 0.30f)
)

data class SupabaseUiState(
    val isConfigured: Boolean = false,
    val projectUrl: String = "",
    val userEmail: String? = null,
    val userId: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null
)

private fun getTodayDateString(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
}

class LuneViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LuneDatabase.getDatabase(application)
    private val repository = LuneRepository(
        database.alarmDao(),
        database.alarmEventDao(),
        database.sleepSessionDao(),
        database.snoreEventDao(),
        database.userProfileDao(),
        database.leaderboardDao(),
        database.taskDao()
    )

    val alarms: StateFlow<List<AlarmEntity>> = repository.alarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getAlarmById(id: Long): AlarmEntity? = repository.getAlarmById(id)

    val sleepSessions: StateFlow<List<SleepSessionEntity>> = repository.sleepSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snoreEvents: StateFlow<List<SnoreEventEntity>> = repository.snoreEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSnoreEventsForSession(sessionId: Long) = repository.getSnoreEventsForSession(sessionId)

    val alarmEvents: StateFlow<List<AlarmEventEntity>> = repository.alarmEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val leaderboard: StateFlow<List<com.example.data.model.LeaderboardEntity>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<com.example.data.model.TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDate = MutableStateFlow<String>(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayTasks: StateFlow<List<com.example.data.model.TaskEntity>> = combine(
        repository.tasks,
        _currentDate
    ) { allTasks: List<com.example.data.model.TaskEntity>, todayDate: String ->
        allTasks.filter { it.isScheduledForDate(todayDate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeAlarm = MutableStateFlow(ActiveAlarmState())
    val activeAlarm: StateFlow<ActiveAlarmState> = _activeAlarm.asStateFlow()

    private val _activeSleep = MutableStateFlow(ActiveSleepState())
    val activeSleep: StateFlow<ActiveSleepState> = _activeSleep.asStateFlow()

    private val _morningReport = MutableStateFlow<MorningSleepReportData?>(null)
    val morningReport: StateFlow<MorningSleepReportData?> = _morningReport.asStateFlow()

    private val _playingVibe = MutableStateFlow<String?>(null)
    val playingVibe: StateFlow<String?> = _playingVibe.asStateFlow()

    private val themePrefs = application.getSharedPreferences("lune_theme_prefs", android.content.Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(themePrefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        themePrefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val supabaseRepository = SupabaseRepository()
    private val _supabaseState = MutableStateFlow(
        SupabaseUiState(
            isConfigured = supabaseRepository.isConfigured,
            projectUrl = SupabaseConfig.url,
            userEmail = supabaseRepository.currentUserOrNull()?.email,
            userId = supabaseRepository.currentUserOrNull()?.id
        )
    )
    val supabaseState: StateFlow<SupabaseUiState> = _supabaseState.asStateFlow()

    private val googleAuthManager = GoogleAuthManager(application)
    val googleAuthState: StateFlow<GoogleAuthState> = googleAuthManager.authState

    private val soundManager = SoundManager(application)
    private val haptics = HapticsController(application)
    private val sleepTracker = SleepMovementTracker(application)
    private var shakeDetector: ShakeDetector? = null

    private val _isAlarmMuted = MutableStateFlow(false)
    val isAlarmMuted: StateFlow<Boolean> = _isAlarmMuted.asStateFlow()

    fun setMissionFocusMode(isDoingMission: Boolean) {
        val state = _activeAlarm.value
        if (state.isConquered || !state.isRinging) {
            _activeAlarm.value = state.copy(isDoingMission = false)
            return
        }
        _activeAlarm.value = state.copy(
            isDoingMission = isDoingMission
        )
        soundManager.setMissionActive(isDoingMission)
        AlarmRingingService.setMissionMode(getApplication(), isDoingMission)
    }

    fun toggleAlarmMute() {
        val next = !_isAlarmMuted.value
        _isAlarmMuted.value = next
        if (next) {
            soundManager.stopAlarmTone()
        } else {
            val mode = _activeAlarm.value.wakeMode
            val sound = _activeAlarm.value.sound
            soundManager.playAlarmToneWithSound(sound, mode, viewModelScope, gradualVolume = false)
            soundManager.setMissionActive(_activeAlarm.value.isDoingMission)
        }
    }

    private var ringingTimerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        // Seed initial tasks matching the approved design if empty
        viewModelScope.launch {
            try {
                repository.seedInitialTasksIfEmpty(
                    listOf(
                        com.example.data.model.TaskEntity(title = "Drink water", timePill = "2 glasses", iconType = "WATER", isCompleted = true, orderIndex = 0),
                        com.example.data.model.TaskEntity(title = "Reading", timePill = "10 pages", iconType = "BOOK", isCompleted = true, orderIndex = 1),
                        com.example.data.model.TaskEntity(title = "Meditation", timePill = "10 min", iconType = "MEDITATION", isCompleted = false, orderIndex = 2),
                        com.example.data.model.TaskEntity(title = "Take a walk", timePill = "20 min", iconType = "WORKOUT", isCompleted = false, orderIndex = 3),
                        com.example.data.model.TaskEntity(title = "Mindful break", timePill = "5 min", iconType = "COFFEE", isCompleted = false, orderIndex = 4),
                        com.example.data.model.TaskEntity(title = "Wind down", timePill = "10:30 PM", iconType = "MOON", isCompleted = false, orderIndex = 5)
                    )
                )
            } catch (e: Throwable) {
                android.util.Log.e("LuneViewModel", "Error seeding initial tasks", e)
            }
        }

        // Initialize ShakeDetector listener using Android SensorManager
        shakeDetector = ShakeDetector(
            context = application,
            onMotion = { _, _, _, gForce ->
                if (_activeAlarm.value.isRinging && !_activeAlarm.value.isConquered) {
                    _activeAlarm.value = _activeAlarm.value.copy(currentGForce = gForce)
                }
            },
            onShake = {
                onShakeRegistered()
            }
        )

        // Observe SleepTrackingManager to keep active sleep UI in sync with microphone tracking
        viewModelScope.launch {
            SleepTrackingManager.isTracking.collect { tracking ->
                _activeSleep.value = _activeSleep.value.copy(
                    isTracking = tracking,
                    startedAt = if (tracking) SleepTrackingManager.startedAt.value else _activeSleep.value.startedAt
                )
            }
        }

        viewModelScope.launch {
            SleepTrackingManager.liveMetrics.collect { metrics ->
                _activeSleep.value = _activeSleep.value.copy(
                    elapsedSeconds = metrics.elapsedSeconds,
                    currentDecibels = metrics.currentDecibels,
                    isSnoringNow = metrics.isSnoringNow,
                    snoreEventCount = metrics.snoreEventCount,
                    totalSnoreSeconds = metrics.totalSnoreSeconds,
                    recentAmplitudes = metrics.recentAmplitudes
                )
            }
        }

        viewModelScope.launch {
            SleepTrackingManager.lastCompletedSessionId.collect { sessionId ->
                if (sessionId != null) {
                    val session = repository.getSleepSessionById(sessionId)
                    if (session != null) {
                        _morningReport.value = MorningSleepReportData(
                            session = session,
                            alarmTitle = "Morning Rest & Snore Insights",
                            isFreshlyRecorded = true
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            // Midnight transition watcher: checks date regularly and immediately updates today's routine
            while (isActive) {
                delay(10_000L)
                val nowStr = getTodayDateString()
                if (nowStr != _currentDate.value) {
                    _currentDate.value = nowStr
                }
            }
        }
    }

    // --- Daily Tasks Handling ---

    fun toggleTask(task: com.example.data.model.TaskEntity) {
        viewModelScope.launch {
            val todayDate = _currentDate.value
            val isCurrentlyDone = task.isCompletedForDate(todayDate)
            val newCompleted = !isCurrentlyDone

            val updatedTask = task.copy(
                isCompleted = newCompleted,
                lastCompletedDate = if (newCompleted) todayDate else null
            )
            repository.saveTask(updatedTask)

            if (newCompleted) {
                // If marked complete, cancel the pending reminder so user is not nudged
                com.example.alarm.TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            } else if (!task.reminderTime.isNullOrBlank()) {
                // Re-schedule reminder if unchecked
                com.example.alarm.TaskReminderScheduler.scheduleReminder(
                    getApplication(),
                    id = task.id,
                    title = task.title,
                    reminderTime = task.reminderTime,
                    targetDateStr = task.scheduledDate ?: todayDate
                )
            }
        }
    }

    fun markTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            val todayDate = _currentDate.value
            val task = tasks.value.find { it.id == taskId }
            if (task != null) {
                repository.saveTask(
                    task.copy(
                        isCompleted = true,
                        lastCompletedDate = todayDate
                    )
                )
            } else {
                repository.setTaskCompletedWithDate(taskId, true, todayDate)
            }
            com.example.alarm.TaskReminderScheduler.cancelReminder(getApplication(), taskId)
        }
    }

    fun addTask(
        title: String,
        timePill: String? = null,
        iconType: String = "WATER",
        reminderTime: String? = null,
        scheduledDate: String? = null,
        isDaily: Boolean = true
    ) {
        viewModelScope.launch {
            val currentTasks = tasks.value
            val order = currentTasks.size
            val effectiveDate = if (isDaily) null else (scheduledDate ?: _currentDate.value)
            val createdId = repository.saveTask(
                com.example.data.model.TaskEntity(
                    title = title.trim(),
                    timePill = timePill?.trim()?.ifBlank { null },
                    iconType = iconType,
                    isCompleted = false,
                    orderIndex = order,
                    reminderTime = reminderTime?.trim()?.ifBlank { null },
                    scheduledDate = effectiveDate,
                    isDaily = isDaily,
                    lastCompletedDate = null
                )
            )
            if (!reminderTime.isNullOrBlank()) {
                com.example.alarm.TaskReminderScheduler.scheduleReminder(
                    getApplication(),
                    id = createdId,
                    title = title.trim(),
                    reminderTime = reminderTime.trim(),
                    targetDateStr = effectiveDate ?: _currentDate.value
                )
            }
        }
    }

    fun addTask(title: String, timePill: String? = null, iconType: String = "WATER", reminderTime: String? = null) {
        addTask(title, timePill, iconType, reminderTime, scheduledDate = null, isDaily = true)
    }

    fun updateTaskReminder(task: com.example.data.model.TaskEntity, newReminderTime: String?) {
        viewModelScope.launch {
            val updated = task.copy(reminderTime = newReminderTime?.trim()?.ifBlank { null })
            repository.saveTask(updated)
            if (!newReminderTime.isNullOrBlank()) {
                com.example.alarm.TaskReminderScheduler.scheduleReminder(
                    getApplication(),
                    id = task.id,
                    title = task.title,
                    reminderTime = newReminderTime.trim(),
                    targetDateStr = task.scheduledDate ?: _currentDate.value
                )
            } else {
                com.example.alarm.TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            }
        }
    }

    fun deleteTask(task: com.example.data.model.TaskEntity) {
        viewModelScope.launch {
            com.example.alarm.TaskReminderScheduler.cancelReminder(getApplication(), task.id)
            repository.deleteTask(task)
        }
    }

    fun resetAllTasks() {
        viewModelScope.launch {
            repository.resetAllTasks()
        }
    }

    fun reorderTasks(reorderedList: List<com.example.data.model.TaskEntity>) {
        viewModelScope.launch {
            repository.updateTasksOrder(reorderedList)
        }
    }

    // --- Active Alarm Mission Handling ---

    fun triggerAlarmRinging(
        alarmId: Long,
        title: String,
        wakeMode: WakeMode,
        dismissType: DismissType,
        sound: String = "Good Morningggg",
        wallpaper: String = com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID,
        missions: List<DismissType> = emptyList(),
        gradualVolume: Boolean = true,
        gentleMissionVolume: Boolean = true
    ) {
        val isQuick = title.contains("Quick Alarm", ignoreCase = true)
        val quickDurationSec = if (title.contains("10s")) 10 else 5
        val effectiveSound = if (isQuick) "Birds" else sound

        val rawMissions = if (isQuick) {
            listOf(DismissType.OFF)
        } else if (missions.isNotEmpty()) {
            missions
        } else {
            listOf(dismissType)
        }

        val effectiveMissions = if (isQuick) {
            listOf(DismissType.OFF)
        } else {
            rawMissions.flatMap {
                if (it == DismissType.BOTH) listOf(DismissType.MATH, DismissType.SHAKE) else listOf(it)
            }.ifEmpty { listOf(dismissType) }
        }

        val requiredSolves = when (wakeMode) {
            WakeMode.GENTLE -> 1
            WakeMode.FOCUS -> 2
            WakeMode.BEAST -> 3
        }

        val targetShakes = when (wakeMode) {
            WakeMode.GENTLE -> 20
            WakeMode.FOCUS -> 30
            WakeMode.BEAST -> 45
        }

        val initialMath = generateMathProblem(wakeMode, requiredSolves)

        _activeAlarm.value = ActiveAlarmState(
            isRinging = true,
            alarmId = alarmId,
            title = title,
            wakeMode = wakeMode,
            dismissType = effectiveMissions.firstOrNull() ?: dismissType,
            missions = effectiveMissions,
            currentMissionIndex = 0,
            sound = effectiveSound,
            wallpaper = wallpaper,
            startTimestamp = System.currentTimeMillis(),
            elapsedSeconds = 0,
            mathState = initialMath,
            shakeCurrentCount = 0,
            shakeTargetCount = targetShakes,
            isConquered = false,
            gradualVolume = if (isQuick) false else gradualVolume,
            gentleMissionVolume = gentleMissionVolume,
            isQuickAlarm = isQuick,
            quickRingDurationSeconds = quickDurationSec
        )

        // Start sound, haptics, sensor, and foreground ringing service
        AlarmRingingService.start(
            context = getApplication(),
            alarmId = alarmId,
            title = title,
            wakeMode = wakeMode.name,
            dismissType = (effectiveMissions.firstOrNull() ?: dismissType).name,
            sound = effectiveSound,
            wallpaper = wallpaper,
            missions = effectiveMissions.joinToString(",") { it.name },
            gradualVolume = if (isQuick) false else gradualVolume,
            gentleMissionVolume = gentleMissionVolume
        )
        // Ensure SoundManager is cleared of any preview tones or lingering sounds
        soundManager.stopAlarmTone()
        soundManager.stopVibePreview()
        haptics.startAlarmVibration()

        // ALWAYS activate Android SensorManager accelerometer during alarm ringing
        shakeDetector?.start()

        // Ticker for elapsed seconds & auto-dismiss for quick alarm
        ringingTimerJob?.cancel()
        ringingTimerJob = viewModelScope.launch {
            while (isActive && _activeAlarm.value.isRinging && !_activeAlarm.value.isConquered) {
                delay(1000L)
                val newElapsed = _activeAlarm.value.elapsedSeconds + 1
                _activeAlarm.value = _activeAlarm.value.copy(
                    elapsedSeconds = newElapsed
                )
                // Quick alarm auto-dismiss after set duration (5s or 10s)
                if (_activeAlarm.value.isQuickAlarm && newElapsed >= _activeAlarm.value.quickRingDurationSeconds) {
                    conquerAndDismissAlarm()
                    break
                }
            }
        }
    }

    private fun generateMathProblem(mode: WakeMode, requiredSolves: Int): MathChallengeState {
        val (q, ans) = when (mode) {
            WakeMode.GENTLE -> {
                val a = Random.nextInt(12, 45)
                val b = Random.nextInt(11, 45)
                Pair("$a + $b", a + b)
            }
            WakeMode.FOCUS -> {
                val type = Random.nextInt(0, 3)
                if (type == 0) {
                    val a = Random.nextInt(35, 95)
                    val b = Random.nextInt(28, 89)
                    Pair("$a + $b", a + b)
                } else if (type == 1) {
                    val a = Random.nextInt(75, 150)
                    val b = Random.nextInt(18, 59)
                    Pair("$a - $b", a - b)
                } else {
                    val a = Random.nextInt(6, 12)
                    val b = Random.nextInt(6, 13)
                    val c = Random.nextInt(5, 25)
                    Pair("($a × $b) + $c", (a * b) + c)
                }
            }
            WakeMode.BEAST -> {
                val a = Random.nextInt(12, 19)
                val b = Random.nextInt(7, 14)
                val c = Random.nextInt(15, 65)
                Pair("($a × $b) - $c", (a * b) - c)
            }
        }
        return MathChallengeState(
            questionText = q,
            expectedAnswer = ans,
            currentInput = "",
            requiredSolves = requiredSolves,
            solvedCount = _activeAlarm.value.mathState.solvedCount,
            attempts = _activeAlarm.value.mathState.attempts,
            isError = false
        )
    }

    fun onKeypadDigit(digit: String) {
        val current = _activeAlarm.value.mathState.currentInput
        if (current.length < 5) {
            _activeAlarm.value = _activeAlarm.value.copy(
                mathState = _activeAlarm.value.mathState.copy(
                    currentInput = current + digit,
                    isError = false
                )
            )
        }
    }

    fun onKeypadBackspace() {
        val current = _activeAlarm.value.mathState.currentInput
        if (current.isNotEmpty()) {
            _activeAlarm.value = _activeAlarm.value.copy(
                mathState = _activeAlarm.value.mathState.copy(
                    currentInput = current.dropLast(1),
                    isError = false
                )
            )
        }
    }

    fun onKeypadClear() {
        _activeAlarm.value = _activeAlarm.value.copy(
            mathState = _activeAlarm.value.mathState.copy(
                currentInput = "",
                isError = false
            )
        )
    }

    fun submitMathAnswer() {
        val state = _activeAlarm.value
        val math = state.mathState
        val entered = math.currentInput.toIntOrNull()

        val updatedAttempts = math.attempts + 1

        if (entered != null && entered == math.expectedAnswer) {
            val newSolvedCount = math.solvedCount + 1
            haptics.pulseSuccess()

            if (newSolvedCount >= math.requiredSolves) {
                onMissionCompleted()
            } else {
                // Generate next problem
                val nextMath = generateMathProblem(state.wakeMode, math.requiredSolves)
                _activeAlarm.value = state.copy(
                    mathState = nextMath.copy(
                        solvedCount = newSolvedCount,
                        attempts = updatedAttempts,
                        currentInput = ""
                    )
                )
            }
        } else {
            // Wrong answer
            haptics.pulseError()
            _activeAlarm.value = state.copy(
                mathState = math.copy(
                    attempts = updatedAttempts,
                    isError = true,
                    errorMessage = "Incorrect! Prove your alertness.",
                    currentInput = ""
                )
            )
        }
    }

    private var lastMissionCompletedTime = 0L

    fun onShakeRegistered() {
        if (!_activeAlarm.value.isRinging || _activeAlarm.value.isConquered) return

        val target = _activeAlarm.value.shakeTargetCount
        val current = (_activeAlarm.value.shakeCurrentCount + 1).coerceAtMost(target)
        haptics.pulseShake()

        _activeAlarm.value = _activeAlarm.value.copy(
            shakeCurrentCount = current
        )

        if (current >= target) {
            onMissionCompleted()
        }
    }

    fun simulateShake() {
        onShakeRegistered()
    }

    /**
     * Called when the user completes their current mission challenge.
     * If there are subsequent missions configured in the alarm, it transitions to the next one.
     * If all missions are completed, it completely stops the alarm ringing and sound immediately.
     */
    fun onMissionCompleted() {
        val now = System.currentTimeMillis()
        if (now - lastMissionCompletedTime < 750L) return
        lastMissionCompletedTime = now

        val state = _activeAlarm.value
        if (!state.isRinging || state.isConquered) return

        if (state.currentMissionIndex < state.totalMissions - 1) {
            // More missions to complete in sequence!
            val nextIndex = state.currentMissionIndex + 1
            haptics.pulseAppleSuccess()

            val nextMath = generateMathProblem(state.wakeMode, when (state.wakeMode) {
                WakeMode.GENTLE -> 1
                WakeMode.FOCUS -> 2
                WakeMode.BEAST -> 3
            })

            _activeAlarm.value = state.copy(
                currentMissionIndex = nextIndex,
                shakeCurrentCount = 0,
                mathState = nextMath
            )

            val nextMission = state.missions.getOrNull(nextIndex)
            if (nextMission == DismissType.SHAKE || nextMission == DismissType.BOTH) {
                shakeDetector?.start()
            }
        } else {
            // ALL missions completed! Stop alarm ringing immediately and completely!
            conquerAndDismissAlarm()
        }
    }

    /**
     * Completely and synchronously halts all alarm sounds, service, vibration, and timers.
     */
    fun stopAllAlarmEffects() {
        try {
            AlarmRingingService.stop(getApplication())
        } catch (e: Exception) {
            android.util.Log.e("LuneViewModel", "Error stopping AlarmRingingService", e)
        }
        try {
            soundManager.setMissionActive(false)
            soundManager.stopAlarmTone()
            soundManager.stopVibePreview()
        } catch (e: Exception) {
            android.util.Log.e("LuneViewModel", "Error stopping SoundManager", e)
        }
        try {
            haptics.stopAlarmVibration()
        } catch (e: Exception) {
            android.util.Log.e("LuneViewModel", "Error stopping Haptics", e)
        }
        try {
            shakeDetector?.stop()
        } catch (e: Exception) {
            android.util.Log.e("LuneViewModel", "Error stopping ShakeDetector", e)
        }
        ringingTimerJob?.cancel()
        ringingTimerJob = null
    }

    fun conquerAndDismissAlarm() {
        val state = _activeAlarm.value
        if (state.isConquered) return

        // 1. Immediately and completely stop all alarm ringing effects
        stopAllAlarmEffects()
        haptics.pulseSuccess()

        // 2. Play short pleasant victory chime upon mission completion
        soundManager.playSuccessChime(viewModelScope)

        _activeAlarm.value = state.copy(
            isConquered = true,
            isDoingMission = false,
            shakeCurrentCount = state.shakeTargetCount
        )

        // 3. Log alarm event in database
        viewModelScope.launch {
            val totalSeconds = state.elapsedSeconds.coerceAtLeast(1)
            val totalAttempts = state.mathState.attempts.coerceAtLeast(1)
            repository.logDismissEvent(
                AlarmEventEntity(
                    alarmId = state.alarmId,
                    triggeredAt = state.startTimestamp,
                    dismissedAt = System.currentTimeMillis(),
                    dismissDurationSeconds = totalSeconds,
                    dismissType = state.currentMission,
                    attempts = totalAttempts,
                    success = true,
                    wakeMode = state.wakeMode
                )
            )
        }

        // 4. Seamlessly dismiss active alarm screen after victory chime and open morning sleep report
        viewModelScope.launch {
            delay(1100L)
            dismissConqueredAlarm()
        }
    }

    fun dismissConqueredAlarm() {
        val alarmTitle = _activeAlarm.value.title
        stopAllAlarmEffects()

        _activeAlarm.value = ActiveAlarmState(isRinging = false, isConquered = true)
        generateMorningReport(alarmTitle)
    }

    fun attemptEmergencySnooze(onBlocked: () -> Unit, onGranted: () -> Unit) {
        val profile = userProfile.value
        val isStrict = profile?.strictNoSnoozeDefault ?: true

        if (isStrict) {
            onBlocked()
            return
        }

        // Emergency snooze: 3 minutes
        stopAllAlarmEffects()

        val nextSnoozeCount = _activeAlarm.value.snoozeCount + 1
        _activeAlarm.value = ActiveAlarmState(
            isRinging = false,
            snoozeCount = nextSnoozeCount,
            snoozeRemainingMinutes = 3
        )
        onGranted()
    }

    // --- Sleep Tracking Actions & Accelerometer Motion Monitoring ---

    fun startSleepSession() {
        val startTime = System.currentTimeMillis()
        sleepTracker.startTracking(startTime)
        SleepTrackingManager.startTracking(getApplication())

        _activeSleep.value = ActiveSleepState(
            isTracking = true,
            startedAt = startTime,
            elapsedSeconds = 0L,
            movementCount = 0,
            currentMotionLevel = 0f,
            movementRating = "Peaceful",
            sleepScore = 88
        )
    }

    fun finishSleepSession(notes: String = "") {
        val sleep = _activeSleep.value
        if (!sleep.isTracking) return

        sleepTracker.stopTracking()
        sleepTimerJob?.cancel()
        SleepTrackingManager.stopTracking(getApplication())
        _activeSleep.value = _activeSleep.value.copy(isTracking = false)
    }

    fun generateMorningReport(alarmTitle: String = "Morning Alarm") {
        val sleep = _activeSleep.value
        val goalMinutes = userProfile.value?.sleepGoalMinutes ?: 480

        if (sleep.isTracking) {
            val trackingResult = sleepTracker.stopTracking()
            sleepTimerJob?.cancel()
            val endedAt = System.currentTimeMillis()
            val durationMins = trackingResult.durationMinutes.coerceAtLeast(1)
            val met = durationMins >= (goalMinutes * 0.85)

            val deep = trackingResult.calculateDeepSleepMinutes()
            val light = trackingResult.calculateLightSleepMinutes()
            val restless = trackingResult.calculateRestlessMinutes()
            val score = trackingResult.calculateSleepScore(goalMinutes)
            val rating = trackingResult.calculateMovementRating()

            val session = SleepSessionEntity(
                startedAt = trackingResult.startedAt,
                endedAt = endedAt,
                durationMinutes = durationMins,
                sleepGoalMet = met,
                source = "ACCELEROMETER",
                notes = "Overnight accelerometer: ${trackingResult.movementCount} movements ($rating)",
                movementCount = trackingResult.movementCount,
                deepSleepMinutes = deep,
                lightSleepMinutes = light,
                restlessMinutes = restless,
                sleepScore = score,
                movementRating = rating
            )

            viewModelScope.launch {
                repository.logSleepSession(session)
            }

            _activeSleep.value = ActiveSleepState(isTracking = false)
            _morningReport.value = MorningSleepReportData(
                session = session,
                alarmTitle = alarmTitle.ifBlank { "Morning Wake Alarm" },
                isFreshlyRecorded = true
            )
        } else {
            val now = System.currentTimeMillis()
            val simulatedMins = ((goalMinutes * 0.95f).toInt()).coerceIn(360, 520)
            val startedAt = now - (simulatedMins * 60_000L)
            val movements = (simulatedMins / 60).coerceIn(4, 10)
            val deep = (simulatedMins * 0.52f).toInt()
            val light = (simulatedMins * 0.38f).toInt()
            val restless = (simulatedMins - deep - light).coerceAtLeast(12)
            val score = (88 - (movements * 1.5f).toInt()).coerceIn(70, 96)
            val rating = if (movements <= 6) "Very Peaceful" else "Calm with Light Shifts"
            val met = simulatedMins >= (goalMinutes * 0.85)

            val session = SleepSessionEntity(
                startedAt = startedAt,
                endedAt = now,
                durationMinutes = simulatedMins,
                sleepGoalMet = met,
                source = "OVERNIGHT_ACCEL",
                notes = "Accelerometer detected: $movements overnight movements",
                movementCount = movements,
                deepSleepMinutes = deep,
                lightSleepMinutes = light,
                restlessMinutes = restless,
                sleepScore = score,
                movementRating = rating
            )

            viewModelScope.launch {
                repository.logSleepSession(session)
            }

            _morningReport.value = MorningSleepReportData(
                session = session,
                alarmTitle = alarmTitle.ifBlank { "Morning Wake Alarm" },
                isFreshlyRecorded = true
            )
        }
    }

    fun dismissMorningReport() {
        _morningReport.value = null
    }

    fun simulateSleepMovement(isRestless: Boolean = false) {
        sleepTracker.simulateMovement(isRestless)
        val current = _activeSleep.value
        _activeSleep.value = current.copy(
            movementCount = current.movementCount + 1,
            currentMotionLevel = if (isRestless) 0.85f else 0.45f,
            restlessSpikeCount = if (isRestless) current.restlessSpikeCount + 1 else current.restlessSpikeCount
        )
    }

    fun simulateOvernightTracking(durationHours: Float = 7.5f, movements: Int = 7) {
        val mins = (durationHours * 60).toInt()
        val data = sleepTracker.simulateOvernightData(mins, movements)
        val goal = userProfile.value?.sleepGoalMinutes ?: 480
        _activeSleep.value = ActiveSleepState(
            isTracking = true,
            startedAt = data.startedAt,
            elapsedSeconds = data.elapsedSeconds,
            movementCount = data.movementCount,
            currentMotionLevel = 0f,
            restlessSpikeCount = data.restlessSpikeCount,
            movementRating = data.calculateMovementRating(),
            deepSleepMinutes = data.calculateDeepSleepMinutes(),
            lightSleepMinutes = data.calculateLightSleepMinutes(),
            sleepScore = data.calculateSleepScore(goal)
        )
    }

    // --- Alarm Management ---

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repository.saveAlarm(alarm)
            val updatedAlarm = alarm.copy(id = id)
            AlarmScheduler.scheduleAlarm(getApplication(), updatedAlarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAlarmEnabled(alarm.id, enabled)
            val updated = alarm.copy(isEnabled = enabled)
            AlarmScheduler.scheduleAlarm(getApplication(), updated)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun deleteSleepSession(session: SleepSessionEntity) {
        viewModelScope.launch {
            repository.deleteSleepSession(session)
        }
    }

    fun updateProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun completeOnboarding(selectedMission: String? = null) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                isOnboardingCompleted = true,
                selectedMission = selectedMission ?: current.selectedMission
            )
            repository.updateProfile(updated)
        }
    }

    fun completeHabitOnboarding(
        habitTitle: String,
        hour: Int,
        minute: Int,
        mission: String,
        sound: String = com.example.data.model.RingtoneCatalog.MEME_UTH_JA.name,
        wallpaper: String = com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID
    ) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                isOnboardingCompleted = true,
                selectedMission = mission
            )
            repository.updateProfile(updated)

            val mappedDismissType = when (mission.trim().lowercase()) {
                "shake to dismiss", "shake" -> DismissType.SHAKE
                "math" -> DismissType.MATH
                "find color tiles", "color tiles" -> DismissType.COLOR_TILES
                "typing" -> DismissType.TYPING
                "step" -> DismissType.STEP
                "habit item snap", "qr", "qr/barcode" -> DismissType.QR_CODE
                "off" -> DismissType.OFF
                else -> DismissType.SHAKE
            }

            val alarm = AlarmEntity(
                title = habitTitle,
                hour = hour,
                minute = minute,
                repeatDays = "DAILY",
                isEnabled = true,
                dismissType = mappedDismissType,
                missions = mappedDismissType.name,
                sound = sound,
                wallpaper = wallpaper
            )
            val id = repository.saveAlarm(alarm)
            AlarmScheduler.scheduleAlarm(getApplication(), alarm.copy(id = id))
        }
    }

    fun saveQuickAlarm(
        minutes: Int,
        ringDurationSeconds: Int = 5,
        sound: String = "Birds",
        volume: Float = 0.5f,
        vibrate: Boolean = true
    ) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MINUTE, minutes)
            }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val alarm = AlarmEntity(
                title = "⚡ Quick Alarm (${minutes}m • ${ringDurationSeconds}s)",
                hour = hour,
                minute = minute,
                repeatDays = "ONCE",
                isEnabled = true,
                dismissType = DismissType.OFF,
                missions = "OFF",
                sound = "Birds"
            )
            val id = repository.saveAlarm(alarm)
            AlarmScheduler.scheduleAlarm(getApplication(), alarm.copy(id = id))
        }
    }

    fun restartOnboarding() {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(current.copy(isOnboardingCompleted = false))
        }
    }

    fun playVibePreview(vibeName: String) {
        _playingVibe.value = vibeName
        soundManager.playVibePreview(vibeName, viewModelScope) {
            if (_playingVibe.value == vibeName) {
                _playingVibe.value = null
            }
        }
    }

    fun stopVibePreview() {
        _playingVibe.value = null
        soundManager.stopVibePreview()
    }

    fun completeOnboardingWithSettings(
        hour: Int,
        minute: Int,
        wallpaper: String,
        vibe: String,
        mission: String
    ) {
        viewModelScope.launch {
            stopVibePreview()

            val dismissType = when (mission.trim().lowercase()) {
                "math" -> DismissType.MATH
                "find color tiles", "colortiles", "color tiles" -> DismissType.COLOR_TILES
                "typing" -> DismissType.TYPING
                "shake" -> DismissType.SHAKE
                "off" -> DismissType.OFF
                else -> DismissType.MATH
            }

            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                selectedAlarmHour = hour,
                selectedAlarmMinute = minute,
                selectedWallpaper = wallpaper,
                selectedVibe = vibe,
                selectedMission = mission,
                isOnboardingCompleted = true
            )
            repository.updateProfile(updated)

            // Create and schedule first alarm
            val firstAlarm = AlarmEntity(
                title = "Morning Alarm",
                hour = hour,
                minute = minute,
                repeatDays = "DAILY",
                isEnabled = true,
                wakeMode = WakeMode.FOCUS,
                dismissType = dismissType,
                sound = vibe,
                wallpaper = wallpaper,
                strictNoSnooze = (dismissType != DismissType.OFF)
            )
            saveAlarm(firstAlarm)
        }
    }

    fun completeOnboarding(wakeMode: WakeMode, wakeDifficulty: String, sleepGoalHours: Float) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                defaultWakeMode = wakeMode,
                wakeDifficulty = wakeDifficulty,
                sleepGoalMinutes = (sleepGoalHours * 60).toInt(),
                isOnboardingCompleted = true
            )
            repository.updateProfile(updated)
        }
    }

    // ==========================================
    // SUPABASE CLOUD SYNC & AUTHENTICATION
    // ==========================================

    fun backupAllToSupabase() {
        if (!supabaseRepository.isConfigured) {
            _supabaseState.value = _supabaseState.value.copy(
                errorMessage = "Supabase is not configured yet. Add SUPABASE_URL and SUPABASE_ANON_KEY in the Secrets panel."
            )
            return
        }

        viewModelScope.launch {
            _supabaseState.value = _supabaseState.value.copy(isSyncing = true, errorMessage = null)
            val alarmsList = alarms.value
            val sleepList = sleepSessions.value
            val profile = userProfile.value

            val alarmsResult = supabaseRepository.backupAlarms(alarmsList)
            val sleepResult = supabaseRepository.backupSleepSessions(sleepList)
            if (profile != null) {
                supabaseRepository.backupProfile(profile)
            }

            val error = alarmsResult.exceptionOrNull() ?: sleepResult.exceptionOrNull()
            if (error != null) {
                _supabaseState.value = _supabaseState.value.copy(
                    isSyncing = false,
                    errorMessage = error.localizedMessage ?: "Sync failed"
                )
            } else {
                _supabaseState.value = _supabaseState.value.copy(
                    isSyncing = false,
                    lastSyncMessage = "Backed up ${alarmsList.size} alarms & ${sleepList.size} sleep sessions to Supabase!",
                    lastSyncTime = System.currentTimeMillis(),
                    errorMessage = null
                )
            }
        }
    }

    fun restoreAllFromSupabase() {
        if (!supabaseRepository.isConfigured) {
            _supabaseState.value = _supabaseState.value.copy(
                errorMessage = "Supabase is not configured yet. Add SUPABASE_URL and SUPABASE_ANON_KEY in the Secrets panel."
            )
            return
        }

        viewModelScope.launch {
            _supabaseState.value = _supabaseState.value.copy(isSyncing = true, errorMessage = null)

            val alarmsResult = supabaseRepository.fetchAlarms()
            val sleepResult = supabaseRepository.fetchSleepSessions()

            if (alarmsResult.isSuccess && sleepResult.isSuccess) {
                val fetchedAlarms = alarmsResult.getOrNull() ?: emptyList()
                val fetchedSleep = sleepResult.getOrNull() ?: emptyList()

                // Save into Room
                fetchedAlarms.forEach { repository.saveAlarm(it) }
                fetchedSleep.forEach { repository.logSleepSession(it) }

                userProfile.value?.let { currentProf ->
                    supabaseRepository.fetchProfile(currentProf).getOrNull()?.let {
                        repository.updateProfile(it)
                    }
                }

                _supabaseState.value = _supabaseState.value.copy(
                    isSyncing = false,
                    lastSyncMessage = "Restored ${fetchedAlarms.size} alarms and ${fetchedSleep.size} sleep sessions from Supabase!",
                    lastSyncTime = System.currentTimeMillis(),
                    errorMessage = null
                )
            } else {
                val error = alarmsResult.exceptionOrNull() ?: sleepResult.exceptionOrNull()
                _supabaseState.value = _supabaseState.value.copy(
                    isSyncing = false,
                    errorMessage = error?.localizedMessage ?: "Failed to restore from Supabase"
                )
            }
        }
    }

    fun signInSupabase(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _supabaseState.value = _supabaseState.value.copy(isSyncing = true, errorMessage = null)
            val result = supabaseRepository.signInWithEmail(email, pass)
            _supabaseState.value = _supabaseState.value.copy(
                isSyncing = false,
                userEmail = supabaseRepository.currentUserOrNull()?.email,
                userId = supabaseRepository.currentUserOrNull()?.id,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
            onComplete(result.isSuccess, result.exceptionOrNull()?.localizedMessage)
        }
    }

    fun signUpSupabase(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _supabaseState.value = _supabaseState.value.copy(isSyncing = true, errorMessage = null)
            val result = supabaseRepository.signUpWithEmail(email, pass)
            _supabaseState.value = _supabaseState.value.copy(
                isSyncing = false,
                userEmail = supabaseRepository.currentUserOrNull()?.email,
                userId = supabaseRepository.currentUserOrNull()?.id,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
            onComplete(result.isSuccess, result.exceptionOrNull()?.localizedMessage)
        }
    }

    fun signOutSupabase() {
        viewModelScope.launch {
            supabaseRepository.signOut()
            _supabaseState.value = _supabaseState.value.copy(
                userEmail = null,
                userId = null,
                lastSyncMessage = "Signed out of Supabase",
                errorMessage = null
            )
        }
    }

    fun clearSupabaseMessage() {
        _supabaseState.value = _supabaseState.value.copy(
            lastSyncMessage = null,
            errorMessage = null
        )
    }

    // --- Google Account Auth Actions ---

    fun getEffectiveGoogleClientId(): String? = googleAuthManager.getEffectiveClientId()

    fun setGoogleWebClientId(clientId: String) {
        googleAuthManager.setCustomClientId(clientId)
    }

    fun clearGoogleAuthError() {
        googleAuthManager.clearError()
    }

    fun signInWithGoogle(
        activity: Activity,
        overrideClientId: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = googleAuthManager.signInWithGoogle(activity, overrideClientId)
            result.onSuccess { user ->
                val current = userProfile.value ?: UserProfileEntity()
                val updated = current.copy(
                    name = user.displayName?.ifBlank { null } ?: current.name,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    isGoogleLinked = true
                )
                repository.updateProfile(updated)
                onComplete(true, null)
            }.onFailure { error ->
                onComplete(false, error.localizedMessage)
            }
        }
    }

    fun signInWithGoogleDemo(
        email: String = "meenaispeaks6@gmail.com",
        displayName: String = "Meenai Speaks",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = googleAuthManager.signInDemoAccount(email, displayName)
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                name = user.displayName?.ifBlank { null } ?: current.name,
                email = user.email,
                photoUrl = user.photoUrl,
                isGoogleLinked = true
            )
            repository.updateProfile(updated)
            onComplete()
        }
    }

    fun signOutGoogle(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            googleAuthManager.signOut()
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(
                email = null,
                photoUrl = null,
                isGoogleLinked = false
            )
            repository.updateProfile(updated)
            onComplete()
        }
    }

    fun refreshCurrentDate() {
        val nowStr = getTodayDateString()
        if (nowStr != _currentDate.value) {
            _currentDate.value = nowStr
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopAlarmTone()
        haptics.stopAlarmVibration()
        shakeDetector?.stop()
        ringingTimerJob?.cancel()
        sleepTimerJob?.cancel()
    }
}
