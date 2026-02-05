/*
 * DateTimeTest.kt
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.base.util

import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for date/time utilities using kotlinx.datetime.
 *
 * Ported from Metrodroid's DateTest.kt and TimeTest.kt
 */
class DateTimeTest {

    private val epochDate = LocalDate(1970, Month.JANUARY, 1)

    /**
     * Calculate days from epoch to January 1st of the given year.
     * This is similar to Metrodroid's yearToDays function.
     */
    private fun yearToDays(year: Int): Int =
        epochDate.daysUntil(LocalDate(year, Month.JANUARY, 1))

    /**
     * Check if a year is a leap year (bissextile).
     */
    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    @Test
    fun testYearToDays() {
        // Test yearToDays calculation matches expected value
        // Before 1600 Java calendar switches to Julian calendar - we don't test those years
        for (year in 1600..2100) {
            val d = yearToDays(year)
            val ly = year - 1
            // Expected calculation: total days since epoch
            // = year * 365 + leap years adjustments - days from epoch to year 0
            val expectedD = year * 365 + ly / 4 - ly / 100 + ly / 400 - 719527
            assertEquals(
                expected = expectedD,
                actual = d,
                message = "Wrong days for year $year: $d vs $expectedD"
            )
        }
    }

    @Test
    fun testDaysToYearMonthDay() {
        // Test that we can correctly convert days since epoch back to year/month/day
        val monthDays = listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        // Calculate starting days for year 1600
        var days = 1600 * 365 + 1599 / 4 - 1599 / 100 + 1599 / 400 - 719527

        // Test through several centuries - limited to avoid test timeout
        for (year in 1600..2100) {
            for (month in 1..12) {
                for (day in 1..monthDays[month - 1]) {
                    // Skip Feb 29 on non-leap years
                    if (day == 29 && month == 2 && !isLeapYear(year)) {
                        continue
                    }

                    // Convert days back to LocalDate
                    val localDate = epochDate + DatePeriod(days = days)
                    days++

                    assertEquals(
                        expected = year,
                        actual = localDate.year,
                        message = "Wrong year for $year-$month-$day vs $localDate"
                    )
                    assertEquals(
                        expected = month,
                        actual = localDate.month.ordinal + 1,
                        message = "Wrong month for $year-$month-$day vs $localDate"
                    )
                    assertEquals(
                        expected = day,
                        actual = localDate.day,
                        message = "Wrong day for $year-$month-$day vs $localDate"
                    )
                }
            }
        }
    }

    @Test
    fun testDaysRoundTrip() {
        // Test that converting to LocalDate and back gives the same number of days
        for (days in 0..2000) {
            val localDate = epochDate + DatePeriod(days = days)
            val roundTrippedDays = epochDate.daysUntil(localDate)

            assertEquals(
                expected = days,
                actual = roundTrippedDays,
                message = "Wrong roundtrip $days vs $roundTrippedDays"
            )
        }
    }

    @Test
    fun testTimeZoneConversionNegativeOffset() {
        // Test time zone with negative offset (e.g., New York)
        val tz = TimeZone.of("America/New_York")

        // Create a local date-time in 1997
        val localDateTime = LocalDateTime(1997, 1, 6, 1, 17)
        val instant = localDateTime.toInstant(tz)

        // New York is UTC-5 in January
        // 1997-01-06 01:17:00 EST = 1997-01-06 06:17:00 UTC
        val expectedMillis = 852531420000L
        assertEquals(expectedMillis, instant.toEpochMilliseconds())

        // Verify the conversion back
        val convertedDateTime = instant.toLocalDateTime(tz)
        assertEquals(1997, convertedDateTime.year)
        assertEquals(Month.JANUARY, convertedDateTime.month)
        assertEquals(6, convertedDateTime.day)
        assertEquals(1, convertedDateTime.hour)
        assertEquals(17, convertedDateTime.minute)
    }

