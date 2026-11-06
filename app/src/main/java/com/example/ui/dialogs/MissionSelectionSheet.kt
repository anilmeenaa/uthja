package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.DismissType

/**
 * Mission Selection Popup matching the user's attached design:
 *
 * - Header: "Mission" title centered, Close "X" button on the top right
 * - Categories:
 *     1. "Wake your brain"
 *        - Typing (keyboard badge)
 *        - Math (math badge, "BEST" orange pill badge)
 *     2. "Wake your body"
 *        - Step (footprints badge, "HOT" red pill badge)
 *        - QR/Barcode (QR grid badge)
 *        - Shake (vibrating phone badge)
 * - Multi-selection support: users can select multiple missions (up to 5)
 * - Clean white background (#FFFFFF), rounded container (#F1F3F5 light selection highlight)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionSelectionSheet(
    selectedMissions: List<DismissType>,
    onMissionsConfirmed: (List<DismissType>) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppleHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current selection list (multi-select up to 5)
    val chosenList = remember {
        mutableStateListOf<DismissType>().apply {
            addAll(selectedMissions.take(5))
        }
    }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted! Barcode scanner is ready.", Toast.LENGTH_SHORT).show()
            if (!chosenList.contains(DismissType.QR_CODE)) {
                toggleSelection(chosenList, DismissType.QR_CODE)
            }
        } else {
            Toast.makeText(context, "Camera permission needed to scan barcodes during alarms.", Toast.LENGTH_LONG).show()
            if (!chosenList.contains(DismissType.QR_CODE)) {
                toggleSelection(chosenList, DismissType.QR_CODE)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            // 1. Top Bar: Centered "Mission" + "X" on right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Mission",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .testTag("close_mission_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Section 1: "Wake your brain"
            Text(
                text = "Wake your brain",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Typing item
            val isTypingSelected = chosenList.contains(DismissType.TYPING)
            MissionRowItem(
                title = "Typing",
                badgeText = null,
                badgeColor = null,
                iconType = MissionIconType.TYPING,
                isSelected = isTypingSelected,
                onClick = {
                    haptics.pulseAppleButtonClick()
                    toggleSelection(chosenList, DismissType.TYPING)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Math item (with BEST badge)
            val isMathSelected = chosenList.contains(DismissType.MATH)
            MissionRowItem(
                title = "Math",
                badgeText = "BEST",
                badgeColor = Color(0xFFF97316), // Orange
                iconType = MissionIconType.MATH,
                isSelected = isMathSelected,
                onClick = {
                    haptics.pulseAppleButtonClick()
                    toggleSelection(chosenList, DismissType.MATH)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Section 2: "Wake your body"
            Text(
                text = "Wake your body",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Step item (with HOT badge)
            val isStepSelected = chosenList.contains(DismissType.STEP)
            MissionRowItem(
                title = "Step",
                badgeText = "HOT",
                badgeColor = Color(0xFFEF4444), // Coral Red
                iconType = MissionIconType.STEP,
                isSelected = isStepSelected,
                onClick = {
                    haptics.pulseAppleButtonClick()
                    toggleSelection(chosenList, DismissType.STEP)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // QR/Barcode item
            val isQrSelected = chosenList.contains(DismissType.QR_CODE)
            MissionRowItem(
                title = "QR/Barcode",
                badgeText = if (isQrSelected && hasCameraPermission) "Ready" else null,
                badgeColor = if (isQrSelected && hasCameraPermission) Color(0xFF22C55E) else null,
                iconType = MissionIconType.QR_CODE,
                isSelected = isQrSelected,
                onClick = {
                    haptics.pulseAppleButtonClick()
                    if (!isQrSelected) {
                        val cameraGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        hasCameraPermission = cameraGranted
                        if (!cameraGranted) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            toggleSelection(chosenList, DismissType.QR_CODE)
                        }
                    } else {
                        toggleSelection(chosenList, DismissType.QR_CODE)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Shake item
            val isShakeSelected = chosenList.contains(DismissType.SHAKE)
            MissionRowItem(
                title = "Shake",
                badgeText = null,
                badgeColor = null,
                iconType = MissionIconType.SHAKE,
                isSelected = isShakeSelected,
                onClick = {
                    haptics.pulseAppleButtonClick()
                    toggleSelection(chosenList, DismissType.SHAKE)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Confirm selection button
            Button(
                onClick = {
                    haptics.pulseAppleSuccess()
                    if (chosenList.contains(DismissType.QR_CODE)) {
                        val cameraGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!cameraGranted) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            return@Button
                        }
                    }
                    onMissionsConfirmed(chosenList.toList())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("apply_mission_selection_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text(
                    text = if (chosenList.isEmpty()) "Done (None Selected)" else "Done (${chosenList.size}/5)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

private fun toggleSelection(list: MutableList<DismissType>, type: DismissType) {
    if (list.contains(type)) {
        list.remove(type)
    } else {
        if (list.size < 5) {
            list.add(type)
        }
    }
}

private enum class MissionIconType {
    TYPING, MATH, STEP, QR_CODE, SHAKE
}

/**
 * Single mission row exactly styled matching the attached reference:
 * - Rounded soft container with slight grey background when active/hovered
 * - Icon container in soft blue / soft lavender tint with rounded corners
 * - Title text in clean bold font
 * - Optional "BEST" / "HOT" badges in pastel pill tags
 * - Subtle check indicator if selected
 */
