package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics

/**
 * Typing Mission UI Screen:
 * "Type the given sentence"
 * - Clean high-contrast typography
 * - Word-by-word visual feedback
 * - Real-time progress bar
 * - Bottom exit bar matching the app's mission design system
 */
@Composable
fun TypingMissionScreen(
    onDismissConquered: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isLockedMission: Boolean = true,
    onAttemptExitBlocked: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null,
    targetSentence: String = "I am awake, clear-minded, and ready to win today."
) {
    val haptics = rememberAppleHaptics()
    var inputSentence by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var hasCompleted by remember { mutableStateOf(false) }

    fun normalizeText(text: String): String {
        return text.trim()
            .replace("[\\p{Punct}&&[^']]+".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .lowercase()
    }

    fun checkAndTriggerCompletion(candidate: String) {
        if (hasCompleted) return
        val normCandidate = normalizeText(candidate)
        val normTarget = normalizeText(targetSentence)
        if (normCandidate == normTarget || candidate.trim().equals(targetSentence.trim(), ignoreCase = true)) {
            hasCompleted = true
            haptics.pulseAppleSuccess()
            onDismissConquered()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Calculate match progress
    var matchedCount = 0
    for (i in inputSentence.indices) {
        if (i < targetSentence.length && inputSentence[i].lowercaseChar() == targetSentence[i].lowercaseChar()) {
            matchedCount++
        } else {
            break
        }
    }

    val progress = (matchedCount.toFloat() / targetSentence.length.toFloat()).coerceIn(0f, 1f)

    BackHandler {
        if (isLockedMission) {
            haptics.pulseAppleButtonClick()
            onAttemptExitBlocked?.invoke()
        } else {
            if (!hasCompleted) {
                hasCompleted = true
                onDismissConquered()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B18))
            .testTag("typing_mission_screen")
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

                    // Progress Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF74E76).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFFFDA4AF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% typed",
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

            // 2. MAIN TYPING CONTENT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Type the sentence",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Prove you are alert by accurately typing the prompt",
                    fontSize = 13.5.sp,
                    color = Color(0xFFA594BD)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Target Quote Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B122B))
                        .border(1.5.dp, Color(0xFF3B2961), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                        for (i in targetSentence.indices) {
                            when {
                                i < matchedCount -> {
                                    withStyle(SpanStyle(color = Color(0xFF10B981), fontWeight = FontWeight.Bold)) {
                                        append(targetSentence[i])
                                    }
                                }
                                i == matchedCount -> {
                                    withStyle(SpanStyle(color = Color(0xFFF74E76), fontWeight = FontWeight.ExtraBold)) {
                                        append(targetSentence[i])
                                    }
                                }
                                else -> {
                                    withStyle(SpanStyle(color = Color(0xFF8B7B9E))) {
                                        append(targetSentence[i])
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = annotatedString,
                        fontSize = 19.sp,
                        lineHeight = 27.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFF74E76),
                    trackColor = Color(0xFF26193D)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Text Input Field
                OutlinedTextField(
                    value = inputSentence,
                    onValueChange = { newVal ->
                        inputSentence = newVal
                        haptics.pulseAppleButtonClick()
                        checkAndTriggerCompletion(newVal)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("typing_mission_input"),
                    placeholder = {
                        Text(
                            text = "Tap to type sentence here...",
                            color = Color(0xFF6B5885)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF74E76),
                        unfocusedBorderColor = Color(0xFF3B2961),
                        focusedContainerColor = Color(0xFF120B20),
                        unfocusedContainerColor = Color(0xFF120B20)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            checkAndTriggerCompletion(inputSentence)
                        }
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Explicit Done / Submit Button
                val isSentenceMatched = normalizeText(inputSentence) == normalizeText(targetSentence) ||
                        inputSentence.trim().equals(targetSentence.trim(), ignoreCase = true)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (isSentenceMatched || progress > 0.8f) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF74E76), Color(0xFFFDA4AF))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF3B2961), Color(0xFF4C3875))
                                )
                            }
                        )
                        .clickable {
                            haptics.pulseAppleButtonClick()
                            if (isSentenceMatched) {
                                checkAndTriggerCompletion(inputSentence)
                            } else {
                                // Also allow completing if user typed most of sentence or taps submit
                                checkAndTriggerCompletion(inputSentence)
                            }
                        }
                        .testTag("submit_typing_mission_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSentenceMatched) "Submit & Complete Mission" else "Check Sentence",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 3. BOTTOM EXIT PREVIEW BAR
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
                            onDismissConquered()
                        }
                    }
                    .testTag("exit_typing_preview_button"),
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
                        text = if (isLockedMission) "🔒 Power-off & Exit Protected · Complete typing" else "Exit preview",
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
