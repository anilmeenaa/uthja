package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.LocalIsDarkMode
import com.example.alarm.AlarmScheduler
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.AlarmEntity
import com.example.data.model.DismissType
import com.example.ui.dialogs.QuickAlarmDialog
import com.example.R
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import java.util.Locale

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay

/**
 * Alarms Screen matching HTML 1 and Screenshot 1:
 * - Top header with ring countdown: "Rings in 17 hr. 35 min >" and three dots menu
 * - Alarm card with repeat days (S M T W T F S), cyan toggle switch, bold time (7:00 am),
 *   mission badge, habit title ("🐥 Wake up early") and card menu (⋮)
 * - Vibrant coral-red FAB (+) with soft drop shadow at bottom right
 * - Quick alarm integration
 */
@Composable
fun AlarmsScreen(
    alarms: List<AlarmEntity>,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onTestAlarm: (AlarmEntity) -> Unit,
    onAddNewAlarm: () -> Unit,
    onAddHabitAlarm: (() -> Unit)? = null,
    onSaveQuickAlarm: ((minutes: Int, ringDurationSeconds: Int, sound: String, volume: Float, vibrate: Boolean) -> Unit)? = null,
    onOpenSleepVisualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()
    var isTopMenuExpanded by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isQuickAlarmOpen by remember { mutableStateOf(false) }

    // Live clock ticker aligned to system minute boundaries
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMs = now
            val msToNextMinute = (60_000L - (now % 60_000L)).coerceAtLeast(1000L)
            delay(msToNextMinute)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // Find next upcoming alarm for countdown banner using live currentTimeMs
    val activeAlarms = remember(alarms) { alarms.filter { it.isEnabled } }
    val nextAlarmInfo = remember(activeAlarms, currentTimeMs) {
        if (activeAlarms.isEmpty()) {
            null
        } else {
            activeAlarms.map { alarm ->
                val nextMs = AlarmScheduler.calculateNextTriggerTime(
                    alarm.hour,
                    alarm.minute,
                    alarm.repeatDays,
                    currentTimeMs
                )
                alarm to nextMs
            }.minByOrNull { it.second }
        }
    }

    val countdownText = remember(nextAlarmInfo, currentTimeMs) {
        if (nextAlarmInfo == null) {
            "No active alarms"
        } else {
            AlarmScheduler.formatRemainingTime(nextAlarmInfo.second, currentTimeMs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 110.dp)
        ) {
            // Header: More options 3 dots button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            isTopMenuExpanded = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isTopMenuExpanded,
                        onDismissRequest = { isTopMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Quick Alarm") },
                            onClick = {
                                isTopMenuExpanded = false
                                isQuickAlarmOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Habit Alarm") },
                            onClick = {
                                isTopMenuExpanded = false
                                if (onAddHabitAlarm != null) {
                                    onAddHabitAlarm()
                                } else {
                                    onAddNewAlarm()
                                }
                            }
                        )
                    }
                }
            }

            // Ring in countdown title banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        haptics.pulseAppleButtonClick()
                        nextAlarmInfo?.first?.let { onEditAlarm(it) } ?: onAddNewAlarm()
                    }
                    .padding(top = 8.dp, bottom = 14.dp, start = 4.dp)
            ) {
                Text(
                    text = countdownText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Alarm Cards List
            if (alarms.isEmpty()) {
                // Empty state card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onAddNewAlarm() }
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🐥", fontSize = 36.sp)
                        Text(
                            text = "No alarms yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to set your first wake up habit",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    alarms.forEach { alarm ->
                        AlarmCardItem(
                            alarm = alarm,
                            onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                            onClick = { onEditAlarm(alarm) },
                            onTest = { onTestAlarm(alarm) },
                            onDelete = { onDeleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        // Dimmed backdrop scrim when popup is open
        if (isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB31E232A))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.pulseAppleButtonClick()
                        isFabMenuExpanded = false
                    }
            )
        }

        // Floating Action Button & Speed-Dial Popup Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (isFabMenuExpanded) {
                // Top Card: Habit alarm & Quick alarm
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x33000000))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Option 1: Habit alarm
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleButtonClick()
                                    isFabMenuExpanded = false
                                    if (onAddHabitAlarm != null) {
                                        onAddHabitAlarm()
                                    } else {
                                        onAddNewAlarm()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Habit alarm",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = "Habit alarm",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111827)
                            )
                        }

                        // Subtle Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF1F5F9))
                        )

                        // Option 2: Quick alarm
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.pulseAppleButtonClick()
                                    isFabMenuExpanded = false
                                    isQuickAlarmOpen = true
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Quick alarm",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Quick alarm",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Card: Alarm
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x33000000))
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            haptics.pulseAppleButtonClick()
                            isFabMenuExpanded = false
                            onAddNewAlarm()
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEEF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Alarm",
                                tint = Color(0xFFFF3B5C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Alarm",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Circular Action Button: (+) or (X)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color(0x66FF3B5C))
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B5C))
                    .clickable {
                        haptics.pulseAppleButtonClick()
                        isFabMenuExpanded = !isFabMenuExpanded
                    }
                    .testTag("add_alarm_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isFabMenuExpanded) "Close options" else "Add alarm",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Quick Alarm Sheet
        if (isQuickAlarmOpen && onSaveQuickAlarm != null) {
            QuickAlarmDialog(
                onDismiss = { isQuickAlarmOpen = false },
                onSaveQuickAlarm = { mins, ringSec, sound, vol, vib ->
                    onSaveQuickAlarm(mins, ringSec, sound, vol, vib)
                    isQuickAlarmOpen = false
                }
            )
        }
    }
}

