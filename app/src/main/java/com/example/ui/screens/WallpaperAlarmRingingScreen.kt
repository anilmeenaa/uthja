package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.DismissType
import com.example.data.model.WallpaperCatalog
import com.example.ui.ActiveAlarmState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Wallpaper Alarm Ringing Screen:
 * Displays the selected alarm wallpaper (preset or user album photo) when the alarm rings!
 * Shows:
 * - Live ringing digital clock time (e.g. 07:00 AM)
 * - Current date (e.g. Tuesday, October 6)
 * - Alarm title & audio sound tag (e.g. ♪ Meow Alarm)
 * - Selected wake-up challenge badge
 * - Prominent "Start Mission" button that transitions directly to the challenge
 * - Anti-cheat lockdown (hardware buttons & back button blocked)
 */
@Composable
fun WallpaperAlarmRingingScreen(
    state: ActiveAlarmState,
    onStartMission: () -> Unit,
    onAttemptExitBlocked: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    onAttemptSnooze: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    val wallpaperItem = remember(state.wallpaper) {
        WallpaperCatalog.findById(state.wallpaper)
    }

    // Live clock ticker
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date()))
    }
    var currentAmPm by remember {
        mutableStateOf(SimpleDateFormat("a", Locale.getDefault()).format(Date()))
    }
    var currentDateString by remember {
        mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTimeString = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
            currentAmPm = SimpleDateFormat("a", Locale.getDefault()).format(now)
            currentDateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
            kotlinx.coroutines.delay(1000L)
        }
    }

    // Subtle pulsing animation on the Start Mission button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    // Lock back button while alarm is ringing
    BackHandler {
        haptics.pulseAppleButtonClick()
        if (state.isQuickAlarm) {
            onStartMission() // Direct dismiss for quick alarm
        } else {
            onAttemptExitBlocked()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("wallpaper_alarm_ringing_screen")
    ) {
        // 1. FULL-SCREEN WALLPAPER BACKGROUND
        if (wallpaperItem.customUri != null) {
            val imageModel = remember(wallpaperItem.customUri) {
                ImageRequest.Builder(context)
                    .data(android.net.Uri.parse(wallpaperItem.customUri))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageModel,
                contentDescription = "Alarm Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (wallpaperItem.drawableRes != null) {
            Image(
                painter = painterResource(id = wallpaperItem.drawableRes),
                contentDescription = wallpaperItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = wallpaperItem.gradientColors))
            )
        }

        // Gradient Scrim for 100% crisp legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // 2. FOREGROUND CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Alarm Title + Mute / Audio button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alarm Title Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.title.ifBlank { "Lune Wake-Up" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Audio Mute/Unmute toggle
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        onToggleMute?.invoke()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFF87171) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center Area: Digital Clock Time, AM/PM, Date, Sound Badge & Quote
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Sound Tag (e.g. ♪ Meow Alarm)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = state.sound,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Digital Clock
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTimeString,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentAmPm.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                // Full Formatted Date
                Text(
                    text = currentDateString,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Wallpaper Quote if available and not already part of the artwork
                if (wallpaperItem.quote != null && wallpaperItem.drawableRes == null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = wallpaperItem.quote,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Wake-Up Mission Indicator Badge
                val missionLabel = if (state.isQuickAlarm) {
                    val remaining = (state.quickRingDurationSeconds - state.elapsedSeconds).coerceAtLeast(0)
                    "⚡ Quick Alarm · Rings for ${state.quickRingDurationSeconds}s (${remaining}s left)"
                } else if (state.totalMissions > 1) {
                    "${state.totalMissions} Missions: " + state.missions.joinToString(" + ") { it.title }
                } else {
                    when (state.currentMission) {
                        DismissType.MATH -> "Math Challenge"
                        DismissType.COLOR_TILES -> "Color Tiles Challenge"
                        DismissType.TYPING -> "Typing Alert"
                        DismissType.SHAKE -> "Physical Shake Challenge"
                        DismissType.STEP -> "Step Challenge"
                        DismissType.QR_CODE -> "QR / Barcode Challenge"
                        DismissType.BOTH -> "Math & Shake Challenge"
                        DismissType.OFF -> "Normal Wake-up"
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (state.isQuickAlarm) Color(0xFF0D9488).copy(alpha = 0.90f) else Color(0xFF6366F1).copy(alpha = 0.85f))
                        .border(1.dp, if (state.isQuickAlarm) Color(0xFF5EEAD4).copy(alpha = 0.6f) else Color(0xFFA5B4FC).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (state.isQuickAlarm) missionLabel else "🎯 Wake-up: $missionLabel",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (state.isQuickAlarm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🌿 Birds Sound (Nature) Playing",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                } else if (state.gradualVolume || state.gentleMissionVolume) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (state.gradualVolume) "🌿 Gentle Wake-Up • Gradual Fade-In Active" else "🌿 Gentle Mission Audio Active",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBAE6FD)
                        )
                    }
                }
            }

            // Bottom Area: Prominent "Start Mission" / "Dismiss Quick Alarm" Button + Snooze + Anti-cheat note
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // START MISSION / DISMISS BUTTON
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (state.isQuickAlarm) {
                                    listOf(
                                        Color(0xFF0F766E),
                                        Color(0xFF0D9488),
                                        Color(0xFF14B8A6)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF0284C7),
                                        Color(0xFF38BDF8),
                                        Color(0xFF6366F1)
                                    )
                                }
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                        .clickable {
                            haptics.pulseSuccess()
                            onStartMission()
                        }
                        .testTag(if (state.isQuickAlarm) "dismiss_quick_alarm_button" else "start_mission_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isQuickAlarm) Icons.Default.Check else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (state.isQuickAlarm) "Dismiss Quick Alarm" else "Start Mission",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Optional Emergency Snooze Button
                if (!state.isQuickAlarm && onAttemptSnooze != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Snooze 5 min",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable {
                                haptics.pulseAppleButtonClick()
                                onAttemptSnooze()
                            }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Anti-Cheat & Prevent Power-Off indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.isQuickAlarm) "⚡ Quick Alarm · No missions required • Auto-stops in ${state.quickRingDurationSeconds}s" else "Power-off & Exit Protected · Complete mission to dismiss",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
