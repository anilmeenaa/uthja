package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.DismissType
import com.example.data.model.WakeMode
import com.example.ui.ActiveAlarmState

/**
 * Mission selection data model for display
 */
data class MissionOption(
    val type: DismissType,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val iconBgColor: Color,
    val iconTintColor: Color
)

/**
 * Wake-Up Mission Selection Screen matching the exact visual reference image (bc60):
 * - Soft off-white / lavender tint background
 * - Top back arrow navigation
 * - Large title: "Choose a wake-up\nmission"
 * - Subtitle: "Complete a small challenge to turn off your alarm."
 * - All 5 missions:
 *     1. Typing: "Type the given sentence"
 *     2. Math: "Solve simple math questions"
 *     3. Step: "Walk steps to wake up"
 *     4. QR/Barcode: "Scan a QR or Barcode to turn off"
 *     5. Shake: "Shake your phone multiple times"
 * - Multi-selection support (up to 5 missions) with circular checkmark selectors (accent red/pink fill when selected, light grey outline when unselected)
 * - Bottom full-width rounded pill action button: "Next" (#FD355D / #FF3B5C)
 */
@Composable
fun WakeUpMissionPickerScreen(
    currentMissions: List<DismissType> = listOf(DismissType.TYPING),
    onMissionsConfirmed: (List<DismissType>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()
    var selectedMissions by remember {
        mutableStateOf(
            if (currentMissions.isNotEmpty()) currentMissions.distinct() else listOf(DismissType.MATH)
        )
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
            Toast.makeText(context, "Camera permission granted! Barcode mission is ready.", Toast.LENGTH_SHORT).show()
            if (!selectedMissions.contains(DismissType.QR_CODE)) {
                selectedMissions = toggleMission(selectedMissions, DismissType.QR_CODE)
            }
        } else {
            Toast.makeText(context, "Camera permission needed to scan barcodes during alarm.", Toast.LENGTH_LONG).show()
            if (!selectedMissions.contains(DismissType.QR_CODE)) {
                selectedMissions = toggleMission(selectedMissions, DismissType.QR_CODE)
            }
        }
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Motion tracking permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Preview state for interactive testing if desired
    var previewMissionType by remember { mutableStateOf<DismissType?>(null) }
    var previewShakeCount by remember { mutableIntStateOf(0) }

    BackHandler {
        if (previewMissionType != null) {
            previewMissionType = null
            previewShakeCount = 0
        } else {
            onDismiss()
        }
    }

    val missionOptions = remember {
        listOf(
            MissionOption(
                type = DismissType.OFF,
                title = "No Mission",
                subtitle = "Turn off alarm immediately without challenge",
                iconRes = R.drawable.ic_mission_off,
                iconBgColor = Color(0xFFF1F5F9),
                iconTintColor = Color(0xFF64748B)
            ),
            MissionOption(
                type = DismissType.MATH,
                title = "Math",
                subtitle = "Solve simple math questions",
                iconRes = R.drawable.ic_mission_math,
                iconBgColor = Color(0xFFEBF8EE),
                iconTintColor = Color(0xFF22C55E)
            ),
            MissionOption(
                type = DismissType.TYPING,
                title = "Typing",
                subtitle = "Type the given sentence",
                iconRes = R.drawable.ic_mission_typing,
                iconBgColor = Color(0xFFF3EEFD),
                iconTintColor = Color(0xFF8B5CF6)
            ),
            MissionOption(
                type = DismissType.SHAKE,
                title = "Shake",
                subtitle = "Shake your phone multiple times",
                iconRes = R.drawable.ic_mission_shake,
                iconBgColor = Color(0xFFFFF4E6),
                iconTintColor = Color(0xFFF59E0B)
            ),
            MissionOption(
                type = DismissType.COLOR_TILES,
                title = "Color Tiles",
                subtitle = "Find and match color tiles",
                iconRes = R.drawable.ic_mission_colortiles,
                iconBgColor = Color(0xFFE8F5E9),
                iconTintColor = Color(0xFF2E7D32)
            ),
            MissionOption(
                type = DismissType.STEP,
                title = "Step",
                subtitle = "Walk steps to wake up",
                iconRes = R.drawable.ic_mission_step_exact,
                iconBgColor = Color(0xFFFEF2EE),
                iconTintColor = Color(0xFFF97316)
            ),
            MissionOption(
                type = DismissType.QR_CODE,
                title = "QR/Barcode",
                subtitle = "Scan a QR or Barcode to turn off",
                iconRes = R.drawable.ic_mission_qr_exact,
                iconBgColor = Color(0xFFF5EEFE),
                iconTintColor = Color(0xFFA855F7)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F3F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. TOP BAR with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("mission_picker_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E1E24),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. SCROLLABLE CONTENT BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "Choose a wake-up\nmission",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1A24),
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = "Complete a small challenge to turn off your alarm.",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6E6877),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3. 5 MISSION CARDS
                missionOptions.forEach { option ->
                    val isSelected = selectedMissions.contains(option.type)
                    MissionOptionCard(
                        option = option,
                        isSelected = isSelected,
                        hasCameraPermission = hasCameraPermission,
                        onClick = {
                            haptics.pulseAppleSelection()
                            if (option.type == DismissType.QR_CODE) {
                                val cameraGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                hasCameraPermission = cameraGranted
                                if (!isSelected) {
                                    // User clicked to add/select barcode mission: take one-time permission now!
                                    if (!cameraGranted) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        selectedMissions = toggleMission(selectedMissions, option.type)
                                    }
                                } else {
                                    selectedMissions = toggleMission(selectedMissions, option.type)
                                }
                            } else if (option.type == DismissType.STEP) {
                                val isStepSelected = selectedMissions.contains(DismissType.STEP)
                                if (!isStepSelected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val stepGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACTIVITY_RECOGNITION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!stepGranted) {
                                        activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    }
                                }
                                selectedMissions = toggleMission(selectedMissions, option.type)
                            } else {
                                selectedMissions = toggleMission(selectedMissions, option.type)
                            }
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. BOTTOM ACTION BUTTON ("Next")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F3F8))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        haptics.pulseAppleSuccess()
                        if (selectedMissions.contains(DismissType.QR_CODE)) {
                            val cameraGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!cameraGranted) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                return@Button
                            }
                        }
                        onMissionsConfirmed(selectedMissions.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(27.dp),
                            spotColor = Color(0x66FD355D)
                        )
                        .testTag("mission_picker_confirm_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFD355D),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Next",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Interactive Previews Overlay
        AnimatedVisibility(
            visible = previewMissionType != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (previewMissionType) {
                DismissType.MATH -> {
                    MathMissionScreen(
                        onDismissConquered = {
                            previewMissionType = null
                            selectedMissions = (listOf(DismissType.MATH) + (selectedMissions - DismissType.MATH)).distinct()
                        }
                    )
                }
                DismissType.TYPING -> {
                    TypingMissionScreen(
                        onDismissConquered = {
                            previewMissionType = null
                            selectedMissions = (listOf(DismissType.TYPING) + (selectedMissions - DismissType.TYPING)).distinct()
                        }
                    )
                }
                DismissType.STEP -> {
                    StepMissionScreen(
                        onDismissConquered = {
                            previewMissionType = null
                            selectedMissions = (listOf(DismissType.STEP) + (selectedMissions - DismissType.STEP)).distinct()
                        }
                    )
                }
                DismissType.QR_CODE -> {
                    QrBarcodeMissionScreen(
                        onDismissConquered = {
                            previewMissionType = null
                            selectedMissions = (listOf(DismissType.QR_CODE) + (selectedMissions - DismissType.QR_CODE)).distinct()
                        }
                    )
                }
                DismissType.SHAKE -> {
                    ActiveAlarmScreen(
                        state = ActiveAlarmState(
                            isRinging = true,
                            title = "Shake to Dismiss",
                            wakeMode = WakeMode.FOCUS,
                            dismissType = DismissType.SHAKE,
                            shakeTargetCount = 10,
                            shakeCurrentCount = previewShakeCount
                        ),
                        onDismissConquered = {
                            previewMissionType = null
                            previewShakeCount = 0
                            selectedMissions = (listOf(DismissType.SHAKE) + (selectedMissions - DismissType.SHAKE)).distinct()
                        },
                        onAttemptSnooze = { },
                        onShake = {
                            previewShakeCount++
                            if (previewShakeCount >= 10) {
                                previewMissionType = null
                                previewShakeCount = 0
                                selectedMissions = selectedMissions + DismissType.SHAKE
                            }
                        },
                        onSimulateShake = {
                            previewShakeCount++
                            if (previewShakeCount >= 10) {
                                previewMissionType = null
                                previewShakeCount = 0
                                selectedMissions = selectedMissions + DismissType.SHAKE
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * Backward compatibility overload for callers passing single mission
 */
@Composable
fun WakeUpMissionPickerScreen(
    currentMission: DismissType = DismissType.TYPING,
    onMissionConfirmed: (DismissType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    WakeUpMissionPickerScreen(
        currentMissions = listOf(currentMission),
        onMissionsConfirmed = { list ->
            onMissionConfirmed(list.firstOrNull() ?: currentMission)
        },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Multi-selection toggle helper (up to 5 missions)
 * Dedicated No Mission (OFF) is mutually exclusive with active challenges.
 */
private fun toggleMission(current: List<DismissType>, type: DismissType): List<DismissType> {
    if (type == DismissType.OFF) {
        return listOf(DismissType.OFF)
    }
    val withoutOff = current.filter { it != DismissType.OFF }
    return if (withoutOff.contains(type)) {
        val updated = withoutOff.filter { it != type }
        if (updated.isEmpty()) listOf(DismissType.OFF) else updated
    } else {
        // If current selection is a single mission (e.g. initial MATH or TYPING default),
        // replace it with the newly tapped mission so clicking "QR/Barcode" directly selects QR Code!
        if (withoutOff.size == 1) {
            listOf(type)
        } else if (withoutOff.size < 5) {
            // Prioritize the newly chosen mission at the front (as primary challenge)
            listOf(type) + withoutOff
        } else {
            withoutOff
        }
    }
}

/**
 * Individual Mission Card matching the reference design:
 * - Rounded white card with soft grey border
 * - Square mission icon on the left
 * - Mission title & subtitle in the middle
 * - Circular checkbox on the right (#FD355D when selected with white check, light grey circle when unselected)
 */
@Composable
private fun MissionOptionCard(
    option: MissionOption,
    isSelected: Boolean,
    hasCameraPermission: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitleText = if (option.type == DismissType.QR_CODE && isSelected) {
        if (hasCameraPermission) "✓ Camera ready · Scan any barcode to dismiss" else "⚠️ Camera permission needed · Tap to allow"
    } else if (option.type == DismissType.STEP && isSelected) {
        "Walk 30 steps in real-time"
    } else {
        option.subtitle
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFD355D) else Color(0xFFE8E5EA),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("mission_option_${option.type.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = option.iconRes),
                    contentDescription = option.title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title & Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B26)
                    )
                    if (option.type == DismissType.QR_CODE && isSelected && hasCameraPermission) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Camera Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitleText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (option.type == DismissType.QR_CODE && isSelected && !hasCameraPermission) Color(0xFFEF4444) else Color(0xFF6B6575)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Circular Checkmark Selector
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFFFD355D) else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) Color.Transparent else Color(0xFFCFC9D6),
                        shape = CircleShape
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