@Composable
private fun MissionRowItem(
    title: String,
    badgeText: String?,
    badgeColor: Color?,
    iconType: MissionIconType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Color(0xFFF1F5F9) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFCBD5E1) else Color(0x0A000000),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("mission_option_${title.lowercase()}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Icon
                when (iconType) {
                    MissionIconType.TYPING -> TypingIconSquare()
                    MissionIconType.MATH -> MathIconSquare()
                    MissionIconType.STEP -> StepIconSquare()
                    MissionIconType.QR_CODE -> QrIconSquare()
                    MissionIconType.SHAKE -> ShakeIconSquare()
                }

                // Title
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )

                // Optional Badge (e.g. "BEST", "HOT")
                if (badgeText != null && badgeColor != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Selection indicator check circle
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
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

/**
 * 1. Typing Icon Square (Cyan/Sky blue rounded badge with keyboard pattern)
 */
@Composable
private fun TypingIconSquare() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFDFF6FF)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val w = size.width
            val h = size.height
            val color = Color(0xFF06B6D4) // Sky cyan

            // Keyboard outline
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 3.dp.toPx()),
                size = Size(w, h - 6.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Keyboard keys
            val keyW = 3.dp.toPx()
            val keyH = 2.dp.toPx()
            val y1 = h * 0.35f
            val y2 = h * 0.52f

            for (i in 0..3) {
                val x = w * 0.18f + i * (w * 0.18f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y1),
                    size = Size(keyW, keyH),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }

            // Spacebar
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.28f, y2),
                size = Size(w * 0.44f, keyH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

/**
 * 2. Math Icon Square (Cyan rounded badge with plus & multiplication symbols)
 */
@Composable
private fun MathIconSquare() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE0F2FE)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val color = Color(0xFF0EA5E9)
            val strokeW = 2.5.dp.toPx()

            // Plus symbol on top-left
            val plusCx = size.width * 0.35f
            val plusCy = size.height * 0.35f
            val arm = size.width * 0.16f
            drawLine(color, Offset(plusCx - arm, plusCy), Offset(plusCx + arm, plusCy), strokeW, StrokeCap.Round)
            drawLine(color, Offset(plusCx, plusCy - arm), Offset(plusCx, plusCy + arm), strokeW, StrokeCap.Round)

            // Multiply 'x' symbol on bottom-right
            val mulCx = size.width * 0.68f
            val mulCy = size.height * 0.68f
            val mulArm = size.width * 0.13f
            drawLine(color, Offset(mulCx - mulArm, mulCy - mulArm), Offset(mulCx + mulArm, mulCy + mulArm), strokeW, StrokeCap.Round)
            drawLine(color, Offset(mulCx + mulArm, mulCy - mulArm), Offset(mulCx - mulArm, mulCy + mulArm), strokeW, StrokeCap.Round)
        }
    }
}

/**
 * 3. Step Icon Square (Soft lavender rounded badge with two footprint marks)
 */
