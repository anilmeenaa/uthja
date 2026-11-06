package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import com.example.alarm.SoundManager
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.AlarmEntity
import com.example.data.model.DismissType
import com.example.data.model.RingtoneCatalog
import com.example.data.model.WakeMode
import com.example.ui.components.AlarmWheelTimePicker
import com.example.ui.screens.WakeUpMissionPickerScreen
import java.util.Calendar
import java.util.Locale

/**
 * Wake-Up Alarm Configuration Dialog matching HTML 6 and Screenshot 6:
 * - Clean title bar with 'X'
 * - Habit label row with chick/emoji and edit pencil
 * - Dynamic ring countdown calculation
 * - Wheel time picker (dimmed previous, highlighted active pill, dimmed next)
 * - Daily checkbox + 7 circular cyan day pills
 * - 5-slot Wake-up mission grid
 * - Sound section with Play button, volume slider, vibration toggle, reminder toggles with Sample buttons
 * - Custom setting (Snooze, Wallpaper)
 * - Fixed bottom coral-red Save button (#FF3B5C)
 */
@Composable
fun AlarmEditScreen(
    initialAlarm: AlarmEntity? = null,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onDelete: ((AlarmEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()

    BackHandler {
        onDismiss()
    }

    var habitEmoji by remember(initialAlarm) {
        val initialTitle = initialAlarm?.title ?: "Wake up early"
        if (initialTitle.startsWith("🐥") || initialTitle.startsWith("💊") ||
            initialTitle.startsWith("🏋️") || initialTitle.startsWith("💧") ||
            initialTitle.startsWith("🙏") || initialTitle.startsWith("🧘")) {
            mutableStateOf(initialTitle.substring(0, 2).trim())
        } else {
            mutableStateOf("🐥")
        }
    }
    var title by remember(initialAlarm) {
        val initialTitle = initialAlarm?.title ?: "Wake up early"
        if (initialTitle.startsWith("🐥") || initialTitle.startsWith("💊") ||
            initialTitle.startsWith("🏋️") || initialTitle.startsWith("💧") ||
            initialTitle.startsWith("🙏") || initialTitle.startsWith("🧘")) {
            mutableStateOf(initialTitle.substring(2).trim())
        } else {
            mutableStateOf(initialTitle)
        }
    }

    var isEditTitleDialogOpen by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(title) }

    var hour by remember(initialAlarm) { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember(initialAlarm) { mutableIntStateOf(initialAlarm?.minute ?: 0) }

    val daysOfWeek = remember { listOf("S", "M", "T", "W", "T", "F", "S") }
    val daysKeys = remember { listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT") }

    var isDaily by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.repeatDays.equals("DAILY", ignoreCase = true) || initialAlarm?.repeatDays.isNullOrBlank())
    }

    var selectedDayIndices by remember(initialAlarm) {
        val set = mutableSetOf<Int>()
        if (initialAlarm == null || isDaily) {
            set.addAll(0..6)
        } else {
            val repeatStr = initialAlarm.repeatDays
            daysKeys.forEachIndexed { idx, key ->
                if (repeatStr.contains(key, ignoreCase = true) || repeatStr.contains(daysOfWeek[idx], ignoreCase = true)) {
                    set.add(idx)
                }
            }
            if (set.isEmpty()) set.addAll(0..6)
        }
        mutableStateOf(set)
    }

    var selectedMissions by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.getMissions() ?: listOf(DismissType.MATH))
    }
    val selectedMissionType = selectedMissions.firstOrNull() ?: DismissType.OFF

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundManager = remember { SoundManager(context) }
    var isRingtonePickerOpen by remember { mutableStateOf(false) }
    var isMissionPickerOpen by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted! Barcode mission is ready.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopVibePreview()
        }
    }

    var soundTrack by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.sound ?: RingtoneCatalog.MEME_UTH_JA.name)
    }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.85f) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    var gentleWakeUpSeconds by remember(initialAlarm) {
        mutableIntStateOf(if (initialAlarm?.gradualVolume != false) 25 else 0)
    }
    var gentleMissionVolumeEnabled by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.gentleMissionVolume != false)
    }
    var timeReminderEnabled by remember { mutableStateOf(false) }
    var weatherReminderEnabled by remember { mutableStateOf(false) }
    var labelReminderEnabled by remember { mutableStateOf(false) }
    var extraLoudEnabled by remember { mutableStateOf(false) }

    var snoozeSetting by remember { mutableStateOf("5 min, 3 times") }
    var selectedWallpaper by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.wallpaper ?: com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID)
    }
    var isWallpaperPickerOpen by remember { mutableStateOf(false) }

    // Dynamic ring countdown computation
    val repeatDaysFormatted = if (isDaily || selectedDayIndices.size == 7) {
        "DAILY"
    } else {
        selectedDayIndices.map { daysKeys[it] }.joinToString(",")
    }

    val nextTriggerMs = remember(hour, minute, repeatDaysFormatted) {
        AlarmScheduler.calculateNextTriggerTime(hour, minute, repeatDaysFormatted)
    }
    val countdownText = remember(nextTriggerMs) {
        AlarmScheduler.formatRemainingTime(nextTriggerMs)
    }

    // Time wheel values
    val periodStr = if (hour >= 12) "p.m." else "a.m."
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    val prevHour = if (displayHour == 1) 12 else displayHour - 1
    val prevMinute = if (minute == 0) 59 else minute - 1
    val nextHour = if (displayHour == 12) 1 else displayHour + 1
    val nextMinute = if (minute == 59) 0 else minute + 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (initialAlarm == null) "Set alarm" else "Edit alarm",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.2).sp
                )

                if (initialAlarm != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            onDelete(initialAlarm)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alarm",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Balance spacer
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

                // Alarm Label Card with Chick Emoji & Pencil Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable {
                            tempTitle = title
                            isEditTitleDialogOpen = true
                        }
                    ) {
                        Text(
                            text = habitEmoji,
                            fontSize = 24.sp
                        )
                        Text(
                            text = title,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                    }

                    IconButton(
                        onClick = {
                            tempTitle = title
                            isEditTitleDialogOpen = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit label",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Countdown Note
                Text(
                    text = countdownText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Scrollable Smooth Wheel Time Picker with Snapping & Haptics
                AlarmWheelTimePicker(
                    hour24 = hour,
                    minute = minute,
                    onTimeChange = { newHour, newMin ->
                        hour = newHour
                        minute = newMin
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Day Selection Section: Daily checkbox & 7 Day Pills
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                haptics.pulseAppleSelection()
                                isDaily = !isDaily
                                if (isDaily) {
                                    selectedDayIndices = (0..6).toMutableSet()
                                }
                            }
                        ) {
                            Checkbox(
                                checked = isDaily,
                                onCheckedChange = {
                                    haptics.pulseAppleSelection()
                                    isDaily = it
                                    if (isDaily) {
                                        selectedDayIndices = (0..6).toMutableSet()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF38BDF8),
                                    uncheckedColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Daily",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7 Circular Day Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEachIndexed { index, letter ->
                            val isSelected = selectedDayIndices.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFDCF2FA) else Color(0xFFF1F5F9)
                                    )
                                    .clickable {
                                        haptics.pulseAppleSelection()
                                        val updated = selectedDayIndices.toMutableSet()
                                        if (updated.contains(index)) {
                                            if (updated.size > 1) updated.remove(index)
                                        } else {
                                            updated.add(index)
                                        }
                                        selectedDayIndices = updated
                                        isDaily = updated.size == 7
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF1A8BB8) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Wake-Up Mission Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleButtonClick()
                                    isMissionPickerOpen = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Wake-up mission",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedMissions.isNotEmpty() && !selectedMissions.contains(DismissType.OFF)) Color(0xFFECE0FD) else Color(0xFFF1F5F9))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (selectedMissions.isNotEmpty() && !selectedMissions.contains(DismissType.OFF)) {
                                            if (selectedMissions.size == 1) selectedMissions[0].title.take(14) else "${selectedMissions.size} Active"
                                        } else "No Mission",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedMissions.isNotEmpty() && !selectedMissions.contains(DismissType.OFF)) Color(0xFF7C3AED) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedMissions.contains(DismissType.OFF)) "0/5" else "${selectedMissions.size}/5",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Edit mission",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5 Slots Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (slotIdx in 0..4) {
                                if (slotIdx < selectedMissions.size) {
                                    val mission = selectedMissions[slotIdx]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                when (mission) {
                                                    DismissType.SHAKE -> Color(0xFFFFECE8)
                                                    DismissType.MATH -> Color(0xFFE8F5E9)
                                                    DismissType.STEP -> Color(0xFFFDF3F0)
                                                    DismissType.QR_CODE -> Color(0xFFF4EEFE)
                                                    DismissType.TYPING -> Color(0xFFF3EEFD)
                                                    else -> Color(0xFFECE0FD)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when (mission) {
                                                    DismissType.SHAKE -> Color(0xFFFF5722)
                                                    DismissType.MATH -> Color(0xFF338957)
                                                    DismissType.STEP -> Color(0xFFED846D)
                                                    DismissType.QR_CODE -> Color(0xFF8455FD)
                                                    DismissType.TYPING -> Color(0xFF936BFD)
                                                    else -> Color(0xFF8B64DC)
                                                },
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                haptics.pulseAppleSelection()
                                                if (mission == DismissType.QR_CODE) {
                                                    val cameraGranted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.CAMERA
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (!cameraGranted) {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                }
                                                isMissionPickerOpen = true
                                            }
                                            .testTag("mission_slot_$slotIdx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Delete badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9))
                                                .clickable {
                                                    haptics.pulseAppleButtonClick()
                                                    selectedMissions = selectedMissions.filterIndexed { i, _ -> i != slotIdx }.ifEmpty { listOf(DismissType.OFF) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            when (mission) {
                                                DismissType.TYPING -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_typing),
                                                        contentDescription = "Typing",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Typing",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF8B5CF6)
                                                    )
                                                }
                                                DismissType.MATH -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_math),
                                                        contentDescription = "Math",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Math",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF22C55E)
                                                    )
                                                }
                                                DismissType.STEP -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_step_exact),
                                                        contentDescription = "Step",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Step",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFED846D)
                                                    )
                                                }
                                                DismissType.QR_CODE -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_qr_exact),
                                                        contentDescription = "QR",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "QR",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF8455FD)
                                                    )
                                                }
                                                DismissType.SHAKE -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_shake),
                                                        contentDescription = "Shake",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Shake",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFFF5722)
                                                    )
                                                }
                                                DismissType.COLOR_TILES -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_colortiles),
                                                        contentDescription = "Tiles",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Tiles",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF2E7D32)
                                                    )
                                                }
                                                DismissType.OFF -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_mission_off),
                                                        contentDescription = "No Mission",
                                                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Off",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                else -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF38BDF8)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = mission.title.take(6),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty slot: Add (+)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptics.pulseAppleSelection()
                                                isMissionPickerOpen = true
                                            }
                                            .testTag("add_mission_slot_$slotIdx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add mission",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Add",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF8FAFC)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Auxiliary Checks Rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wake up check",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Off",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prevent power-off",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Off",
                                    fontSize = 14.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alarm Sound Section Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section Header: Alarm ringtone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleButtonClick()
                                    isRingtonePickerOpen = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Alarm ringtone",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (RingtoneCatalog.isMemeRingtone(soundTrack)) Color(0xFFEDE9FE) else Color(0xFFF1F5F9))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (RingtoneCatalog.isMemeRingtone(soundTrack)) "👾 Viral Meme" else "Standard Tone",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (RingtoneCatalog.isMemeRingtone(soundTrack)) Color(0xFF7C3AED) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Change",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7C3AED)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Change ringtone",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sound Picker Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleSelection()
                                    isRingtonePickerOpen = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPlayingPreview) {
                                                if (RingtoneCatalog.isMemeRingtone(soundTrack)) Color(0xFF7C3AED) else Color(0xFF0F172A)
                                            } else {
                                                if (RingtoneCatalog.isMemeRingtone(soundTrack)) Color(0xFFEDE9FE) else Color(0xFFF1F5F9)
                                            }
                                        )
                                        .clickable {
                                            haptics.pulseAppleButtonClick()
                                            if (isPlayingPreview) {
                                                soundManager.stopVibePreview()
                                                isPlayingPreview = false
                                            } else {
                                                isPlayingPreview = true
                                                soundManager.playVibePreview(soundTrack, scope) {
                                                    isPlayingPreview = false
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview sound",
                                        tint = if (isPlayingPreview) Color.White else (if (RingtoneCatalog.isMemeRingtone(soundTrack)) Color(0xFF7C3AED) else Color(0xFF334155)),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = soundTrack,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                        if (RingtoneCatalog.isMemeRingtone(soundTrack)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFEDE9FE))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "👾 Meme",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7C3AED)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (RingtoneCatalog.isMemeRingtone(soundTrack)) "Viral wake-up meme • Tap to change" else "Tap to choose ringtones & meme audio",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Choose ringtone",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Progress Timeline
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFCBD5E1))
                            )
                        }

                        // Volume Slider & Vibration Row
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )

                            Slider(
                                value = volume,
                                onValueChange = { volume = it },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF1E293B),
                                    activeTrackColor = Color(0xFF1E293B),
                                    inactiveTrackColor = Color(0xFFF1F5F9)
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = "Vibration",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                                Checkbox(
                                    checked = vibrationEnabled,
                                    onCheckedChange = {
                                        haptics.pulseAppleSelection()
                                        vibrationEnabled = it
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF38BDF8),
                                        uncheckedColor = Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF8FAFC)))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Gentle wake-up row (Volume Fade-in)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.pulseAppleSelection()
                                    gentleWakeUpSeconds = when (gentleWakeUpSeconds) {
                                        0 -> 15
                                        15 -> 25
                                        25 -> 40
                                        40 -> 60
                                        else -> 0
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gentle wake-up (Fade-in)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (gentleWakeUpSeconds > 0) "Starts at 10% volume, gradually increases" else "Instant full volume",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (gentleWakeUpSeconds > 0) "$gentleWakeUpSeconds s" else "Off",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (gentleWakeUpSeconds > 0) Color(0xFF0284C7) else Color(0xFF94A3B8)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Gentle Mission Volume row (Focus volume so user isn't frustrated)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.pulseAppleSelection()
                                    gentleMissionVolumeEnabled = !gentleMissionVolumeEnabled
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Gentle Mission Volume",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE0F2FE))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Anti-Frustration",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                }
                                Text(
                                    text = "Lowers alarm to 30% while solving challenges so you don't get frustrated",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = gentleMissionVolumeEnabled,
                                onCheckedChange = {
                                    haptics.pulseAppleSelection()
                                    gentleMissionVolumeEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        // Reminder toggles with Sample buttons
                        listOf(
                            Triple("Time reminder", timeReminderEnabled) { timeReminderEnabled = !timeReminderEnabled },
                            Triple("Weather reminder", weatherReminderEnabled) { weatherReminderEnabled = !weatherReminderEnabled },
                            Triple("Label reminder", labelReminderEnabled) { labelReminderEnabled = !labelReminderEnabled },
                            Triple("Extra loud effect", extraLoudEnabled) { extraLoudEnabled = !extraLoudEnabled }
                        ).forEach { (label, isChecked, onToggle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { haptics.pulseAppleButtonClick() }
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Sample",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Sample",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        haptics.pulseAppleSelection()
                                        onToggle()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF38BDF8),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Setting Section Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Snooze row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleSelection()
                                    snoozeSetting = if (snoozeSetting.contains("5 min")) "10 min, 3 times" else "5 min, 3 times"
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Snooze",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = snoozeSetting,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF8FAFC)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Wallpaper row
                        val currentWallpaperObj = remember(selectedWallpaper) {
                            com.example.data.model.WallpaperCatalog.findById(selectedWallpaper)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.pulseAppleButtonClick()
                                    isWallpaperPickerOpen = true
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Alarm wallpaper",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = currentWallpaperObj.title,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = currentWallpaperObj.gradientColors
                                            )
                                        )
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentWallpaperObj.customUri != null) {
                                        coil.compose.AsyncImage(
                                            model = currentWallpaperObj.customUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else if (currentWallpaperObj.drawableRes != null) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = currentWallpaperObj.drawableRes),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(text = currentWallpaperObj.iconEmoji, fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Change Wallpaper",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fixed Bottom Save Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x00F7F8FA),
                                Color(0xF2F7F8FA),
                                Color(0xFFF7F8FA)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        haptics.pulseAppleSuccess()
                        if (selectedMissions.contains(DismissType.QR_CODE)) {
                            val cameraGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!cameraGranted) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        val finalTitle = "$habitEmoji $title".trim()
                        val entity = (initialAlarm ?: AlarmEntity(
                            title = finalTitle,
                            hour = hour,
                            minute = minute
                        )).copy(
                            title = finalTitle,
                            hour = hour,
                            minute = minute,
                            repeatDays = repeatDaysFormatted,
                            dismissType = selectedMissionType,
                            sound = soundTrack,
                            wallpaper = selectedWallpaper,
                            isEnabled = true,
                            missions = selectedMissions.joinToString(",") { it.name },
                            gradualVolume = gentleWakeUpSeconds > 0,
                            gentleMissionVolume = gentleMissionVolumeEnabled
                        )
                        onSave(entity)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x55FF3B5C))
                        .testTag("save_alarm_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B5C),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

    // Edit Title Dialog
    if (isEditTitleDialogOpen) {
        AlertDialog(
            onDismissRequest = { isEditTitleDialogOpen = false },
            title = { Text("Edit Habit / Alarm Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("🐥", "💊", "🏋️", "💧", "🙏", "🧘", "⏰").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (habitEmoji == emoji) Color(0xFFDCF2FA) else Color(0xFFF1F5F9))
                                    .clickable { habitEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Alarm Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        title = tempTitle.ifBlank { "Wake up early" }
                        isEditTitleDialogOpen = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditTitleDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isRingtonePickerOpen) {
        RingtonePickerDialog(
            selectedSoundName = soundTrack,
            onRingtoneSelected = { chosen ->
                soundTrack = chosen
                isRingtonePickerOpen = false
            },
            onDismiss = {
                isRingtonePickerOpen = false
            }
        )
    }

    if (isMissionPickerOpen) {
        WakeUpMissionPickerScreen(
            currentMissions = selectedMissions,
            onMissionsConfirmed = { chosenList ->
                selectedMissions = chosenList.ifEmpty { listOf(DismissType.OFF) }
                if (chosenList.contains(DismissType.QR_CODE)) {
                    val cameraGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!cameraGranted) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                isMissionPickerOpen = false
            },
            onDismiss = {
                isMissionPickerOpen = false
            }
        )
    }

    if (isWallpaperPickerOpen) {
        com.example.ui.screens.AlarmWallpaperPickerScreen(
            currentWallpaperId = selectedWallpaper,
            onWallpaperSelected = { chosenWp ->
                selectedWallpaper = chosenWp.id
                isWallpaperPickerOpen = false
            },
            onDismiss = {
                isWallpaperPickerOpen = false
            }
        )
    }
}

@Composable
fun AlarmEditDialog(
    initialAlarm: AlarmEntity? = null,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onDelete: ((AlarmEntity) -> Unit)? = null
) {
    AlarmEditScreen(
        initialAlarm = initialAlarm,
        onDismiss = onDismiss,
        onSave = onSave,
        onDelete = onDelete
    )
}
