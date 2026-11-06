package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.alarm.rememberAppleHaptics
import com.example.ui.theme.ThemeMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.admin.AntiCheatProtectionManager
import com.example.admin.DeviceAdminManager
import com.example.data.auth.GoogleAuthState
import com.example.data.model.UserProfileEntity
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.GoogleIcon

private enum class SettingsPage {
    MAIN,
    PREVENT_POWER_OFF,
    DEVICE_ADMIN_ACTIVATION,
    ALARM_OPTIMIZATION
}

@Composable
fun SettingsScreen(
    userProfile: UserProfileEntity?,
    googleAuthState: GoogleAuthState = GoogleAuthState(),
    effectiveGoogleClientId: String? = null,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onSignInGoogle: () -> Unit = {},
    onSignInGoogleWithClientId: (String?) -> Unit = {},
    onSignInGoogleDemo: (String, String) -> Unit = { _, _ -> },
    onSignOutGoogle: () -> Unit = {},
    onClearGoogleAuthError: () -> Unit = {},
    onUpdateProfile: (UserProfileEntity) -> Unit = {},
    onTestAlarm: () -> Unit = {},
    onTestShakeAlarm: () -> Unit = {},
    onTestColorTilesAlarm: () -> Unit = {},
    onTestTypingAlarm: () -> Unit = {},
    onTestStepAlarm: () -> Unit = {},
    onTestBarcodeAlarm: () -> Unit = {},
    onTestMorningReport: () -> Unit = {},
    onRestartOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminManager = remember { DeviceAdminManager(context) }
    var currentSubPage by remember { mutableStateOf(SettingsPage.MAIN) }

    // Anti-cheat and anti-uninstall state
    val isPowerOffPrevented by AntiCheatProtectionManager.isPreventPowerOffEnabled.collectAsState()
    val powerOffBlocksCount by AntiCheatProtectionManager.powerOffBlocksCount.collectAsState()
    var isAntiUninstallActive by remember { mutableStateOf(adminManager.isAntiUninstallActive()) }

    // Recheck device admin whenever returning to the screen from Android Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAntiUninstallActive = adminManager.isAntiUninstallActive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showNoticesDialog by remember { mutableStateOf(false) }

    val activity = context as? Activity

    BackHandler(enabled = currentSubPage != SettingsPage.MAIN) {
        currentSubPage = SettingsPage.MAIN
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentSubPage) {
            SettingsPage.MAIN -> {
                SettingsMainView(
                    userProfile = userProfile,
                    googleAuthState = googleAuthState,
                    onSignInGoogle = onSignInGoogle,
                    onSignOutGoogle = onSignOutGoogle,
                    isPowerOffPrevented = isPowerOffPrevented,
                    isAntiUninstallActive = isAntiUninstallActive,
                    themeMode = themeMode,
                    onToggleAntiUninstall = { targetEnabled ->
                        if (targetEnabled) {
                            currentSubPage = SettingsPage.DEVICE_ADMIN_ACTIVATION
                        } else {
                            if (AntiCheatProtectionManager.isAlarmMissionActive.value) {
                                Toast.makeText(
                                    context,
                                    "⚠️ Cannot deactivate Device Admin while wake-up alarm is ringing!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val deactivated = adminManager.deactivateAdmin()
                                isAntiUninstallActive = !deactivated && adminManager.isAntiUninstallActive()
                                Toast.makeText(context, "Anti-uninstall protection deactivated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onOpenProfile = {
                        if (userProfile?.isGoogleLinked != true && !googleAuthState.isSignedIn) {
                            onSignInGoogle()
                        }
                    },
                    onOpenPreventPowerOff = { currentSubPage = SettingsPage.PREVENT_POWER_OFF },
                    onOpenAlarmOptimization = { currentSubPage = SettingsPage.ALARM_OPTIMIZATION },
                    onOpenTheme = { showThemeDialog = true },
                    onOpenSoundOutput = { Toast.makeText(context, "Sound output: Current device speaker", Toast.LENGTH_SHORT).show() },
                    onOpenNotificationSetting = { Toast.makeText(context, "All notification channels active", Toast.LENGTH_SHORT).show() },
                    onOpenSystemConfig = { Toast.makeText(context, "App language: English", Toast.LENGTH_SHORT).show() },
                    onOpenNotices = { showNoticesDialog = true },
                    onOpenFaq = { showFaqDialog = true },
                    onOpenSendFeedback = { showFeedbackDialog = true },
                    onOpenCopyright = { Toast.makeText(context, "Uthja copyright 2026. All rights reserved.", Toast.LENGTH_SHORT).show() },
                    onOpenAbout = { showAboutDialog = true }
                )
            }
            SettingsPage.PREVENT_POWER_OFF -> {
                PreventPowerOffView(
                    isEnabled = isPowerOffPrevented,
                    blocksCount = powerOffBlocksCount,
                    isAntiUninstallActive = isAntiUninstallActive,
                    onToggle = { AntiCheatProtectionManager.setPreventPowerOffEnabled(context, it) },
                    onRequestDeviceAdmin = { currentSubPage = SettingsPage.DEVICE_ADMIN_ACTIVATION },
                    onTestMission = onTestShakeAlarm,
                    onBack = { currentSubPage = SettingsPage.MAIN }
                )
            }
            SettingsPage.DEVICE_ADMIN_ACTIVATION -> {
                DeviceAdminActivationView(
                    onActivate = {
                        try {
                            val intent = adminManager.createActivationIntent(
                                "Prevents deleting or stopping the alarm app to bypass morning wake-up challenges."
                            )
                            activity?.startActivity(intent)
                            Toast.makeText(context, "Please tap 'Activate' to lock uninstall", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to launch admin activation", Toast.LENGTH_SHORT).show()
                        }
                        currentSubPage = SettingsPage.MAIN
                    },
                    onCancel = { currentSubPage = SettingsPage.MAIN },
                    onUninstall = {
                        if (AntiCheatProtectionManager.isAlarmMissionActive.value) {
                            Toast.makeText(
                                context,
                                "⚠️ Cannot deactivate or uninstall while alarm mission is ringing!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            adminManager.deactivateAdmin()
                            isAntiUninstallActive = false
                            Toast.makeText(context, "Admin rights removed", Toast.LENGTH_SHORT).show()
                        }
                        currentSubPage = SettingsPage.MAIN
                    }
                )
            }
            SettingsPage.ALARM_OPTIMIZATION -> {
                AlarmOptimizationView(
                    onBack = { currentSubPage = SettingsPage.MAIN },
                    onSendFeedback = { showFeedbackDialog = true }
                )
            }
        }

        // --- Dialogs ---

        if (showFeedbackDialog) {
            FeedbackDialog(onDismiss = { showFeedbackDialog = false })
        }

        if (showAboutDialog) {
            AboutAppDialog(onDismiss = { showAboutDialog = false })
        }

        if (showFaqDialog) {
            FaqDialog(onDismiss = { showFaqDialog = false })
        }

        if (showNoticesDialog) {
            NoticesDialog(onDismiss = { showNoticesDialog = false })
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = themeMode,
                onSelectTheme = { selectedMode ->
                    onThemeModeChanged(selectedMode)
                },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 1. MAIN SETTINGS SCREEN (Exact matching Screenshots 1 & 2)
// -----------------------------------------------------------------------------
@Composable
private fun SettingsMainView(
    userProfile: UserProfileEntity?,
    googleAuthState: GoogleAuthState,
    onSignInGoogle: () -> Unit,
    onSignOutGoogle: () -> Unit,
    isPowerOffPrevented: Boolean,
    isAntiUninstallActive: Boolean,
    themeMode: ThemeMode,
    onToggleAntiUninstall: (Boolean) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPreventPowerOff: () -> Unit,
    onOpenAlarmOptimization: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenSoundOutput: () -> Unit,
    onOpenNotificationSetting: () -> Unit,
    onOpenSystemConfig: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenSendFeedback: () -> Unit,
    onOpenCopyright: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // Title: Settings
        Text(
            text = "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .testTag("settings_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Top Grouped Card (Profile, Pro, Prevent power-off, Prevent app uninstall)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.04f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                // Row 1: Profile (DJing Anil / Google User)
                val isGoogleSignedIn = (userProfile?.isGoogleLinked == true) || googleAuthState.isSignedIn
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProfile)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar: Gmail profile picture if available, else cute penguin
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = userProfile?.photoUrl?.ifBlank { null }
                                ?: googleAuthState.user?.photoUrl?.ifBlank { null }
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_penguin_avatar),
                                    contentDescription = "Profile avatar",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            val displayName = userProfile?.name?.ifBlank { null }
                                ?: googleAuthState.user?.displayName?.ifBlank { null }
                                ?: "DJing Anil"
                            Text(
                                text = displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E232A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val displayEmail = userProfile?.email?.ifBlank { null }
                                ?: googleAuthState.user?.email?.ifBlank { null }
                            Text(
                                text = displayEmail ?: "7 days since I met Alarmy❤️",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Row: Login with Google (above Pro option, simple option, no popup, direct login by gmail)
                if (!isGoogleSignedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSignInGoogle() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                GoogleIcon(size = 16.dp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Login with Google",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E232A)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

                    if (showLogoutConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutConfirmDialog = false },
                            title = {
                                Text(
                                    text = "Log out of Google Account?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to log out? Your cloud sync and profile will be disconnected until you sign in again.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLogoutConfirmDialog = false
                                        onSignOutGoogle()
                                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.testTag("confirm_logout_button")
                                ) {
                                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showLogoutConfirmDialog = false },
                                    modifier = Modifier.testTag("cancel_logout_button")
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                GoogleIcon(size = 16.dp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Google Account",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E232A)
                                )
                                val accountEmail = userProfile?.email ?: googleAuthState.user?.email ?: "Linked"
                                Text(
                                    text = accountEmail,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("logout_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log out of Google account",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Row: Pro (Subscribed >)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Red circular badge with checkmark
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Pro",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E232A)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Subscribed",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Row 3: Prevent power-off (ON >)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPreventPowerOff)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Green shield with star
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_star),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Prevent power-off",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E232A)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPowerOffPrevented) Color(0xFF00BA68) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPowerOffPrevented) "ON" else "OFF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPowerOffPrevented) Color(0xFF00BA68) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Row 4: Prevent app uninstall (Switch toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cyan shield with lock
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_lock),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Prevent app uninstall",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E232A)
                        )
                    }

                    Switch(
                        checked = isAntiUninstallActive,
                        onCheckedChange = { onToggleAntiUninstall(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF38BDF8),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("prevent_uninstall_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 2: Detailed Settings List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Alarm optimization
                SettingsListRow(
                    title = "Alarm optimization",
                    subtitle = null,
                    onClick = onOpenAlarmOptimization
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Theme (Locked to Light mode only)
                SettingsListRow(
                    title = "Theme",
                    subtitle = "Light mode active",
                    trailingText = "Light mode",
                    onClick = {
                        Toast.makeText(context, "Light mode is default for Uthja", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Sound output
                SettingsListRow(
                    title = "Sound output",
                    subtitle = null,
                    trailingText = "Current device",
                    onClick = onOpenSoundOutput
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Notification setting
                SettingsListRow(
                    title = "Notification setting",
                    subtitle = "Service notification · Promotion & update",
                    onClick = onOpenNotificationSetting
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // System configuration
                SettingsListRow(
                    title = "System configuration",
                    subtitle = "App language · Else",
                    onClick = onOpenSystemConfig
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Bottom Links (Notices, FAQ, Send feedback, Copyright infringement report, About)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                SettingsLinkRow(text = "Notices", onClick = onOpenNotices)
                SettingsLinkRow(text = "FAQ", onClick = onOpenFaq)
                SettingsLinkRow(text = "Send feedback", onClick = onOpenSendFeedback)
                SettingsLinkRow(text = "Copyright infringement report", onClick = onOpenCopyright)
                SettingsLinkRow(text = "About", onClick = onOpenAbout)
            }
        }
    }
}

@Composable
private fun SettingsListRow(
    title: String,
    subtitle: String?,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

// -----------------------------------------------------------------------------
// 2. PREVENT POWER-OFF SUB-SCREEN (Exact matching Screenshot 3)
// -----------------------------------------------------------------------------
@Composable
private fun PreventPowerOffView(
    isEnabled: Boolean,
    blocksCount: Int,
    isAntiUninstallActive: Boolean,
    onToggle: (Boolean) -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onTestMission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPermission = remember { AntiCheatProtectionManager.canDrawOverlays(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Bar (Back button, Info icon)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E232A),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Prevents turning off device or uninstalling the app while alarm rings",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF1E232A),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = "Prevent power-off",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E232A)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Stat Row: Successful block counts + dynamic times badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Successful block counts",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E232A)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE6F9F0))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$blocksCount times",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00BA68)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Toggle Card: "Prevent power-off"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prevent power-off",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E232A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Blocks power menu & screen off during missions",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38BDF8),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section: Protection Status & Permissions
        Text(
            text = "Protection Status & Permissions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E232A)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Card 1: Display over apps
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasOverlayPermission) Color(0xFF00BA68) else Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasOverlayPermission) "Display over apps (Active)" else "Display over apps (Action Required)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasOverlayPermission) Color(0xFF00BA68) else Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasOverlayPermission) {
                                "Allows Lune to display dismiss challenge over power dialogs and system overlays."
                            } else {
                                "Permission recommended so Lune can immediately cover the power-off menu when pressed."
                            },
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }

                if (!hasOverlayPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                context.startActivity(AntiCheatProtectionManager.getOverlayPermissionIntent(context))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Overlay Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Prevent App Uninstall (Device Administrator)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isAntiUninstallActive) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isAntiUninstallActive) Color(0xFF00BA68) else Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAntiUninstallActive) "Prevent App Uninstall (Active)" else "Prevent App Uninstall (Inactive)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAntiUninstallActive) Color(0xFF00BA68) else Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAntiUninstallActive) {
                                "Device Administrator is activated. Android OS completely blocks uninstallation or force-stop during wake-up challenges."
                            } else {
                                "Activate Device Administrator to prevent deleting or stopping Lune when morning alarm sounds."
                            },
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }

                if (!isAntiUninstallActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestDeviceAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activate Anti-Uninstall Protection", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: Power-off & Screen-off Interception
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00BA68),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Hardware Key & Power Interception",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BA68)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detects power button presses, screen-off events, and system dialogs. The screen immediately wakes back up and re-locks to the alarm mission.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Test Protection Button
        Button(
            onClick = onTestMission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E232A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Test Mission Protection Now",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------
// 3. ACTIVATE DEVICE ADMIN APP? SUB-SCREEN (Exact matching Screenshot 4)
// -----------------------------------------------------------------------------
@Composable
private fun DeviceAdminActivationView(
    onActivate: () -> Unit,
    onCancel: () -> Unit,
    onUninstall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Back arrow
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1E232A),
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Title: Activate device admin app?
            Text(
                text = "Activate device\nadmin app?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E232A),
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Alarmy Icon & Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Alarmy",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E232A)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Activating this admin app will allow the Alarmy app to perform the following operations:",
                fontSize = 15.sp,
                color = Color(0xFF4E5968),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

        // Action 1: Activate this device admin app
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Activate this device admin app",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD97706)
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

        // Action 2: Cancel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCancel)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Cancel",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD97706)
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

        // Action 3: Uninstall app
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onUninstall)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Uninstall app",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD97706)
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
    }
}

// -----------------------------------------------------------------------------
// 4. ALARM OPTIMIZATION SUB-SCREEN (Exact matching Screenshots 5 & 6)
// -----------------------------------------------------------------------------
@Composable
private fun AlarmOptimizationView(
    onBack: () -> Unit,
    onSendFeedback: () -> Unit
) {
    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    var expanded3 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E232A),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Alarm optimization",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E232A),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Title: Your alarm isn't ringing?
        Text(
            text = "Your alarm isn’t ringing?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E232A)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = "Alarms may be blocked by the phone’s system. 😢 Check the following guidelines!",
            fontSize = 14.5.sp,
            color = Color(0xFF4E5968),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Card 1: Essential permission for alarm
        OptimizationAccordionCard(
            title = "Essential permission for alarm",
            iconRes = R.drawable.ic_music_off,
            iconTint = Color(0xFFEF4444),
            iconBg = Color(0xFFFFECEE),
            isExpanded = expanded1,
            onToggle = { expanded1 = !expanded1 },
            expandedContent = "Exact Alarm Permission is granted. Alarm will ring on time even in Doze Mode and lock screen."
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Allow Alarmy in Do Not Disturb mode
        OptimizationAccordionCard(
            title = "Allow Alarmy in Do Not Disturb mode",
            iconRes = R.drawable.ic_shield_star,
            iconTint = Color(0xFF3B82F6),
            iconBg = Color(0xFFEFF6FF),
            isExpanded = expanded2,
            onToggle = { expanded2 = !expanded2 },
            expandedContent = "Alarms are tagged with AudioAttributes.USAGE_ALARM which bypasses DND and silent switches automatically."
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: Exclude from battery optimization
        OptimizationAccordionCard(
            title = "Exclude from battery optimization",
            iconRes = R.drawable.ic_shield_lock,
            iconTint = Color(0xFF6366F1),
            iconBg = Color(0xFFEEF2FF),
            isExpanded = expanded3,
            onToggle = { expanded3 = !expanded3 },
            expandedContent = "Battery optimization exemption prevents background killer daemons from terminating upcoming wake-up jobs."
        )

        Spacer(modifier = Modifier.height(50.dp))

        // Bottom Contact Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "If you’re facing any problems, contact us!",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSendFeedback,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD6D9E0),
                    contentColor = Color(0xFF1E232A)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Send feedback",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun OptimizationAccordionCard(
    title: String,
    iconRes: Int,
    iconTint: Color,
    iconBg: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedContent: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E232A)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = expandedContent,
                    fontSize = 13.5.sp,
                    color = Color(0xFF4E5968),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helper Dialogs
// -----------------------------------------------------------------------------
@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    var feedbackText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Send Feedback", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Help us improve your waking & sleeping experience!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Describe your feedback or question...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Uthja v2.4.0", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Designed for intentional waking and healthy sleep routines.", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Anti-snooze cheat prevention & sleep tracking enabled.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun FaqDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Q: How does Prevent Power-Off work?", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("A: It uses accessibility and overlay services to detect shutdown attempts while an alarm is ringing.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Q: Why do I need Device Administrator?", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("A: To prevent drowsy morning uninstalls designed to silence challenges.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun NoticesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notices", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Version 2.4.0: Modern sleep telemetry & report tabs now live.", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• Keep your phone charging overnight when using sleep tracking.", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppleHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "App Theme",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose light, dark, or system mode",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOptionItem(
                    themeMode = ThemeMode.SYSTEM,
                    isSelected = currentTheme == ThemeMode.SYSTEM,
                    icon = Icons.Default.BrightnessAuto,
                    iconColor = Color(0xFF38BDF8),
                    onClick = {
                        haptics.pulseAppleSelection()
                        onSelectTheme(ThemeMode.SYSTEM)
                    }
                )

                ThemeOptionItem(
                    themeMode = ThemeMode.LIGHT,
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFF59E0B),
                    onClick = {
                        haptics.pulseAppleSelection()
                        onSelectTheme(ThemeMode.LIGHT)
                    }
                )

                ThemeOptionItem(
                    themeMode = ThemeMode.DARK,
                    isSelected = currentTheme == ThemeMode.DARK,
                    icon = Icons.Default.Bedtime,
                    iconColor = Color(0xFFA78BFA),
                    onClick = {
                        haptics.pulseAppleSelection()
                        onSelectTheme(ThemeMode.DARK)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun ThemeOptionItem(
    themeMode: ThemeMode,
    isSelected: Boolean,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = themeMode.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = themeMode.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 6.dp else 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            )
        }
    }
}
