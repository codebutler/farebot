package com.codebutler.farebot.base.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class DateFormatStyle { SHORT, LONG }

fun formatDate(instant: Instant, style: DateFormatStyle): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (style) {
        DateFormatStyle.SHORT -> "${dt.year}-${(dt.month.ordinal + 1).pad()}-${dt.day.pad()}"
        DateFormatStyle.LONG -> "${dt.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${dt.day}, ${dt.year}"
    }
}

fun formatTime(instant: Instant, style: DateFormatStyle): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (style) {
        DateFormatStyle.SHORT -> "${dt.hour.pad()}:${dt.minute.pad()}"
        DateFormatStyle.LONG -> "${dt.hour.pad()}:${dt.minute.pad()}:${dt.second.pad()}"
    }
}

fun formatDateTime(instant: Instant, dateStyle: DateFormatStyle, timeStyle: DateFormatStyle): String =
    "${formatDate(instant, dateStyle)} ${formatTime(instant, timeStyle)}"

private fun Int.pad(): String = toString().padStart(2, '0')
