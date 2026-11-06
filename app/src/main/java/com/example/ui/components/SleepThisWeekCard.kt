package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Sleep this week" chart card matching the uploaded design screenshot.
 * Displays Y-axis labels (9h, 7h, 5h, 3h, 1h), faint gridlines, and 7 vertical
 * lavender rounded pill bars with sparkle stars and day labels (M, T, W, T, F, S, S).
 */
@Composable
fun SleepThisWeekCard(
    modifier: Modifier = Modifier,
    avgHoursString: String = "7h 28m",
    weeklyHours: List<Float> = listOf(6.5f, 6.0f, 2.8f, 7.0f, 4.8f, 9.2f, 7.8f)
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAnimated = true
    }

    val chartProgress by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "chartProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x148E69AA),
                ambientColor = Color(0x0A8E69AA)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: "Sleep this week 🌙" & "Avg 7h 28m"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sleep this week",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B2337)
                    )
                    CrescentMoonWithStar(
                        color = Color(0xFFCBBDF5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Avg",
                        fontSize = 13.5.sp,
                        color = Color(0xFF9EA3B0)
                    )
                    Text(
                        text = avgHoursString,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF785CD8)
                    )
                }
            }

            // Chart Area with Y-axis labels and 7 Bar Columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                // Background Horizontal Gridlines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val yValues = listOf(0.08f, 0.28f, 0.48f, 0.68f, 0.88f)
                    val leftOffset = 28.dp.toPx()
                    val gridColor = Color(0xFFF2ECE6)

                    yValues.forEach { ratio ->
                        val y = size.height * ratio
                        drawLine(
                            color = gridColor,
                            start = Offset(leftOffset, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Y-Axis Labels (9h, 7h, 5h, 3h, 1h)
                    Column(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight()
                            .padding(bottom = 22.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("9h", "7h", "5h", "3h", "1h").forEach { label ->
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = Color(0xFF9E98AB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 7 Bar Columns (M, T, W, T, F, S, S)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyHours.forEachIndexed { index, hours ->
                            val day = days.getOrElse(index) { "" }
                            val maxHeightHours = 10f
                            val barRatio = ((hours / maxHeightHours) * chartProgress).coerceIn(0.08f, 1f)
                            val hasStar = hours >= 6.0f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Bar with rounded capsule & internal sparkle
                                Box(
                                    modifier = Modifier
                                        .width(25.dp)
                                        .fillMaxHeight(barRatio * 0.82f)
                                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFFC7B1FA),
                                                    Color(0xFFD6C4FC),
                                                    Color(0xFFEDE4FD)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (hasStar) {
                                        Box(modifier = Modifier.padding(top = 6.dp)) {
                                            WhiteSparkleStar(modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day Label
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7A7588)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Delicate white translucent sparkle star placed inside the top of sleep bars.
 */
@Composable
private fun WhiteSparkleStar(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx, cy * 0.45f, cx * 0.45f, cy, 0f, cy)
            cubicTo(cx * 0.45f, cy, cx, cy * 1.55f, cx, h)
            cubicTo(cx, cy * 1.55f, cx * 1.55f, cy, w, cy)
            cubicTo(cx * 1.55f, cy, cx, cy * 0.45f, cx, 0f)
            close()
        }
        drawPath(path, color = Color.White.copy(alpha = 0.85f))
    }
}