    @Test
    fun testTimeZoneConversionPositiveOffset() {
        // Test time zone with positive offset (e.g., Helsinki)
        val tz = TimeZone.of("Europe/Helsinki")

        // Create a local date-time in 1997
        val localDateTime = LocalDateTime(1997, 1, 6, 1, 17)
        val instant = localDateTime.toInstant(tz)

        // Helsinki is UTC+2 in January
        // 1997-01-06 01:17:00 EET = 1997-01-05 23:17:00 UTC
        val expectedMillis = 852506220000L
        assertEquals(expectedMillis, instant.toEpochMilliseconds())

        // Verify the conversion back
        val convertedDateTime = instant.toLocalDateTime(tz)
        assertEquals(1997, convertedDateTime.year)
        assertEquals(Month.JANUARY, convertedDateTime.month)
        assertEquals(6, convertedDateTime.day)
        assertEquals(1, convertedDateTime.hour)
        assertEquals(17, convertedDateTime.minute)
    }

    @Test
    fun testInstantFromEpochDaysAndMinutes() {
        // Test creating Instant from days + minutes since a base year
        val baseYear = 1997
        val baseDays = yearToDays(baseYear)

        // Day offset of 5 from Jan 1, 1997 = Jan 6, 1997
        val dayOffset = 5
        // Minute offset of 77 = 1:17
        val minuteOffset = 77

        val tz = TimeZone.of("America/New_York")

        // Build the date: Jan 1, 1997 + 5 days = Jan 6, 1997
        val date = epochDate + DatePeriod(days = baseDays + dayOffset)

        // Build the time: 77 minutes = 1 hour 17 minutes
        val hour = minuteOffset / 60
        val minute = minuteOffset % 60

        val localDateTime = date.atTime(hour, minute)
        val instant = localDateTime.toInstant(tz)

        // Verify the result
        assertEquals(1997, instant.toLocalDateTime(tz).year)
        assertEquals(Month.JANUARY, instant.toLocalDateTime(tz).month)
        assertEquals(6, instant.toLocalDateTime(tz).day)
        assertEquals(1, instant.toLocalDateTime(tz).hour)
        assertEquals(17, instant.toLocalDateTime(tz).minute)
    }

    @Test
    fun testLeapYearDetection() {
        // Non-leap years
        assertTrue(!isLeapYear(1900), "1900 should not be a leap year (divisible by 100 but not 400)")
        assertTrue(!isLeapYear(2001), "2001 should not be a leap year")
        assertTrue(!isLeapYear(2100), "2100 should not be a leap year (divisible by 100 but not 400)")

        // Leap years
        assertTrue(isLeapYear(2000), "2000 should be a leap year (divisible by 400)")
        assertTrue(isLeapYear(2004), "2004 should be a leap year (divisible by 4)")
        assertTrue(isLeapYear(2024), "2024 should be a leap year (divisible by 4)")
    }

    @Test
    fun testDatePeriodAddition() {
        val startDate = LocalDate(2020, 1, 15)

        // Add months
        val plus1Month = startDate + DatePeriod(months = 1)
        assertEquals(LocalDate(2020, 2, 15), plus1Month)

        // Add years
        val plus1Year = startDate + DatePeriod(years = 1)
        assertEquals(LocalDate(2021, 1, 15), plus1Year)

        // Add days
        val plus30Days = startDate + DatePeriod(days = 30)
        assertEquals(LocalDate(2020, 2, 14), plus30Days)
    }

    @Test
    fun testInstantComparison() {
        val instant1 = Instant.fromEpochMilliseconds(1000000000000)
        val instant2 = Instant.fromEpochMilliseconds(1000000000001)
        val instant3 = Instant.fromEpochMilliseconds(1000000000000)

        assertTrue(instant1 < instant2)
        assertTrue(instant2 > instant1)
        assertTrue(instant1 == instant3)
        assertTrue(instant1 <= instant3)
        assertTrue(instant1 >= instant3)
    }
}
