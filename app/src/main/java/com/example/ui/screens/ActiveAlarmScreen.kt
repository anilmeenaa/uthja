package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.ShakeDetector
import com.example.alarm.rememberAppleHaptics
import com.example.ui.ActiveAlarmState

/**
 * Shake to dismiss active alarm screen matching the exact UI in the user attachments:
 * - Pure black canvas with top white accent line
 * - Top navigation bar: Left back '<' arrow, Right mute/unmute audio icon
 * - State 1 (0 shakes): "Shake to dismiss alarm" title with hand holding smartphone graphic
 * - State 2 (in progress): "$shakesLeft times left" subtitle + large bold display number (e.g. 29, 25, 17)
 * - Fiery red-orange liquid gradient fill rising smoothly from the bottom based on shake progress
 * - Bottom exit bar in slate grey with "✕ Exit preview"
 * - Accelerometer motion listener with tactile haptic pulse on every shake threshold met
 */
@Composable
fun ActiveAlarmScreen(
    state: ActiveAlarmState,
    onDismissConquered: () -> Unit,
    onAttemptSnooze: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isLockedMission: Boolean = true,
    onAttemptExitBlocked: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null,
    onShake: (() -> Unit)? = null,
    // Optional legacy callbacks maintained for compatibility
    onKeypadDigit: ((String) -> Unit)? = null,
    onKeypadBackspace: (() -> Unit)? = null,
    onKeypadClear: (() -> Unit)? = null,
    onSubmitMath: (() -> Unit)? = null,
    onSimulateShake: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()

    // Real device accelerometer motion listener
    DisposableEffect(Unit) {
        val detector = ShakeDetector(
            context = context,
            onShake = {
                haptics.pulseShake()
                onShake?.invoke() ?: onSimulateShake?.invoke()
            }
        )
        detector.start()
        onDispose {
            detector.stop()
        }
    }

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

    val targetCount = state.shakeTargetCount.coerceAtLeast(1)
    val currentCount = state.shakeCurrentCount.coerceIn(0, targetCount)
    val shakesLeft = (targetCount - currentCount).coerceAtLeast(0)

    // Trigger completion safely when shake target count is conquered
    LaunchedEffect(currentCount, targetCount) {
        if (currentCount >= targetCount && !hasCompleted) {
            triggerCompletion()
        }
    }

    // Calculate progress fraction for the rising red-orange gradient
    val progressFraction = (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shakeLiquidProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Interactive fallback: tapping also triggers a shake with haptic feedback
                haptics.pulseShake()
                onShake?.invoke() ?: onSimulateShake?.invoke()
            }
            .testTag("active_alarm_screen")
    ) {
        // 1. RISING FIERY RED-ORANGE GRADIENT FILL
        // Rises from the bottom of the screen as progress increases
        if (animatedProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF521D), // Vivid fiery orange at liquid surface
                                Color(0xFFFF3322), // Vibrant hot red-orange
                                Color(0xFFE50926)  // Deep intense crimson at bottom
                            )
                        )
                    )
            )
        }

        // 2. MAIN CONTENT LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with progress line
            Column(modifier = Modifier.fillMaxWidth()) {
                // Thin white accent line at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.95f))
                )

                // Top icons row: '<', Meme badge, and mute/unmute
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
                                triggerCompletion()
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

                    if (com.example.data.model.RingtoneCatalog.isMemeRingtone(state.sound)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF7C3AED).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "👾 Meme: ${state.sound.take(18)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
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

            // Center Display: Initial Hand Illustration vs Countdown Number
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentCount > 0,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "shakeDisplayState"
                ) { isShakingActive ->
                    if (!isShakingActive) {
                        // INITIAL SCREEN (Screenshot 1): "Shake to dismiss alarm" + Hand Holding Phone
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Shake to dismiss alarm",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.3).sp
                            )

                            Spacer(modifier = Modifier.height(44.dp))

                            HandHoldingPhoneIllustration(
                                modifier = Modifier.size(width = 175.dp, height = 230.dp)
                            )
                        }
                    } else {
                        // SHAKING IN-PROGRESS (Screenshots 2, 3, 4): "X times left" + Giant Number
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "$shakesLeft times left",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.2).sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "$shakesLeft",
                                fontSize = 118.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-3).sp,
                                lineHeight = 120.sp
                            )
                        }
                    }
                }
            }

            // 3. BOTTOM EXIT PREVIEW BAR
            // When locked during an active mission, displays power-off & exit protection banner
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
                    .testTag("exit_preview_button"),
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
                        text = if (isLockedMission) "🔒 Power-off & Exit Protected · Complete challenge" else "Exit preview",
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

