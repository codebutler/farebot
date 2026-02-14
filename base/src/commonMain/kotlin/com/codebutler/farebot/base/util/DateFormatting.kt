package com.codebutler.farebot.base.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class DateFormatStyle { SHORT, LONG }

fun formatDate(
    instant: Instant,
    style: DateFormatStyle,
): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (style) {
        DateFormatStyle.SHORT -> "${dt.year}-${(dt.month.ordinal + 1).pad()}-${dt.day.pad()}"
        DateFormatStyle.LONG -> "${dt.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${dt.day}, ${dt.year}"
    }
}

fun formatTime(
    instant: Instant,
    style: DateFormatStyle,
): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (style) {
        DateFormatStyle.SHORT -> "${dt.hour.pad()}:${dt.minute.pad()}"
        DateFormatStyle.LONG -> "${dt.hour.pad()}:${dt.minute.pad()}:${dt.second.pad()}"
    }
}

fun formatDateTime(
    instant: Instant,
    dateStyle: DateFormatStyle,
    timeStyle: DateFormatStyle,
): String = "${formatDate(instant, dateStyle)} ${formatTime(instant, timeStyle)}"

/**
 * Formats an instant as a human-friendly date for section headers.
 * - Today: "Today"
 * - Yesterday: "Yesterday"
 * - This year: "Feb 11"
 * - Other years: "Feb 11, 2024"
 */
fun formatHumanDate(
    instant: Instant,
    todayLabel: String = "Today",
    yesterdayLabel: String = "Yesterday",
): String {
    val tz = TimeZone.currentSystemDefault()
    val dt = instant.toLocalDateTime(tz)
    val now =
        kotlin.time.Clock.System
            .now()
            .toLocalDateTime(tz)
    val monthAbbrev =
        dt.month.name
            .take(3)
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    val today = kotlinx.datetime.LocalDate(now.year, now.month, now.day)
    val date = kotlinx.datetime.LocalDate(dt.year, dt.month, dt.day)
    val daysDiff = today.toEpochDays() - date.toEpochDays()

    return when (daysDiff) {
        0L -> todayLabel
        1L -> yesterdayLabel
        else -> if (dt.year == now.year) "$monthAbbrev ${dt.day}" else "$monthAbbrev ${dt.day}, ${dt.year}"
    }
}

/**
 * Formats an instant as a short 12-hour time string, e.g. "2:35 PM".
 */
fun formatTimeShort(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour =
        if (dt.hour == 0) {
            12
        } else if (dt.hour > 12) {
            dt.hour - 12
        } else {
            dt.hour
        }
    val amPm = if (dt.hour < 12) "AM" else "PM"
    return "$hour:${dt.minute.pad()} $amPm"
}

private fun Int.pad(): String = toString().padStart(2, '0')
