package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DailySleepPatternItem(
    val dayOfWeek: String,
    val fullDate: String,
    val dateMillis: Long,
    val durationHours: Float,
    val durationFormatted: String,
    val targetHours: Float,
    val trendHours: Float,
    val bedtime: String,
    val bedtimeMinutes: Float,
    val wakeAlarmTime: String,
    val wakeMinutes: Float,
    val alarmName: String,
    val alarmDismissType: String,
    val dismissDelaySeconds: Int,
    val sleepScore: Int,
    val status: String,
    val hasRealData: Boolean
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("dayOfWeek", dayOfWeek)
            put("fullDate", fullDate)
            put("dateMillis", dateMillis)
            put("durationHours", (durationHours * 10).roundToInt() / 10.0)
            put("durationFormatted", durationFormatted)
            put("targetHours", targetHours)
            put("trendHours", (trendHours * 10).roundToInt() / 10.0)
            put("bedtime", bedtime)
            put("bedtimeMinutes", bedtimeMinutes)
            put("wakeAlarmTime", wakeAlarmTime)
            put("wakeMinutes", (wakeMinutes * 10).roundToInt() / 10.0)
            put("alarmName", alarmName)
            put("alarmDismissType", alarmDismissType)
            put("dismissDelaySeconds", dismissDelaySeconds)
            put("sleepScore", sleepScore)
            put("status", status)
            put("hasRealData", hasRealData)
        }
    }
}

data class WeeklySleepSummary(
    val weekRangeText: String,
    val days: List<DailySleepPatternItem>,
    val avgDurationHours: Float,
    val avgDurationFormatted: String,
    val targetGoalHours: Float,
    val totalSleepDebtHours: Float,
    val wakeConsistencyPercent: Int,
    val avgDismissSeconds: Int,
    val fastestWakeDay: String,
    val highestSleepDay: String
) {
    fun toJsonString(): String {
        val array = JSONArray()
        days.forEach { array.put(it.toJsonObject()) }
        return array.toString()
    }
}

object WeeklySleepPatternCalculator {

    fun computeWeeklySummary(
        alarmEvents: List<AlarmEventEntity>,
        alarms: List<AlarmEntity>,
        sleepSessions: List<SleepSessionEntity>,
        userProfile: UserProfileEntity?,
        weekOffset: Int = 0 // 0 = current week, -1 = last week
    ): WeeklySleepSummary {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        if (weekOffset != 0) {
            cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        }

        // Move to Monday of target week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val mondayMillis = cal.timeInMillis

        val targetGoalHours = (userProfile?.sleepGoalMinutes ?: 480) / 60f
        val bedtimeHour = userProfile?.bedtimeHour ?: 23
        val bedtimeMinute = userProfile?.bedtimeMinute ?: 15

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dailyItems = mutableListOf<DailySleepPatternItem>()

        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val dayDurationList = mutableListOf<Float>()

        for (i in 0..6) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = mondayMillis + (i * 86_400_000L)

            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val dayLabel = dayNames[i]
            val fullDateStr = dateFormat.format(Date(dayStart))

            // Check if there is an alarm event in this day's window
            val matchingEvent = alarmEvents.firstOrNull { event ->
                val eventTime = if (event.dismissedAt > 0) event.dismissedAt else event.triggeredAt
                eventTime in (dayStart - 4 * 3600_000L)..dayEnd
            }

            // Check if there is a sleep session for this day
            val matchingSession = sleepSessions.firstOrNull { session ->
                session.startedAt in (dayStart - 12 * 3600_000L)..dayEnd ||
                        session.endedAt in dayStart..dayEnd
            }

            val matchingAlarm = if (matchingEvent != null) {
                alarms.firstOrNull { it.id == matchingEvent.alarmId }
            } else {
                alarms.firstOrNull { it.isEnabled } ?: alarms.firstOrNull()
            }

            val (wakeHours, wakeMins, wakeStr, alarmName, dismissType, dismissDelay, hasReal) =
                if (matchingEvent != null) {
                    val eventCal = Calendar.getInstance().apply {
                        timeInMillis = if (matchingEvent.dismissedAt > 0) matchingEvent.dismissedAt else matchingEvent.triggeredAt
                    }
                    val h = eventCal.get(Calendar.HOUR_OF_DAY)
                    val m = eventCal.get(Calendar.MINUTE)
                    val str = timeFormat.format(eventCal.time)
                    val name = matchingAlarm?.title ?: "Wake Alarm"
                    val type = matchingEvent.dismissType.title
                    val delay = matchingEvent.dismissDurationSeconds.coerceAtLeast(10)
                    Septuple(h, m, str, name, type, delay, true)
                } else if (matchingAlarm != null) {
                    // Estimated from active scheduled alarm
                    val h = matchingAlarm.hour
                    val m = matchingAlarm.minute
                    val eventCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                    }
                    val str = timeFormat.format(eventCal.time)
                    val name = matchingAlarm.title
                    val type = matchingAlarm.dismissType.title
                    val delay = 25 + (i * 3) % 20
                    Septuple(h, m, str, name, type, delay, false)
                } else {
                    Septuple(7, 0, "07:00 AM", "Morning Alarm", "Math", 30, false)
                }