/**
 * Hand holding smartphone vector illustration matching Screenshot 1:
 * - Peach/salmon hand digits and palm
 * - Dark blue phone chassis with rounded corners
 * - White inner screen
 * - Top speaker ear-piece capsule notch
 */
@Composable
fun HandHoldingPhoneIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val skinColor = Color(0xFFF1ADA4)
        val phoneChassis = Color(0xFF274384)
        val phoneScreen = Color(0xFFF2F5FA)
        val speakerColor = Color(0xFF182E5E)

        // 1. Draw Palm & Base of Hand (curving behind bottom-right of the phone)
        val palmPath = Path().apply {
            moveTo(w * 0.65f, h * 0.44f)
            cubicTo(
                w * 0.90f, h * 0.46f,
                w * 0.98f, h * 0.58f,
                w * 0.92f, h * 0.72f
            )
            cubicTo(
                w * 0.85f, h * 0.86f,
                w * 0.64f, h * 0.88f,
                w * 0.50f, h * 0.87f
            )
            cubicTo(
                w * 0.38f, h * 0.86f,
                w * 0.34f, h * 0.76f,
                w * 0.40f, h * 0.68f
            )
            close()
        }
        drawPath(palmPath, color = skinColor)

        // 2. Draw Phone Outer Frame
        val phoneLeft = w * 0.28f
        val phoneTop = h * 0.14f
        val phoneWidth = w * 0.48f
        val phoneHeight = h * 0.60f
        val cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())

        drawRoundRect(
            color = phoneChassis,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = cornerRadius
        )

        // 3. Draw Phone Screen
        val borderWidth = 3.5.dp.toPx()
        val screenLeft = phoneLeft + borderWidth
        val screenTop = phoneTop + borderWidth
        val screenWidth = phoneWidth - (borderWidth * 2)
        val screenHeight = phoneHeight - (borderWidth * 2)
        val screenRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

        drawRoundRect(
            color = phoneScreen,
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = screenRadius
        )

        // 4. Draw Top Speaker Notch
        val speakerWidth = phoneWidth * 0.28f
        val speakerHeight = 3.dp.toPx()
        val speakerLeft = phoneLeft + (phoneWidth - speakerWidth) / 2
        val speakerTop = phoneTop + 6.dp.toPx()

        drawRoundRect(
            color = speakerColor,
            topLeft = Offset(speakerLeft, speakerTop),
            size = Size(speakerWidth, speakerHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 5. Draw 4 Fingers Gripping Left Side of Phone
        // Finger 1 (Thumb / Top finger)
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(w * 0.18f, h * 0.30f),
            size = Size(w * 0.18f, h * 0.082f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Finger 2 (Index)
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(w * 0.20f, h * 0.395f),
            size = Size(w * 0.17f, h * 0.076f),
            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx())
        )

        // Finger 3 (Middle)
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(w * 0.20f, h * 0.485f),
            size = Size(w * 0.17f, h * 0.076f),
            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx())
        )

        // Finger 4 (Ring/Pinky)
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(w * 0.21f, h * 0.575f),
            size = Size(w * 0.16f, h * 0.072f),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
    }
}
