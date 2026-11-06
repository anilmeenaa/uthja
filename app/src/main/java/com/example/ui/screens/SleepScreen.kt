package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.SleepSessionEntity
import com.example.data.model.SnoreEventEntity
import com.example.data.model.UserProfileEntity
import com.example.sleep.ActiveSleepScreen
import com.example.sleep.SleepAmbientPlayer
import com.example.sleep.SleepGuidanceScreen
import com.example.sleep.SleepReportScreen
import com.example.sleep.SleepSoundItem
import com.example.sleep.SoundCatalog
import com.example.sleep.SoundPickerBottomSheet
import com.example.ui.ActiveSleepState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    activeSleep: ActiveSleepState,
    sleepSessions: List<SleepSessionEntity>,
    snoreEvents: List<SnoreEventEntity> = emptyList(),
    alarms: List<com.example.data.model.AlarmEntity> = emptyList(),
    alarmEvents: List<com.example.data.model.AlarmEventEntity> = emptyList(),
    userProfile: UserProfileEntity?,
    onStartSleep: () -> Unit,
    onFinishSleep: () -> Unit,
    onDeleteSession: (SleepSessionEntity) -> Unit,
    onSimulateMovement: (Boolean) -> Unit = {},
    onSimulateOvernight: () -> Unit = {},
    onViewReport: (SleepSessionEntity) -> Unit = {},
    onAddHabitAlarm: () -> Unit = {},
    onGuidanceVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE) }

    // Screen sub-navigation states
    var showGuidance by remember { mutableStateOf(false) }
    var showActiveTrackingScreen by remember { mutableStateOf(activeSleep.isTracking) }
    var showReportScreen by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Notify parent whenever guidance visibility changes to hide the bottom navigation bar
    LaunchedEffect(showGuidance) {
        onGuidanceVisibilityChanged(showGuidance)
    }

    val startTrackingSession = {
        onStartSleep()
        showActiveTrackingScreen = true
    }

    // Microphone runtime permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val neverShow = prefs.getBoolean("never_show_guidance", false)
        if (neverShow) {
            startTrackingSession()
        } else {
            showGuidance = true
        }
    }

    val requestStartTracking = {
        if (activeSleep.isTracking) {
            showActiveTrackingScreen = true
        } else {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val neverShow = prefs.getBoolean("never_show_guidance", false)
            if (hasMic) {
                if (neverShow) {
                    startTrackingSession()
                } else {
                    showGuidance = true
                }
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Handle back button for sub-pages
    BackHandler(enabled = showGuidance) {
        showGuidance = false
    }
    BackHandler(enabled = !showGuidance && showActiveTrackingScreen) {
        showActiveTrackingScreen = false
    }
    BackHandler(enabled = !showGuidance && !showActiveTrackingScreen && showReportScreen) {
        showReportScreen = false
    }

    // Ambient sound playback engine
    val ambientPlayer = remember { SleepAmbientPlayer(context) }
    var currentSound by remember { mutableStateOf<SleepSoundItem>(SoundCatalog.NO_SOUND) }
    var isSoundPlaying by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            ambientPlayer.stop()
        }
    }

    // Keep active screen in sync if tracking started from outside
    if (activeSleep.isTracking && !showActiveTrackingScreen && !showReportScreen) {
        showActiveTrackingScreen = true
    }

    val latestSession = sleepSessions.firstOrNull()
    val hasSessionData = latestSession != null
    val lastQualityScore = latestSession?.sleepScore ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Main Sleep Dashboard (Exact matching Screenshot 1) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HeaderSection: "Sleep"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Sleep",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.testTag("sleep_header_title")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // If a tracking session is ongoing in background, show active resume banner
                    if (activeSleep.isTracking) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clickable { showActiveTrackingScreen = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18203D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C88FF))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Sleep tracking in progress...",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val mins = (activeSleep.elapsedSeconds / 60)
                                        Text(
                                            text = "$mins min elapsed • Tap to resume full view",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Main Sleep Hero Card: "Find out your sleep issues" + Arc Gauge (Screenshot 1)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = Color.Black.copy(alpha = 0.04f),
                                ambientColor = Color.Black.copy(alpha = 0.02f)
                            )
                            .testTag("sleep_tracking_hero_card"),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Card Title
                            Text(
                                text = "Find out your sleep issues",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                            )

                            // Semi-circular Arc Gauge Meter & Sleep Quality Content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(230.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val trackArcColor = MaterialTheme.colorScheme.surfaceVariant
                                    val activeArcColor = MaterialTheme.colorScheme.primary

                                    Canvas(modifier = Modifier.size(216.dp)) {
                                        val strokeW = 16.dp.toPx()
                                        val arcSize = size.width - strokeW
                                        val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                                        val startAngle = 145f
                                        val totalSweep = 250f

                                        // Background Track Arc
                                        drawArc(
                                            color = trackArcColor,
                                            startAngle = startAngle,
                                            sweepAngle = totalSweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = Size(arcSize, arcSize),
                                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                        )

                                        // Active Track Arc
                                        val activeSweep = if (hasSessionData) {
                                            (lastQualityScore.coerceIn(5, 100) / 100f) * totalSweep
                                        } else {
                                            0f
                                        }
                                        if (activeSweep > 0f) {
                                            drawArc(
                                                color = activeArcColor,
                                                startAngle = startAngle,
                                                sweepAngle = activeSweep,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = Size(arcSize, arcSize),
                                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                            )
                                        }
                                    }

                                    // Content perfectly centered inside the arc without overlapping
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {
                                        Text(
                                            text = if (hasSessionData) "Last night quality" else "No sleep tracked yet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (hasSessionData) "$lastQualityScore" else "--",
                                            fontSize = 52.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 56.sp
                                        )
                                    }
                                }

                                // Information text placed cleanly below the circular progress bar with proper spacing
                                if (hasSessionData) {
                                    val snoreMins = latestSession!!.snoreDurationMinutes
                                    val infoText = if (snoreMins > 0) {
                                        "$snoreMins min snoring sound • ${latestSession.movementRating}"
                                    } else {
                                        "Peaceful rest • No snoring detected"
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = infoText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Track My Sleep Action Button
                            Button(
                                onClick = requestStartTracking,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("track_sleep_button"),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = if (activeSleep.isTracking) "Resume tracking" else "Track my sleep",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SleepReportNavigationCard: "My sleep report" (Screenshot 1)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = Color.Black.copy(alpha = 0.03f),
                                ambientColor = Color.Black.copy(alpha = 0.02f)
                            )
                            .clickable {
                                showReportScreen = true
                            }
                            .testTag("sleep_report_navigation_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sleep_report_moon),
                                    contentDescription = "Crescent Moon and Star",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Text(
                                    text = "My sleep report",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.2).sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open report",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Guidance Screen ---
        AnimatedVisibility(
            visible = showGuidance,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            SleepGuidanceScreen(
                onGotIt = {
                    showGuidance = false
                    startTrackingSession()
                },
                onNeverShowAgain = {
                    prefs.edit().putBoolean("never_show_guidance", true).apply()
                    showGuidance = false
                    startTrackingSession()
                }
            )
        }

        // --- 3. Active Sleep Tracking Screen ---
        AnimatedVisibility(
            visible = showActiveTrackingScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ActiveSleepScreen(
                currentSound = currentSound,
                activeSleep = activeSleep,
                onCollapse = {
                    showActiveTrackingScreen = false
                },
                onOpenSoundPicker = {
                    showSoundPicker = true
                },
                onFinishSleep = {
                    showActiveTrackingScreen = false
                    ambientPlayer.stop()
                    isSoundPlaying = false
                    onFinishSleep()
                    showReportScreen = true
                }
            )
        }

        // --- 4. Sound Picker Bottom Sheet ---
        if (showSoundPicker) {
            SoundPickerBottomSheet(
                selectedSound = currentSound,
                isPlaying = isSoundPlaying,
                timerMinutes = timerMinutes,
                onSoundSelected = { sound ->
                    currentSound = sound
                    if (sound.id == "no_sound") {
                        ambientPlayer.stop()
                        isSoundPlaying = false
                    } else {
                        ambientPlayer.playSound(sound, scope)
                        isSoundPlaying = true
                    }
                },
                onSetTimerMinutes = { mins ->
                    timerMinutes = mins
                    ambientPlayer.setTimer(mins, scope)
                },
                onDismiss = {
                    showSoundPicker = false
                }
            )
        }

        // --- 5. Sleep Report Screen (Screenshots 2, 3, 4 with 3 tabs) ---
        AnimatedVisibility(
            visible = showReportScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            SleepReportScreen(
                sessions = sleepSessions,
                snoreEvents = snoreEvents,
                onBack = {
                    showReportScreen = false
                },
                onSetHabitAlarm = onAddHabitAlarm
            )
        }


        // --- 6. Microphone Permission Rationale Dialog ---
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = {
                    Text(
                        text = "Microphone Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Lune analyzes overnight sleep audio locally on your device to estimate snoring patterns and sleep restfulness.\n\nAll machine learning inference runs 100% offline via MediaPipe YAMNet. No raw audio is ever saved to storage or uploaded anywhere.",
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationale = false
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222429))
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPermissionRationale = false
                            val neverShow = prefs.getBoolean("never_show_guidance", false)
                            if (neverShow) {
                                startTrackingSession()
                            } else {
                                showGuidance = true
                            }
                        }
                    ) {
                        Text("Continue without mic", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
