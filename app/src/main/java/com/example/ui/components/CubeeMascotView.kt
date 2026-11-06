package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.alarm.rememberAppleHaptics
import kotlinx.coroutines.delay

enum class CubeeState {
    WAKING,
    IDLE,
    LAUGHING
}

/**
 * Authentic Cubee Mascot Companion.
 * Natively renders the exact SVG character vector, color palette, gradients,
 * highlights, blush cheeks, and keyframed animations defined in
 * `app/src/main/assets/cubee/index.html` and `original_cubee_avatar.json`.
 *
 * Runs via high-performance Jetpack Compose Canvas without Chromium or WebView,
 * eliminating Mesa DRM rendernode errors and providing fluid 60/120fps motion.
 */
@Composable
fun CubeeMascotView(
    isVisible: Boolean,
    onMascotTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.75f, animationSpec = tween(400)),
        exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.75f, animationSpec = tween(300)),
        modifier = modifier
    ) {
        val haptics = rememberAppleHaptics()
        var mascotState by remember { mutableStateOf(CubeeState.WAKING) }
        var wakingStep by remember { mutableIntStateOf(0) }
        var isBlinking by remember { mutableStateOf(false) }

        // 1. Initial Waking sequence (2000ms duration)
        LaunchedEffect(Unit) {
            wakingStep = 0
            delay(600)
            wakingStep = 1 // yawn & half-open eyes
            delay(700)
            wakingStep = 2 // sparkle open wide eyes
            delay(700)
            mascotState = CubeeState.IDLE
        }

        // 2. Natural eye blinks during Idle
        LaunchedEffect(mascotState) {
            if (mascotState != CubeeState.IDLE) {
                isBlinking = false
                return@LaunchedEffect
            }
            while (true) {
                delay(3600)
                isBlinking = true
                delay(180)
                isBlinking = false
            }
        }

        // 3. Return to Idle after Laughing
        LaunchedEffect(mascotState) {
            if (mascotState == CubeeState.LAUGHING) {
                delay(1800)
                mascotState = CubeeState.IDLE
            }
        }

        // Idle floating & breathing transition
        val infiniteTransition = rememberInfiniteTransition(label = "cubee_idle_transition")
        val floatOffsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float_offset_y"
        )
        val floatTilt by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float_tilt"
        )

        // Laugh bounce transition (cycle 450ms)
        val laughTransition = rememberInfiniteTransition(label = "cubee_laugh_transition")
        val laughBounceY by laughTransition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(225, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laugh_bounce_y"
        )
        val laughSquashY by laughTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(225, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laugh_squash_y"
        )
        val laughSquashX by laughTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(225, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laugh_squash_x"
        )
        val laughTilt by laughTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(225, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laugh_tilt"
        )

        val handleTap = {
            haptics.pulseAppleSuccess()
            mascotState = CubeeState.LAUGHING
            onMascotTapped()
        }

        Box(
            modifier = Modifier
                .size(116.dp)
                .semantics {
                    contentDescription = "Cubee mascot companion"
                    role = Role.Button
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = handleTap
                )
                .testTag("cubee_mascot_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(116.dp)
                    .testTag("cubee_mascot_canvas")
            ) {
                val scale = size.minDimension / 160f
                val isLaughing = mascotState == CubeeState.LAUGHING

                val currentOffsetY = if (isLaughing) laughBounceY else floatOffsetY
                val currentTilt = if (isLaughing) laughTilt else floatTilt
                val currentScaleX = if (isLaughing) laughSquashX else 1f
                val currentScaleY = if (isLaughing) laughSquashY else 1f

                scale(scale, Offset.Zero) {
                    // Ground shadow stays anchored near the bottom
                    drawGroundShadow(isLaughing)

                    // Character body with bounce/float physics
                    translate(left = 0f, top = currentOffsetY) {
                        scale(
                            scaleX = currentScaleX,
                            scaleY = currentScaleY,
                            pivot = Offset(80f, 138f)
                        ) {
                            rotate(degrees = currentTilt, pivot = Offset(80f, 82f)) {
                                drawCubeeCharacter(
                                    mascotState = mascotState,
                                    wakingStep = wakingStep,
                                    isBlinking = isBlinking,
                                    isLaughing = isLaughing
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGroundShadow(isLaughing: Boolean) {
    val shadowRx = if (isLaughing) 38f else 44f
    val shadowRy = 7f
    val shadowCenter = Offset(80f, 148f)
    drawOval(
        color = Color(0x291E1B4B), // rgba(30, 27, 75, 0.16)
        topLeft = Offset(shadowCenter.x - shadowRx, shadowCenter.y - shadowRy),
        size = Size(shadowRx * 2f, shadowRy * 2f)
    )
}

private fun DrawScope.drawCubeeCharacter(
    mascotState: CubeeState,
    wakingStep: Int,
    isBlinking: Boolean,
    isLaughing: Boolean
) {
    // 1. Cute Ears / Antennae on Top
    // Left ear
    val earLeftRot = if (isLaughing) -10f else -4f
    rotate(degrees = earLeftRot, pivot = Offset(51f, 25f)) {
        drawRoundRect(
            color = Color(0xFF4F46E5),
            topLeft = Offset(42f, 14f),
            size = Size(18f, 22f),
            cornerRadius = CornerRadius(9f, 9f)
        )
        drawRoundRect(
            color = Color(0xFFA5B4FC),
            topLeft = Offset(46f, 17f),
            size = Size(10f, 14f),
            cornerRadius = CornerRadius(5f, 5f)
        )
    }

    // Right ear
    val earRightRot = if (isLaughing) 10f else 4f
    rotate(degrees = earRightRot, pivot = Offset(109f, 25f)) {
        drawRoundRect(
            color = Color(0xFF4F46E5),
            topLeft = Offset(100f, 14f),
            size = Size(18f, 22f),
            cornerRadius = CornerRadius(9f, 9f)
        )
        drawRoundRect(
            color = Color(0xFFA5B4FC),
            topLeft = Offset(104f, 17f),
            size = Size(10f, 14f),
            cornerRadius = CornerRadius(5f, 5f)
        )
    }

    // 2. Main Cute Rounded Cube Body with original gradient
    val cubeBrush = Brush.linearGradient(
        0.00f to Color(0xFF7C3AED),
        0.45f to Color(0xFF6366F1),
        1.00f to Color(0xFF4F46E5),
        start = Offset(20f, 16f),
        end = Offset(140f, 144f)
    )
    drawRoundRect(
        brush = cubeBrush,
        topLeft = Offset(24f, 26f),
        size = Size(112f, 112f),
        cornerRadius = CornerRadius(32f, 32f)
    )

    // 3. Top Specular 3D Highlight Curve
    val specularPath = Path().apply {
        moveTo(40f, 32f)
        cubicTo(55f, 28f, 105f, 28f, 120f, 32f)
        cubicTo(128f, 35f, 130f, 46f, 122f, 52f)
        cubicTo(105f, 45f, 55f, 45f, 38f, 52f)
        cubicTo(30f, 46f, 32f, 35f, 40f, 32f)
        close()
    }
    val specularBrush = Brush.linearGradient(
        0.0f to Color.White.copy(alpha = 0.45f),
        1.0f to Color.White.copy(alpha = 0.00f),
        start = Offset(80f, 22f),
        end = Offset(80f, 58f)
    )
    drawPath(path = specularPath, brush = specularBrush)

    // 4. Inner Face Card (Creamy Soft Contrast Plate)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.96f),
        topLeft = Offset(34f, 42f),
        size = Size(92f, 84f),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // 5. Rosy Blushing Cheeks with Soft Radial Glow
    val cheekAlpha = if (isLaughing) 0.85f else 0.60f
    val cheekRx = if (isLaughing) 11f else 8.5f
    val cheekRy = if (isLaughing) 8.5f else 6.0f

    // Left cheek
    val leftCheekCenter = Offset(48f, 90f)
    val cheekBrushLeft = Brush.radialGradient(
        0.0f to Color(0xFFFB7185).copy(alpha = cheekAlpha),
        1.0f to Color(0xFFFB7185).copy(alpha = 0.0f),
        center = leftCheekCenter,
        radius = cheekRx
    )
    drawOval(
        brush = cheekBrushLeft,
        topLeft = Offset(leftCheekCenter.x - cheekRx, leftCheekCenter.y - cheekRy),
        size = Size(cheekRx * 2f, cheekRy * 2f)
    )

    // Right cheek
    val rightCheekCenter = Offset(112f, 90f)
    val cheekBrushRight = Brush.radialGradient(
        0.0f to Color(0xFFFB7185).copy(alpha = cheekAlpha),
        1.0f to Color(0xFFFB7185).copy(alpha = 0.0f),
        center = rightCheekCenter,
        radius = cheekRx
    )
    drawOval(
        brush = cheekBrushRight,
        topLeft = Offset(rightCheekCenter.x - cheekRx, rightCheekCenter.y - cheekRy),
        size = Size(cheekRx * 2f, cheekRy * 2f)
    )

    // 6. Eyes Rendering
    drawCubeeEyes(
        mascotState = mascotState,
        wakingStep = wakingStep,
        isBlinking = isBlinking,
        isLaughing = isLaughing
    )

    // 7. Mouth Rendering
    drawCubeeMouth(
        mascotState = mascotState,
        wakingStep = wakingStep,
        isLaughing = isLaughing
    )
}

private fun DrawScope.drawCubeeEyes(
    mascotState: CubeeState,
    wakingStep: Int,
    isBlinking: Boolean,
    isLaughing: Boolean
) {
    val darkEyeColor = Color(0xFF1E1B4B)

    // 1. Laughing Eyes: Joyful squinting crescents (^_^)
    if (isLaughing) {
        val leftLaughPath = Path().apply {
            moveTo(50f, 78f)
            cubicTo(55f, 70f, 65f, 70f, 70f, 78f)
        }
        val rightLaughPath = Path().apply {
            moveTo(90f, 78f)
            cubicTo(95f, 70f, 105f, 70f, 110f, 78f)
        }
        drawPath(
            path = leftLaughPath,
            color = darkEyeColor,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = rightLaughPath,
            color = darkEyeColor,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )
        return
    }

    // 2. Waking sequence eyes
    if (mascotState == CubeeState.WAKING) {
        if (wakingStep == 0) {
            // Sleeping closed eye curves (~ ~)
            val leftSleepPath = Path().apply {
                moveTo(51f, 76f)
                cubicTo(56f, 80f, 64f, 80f, 69f, 76f)
            }
            val rightSleepPath = Path().apply {
                moveTo(91f, 76f)
                cubicTo(96f, 80f, 104f, 80f, 109f, 76f)
            }
            drawPath(
                path = leftSleepPath,
                color = darkEyeColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            drawPath(
                path = rightSleepPath,
                color = darkEyeColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            return
        } else if (wakingStep == 1) {
            // Half open drowsy eyes
            drawOval(
                color = darkEyeColor,
                topLeft = Offset(60f - 6f, 75f - 3.5f),
                size = Size(12f, 7f)
            )
            drawOval(
                color = darkEyeColor,
                topLeft = Offset(100f - 6f, 75f - 3.5f),
                size = Size(12f, 7f)
            )
            return
        }
    }

    // 3. Regular Blinking
    if (isBlinking) {
        drawLine(
            color = darkEyeColor,
            start = Offset(52f, 75f),
            end = Offset(68f, 75f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = darkEyeColor,
            start = Offset(92f, 75f),
            end = Offset(108f, 75f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        return
    }

    // 4. Default Wide Kawaii Eyes with Dual Sparkle Highlights
    // Left pupil
    drawOval(
        color = darkEyeColor,
        topLeft = Offset(60f - 7.5f, 74f - 9.5f),
        size = Size(15f, 19f)
    )
    // Left sparkles
    drawCircle(color = Color.White, radius = 3.2f, center = Offset(57.5f, 71f))
    drawCircle(color = Color.White, radius = 1.4f, center = Offset(63f, 77.5f))

    // Right pupil
    drawOval(
        color = darkEyeColor,
        topLeft = Offset(100f - 7.5f, 74f - 9.5f),
        size = Size(15f, 19f)
    )
    // Right sparkles
    drawCircle(color = Color.White, radius = 3.2f, center = Offset(97.5f, 71f))
    drawCircle(color = Color.White, radius = 1.4f, center = Offset(103f, 77.5f))
}

private fun DrawScope.drawCubeeMouth(
    mascotState: CubeeState,
    wakingStep: Int,
    isLaughing: Boolean
) {
    // 1. Laughing Mouth (Open joyous grin with pink tongue)
    if (isLaughing) {
        val cavityPath = Path().apply {
            moveTo(70f, 88f)
            quadraticTo(80f, 88f, 90f, 88f)
            quadraticTo(90f, 102f, 80f, 102f)
            quadraticTo(70f, 102f, 70f, 88f)
            close()
        }
        drawPath(path = cavityPath, color = Color(0xFF9F1239), style = Fill)

        // Cute tongue
        drawOval(
            color = Color(0xFFFDA4AF),
            topLeft = Offset(80f - 6.5f, 98f - 4f),
            size = Size(13f, 8f)
        )

        // Upper lip arc
        val lipPath = Path().apply {
            moveTo(69f, 88f)
            quadraticTo(80f, 88f, 91f, 88f)
        }
        drawPath(
            path = lipPath,
            color = Color(0xFF1E1B4B),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        return
    }

    // 2. Waking Yawn
    if (mascotState == CubeeState.WAKING && wakingStep == 1) {
        drawOval(
            color = Color(0xFFBE123C),
            topLeft = Offset(80f - 4.5f, 89f - 6.5f),
            size = Size(9f, 13f)
        )
        return
    }

    // 3. Calm, Sweet Kawaii Smile (Idle & Rest)
    val smilePath = Path().apply {
        moveTo(73f, 88f)
        quadraticTo(80f, 94f, 87f, 88f)
    }
    drawPath(
        path = smilePath,
        color = Color(0xFF1E1B4B),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}
