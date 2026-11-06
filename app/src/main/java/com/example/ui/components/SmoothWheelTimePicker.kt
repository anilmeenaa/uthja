package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LuneColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * High-precision, ultra-smooth scrolling tumbler wheel time picker.
 * Features:
 * - Magnetic snap-to-item physics
 * - Seamless infinite circular scrolling on hours & minutes
 * - Centered glowing selection lens with depth gradient fades
 * - Native tactile haptic ticks on item transitions
 * - AM / PM high-contrast tactile selector pill
 * - Quick-tap increment/decrement buttons & quick-time preset chips
 */
@Composable
fun SmoothWheelTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChange: (newHour24: Int, newMinute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayHour = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val isPm = hour24 >= 12

    val itemHeight = 48.dp
    val visibleItemsCount = 3
    val totalHeight = itemHeight * visibleItemsCount

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // 12 hours list (1..12)
    val hoursList = remember { (1..12).toList() }
    // 60 minutes list (0..59)
    val minutesList = remember { (0..59).toList() }

    // Multipliers for infinite wrap-around feel
    val repeatCount = 200
    val hourInitialIndex = remember(displayHour) {
        (repeatCount / 2) * 12 + (displayHour - 1)
    }
    val minuteInitialIndex = remember(minute) {
        (repeatCount / 2) * 60 + minute
    }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = hourInitialIndex)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = minuteInitialIndex)

    // Calculate currently centered item for hours
    val selectedDisplayHour by remember {
        derivedStateOf {
            val idx = hourListState.firstVisibleItemIndex
            val offset = hourListState.firstVisibleItemScrollOffset
            val adjusted = if (offset > 48) idx + 1 else idx
            val raw = adjusted % 12
            hoursList[raw]
        }
    }

    // Calculate currently centered item for minutes
    val selectedMinute by remember {
        derivedStateOf {
            val idx = minuteListState.firstVisibleItemIndex
            val offset = minuteListState.firstVisibleItemScrollOffset
            val adjusted = if (offset > 48) idx + 1 else idx
            val raw = adjusted % 60
            minutesList[raw]
        }
    }

    // Emit haptic & notify changes when user scrolls hours
    LaunchedEffect(hourListState) {
        snapshotFlow { selectedDisplayHour }
            .distinctUntilChanged()
            .drop(1)
            .collect { newDisplayHour ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val computed24 = if (isPm) {
                    if (newDisplayHour == 12) 12 else newDisplayHour + 12
                } else {
                    if (newDisplayHour == 12) 0 else newDisplayHour
                }
                onTimeChange(computed24, minute)
            }
    }

    // Emit haptic & notify changes when user scrolls minutes
    LaunchedEffect(minuteListState) {
        snapshotFlow { selectedMinute }
            .distinctUntilChanged()
            .drop(1)
            .collect { newMin ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTimeChange(hour24, newMin)
            }
    }

    // Outer Container Card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.5.dp, LuneColors.borderLight, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick instruction / readout bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = LuneColors.primaryDeep,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SWIPE OR SCROLL SMOOTHLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuneColors.primaryDeep,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Current Time Indicator Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LuneColors.surfaceLavender
            ) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d %s", displayHour, minute, if (isPm) "PM" else "AM"),
                    style = MaterialTheme.typography.labelMedium,
                    color = LuneColors.primaryDeep,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Main Scrolling Wheel Drum Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, LuneColors.border.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Lens Highlight Bar across the center row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LuneColors.surfaceLavender.copy(alpha = 0.85f))
                    .border(1.dp, LuneColors.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            )

            // 3 Columns: Hour Wheel | Separator | Minute Wheel | AM/PM Switch
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 1. Hours Wheel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight),
                    contentAlignment = Alignment.Center
                ) {
                    WheelDrum(
                        listState = hourListState,
                        totalItems = hoursList.size * repeatCount,
                        itemHeight = itemHeight,
                        visibleCount = visibleItemsCount,
                        getItemText = { idx ->
                            val h = hoursList[idx % 12]
                            String.format(Locale.US, "%02d", h)
                        },
                        onItemClick = { clickedIdx ->
                            coroutineScope.launch {
                                hourListState.animateScrollToItem(clickedIdx)
                            }
                        },
                        testTag = "wheel_hours"
                    )
                }

                // 2. Colon Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = LuneColors.primaryDeep,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // 3. Minutes Wheel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight),
                    contentAlignment = Alignment.Center
                ) {
                    WheelDrum(
                        listState = minuteListState,
                        totalItems = minutesList.size * repeatCount,
                        itemHeight = itemHeight,
                        visibleCount = visibleItemsCount,
                        getItemText = { idx ->
                            val m = minutesList[idx % 60]
                            String.format(Locale.US, "%02d", m)
                        },
                        onItemClick = { clickedIdx ->
                            coroutineScope.launch {
                                minuteListState.animateScrollToItem(clickedIdx)
                            }
                        },
                        testTag = "wheel_minutes"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 4. AM / PM Smooth Vertical Selector
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .height(totalHeight - 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LuneColors.surface)
                        .border(1.dp, LuneColors.border, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AM Button
                    val amSelected = !isPm
                    val amBg by animateColorAsState(
                        targetValue = if (amSelected) LuneColors.primary else Color.Transparent,
                        label = "amBg"
                    )
                    val amText by animateColorAsState(
                        targetValue = if (amSelected) Color.White else LuneColors.textSoft,
                        label = "amText"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(amBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isPm) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newH = (hour24 - 12).coerceAtLeast(0)
                                    onTimeChange(newH, minute)
                                }
                            }
                    ) {
                        Text(
                            text = "AM",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (amSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = amText
                        )
                    }

                    // PM Button
                    val pmSelected = isPm
                    val pmBg by animateColorAsState(
                        targetValue = if (pmSelected) LuneColors.primary else Color.Transparent,
                        label = "pmBg"
                    )
                    val pmText by animateColorAsState(
                        targetValue = if (pmSelected) Color.White else LuneColors.textSoft,
                        label = "pmText"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pmBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isPm) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newH = (hour24 + 12).coerceAtMost(23)
                                    onTimeChange(newH, minute)
                                }
                            }
                    ) {
                        Text(
                            text = "PM",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (pmSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = pmText
                        )
                    }
                }
            }

            // Top Gradient Shadow overlay for cylindrical 3D depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFF8FAFC).copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            // Bottom Gradient Shadow overlay for cylindrical 3D depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFC).copy(alpha = 0.0f),
                                Color(0xFFF8FAFC)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Preset Adjust Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresetTimeChip(
                label = "6:30 AM",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTimeChange(6, 30)
                    coroutineScope.launch {
                        val targetH = (repeatCount / 2) * 12 + 5
                        val targetM = (repeatCount / 2) * 60 + 30
                        hourListState.animateScrollToItem(targetH)
                        minuteListState.animateScrollToItem(targetM)
                    }
                }
            )

            PresetTimeChip(
                label = "7:00 AM",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTimeChange(7, 0)
                    coroutineScope.launch {
                        val targetH = (repeatCount / 2) * 12 + 6
                        val targetM = (repeatCount / 2) * 60
                        hourListState.animateScrollToItem(targetH)
                        minuteListState.animateScrollToItem(targetM)
                    }
                }
            )

            PresetTimeChip(
                label = "7:30 AM",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTimeChange(7, 30)
                    coroutineScope.launch {
                        val targetH = (repeatCount / 2) * 12 + 6
                        val targetM = (repeatCount / 2) * 60 + 30
                        hourListState.animateScrollToItem(targetH)
                        minuteListState.animateScrollToItem(targetM)
                    }
                }
            )

            PresetTimeChip(
                label = "+15m",
                modifier = Modifier.weight(0.9f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val newTotalMin = (hour24 * 60 + minute + 15) % (24 * 60)
                    val newH = newTotalMin / 60
                    val newM = newTotalMin % 60
                    onTimeChange(newH, newM)
                    coroutineScope.launch {
                        val curM = minuteListState.firstVisibleItemIndex
                        minuteListState.animateScrollToItem(curM + 15)
                    }
                }
            )

            PresetTimeChip(
                label = "+30m",
                modifier = Modifier.weight(0.9f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val newTotalMin = (hour24 * 60 + minute + 30) % (24 * 60)
                    val newH = newTotalMin / 60
                    val newM = newTotalMin % 60
                    onTimeChange(newH, newM)
                    coroutineScope.launch {
                        val curM = minuteListState.firstVisibleItemIndex
                        minuteListState.animateScrollToItem(curM + 30)
                    }
                }
            )
        }
    }
}

/**
 * Internal single tumbler drum column with snap-fling physics.
 */
@Composable
private fun WheelDrum(
    listState: LazyListState,
    totalItems: Int,
    itemHeight: Dp,
    visibleCount: Int,
    getItemText: (Int) -> String,
    onItemClick: (Int) -> Unit,
    testTag: String
) {
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current

    val itemHeightPx = with(density) { itemHeight.toPx() }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag)
    ) {
        items(
            count = totalItems,
            key = { it }
        ) { index ->
            val isCenter by remember {
                derivedStateOf {
                    val currentIndex = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    val centerIndex = if (offset > itemHeightPx / 2) currentIndex + 1 else currentIndex
                    index == centerIndex
                }
            }

            val scale by animateFloatAsState(
                targetValue = if (isCenter) 1.15f else 0.82f,
                animationSpec = tween(120),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCenter) 1.0f else 0.35f,
                animationSpec = tween(120),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onItemClick(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getItemText(index),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCenter) LuneColors.primaryDeep else LuneColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                )
            }
        }
    }
}

@Composable
private fun PresetTimeChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LuneColors.surfaceSoft)
            .border(1.dp, LuneColors.borderLight, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = LuneColors.textSoft
        )
    }
}