            // Calculate sleep duration based on bedtime to alarm trigger/wake time
            val rawDurationHours: Float = if (matchingSession != null && matchingSession.durationMinutes > 60) {
                matchingSession.durationMinutes / 60f
            } else {
                // Time difference from bedtime the night before to alarm wake time
                val bedTotalMins = bedtimeHour * 60 + bedtimeMinute
                val wakeTotalMins = (wakeHours + 24) * 60 + wakeMins
                val diffMins = wakeTotalMins - bedTotalMins
                (diffMins / 60f).coerceIn(4.5f, 10.5f)
            }

            dayDurationList.add(rawDurationHours)

            // Rolling trend
            val trend = dayDurationList.average().toFloat()

            val bedCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, bedtimeHour)
                set(Calendar.MINUTE, bedtimeMinute)
            }
            val bedtimeStr = timeFormat.format(bedCal.time)
            val bedtimeFraction = bedtimeHour + (bedtimeMinute / 60f)
            val wakeFraction = wakeHours + (wakeMins / 60f)

            val durationH = rawDurationHours.toInt()
            val durationM = ((rawDurationHours - durationH) * 60).roundToInt()
            val durationFormatted = "${durationH}h ${String.format(Locale.getDefault(), "%02dm", durationM)}"

            val status = when {
                rawDurationHours >= targetGoalHours + 0.5f -> "Recovery"
                rawDurationHours >= targetGoalHours - 0.4f -> "Optimal"
                rawDurationHours >= targetGoalHours - 1.2f -> "Mild Deficit"
                else -> "Deficit"
            }

            val score = matchingSession?.sleepScore ?: (85 + ((rawDurationHours - targetGoalHours) * 8).toInt()).coerceIn(60, 98)

            dailyItems.add(
                DailySleepPatternItem(
                    dayOfWeek = dayLabel,
                    fullDate = fullDateStr,
                    dateMillis = dayStart,
                    durationHours = rawDurationHours,
                    durationFormatted = durationFormatted,
                    targetHours = targetGoalHours,
                    trendHours = trend,
                    bedtime = bedtimeStr,
                    bedtimeMinutes = bedtimeFraction,
                    wakeAlarmTime = wakeStr,
                    wakeMinutes = wakeFraction,
                    alarmName = alarmName,
                    alarmDismissType = dismissType,
                    dismissDelaySeconds = dismissDelay,
                    sleepScore = score,
                    status = status,
                    hasRealData = hasReal
                )
            )
        }

        val avgHours = dailyItems.map { it.durationHours }.average().toFloat()
        val avgH = avgHours.toInt()
        val avgM = ((avgHours - avgH) * 60).roundToInt()
        val avgFormatted = "${avgH}h ${avgM}m"

        val totalDebt = dailyItems.sumOf { (it.durationHours - targetGoalHours).toDouble() }.toFloat()
        val avgDismiss = dailyItems.map { it.dismissDelaySeconds }.average().toInt()

        // Consistency: std-dev of wake times
        val wakeTimes = dailyItems.map { it.wakeMinutes }
        val avgWake = wakeTimes.average()
        val variance = wakeTimes.map { (it - avgWake) * (it - avgWake) }.average()
        val stdDev = Math.sqrt(variance)
        val consistency = ((1.0 - (stdDev / 3.0)).coerceIn(0.65, 0.98) * 100).toInt()

        val fastest = dailyItems.minByOrNull { it.dismissDelaySeconds }?.dayOfWeek ?: "Mon"
        val highest = dailyItems.maxByOrNull { it.durationHours }?.dayOfWeek ?: "Sat"

        val startStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(mondayMillis))
        val endStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(mondayMillis + 6 * 86_400_000L))
        val weekRange = "$startStr - $endStr"

        return WeeklySleepSummary(
            weekRangeText = weekRange,
            days = dailyItems,
            avgDurationHours = avgHours,
            avgDurationFormatted = avgFormatted,
            targetGoalHours = targetGoalHours,
            totalSleepDebtHours = totalDebt,
            wakeConsistencyPercent = consistency,
            avgDismissSeconds = avgDismiss,
            fastestWakeDay = fastest,
            highestSleepDay = highest
        )
    }

    private data class Septuple<A, B, C, D, E, F, G>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
        val sixth: F,
        val seventh: G
    )
}