@Composable
private fun StepIconSquare() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3E8FF)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val color = Color(0xFFA855F7) // Lavender purple

            // Left footprint (heel + foot pad + toes)
            val leftCx = size.width * 0.33f
            drawOval(
                color = color,
                topLeft = Offset(leftCx - 3.5.dp.toPx(), size.height * 0.38f),
                size = Size(7.dp.toPx(), 11.dp.toPx())
            )
            // Left heel
            drawOval(
                color = color,
                topLeft = Offset(leftCx - 2.8.dp.toPx(), size.height * 0.70f),
                size = Size(5.6.dp.toPx(), 5.dp.toPx())
            )
            // Left toes
            drawCircle(color, 1.2.dp.toPx(), Offset(leftCx - 2.5.dp.toPx(), size.height * 0.30f))
            drawCircle(color, 1.4.dp.toPx(), Offset(leftCx, size.height * 0.28f))
            drawCircle(color, 1.2.dp.toPx(), Offset(leftCx + 2.5.dp.toPx(), size.height * 0.30f))

            // Right footprint (staggered slightly higher)
            val rightCx = size.width * 0.67f
            drawOval(
                color = color,
                topLeft = Offset(rightCx - 3.5.dp.toPx(), size.height * 0.20f),
                size = Size(7.dp.toPx(), 11.dp.toPx())
            )
            // Right heel
            drawOval(
                color = color,
                topLeft = Offset(rightCx - 2.8.dp.toPx(), size.height * 0.52f),
                size = Size(5.6.dp.toPx(), 5.dp.toPx())
            )
            // Right toes
            drawCircle(color, 1.2.dp.toPx(), Offset(rightCx - 2.5.dp.toPx(), size.height * 0.12f))
            drawCircle(color, 1.4.dp.toPx(), Offset(rightCx, size.height * 0.10f))
            drawCircle(color, 1.2.dp.toPx(), Offset(rightCx + 2.5.dp.toPx(), size.height * 0.12f))
        }
    }
}

/**
 * 4. QR/Barcode Icon Square (Lavender badge with QR finder patterns)
 */
@Composable
private fun QrIconSquare() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3E8FF)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val color = Color(0xFFA855F7)
            val strokeW = 1.8.dp.toPx()
            val boxSize = size.width * 0.38f

            // Top-left finder box
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = strokeW)
            )
            drawRect(
                color = color,
                topLeft = Offset(boxSize * 0.3f, boxSize * 0.3f),
                size = Size(boxSize * 0.4f, boxSize * 0.4f)
            )

            // Top-right finder box
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - boxSize, 0f),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = strokeW)
            )
            drawRect(
                color = color,
                topLeft = Offset(size.width - boxSize + boxSize * 0.3f, boxSize * 0.3f),
                size = Size(boxSize * 0.4f, boxSize * 0.4f)
            )

            // Bottom-left finder box
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, size.height - boxSize),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = strokeW)
            )
            drawRect(
                color = color,
                topLeft = Offset(boxSize * 0.3f, size.height - boxSize + boxSize * 0.3f),
                size = Size(boxSize * 0.4f, boxSize * 0.4f)
            )

            // Bottom-right QR data dots
            drawRect(
                color = color,
                topLeft = Offset(size.width - boxSize * 0.8f, size.height - boxSize * 0.8f),
                size = Size(boxSize * 0.35f, boxSize * 0.35f)
            )
        }
    }
}

/**
 * 5. Shake Icon Square (Soft lavender badge with angled phone and vibration waves)
 */
@Composable
private fun ShakeIconSquare() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3E8FF)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val color = Color(0xFFA855F7)
            val strokeW = 1.8.dp.toPx()

            // Angled phone outline
            val path = Path().apply {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Draw rotated rectangular phone shape
                moveTo(cx - 5.dp.toPx(), cy - 9.dp.toPx())
                lineTo(cx + 6.dp.toPx(), cy - 6.dp.toPx())
                lineTo(cx + 2.dp.toPx(), cy + 9.dp.toPx())
                lineTo(cx - 9.dp.toPx(), cy + 6.dp.toPx())
                close()
            }
            drawPath(path = path, color = color, style = Stroke(width = strokeW, cap = StrokeCap.Round))

            // Screen inside
            val screenPath = Path().apply {
                val cx = size.width / 2f
                val cy = size.height / 2f
                moveTo(cx - 4.dp.toPx(), cy - 6.dp.toPx())
                lineTo(cx + 4.dp.toPx(), cy - 4.dp.toPx())
                lineTo(cx + 1.dp.toPx(), cy + 6.dp.toPx())
                lineTo(cx - 7.dp.toPx(), cy + 4.dp.toPx())
                close()
            }
            drawPath(path = screenPath, color = color.copy(alpha = 0.35f))

            // Left vibration arc
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, size.height * 0.25f),
                size = Size(6.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Right vibration arc
            drawArc(
                color = color,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width - 6.dp.toPx(), size.height * 0.25f),
                size = Size(6.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
