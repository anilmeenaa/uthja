package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics

/**
 * "Quick mood check-in" card matching the uploaded design screenshot.
 * Displays 5 custom pastel mood faces and an interactive "Log mood" button.
 */
@Composable
fun QuickMoodCheckInCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    var selectedMoodIndex by remember { mutableIntStateOf(1) } // Default: Calm (2nd face)

    Box(
        modifier = modifier
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x148E69AA),
                ambientColor = Color(0x0A8E69AA)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(26.dp))
            .aspectRatio(900f / 1060f)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Text(
                    text = "Quick mood\ncheck-in",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B2337),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "How are you feeling?",
                        fontSize = 12.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF7E8494)
                    )
                    PinkFourPointSparkle(modifier = Modifier.size(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5 Mood Emoji Faces in Horizontal Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moods = listOf(
                    MoodData(Color(0xFFFFF0B3), "Happy", 0),
                    MoodData(Color(0xFFE0F4EB), "Calm", 1),
                    MoodData(Color(0xFFE8DEF8), "Neutral", 2),
                    MoodData(Color(0xFFFFDDD2), "Tired", 3),
                    MoodData(Color(0xFFFFD4DF), "Sad", 4)
                )

                moods.forEach { mood ->
                    val isSelected = selectedMoodIndex == mood.index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "moodScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(mood.bg)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color(0xFF8B64DC) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptics.pulseAppleSelection()
                                    selectedMoodIndex = mood.index
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MoodFaceCanvas(moodIndex = mood.index)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // "Log mood" Button with soft purple/lavender gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFC4A8FA), Color(0xFFA685F5))
                        )
                    )
                    .clickable {
                        haptics.pulseAppleButtonClick()
                        val moodNames = listOf("Joyful ☀️", "Calm 🌿", "Centered 🌙", "Restless ☕", "Gentle 🌸")
                        Toast.makeText(
                            context,
                            "Logged as ${moodNames.getOrElse(selectedMoodIndex) { "Good" }}! Take care today.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log mood",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

private data class MoodData(
    val bg: Color,
    val name: String,
    val index: Int
)

/**
 * Draws minimalist facial expressions matching the uploaded screenshot.
 */
@Composable
private fun MoodFaceCanvas(moodIndex: Int) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val faceColor = Color(0xFF382A4A)
        val strokeW = 1.3f

        when (moodIndex) {
            0 -> { // Happy: smiling eyes & mouth
                // Eyes (slight curve or dots)
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.35f, h * 0.40f))
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.65f, h * 0.40f))
                // Smile
                val smile = Path().apply {
                    moveTo(w * 0.32f, h * 0.60f)
                    cubicTo(w * 0.38f, h * 0.78f, w * 0.62f, h * 0.78f, w * 0.68f, h * 0.60f)
                }
                drawPath(smile, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
            1 -> { // Calm: gentle closed curved eyes & soft smile
                val leftEye = Path().apply {
                    moveTo(w * 0.26f, h * 0.42f)
                    cubicTo(w * 0.32f, h * 0.48f, w * 0.40f, h * 0.48f, w * 0.44f, h * 0.42f)
                }
                val rightEye = Path().apply {
                    moveTo(w * 0.56f, h * 0.42f)
                    cubicTo(w * 0.60f, h * 0.48f, w * 0.68f, h * 0.48f, w * 0.74f, h * 0.42f)
                }
                drawPath(leftEye, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawPath(rightEye, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                // Gentle smile
                val smile = Path().apply {
                    moveTo(w * 0.36f, h * 0.64f)
                    cubicTo(w * 0.42f, h * 0.74f, w * 0.58f, h * 0.74f, w * 0.64f, h * 0.64f)
                }
                drawPath(smile, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
            2 -> { // Neutral: dots for eyes, straight mouth
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.35f, h * 0.42f))
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.65f, h * 0.42f))
                drawLine(
                    color = faceColor,
                    start = Offset(w * 0.36f, h * 0.66f),
                    end = Offset(w * 0.64f, h * 0.66f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            3 -> { // Tired/Stressed: angled furrowed eyes & down mouth
                val leftBrow = Path().apply {
                    moveTo(w * 0.28f, h * 0.40f)
                    lineTo(w * 0.42f, h * 0.46f)
                }
                val rightBrow = Path().apply {
                    moveTo(w * 0.72f, h * 0.40f)
                    lineTo(w * 0.58f, h * 0.46f)
                }
                drawPath(leftBrow, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawPath(rightBrow, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                // Frown
                val frown = Path().apply {
                    moveTo(w * 0.34f, h * 0.70f)
                    cubicTo(w * 0.42f, h * 0.60f, w * 0.58f, h * 0.60f, w * 0.66f, h * 0.70f)
                }
                drawPath(frown, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
            4 -> { // Sad: sad eyes & frown
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.35f, h * 0.44f))
                drawCircle(faceColor, radius = 1.2f, center = Offset(w * 0.65f, h * 0.44f))
                val frown = Path().apply {
                    moveTo(w * 0.32f, h * 0.70f)
                    cubicTo(w * 0.40f, h * 0.58f, w * 0.60f, h * 0.58f, w * 0.68f, h * 0.70f)
                }
                drawPath(frown, faceColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            }
        }
    }
}
