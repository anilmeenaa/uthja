package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmEventEntity
import com.example.data.model.SleepSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WeeklySleepPatternCalculator
import com.example.ui.components.AppText
import com.example.ui.components.AppTextTone
import com.example.ui.components.AppTextVariant
import com.example.ui.components.CardTone
import com.example.ui.components.GradientBlobTone
import com.example.ui.components.LuneButton
import com.example.ui.components.LuneButtonSize
import com.example.ui.components.LuneButtonVariant
import com.example.ui.components.LuneCard
import com.example.ui.components.LuneSectionHeader
import com.example.ui.components.SleepDurationRechartsView
import com.example.ui.components.WaveBackground
import com.example.ui.theme.LuneColors

@Composable
fun AnalyticsScreen(
    alarmEvents: List<AlarmEventEntity>,
    alarms: List<AlarmEntity> = emptyList(),
    sleepSessions: List<SleepSessionEntity> = emptyList(),
    userProfile: UserProfileEntity?,
    onOpenFullVisualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    var isNotified by remember { mutableStateOf(false) }

    val weeklySummary = remember(alarmEvents, alarms, sleepSessions, userProfile) {
        WeeklySleepPatternCalculator.computeWeeklySummary(
            alarmEvents = alarmEvents,
            alarms = alarms,
            sleepSessions = sleepSessions,
            userProfile = userProfile,
            weekOffset = 0
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stats_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    WaveBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Header with Live Recharts Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = "ANALYTICS",
                            variant = AppTextVariant.CAPTION,
                            tone = AppTextTone.SECONDARY
                        )
                        AppText(
                            text = "Stats & Insights",
                            variant = AppTextVariant.TITLE,
                            tone = AppTextTone.DEFAULT
                        )
                    }

                    // Recharts Live Badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF3EFFE))
                            .border(1.dp, Color(0xFFDDD2FB), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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
                            AppText(
                                text = "Recharts Live",
                                variant = AppTextVariant.CAPTION,
                                tone = AppTextTone.ACCENT
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                AppText(
                    text = "Weekly summary of sleep duration patterns based on alarm history.",
                    variant = AppTextVariant.BODY,
                    tone = AppTextTone.MUTED
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Live Recharts Data Visualization Card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SleepDurationRechartsView(
                        summary = weeklySummary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Open Full-screen visualizer CTA
                    LuneButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            onOpenFullVisualizer()
                        },
                        variant = LuneButtonVariant.SECONDARY,
                        size = LuneButtonSize.MD,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            AppText(
                                text = "View Detailed Pattern Analysis",
                                variant = AppTextVariant.LABEL,
                                tone = AppTextTone.ACCENT
                            )
                        }
                    }
                }
            }

            // Preview Highlights Section
            item {
                LuneSectionHeader(
                    title = "Upcoming Analytics"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatsPreviewCard(
                        icon = Icons.Filled.Timeline,
                        iconBg = Color(0xFFE8F6F1),
                        iconColor = Color(0xFF2C9C72),
                        title = "Circadian Rhythm Mapping",
                        description = "Visual charts of your daily biological energy curves, wake consistency, and bedtime habits."
                    )

                    StatsPreviewCard(
                        icon = Icons.Filled.Bolt,
                        iconBg = Color(0xFFFFEDE6),
                        iconColor = Color(0xFFF67453),
                        title = "Anti-Snooze Consistency",
                        description = "Track your wake-up mission speed, difficulty progressions, and unbroken streak days."
                    )

                    StatsPreviewCard(
                        icon = Icons.Filled.WatchLater,
                        iconBg = Color(0xFFEFE8FD),
                        iconColor = Color(0xFF6B4EF9),
                        title = "Weekly Sleep Debt",
                        description = "Identify cumulative fatigue trends and balance optimal recovery hours effortlessly."
                    )

                    StatsPreviewCard(
                        icon = Icons.Filled.EmojiEvents,
                        iconBg = Color(0xFFFFF7DC),
                        iconColor = Color(0xFFD4971A),
                        title = "Milestone Achievements",
                        description = "Earn badges for 7-day zero-snooze streaks, dawn wake-ups, and cognitive mission mastery."
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StatsPreviewCard(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, LuneColors.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    variant = AppTextVariant.LABEL,
                    tone = AppTextTone.DEFAULT
                )
                Spacer(modifier = Modifier.height(2.dp))
                AppText(
                    text = description,
                    variant = AppTextVariant.CAPTION,
                    tone = AppTextTone.SECONDARY
                )
            }
        }
    }
}