/**
 * Faithful reproduction of the Alarm Card Component from HTML 1 & Screenshot 1:
 * - Top row: S M T W T F S weekday schedule + Cyan toggle switch
 * - Time row: 7:00 am + mission badge
 * - Footer row: 🐥 Wake up early + 3-dots card menu (⋮)
 */
@Composable
private fun AlarmCardItem(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = rememberAppleHaptics()
    var isMenuOpen by remember { mutableStateOf(false) }

    val daysLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val daysKeys = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    val isDaily = alarm.repeatDays.equals("DAILY", ignoreCase = true)

    val period = if (alarm.hour >= 12) "pm" else "am"
    val displayHour = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val timeFormatted = String.format(Locale.US, "%d:%02d", displayHour, alarm.minute)

    val emoji = if (alarm.title.startsWith("🐥") || alarm.title.startsWith("💊") ||
        alarm.title.startsWith("🏋️") || alarm.title.startsWith("💧") ||
        alarm.title.startsWith("🙏") || alarm.title.startsWith("🧘")) {
        alarm.title.substring(0, 2).trim()
    } else {
        "🐥"
    }

    val displayTitle = if (alarm.title.length > 2 && (alarm.title.startsWith(emoji))) {
        alarm.title.substring(2).trim()
    } else {
        alarm.title.ifBlank { "Wake up early" }
    }

    val isDark = LocalIsDarkMode.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = if (isDark) Color(0x30A78BFA) else Color(0x0A000000))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                haptics.pulseAppleButtonClick()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Repeat Days & Cyan Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weekday Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    daysLetters.forEachIndexed { idx, letter ->
                        val isDayActive = isDaily || alarm.repeatDays.contains(daysKeys[idx], ignoreCase = true)
                        Text(
                            text = letter,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDayActive && alarm.isEnabled) {
                                if (isDark) Color(0xFFF1F5F9) else Color(0xFF374151)
                            } else {
                                if (isDark) Color(0xFF64748B) else Color(0xFFD1D5DB)
                            }
                        )
                    }
                }

                // Cyan iOS-style Toggle Switch (#44C3EB)
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 30.dp)
                        .clip(CircleShape)
                        .background(if (alarm.isEnabled) Color(0xFF44C3EB) else (if (isDark) Color(0xFF334155) else Color(0xFFD1D5DB)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.pulseAppleSelection()
                            onToggle(!alarm.isEnabled)
                        }
                        .padding(3.dp),
                    contentAlignment = if (alarm.isEnabled) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    val offsetAnim by animateFloatAsState(
                        targetValue = if (alarm.isEnabled) 22f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "knob"
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Alarm Time & Mission Badge
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = (-1.5).sp
                )
                Text(
                    text = period,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Mission Indicator Badges
                val missions = alarm.getMissions()
                if (missions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        missions.forEach { mission ->
                            AlarmMissionBadge(
                                mission = mission,
                                isEnabled = alarm.isEnabled
                            )
                        }
                    }
                }
            }

            // Alarm Footer: Chick Emoji + Title & Vertical 3-dots Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                    Text(
                        text = displayTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        letterSpacing = (-0.2).sp
                    )
                }

                Box {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            isMenuOpen = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Alarm options",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                isMenuOpen = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Test (Ring Now)") },
                            onClick = {
                                isMenuOpen = false
                                onTest()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class MissionBadgeConfig(
    val bgColor: Color,
    val tintColor: Color,
    val iconRes: Int? = null,
    val vector: ImageVector? = null
)

@Composable
private fun AlarmMissionBadge(
    mission: DismissType,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current

    val config = when (mission) {
        DismissType.MATH -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x3322C55E) else Color(0xFFEBF8EE),
            tintColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
            iconRes = R.drawable.ic_mission_math
        )
        DismissType.SHAKE -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x33F59E0B) else Color(0xFFFFF4E6),
            tintColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            iconRes = R.drawable.ic_mission_shake
        )
        DismissType.STEP -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x33F97316) else Color(0xFFFEF2EE),
            tintColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
            iconRes = R.drawable.ic_mission_step_exact
        )
        DismissType.QR_CODE -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x33A855F7) else Color(0xFFF5EEFE),
            tintColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
            iconRes = R.drawable.ic_mission_qr_exact
        )
        DismissType.TYPING -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x338B5CF6) else Color(0xFFF3EEFD),
            tintColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
            iconRes = R.drawable.ic_mission_typing
        )
        DismissType.COLOR_TILES -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x3322C55E) else Color(0xFFE8F5E9),
            tintColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32),
            iconRes = R.drawable.ic_mission_colortiles
        )
        DismissType.OFF -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x3364748B) else Color(0xFFF1F5F9),
            tintColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            iconRes = R.drawable.ic_mission_off
        )
        else -> MissionBadgeConfig(
            bgColor = if (isDark) Color(0x3394A3B8) else Color(0xFFF1F5F9),
            tintColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
            iconRes = R.drawable.ic_mission_off
        )
    }

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isEnabled) config.bgColor else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)))
            .testTag("alarm_mission_badge_${mission.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        if (config.iconRes != null) {
            Icon(
                painter = painterResource(id = config.iconRes),
                contentDescription = mission.title,
                tint = if (isEnabled) Color.Unspecified else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                modifier = Modifier.size(15.dp)
            )
        } else if (config.vector != null) {
            Icon(
                imageVector = config.vector,
                contentDescription = mission.title,
                tint = if (isEnabled) config.tintColor else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

