package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.LuneColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    userProfile: UserProfileEntity? = null,
    onToggleTask: (TaskEntity) -> Unit,
    onAddTask: (title: String, timePill: String?, iconType: String, reminderTime: String?, scheduledDate: String?, isDaily: Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onResetTasks: () -> Unit,
    onUpdateTaskReminder: (TaskEntity, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppleHaptics()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToSetReminder by remember { mutableStateOf<TaskEntity?>(null) }
    var isCompletedSectionExpanded by remember { mutableStateOf(true) }

    // Track task IDs currently executing the checkmark scale + slide out animation
    var completingTaskIds by remember { mutableStateOf(setOf<Long>()) }

    // Dynamic date display (e.g. "Thursday, November 5")
    val formattedDateSubtitle = remember(currentDate) {
        formatDateToDisplay(currentDate)
    }

    // 1. Strictly filter tasks scheduled for today (or recurring daily habits)
    val todayTasks = remember(tasks, currentDate) {
        tasks.filter { it.isScheduledForDate(currentDate) }
    }

    val activeTasks = remember(todayTasks, currentDate) {
        todayTasks.filter { !it.isCompletedForDate(currentDate) }
    }

    val completedTasks = remember(todayTasks, currentDate) {
        todayTasks.filter { it.isCompletedForDate(currentDate) }
    }

    val completedCount = completedTasks.size
    val totalCount = todayTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "task_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: "Today's Routine" with dynamic date below
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Routine",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuneColors.text
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formattedDateSubtitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = LuneColors.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            onResetTasks()
                        },
                        modifier = Modifier.testTag("reset_tasks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Tasks",
                            tint = LuneColors.textSoft
                        )
                    }
                }
            }

            // Summary Progress Card with smooth animated bar and counts
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = LuneColors.surfaceSoft),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalCount == 0) "No tasks scheduled for today" else "$completedCount of $totalCount completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LuneColors.text
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuneColors.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LuneColors.primary,
                        trackColor = LuneColors.surfaceMuted,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Task List
            if (todayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tasks planned for today",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = LuneColors.textSoft
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + below to schedule today's morning or evening routine",
                            fontSize = 13.sp,
                            color = LuneColors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active today tasks with slide-away exit transition
                    items(activeTasks, key = { it.id }) { task ->
                        val isCompleting = task.id in completingTaskIds

                        AnimatedVisibility(
                            visible = !isCompleting,
                            enter = fadeIn(tween(220)) + expandVertically(),
                            exit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 220))
                        ) {
                            TaskRowItem(
                                task = task,
                                isCompleted = isCompleting,
                                isCompleting = isCompleting,
                                onToggle = {
                                    if (task.id !in completingTaskIds) {
                                        completingTaskIds = completingTaskIds + task.id
                                        haptics.pulseAppleSuccess()
                                        scope.launch {
                                            delay(280L)
                                            onToggleTask(task)
                                            completingTaskIds = completingTaskIds - task.id
                                        }
                                    }
                                },
                                onDelete = {
                                    haptics.pulseAppleButtonClick()
                                    onDeleteTask(task)
                                },
                                onSetReminder = {
                                    taskToSetReminder = task
                                }
                            )
                        }
                    }

                    // Collapsible Completed Tasks Section at bottom
                    if (completedTasks.isNotEmpty()) {
                        item(key = "completed_section_header") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptics.pulseAppleSelection()
                                        isCompletedSectionExpanded = !isCompletedSectionExpanded
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Completed (${completedTasks.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Icon(
                                    imageVector = if (isCompletedSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isCompletedSectionExpanded) "Collapse" else "Expand",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isCompletedSectionExpanded) {
                            items(completedTasks, key = { "completed_${it.id}" }) { task ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    TaskRowItem(
                                        task = task,
                                        isCompleted = true,
                                        isCompleting = false,
                                        onToggle = {
                                            haptics.pulseAppleButtonClick()
                                            onToggleTask(task)
                                        },
                                        onDelete = {
                                            haptics.pulseAppleButtonClick()
                                            onDeleteTask(task)
                                        },
                                        onSetReminder = {
                                            taskToSetReminder = task
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }

        // FAB to add task matching Alarms screen styling & position above bottom nav
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape, spotColor = Color(0x66FF3B5C))
                .clip(CircleShape)
                .background(Color(0xFFFF3B5C))
                .clickable {
                    haptics.pulseAppleButtonClick()
                    showAddDialog = true
                }
                .testTag("add_task_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }

    // Add Task Bottom Sheet with Presets, Date Scheduling (Today/Tomorrow/Calendar), and Custom Creator
    if (showAddDialog) {
        TaskCreationBottomSheet(
            currentDate = currentDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, pill, iconType, reminder, scheduledDate, isDaily ->
                onAddTask(title, pill, iconType, reminder, scheduledDate, isDaily)
                showAddDialog = false
            }
        )
    }

    // Reminder Dialog
    taskToSetReminder?.let { task ->
        SetReminderDialog(
            task = task,
            onDismiss = { taskToSetReminder = null },
            onSave = { newReminder ->
                onUpdateTaskReminder(task, newReminder)
                taskToSetReminder = null
            }
        )
    }
}

/**
 * Backward compatibility overload for callers passing single 4-param onAddTask
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    userProfile: UserProfileEntity?,
    onToggleTask: (TaskEntity) -> Unit,
    onAddTask: (title: String, timePill: String?, iconType: String, reminderTime: String?) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onResetTasks: () -> Unit,
    onUpdateTaskReminder: (TaskEntity, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    TasksScreen(
        tasks = tasks,
        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        userProfile = userProfile,
        onToggleTask = onToggleTask,
        onAddTask = { title, timePill, iconType, reminderTime, _, _ ->
            onAddTask(title, timePill, iconType, reminderTime)
        },
        onDeleteTask = onDeleteTask,
        onResetTasks = onResetTasks,
        onUpdateTaskReminder = onUpdateTaskReminder,
        modifier = modifier
    )
}

@Composable
private fun TaskRowItem(
    task: TaskEntity,
    isCompleted: Boolean,
    isCompleting: Boolean = false,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSetReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getTaskIcon(task.iconType)

    val checkBg by animateColorAsState(
        targetValue = if (isCompleted) LuneColors.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "checkBg"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (isCompleted) LuneColors.primary else LuneColors.border,
        animationSpec = tween(durationMillis = 200),
        label = "checkBorder"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleting) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "checkScale"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) LuneColors.surfaceMuted.copy(alpha = 0.55f) else LuneColors.surfaceSoft
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with scale & pop animation
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                    .clip(CircleShape)
                    .background(checkBg)
                    .border(
                        width = 2.dp,
                        color = checkBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCompleted,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Task Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFFF1F5F9) else LuneColors.primarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF94A3B8) else LuneColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Meta details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) LuneColors.textSoft else LuneColors.text,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    task.timePill?.let { pill ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCompleted) Color(0xFFF1F5F9) else Color(0xFFFFEEF1))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pill,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCompleted) Color(0xFF94A3B8) else Color(0xFFFF3B5C)
                            )
                        }
                    }

                    if (!task.isDaily && !task.scheduledDate.isNullOrBlank()) {
                        Text(
                            text = "📅 ${formatDateToShort(task.scheduledDate)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    task.reminderTime?.let { reminder ->
                        Text(
                            text = "🔔 $reminder",
                            fontSize = 11.sp,
                            color = if (isCompleted) Color(0xFF94A3B8) else LuneColors.primary
                        )
                    }
                }
            }

            // Reminder & Delete actions
            IconButton(
                onClick = onSetReminder,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Set Reminder",
                    tint = if (task.reminderTime != null) LuneColors.primary else LuneColors.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LuneColors.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

private fun getTaskIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "WATER" -> Icons.Default.WaterDrop
        "BOOK" -> Icons.Default.MenuBook
        "MEDITATION" -> Icons.Default.SelfImprovement
        "WORKOUT" -> Icons.Default.FitnessCenter
        "COFFEE" -> Icons.Default.Coffee
        "MOON" -> Icons.Default.NightsStay
        else -> Icons.Default.Check
    }
}

private data class RoutinePreset(
    val title: String,
    val pill: String,
    val iconType: String,
    val emoji: String,
    val suggestedReminder: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCreationBottomSheet(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, timePill: String?, iconType: String, reminderTime: String?, scheduledDate: String?, isDaily: Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var timePill by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("WATER") }
    var reminderTime by remember { mutableStateOf("") }

    // Date scheduling options: "TODAY", "TOMORROW", "CUSTOM"
    var selectedScheduleOption by remember { mutableStateOf("TODAY") }
    var customScheduledDate by remember { mutableStateOf(currentDate) }
    var isDaily by remember { mutableStateOf(true) }

    val tomorrowDate = remember(currentDate) {
        getTomorrowDateString(currentDate)
    }

    val presets = remember {
        listOf(
            RoutinePreset("Drink 500ml water", "2 glasses", "WATER", "💧", "07:00 AM"),
            RoutinePreset("Morning sunlight", "10 min", "WORKOUT", "☀️", "07:15 AM"),
            RoutinePreset("Mindfulness meditation", "10 min", "MEDITATION", "🧘", "07:30 AM"),
            RoutinePreset("Morning walk & stretch", "20 min", "WORKOUT", "🏃", "07:45 AM"),
            RoutinePreset("Mindful tea / coffee", "5 min", "COFFEE", "☕", "08:15 AM"),
            RoutinePreset("Read 10 pages", "10 pages", "BOOK", "📖", "08:30 AM"),
            RoutinePreset("Plan top 3 priorities", "5 min", "BOOK", "📝", "09:00 AM"),
            RoutinePreset("Wind down & unplug", "10:30 PM", "MOON", "🌙", "10:30 PM")
        )
    }

    val iconOptions = listOf(
        "WATER" to Icons.Default.WaterDrop,
        "BOOK" to Icons.Default.MenuBook,
        "MEDITATION" to Icons.Default.SelfImprovement,
        "WORKOUT" to Icons.Default.FitnessCenter,
        "COFFEE" to Icons.Default.Coffee,
        "MOON" to Icons.Default.NightsStay
    )

    // Android DatePickerDialog for custom future date selection
    val showDatePicker = {
        val cal = Calendar.getInstance()
        val parts = customScheduledDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val m = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: cal.get(Calendar.MONTH)
        val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val pickedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            customScheduledDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pickedCal.time)
            selectedScheduleOption = "CUSTOM"
            isDaily = false
        }, y, m, d).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Routine Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Pick a routine preset or customize your schedule",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1: Presets
            Text(
                text = "⚡ Quick Routine Presets (Tap to use)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal scrolling row of preset cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .clickable {
                                haptics.pulseAppleSelection()
                                title = preset.title
                                timePill = preset.pill
                                selectedIcon = preset.iconType
                                reminderTime = preset.suggestedReminder ?: ""
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = preset.emoji, fontSize = 16.sp)
                                Text(
                                    text = preset.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEEF1))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = preset.pill,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF3B5C)
                                    )
                                }
                                preset.suggestedReminder?.let {
                                    Text(
                                        text = "· 🔔 $it",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Date Scheduling & Recurrence
            Text(
                text = "📅 Schedule Date",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Chip 1: Today
                val isTodaySelected = selectedScheduleOption == "TODAY"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isTodaySelected) Color(0xFF1E1B4B) else Color(0xFFF1F5F9))
                        .clickable {
                            haptics.pulseAppleSelection()
                            selectedScheduleOption = "TODAY"
                            customScheduledDate = currentDate
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Today",
                        fontSize = 13.5.sp,
                        fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTodaySelected) Color.White else Color(0xFF334155)
                    )
                }

                // Chip 2: Tomorrow
                val isTomorrowSelected = selectedScheduleOption == "TOMORROW"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isTomorrowSelected) Color(0xFF1E1B4B) else Color(0xFFF1F5F9))
                        .clickable {
                            haptics.pulseAppleSelection()
                            selectedScheduleOption = "TOMORROW"
                            customScheduledDate = tomorrowDate
                            isDaily = false
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tomorrow",
                        fontSize = 13.5.sp,
                        fontWeight = if (isTomorrowSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTomorrowSelected) Color.White else Color(0xFF334155)
                    )
                }

                // Chip 3: Calendar Date Picker
                val isCustomSelected = selectedScheduleOption == "CUSTOM"
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCustomSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9))
                        .border(
                            width = 1.dp,
                            color = if (isCustomSelected) Color(0xFF7C3AED) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptics.pulseAppleButtonClick()
                            showDatePicker()
                        }
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick date",
                            tint = if (isCustomSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCustomSelected) formatDateToShort(customScheduledDate) else "Pick Date",
                            fontSize = 13.sp,
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomSelected) Color.White else Color(0xFF334155),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recurrence Switch: Repeat daily routine
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDaily) Color(0xFFEDE9FE) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (isDaily) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Repeat daily routine",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isDaily) "Appears every day in Today's Routine" else "Scheduled only for ${formatDateToShort(if (selectedScheduleOption == "TOMORROW") tomorrowDate else customScheduledDate)}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Switch(
                    checked = isDaily,
                    onCheckedChange = {
                        haptics.pulseAppleButtonClick()
                        isDaily = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C3AED),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 3: Task Details Form fields
            Text(
                text = "📝 Task Details",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title (e.g. Morning stretch)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = timePill,
                    onValueChange = { timePill = it },
                    label = { Text("Goal (e.g. 15 min)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_duration_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text("Reminder (07:30 AM)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_reminder_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Icon Category", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                iconOptions.forEach { (type, vector) ->
                    val isSelected = selectedIcon == type
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFF3B5C) else Color(0xFFF1F5F9))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFF3B5C) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                            .clickable {
                                haptics.pulseAppleSelection()
                                selectedIcon = type
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = type,
                            tint = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        haptics.pulseAppleSuccess()
                        val finalDate = when (selectedScheduleOption) {
                            "TOMORROW" -> tomorrowDate
                            "CUSTOM" -> customScheduledDate
                            else -> currentDate
                        }
                        onConfirm(
                            title.trim(),
                            timePill.trim().ifBlank { null },
                            selectedIcon,
                            reminderTime.trim().ifBlank { null },
                            if (isDaily) null else finalDate,
                            isDaily
                        )
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B5C),
                    disabledContainerColor = Color(0xFFFFCCD5),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_add_task_button")
            ) {
                Text(
                    text = if (isDaily) "Add to Routine" else "Schedule for ${formatDateToShort(if (selectedScheduleOption == "TOMORROW") tomorrowDate else customScheduledDate)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SetReminderDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var reminderTime by remember { mutableStateOf(task.reminderTime ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Reminder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter reminder time for '${task.title}':", fontSize = 13.sp)
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text("e.g. 07:30 AM or 10:00 PM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(reminderTime.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuneColors.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (!task.reminderTime.isNullOrBlank()) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Clear Reminder", color = Color(0xFFEF4444))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatDateToDisplay(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr)
        if (parsed != null) {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(parsed)
        } else {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        }
    } catch (_: Exception) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
}

private fun formatDateToShort(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = sdf.parse(dateStr)
        if (parsed != null) {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(parsed)
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun getTomorrowDateString(baseDateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val parsed = sdf.parse(baseDateStr)
        if (parsed != null) cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, 1)
        sdf.format(cal.time)
    } catch (_: Exception) {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
}
