package com.codebutler.farebot.transit.seqgo

import com.codebutler.farebot.base.util.ByteUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * Date parsing utilities for Go Cards, extracted for multiplatform compatibility.
 */
object SeqGoDateUtil {
    /**
     * Date format:
     *
     * 0001111 1100 00100 = 2015-12-04
     * yyyyyyy mmmm ddddd
     *
     * Bottom 11 bits = minutes since 00:00
     * Time is represented in localtime, Australia/Brisbane.
     *
     * Assumes that data has already been byte-reversed for big endian parsing.
     *
     * @param timestamp Four bytes of input representing the timestamp to parse
     * @return Date and time represented by that value
     */
    fun unpackDate(timestamp: ByteArray): Instant {
        val minute = ByteUtils.getBitsFromBuffer(timestamp, 5, 11)
        val year = ByteUtils.getBitsFromBuffer(timestamp, 16, 7) + 2000
        val month = ByteUtils.getBitsFromBuffer(timestamp, 23, 4)
        val day = ByteUtils.getBitsFromBuffer(timestamp, 27, 5)

        if (minute > 1440) {
            throw AssertionError("Minute > 1440")
        }
        if (minute < 0) {
            throw AssertionError("Minute < 0")
        }

        if (day > 31) {
            throw AssertionError("Day > 31")
        }
        if (month > 12) {
            throw AssertionError("Month > 12")
        }

        val hour = minute / 60
        val min = minute % 60
        return LocalDateTime(year, month, day, hour, min).toInstant(TimeZone.of("Australia/Brisbane"))
    }
}
