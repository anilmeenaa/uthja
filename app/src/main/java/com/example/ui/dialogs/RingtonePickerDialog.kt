package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.SoundManager
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.RingtoneCatalog
import com.example.data.model.RingtoneCategory
import com.example.data.model.RingtoneItem

@Composable
fun RingtonePickerDialog(
    selectedSoundName: String,
    onRingtoneSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    val scope = rememberCoroutineScope()
    val soundManager = remember { SoundManager(context) }

    var currentSelected by remember { mutableStateOf(selectedSoundName) }
    var selectedCategory by remember { mutableStateOf(RingtoneCategory.ALL) }
    var previewingSoundName by remember { mutableStateOf<String?>(null) }

    BackHandler {
        soundManager.stopVibePreview()
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopVibePreview()
        }
    }

    val filteredRingtones = remember(selectedCategory) {
        if (selectedCategory == RingtoneCategory.ALL) {
            RingtoneCatalog.ALL_RINGTONES
        } else {
            RingtoneCatalog.ALL_RINGTONES.filter { it.category == selectedCategory }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("ringtone_picker_screen"),
        color = Color(0xFFF7F3F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. TOP APP BAR WITH CLEAR HEADING
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            soundManager.stopVibePreview()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("ringtone_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E1E24),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Alarm Ringtone",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1A24)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE9FE))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredRingtones.size} Ringtones",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }
            }

            // 2. PROMINENT PAGE TITLE & DESCRIPTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Big Bold Heading
                Text(
                    text = "Choose Alarm Ringtone",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1A24),
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Informative Subtitle
                Text(
                    text = "Select your wake-up alarm sound, melodic chime, or viral meme audio.",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6E6877),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Currently Active Ringtone Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (RingtoneCatalog.isMemeRingtone(currentSelected)) Color(0xFFEDE9FE) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (RingtoneCatalog.isMemeRingtone(currentSelected)) Color(0xFF7C3AED) else Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Currently selected",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentSelected,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        if (RingtoneCatalog.isMemeRingtone(currentSelected)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEDE9FE))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "👾 Viral Meme",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RingtoneCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    val label = when (category) {
                        RingtoneCategory.ALL -> "🔔 All"
                        RingtoneCategory.MEME -> "👾 Meme (2)"
                        RingtoneCategory.MELODIC -> "🎵 Melodic"
                        RingtoneCategory.NATURE -> "🌿 Nature"
                        RingtoneCategory.LOUD -> "🚨 Alert"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    if (category == RingtoneCategory.MEME) Color(0xFF7C3AED) else Color(0xFF0F172A)
                                } else Color.White
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                haptics.pulseAppleSelection()
                                selectedCategory = category
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            // Ringtones List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Meme Spotlight Banner if on ALL or MEME tab
                if (selectedCategory == RingtoneCategory.ALL || selectedCategory == RingtoneCategory.MEME) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        MemeSpotlightBanner()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                items(filteredRingtones, key = { it.id }) { item ->
                    val isSelected = item.name.equals(currentSelected, ignoreCase = true) ||
                            (item.isMeme && currentSelected.contains(item.name.take(8), ignoreCase = true))
                    val isPreviewing = previewingSoundName == item.name

                    RingtoneRowCard(
                        item = item,
                        isSelected = isSelected,
                        isPreviewing = isPreviewing,
                        onSelect = {
                            haptics.pulseAppleSelection()
                            currentSelected = item.name
                        },
                        onTogglePreview = {
                            if (isPreviewing) {
                                soundManager.stopVibePreview()
                                previewingSoundName = null
                            } else {
                                haptics.pulseAppleButtonClick()
                                previewingSoundName = item.name
                                soundManager.playRingtonePreview(item, scope) {
                                    if (previewingSoundName == item.name) {
                                        previewingSoundName = null
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Confirm Button
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Ringtone",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentSelected,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                if (RingtoneCatalog.isMemeRingtone(currentSelected)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEDE9FE))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "👾 Meme",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            haptics.pulseAppleSuccess()
                            soundManager.stopVibePreview()
                            onRingtoneSelected(currentSelected)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_ringtone_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (RingtoneCatalog.isMemeRingtone(currentSelected)) {
                                Color(0xFF7C3AED)
                            } else {
                                Color(0xFF0F172A)
                            }
                        )
                    ) {
                        Text(
                            text = "Set as Alarm Ringtone",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemeSpotlightBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFD946EF)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🔥 TRENDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "👾 Viral Meme Ringtones",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High-energy wake-up meme dialogues guaranteed to get you out of bed immediately!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RingtoneRowCard(
    item: RingtoneItem,
    isSelected: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit
) {
    val borderColor = if (isSelected) {
        if (item.isMeme) Color(0xFF8B5CF6) else Color(0xFF0F172A)
    } else {
        Color(0xFFE2E8F0)
    }

    val backgroundColor = if (isSelected) {
        if (item.isMeme) Color(0xFFFAF5FF) else Color(0xFFF8FAFC)
    } else {
        Color.White
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            borderColor
        ),
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("ringtone_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Play/Stop Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPreviewing) {
                            if (item.isMeme) Color(0xFF7C3AED) else Color(0xFF0F172A)
                        } else {
                            if (item.isMeme) Color(0xFFF3E8FF) else Color(0xFFF1F5F9)
                        }
                    )
                    .clickable { onTogglePreview() }
                    .testTag("preview_button_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPreviewing) "Stop" else "Preview",
                    tint = if (isPreviewing) Color.White else (if (item.isMeme) Color(0xFF7C3AED) else Color(0xFF334155)),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: Title, Meme Tag, Subtitle & Wave Animation
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Tag Pill: 👾 Meme or Category Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (item.category) {
                                    RingtoneCategory.MEME -> Color(0xFFEDE9FE)
                                    RingtoneCategory.MELODIC -> Color(0xFFE0F2FE)
                                    RingtoneCategory.NATURE -> Color(0xFFDCFCE7)
                                    RingtoneCategory.LOUD -> Color(0xFFFEE2E2)
                                    else -> Color(0xFFF1F5F9)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.category) {
                                RingtoneCategory.MEME -> Color(0xFF7C3AED)
                                RingtoneCategory.MELODIC -> Color(0xFF0284C7)
                                RingtoneCategory.NATURE -> Color(0xFF16A34A)
                                RingtoneCategory.LOUD -> Color(0xFFDC2626)
                                else -> Color(0xFF475569)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )

                // Animated audio wave when previewing
                AnimatedVisibility(visible = isPreviewing) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        EqualizerWaveBars(isMeme = item.isMeme)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Playing preview...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (item.isMeme) Color(0xFF7C3AED) else Color(0xFF0284C7)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Radio Checkmark
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            if (item.isMeme) Color(0xFF7C3AED) else Color(0xFF0F172A)
                        } else Color(0xFFF1F5F9)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else Color(0xFFCBD5E1),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerWaveBars(isMeme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val barColor = if (isMeme) Color(0xFF7C3AED) else Color(0xFF0284C7)

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(1.5.dp)).background(barColor))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).clip(RoundedCornerShape(1.5.dp)).background(barColor))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).clip(RoundedCornerShape(1.5.dp)).background(barColor))
    }
}
