package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import com.example.ui.components.AlarmWheelTimePicker
import java.util.Locale

/**
 * Dedicated Full-Screen 3-Step Habit Alarm Creator:
 * - Step 1: "What habit do you want to build?"
 * - Step 2: "When do you want to do this habit?" (wheel time picker)
 * - Step 3: "Choose a mission for your habit alarm"
 */
@Composable
fun HabitAlarmScreen(
    onClose: () -> Unit,
    onSaveHabitAlarm: (habitTitle: String, hour: Int, minute: Int, mission: String, sound: String, wallpaper: String) -> Unit,
    initialMission: String = "Habit item snap",
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()

    var currentStep by remember { mutableIntStateOf(1) }
    var selectedHabit by remember { mutableStateOf("🐥 Wake up early") }
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedMission by remember { mutableStateOf(initialMission) }
    var selectedSound by remember { mutableStateOf(com.example.data.model.RingtoneCatalog.MEME_UTH_JA.name) }
    var selectedWallpaper by remember { mutableStateOf(com.example.data.model.WallpaperCatalog.DEFAULT_WALLPAPER_ID) }
    var isRingtonePickerOpen by remember { mutableStateOf(false) }
    var isWallpaperPickerOpen by remember { mutableStateOf(false) }

    var isCustomHabitDialogOpen by remember { mutableStateOf(false) }
    var customHabitInput by remember { mutableStateOf("") }

    BackHandler(enabled = true) {
        haptics.pulseAppleButtonClick()
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut()
                    )
                }
            },
            label = "HabitAlarmTransition",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            when (step) {
                1 -> HabitSelectionStep(
                    selectedHabit = selectedHabit,
                    onHabitSelected = { habit ->
                        haptics.pulseAppleButtonClick()
                        selectedHabit = habit
                        currentStep = 2
                    },
                    onCustomHabitClick = {
                        haptics.pulseAppleButtonClick()
                        customHabitInput = ""
                        isCustomHabitDialogOpen = true
                    },
                    onClose = {
                        haptics.pulseAppleButtonClick()
                        onClose()
                    }
                )

                2 -> HabitTimeStep(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onTimeChanged = { h, m ->
                        selectedHour = h
                        selectedMinute = m
                    },
                    onNext = {
                        haptics.pulseAppleButtonClick()
                        currentStep = 3
                    },
                    onBack = {
                        haptics.pulseAppleButtonClick()
                        currentStep = 1
                    }
                )

                3 -> ChooseMissionStep(
                    selectedMission = selectedMission,
                    onMissionSelected = { mission ->
                        haptics.pulseAppleSelection()
                        selectedMission = mission
                    },
                    selectedSound = selectedSound,
                    onOpenRingtonePicker = {
                        haptics.pulseAppleSelection()
                        isRingtonePickerOpen = true
                    },
                    selectedWallpaper = selectedWallpaper,
                    onOpenWallpaperPicker = {
                        haptics.pulseAppleSelection()
                        isWallpaperPickerOpen = true
                    },
                    onSave = {
                        haptics.pulseAppleSuccess()
                        onSaveHabitAlarm(selectedHabit, selectedHour, selectedMinute, selectedMission, selectedSound, selectedWallpaper)
                    },
                    onBack = {
                        haptics.pulseAppleButtonClick()
                        currentStep = 2
                    }
                )
            }
        }

        // Custom Habit Input Dialog
        if (isCustomHabitDialogOpen) {
            AlertDialog(
                onDismissRequest = { isCustomHabitDialogOpen = false },
                title = { Text("Enter your own habit") },
                text = {
                    OutlinedTextField(
                        value = customHabitInput,
                        onValueChange = { customHabitInput = it },
                        label = { Text("Habit name") },
                        placeholder = { Text("e.g. Morning journaling") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (customHabitInput.isNotBlank()) {
                                selectedHabit = "✨ ${customHabitInput.trim()}"
                                isCustomHabitDialogOpen = false
                                currentStep = 2
                            }
                        }
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isCustomHabitDialogOpen = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isRingtonePickerOpen) {
            com.example.ui.dialogs.RingtonePickerDialog(
                selectedSoundName = selectedSound,
                onRingtoneSelected = { chosen ->
                    selectedSound = chosen
                    isRingtonePickerOpen = false
                },
                onDismiss = {
                    isRingtonePickerOpen = false
                }
            )
        }

        if (isWallpaperPickerOpen) {
            AlarmWallpaperPickerScreen(
                currentWallpaperId = selectedWallpaper,
                onWallpaperSelected = { chosen ->
                    selectedWallpaper = chosen.id
                    isWallpaperPickerOpen = false
                },
                onDismiss = {
                    isWallpaperPickerOpen = false
                }
            )
        }
    }
}

@Composable
fun HabitAlarmScreen(
    onClose: () -> Unit,
    onSaveHabitAlarm: (habitTitle: String, hour: Int, minute: Int, mission: String) -> Unit,
    initialMission: String = "Habit item snap",
    modifier: Modifier = Modifier
) {
    HabitAlarmScreen(
        onClose = onClose,
        onSaveHabitAlarm = { title, h, m, mission, _, _ ->
            onSaveHabitAlarm(title, h, m, mission)
        },
        initialMission = initialMission,
        modifier = modifier
    )
}

/**
 * Step 1: "What habit do you want to build?"
 */
@Composable
private fun HabitSelectionStep(
    selectedHabit: String,
    onHabitSelected: (String) -> Unit,
    onCustomHabitClick: () -> Unit,
    onClose: () -> Unit
) {
    val habits = listOf(
        "🐥" to "Wake up early",
        "💊" to "Take medication",
        "🏋️" to "5-min stretch",
        "💧" to "Drink water",
        "🙏" to "Say prayers",
        "🧘" to "1-min meditation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF1E232A),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Progress Bar 1/3
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE3E6EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.333f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E232A))
                    )
                }
                Text(
                    text = "1/3",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Screen Title
        Text(
            text = "What habit do you want to build?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF191D23),
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Schedule a daily alarm dedicated to your habit",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Habit Cards List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            habits.forEach { (emoji, title) ->
                val fullLabel = "$emoji $title"
                val isSelected = selectedHabit == fullLabel

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(22.dp), spotColor = Color(0x0A000000))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF1E232A) else Color(0xFFF0F1F3),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onHabitSelected(fullLabel) }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 28.sp
                        )
                        Text(
                            text = title,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                    }
                }
            }

            // "Enter my own" custom habit card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(22.dp), spotColor = Color(0x05000000))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF0F1F3), RoundedCornerShape(22.dp))
                    .clickable { onCustomHabitClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE1F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom",
                            tint = Color(0xFF29A3FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Enter my own",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF29A3FF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * Step 2: "When do you want to do this habit?"
 */
@Composable
private fun HabitTimeStep(
    hour: Int,
    minute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val haptics = rememberAppleHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back button and 2/3 progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = Color(0xFF1E232A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(136.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = 0.667f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF2C2C2E))
                        )
                    }
                    Text(
                        text = "2/3",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Screen Title
            Text(
                text = "When do you want to do\nthis habit?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E24),
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Interactive Wheel Time Picker
            AlarmWheelTimePicker(
                hour24 = hour,
                minute = minute,
                onTimeChange = onTimeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Next Button: Red-Pink (#FF3752)
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x40FF3752))
                .testTag("habit_time_next"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF3752),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Next",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Step 3: "Choose a mission for your habit alarm"
 */
@Composable
private fun ChooseMissionStep(
    selectedMission: String,
    onMissionSelected: (String) -> Unit,
    selectedSound: String,
    onOpenRingtonePicker: () -> Unit,
    selectedWallpaper: String,
    onOpenWallpaperPicker: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val missions = listOf(
        Triple("Shake to dismiss", true, Icons.Default.Vibration to Color(0xFFFF5722) to Color(0xFFFFECE8)),
        Triple("Math", false, Icons.Default.Calculate to Color(0xFF0EA5E9) to Color(0xFFE0F3FE)),
        Triple("Habit item snap", false, Icons.Default.CameraAlt to Color(0xFFFF5A73) to Color(0xFFFFE4E8)),
        Triple("Find Color Tiles", false, Icons.Default.GridOn to Color(0xFF14B8A6) to Color(0xFFE0F8F7)),
        Triple("Typing", false, Icons.Default.Keyboard to Color(0xFF38BDF8) to Color(0xFFE2F0FD)),
        Triple("Off", false, Icons.Default.Close to Color(0xFF1F2937) to Color(0xFFF3F4F6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back button and 3/3 progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = Color(0xFF1E232A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(136.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF2C2C2E))
                        )
                    }
                    Text(
                        text = "3/3",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Screen Title
            Text(
                text = "Choose a mission for your\nhabit alarm",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E24),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                missions.forEach { (name, isBest, iconPair) ->
                    val (icon, tint) = iconPair.first
                    val bg = iconPair.second
                    val isSelected = selectedMission.equals(name, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color.Black else Color.Transparent,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .shadow(2.dp, RoundedCornerShape(22.dp), spotColor = Color(0x08000000))
                            .clickable { onMissionSelected(name) }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827)
                                )
                                if (isBest) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFEEFC3))
                                            .padding(horizontal = 9.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "BEST",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFDE7500),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ringtone Selector Card with 👾 Meme Tag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                .clickable { onOpenRingtonePicker() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alarm Ringtone",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedSound,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E232A),
                        maxLines = 1
                    )
                    if (com.example.data.model.RingtoneCatalog.isMemeRingtone(selectedSound)) {
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
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Change",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alarm Wallpaper Selector Card
        val wallpaperItem = remember(selectedWallpaper) {
            com.example.data.model.WallpaperCatalog.findById(selectedWallpaper)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                .clickable { onOpenWallpaperPicker() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = wallpaperItem.gradientColors
                            )
                        )
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (wallpaperItem.customUri != null) {
                        coil.compose.AsyncImage(
                            model = wallpaperItem.customUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (wallpaperItem.drawableRes != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = wallpaperItem.drawableRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(text = wallpaperItem.iconEmoji, fontSize = 16.sp)
                    }
                }

                Column {
                    Text(
                        text = "Alarm Wallpaper",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = wallpaperItem.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E232A),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Change",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button: (#FF3850)
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x40FF3850))
                .testTag("habit_mission_save"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF3850),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Save Habit Alarm",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
