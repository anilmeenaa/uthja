# Today's Routine & Date-Based Task Scheduling Architecture

A complete, production-grade upgrade to the Today's Routine and Tasks management system in Uthja. This plan introduces date-based task scheduling, automatic midnight date transitions, smooth completion animations with slide-out physics, and persistent daily routine tracking while preserving the purple visual design.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed with you in Phase 1 and are baked into this implementation plan:

- **Completed Tasks Destination**: Completed tasks play a smooth checkmark scale animation, slide out horizontally from today's active list, and smoothly appear in a clean, collapsible **"Completed (N)"** drawer at the bottom where they can be reviewed or unmarked if needed.
- **Date Selection in Add Task Sheet**: The Add Routine Task bottom sheet will feature quick selection chips for **"Today"** and **"Tomorrow"**, alongside a calendar picker button that opens a Material 3 DatePickerDialog for any future date.
- **Task Recurrence**: Tasks support both **Daily Routine** (repeating daily routines that automatically appear fresh each morning) and **Single Date Scheduling** (one-off tasks tied strictly to a designated calendar date).

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the Tasks tab into an intelligent daily planner. It displays only tasks scheduled for today, dynamically updates when the clock strikes midnight, allows scheduling tasks for future dates with exact reminder alarms, and delivers micro-animated completion interactions that update progress bars in real time.
- **Target Audience / Persona**: Users building disciplined morning and evening habits who need their daily routine front-and-center, with the flexibility to queue tasks for tomorrow or later in the week without cluttering today's list.
- **Key Value**: Zero distraction from future tasks on today's dashboard, seamless midnight transitions (no manual refresh or accidental data wipe), and satisfying tactile animations that motivate task completion.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Viewing Today's Routine**:
   - Header shows "Today's Routine" with dynamic date subtitle (e.g. `Thursday, November 5`).
   - Progress bar and counter show `X / Y completed (Z%)`.
   - List displays only active tasks scheduled for today or daily recurring routines not yet completed today.
   - At the bottom, a neat collapsible card shows "Completed (N)" with expand/collapse chevron.
2. **Scheduling a Task via (+) FAB**:
   - Tapping the floating `+` button opens the bottom sheet with Lune's purple/coral accent.
   - User picks from quick routine presets (water, sunlight, meditation, reading, etc.) or enters a custom task.
   - User chooses schedule date: **Today**, **Tomorrow**, or taps **Calendar icon** to pick any future date.
   - User toggles "Repeat daily" if they want it every morning, or leaves it for the chosen date only.
   - Sets optional reminder time (e.g. `07:30 AM`).
   - Tapping "Add to Routine" saves the task and schedules a precise system notification if a reminder was set.
   - If scheduled for tomorrow or a future date, it does not appear in today's active list, but is safely persisted in Room.
3. **Completing a Task**:
   - User taps the circular checkbox.
   - Checkmark scales up with an energetic pop (`scaleIn(spring)`) and purple fill animation.
   - Completed row smoothly slides out (`slideOutHorizontally` + `fadeOut`) after a brief 250ms celebration delay.
   - Remaining tasks animate upward using `Modifier.animateItemPlacement()`.
   - Progress bar smoothly fills to the new percentage, and the task moves into the "Completed (N)" section.
   - Accidental double-taps are blocked while the transition executes.
4. **Midnight Rollover (12:00 AM)**:
   - Dynamic ticker in `LuneViewModel` detects date transition (even while app is running in foreground or resumes from background).
   - "Today's Routine" updates its displayed date string.
   - Tasks scheduled for the new date immediately appear.
   - Daily recurring routines reset their active status for the new day, while past completed records remain archived.

### Visual Identity & Theme
- **Primary Colors**: Deep midnight purple (`#1E1B4B`), vibrant electric violet (`#7C3AED`), active accent coral (`#FF3B5C`), and serene surface lavender (`#F8FAFC` / `#ECE0FD`).
- **Typography**: Crisp modern sans with strong weights for headers (`26.sp`, bold) and legible metadata pills (`11.sp`, medium).
- **Motion & Feedback**:
  - Haptic feedback on tap (`pulseAppleSelection` and `pulseAppleSuccess`).
  - Progress indicator animated via `animateFloatAsState(tween(400))`.
  - Item placement animation in `LazyColumn` for seamless layout reflows.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Date Modeling & Daily Recurrence in Room
- **Chosen Approach**: Enhance `TaskEntity` with three fields:
  - `scheduledDate: String?`: Date string in ISO format (`yyyy-MM-dd`), e.g. `2026-11-05`.
  - `isDaily: Boolean`: `true` for perpetual daily habits (e.g. "Drink water"), `false` for one-off scheduled tasks.
  - `lastCompletedDate: String?`: Date string of when the task was most recently marked completed.
- **Why**: Clean, reliable, and backward-compatible. A daily task is active for today if `lastCompletedDate != todayDate`. Once completed, `lastCompletedDate = todayDate`. When midnight passes, `todayDate` shifts to tomorrow, automatically reactivating the daily task without needing batch updates or database write churn!
- **Trade-off**: Avoids complicated multi-table habit logs while keeping database queries simple and fast.

