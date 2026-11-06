package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity

/**
 * "Today's plan" task card reproduction matching the user's uploaded design.
 * Features:
 * - "Today's plan" header with delicate pink star sparkle and optional manage navigation
 * - Capsule badge showing "1/3 done" with dynamic circular progress border
 * - Dynamically renders the user's tasks stored in Room database (or default presets)
 * - Interactive checkmark toggles with Apple Taptic feedback synced across Home and Tasks screens
 */
@Composable
fun TodayPlanCard(
    userProfile: UserProfileEntity?,
    tasks: List<TaskEntity> = emptyList(),
    onToggleTask: ((TaskEntity) -> Unit)? = null,
    onManageTasks: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()

    val displayTasks = remember(tasks) {
        if (tasks.isNotEmpty()) {
            tasks
        } else {
            listOf(
                TaskEntity(id = 1L, title = "Drink water", iconType = "WATER", isCompleted = true, orderIndex = 0),
                TaskEntity(id = 2L, title = "Meditation", iconType = "MEDITATION", isCompleted = false, orderIndex = 1),
                TaskEntity(id = 3L, title = "10-min mindful break", iconType = "COFFEE", isCompleted = true, orderIndex = 2),
                TaskEntity(id = 4L, title = "Wind down at", timePill = "10:30 PM", iconType = "MOON", isCompleted = false, orderIndex = 3)
            )
        }
    }

    val completedCount = displayTasks.count { it.isCompleted }
    val totalCount = displayTasks.size.coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x1A8E69AA),
                ambientColor = Color(0x0F8E69AA)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // TOP BAR: "Today's plan ✦" | "1/3 done" Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title with pink sparkle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = onManageTasks != null,
                        onClick = {
                            haptics.pulseAppleSelection()
                            onManageTasks?.invoke()
                        }
                    )
                ) {
                    Text(
                        text = "Today’s plan",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B2337)
                    )
                    PinkFourPointSparkle(
                        modifier = Modifier.size(13.dp)
                    )
                    if (onManageTasks != null) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Manage tasks",
                            tint = Color(0xFFA59AB8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Pill: "X/Y done"
                ProgressPillBadge(
                    completedCount = completedCount,
                    totalCount = totalCount
                )
            }

            // TASK ITEMS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                displayTasks.forEach { task ->
                    TaskItemRow(
                        isDone = task.isCompleted,
                        onToggle = {
                            if (task.isCompleted) {
                                haptics.pulseAppleSelection()
                            } else {
                                haptics.pulseAppleButtonClick()
                                if (completedCount + 1 == totalCount) {
                                    haptics.pulseAppleSuccess()
                                }
                            }
                            onToggleTask?.invoke(task)
                        },
                        titleContent = {
                            if (task.timePill != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = task.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF1B2337)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF9EAF2))
                                            .padding(horizontal = 9.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = task.timePill,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2B1C33)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = task.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF1B2337)
                                )
                            }
                        },
                        trailingIcon = {
                            TaskTrailingIcon(iconType = task.iconType)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Trailing pastel icon selector matching the user's design.
 */
@Composable
private fun TaskTrailingIcon(iconType: String) {
    val lilacTint = Color(0xFFCBBDF5)

    when (iconType.uppercase()) {
        "WATER" -> {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = "Water drop",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "MINDFUL" -> {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Mindful break",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "MOON" -> {
            CrescentMoonWithStar(
                color = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "BOOK" -> {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = "Reading",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "WORKOUT" -> {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = "Workout",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "SUN" -> {
            Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = "Morning sun",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        "COFFEE" -> {
            Icon(
                imageVector = Icons.Filled.Coffee,
                contentDescription = "Coffee",
                tint = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
        else -> {
            CrescentMoonWithStar(
                color = lilacTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Task item row featuring circular checkbox, task description, and trailing pastel icon.
 */
@Composable
private fun TaskItemRow(
    isDone: Boolean,
    onToggle: () -> Unit,
    titleContent: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Circular Checkbox
            if (isDone) {
                Box(
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFAFA0E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .border(1.3.dp, Color(0xFFE4D0ED), CircleShape)
                )
            }

            titleContent()
        }

        trailingIcon()
    }
}

/**
 * Delicate 4-point sparkle star in pastel pink tint.
 */
@Composable
fun PinkFourPointSparkle(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF398BA)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx, cy * 0.52f, cx * 0.52f, cy, 0f, cy)
            cubicTo(cx * 0.52f, cy, cx, cy * 1.48f, cx, h)
            cubicTo(cx, cy * 1.48f, cx * 1.48f, cy, w, cy)
            cubicTo(cx * 1.48f, cy, cx, cy * 0.52f, cx, 0f)
            close()
        }
        drawPath(path, color = color)
    }
}

/**
 * Progress badge capsule ("X/Y done") with smooth dynamic border track matching the screenshot.
 */
@Composable
fun ProgressPillBadge(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = (completedCount.toFloat() / totalCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350),
        label = "pillProgress"
    )

    Box(
        modifier = modifier
            .width(88.dp)
            .height(29.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidthPx = 1.6.dp.toPx()
            val w = size.width
            val h = size.height
            val r = h / 2f

            val capsulePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = strokeWidthPx / 2f,
                        top = strokeWidthPx / 2f,
                        right = w - strokeWidthPx / 2f,
                        bottom = h - strokeWidthPx / 2f,
                        cornerRadius = CornerRadius(r, r)
                    )
                )
            }

            // Background inactive track (very light muted lavender)
            drawPath(
                path = capsulePath,
                color = Color(0xFFF0E5F8),
                style = Stroke(width = strokeWidthPx)
            )

            // Active progress segment (soft purple/lavender)
            if (animatedProgress > 0.01f) {
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(capsulePath, false)
                val totalLength = pathMeasure.length
                val progressPath = Path()

                // Starting from the bottom-left curve to match the screenshot!
                val startDistance = totalLength * 0.5f
                val endDistance = startDistance + (totalLength * animatedProgress)

                if (endDistance <= totalLength) {
                    pathMeasure.getSegment(startDistance, endDistance, progressPath, true)
                } else {
                    pathMeasure.getSegment(startDistance, totalLength, progressPath, true)
                    pathMeasure.getSegment(0f, endDistance - totalLength, progressPath, true)
                }

                drawPath(
                    path = progressPath,
                    color = Color(0xFFB197FC),
                    style = Stroke(width = strokeWidthPx + 0.4f, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = "$completedCount/$totalCount done",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1B2337)
        )
    }
}

/**
 * Delicate crescent moon with a tiny sparkle star icon in soft pastel lilac.
 */
@Composable
fun CrescentMoonWithStar(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFCBBDF5)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Moon outer circle and cutout circle
        val moonRadius = w * 0.40f
        val moonCenter = Offset(w * 0.42f, h * 0.55f)
        val cutoutCenter = Offset(w * 0.54f, h * 0.45f)
        val cutoutRadius = w * 0.35f

        val moonPath = Path().apply {
            addOval(
                Rect(
                    center = moonCenter,
                    radius = moonRadius
                )
            )
        }
        val cutoutPath = Path().apply {
            addOval(
                Rect(
                    center = cutoutCenter,
                    radius = cutoutRadius
                )
            )
        }

        val finalMoon = Path().apply {
            op(moonPath, cutoutPath, PathOperation.Difference)
        }
        drawPath(finalMoon, color = color)

        // Tiny 4-point sparkle star in upper right of moon
        val starCx = w * 0.82f
        val starCy = h * 0.28f
        val starRadius = w * 0.15f

        val starPath = Path().apply {
            moveTo(starCx, starCy - starRadius)
            cubicTo(starCx, starCy - starRadius * 0.5f, starCx + starRadius * 0.5f, starCy, starCx + starRadius, starCy)
            cubicTo(starCx + starRadius * 0.5f, starCy, starCx, starCy + starRadius * 0.5f, starCx, starCy + starRadius)
            cubicTo(starCx, starCy + starRadius * 0.5f, starCx - starRadius * 0.5f, starCy, starCx - starRadius, starCy)
            cubicTo(starCx - starRadius * 0.5f, starCy, starCx, starCy - starRadius * 0.5f, starCx, starCy - starRadius)
            close()
        }
        drawPath(starPath, color = color)
    }
}
