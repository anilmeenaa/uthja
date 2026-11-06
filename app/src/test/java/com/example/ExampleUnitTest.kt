package com.example

import com.example.data.model.WallpaperCatalog
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun test_scenic_nature_is_default_wallpaper() {
    assertEquals("wp_nature_scenic", WallpaperCatalog.DEFAULT_WALLPAPER_ID)

    val defaultNull = WallpaperCatalog.findById(null)
    assertEquals("wp_nature_scenic", defaultNull.id)
    assertEquals("Scenic Nature", defaultNull.title)

    val defaultBlank = WallpaperCatalog.findById("")
    assertEquals("wp_nature_scenic", defaultBlank.id)

    val defaultExplicit = WallpaperCatalog.findById("DEFAULT")
    assertEquals("wp_nature_scenic", defaultExplicit.id)

    // User selected other wallpaper retains choice
    val memeChoice = WallpaperCatalog.findById("wp_y_u_no")
    assertEquals("wp_y_u_no", memeChoice.id)
    assertEquals("Y U NO Wake Up", memeChoice.title)
  }

  @Test
  fun test_task_date_scheduling_and_midnight_rollover() {
    val today = "2026-11-05"
    val tomorrow = "2026-11-06"

    // 1. Daily recurring task
    val dailyHabit = com.example.data.model.TaskEntity(
        id = 1L,
        title = "Drink Water",
        isDaily = true,
        lastCompletedDate = today
    )
    assertTrue("Daily habit scheduled for today", dailyHabit.isScheduledForDate(today))
    assertTrue("Daily habit scheduled for tomorrow", dailyHabit.isScheduledForDate(tomorrow))
    assertTrue("Daily habit is completed for today", dailyHabit.isCompletedForDate(today))
    assertFalse("Daily habit automatically active for tomorrow (midnight reset)", dailyHabit.isCompletedForDate(tomorrow))

    // 2. Tomorrow-scheduled task
    val tomorrowTask = com.example.data.model.TaskEntity(
        id = 2L,
        title = "Dentist Appointment",
        scheduledDate = tomorrow,
        isDaily = false,
        isCompleted = false
    )
    assertFalse("Tomorrow task must not appear in today's routine", tomorrowTask.isScheduledForDate(today))
    assertTrue("Tomorrow task automatically appears when date arrives", tomorrowTask.isScheduledForDate(tomorrow))
    assertFalse("Tomorrow task is not yet completed", tomorrowTask.isCompletedForDate(tomorrow))

    // 3. Today-only task
    val todayOnlyTask = com.example.data.model.TaskEntity(
        id = 3L,
        title = "Submit Report",
        scheduledDate = today,
        isDaily = false,
        isCompleted = true
    )
    assertTrue("Today only task scheduled for today", todayOnlyTask.isScheduledForDate(today))
    assertFalse("Today only task does not carry over to tomorrow", todayOnlyTask.isScheduledForDate(tomorrow))
    assertTrue("Today only task is completed", todayOnlyTask.isCompletedForDate(today))
  }

  @Test
  fun test_reminder_scheduler_date_trigger() {
    val triggerMs = com.example.alarm.TaskReminderScheduler.calculateTriggerTimeMsForDate(8, 30, "2026-11-20")
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = triggerMs }
    assertEquals(2026, cal.get(java.util.Calendar.YEAR))
    assertEquals(java.util.Calendar.NOVEMBER, cal.get(java.util.Calendar.MONTH))
    assertEquals(20, cal.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(8, cal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(30, cal.get(java.util.Calendar.MINUTE))
  }
}