### Decision 2: Midnight Transition Engine
- **Chosen Approach**: Combine a coroutine periodic ticker in `LuneViewModel` (checking every 30 seconds for date roll) with `ProcessLifecycleOwner` / `Lifecycle.Event.ON_RESUME` checks.
- **Why**: Handles both scenarios: (1) user has the app open when midnight strikes, and (2) user reopens the app the next morning after it was suspended.
- **Trade-off**: Virtually zero battery impact (simple comparison of `currentDateString` every 30s) while ensuring instant visual reactivity.

### Decision 3: Micro-Animations & Preventing Double-Tap Glitches
- **Chosen Approach**: In `TasksScreen`, track `animatingTaskIds: Set<Long>`. When a task is tapped, it immediately enters `animatingTaskIds`, disables further clicks on that row, triggers the checkmark fill and spring pop, waits 280ms, and then commits `onToggleTask(task)`.
- **Why**: Guarantees users see the tactile tick animation before the row slides out, while preventing rapid double-clicks from corrupting state.

---

## 4. Technical Architecture & Data Strategy

### System Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              TasksScreen                               │
│  ┌───────────────────────┐  ┌────────────────────────────────────────┐ │
│  │ Today's Routine       │  │ Progress: 3/5 Completed (60%)          │ │
│  │ Thursday, November 5  │  │ [████████████░░░░░░░░]                 │ │
│  └───────────────────────┘  └────────────────────────────────────────┘ │
│                                                                        │
│  Active Today's Tasks (Scheduled for Today OR Daily Uncompleted)      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ [○] Drink 500ml water             💧 2 glasses         🔔 07:00   │  │
│  │ [○] Morning sunlight              ☀️ 10 min            🔔 07:15   │  │
│  │ [○] Project Review (Nov 5 only)   📝 30 min            🔔 11:00   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  Collapsible Completed Tasks (Completed Today)                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ [✓] Completed (2)                                             ▼  │  │
│  │   [●] Morning stretch             🏃 20 min                       │  │
│  │   [●] Mindful meditation          🧘 10 min                       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  [+] Floating Action Button (above bottom navigation)                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ opens
┌───────────────────────────────────▼────────────────────────────────────┐
│                    TaskCreationBottomSheet                             │
│  - Quick presets: 💧 Drink water, ☀️ Sunlight, 🧘 Meditation, etc.     │
│  - Schedule Date: [Today] [Tomorrow] [📅 Custom Date Picker]           │
│  - Recurrence: [✓ Repeat daily routine]                                │
│  - Custom title, duration pill, icon picker, and reminder time         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ saves
┌───────────────────────────────────▼────────────────────────────────────┐
│                   LuneViewModel & LuneRepository                       │
│  - currentDateFlow: StateFlow<String> (auto-refreshed at midnight)     │
│  - todayTasks: derivedStateOf { tasks for todayDate }                  │
│  - TaskReminderScheduler: schedules exact alarm for scheduled date/time│
│  - LuneDatabase (Room v15): tasks table with scheduledDate & isDaily   │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model Enhancements (`Entities.kt`)

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timePill: String? = null,
    val iconType: String = "WATER",
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val reminderTime: String? = null,
    val scheduledDate: String? = null, // "yyyy-MM-dd" e.g. "2026-11-05"
    val isDaily: Boolean = true,       // true = perpetual habit, false = specific date
    val lastCompletedDate: String? = null // "yyyy-MM-dd"
) {
    fun isCompletedForDate(targetDate: String): Boolean {
        return if (isDaily) {
            lastCompletedDate == targetDate
        } else {
            isCompleted
        }
    }

    fun isScheduledForDate(targetDate: String): Boolean {
        return if (isDaily) {
            true
        } else {
            scheduledDate == targetDate
        }
    }
}
```

### File Modification Plan
1. **`Entities.kt`**: Add `scheduledDate`, `isDaily`, and `lastCompletedDate` to `TaskEntity` with convenience helper methods.
2. **`LuneDatabase.kt`**: Bump Room database version to `15` to support new columns with fallback destructive migration.
3. **`TaskReminderScheduler.kt`**: Update `calculateTriggerTimeMs` to support specific calendar dates (`yyyy-MM-dd`) so reminders fire on their exact scheduled day.
4. **`LuneViewModel.kt`**:
   - Add `currentDateFlow: StateFlow<String>` with continuous midnight watcher.
   - Update `addTask()` to accept `scheduledDate` and `isDaily`.
   - Update `toggleTask()` to update `lastCompletedDate` appropriately.
   - Filter `todayTasks` reactive flow to strictly present today's tasks.
5. **`TasksScreen.kt`**:
   - Add dynamic date header subtitle under "Today's Routine".
   - Implement checkmark spring animation and horizontal slide-out.
   - Implement collapsible "Completed (N)" section at the bottom.
   - Update `TaskCreationBottomSheet` with [Today], [Tomorrow], and Material 3 Calendar Date Picker chips.
6. **`ExampleUnitTest.kt` & `ExampleRobolectricTest.kt`**: Add unit tests for date filtering, midnight rollover logic, and task completion state.
