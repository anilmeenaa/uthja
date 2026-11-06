package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.DailySleepPatternItem
import com.example.data.model.SleepSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WeeklySleepPatternCalculator
import com.example.ui.components.SleepDurationRechartsView

@Composable
fun SleepDurationVisualizerScreen(
    alarmEvents: List<AlarmEventEntity>,
    alarms: List<AlarmEntity>,
    sleepSessions: List<SleepSessionEntity>,
    userProfile: UserProfileEntity?,
    onBack: () -> Unit,
    onAdjustAlarm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val haptics = rememberAppleHaptics()

    var weekOffset by remember { mutableIntStateOf(0) } // 0 = current week, -1 = last week
    var selectedDayIndex by remember { mutableIntStateOf(6) }

    val weeklySummary = remember(alarmEvents, alarms, sleepSessions, userProfile, weekOffset) {
        WeeklySleepPatternCalculator.computeWeeklySummary(
            alarmEvents = alarmEvents,
            alarms = alarms,
            sleepSessions = sleepSessions,
            userProfile = userProfile,
            weekOffset = weekOffset
        )
    }

    val selectedDay = weeklySummary.days.getOrNull(selectedDayIndex) ?: weeklySummary.days.lastOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Back button + Title + Recharts pill
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptics.pulseAppleButtonClick()
                                onBack()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                .testTag("recharts_screen_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E232A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Sleep Duration Patterns",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "Weekly summary from alarm history",
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Recharts Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3EFFE))
                            .border(1.dp, Color(0xFFDDD2FB), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = null,
                                tint = Color(0xFF785CD8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Recharts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF785CD8)
                            )
                        }
                    }
                }
            }

            // Week Selector Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            weekOffset -= 1
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous week",
                            tint = Color(0xFF475569)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = weeklySummary.weekRangeText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (weekOffset == 0) "Current Week" else "${Math.abs(weekOffset)} week(s) ago",
                            fontSize = 11.5.sp,
                            color = if (weekOffset == 0) Color(0xFF785CD8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = {
                            if (weekOffset < 0) {
                                haptics.pulseAppleButtonClick()
                                weekOffset += 1
                            }
                        },
                        enabled = weekOffset < 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next week",
                            tint = if (weekOffset < 0) Color(0xFF475569) else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Summary KPI Cards Row (4 Metric Pills)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Avg Sleep",
                        value = weeklySummary.avgDurationFormatted,
                        subtitle = "Target ${weeklySummary.targetGoalHours.toInt()}h",
                        icon = Icons.Filled.Bedtime,
                        iconTint = Color(0xFF785CD8),
                        modifier = Modifier.weight(1f)
                    )

                    val isSurplus = weeklySummary.totalSleepDebtHours >= 0
                    val debtSign = if (isSurplus) "+" else ""
                    MetricCard(
                        title = "Sleep Debt",
                        value = "$debtSign${weeklySummary.totalSleepDebtHours.format(1)}h",
                        subtitle = if (isSurplus) "Surplus" else "Deficit",
                        icon = Icons.Filled.Schedule,
                        iconTint = if (isSurplus) Color(0xFF10B981) else Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Consistency",
                        value = "${weeklySummary.wakeConsistencyPercent}%",
                        subtitle = "Circadian Score",
                        icon = Icons.Filled.Bolt,
                        iconTint = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Wake Speed",
                        value = "${weeklySummary.avgDismissSeconds}s",
                        subtitle = "Avg Mission Time",
                        icon = Icons.Filled.Timer,
                        iconTint = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Visualization: Embedded Recharts View Card
            item {
                SleepDurationRechartsView(
                    summary = weeklySummary,
                    onDaySelected = { idx ->
                        selectedDayIndex = idx
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Correlated Alarm History Insights Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9))))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDF4FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFC026D3),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Text(
                                text = "Alarm & Sleep Pattern Insights",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        InsightRow(
                            emoji = "⚡",
                            title = "Mission Reaction Time",
                            desc = "Fastest alarm conquer was on ${weeklySummary.fastestWakeDay} (${weeklySummary.days.minByOrNull { it.dismissDelaySeconds }?.dismissDelaySeconds ?: 20}s response). Wake challenges cut snooze procrastination by 100%."
                        )

                        InsightRow(
                            emoji = "🌙",
                            title = "Sleep Duration Peak",
                            desc = "Longest restorative sleep was on ${weeklySummary.highestSleepDay} (${weeklySummary.days.maxByOrNull { it.durationHours }?.durationFormatted ?: "8h"})."
                        )

                        InsightRow(
                            emoji = "🎯",
                            title = "Circadian Recommendation",
                            desc = if (weeklySummary.totalSleepDebtHours < -2f) {
                                "Cumulative sleep debt is ${Math.abs(weeklySummary.totalSleepDebtHours).format(1)} hours this week. Consider shifting bedtime 30 minutes earlier to hit your ${weeklySummary.targetGoalHours.toInt()}h goal."
                            } else {
                                "Excellent circadian consistency! Your wake-up alarms align cleanly with restorative 90-minute REM cycles."
                            }
                        )
                    }
                }
            }

            // Day-by-Day Alarm & Sleep Breakdown List
            item {
                Text(
                    text = "Weekly Breakdown by Day",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            itemsIndexed(weeklySummary.days) { index, item ->
                DailyPatternCard(
                    item = item,
                    isSelected = selectedDayIndex == index,
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        selectedDayIndex = index
                    }
                )
            }

            // Bottom spacer for comfortable scrolling
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun InsightRow(
    emoji: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DailyPatternCard(
    item: DailySleepPatternItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (item.status) {
        "Optimal" -> Color(0xFF10B981)
        "Recovery" -> Color(0xFF785CD8)
        "Mild Deficit" -> Color(0xFF0284C7)
        else -> Color(0xFFF97316)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF785CD8) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF9F7FE) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF785CD8) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.dayOfWeek,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }

                    Column {
                        Text(
                            text = item.fullDate,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Bed ${item.bedtime} ➔ Alarm ${item.wakeAlarmTime}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.durationFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Progress Bar relative to target goal
            val progress = (item.durationHours / (item.targetHours * 1.25f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color(0xFFF1F5F9)
            )

            // Alarm Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.alarmName} • ${item.alarmDismissType}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = "Solved in ${item.dismissDelaySeconds}s",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF059669)
                )
            }
        }
    }
}

private fun Float.format(digits: Int): String {
    return String.format(java.util.Locale.getDefault(), "%.${digits}f", this)
}
