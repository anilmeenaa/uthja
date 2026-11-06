package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Quick Alarm Modal matching HTML 5 and Screenshot 5:
 * - Dynamic minute accumulation (+ 0m) with reset button
 * - Ring at hh:mm a preview
 * - 2x3 grid of preset pills (1 min, 5 min, 10 min, 15 min, 30 min, 1 hours)
 * - Alarm sound selector ("Orkney >")
 * - Volume slider and cyan vibration toggle
 * - Coral/Red Save button (#FF3B4E)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAlarmDialog(
    onDismiss: () -> Unit,
    onSaveQuickAlarm: (minutes: Int, ringDurationSeconds: Int, sound: String, volume: Float, vibrate: Boolean) -> Unit
) {
    val haptics = rememberAppleHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var addedMinutes by remember { mutableIntStateOf(0) }
    var ringDurationSeconds by remember { mutableIntStateOf(5) } // Default 5 sec
    var soundName by remember { mutableStateOf("Birds") } // Only Birds sound from Nature
    var volume by remember { mutableFloatStateOf(0.42f) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    val targetCalendar = remember(addedMinutes) {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, addedMinutes)
        }
    }
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val ringTimeStr = timeFormat.format(targetCalendar.time)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Title & Close 'X' Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quick alarm",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E232A),
                    letterSpacing = (-0.2).sp
                )
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Center Display: Minutes Counter & Reset Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(end = 4.dp, bottom = 12.dp)
                )
                Text(
                    text = "$addedMinutes",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF374151),
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "m",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(start = 2.dp, top = 16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        addedMinutes = 0
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset timer",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Ring Time Preview Text
            Text(
                text = "Ring at $ringTimeStr",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            // Preset Grid: 2 rows x 3 columns
            val presets = listOf(
                1 to "1 min",
                5 to "5 min",
                10 to "10 min",
                15 to "15 min",
                30 to "30 min",
                60 to "1 hours"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (rowIndex in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (colIndex in 0..2) {
                            val item = presets[rowIndex * 3 + colIndex]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable {
                                        haptics.pulseAppleButtonClick()
                                        addedMinutes += item.first
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.second,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm Sound Row (Locked to Nature Birds per specification)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarm sound",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1F2937)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "🌿 Birds (Nature)",
                        fontSize = 13.5.sp,
                        color = Color(0xFF0D9488),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ring Duration Row (5 sec or 10 sec, default 5 sec)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ring duration",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1F2937)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5 to "5 sec", 10 to "10 sec").forEach { (sec, label) ->
                        val isSelected = ringDurationSeconds == sec
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .clickable {
                                    haptics.pulseAppleSelection()
                                    ringDurationSeconds = sec
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("quick_alarm_ring_duration_${sec}s"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume & Vibration Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(18.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF20242A),
                        activeTrackColor = Color(0xFF20242A),
                        inactiveTrackColor = Color(0xFFEDF0F3)
                    )
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFFE5E7EB))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Vibration",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(18.dp)
                    )
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            haptics.pulseAppleSelection()
                            vibrationEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF38A7CA),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        ),
                        modifier = Modifier.size(width = 46.dp, height = 26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Action Button (Component 3: Primary Action Button #FF3B5C)
            Button(
                onClick = {
                    haptics.pulseAppleSuccess()
                    val finalMinutes = if (addedMinutes <= 0) 5 else addedMinutes
                    onSaveQuickAlarm(finalMinutes, ringDurationSeconds, "Birds", volume, vibrationEnabled)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x66FF3B5C))
                    .testTag("save_quick_alarm_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B5C),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Save",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
