package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import kotlin.random.Random

/**
 * Math Challenge Mission UI Screen:
 * "Solve simple math questions"
 * - High-contrast dark theme matching the mission design system
 * - Clean equation presentation with question step counter
 * - Interactive numeric keypad with tactile haptics
 * - Immediate feedback on solve and bottom "✕ Exit preview" action
 */
@Composable
fun MathMissionScreen(
    onDismissConquered: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isLockedMission: Boolean = true,
    onAttemptExitBlocked: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null,
    totalQuestions: Int = 3
) {
    val haptics = rememberAppleHaptics()

    var currentQuestionIndex by remember { mutableIntStateOf(1) }
    var numA by remember { mutableIntStateOf(Random.nextInt(12, 48)) }
    var numB by remember { mutableIntStateOf(Random.nextInt(8, 35)) }
    var operator by remember { mutableStateOf(if (Random.nextBoolean()) "+" else "−") }
    var currentInput by remember { mutableStateOf("") }
    var isErrorFlash by remember { mutableStateOf(false) }

    fun generateNextQuestion() {
        numA = Random.nextInt(15, 60)
        numB = Random.nextInt(9, 40)
        operator = if (Random.nextBoolean()) "+" else "−"
        if (operator == "−" && numA < numB) {
            val tmp = numA
            numA = numB
            numB = tmp
        }
        currentInput = ""
    }

    val expectedAnswer = if (operator == "+") numA + numB else numA - numB

    var hasCompleted by remember { mutableStateOf(false) }

    fun triggerCompletion() {
        if (!hasCompleted) {
            hasCompleted = true
            haptics.pulseAppleSuccess()
            onDismissConquered()
        }
    }

    BackHandler {
        if (isLockedMission) {
            haptics.pulseAppleButtonClick()
            onAttemptExitBlocked?.invoke()
        } else {
            triggerCompletion()
        }
    }

    val inputBorderColor by animateColorAsState(
        targetValue = if (isErrorFlash) Color(0xFFEF4444) else Color(0xFF8B64DC),
        animationSpec = tween(250),
        label = "inputBorder"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B18))
            .testTag("math_mission_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.9f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            if (isLockedMission) {
                                onAttemptExitBlocked?.invoke()
                            } else {
                                onDismissConquered()
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isLockedMission) Icons.Default.Lock else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = if (isLockedMission) "Mission locked" else "Back",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLockedMission) 22.dp else 28.dp)
                        )
                    }

                    // Mission Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF7C3AED).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Question $currentQuestionIndex of $totalQuestions",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎧 Gentle 30%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBAE6FD)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.pulseAppleSelection()
                                onToggleMute?.invoke()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. EQUATION DISPLAY & INPUT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Solve math question",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Awaken your mind with simple arithmetic",
                    fontSize = 13.5.sp,
                    color = Color(0xFFA594BD)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Math Equation Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF23173D),
                                    Color(0xFF19102D)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF3B2961), RoundedCornerShape(24.dp))
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$numA $operator $numB",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Answer Box
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF120B20))
                                .border(2.dp, inputBorderColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentInput.isEmpty()) "?" else currentInput,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentInput.isEmpty()) Color(0xFF6B5885) else Color.White
                            )
                        }
                    }
                }
            }

            // 3. NUMERIC KEYPAD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isAction = key == "C" || key == "OK"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when (key) {
                                            "OK" -> Color(0xFF8B64DC)
                                            "C" -> Color(0xFF2D1B36)
                                            else -> Color(0xFF1E1530)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (key == "OK") Color(0xFFA78BFA) else Color(0xFF33234F),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        haptics.pulseAppleButtonClick()
                                        when (key) {
                                            "C" -> {
                                                currentInput = ""
                                                isErrorFlash = false
                                            }
                                            "OK" -> {
                                                val answerVal = currentInput.toIntOrNull()
                                                if (answerVal == expectedAnswer) {
                                                    haptics.pulseAppleSelection()
                                                    if (currentQuestionIndex >= totalQuestions) {
                                                        triggerCompletion()
                                                    } else {
                                                        currentQuestionIndex++
                                                        generateNextQuestion()
                                                    }
                                                } else {
                                                    haptics.pulseShake()
                                                    isErrorFlash = true
                                                    currentInput = ""
                                                }
                                            }
                                            else -> {
                                                if (currentInput.length < 4) {
                                                    isErrorFlash = false
                                                    currentInput += key
                                                    // Auto-check if matches length
                                                    val entered = currentInput.toIntOrNull()
                                                    if (entered == expectedAnswer) {
                                                        haptics.pulseAppleSelection()
                                                        if (currentQuestionIndex >= totalQuestions) {
                                                            triggerCompletion()
                                                        } else {
                                                            currentQuestionIndex++
                                                            generateNextQuestion()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = if (isAction) 16.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (key == "C") Color(0xFFF87171) else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. BOTTOM EXIT PREVIEW BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLockedMission) Color(0xFF1E232A) else Color(0xFFCFD5DE))
                    .navigationBarsPadding()
                    .height(54.dp)
                    .clickable {
                        haptics.pulseAppleButtonClick()
                        if (isLockedMission) {
                            onAttemptExitBlocked?.invoke()
                        } else {
                            triggerCompletion()
                        }
                    }
                    .testTag("exit_math_preview_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLockedMission) Icons.Default.Lock else Icons.Default.Close,
                        contentDescription = if (isLockedMission) "Mission locked" else "Exit preview",
                        tint = if (isLockedMission) Color(0xFFFBBF24) else Color(0xFF1E293B),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (isLockedMission) "🔒 Power-off & Exit Protected · Complete math" else "Exit preview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLockedMission) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
